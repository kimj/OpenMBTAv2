package com.mentalmachines.ttime.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mentalmachines.ttime.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.objects.Alert;

import java.util.ArrayList;

/**
 * Created by CaptofOuterSpace on 3/20/2016.
 */
public class AlertDetailFragment extends Fragment {
    public static final String TAG = "AlertDetailFragment";
    ArrayList<Alert> mStopAlerts;
    public static String TAG = "AlertDetailFragment";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alert_detail_fragment, container);
        ImageView imageViewSeverityIcon = (ImageView) view.findViewById(R.id.imageViewSeverityIcon);
        TextView textViewEffect = (TextView) view.findViewById(R.id.textViewEffect);
        TextView textViewCause = (TextView) view.findViewById(R.id.textViewCause);
        TextView textViewDescriptionText = (TextView) view.findViewById(R.id.textViewDescriptionText);
        TextView textViewSeverity = (TextView) view.findViewById(R.id.textViewSeverity);
        TextView textViewEffectStart = (TextView) view.findViewById(R.id.textViewEffectStart);
        TextView textViewEffectEnd = (TextView) view.findViewById(R.id.textViewEffectEnd);
        Bundle b = getArguments();
        String alertId = b.getString(DBHelper.KEY_ALERT_ID);
        if (alertId != null) {}

        Alert alert = DBHelper.getAlertById(alertId);
        if (alert != null){
            textViewEffect.setText(alert.effect);
            textViewCause.setText(alert.cause);
            textViewDescriptionText.setText(alert.description_text);
            textViewSeverity.setText(alert.severity);
            textViewEffectStart.setText(alert.effect_start);
            textViewEffectEnd.setText(alert.effect_end);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = new Bundle();
        String alertId = arguments.getString(DBHelper.KEY_ALERT_ID);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
