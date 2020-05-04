package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import java.util.Date;

import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

import static android.content.Context.BIND_AUTO_CREATE;

public class InfectionFragment extends Fragment implements View.OnClickListener {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;
    private long lastUpdateTime;

    private LocationService service;

    private Handler uiHandler;

    InfectionFragment(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_infection, container, false);

        infectionStatus = view.findViewById(R.id.my_infection_status);
        infectionProbability = view.findViewById(R.id.my_infection_probability);
        view.findViewById(R.id.my_infection_refresh).setOnClickListener(this);

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText("Refresh to see your status");

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InfectionFragment.this.service = ((LocationService.LocationBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        getActivity().bindService(new Intent(getActivity(), LocationService.class), conn, BIND_AUTO_CREATE);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.my_infection_refresh: {
                onModelRefresh(view);
            } break;
        }
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();

        // TODO: Which location?
        service.getReceiver().getMyLastLocation(AccountFragment.getAccount(getActivity()))
                .thenApply(location -> service.getAnalyst().updateInfectionPredictions(location, refreshTime, new Date())
                        .thenAccept(n -> {
                            infectionStatus.setText(R.string.infection_status_posted);
                                uiHandler.post(() -> {
                                    infectionStatus.setText(service.getAnalyst().getCarrier().getInfectionStatus().toString());
                                    infectionProbability.setProgress(Math.round(service.getAnalyst().getCarrier().getIllnessProbability() * 100));
                                    Log.e("PROB:", service.getAnalyst().getCarrier().getIllnessProbability() + "");
                                });
                        }))
                .join();
    }

    @VisibleForTesting
    public LocationService getLocationService() {
        return service;
    }

}
