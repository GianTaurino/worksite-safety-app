package it.unisalento.worksitesafety.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

public class ToastRunnable implements Runnable {//to toast to your main activity for some time
    String mText;
    int mtime;
    Context context;

    public ToastRunnable(String mText, int mtime, Context context) {
        this.mText = mText;
        this.mtime = mtime;
        this.context = context;
    }

    @Override
    public void run() {

        final Toast mytoast = Toast.makeText(context, mText, Toast.LENGTH_LONG);
        mytoast.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mytoast.cancel();
            }
        }, mtime);
    }
}