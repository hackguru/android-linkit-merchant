package ams.android.linkitmerchant.Fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import ams.android.linkitmerchant.Activity.MainActivity;
import ams.android.linkitmerchant.Adapter.AdapterListview;
import ams.android.linkitmerchant.Model.LinkitObject;
import ams.android.linkitmerchant.R;
import ams.android.linkitmerchant.Tools.GlobalApplication;
import ams.android.linkitmerchant.Tools.myListView;


/**
 * Created by Aidin on 2/1/2015.
 */
public class FragmentLinks extends Fragment {
    private static String TAG = "linkitMerchant";
    ArrayList<LinkitObject> items = new ArrayList<LinkitObject>();
    public AdapterListview adapterListview;
    myListView listView;
    String userID, regID;
    SwipeRefreshLayout swipeLayout;
    LinkitObject currentItem;
    RelativeLayout layWaiting;
    Boolean callState = false;
    TextView txtEmptyInfo;
    String globalEndDate = null;
    String globalStartDate = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!(getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        ((MainActivity) getActivity()).currentFragmentName = "Link";
        try {
            currentItem = getArguments().getParcelable("item");
        } catch (Exception ex) {
        }
        View rootView = inflater.inflate(R.layout.fragment_links, container, false);
        userID = ((GlobalApplication) getActivity().getApplication()).getUserId();
        regID = ((GlobalApplication) getActivity().getApplication()).getRegistrationId();
        listView = (myListView) rootView.findViewById(R.id.listView);
        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        ImageButton btnLogout = (ImageButton) rootView.findViewById(R.id.btn_logout);
        ImageButton btnInsta = (ImageButton) rootView.findViewById(R.id.btn_instagram);
        layWaiting = (RelativeLayout) rootView.findViewById(R.id.lay_waiting);
        txtEmptyInfo = (TextView) rootView.findViewById(R.id.txtEmptyInfo);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder
                        .setTitle("Logout")
                        .setMessage("Do you want to logout?")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                serverLogout();
                            }
                        });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        btnInsta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent insta_intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.instagram.android");
                    startActivity(insta_intent);
                } catch (Exception e) {
                    Log.e("linkitBuyer", "can't open Instagram");
                }
            }
        });

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        adapterListview = new AdapterListview(getActivity(), getFragmentManager(), items);
        listView.setAdapter(adapterListview);
        listView.setDescendantFocusability(ListView.FOCUS_AFTER_DESCENDANTS);
        listView.setOnDetectScrollListener(new myListView.OnDetectScrollListener() {
            @Override
            public void onUpScrolling() {

            }

            @Override
            public void onDownScrolling() {
                if (listView.getLastVisiblePosition() == items.size() - 1) {
                    if (!callState) {
                        //Log.i("linkit", "end list");
                        layWaiting.setVisibility(View.VISIBLE);
                        callState = true;
                        addDataToEnd();
                    }
                }
            }
        });

        swipeLayout.setRefreshing(true);
        refreshData();

        // Get tracker.
        Tracker t = ((GlobalApplication) getActivity().getApplication()).getTracker(GlobalApplication.TrackerName.APP_TRACKER);
        t.setScreenName("LinkitMerchant - List");
        t.send(new HitBuilders.AppViewBuilder().build());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void serverLogout() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
        client.addHeader("device", "android");
        client.addHeader("userType", "merchant");
        String URL = getResources().getString(R.string.BASE_URL) + "users/updateregid";
        client.post(URL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                ((GlobalApplication) getActivity().getApplication()).clearAllSettings();
                items.clear();
                FragmentLogin f1 = new FragmentLogin();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container, f1);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                if (statusCode == 401) {
                    Log.e("linkit-merchant", "ERR 401");
                    ((GlobalApplication) getActivity().getApplication()).clearAllSettings();
//                    showToast("Logout");
//                    FragmentLogin f1 = new FragmentLogin();
//                    FragmentTransaction ft = getFragmentManager().beginTransaction();
//                    ft.replace(R.id.container, f1); // f1_c   ontainer is your FrameLayout container
//                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//                    ft.addToBackStack("Login");
//                    ft.commit();
                } else {
                    Log.e("linkit-merchant", "ERR");
                }

            }

            @Override
            public void onRetry(int retryNo) {
            }
        });
    }

    public void addDataToEnd() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams requestParams = new RequestParams();
        client.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
        client.addHeader("device", "android");
        client.addHeader("userType", "merchant");
        if (globalStartDate != null) {
            requestParams.add("endDate", globalStartDate);
        }
        String URL = getResources().getString(R.string.BASE_URL) + "users/" + userID + "/postedmedias";
        client.get(URL, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    parseJSON(new String(response, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                globalStartDate = items.get(items.size() - 1).createdDate;
                adapterListview.notifyDataSetChanged();
                layWaiting.setVisibility(View.INVISIBLE);
                callState = false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                callState = false;
                swipeLayout.setRefreshing(false);
            }
        });
    }

    public void refreshData() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams requestParams = new RequestParams();
        client.addHeader("token", ((GlobalApplication) getActivity().getApplication()).getRegistrationId());
        client.addHeader("device", "android");
        client.addHeader("userType", "merchant");
        if (globalStartDate != null) {
            requestParams.add("startDate", globalStartDate);
        }
        String URL = getResources().getString(R.string.BASE_URL) + "users/" + userID + "/postedmedias";
        client.get(URL, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                items.clear();
                try {
                    parseJSON(new String(response, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (items.isEmpty()) {
                    txtEmptyInfo.setVisibility(View.VISIBLE);
                } else {
                    txtEmptyInfo.setVisibility(View.GONE);
                    //globalEndDate = items.get(0).createdDate;
                    globalStartDate = items.get(items.size() - 1).createdDate;

                    adapterListview.notifyDataSetChanged();
                    swipeLayout.setRefreshing(false);
                    if (currentItem != null) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        FragmentWebView f1 = new FragmentWebView();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("item", items.get(0));
                        f1.setArguments(bundle);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.add(R.id.container, f1, "WebView");
                        ft.addToBackStack("WebView");
                        ft.commit();
                        currentItem = null;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                swipeLayout.setRefreshing(false);
            }
        });
    }

    private void parseJSON(String jsonStr) {
        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                JSONArray feeds = jsonObj.getJSONArray("results");
                for (int i = 0; i < feeds.length(); i++) {
                    JSONObject item = feeds.getJSONObject(i);
                    LinkitObject myobject = new LinkitObject();

                    if (item.has("_id")) {
                        myobject.mediaID = item.getString("_id");
                    } else {
                        myobject.mediaID = "";
                    }
                    if (item.has("created")) {
                        myobject.createdDate = item.getString("created");
                    } else {
                        myobject.createdDate = "";
                    }
                    if (item.has("productDescription")) {
                        myobject.productDescription = item.getString("productDescription");
                    } else {
                        myobject.productDescription = "";
                    }
                    if (item.has("linkToProduct")) {
                        myobject.productLink = item.getString("linkToProduct");
                    } else {
                        myobject.productLink = "";
                    }
                    if (item.has("productLinkScreenshot")) {
                        myobject.linkSrceenShot = item.getString("productLinkScreenshot");
                    } else {
                        myobject.linkSrceenShot = "";
                    }
                    if (item.getJSONObject("images").getJSONObject("standard_resolution").has("url")) {
                        myobject.imageUrl = item.getJSONObject("images").getJSONObject("standard_resolution").getString("url");
                    } else {
                        myobject.imageUrl = "";
                    }
                    if (item.getJSONObject("owner").has("website")) {
                        myobject.ownerWebsite = item.getJSONObject("owner").getString("website");
                    } else {
                        myobject.ownerWebsite = "";
                    }

                    items.add(myobject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            //Log.e(TAG, "Couldn't get any data from the url");
        }
    }
}
