package com.eggdevs.quizzer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ScoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        TextView tvScore = findViewById(R.id.tvScore);
        TextView tvTotal = findViewById(R.id.tvTotal);
        Button btnDone = findViewById(R.id.btnDone);

        tvScore.setText(String.valueOf(getIntent().getIntExtra("score", 0)));
        tvTotal.setText("Out of " + getIntent().getIntExtra("total", 0));

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}