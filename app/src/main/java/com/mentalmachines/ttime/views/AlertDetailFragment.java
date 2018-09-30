package com.mentalmachines.ttime.views;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mentalmachines.ttime.data.DBHelper;
import com.mentalmachines.ttime.R;
import com.mentalmachines.ttime.data.model.Alert;

/**
 * Created by CaptofOuterSpace on 3/20/2016.
 */
public class AlertDetailFragment extends Fragment {
    public static final String TAG = "AlertDetailFragment";

    public AlertDetailFragment() { } //req'd empty constructor

    public static AlertDetailFragment newInstance(String AlertID) {
        Bundle args = new Bundle();
        args.putString(DBHelper.KEY_ALERT_ID, AlertID);
        AlertDetailFragment fragment = new AlertDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.alert_detail_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle b = getArguments();
        String alertId = b.getString(DBHelper.KEY_ALERT_ID);
        if (TextUtils.isEmpty(alertId)) {
            Toast.makeText(getContext(), "Error finding that alert", Toast.LENGTH_SHORT).show();
            getActivity().onBackPressed();
            return;
        }

        Alert alert = DBHelper.getAlertById(alertId);
        showAlert(alert);
    }

    public void showAlert(Alert alert) {
        final View view = getView();
        //ImageView imageViewSeverityIcon = (ImageView) view.findViewById(R.id.imageViewSeverityIcon);
        ((TextView) view.findViewById(R.id.textViewEffect)).setText(alert.effect);
        ((TextView) view.findViewById(R.id.textViewCause)).setText(alert.cause);
        ((TextView) view.findViewById(R.id.textViewDescriptionText)).setText(alert.description_text);
        ((TextView) view.findViewById(R.id.textViewSeverity)).setText(alert.severity);
        ((TextView) view.findViewById(R.id.textViewEffectStart)).setText(alert.effect_start);
        ((TextView) view.findViewById(R.id.textViewEffectEnd)).setText(alert.effect_end);
    }

}
