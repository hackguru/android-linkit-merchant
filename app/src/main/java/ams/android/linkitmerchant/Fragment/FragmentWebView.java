package ams.android.linkitmerchant.Fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ams.android.linkitmerchant.Model.LinkitObject;
import ams.android.linkitmerchant.R;
import ams.android.linkitmerchant.Tools.GlobalApplication;


/**
 * Created by Aidin on 2/3/2015.
 */
public abstract class FragmentWebView extends Fragment {
    WebView vistaWeb;
    Button btnCapture, btnGo;
    EditText etxtUrl;
    CropImageView cropImageView;
    ImageButton btnBack;
    ImageButton btnForward;
    RelativeLayout layWaiting;
    LinkitObject currentItem;
    Bitmap bm;
    String urlPhoto, urlJSON;
    Boolean isInWebViewState = true;
    protected BackHandlerInterface backHandlerInterface;
    private static String defaultURL = "http://www.google.com/?gws_rd=ssl";
    ImageLoader imageLoader = ImageLoader.getInstance();
    DisplayImageOptions options;
    ImageLoadingListener imageListener;

    public abstract boolean onBackPressed();


    public static final FragmentWebView newInstance(LinkitObject item) {
        FragmentWebView f = new FragmentWebView() {
            @Override
            public boolean onBackPressed() {
                return false;
            }
        };
        f.currentItem = item;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!(getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        final View rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        vistaWeb = (WebView) rootView.findViewById(R.id.webView_Content);
        btnCapture = (Button) rootView.findViewById(R.id.btn_capture);
        cropImageView = (CropImageView) rootView.findViewById(R.id.img_screenshot);
        final ProgressBar progressBarLoad = (ProgressBar) rootView.findViewById(R.id.progressBar_load);
        RelativeLayout layTopBar = (RelativeLayout) rootView.findViewById(R.id.lay_topBar);
        layWaiting = (RelativeLayout) rootView.findViewById(R.id.lay_waiting);
        btnBack = (ImageButton) rootView.findViewById(R.id.btn_back);
        btnForward = (ImageButton) rootView.findViewById(R.id.btn_forward);
        final ImageView imgInsta = (ImageView) rootView.findViewById(R.id.img_insta_preview);

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

        etxtUrl = (EditText) rootView.findViewById(R.id.etxtUrl);
        urlPhoto = getResources().getString(R.string.BASE_URL).toString() + "media/matchScreenShot/" + currentItem.mediaID;
        urlJSON = getResources().getString(R.string.BASE_URL).toString() + "media/match/" + currentItem.mediaID;
        etxtUrl.setText(currentItem.productLink);
        cropImageView.setAspectRatio(600, 600);
        cropImageView.setFixedAspectRatio(true);
        cropImageView.setGuidelines(2);


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vistaWeb.goBack();
                checkNavigationButton();
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vistaWeb.goForward();
                checkNavigationButton();
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
                    //bm = Bitmap.createScaledBitmap(bm, 600, 600, false);
                    cropImageView.setImageBitmap(bm);
                    cropImageView.setVisibility(View.VISIBLE);
                    btnCapture.setText("CROP");
                    vistaWeb.setVisibility(View.INVISIBLE);
                    isInWebViewState = false;
                } else {
                    bm = cropImageView.getCroppedImage();
                    new PostPhotoAsync().execute();
                    layWaiting.setVisibility(View.VISIBLE);
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
                checkNavigationButton();
            }
        });

        //vistaWeb.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.4.2; GT-I9500 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.109 Mobile Safari/537.36");
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

        return rootView;
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

    private void checkNavigationButton() {
        if (vistaWeb.canGoBack()) {
            btnBack.setVisibility(View.VISIBLE);
        } else {
            btnBack.setVisibility(View.INVISIBLE);
        }

        if (vistaWeb.canGoForward()) {
            btnForward.setVisibility(View.VISIBLE);
        } else {
            btnForward.setVisibility(View.INVISIBLE);
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
                //json.put("productDescription", "Photo has more information!");
                json.put("linkToProduct", etxtUrl.getText());
                StringEntity se = new StringEntity(json.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(se);
                response = client.execute(post);

                    /*Checking response */
                InputStream in = null;
                if (response != null) {
                    in = response.getEntity().getContent(); //Get the data in the entity
                }
                Log.i("linkit Response: ", in.toString());
                return "OK";

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            getFragmentManager().popBackStack();
            ((FragmentLinks) getFragmentManager().findFragmentByTag("Links")).refreshData(null, null, null);
            //Toast.makeText(getActivity(), "JSON Upload : " + result, Toast.LENGTH_LONG).show();

        }
    }

    public class PostPhotoAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                //bm = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Forest.png");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] data = bos.toByteArray();
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost postRequest = new HttpPost(urlPhoto);
                postRequest.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
                postRequest.addHeader("device", "android");
                postRequest.addHeader("userType", "merchant");
                ByteArrayBody bab = new ByteArrayBody(data, "androidScreenShot.png");
                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("upload", bab);
                postRequest.setEntity(reqEntity);
                HttpResponse response = httpClient.execute(postRequest);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                String sResponse;
                StringBuilder s = new StringBuilder();
                while ((sResponse = reader.readLine()) != null) {
                    s = s.append(sResponse);
                }
                Log.i("linkit Response: ", s.toString());
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getActivity(), "Photo Upload : " + result, Toast.LENGTH_LONG).show();
            new PostJSONAsync().execute();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getActivity() instanceof BackHandlerInterface)) {
            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
        } else {
            backHandlerInterface = (BackHandlerInterface) getActivity();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        backHandlerInterface.setSelectedFragment(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public interface BackHandlerInterface {
        public void setSelectedFragment(FragmentWebView backHandledFragment);
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
        checkNavigationButton();
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
