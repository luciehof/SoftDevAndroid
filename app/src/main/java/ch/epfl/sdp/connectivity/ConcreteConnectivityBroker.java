package ch.epfl.sdp.connectivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Observable;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.GPS;
import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.INTERNET;

/**
 * Observable implementation of ConnectivityBroker
 * It notifies Observers when Internet becomes available/unavailable
 */
public class ConcreteConnectivityBroker extends Observable implements ConnectivityBroker {

    private boolean hasInternetConnection = false;

    private LocationManager locationManager;
    private Context context;

    private ConnectivityManager.NetworkCallback internetNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                hasInternetConnection = true;

                notifyObservers(INTERNET);
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            hasInternetConnection = false;

            notifyObservers(INTERNET);
        }
    };

    public ConcreteConnectivityBroker(LocationManager locationManager, Context context) {
        this.locationManager = locationManager;
        this.context = context;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(internetNetworkCallback);
        }
    }

    @Override
    public boolean isProviderEnabled(@NonNull Provider provider) {
        assert provider != null;
        switch (provider) {
            case GPS:
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            case INTERNET:
                return hasInternetConnection;
            default:
                throw new IllegalArgumentException("Invalid provider");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean requestLocationUpdates(@NonNull Provider provider, long minTimeDelay, float minSpaceDist, LocationListener listener) {
        assert provider != null;
        if (provider != GPS) {
            throw new IllegalArgumentException(String.format("Provider %s is not yet supported", provider));
        }

        if (hasPermissions(GPS)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeDelay, minSpaceDist, listener);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeUpdates(LocationListener listener) {
        locationManager.removeUpdates(listener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastKnownLocation(@NonNull Provider provider) {
        assert provider != null;
        if (provider == GPS && hasPermissions(provider)) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }

    @Override
    public boolean hasPermissions(Provider provider) {
        switch (provider) {
            case GPS:
                return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            case INTERNET:
                // Always has Internet connection
                return true;
            default:
                throw new IllegalArgumentException("Invalid provider: " + provider);
        }

    }

    @Override
    public void requestPermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }
}
