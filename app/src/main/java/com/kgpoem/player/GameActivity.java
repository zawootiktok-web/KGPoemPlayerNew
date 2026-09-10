package com.kgpoem.player;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public final class GameActivity extends AppCompatActivity {
    private static final String CORRECT_ANSWER = "ကြောင်";

    private TextView question;
    private TextView score;
    private int points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        question = findViewById(R.id.question);
        score = findViewById(R.id.score);
        findViewById(R.id.backGame).setOnClickListener(view -> finish());

        View.OnClickListener choiceListener = view -> checkAnswer((Button) view);
        findViewById(R.id.choice1).setOnClickListener(choiceListener);
        findViewById(R.id.choice2).setOnClickListener(choiceListener);
        findViewById(R.id.choice3).setOnClickListener(choiceListener);
    }

    private void checkAnswer(Button selectedButton) {
        String answer = selectedButton.getTag().toString();
        if (CORRECT_ANSWER.equals(answer)) {
            points += 10;
            question.setText(R.string.correct_answer);
        } else {
            question.setText(R.string.try_again);
        }
        score.setText(getString(R.string.score_format, points));
    }
}
