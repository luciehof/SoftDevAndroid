package ch.epfl.sdp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.Executor;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView infectionUploadView;
    private User user = new User("Test", User.DEFAULT_FAMILY_NAME, User.DEFAULT_EMAIL,
            User.DEFAULT_URI, User.DEFAULT_PLAYERID, User.DEFAULT_USERID, 25, false);
    private String userName = "Test"; // TODO: this is temporary: we need to get the real current user

    private Executor executor;
    private BiometricPromptWrapper biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionUploadView = findViewById(R.id.infectionStatusUploadConfirmation);

        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

        this.executor = ContextCompat.getMainExecutor(this);
        Intent intent = getIntent();

        if (BiometricUtils.canAuthenticate(this)) {
            if (intent.hasExtra("wrapper")) {
                this.biometricPrompt = (BiometricPromptWrapper) intent.getSerializableExtra("wrapper");
            } else {
                this.biometricPrompt = biometricPromptBuilder(this.executor);;
            }
            this.promptInfo = promptInfoBuilder();
        }
    }

    public void onClick(View view) {
        if (BiometricUtils.canAuthenticate(this)) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            executeHealthStatusChange();
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        outState.putCharSequence("INFECTION_STATUS_TEXT", infectionText.getText());
        outState.putCharSequence("INFECTION_STATUS_BUTTON", infectionButton.getText());
    }

    @Override
    public void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        CharSequence DEFAULT_INFECTION_TEXT = getResources().getString(R.string.your_user_status_is_set_to_not_infected);
        CharSequence DEFAULT_INFECTION_BUTTON = getResources().getString(R.string.i_am_infected);
        infectionText.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_TEXT", DEFAULT_INFECTION_TEXT));
        infectionButton.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_BUTTON", DEFAULT_INFECTION_BUTTON));
    }

    private BiometricPromptWrapper biometricPromptBuilder(Executor executor) {
        return new ConcreteBiometricPromptWrapper(new BiometricPrompt(
                UserInfectionActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                displayNegativeButtonToast(errorCode);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                executeAndDisplayAuthSuccessToast();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                displayAuthFailedToast();
            }
        }));
    }

    private BiometricPrompt.PromptInfo promptInfoBuilder() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setConfirmationRequired(true)
                .setTitle("Biometric authentication")
                .setSubtitle("Confirm your health status change")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void executeHealthStatusChange() {
        CharSequence buttonText = infectionStatusButton.getText();
        if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
            setInfectionColorAndMessage(R.string.i_am_cured,
                    R.string.your_user_status_is_set_to_infected, R.color.colorRedInfected, true);
        } else {
            setInfectionColorAndMessage(R.string.i_am_infected,
                    R.string.your_user_status_is_set_to_not_infected, R.color.colorGreenCured,
                    false);
        }
    }

    private void setInfectionColorAndMessage(int buttonTextID, int messageID, int colorID,
                                             boolean infected){
        clickAction(infectionStatusButton, infectionStatusView, buttonTextID,
                messageID, colorID);
        user.modifyUserInfectionStatus(userName, infected,
                value -> infectionUploadView.setText(updateUploadStatusTextView(value)));
    }

    private String updateUploadStatusTextView(String value){
        return String.format("%s at %s", value, Calendar.getInstance().getTime());
    }

    private void displayAuthFailedToast(){
        Toast.makeText(getApplicationContext(), "Authentication failed",
                Toast.LENGTH_SHORT)
                .show();
    }

    private void displayNegativeButtonToast(int errorCode){
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
            Toast.makeText(getApplicationContext(),
                    "Come back when sure about your health status!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void executeAndDisplayAuthSuccessToast(){
        Toast.makeText(getApplicationContext(),
                "Authentication succeeded!", Toast.LENGTH_SHORT).show();
        executeHealthStatusChange();
    }

    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int buttonColor) {
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }
}
