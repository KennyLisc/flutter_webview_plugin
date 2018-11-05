package com.flutter_webview_plugin;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Build;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import android.widget.FrameLayout;



import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import static android.app.Activity.RESULT_OK;

/**
 * Created by lejard_h on 20/12/2017.
 */

class WebviewManager {

    static final String LOG_TAG = "FlutterWebview";
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageArray;
    private final static int FILECHOOSER_RESULTCODE=1;

    @TargetApi(7)
    class ResultHandler {
        public boolean handleResult(int requestCode, int resultCode, Intent intent){
            boolean handled = false;
            if(Build.VERSION.SDK_INT >= 21){
                Uri[] results = null;
                // check result
                if(resultCode == Activity.RESULT_OK){
                    if(requestCode == FILECHOOSER_RESULTCODE){
                        if(mUploadMessageArray != null){
                            String dataString = intent.getDataString();
                            if(dataString != null){
                                results = new Uri[]{Uri.parse(dataString)};
                            }
                        }
                        handled = true;
                    }
                }
                mUploadMessageArray.onReceiveValue(results);
                mUploadMessageArray = null;
            }else {
                if (requestCode == FILECHOOSER_RESULTCODE) {
                    if (null != mUploadMessage) {
                        Uri result = intent == null || resultCode != RESULT_OK ? null
                                : intent.getData();
                        mUploadMessage.onReceiveValue(result);
                        mUploadMessage = null;
                    }
                    handled = true;
                }
            }
            return handled;
        }
    }

    boolean closed = false;
    WebView webView;
    Activity activity;
    ResultHandler resultHandler;

    WebviewManager(final Activity activity) {
        this.webView = new ObservableWebView(activity);
        this.activity = activity;
        this.resultHandler = new ResultHandler();
        WebViewClient webViewClient = new BrowserClient();
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
                                webView.goBack();
                            } else {
                                close();
                            }
                            return true;
                    }
                }

                return false;
            }
        });

        ((ObservableWebView) webView).setOnScrollChangedCallback(new ObservableWebView.OnScrollChangedCallback(){
            public void onScroll(int x, int y, int oldx, int oldy){
                Map<String, Object> yDirection = new HashMap<>();
                yDirection.put("yDirection", (double)y);
                FlutterWebviewPlugin.channel.invokeMethod("onScrollYChanged", yDirection);
                Map<String, Object> xDirection = new HashMap<>();
                xDirection.put("xDirection", (double)x);
                FlutterWebviewPlugin.channel.invokeMethod("onScrollXChanged", xDirection);
            }
        });

        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(new WebChromeClient()
        {
            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                activity.startActivityForResult(Intent.createChooser(i,"File Chooser"), FILECHOOSER_RESULTCODE);

            }

            // For Android 3.0+
            public void openFileChooser( ValueCallback uploadMsg, String acceptType ) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
               activity.startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FILECHOOSER_RESULTCODE);
            }

            //For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                activity.startActivityForResult( Intent.createChooser( i, "File Chooser" ), FILECHOOSER_RESULTCODE );

            }

            //For Android 5.0+
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams){
                if(mUploadMessageArray != null){
                    mUploadMessageArray.onReceiveValue(null);
                }
                mUploadMessageArray = filePathCallback;

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                Intent[] intentArray;
                intentArray = new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                activity.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        webView.addJavascriptInterface(new JavaScriptBridgeInterface(), JavaScriptBridgeInterface.name);
    }

    private void clearCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean aBoolean) {

                }
            });
        } else {
            CookieManager.getInstance().removeAllCookie();
        }
    }

    private void clearCache() {
        webView.clearCache(true);
        webView.clearFormData();
    }

    void openUrl(boolean withJavascript, boolean clearCache, boolean hidden, boolean clearCookies, String userAgent, String url, Map<String, String> headers, boolean withZoom, boolean withLocalStorage, boolean scrollBar) {
        webView.getSettings().setJavaScriptEnabled(withJavascript);
        webView.getSettings().setBuiltInZoomControls(withZoom);
        webView.getSettings().setSupportZoom(withZoom);
        webView.getSettings().setDomStorageEnabled(withLocalStorage);

        if (clearCache) {
            clearCache();
        }

        if (hidden) {
            webView.setVisibility(View.INVISIBLE);
        }

        if (clearCookies) {
            clearCookies();
        }

        if (userAgent != null) {
            webView.getSettings().setUserAgentString(userAgent);
        }
      
        if(!scrollBar){
            webView.setVerticalScrollBarEnabled(false);
        }

        if (headers != null) {
            webView.loadUrl(url, headers);
        } else {
            webView.loadUrl(url);
        }
    }

    void close(MethodCall call, MethodChannel.Result result) {
        if (webView != null) {
            ViewGroup vg = (ViewGroup) (webView.getParent());
            vg.removeView(webView);
        }
        webView = null;
        if (result != null) {
            result.success(null);
        }

        closed = true;
        FlutterWebviewPlugin.channel.invokeMethod("onDestroy", null);
    }

    void close() {
        close(null, null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    void eval(MethodCall call, final MethodChannel.Result result) {
        String code = call.argument("code");

        webView.evaluateJavascript(code, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                result.success(value);
            }
        });
    }
    /** 
    * Reloads the Webview.
    */
    void reload(MethodCall call, MethodChannel.Result result) {
        if (webView != null) {
            webView.reload();
        }
    }
    /** 
    * Navigates back on the Webview.
    */
    void back(MethodCall call, MethodChannel.Result result) {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }
    /** 
    * Navigates forward on the Webview.
    */
    void forward(MethodCall call, MethodChannel.Result result) {
        if (webView != null && webView.canGoForward()) {
            webView.goForward();
        }
    }

    void resize(FrameLayout.LayoutParams params) {
        webView.setLayoutParams(params);
    }
    /** 
    * Checks if going back on the Webview is possible.
    */
    boolean canGoBack() {
        return webView.canGoBack();
    }
    /** 
    * Checks if going forward on the Webview is possible.
    */
    boolean canGoForward() {
        return webView.canGoForward();
    }
    void hide(MethodCall call, MethodChannel.Result result) {
        if (webView != null) {
            webView.setVisibility(View.INVISIBLE);
        }
    }
    void show(MethodCall call, MethodChannel.Result result) {
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
        }
    }

    void stopLoading(MethodCall call, MethodChannel.Result result){
        if (webView != null){
            webView.stopLoading();
        }
    }

    public void loadUrl(String url, MethodChannel.Result result) {
        if (!url.isEmpty()) {
            webView.loadUrl(url);
        } else {
            result.error(LOG_TAG, "url is empty", null);
            return;
        }
        result.success(true);
    }

    public void loadUrl(String url, Map<String, String> headers, MethodChannel.Result result) {
        if (!url.isEmpty()) {
            webView.loadUrl(url, headers);
        } else {
            result.error(LOG_TAG, "url is empty", null);
            return;
        }
        result.success(true);
    }

    public void injectDeferredObject(String source, String jsWrapper, final MethodChannel.Result result) {
        String scriptToInject;
        if (jsWrapper != null) {
            org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            String jsonRepr = jsonEsc.toString();
            String jsonSourceString = jsonRepr.substring(1, jsonRepr.length() - 1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        final String finalScriptToInject = scriptToInject;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // This action will have the side-effect of blurring the currently focused element
                    webView.loadUrl("javascript:" + finalScriptToInject);
                } else {
                    webView.evaluateJavascript(finalScriptToInject, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (result == null)
                                return;

                            JsonReader reader = new JsonReader(new StringReader(s));

                            // Must set lenient to parse single values
                            reader.setLenient(true);

                            try {
                                String msg;
                                if (reader.peek() == JsonToken.STRING) {
                                    msg = reader.nextString();

                                    JsonReader reader2 = new JsonReader(new StringReader(msg));
                                    reader2.setLenient(true);

                                    if (reader2.peek() == JsonToken.STRING)
                                        msg = reader2.nextString();

                                    result.success(msg);
                                } else {
                                    result.success("");
                                }

                            } catch (IOException e) {
                                Log.e(LOG_TAG, "IOException", e);
                            } finally {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    // NOOP
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
