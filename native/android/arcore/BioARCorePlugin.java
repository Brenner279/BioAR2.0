package br.com.michelbrenner.bioar.arcore;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BioARCore")
public class BioARCorePlugin extends Plugin {
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
