package com.example.newsapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity  {

    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private ArrayList<NewsItem> newsList;
    private ProgressBar progressBar;
    private SearchView searchView;
    private Spinner spinnerCategories, spinnerLanguages;
    private ImageButton btnThemeToggle, btnSettings;

    private static final String API_KEY  = "c71f6b20e524848a0e85b9ebbd93ca8d";
    private static final String BASE_URL  = "https://gnews.io/api/v4/top-headlines?token=" + API_KEY;

    private String selectedCategory = "general";
    private String selectedLanguage = "en";
    private boolean isInitialLoad = true;
    private boolean isFetchingNews = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupSearch();
        setupThemeToggle();
        setupSettings();
    }

    private void initViews()  {
        recyclerView  = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        spinnerCategories = findViewById(R.id.spinnerCategories);
        spinnerLanguages = findViewById(R.id.spinnerLanguages);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnSettings = findViewById(R.id.btnSettings);

        newsList = new ArrayList<>();
        newsAdapter  = new NewsAdapter(this, newsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(newsAdapter);
    }

//... Rest of your code ...

private void setupSpinners() {
        List<String> categories = Arrays.asList("General", "Business", "Technology", "Health", "Sports", "Entertainment", "Science");
        List<String> categoryCodes = Arrays.asList("general", "business", "technology", "health", "sports", "entertainment", "science");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategories.setAdapter(catAdapter);

        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newCategory = categoryCodes.get(position);
                if (!selectedCategory.equals(newCategory)) {
                    selectedCategory = newCategory;
                    fetchNews(getNewsUrl());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> languages = Arrays.asList("English", "Spanish", "French", "German", "Chinese", "Hindi");
        List<String> langCodes = Arrays.asList("en", "es", "fr", "de", "zh", "hi");

        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguages.setAdapter(langAdapter);

        spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newLang = langCodes.get(position);
                if (!selectedLanguage.equals(newLang)) {
                    selectedLanguage = newLang;
                    fetchNews(getNewsUrl());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String searchUrl = "https://gnews.io/api/v4/search?q=" + query + "&lang=" + selectedLanguage + "&token=" + API_KEY;
                fetchNews(searchUrl);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void setupThemeToggle() {
        btnThemeToggle.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                btnThemeToggle.setImageResource(R.drawable.ic_moon);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                btnThemeToggle.setImageResource(R.drawable.ic_sun);
            }
        });
    }

    private void setupSettings() {
        btnSettings.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logoutUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        String loginType = preferences.getString("loginType", "");

        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        clearLocalSession();

        if ("google".equals(loginType)) {
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> goToSplash());
        } else {
            goToSplash();
        }
    }

    private void clearLocalSession() {
        DatabaseHelper dbHelper = new DatabaseHelper(this); // Replace with your helper class
        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        db.delete("users", null, null); // Replace "users" with your actual table
        db.close();
    }

    private void goToSplash() {
        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getNewsUrl() {
        return BASE_URL + "&category=" + selectedCategory + "&lang=" + selectedLanguage;
    }

    private void fetchNews(String url) {
        if (isFetchingNews) return;

        isFetchingNews = true;
        progressBar.setVisibility(View.VISIBLE);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    isFetchingNews = false;
                    progressBar.setVisibility(View.GONE);
                    newsList.clear();

                    try {
                        JSONArray articles = response.getJSONArray("articles");
                        for (int i = 0; i < articles.length(); i++) {
                            JSONObject article = articles.getJSONObject(i);
                            String title = article.getString("title");
                            String description = article.getString("description");
                            String imageUrl = article.getString("image");
                            String newsUrl = article.getString("url");
                            newsList.add(new NewsItem(title, description, imageUrl, newsUrl));
                        }
                        newsAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing news", Toast.LENGTH_SHORT).show();
                        Log.e("JSON", "Parsing error: ", e);  // I've added the e parameter to make it more informative in logs
                    }
                },
                error -> {
                    isFetchingNews = false;
                    progressBar.setVisibility(View.GONE);
                    if (newsList.isEmpty()) {
                        Toast.makeText(this, "Failed to load news", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("Volley", "Error: ", error);  // I've added the error parameter to make it more informative in logs
                });

        queue.add(request);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (isInitialLoad) {
            new Handler().postDelayed(() -> fetchNews(getNewsUrl()), 500);
            isInitialLoad = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}
