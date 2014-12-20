/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.bug.CrashReportManager;

public class TutorialCrashReporterFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_crash_reporter;
    private ViewGroup rootView;
//    private String sampleCrashReport = "{\n" +
//            "  \"id\": \"dce8fa90-7904-4cfa-870f-bcdc80738ea6\",\n" +
//            "  \"key\": \"2014-10-26T00:52:48.000Z\",\n" +
//            "  \"value\": {\n" +
//            "    \"user_crash_date\": \"2014-10-25T20:52:48.000-04:00\",\n" +
//            "    \"android_version\": \"5.0\",\n" +
//            "    \"application_version_name\": \"2.1.0 - beta 6 alpha 1 - debug build 10/25/14\",\n" +
//            "    \"signature\": {\n" +
//            "      \"full\": \"java.lang.RuntimeException: Unable to start activity ComponentInfo{org.ciasaboark.tacere/org.ciasaboark.tacere.activity.MainActivity}: java.lang.IllegalArgumentException: source object cannot be null at org.ciasaboark.tacere.database.DataSetManager.<init>(DataSetManager.java:31)\",\n" +
//            "      \"digest\": \"java.lang.RuntimeException: Unable to start activity ComponentInfo{org.ciasaboark.tacere/org.ciasaboark.tacere.activity.MainActivity}: java.lang.IllegalArgumentException: source object cannot be null : \\tat org.ciasaboark.tacere.database.DataSetManager.<init>(DataSetManager.java:31)\",\n" +
//            "      \"rootCause\": \"java.lang.IllegalArgumentException: source object cannot be null at org.ciasaboark.tacere.activity.MainActivity.onCreate(MainActivity.java:78)\"\n" +
//            "    },\n" +
//            "    \"device\": \"asus google Nexus 7\"\n" +
//            "  }\n" +
//            "}";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        TextView sampleReport = (TextView) rootView.findViewById(R.id.tutorial_report_sample_button);
        sampleReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault_Dialog));
                builder.setTitle(R.string.tutorial_crash_report_sample_title).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //nothing to do
                    }
                });
                WebView webView = new WebView(getActivity());
                webView.loadUrl("file:///android_asset/sample_bug_report.html");
                builder.setView(webView);
                builder.show();
            }
        });

        final CheckBox sendReportsCheckbox = (CheckBox) rootView.findViewById(R.id.tutorial_crash_report_checkbox);
        final CrashReportManager crashReportManager = new CrashReportManager(getActivity());

        sendReportsCheckbox.setChecked(crashReportManager.isReportsEnabled());
        sendReportsCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean willSendReports = sendReportsCheckbox.isChecked();
                crashReportManager.setReportsEnabled(willSendReports);
            }
        });
        return rootView;
    }
}