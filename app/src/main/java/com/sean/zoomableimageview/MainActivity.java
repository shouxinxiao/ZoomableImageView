package com.sean.zoomableimageview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    List<String>data = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(new MyAdapter(data));

    }

    private void initData() {
        data.add("http://pic.58pic.com/58pic/13/53/30/36G58PICBcS_1024.jpg");
        for (int i = 0;i<20;i++){
            if (i % 2 == 0){
                data.add("http://pic.58pic.com/58pic/13/60/97/48Q58PIC92r_1024.jpg");
            }else{
                data.add("http://d.5857.com/xgs_150428/desk_005.jpg");
            }
        }

    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{
        List<String> data;
        public MyAdapter(List<String> data){
            this.data = data;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View view = layoutInflater.inflate(R.layout.rv_item,parent,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.bindData(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView mImageView;
        String url;
        public MyViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.image);
            itemView.setOnClickListener(this);
        }

        public void bindData(String url){
            this.url = url;
            Glide.with(MainActivity.this).load(url).into(mImageView);
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this,ShowImageViewActivity.class);
            //发送url
            intent.putExtra(ShowImageViewActivity.INTENT_KEY,url);
            startActivity(intent);
        }
    }

}
