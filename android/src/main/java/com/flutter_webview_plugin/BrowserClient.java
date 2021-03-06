package com.flutter_webview_plugin;

import android.graphics.Bitmap;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lejard_h on 20/12/2017.
 */

public class BrowserClient extends WebViewClient {
    public BrowserClient() {
        super();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("type", "startLoad");
        FlutterWebviewPlugin.channel.invokeMethod("onState", data);
    }

    static final String consoleLogJS = "(function() {" +
            "   var oldLogs = {" +
            "       'log': console.log," +
            "       'debug': console.debug," +
            "       'error': console.error," +
            "       'info': console.info," +
            "       'warn': console.warn" +
            "   };" +
            "   for (var k in oldLogs) {" +
            "       (function(oldLog) {" +
            "           console[oldLog] = function() {" +
            "               var message = '';" +
            "               for (var i in arguments) {" +
            "                   if (message == '') {" +
            "                       message += arguments[i];" +
            "                   }" +
            "                   else {" +
            "                       message += ' ' + arguments[i];" +
            "                   }" +
            "               }" +
            "               oldLogs[oldLog].call(console, message);" +
            "           }" +
            "       })(k);" +
            "   }" +
            "})();";

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);

        view.evaluateJavascript(consoleLogJS, null);
        view.evaluateJavascript(JavaScriptBridgeInterface.flutterInAppBroserJSClass, null);

        FlutterWebviewPlugin.channel.invokeMethod("onUrlChanged", data);

        data.put("type", "finishLoad");
        FlutterWebviewPlugin.channel.invokeMethod("onState", data);

    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        Map<String, Object> data = new HashMap<>();
        data.put("url", request.getUrl().toString());
        data.put("code", Integer.toString(errorResponse.getStatusCode()));
        FlutterWebviewPlugin.channel.invokeMethod("onHttpError", data);
    }
}