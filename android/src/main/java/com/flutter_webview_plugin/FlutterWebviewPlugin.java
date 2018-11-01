package com.flutter_webview_plugin;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.view.Display;
import android.widget.FrameLayout;
import com.tencent.smtt.sdk.QbSdk;

import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterWebviewPlugin
 */
public class FlutterWebviewPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
    private Activity activity;
    private WebviewManager webViewManager;
    static MethodChannel channel;
    private static final String CHANNEL_NAME = "flutter_webview_plugin";
    private static boolean qbSdkLoaded = false;

    public static void registerWith(PluginRegistry.Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
        final FlutterWebviewPlugin instance = new FlutterWebviewPlugin(registrar.activity());
        registrar.addActivityResultListener(instance);
        channel.setMethodCallHandler(instance);
    }

    private FlutterWebviewPlugin(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "initQbSdk":
                initQbSdk(call, result);
                break;
            case "start":
                openUrl(call, result);
                break;
            case "launch":
                openUrl(call, result);
                break;
            case "close":
                close(call, result);
                break;
            case "eval":
                eval(call, result);
                break;
            case "resize":
                resize(call, result);
                break;
            case "reload":
                reload(call, result);
                break;
            case "back":
                back(call, result);
                break;
            case "forward":
                forward(call, result);
                break;
            case "hide":
                hide(call, result);
                break;
            case "show":
                show(call, result);
                break;
            case "reloadUrl":
                reloadUrl(call, result);
                break;
            case "stopLoading":
                stopLoading(call, result);
                break;				
            default:
                result.notImplemented();
                break;
        }
    }

    private void initQbSdk(MethodCall call, final MethodChannel.Result result) {
        if (qbSdkLoaded) {
            result.success(null);
            return;
        }

        qbSdkLoaded = true;
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

            @Override
            public void onViewInitFinished(boolean arg0) {
                // TODO Auto-generated method stub
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.d("app", " onViewInitFinished is " + arg0);
                result.success(null);
            }

            @Override
            public void onCoreInitFinished() {
                // TODO Auto-generated method stub
                result.success(null);
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(activity.getApplicationContext(), cb);
        //https://www.jianshu.com/p/8ee549bdcbb8

    }

    private void openUrl(MethodCall call, MethodChannel.Result result) {
        boolean hidden = call.argument("hidden");
        String url = call.argument("url");
        String userAgent = call.argument("userAgent");
        boolean withJavascript = call.argument("withJavascript");
        boolean clearCache = call.argument("clearCache");
        boolean clearCookies = call.argument("clearCookies");
        boolean withZoom = call.argument("withZoom");
        boolean withLocalStorage = call.argument("withLocalStorage");
        Map<String, String> headers = call.argument("headers");
        boolean scrollBar = call.argument("scrollBar");

        if (webViewManager == null || webViewManager.closed == true) {
            webViewManager = new WebviewManager(activity);
        }

        FrameLayout.LayoutParams params = buildLayoutParams(call);

        activity.addContentView(webViewManager.webView, params);

        webViewManager.openUrl(withJavascript,
                clearCache,
                hidden,
                clearCookies,
                userAgent,
                url,
                headers,
                withZoom,
                withLocalStorage,
                scrollBar
        );
        result.success(null);
    }

    private FrameLayout.LayoutParams buildLayoutParams(MethodCall call) {
        Map<String, Number> rc = call.argument("rect");
        FrameLayout.LayoutParams params;
        if (rc != null) {
            params = new FrameLayout.LayoutParams(
                    dp2px(activity, rc.get("width").intValue()), dp2px(activity, rc.get("height").intValue()));
            params.setMargins(dp2px(activity, rc.get("left").intValue()), dp2px(activity, rc.get("top").intValue()),
                    0, 0);
        } else {
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            params = new FrameLayout.LayoutParams(width, height);
        }

        return params;
    }

    private void stopLoading(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null){
            webViewManager.stopLoading(call, result);
        }
    }

    private void close(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.close(call, result);
            webViewManager = null;
        }
    }

    /** 
    * Navigates back on the Webview.
    */
    private void back(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.back(call, result);
        }
    }
    /** 
    * Navigates forward on the Webview.
    */
    private void forward(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.forward(call, result);
        }
    }

    /** 
    * Reloads the Webview.
    */
    private void reload(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.reload(call, result);
        }
    }
    private void reloadUrl(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            String url = call.argument("url");
            webViewManager.openUrl(false,
                    false,
                    false,
                    false,
                    "",
                    url,
                    null,
                    false,
                    false,
                    false
            );
        }
    }
    private void eval(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.eval(call, result);
        }
    }

    private void resize(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            FrameLayout.LayoutParams params = buildLayoutParams(call);
            webViewManager.resize(params);
        }
        result.success(null);
    }
    private void hide(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.hide(call, result);
        }
    }
    private void show(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.show(call, result);
        }
    }

    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public boolean onActivityResult(int i, int i1, Intent intent) {
        if(webViewManager != null && webViewManager.resultHandler != null){
            return webViewManager.resultHandler.handleResult(i, i1, intent);
        }
        return false;
    }
}
