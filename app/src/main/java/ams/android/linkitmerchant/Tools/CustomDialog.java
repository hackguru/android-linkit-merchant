package ams.android.linkitmerchant.Tools;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import ams.android.linkitmerchant.R;

/**
 * Created by Aidin on 2/12/2015.
 */
public class CustomDialog extends Dialog  {

    public Activity c;
    public Dialog d;
    public Button ok, cancel;
    public EditText editTextDesc;

    public CustomDialog(Activity a) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_input);

//        ok.setOnClickListener(this);
//        cancel.setOnClickListener(this);

    }

//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.btn_ok:
//                //c.finish();
//                break;
//            case R.id.btn_cancel:
//                dismiss();
//                break;
//            default:
//                break;
//        }
//        dismiss();
//    }
}