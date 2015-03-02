package ams.android.linkitmerchant.Fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.edmodo.cropper.CropImageView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ams.android.linkitmerchant.Activity.MainActivity;
import ams.android.linkitmerchant.Model.LinkitObject;
import ams.android.linkitmerchant.R;
import ams.android.linkitmerchant.Tools.GlobalApplication;


/**
 * Created by Aidin on 2/3/2015.
 */
public class FragmentWebView extends Fragment {
    private static String TAG = "linkitMerchant";
    static WebView vistaWeb;
    static Bitmap bm;
    static ImageLoader imageLoader = ImageLoader.getInstance();
    static DisplayImageOptions options;
    static ImageLoadingListener imageListener;
    RelativeLayout mainView;
    Button btnCapture;
    EditText etxtUrl;
    CropImageView cropImageView;
    ImageButton btnBack;
    ImageButton btnForward;
    RelativeLayout layWaiting;
    LinkitObject currentItem;
    String urlPhoto, urlJSON;
    Boolean isInWebViewState = true;
    private static String defaultURL;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!(getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        ((MainActivity) getActivity()).currentFragmentName = "WebView";
        try {
            currentItem = getArguments().getParcelable("item");
        } catch (Exception ex) {
        }
        final View rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        final ProgressBar progressBarLoad = (ProgressBar) rootView.findViewById(R.id.progressBar_load);
        final ImageView imgInsta = (ImageView) rootView.findViewById(R.id.img_insta_preview);
        final ImageView imgInstaFull = (ImageView) rootView.findViewById(R.id.imgInstaPreviewFull);
        RelativeLayout layTopBar = (RelativeLayout) rootView.findViewById(R.id.lay_topBar);
        Button btnDone = (Button) rootView.findViewById(R.id.btnDone);
        mainView = (RelativeLayout) rootView.findViewById(R.id.lay_MainView);
        vistaWeb = (WebView) rootView.findViewById(R.id.webView_Content);
        btnCapture = (Button) rootView.findViewById(R.id.btn_capture);
        cropImageView = (CropImageView) rootView.findViewById(R.id.img_screenshot);
        layWaiting = (RelativeLayout) rootView.findViewById(R.id.lay_waiting);
        btnBack = (ImageButton) rootView.findViewById(R.id.btn_back);
        btnForward = (ImageButton) rootView.findViewById(R.id.btn_forward);
        etxtUrl = (EditText) rootView.findViewById(R.id.etxtUrl);

        options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .showImageOnFail(R.drawable.fail)
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.unlink).cacheInMemory(true)
                .preProcessor(new BitmapProcessor() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return Bitmap.createScaledBitmap(bitmap, 100, 100, true);
                    }
                })
                .cacheOnDisk(true).build();

        imageListener = new ImageDisplayListener();
        if (!imageLoader.isInited()) {
            imageLoader.init(ImageLoaderConfiguration.createDefault(getActivity().getApplicationContext()));
        }
        imageLoader = ImageLoader.getInstance();

        ViewTreeObserver vto = layTopBar.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageLoader.displayImage(currentItem.imageUrl, imgInsta, options, imageListener);
                imgInsta.getViewTreeObserver().removeOnPreDrawListener(this);
                imgInsta.getLayoutParams().width = imgInsta.getMeasuredHeight();
                return true;
            }
        });

        urlPhoto = getResources().getString(R.string.BASE_URL).toString() + "media/matchScreenShot/" + currentItem.mediaID;
        urlJSON = getResources().getString(R.string.BASE_URL).toString() + "media/match/" + currentItem.mediaID;
        defaultURL = (currentItem.ownerWebsite.length() > 0 ? currentItem.ownerWebsite : "http://www.google.com/?gws_rd=ssl");
        etxtUrl.setText(currentItem.productLink);

        cropImageView.setAspectRatio(640, 640);
        cropImageView.setFixedAspectRatio(true);
        cropImageView.setGuidelines(2);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vistaWeb.goBack();
            }
        });
        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vistaWeb.goForward();
            }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment currentFragment = getFragmentManager().findFragmentByTag("WebView");
                getActivity().getFragmentManager().beginTransaction().remove(currentFragment).commit();
                getFragmentManager().popBackStack();
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnBack.setVisibility(View.INVISIBLE);
                btnForward.setVisibility(View.INVISIBLE);

                if (isInWebViewState) {
                    vistaWeb.setDrawingCacheEnabled(true);
                    bm = vistaWeb.getDrawingCache();
                    cropImageView.setImageBitmap(bm);
                    cropImageView.setVisibility(View.VISIBLE);
                    btnCapture.setText("CROP");
                    vistaWeb.setVisibility(View.INVISIBLE);
                    isInWebViewState = false;
                } else {
                    try {
                        bm = cropImageView.getCroppedImage();
                        new PostPhotoAsync().execute();
                        layWaiting.setVisibility(View.VISIBLE);
                    } catch (Exception ex) {

                    }
                }
            }
        });
        etxtUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String url = etxtUrl.getText().toString();
                    if (URLUtil.isValidUrl(url)) {
                        vistaWeb.loadUrl(url);
                    } else {
                        vistaWeb.loadUrl("http://www.google.com/search?q=" + url);
                    }
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etxtUrl.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    return true;
                }
                return false;
            }
        });

        vistaWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                etxtUrl.setText(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBarLoad.setVisibility(View.VISIBLE);
                progressBarLoad.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                etxtUrl.setText(url);
                progressBarLoad.setVisibility(View.INVISIBLE);
            }
        });

        vistaWeb.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        vistaWeb.getSettings().setAppCacheEnabled(true);
        vistaWeb.getSettings().setJavaScriptEnabled(true);
        vistaWeb.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        if (currentItem.productLink.isEmpty()) {
            vistaWeb.loadUrl(defaultURL);
            etxtUrl.setText(defaultURL);
        } else {
            vistaWeb.loadUrl(currentItem.productLink);
        }

        imgInsta.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        imgInstaFull.setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:

                        imgInstaFull.setVisibility(View.INVISIBLE);
                        return true;
                }

                return false;
            }
        });


        imageLoader.displayImage(currentItem.imageUrl, imgInstaFull, options, imageListener);

        // Get tracker.
        Tracker t = ((GlobalApplication) getActivity().getApplication()).getTracker(GlobalApplication.TrackerName.APP_TRACKER);
        t.setScreenName("LinkitMerchant - WebView");
        t.send(new HitBuilders.AppViewBuilder().build());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainView.removeView(vistaWeb);
        vistaWeb.setFocusable(true);
        vistaWeb.removeAllViews();
        //vistaWeb.clearHistory();
        vistaWeb.destroy();
    }

    private class ImageDisplayListener extends SimpleImageLoadingListener {
        final List<String> displayedImages = Collections
                .synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            super.onLoadingStarted(imageUri, view);
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            super.onLoadingFailed(imageUri, view, failReason);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 0);
                    displayedImages.add(imageUri);
                }
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            super.onLoadingCancelled(imageUri, view);
        }
    }

    private class PostJSONAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            HttpResponse response;
            JSONObject json = new JSONObject();

            try {
                HttpPost post = new HttpPost(urlJSON);
                post.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
                post.addHeader("device", "android");
                post.addHeader("userType", "merchant");
                json.put("linkToProduct", etxtUrl.getText());
                StringEntity se = new StringEntity(json.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(se);
                client.execute(post);
                return "OK";

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                ((MainActivity) getActivity()).currentFragmentName = "Link";
                Fragment currentFragment = getFragmentManager().findFragmentByTag("WebView");
                getActivity().getFragmentManager().beginTransaction().remove(currentFragment).commit();
                ((FragmentLinks) getFragmentManager().findFragmentByTag("Links")).refreshData();
            } catch (Exception ex) {
                //Log.e("refresh after submit : ", ex.getMessage().toString());
            }
        }
    }

    public class PostPhotoAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bm = Bitmap.createScaledBitmap(bm, 640, 640, true);
                bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] data = bos.toByteArray();
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost postRequest = new HttpPost(urlPhoto);
                postRequest.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
                postRequest.addHeader("device", "android");
                postRequest.addHeader("userType", "merchant");
                ByteArrayBody bab = new ByteArrayBody(data, "androidScreenShot.png");
                MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
                reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("upload", bab);
                postRequest.setEntity(reqEntity.build());
                //HttpResponse response =
                httpClient.execute(postRequest);
                //BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
//                String sResponse;
//                StringBuilder s = new StringBuilder();
//                while ((sResponse = reader.readLine()) != null) {
//                    s = s.append(sResponse);
//                }
                //Log.i("linkit Response: ", s.toString());
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            new PostJSONAsync().execute();
        }
    }

    public void goBack() {
        vistaWeb.goBack();
    }

    public void backButtonWasPressed() {
        vistaWeb.setDrawingCacheEnabled(false);
        cropImageView.setVisibility(View.INVISIBLE);
        cropImageView.refreshDrawableState();
        btnCapture.setText("CAPTURE");
        vistaWeb.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        btnForward.setVisibility(View.VISIBLE);
        isInWebViewState = true;
    }

    public Boolean canGoBackHistory() {
        return vistaWeb.canGoBack();
    }

    public Boolean isReadyForExit() {
        if (isInWebViewState) {
            return true;
        } else {
            return false;
        }
    }
}
