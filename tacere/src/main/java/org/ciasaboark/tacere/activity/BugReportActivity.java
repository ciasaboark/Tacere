/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.billing.KeySet;
import org.ciasaboark.tacere.versioning.Versioning;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BugReportActivity extends ActionBarActivity {
    private static final String TAG = "BugReportActivity";
    private static final String REPORT_MESSAGE = "reportMessage";
    private static final String REPORT_EMAIL = "reportEmail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            Log.w(TAG, "could not get reference to actionbar, can not hide.");
        } else {
            actionBar.hide();
        }
        setContentView(R.layout.activity_bug_report);

        final Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.report_types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);


        Button closeButton = (Button) findViewById(R.id.bug_report_button_cancel);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final Button sendButton = (Button) findViewById(R.id.bug_report_button_send);
        sendButton.setEnabled(false);
        sendButton.setVisibility(View.INVISIBLE);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButton.setEnabled(false);

                final ProgressBar busySpinner = (ProgressBar) findViewById(R.id.bug_report_progressbar);
                busySpinner.setVisibility(View.VISIBLE);

                EditText messageEditText = (EditText) findViewById(R.id.bug_report_message);
                final String messageText = messageEditText.getText().toString();

                EditText emailEditText = (EditText) findViewById(R.id.bug_report_email);
                String emailText = emailEditText.getText().toString();
                final String emailString = emailText.length() == 0 ? "no email address given" : emailText;

                final String spinnerSelection = spinner.getSelectedItem() == null ?
                        "bug" :
                        spinner.getSelectedItem().toString();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean reportSent = false;
                        GitHubClient client = new GitHubClient();
                        client.setOAuth2Token(KeySet.GITHUB_OAUTH);

                        RepositoryService repositoryService = new RepositoryService(client);
                        try {
                            Repository repository = repositoryService.getRepository("ciasaboark", "Tacere");

                            IssueService issueService = new IssueService();
                            issueService.getClient().setOAuth2Token(KeySet.GITHUB_OAUTH);
                            Issue issue = new Issue();
                            issue.setTitle("Tacere issue submit");


                            String bodyText = "";
                            bodyText += messageText;
                            bodyText += "\n\nEmail : " + emailString;
                            bodyText += "\nAndroid Version: " + Build.VERSION.RELEASE;
                            bodyText += "\nTacere version: " + Versioning.getVersionCode();
                            bodyText += "\nDevice: " + Build.MANUFACTURER + " - " + Build.MODEL;
                            bodyText += "\nRom: " + Build.DISPLAY;

                            issue.setBody(bodyText);

                            Label label = new Label();
                            label.setName("autosubmit");
                            List<Label> labels = new ArrayList<Label>();
                            labels.add(label);

                            String reportTypeLabel;
                            switch (spinnerSelection.toLowerCase()) {
                                case "bug":
                                    reportTypeLabel = "bug";
                                    break;
                                case "wishlist":
                                    reportTypeLabel = "wishlist";
                                    break;
                                default:
                                    Log.w(TAG, "unknown reportType " + spinnerSelection + ", assuming to " +
                                            "be a bug report");
                                    reportTypeLabel = "bug";
                            }

                            Label reportLabel = new Label();
                            reportLabel.setName(reportTypeLabel);
                            labels.add(reportLabel);
                            issue.setLabels(labels);

                            UserService userService = new UserService(client);
                            User user = userService.getUser("ciasaboark");
                            issue.setAssignee(user);

                            try {
                                issueService.createIssue(repository, issue);
                                reportSent = true;
                            } catch (IOException e) {
                                Log.e(TAG, "unable to create issue in repository");
                            }

                        } catch (IOException e) {
                            Log.e(TAG, "unable to get list of user repositories");
                        }


                        if (reportSent) {
                            BugReportActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(BugReportActivity.this, R.string.bug_report_toast_sent, Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            });
                        } else {
                            BugReportActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(BugReportActivity.this, R.string.bug_report_toast_failed, Toast.LENGTH_LONG).show();
                                    busySpinner.setVisibility(View.INVISIBLE);
                                    sendButton.setEnabled(true);
                                    sendButton.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }).start();

            }
        });

        EditText messageEditText = (EditText) findViewById(R.id.bug_report_message);
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //nothing to do here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //nothing to do here
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    sendButton.setVisibility(View.VISIBLE);
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setVisibility(View.INVISIBLE);
                    sendButton.setEnabled(false);
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bug_report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
