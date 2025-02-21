package com.pichillilorenzo.flutter_inappwebview.headless_in_app_webview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pichillilorenzo.flutter_inappwebview.InAppWebViewFlutterPlugin;
import com.pichillilorenzo.flutter_inappwebview.Util;
import com.pichillilorenzo.flutter_inappwebview.in_app_webview.FlutterWebView;
import com.pichillilorenzo.flutter_inappwebview.types.Size2D;
import com.pichillilorenzo.flutter_inappwebview.types.URLRequest;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class HeadlessInAppWebView implements MethodChannel.MethodCallHandler {

  protected static final String LOG_TAG = "HeadlessInAppWebView";
  @NonNull
  public final String id;
  public final MethodChannel channel;
  @Nullable
  public FlutterWebView flutterWebView;
  @Nullable
  public InAppWebViewFlutterPlugin plugin;

  public HeadlessInAppWebView(@NonNull final InAppWebViewFlutterPlugin plugin, @NonNull String id, @NonNull FlutterWebView flutterWebView) {
    this.id = id;
    this.plugin = plugin;
    this.flutterWebView = flutterWebView;
    this.channel = new MethodChannel(plugin.messenger, "com.pichillilorenzo/flutter_headless_inappwebview_" + id);
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    switch (call.method) {
      case "dispose":
        dispose();
        result.success(true);
        break;
      case "setSize":
        {
          Map<String, Object> sizeMap = (Map<String, Object>) call.argument("size");
          Size2D size = Size2D.fromMap(sizeMap);
          if (size != null)
            setSize(size);
        }
        result.success(true);
        break;
      case "getSize":
        {
          Size2D size = getSize();
          result.success(size != null ? size.toMap() : null);
        }
        break;
      case "loadData":
        String data = (String) call.argument("data");
        String mimeType = (String) call.argument("mimeType");
        String encoding = (String) call.argument("encoding");
        String baseUrl = (String) call.argument("baseUrl");
        String historyUrl = (String) call.argument("historyUrl");
        this.flutterWebView.webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
        result.success(true);
        break;
      case "capture":
        {
          float scale = this.flutterWebView.webView.getScale();
          int width = this.flutterWebView.webView.getWidth();
          int height = (int) (this.flutterWebView.webView.getContentHeight() * scale + 0.5);
          Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
          Canvas canvas = new Canvas(bitmap);
          this.flutterWebView.webView.draw(canvas);
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            result.success(byteArrayOutputStream .toByteArray());
          } else {
            result.notImplemented();
          }
        }
        break;
      default:
        result.notImplemented();
    }
  }

  public void onWebViewCreated() {
    Map<String, Object> obj = new HashMap<>();
    channel.invokeMethod("onWebViewCreated", obj);
  }

  public void prepare(Map<String, Object> params) {
    // Add the headless WebView to the view hierarchy.
    // This way is also possible to take screenshots.
    ViewGroup contentView = (ViewGroup) plugin.activity.findViewById(android.R.id.content);
    ViewGroup mainView = (ViewGroup) (contentView).getChildAt(0);
    if (mainView != null) {
      View view = flutterWebView.getView();
      final Map<String, Object> initialSize = (Map<String, Object>) params.get("initialSize");
      Size2D size = Size2D.fromMap(initialSize);
      if (size != null) {
        setSize(size);
      } else {
        view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      }
      mainView.addView(view, 0);
      view.setVisibility(View.INVISIBLE);
    }
  }
  
  public void setSize(@NonNull Size2D size) {
    if (flutterWebView != null && flutterWebView.webView != null) {
      View view = flutterWebView.getView();
      float scale = Util.getPixelDensity(view.getContext());
      view.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scale), (int) (size.getHeight() * scale)));
    }
  }

  @Nullable
  public Size2D getSize() {
    if (flutterWebView != null && flutterWebView.webView != null) {
      View view = flutterWebView.getView();
      float scale = Util.getPixelDensity(view.getContext());
      ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
      return new Size2D(layoutParams.width / scale, layoutParams.height / scale);
    }
    return null;
  }

  public void dispose() {
    channel.setMethodCallHandler(null);
    HeadlessInAppWebViewManager.webViews.remove(id);
    ViewGroup contentView = (ViewGroup) plugin.activity.findViewById(android.R.id.content);
    ViewGroup mainView = (ViewGroup) (contentView).getChildAt(0);
    if (mainView != null) {
      mainView.removeView(flutterWebView.getView());
    }
    flutterWebView.dispose();
    flutterWebView = null;
    plugin = null;
  }
}
