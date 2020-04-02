package ch.epfl.sdp.contamination;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.epfl.sdp.Callback;

import static ch.epfl.sdp.TestUtils.buildLocation;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConcreteAnalysisTest {

    Location testLocation = buildLocation(65, 63);
    Date testDate = new Date(System.currentTimeMillis());

    Set<Carrier> peopleAround;

    Map<Long, Carrier> rangePeople;

    @Before
    public void fillCollections() {
        peopleAround = new HashSet<>();
        peopleAround.add(new Layman(HEALTHY));

        rangePeople = new HashMap<>();
        rangePeople.put(1585223363913L, new Layman(Carrier.InfectionStatus.INFECTED));
        rangePeople.put(1585223373883L, new Layman(Carrier.InfectionStatus.IMMUNE));
        rangePeople.put(1585223373893L, new Layman(Carrier.InfectionStatus.UNKNOWN, 0.3f));
        rangePeople.put(1585223373903L, new Layman(HEALTHY));

    }

    DataReceiver mockReceiver = new DataReceiver() {
        @Override
        public void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback) {
            if (location == testLocation && date.equals(testDate)) {
                callback.onCallback(peopleAround);
            }
        }

        @Override
        public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {
            Map<Carrier, Integer> met = new HashMap<>();
            for (long t : rangePeople.keySet()) {
                if (startDate.getTime() <= t && t <= endDate.getTime()) {
                    met.put(rangePeople.get(t), 1);
                }
            }

            callback.onCallback(met);
        }

        @Override
        public Location getMyLocationAtTime(Date date) {
            return null;
        }
    };

    @Test
    public void noEvolutionWithoutInfections() {

        Carrier me = new Layman(HEALTHY);

        assertThat(me.setIllnessProbability(.5f), equalTo(false));

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver);
        analyst.updateInfectionPredictions(testLocation, new Date(1585223373980L));
        assertThat(me.getInfectionStatus(), equalTo(Carrier.InfectionStatus.HEALTHY));

    }

    @Test
    public void becomesUnknownAfterContactWithInfected() {

        Carrier me = new Layman(HEALTHY);

        InfectionAnalyst analyst = new ConcreteAnalysis(me, mockReceiver);
        analyst.updateInfectionPredictions(testLocation, new Date(1585220363913L));
        assertThat(me.getInfectionStatus(), equalTo(Carrier.InfectionStatus.UNKNOWN));
    }
}