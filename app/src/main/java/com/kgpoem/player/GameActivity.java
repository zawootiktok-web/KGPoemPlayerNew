package com.kgpoem.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public final class GameActivity extends AppCompatActivity {

    private static final class QuizItem {
        final String question;
        final String[] options;
        final int correctIndex;

        QuizItem(String question, String[] options, int correctIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    private final List<QuizItem> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int points = 0;

    private TextView questionView;
    private TextView scoreView;
    private Button choice1;
    private Button choice2;
    private Button choice3;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        questionView = findViewById(R.id.question);
        scoreView = findViewById(R.id.score);
        choice1 = findViewById(R.id.choice1);
        choice2 = findViewById(R.id.choice2);
        choice3 = findViewById(R.id.choice3);

        findViewById(R.id.backGame).setOnClickListener(view -> finish());

        initQuestions();
        displayCurrentQuestion();

        View.OnClickListener choiceListener = view -> {
            Button btn = (Button) view;
            checkAnswer(btn);
        };
        choice1.setOnClickListener(choiceListener);
        choice2.setOnClickListener(choiceListener);
        choice3.setOnClickListener(choiceListener);
    }

    private void initQuestions() {
        questions.add(new QuizItem("ကြောင်လေးက ဘယ်ကောင်လဲ?", new String[]{"ကြောင်", "ခွေး", "ယုန်"}, 0));
        questions.add(new QuizItem("မနက်စောစော တွန်တဲ့ အကောင်က ဘာလဲ?", new String[]{"ဘဲ", "ကြက်ဖ", "ကျီးကန်း"}, 1));
        questions.add(new QuizItem("နှာမောင်းရှည်ရှည်နဲ့ အကောင်ကြီးက ဘာလဲ?", new String[]{"ကျား", "ခြင်္သေ့", "ဆင်"}, 2));
        questions.add(new QuizItem("ရေထဲမှာ ကူးခတ်နေတဲ့ သတ္တဝါလေးက ဘာလဲ?", new String[]{"ငါး", "ငှက်", "မျောက်"}, 0));
        questions.add(new QuizItem("သစ်ပင်ပေါ် ခုန်ပေါက်နေတဲ့ အကောင်လေးက ဘာလဲ?", new String[]{"လိပ်", "မျောက်", "ဆင်"}, 1));
    }

    private void displayCurrentQuestion() {
        if (questions.isEmpty()) return;
        QuizItem item = questions.get(currentQuestionIndex);
        questionView.setText(item.question);

        choice1.setText(item.options[0]);
        choice1.setTag(item.options[0]);

        choice2.setText(item.options[1]);
        choice2.setTag(item.options[1]);

        choice3.setText(item.options[2]);
        choice3.setTag(item.options[2]);
    }

    private void checkAnswer(Button selectedButton) {
        QuizItem item = questions.get(currentQuestionIndex);
        String selectedAnswer = selectedButton.getTag().toString();
        String correctAnswer = item.options[item.correctIndex];

        if (correctAnswer.equals(selectedAnswer)) {
            points += 10;
            scoreView.setText(getString(R.string.score_format, points));
            Toast.makeText(this, "⭐ သိပ်တော်တယ်! မှန်ပါတယ်!", Toast.LENGTH_SHORT).show();

            // Next question after a short delay
            handler.postDelayed(() -> {
                currentQuestionIndex = (currentQuestionIndex + 1) % questions.size();
                displayCurrentQuestion();
            }, 600L);
        } else {
            Toast.makeText(this, "ထပ်မံကြိုးစားကြည့်ပါဦးနော် ✨", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
