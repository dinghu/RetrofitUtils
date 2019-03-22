package com.retrofit.utils.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.retrofit.utils.Configuration;
import com.retrofit.utils.RetrofitHelper;
import com.retrofit.utils.RetrofitUtils;
import com.retrofit.utils.observer.HttpCallback;

public class MainActivity extends AppCompatActivity {

    RetrofitUtils retrofitUtils;
    RetrofitUtils retrofitUtils2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.Builder builder = new Configuration.Builder();
        builder.baseUrl("https://www.baidu.com").context(this);
        retrofitUtils = new RetrofitUtils(builder.build());
        retrofitUtils.getFullPath("https://www.baidu.com",null, new HttpCallback<String>() {
            @Override
            public void onError(String code, String message) {

            }

            @Override
            public void onResponse(String response) {
                Log.i("kv",response);
                Toast.makeText(MainActivity.this, response, 2000).show();
            }
        });


//        Configuration.Builder builder2 = Configuration.newConfiguration();
//        builder2.baseUrl("https://www.baidu.com").context(this);
//        retrofitUtils2 = new RetrofitUtils(builder2.build());
//        retrofitUtils2.get("", new HttpCallback<String>() {
//            @Override
//            public void onError(String code, String message) {
//                Toast.makeText(MainActivity.this, message, 2000).show();
//            }
//
//            @Override
//            public void onResponse(String response) {
//            }
//        });
    }
}
