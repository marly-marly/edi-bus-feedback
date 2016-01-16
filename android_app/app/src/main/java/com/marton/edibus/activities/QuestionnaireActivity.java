package com.marton.edibus.activities;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.inject.Inject;
import com.marton.edibus.R;
import com.marton.edibus.WebCallBack;
import com.marton.edibus.models.Questionnaire;
import com.marton.edibus.network.BusWebClient;
import com.marton.edibus.utilities.QuestionnaireManager;

import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;

public class QuestionnaireActivity extends RoboActionBarActivity {

    private boolean concessionCardValue = false;

    private final String[] genders = {"Male", "Female"};

    private String selectedGender = "Female";

    private final String[] travelReasons = {"Work", "Leisure", "Both"};

    private String selectedTravelReason = "Work";

    @Inject
    BusWebClient busWebClient;

    @InjectView(R.id.age)
    EditText ageEditText;

    @InjectView(R.id.gender_layout)
    LinearLayout genderLayout;

    @InjectView(R.id.gender_text)
    TextView genderTextView;

    @InjectView(R.id.concession_card_switch_layout)
    LinearLayout concessionCardSwitchLayout;

    @InjectView(R.id.concession_card_switch_text)
    TextView concessionCardSwitchTextView;

    @InjectView(R.id.concession_card_switch)
    Switch concessionCardSwitch;

    @InjectView(R.id.travel_reason_layout)
    LinearLayout travelReasonLayout;

    @InjectView(R.id.travel_reason_text)
    TextView travelReasonTextView;

    @InjectView(R.id.save)
    Button saveButton;

    @InjectView(R.id.cancel)
    Button cancelButton;

    @InjectView(R.id.never_ask_again)
    Switch neverAskAgainSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_questionnaire);

        // Create The Toolbar and setting it as the Toolbar for the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        // Set up alert dialogs with simple lists
        final AlertDialog.Builder genderBuilder = new AlertDialog.Builder(this);
        genderBuilder.setTitle("Gender");
        genderBuilder.setItems(this.genders, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedGender = genders[which];
                genderTextView.setText(selectedGender);
            }
        });

        final AlertDialog.Builder travelReasonBuilder = new AlertDialog.Builder(this);
        travelReasonBuilder.setTitle("Reason of Travel");
        travelReasonBuilder.setItems(this.travelReasons, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedTravelReason = travelReasons[which];
                travelReasonTextView.setText(selectedTravelReason);
            }
        });

        // Set up listeners
        this.genderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                genderBuilder.show();
            }
        });

        this.travelReasonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                travelReasonBuilder.show();
            }
        });

        this.concessionCardSwitchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                concessionCardValue = !concessionCardValue;
                concessionCardSwitch.setChecked(concessionCardValue);
                if (concessionCardValue) {
                    concessionCardSwitchTextView.setText("Yes");
                } else {
                    concessionCardSwitchTextView.setText("No");
                }
            }
        });

        this.concessionCardSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                concessionCardValue = concessionCardSwitch.isChecked();
                if (concessionCardValue) {
                    concessionCardSwitchTextView.setText("Yes");
                } else {
                    concessionCardSwitchTextView.setText("No");
                }
            }
        });

        final Activity currentActivity = this;

        this.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                WebCallBack<Boolean> callback = new WebCallBack<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {

                        QuestionnaireManager.writeQuestionnaireFilledInToSharedPreferences(true);
                        currentActivity.finish();
                    }
                };

                saveQuestionnaire(callback);
            }
        });

        this.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean neverAskAgain = neverAskAgainSwitch.isChecked();
                if (neverAskAgain){
                    QuestionnaireManager.writeQuestionnaireFilledInToSharedPreferences(true);
                }
                currentActivity.finish();
            }
        });
    }

    private void saveQuestionnaire(WebCallBack<Boolean> callback){

        String ageEditTextValue = this.ageEditText.getText().toString();
        int age = -1;
        if (!ageEditTextValue.equals("")){
            age = Integer.valueOf(ageEditTextValue);
        }

        Questionnaire questionnaire = new Questionnaire(age, this.selectedGender, this.concessionCardSwitch.isChecked(), selectedTravelReason);

        this.busWebClient.uploadNewQuestionnaire(questionnaire, callback);
    }

    @Override
    public void onStart(){
        super.onStart();

        final ActionBar actionBar = this.getSupportActionBar();

        if (actionBar != null){
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.actionbar_journey, null);
            actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            TextView activityTitleTextView = (TextView) view.findViewById(R.id.activity_title);
            activityTitleTextView.setText("Questionnaire");
        }
    }
}
