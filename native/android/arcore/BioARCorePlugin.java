package br.com.michelbrenner.bioar.arcore;

import android.app.Activity;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import androidx.activity.result.ActivityResult;
import java.util.Locale;
import java.util.Set;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BioARCore")
public class BioARCorePlugin extends Plugin {
    private TextToSpeech speechEngine;
    private boolean speechReady = false;

    @PluginMethod
    public void speakText(PluginCall call) {
        String text = call.getString("text");
        if (text == null || text.trim().isEmpty()) {
            call.reject("O texto para narração está vazio.");
            return;
        }

        if (speechEngine != null) {
            if (!speechReady) {
                call.reject("O mecanismo de voz ainda está inicializando. Tente novamente em instantes.");
                return;
            }
            speakWithLocalPortugueseVoice(call, text);
            return;
        }

        speechEngine = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                speechEngine = null;
                call.reject("Não foi possível inicializar o mecanismo de Texto para fala do Android.");
                return;
            }
            speechReady = true;
            speakWithLocalPortugueseVoice(call, text);
        });
    }

    private void speakWithLocalPortugueseVoice(PluginCall call, String text) {
        Set<Voice> voices = speechEngine.getVoices();
        Voice selectedVoice = null;
        if (voices != null) {
            for (Voice voice : voices) {
                Locale locale = voice.getLocale();
                if (locale != null
                        && "pt".equalsIgnoreCase(locale.getLanguage())
                        && !voice.isNetworkConnectionRequired()) {
                    if (selectedVoice == null) selectedVoice = voice;
                    if ("BR".equalsIgnoreCase(locale.getCountry())) {
                        selectedVoice = voice;
                        break;
                    }
                }
            }
        }

        if (selectedVoice == null) {
            call.reject("Nenhuma voz local em português foi encontrada. Baixe Português (Brasil) em Texto para fala nas configurações do Android.");
            return;
        }

        speechEngine.setVoice(selectedVoice);
        speechEngine.setSpeechRate(0.98f);
        int result = speechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bioar_narration");
        if (result == TextToSpeech.ERROR) {
            call.reject("O Android não conseguiu iniciar a reprodução da narração.");
        } else {
            call.resolve();
        }
    }

    @PluginMethod
    public void stopSpeech(PluginCall call) {
        if (speechEngine != null) speechEngine.stop();
        call.resolve();
    }

    @PluginMethod
    public void startMarkerScanner(PluginCall call) {
        Intent intent = new Intent(getContext(), MarkerScannerActivity.class);
        startActivityForResult(call, intent, "markerScannerResult");
    }

    @ActivityCallback
    private void markerScannerResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject response = new JSObject();
        Intent data = result.getData();
        boolean recognized = result.getResultCode() == Activity.RESULT_OK
                && data != null && data.getBooleanExtra("recognized", false);
        response.put("recognized", recognized);
        response.put("markerId", data == null ? "" : data.getStringExtra("markerId"));
        response.put("markerName", data == null ? "" : data.getStringExtra("markerName"));
        call.resolve(response);
    }
}
