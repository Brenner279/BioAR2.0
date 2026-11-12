package br.com.michelbrenner.bioar.arcore;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraNotAvailableException;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableException;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class MarkerScannerActivity extends Activity implements GLSurfaceView.Renderer {
    private static final int CAMERA_PERMISSION_REQUEST = 4101;
    private static final String MARKER_ID = "heart";
    private static final String MARKER_NAME = "Coração";
    private static final float MARKER_WIDTH_METERS = 0.14f;

    private GLSurfaceView surfaceView;
    private TextView statusView;
    private Session session;
    private boolean installRequested;
    private boolean surfaceResumed;
    private volatile boolean sessionResumed;
    private volatile boolean viewportChanged = true;
    private volatile String statusOnScreen = "";
    private volatile boolean markerRecognized;

    private int surfaceWidth;
    private int surfaceHeight;
    private int cameraTextureId = -1;
    private boolean cameraTextureAssigned;
    private int cameraProgram;
    private int positionAttribute;
    private int texCoordAttribute;
    private int textureUniform;

    private final FloatBuffer quadVertices = createFloatBuffer(new float[] {
        -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f
    });
    private final FloatBuffer transformedTexCoords = createFloatBuffer(new float[8]);

    private static final String VERTEX_SHADER =
        "attribute vec4 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() { gl_Position = a_Position; v_TexCoord = a_TexCoord; }\n";

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "uniform samplerExternalOES sTexture;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() { gl_FragColor = texture2D(sTexture, v_TexCoord); }\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScannerInterface();
    }

    private void buildScannerInterface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        surfaceView = new GLSurfaceView(this);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        statusView = new TextView(this);
        statusView.setText("Preparando o scanner ARCore…");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(16);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(20, 16, 20, 16);
        statusView.setBackground(background(0xD9192938, 22));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        statusParams.setMargins(18, 24, 18, 0);
        root.addView(statusView, statusParams);

        Button finishButton = new Button(this);
        finishButton.setText("Finalizar teste");
        finishButton.setTextColor(Color.WHITE);
        finishButton.setTextSize(15);
        finishButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        finishButton.setAllCaps(false);
        finishButton.setBackground(background(0xFF0F766E, 18));
        finishButton.setOnClickListener(view -> finishScanner());
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        buttonParams.setMargins(28, 0, 28, 26);
        root.addView(finishButton, buttonParams);
        setContentView(root);
    }

    private GradientDrawable background(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        return drawable;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startArSession();
    }

    private void startArSession() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            postStatus("A realidade aumentada exige Android 7.0 ou superior.");
            return;
        }
        try {
            ArCoreApk.InstallStatus installStatus =
                ArCoreApk.getInstance().requestInstall(this, !installRequested);
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                installRequested = true;
                postStatus("Conclua a instalação/atualização do Google Play Services para RA e volte ao BioAR.");
                return;
            }

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST);
                postStatus("Autorize o acesso à câmera para iniciar a realidade aumentada.");
                return;
            }

            if (session == null) {
                session = new Session(this);
                configureImageDatabase();
            }
            session.resume();
            sessionResumed = true;
            resumeSurface();
            postStatus("Aponte a câmera para o marcador A1 do coração, impresso em 14 × 14 cm.");
        } catch (UnavailableException exception) {
            postStatus("ARCore indisponível: " + safeMessage(exception));
            closeSession();
        } catch (CameraNotAvailableException exception) {
            postStatus("A câmera não está disponível. Feche outros aplicativos que possam estar usando-a.");
            closeSession();
        } catch (Exception exception) {
            postStatus("Falha ao iniciar o scanner: " + safeMessage(exception));
            closeSession();
        }
    }

    private void configureImageDatabase() throws Exception {
        AugmentedImageDatabase database = new AugmentedImageDatabase(session);
        try (InputStream input = getAssets().open("ar_markers/heart-marker.png")) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) throw new IllegalStateException("A imagem de referência do coração não pôde ser carregada.");
            try {
                database.addImage(MARKER_ID, bitmap, MARKER_WIDTH_METERS);
            } finally {
                bitmap.recycle();
            }
        }

        Config config = new Config(session);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setAugmentedImageDatabase(database);
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
    }

    private void resumeSurface() {
        if (surfaceView != null && !surfaceResumed) {
            surfaceView.onResume();
            surfaceResumed = true;
        }
    }

    private void pauseSurface() {
        if (surfaceView != null && surfaceResumed) {
            surfaceView.onPause();
            surfaceResumed = false;
        }
    }

    @Override
    protected void onPause() {
        pauseSurface();
        if (session != null && sessionResumed) {
            session.pause();
            sessionResumed = false;
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startArSession();
            } else {
                postStatus("A permissão da câmera é necessária para reconhecer o marcador.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        pauseSurface();
        closeSession();
        super.onDestroy();
    }

    private void closeSession() {
        sessionResumed = false;
        if (session != null) {
            session.close();
            session = null;
        }
    }

    private void finishScanner() {
        Intent result = new Intent();
        result.putExtra("recognized", markerRecognized);
        result.putExtra("markerId", markerRecognized ? MARKER_ID : "");
        result.putExtra("markerName", markerRecognized ? MARKER_NAME : "");
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void postStatus(String message) {
        if (message == null || message.equals(statusOnScreen)) return;
        statusOnScreen = message;
        runOnUiThread(() -> {
            if (statusView != null) statusView.setText(message);
        });
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return (message == null || message.trim().isEmpty())
            ? exception.getClass().getSimpleName() : message;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        cameraTextureId = createCameraTexture();
        cameraProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionAttribute = GLES20.glGetAttribLocation(cameraProgram, "a_Position");
        texCoordAttribute = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord");
        textureUniform = GLES20.glGetUniformLocation(cameraProgram, "sTexture");
        cameraTextureAssigned = false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        viewportChanged = true;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Session currentSession = session;
        if (currentSession == null || !sessionResumed || cameraTextureId < 0 || cameraProgram == 0) return;

        try {
            if (!cameraTextureAssigned) {
                currentSession.setCameraTextureName(cameraTextureId);
                cameraTextureAssigned = true;
            }
            if (viewportChanged && surfaceWidth > 0 && surfaceHeight > 0) {
                currentSession.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(),
                    surfaceWidth, surfaceHeight);
                viewportChanged = false;
            }

            Frame frame = currentSession.update();
            drawCameraBackground(frame);
            updateImageStatus(currentSession);
        } catch (CameraNotAvailableException exception) {
            postStatus("A câmera ficou indisponível. Feche o scanner e tente novamente.");
        } catch (Exception exception) {
            postStatus("Erro durante o rastreamento: " + safeMessage(exception));
        }
    }

    private void updateImageStatus(Session currentSession) {
        for (AugmentedImage image : currentSession.getAllTrackables(AugmentedImage.class)) {
            if (!MARKER_ID.equals(image.getName())) continue;
            if (image.getTrackingState() == TrackingState.TRACKING) {
                markerRecognized = true;
                if (image.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {
                    postStatus("MARCADOR A1 RECONHECIDO\nCoração • rastreamento ativo");
                } else {
                    postStatus("Coração reconhecido\nAguardando rastreamento completo…");
                }
                return;
            }
        }
        if (!markerRecognized) {
            postStatus("Aponte a câmera para o marcador A1 do coração, impresso em 14 × 14 cm.");
        }
    }

    private void drawCameraBackground(Frame frame) {
        transformedTexCoords.clear();
        quadVertices.position(0);
        frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadVertices,
            Coordinates2d.TEXTURE_NORMALIZED, transformedTexCoords);
        transformedTexCoords.position(0);
        quadVertices.position(0);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(cameraProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glUniform1i(textureUniform, 0);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glVertexAttribPointer(positionAttribute, 2, GLES20.GL_FLOAT, false, 0, quadVertices);
        GLES20.glEnableVertexAttribArray(texCoordAttribute);
        GLES20.glVertexAttribPointer(texCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoords);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(texCoordAttribute);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private int createCameraTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return textures[0];
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String info = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new IllegalStateException("Falha ao preparar o renderizador da câmera: " + info);
        }
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String info = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Falha ao compilar shader: " + info);
        }
        return shader;
    }

    private static FloatBuffer createFloatBuffer(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * 4).order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(values);
        floatBuffer.position(0);
        return floatBuffer;
    }
}
