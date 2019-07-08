package com.example.traveller.Login;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.transition.TransitionManager;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.basgeekball.awesomevalidation.utility.RegexTemplate;
import com.example.traveller.R;
import com.example.traveller.TokenManager;
import com.example.traveller.Utils;
import com.example.traveller.entities.AccessToken;
import com.example.traveller.entities.ApiError;
import com.example.traveller.network.ApiService;
import com.example.traveller.network.RetrofitBuilder;
import com.example.traveller.timeline.ProfileDrawerImage;
import com.example.traveller.utils.Tools;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginCardOverlap extends AppCompatActivity{

   private View parent_view;
    private static final String TAG = "LoginCardOverlap";

    @BindView(R.id.til_email)
    TextInputLayout tilEmail;
    @BindView(R.id.til_password)
    TextInputLayout tilPassword;
    @BindView(R.id.container)
    RelativeLayout container;
    @BindView(R.id.form_container)
    LinearLayout formContainer;
    @BindView(R.id.loader)
    ProgressBar loader;

    ApiService service;
    TokenManager tokenManager;
    AwesomeValidation validator;
    Call<AccessToken> call;

    FacebookManager facebookManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_card_overlap);
        parent_view = findViewById(android.R.id.content);
        Tools.setSystemBarColor(this);

        ButterKnife.bind(this);

        service = RetrofitBuilder.createService(ApiService.class);
        tokenManager = TokenManager.getInstance(getSharedPreferences("prefs", MODE_PRIVATE));
        validator = new AwesomeValidation(ValidationStyle.TEXT_INPUT_LAYOUT);
        facebookManager = new FacebookManager(service, tokenManager);
        setupRules();

        if(tokenManager.getToken().getAccessToken() != null){
            startActivity(new Intent(LoginCardOverlap.this, ProfileDrawerImage.class));
            finish();
        }

    }

    private void showLoading(){
        TransitionManager.beginDelayedTransition(container);
        formContainer.setVisibility(View.GONE);
        loader.setVisibility(View.VISIBLE);
    }

    private void showForm(){
        TransitionManager.beginDelayedTransition(container);
        formContainer.setVisibility(View.VISIBLE);
        loader.setVisibility(View.GONE);
    }


    //@OnClick(R.id.btn_facebook)
    void loginFacebook(){

        showLoading();
        facebookManager.login(this, new FacebookManager.FacebookLoginListener() {
            @Override
            public void onSuccess() {
                facebookManager.clearSession();
                startActivity(new Intent(LoginCardOverlap.this, ProfileDrawerImage.class));
                finish();
            }

            @Override
            public void onError(String message) {
                showForm();
                Toast.makeText(LoginCardOverlap.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.btn_login)
    void login(){

        String email = tilEmail.getEditText().getText().toString();
        String password = tilPassword.getEditText().getText().toString();

        tilEmail.setError(null);
        tilPassword.setError(null);

        validator.clear();

        if (validator.validate()) {
            showLoading();
            call = service.login(email, password);
            call.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                    Log.w(TAG, "onResponse: " + response);

                    if (response.isSuccessful()) {
                        tokenManager.saveToken(response.body());
                        startActivity(new Intent(LoginCardOverlap.this, ProfileDrawerImage.class));
                        finish();
                    } else {
                        if (response.code() == 422) {
                            handleErrors(response.errorBody());
                        }
                        if (response.code() == 401) {
                            ApiError apiError = Utils.converErrors(response.errorBody());
                            Toast.makeText(LoginCardOverlap.this, apiError.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        showForm();
                    }

                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    Log.w(TAG, "onFailure: " + t.getMessage());
                    showForm();
                }
            });

        }

    }

    @OnClick(R.id.link_signup)
    void goToRegister(){
        startActivity(new Intent(LoginCardOverlap.this, RegisterCardOverlap.class));
    }

    private void handleErrors(ResponseBody response) {

        ApiError apiError = Utils.converErrors(response);

        for (Map.Entry<String, List<String>> error : apiError.getErrors().entrySet()) {
            if (error.getKey().equals("username")) {
                tilEmail.setError(error.getValue().get(0));
            }
            if (error.getKey().equals("password")) {
                tilPassword.setError(error.getValue().get(0));
            }
        }

    }

    public void setupRules() {

        validator.addValidation(this, R.id.til_email, Patterns.EMAIL_ADDRESS, R.string.err_email);
        validator.addValidation(this, R.id.til_password, RegexTemplate.NOT_EMPTY, R.string.err_password);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookManager.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (call != null) {
            call.cancel();
            call = null;
        }
        facebookManager.onDestroy();
    }

}
