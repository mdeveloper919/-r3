package com.android.r3;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.TextView;

public class DetailView extends AppCompatActivity {

    String mimeType = "text/html";
    String encoding = "utf-8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);

        Intent intent = getIntent();
        String insert_date = intent.getStringExtra("insert_date");
        String long_info = intent.getStringExtra("long_info");

        WebView webView = (WebView)findViewById(R.id.webView2);
        webView.loadData(long_info, mimeType, encoding);


    }
}
