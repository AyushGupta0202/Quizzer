package com.eggdevs.quizzer.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eggdevs.quizzer.QuestionsActivity;
import com.eggdevs.quizzer.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.List;

public class GridAdapter extends BaseAdapter {

    private List<String> sets;
    private String category;
    private InterstitialAd mInterstitialAd;

    public GridAdapter(List<String> sets, String category, InterstitialAd mInterstitialAd) {
        this.sets = sets;
        this.category = category;
        this.mInterstitialAd = mInterstitialAd;
    }

    @Override
    public int getCount() {
        return sets.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View view, final ViewGroup viewGroup) {
        View v;
        if (view == null) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.set_item, viewGroup, false);
        }
        else {
            v = view;
        }

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mInterstitialAd.setAdListener(new AdListener(){
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();

                        mInterstitialAd.loadAd(new AdRequest.Builder().build());

                        viewGroup.getContext().startActivity(new Intent(viewGroup.getContext(), QuestionsActivity.class)
                                .putExtra("category", category)
                                .putExtra("setId", sets.get(position)));
                    }
                });

                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                    return;
                }

                viewGroup.getContext().startActivity(new Intent(viewGroup.getContext(), QuestionsActivity.class)
                        .putExtra("category", category)
                        .putExtra("setId", sets.get(position)));
            }
        });

        ((TextView)v.findViewById(R.id.tvSet)).setText(String.valueOf(position + 1));
        return v;
    }
}
