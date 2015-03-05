package ams.android.linkitmerchant.Adapter;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ams.android.linkitmerchant.Fragment.FragmentLinks;
import ams.android.linkitmerchant.Fragment.FragmentWebView;
import ams.android.linkitmerchant.Model.LinkitObject;
import ams.android.linkitmerchant.R;
import ams.android.linkitmerchant.Tools.ClearableEditText;
import ams.android.linkitmerchant.Tools.GlobalApplication;

/**
 * Created by Aidin on 2/3/2015.
 */
public class AdapterListview extends BaseAdapter {
    private static String TAG = "linkitMerchant";
    private Context context;
    private final FragmentManager fragmentManager;
    private ArrayList<LinkitObject> items = new ArrayList<>();
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private final DisplayImageOptions options;
    private final ImageLoadingListener imageListener;
    private static int deletePosition = -1;
    private static int unMatchPosition = -1;

    private final class DescriptionData {
        int position;
        String description;
    }

    public AdapterListview(Context context, FragmentManager fragmentManager, ArrayList<LinkitObject> items) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.items = items;
        options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .showImageOnFail(R.drawable.fail)
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.unlink)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        imageListener = new ImageDisplayListener();
        if (!imageLoader.isInited()) {
            imageLoader.init(ImageLoaderConfiguration.createDefault(context));
        }
        imageLoader = ImageLoader.getInstance();
    }

    private static class ViewHolder {
        public ImageView imgLink;
        public ImageView imgInsta;
        public ClearableEditText txtDesc;
        public TextView txtDescFix;
        public ImageButton btnDeleteInsta;
        public ImageButton btnUnMatchLink;
    }

    @Override
    public View getView(final int position, View rootView, ViewGroup parent) {
        final ViewHolder holder;
        if (rootView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.item_list, null);
            holder = new ViewHolder();
            holder.imgLink = (ImageView) rootView.findViewById(R.id.img_link);
            holder.imgInsta = (ImageView) rootView.findViewById(R.id.img_insta);
            holder.txtDesc = (ClearableEditText) rootView.findViewById(R.id.etxtDesc);
            holder.txtDescFix = (TextView) rootView.findViewById(R.id.txtDescFix);
            holder.btnDeleteInsta = (ImageButton) rootView.findViewById(R.id.btnDelete);
            holder.btnUnMatchLink = (ImageButton) rootView.findViewById(R.id.btnUnMatch);
            rootView.setTag(holder);
        } else {
            holder = (ViewHolder) rootView.getTag();
        }
        imageLoader.displayImage(items.get(position).linkSrceenShot, holder.imgLink, options, imageListener);
        imageLoader.displayImage(items.get(position).imageUrl, holder.imgInsta, options, imageListener);

        if (position == deletePosition) {
            holder.btnDeleteInsta.setVisibility(View.VISIBLE);
        } else {
            holder.btnDeleteInsta.setVisibility(View.GONE);
        }
        if (position == unMatchPosition) {
            holder.btnUnMatchLink.setVisibility(View.VISIBLE);
        } else {
            holder.btnUnMatchLink.setVisibility(View.GONE);
        }

        holder.txtDescFix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.txtDesc.setVisibility(View.VISIBLE);
                holder.txtDesc.selectAll();
                holder.txtDesc.requestFocus();
                holder.txtDescFix.setVisibility(View.INVISIBLE);
            }
        });
        holder.txtDesc.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    holder.txtDesc.clearFocus();
                    holder.txtDesc.setVisibility(View.INVISIBLE);
                    holder.txtDescFix.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    return false;
                }
            }
        });
        holder.txtDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(holder.txtDesc.getWindowToken(), 0);
                    DescriptionData descriptionData = new DescriptionData();
                    descriptionData.position = position;
                    descriptionData.description = holder.txtDesc.getText().toString();
                    holder.txtDescFix.setText(holder.txtDesc.getText());
                    new postDescriptionAsync().execute(descriptionData);
                    holder.txtDesc.clearFocus();
                    holder.txtDesc.changeToNormalMode();
                    holder.txtDesc.setVisibility(View.INVISIBLE);
                    holder.txtDescFix.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    //return true;
                }
                return false;
            }
        });

        if (!items.get(position).productDescription.equals("null")) {
            holder.txtDesc.setText(items.get(position).productDescription);
            holder.txtDescFix.setText(items.get(position).productDescription);
        } else {
            holder.txtDesc.setText("");
            holder.txtDescFix.setText("");
        }

        // Link Button click
        holder.imgLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.btnUnMatchLink.getVisibility() == View.VISIBLE) {
                    Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    holder.btnUnMatchLink.startAnimation(animFadeOut);
                    holder.btnUnMatchLink.setVisibility(View.GONE);
                    unMatchPosition = -1;
                    FragmentLinks.notifyDataSetChanged();
                } else {
                    FragmentWebView f1 = new FragmentWebView();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("item", items.get(position));
                    f1.setArguments(bundle);
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.add(R.id.container, f1, "WebView");
                    ft.addToBackStack("WebView");
                    ft.commit();

                }
            }
        });
        // Link Button Long click
        holder.imgLink.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (items.get(position).productLink.length() > 0) {
                    if (holder.btnUnMatchLink.getVisibility() == View.GONE) {
                        Animation animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                        holder.btnUnMatchLink.setVisibility(View.VISIBLE);
                        holder.btnUnMatchLink.startAnimation(animFadeIn);
                        unMatchPosition = position;
                        FragmentLinks.notifyDataSetChanged();
                    } else {
                        Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                        holder.btnUnMatchLink.startAnimation(animFadeOut);
                        holder.btnUnMatchLink.setVisibility(View.GONE);
                        unMatchPosition = -1;
                        FragmentLinks.notifyDataSetChanged();
                    }
                }
                return true;
            }
        });


        // Insta Button click
        holder.imgInsta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.btnDeleteInsta.getVisibility() == View.VISIBLE) {
                    Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    holder.btnDeleteInsta.startAnimation(animFadeOut);
                    holder.btnDeleteInsta.setVisibility(View.GONE);
                    deletePosition = -1;
                    FragmentLinks.notifyDataSetChanged();
                }
            }
        });
        // Insta Button Long click
        holder.imgInsta.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (holder.btnDeleteInsta.getVisibility() == View.GONE) {
                    Animation animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                    holder.btnDeleteInsta.setVisibility(View.VISIBLE);
                    holder.btnDeleteInsta.startAnimation(animFadeIn);
                    deletePosition = position;
                    FragmentLinks.notifyDataSetChanged();
                } else {
                    Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    holder.btnDeleteInsta.startAnimation(animFadeOut);
                    holder.btnDeleteInsta.setVisibility(View.GONE);
                    deletePosition = -1;
                    FragmentLinks.notifyDataSetChanged();
                }
                return true;
            }
        });
        // Delete task for insta image
        holder.btnDeleteInsta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this post?")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DescriptionData descriptionData = new DescriptionData();
                                descriptionData.position = position;
                                new postDeleteAsync().execute(descriptionData);
                                Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                                holder.btnDeleteInsta.startAnimation(animFadeOut);
                                holder.btnDeleteInsta.setVisibility(View.GONE);
                                deletePosition = -1;
                                FragmentLinks.notifyDataSetChanged();
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
        // unMatch task for link image
        holder.btnUnMatchLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DescriptionData descriptionData = new DescriptionData();
                descriptionData.position = position;
                new postUnMatchAsync().execute(descriptionData);
                Animation animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                holder.btnUnMatchLink.startAnimation(animFadeOut);
                holder.btnUnMatchLink.setVisibility(View.GONE);
                unMatchPosition = -1;
                FragmentLinks.notifyDataSetChanged();
            }
        });

        return rootView;
    }

    private class postDescriptionAsync extends AsyncTask<DescriptionData, Void, String> {
        @Override
        protected String doInBackground(DescriptionData... descriptionDatas) {
            HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            HttpResponse response;
            JSONObject json = new JSONObject();

            try {
                String urlJSON = context.getResources().getString(R.string.BASE_URL).toString() + "media/match/" + items.get(descriptionDatas[0].position).mediaID;
                HttpPost post = new HttpPost(urlJSON);
                post.addHeader("token", ((GlobalApplication) context.getApplicationContext()).getRegistrationId());
                post.addHeader("device", "android");
                post.addHeader("userType", "merchant");
                json.put("productDescription", descriptionDatas[0].description);
                StringEntity se = new StringEntity(json.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(se);
                response = client.execute(post);

                    /*Checking response */
                InputStream in = null;
                if (response != null) {
                    in = response.getEntity().getContent(); //Get the data in the entity
                }

                items.get(descriptionDatas[0].position).productDescription = descriptionDatas[0].description;

                Log.i("linkit Response : " + descriptionDatas[0].description + " => ", in.toString());
                return "OK";

            } catch (Exception e) {
                Log.i("linkit Response: ", "error" + e.getMessage());
                e.printStackTrace();
                return "ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {


        }
    }

    private class postUnMatchAsync extends AsyncTask<DescriptionData, Void, Integer> {
        @Override
        protected Integer doInBackground(DescriptionData... descriptionDatas) {
            HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            JSONObject json = new JSONObject();
            try {
                String urlJSON = context.getResources().getString(R.string.BASE_URL).toString() + "media/match/" + items.get(descriptionDatas[0].position).mediaID;
                HttpPost post = new HttpPost(urlJSON);
                post.addHeader("token", ((GlobalApplication) context.getApplicationContext()).getRegistrationId());
                post.addHeader("device", "android");
                post.addHeader("userType", "merchant");
                json.put("linkToProduct", "");
                StringEntity se = new StringEntity(json.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(se);
                client.execute(post);
                items.get(descriptionDatas[0].position).productDescription = descriptionDatas[0].description;
                Log.i(TAG, "unMatch item " + descriptionDatas[0].position);
                return descriptionDatas[0].position;
            } catch (Exception e) {
                Log.i(TAG, "unMatch error " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result > -1) {
                items.remove(result);
                FragmentLinks.unMatchItem(result);
            }

        }
    }

    private class postDeleteAsync extends AsyncTask<DescriptionData, Void, Integer> {
        @Override
        protected Integer doInBackground(DescriptionData... descriptionDatas) {
            HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            try {
                String urlJSON = context.getResources().getString(R.string.BASE_URL).toString() + "media/" + items.get(descriptionDatas[0].position).mediaID;
                HttpDelete delete = new HttpDelete(urlJSON);
                delete.addHeader("token", ((GlobalApplication) context.getApplicationContext()).getRegistrationId());
                delete.addHeader("device", "android");
                delete.addHeader("userType", "merchant");
                client.execute(delete);
                Log.i(TAG, "delete item " + descriptionDatas[0].position);
                return descriptionDatas[0].position;
            } catch (Exception e) {
                Log.i(TAG, "delete error " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result > -1) {
                items.remove(result);
                FragmentLinks.deleteItem(result);
            }
        }
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

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}

