package com.homework.fypmaza;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.content.Intent;

import com.homework.fypmaza.lib.Faqpage;


public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
// Find the button by ID and set a click listener to navigate to the next page
        Button nextPageButton = findViewById(R.id.btnNextPage);
        nextPageButton.setOnClickListener(v -> navigateToNextPage());

        // New code to navigate to FAQ page
        Button faqPageButton = findViewById(R.id.btnNextPage2);
        faqPageButton.setOnClickListener(v -> navigateToFaqPage());

    }

    private void navigateToNextPage() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
    }

    // New method to navigate to FAQ page
    private void navigateToFaqPage() {
        Intent intent = new Intent(this, Faqpage.class);
        startActivity(intent);
    }
}