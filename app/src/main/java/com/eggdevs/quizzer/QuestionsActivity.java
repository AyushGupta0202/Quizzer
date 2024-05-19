package com.eggdevs.quizzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eggdevs.quizzer.models.QuestionModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuestionsActivity extends AppCompatActivity {

    public static final String FILE_NAME = "QUIZZER";
    public static final String KEY_NAME = "QUESTIONS";

    private TextView tvQuestion, tvNumberIndicator;
    private FloatingActionButton fabBookmark;
    private LinearLayout optionsContainer;
    private Button btnShare, btnNext;
    private int count = 0;
    private List<QuestionModel> questionModelList;
    private int position = 0, score = 0;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private Dialog loadingDialog;

    private String setId;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private List<QuestionModel> bookmarkList;
    private int matchedQuestionPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupUI();

        preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();

        getBookmarks();

        fabBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (modelMatch()) {
                    bookmarkList.remove(matchedQuestionPosition);
                    fabBookmark.setImageDrawable(getDrawable(R.drawable.bookmark_border));
                } else {
                    bookmarkList.add(questionModelList.get(position));
                    fabBookmark.setImageDrawable(getDrawable(R.drawable.bookmark));
                }
            }
        });

        setId = getIntent().getStringExtra("setId");

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(false);

        loadingDialog.show();
        myRef.child("SETS").child(setId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {

                    String id = snapshot1.getKey();
                    String question = snapshot1.child("question").getValue().toString();
                    String optionA = snapshot1.child("optionA").getValue().toString();
                    String optionB = snapshot1.child("optionB").getValue().toString();
                    String optionC = snapshot1.child("optionC").getValue().toString();
                    String optionD = snapshot1.child("optionD").getValue().toString();
                    String correctAns = snapshot1.child("correctAns").getValue().toString();

                    questionModelList.add(new QuestionModel(id, question, optionA, optionB, optionC, optionD, correctAns, setId));
                }
                if (questionModelList.size() > 0) {

                    for (int i = 0; i < 4; i++) {
                        optionsContainer.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                checkAnswer((Button)view);
                            }
                        });
                    }

                    playAnim(tvQuestion, 0, questionModelList.get(position).getQuestion());
                    btnNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            btnNext.setEnabled(false);
                            btnNext.setAlpha(0.7f);
                            position++;

                            enableOption(true);

                            if (position == questionModelList.size()) {
                                ///score activity
                                startActivity(new Intent(QuestionsActivity.this,
                                        ScoreActivity.class)
                                .putExtra("score", score)
                                .putExtra("total", questionModelList.size()));

                                finish();

                                return;
                            }
                            count = 0;
                            playAnim(tvQuestion, 0, questionModelList.get(position).getQuestion());
                        }
                    });

                    btnShare.setOnClickListener(new View.OnClickListener() {
                        private String sendIntentBody = questionModelList.get(position).getQuestion() + "\n"
                               + questionModelList.get(position).getOptionA() + "\n"
                               + questionModelList.get(position).getOptionB() + "\n"
                               + questionModelList.get(position).getOptionC() + "\n"
                               + questionModelList.get(position).getOptionD();

                        @Override
                        public void onClick(View view) {
                            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_SUBJECT, "Quizzer Challenge")
                                    .putExtra(Intent.EXTRA_TEXT, sendIntentBody);
                            startActivity(Intent.createChooser(shareIntent, "Share via"));
                        }
                    });
                } else {
                    finish();
                    Toast.makeText(QuestionsActivity.this, "No questions!", Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuestionsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeBookmarks();
    }

    private void setupUI() {
        tvQuestion = findViewById(R.id.tvQuestion);
        tvNumberIndicator = findViewById(R.id.tvNumberIndicator);
        fabBookmark = findViewById(R.id.fabBookmark);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnNext = findViewById(R.id.btnNext);
        btnShare = findViewById(R.id.btnShare);

        questionModelList = new ArrayList<>();
    }

    private void playAnim(final View view, final int value, final String data) {

        for (int i = 0; i < 4; i++) {
            optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
        }

        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(500).setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (value == 0 && count < 4) {
                    String option = "";
                    if (count == 0) {
                        option = questionModelList.get(position).getOptionA();
                    } else if (count == 1) {
                        option = questionModelList.get(position).getOptionB();
                    } else if (count == 2) {
                        option = questionModelList.get(position).getOptionC();
                    } else if (count == 3) {
                        option = questionModelList.get(position).getOptionD();
                    }
                    playAnim(optionsContainer.getChildAt(count), 0, option);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (value == 0) {
                    try {
                        ((TextView)view).setText(data);
                        tvNumberIndicator.setText(position + 1 + "/" + questionModelList.size());

                        if (modelMatch()) {
                            fabBookmark.setImageDrawable(getDrawable(R.drawable.bookmark));
                        } else {
                            fabBookmark.setImageDrawable(getDrawable(R.drawable.bookmark_border));
                        }
                    }
                    catch (ClassCastException e) {
                        ((Button)view).setText(data);
                    }
                    view.setTag(data);
                    playAnim(view, 1, data);
                }
                else {
                    enableOption(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void checkAnswer(Button selectedOption) {
        enableOption(false);
        btnNext.setEnabled(true);
        btnNext.setAlpha(1);
        if (selectedOption.getText().toString().equals(questionModelList.get(position).getCorrectAns())) {
            ///correct
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4caf50")));
            score++;
        } else {
            ///incorrect
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff0000")));

            Button correctOption = optionsContainer.findViewWithTag(questionModelList.get(position).getCorrectAns());
            correctOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4caf50")));
        }
    }

    private void enableOption(boolean enable) {
        for (int i = 0; i < 4; i++) {
            optionsContainer.getChildAt(i).setEnabled(enable);
            if (enable) {
                optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));

            }
        }
    }

    private void getBookmarks() {
        String json = preferences.getString(KEY_NAME, "");
        Type type = new TypeToken<List<QuestionModel>>(){}.getType();
        bookmarkList = gson.fromJson(json, type);

        if (bookmarkList == null) {
            bookmarkList = new ArrayList<>();
        }
    }

    private boolean modelMatch() {
        boolean matched = false;
        int i = 0;

        for (QuestionModel model : bookmarkList) {


            if (model.getQuestion().equals(questionModelList.get(position).getQuestion()) &&
            model.getCorrectAns().equals(questionModelList.get(position).getCorrectAns()) &&
                    model.getSet().equals(questionModelList.get(position).getSet())) {

                matched = true;
                matchedQuestionPosition = i;
            }
            i++;
        }
        return matched;
    }

    private void storeBookmarks() {
        String json = gson.toJson(bookmarkList);

        editor.putString(KEY_NAME, json);
        editor.commit();
    }
}