package com.benmu.framework.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.benmu.framework.BMWXEnvironment;
import com.benmu.framework.R;
import com.benmu.framework.adapter.router.RouterTracker;
import com.benmu.framework.constant.Constant;
import com.benmu.framework.constant.WXEventCenter;
import com.benmu.framework.event.mediator.EventCenter;
import com.benmu.framework.manager.ManagerFactory;
import com.benmu.framework.manager.impl.FileManager;
import com.benmu.framework.manager.impl.ModalManager;
import com.benmu.framework.manager.impl.dispatcher.DispatchEventManager;
import com.benmu.framework.model.WebViewParamBean;
import com.benmu.widget.utils.BaseCommonUtil;

import java.io.File;
import java.util.Map;

/**
 * Created by Carry on 2017/8/25.
 */

public class GlobalWebViewActivity extends AbstractWeexActivity {

    private final String LOCAL_SCHEME = "bmlocal";

    private View rl_refresh;
    private ProgressBar mProgressBar;
    private WebView mWeb;
    private String mFailUrl;
    public static String WEBVIEW_URL = "WEBVIEW_URL";
    private WebViewParamBean mWebViewParams;
    private RelativeLayout mContainer;
    private String mTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        Intent data = getIntent();
        mWebViewParams = (WebViewParamBean) data.getSerializableExtra(Constant.WEBVIEW_PARAMS);
        String mUrl = mWebViewParams.getUrl();

        Uri imageUri = Uri.parse(mUrl);
        if (LOCAL_SCHEME.equalsIgnoreCase(imageUri.getScheme())) {
            mUrl = "file://" + localPath(imageUri);
        }

//        ShareInfoBean shareInfo = mWebViewParams.getShareInfo();
//        if (shareInfo != null) {
//            getNavigationBar().setRightIcon(R.drawable.icon_share);
//        }

        rl_refresh = findViewById(R.id.rl_refresh);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_progress);
        mWeb = (WebView) findViewById(R.id.webView);
        mContainer = (RelativeLayout) findViewById(R.id.rl_container);
        WebSettings settings = mWeb.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        mWeb.addJavascriptInterface(new JSMethod(this), "bmnative");
        settings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWeb.setWebViewClient(new MyWebViewClient(this));
        mWeb.setWebChromeClient(new MyWebChromeClient());
        if (!TextUtils.isEmpty(mUrl)) {
            mWeb.loadUrl(mUrl);
        }
        ModalManager.BmLoading.showLoading(this, "", true);
    }

    private String localPath(Uri uri) {
        String path = uri.getHost() + File.separator +
                uri.getPath();
        return FileManager.getPathBundleDir(this, "bundle/" + path)
                .getAbsolutePath();
    }

    private static class MyWebViewClient extends WebViewClient {
        GlobalWebViewActivity activity;

        public MyWebViewClient(GlobalWebViewActivity activity) {
            Log.d("SVProgressHUD", "MyWebViewClient hasCode -> " + activity.hashCode());
            this.activity = activity;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            ModalManager.BmLoading.dismissLoading(activity);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String
                failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            //L.i("web failingUrl == " + failingUrl);
            activity.mFailUrl = failingUrl;
            activity.showRefreshView();
        }
    }


    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (!TextUtils.isEmpty(title) && mTitle == null) {
                getNavigationBar().setTitle(title);
            }
        }

        @Override

        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.e("onConsoleMessage", "onConsoleMessage>>>>>" + consoleMessage.message());
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private void showRefreshView() {
        showWebCloseView();
    }


    @Override
    public void onBackPressed() {
        if (mWeb.canGoBack()) {
            mWeb.goBack();
        } else {
            BaseCommonUtil.clearAllCookies(this);
            super.onBackPressed();
        }

    }

    private void showWebCloseView() {

    }


    public static class JSMethod {
        private Context mContext;

        public JSMethod(Context mContext) {
            this.mContext = mContext;
        }

        @JavascriptInterface
        public void closePage() {
            //关闭当前页面
            RouterTracker.popActivity();
        }

        @JavascriptInterface
        public void fireEvent(String eventName, String param) {
            if (!TextUtils.isEmpty(eventName)) {
                Intent emit = new Intent(WXEventCenter.EVENT_JS_EMIT);
                emit.putExtra("data", new EventCenter.Emit(eventName, param));
                ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post(emit);
            }
        }
    }
}
