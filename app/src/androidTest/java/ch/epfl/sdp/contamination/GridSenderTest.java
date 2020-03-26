package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.QueryHandler;
import ch.epfl.sdp.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestUtils.buildLocation;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

public class GridSenderTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot queryDocumentSnapshot;

    @Mock
    private QuerySnapshot firstPeriodSnapshot;

    @Mock
    private QuerySnapshot secondPeriodSnapshot;

    @Mock
    private QueryDocumentSnapshot firstPeriodDocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot secondPeriodDocumentSnapshot;

    @Mock
    private QuerySnapshot timesListSnapshot;

    @Mock
    private QueryDocumentSnapshot range1DocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot range2DocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot afterRangeDocumentSnapshot;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    final long rangeStart = 1585223373883L;
    final long rangeEnd = 1585223373963L;
    final long outsideRange = 1585223373983L;

    @Before
    public void setupMockito() {
        when(querySnapshot.iterator()).thenReturn(Collections.singletonList(queryDocumentSnapshot).iterator());
        when(queryDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.HEALTHY.toString());
        when(queryDocumentSnapshot.get("illnessProbability")).thenReturn(0.5d);

        when(firstPeriodSnapshot.iterator()).thenReturn(Collections.singletonList(firstPeriodDocumentSnapshot).iterator());
        when(secondPeriodSnapshot.iterator()).thenReturn(Collections.singletonList(secondPeriodDocumentSnapshot).iterator());
        when(firstPeriodDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.IMMUNE.toString());
        when(firstPeriodDocumentSnapshot.get("illnessProbability")).thenReturn(0.0d);
        when(secondPeriodDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.UNKNOWN.toString());
        when(secondPeriodDocumentSnapshot.get("illnessProbability")).thenReturn(0.75d);

        when(timesListSnapshot.iterator()).thenReturn(Arrays.asList(range1DocumentSnapshot, range2DocumentSnapshot, afterRangeDocumentSnapshot).iterator());
        when(range1DocumentSnapshot.get("Time")).thenReturn(String.valueOf(rangeStart));
        when(range2DocumentSnapshot.get("Time")).thenReturn(String.valueOf(rangeEnd));
        when(afterRangeDocumentSnapshot.get("Time")).thenReturn(String.valueOf(outsideRange));
    }

    class MockGridInteractor extends GridFirestoreInteractor {

        // TODO: GridFirestoreInteractor should become an interface too
        MockGridInteractor() {
            super(null);
        }
    }

    private void setSuccessfulSender() {
        ((DataExchangeActivity.ConcreteDataSender) mActivityRule.getActivity().getSender()).setInteractor(new MockGridInteractor() {
            @Override
            public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
                success.onSuccess(null);
            }
        });
    }

    @Test
    public void dataSenderUploadsInformation() {
        setSuccessfulSender();

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().getSender().registerLocation(
                new Layman(Carrier.InfectionStatus.HEALTHY),
                buildLocation(10, 20),
                new Date(System.currentTimeMillis())));

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));
    }

    private void setFailingSender() {
        ((DataExchangeActivity.ConcreteDataSender) mActivityRule.getActivity().getSender()).setInteractor(new MockGridInteractor() {
            @Override
            public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
                failure.onFailure(null);
            }
        });
    }

    @Test
    public void dataSenderFailsWithError() {
        setFailingSender();

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().getSender().registerLocation(
                new Layman(Carrier.InfectionStatus.HEALTHY),
                buildLocation(10, 20),
                new Date(System.currentTimeMillis())));

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Failed")));
    }

    @Test
    public void dataReceiverFindsContacts() {
        ((DataExchangeActivity.ConcreteDataReceiver) mActivityRule.getActivity().getReceiver()).setInteractor(new MockGridInteractor() {
            @Override
            public void read(Location location, long time, QueryHandler handler) {
                handler.onSuccess(querySnapshot);
            }
        });

        Callback<Set<? extends Carrier>> successCallback = value -> {
            assertThat(value.size(), is(1));
            assertThat(value.iterator().hasNext(), is(true));
            assertThat(value.iterator().next().getIllnessProbability(), is(0.0f));
        };

        mActivityRule.getActivity().getReceiver().getUserNearby(
                buildLocation(10, 20),
                new Date(1585223373883L),
                successCallback);
    }

    private void setFakeReceiver(Location testLocation) {
        ((DataExchangeActivity.ConcreteDataReceiver) mActivityRule.getActivity().getReceiver()).setInteractor(new MockGridInteractor() {

            @Override
            public void getTimes(Location location, QueryHandler handler) {
                if (location != testLocation) {
                    handler.onFailure();
                } else {
                    handler.onSuccess(timesListSnapshot);
                }
            }

            @Override
            public void read(Location location, long time, QueryHandler handler) {
                if (location != testLocation) {
                    handler.onFailure();
                } else {
                    if (time == rangeStart) {
                        handler.onSuccess(firstPeriodSnapshot);
                    } else if (time == rangeEnd) {
                        handler.onSuccess(secondPeriodSnapshot);
                    }
                }
            }
        });
    }

    @Test
    public void dataReceiverFindsContactsDuring() {

        final Location testLocation = buildLocation(70.5, 71.25);

        setFakeReceiver(testLocation);

        Callback<Map<? extends Carrier, Integer>> callback = value -> {
            assertThat(value.size(), is(2));
            assertThat(value.keySet().contains(new Layman(Carrier.InfectionStatus.IMMUNE, 0f)), is(true));
            assertThat(value.containsKey(new Layman(Carrier.InfectionStatus.UNKNOWN)), is(false));
            assertThat(value.get(new Layman(Carrier.InfectionStatus.UNKNOWN, 0.75f)), is(1));
        };

        mActivityRule.getActivity().getReceiver().getUserNearbyDuring(
                testLocation,
                new Date(rangeStart),
                new Date(rangeEnd),
                callback);
    }

    @Test
    public void laymanEqualityTest() {
        // This test ensures that Layman properly overrides equals and hashCode methods

        Carrier c1 = new Layman(Carrier.InfectionStatus.INFECTED);
        Carrier c2 = new Layman(Carrier.InfectionStatus.INFECTED, 1f);

        Map<Carrier, Integer> aMap = new HashMap<>();
        aMap.put(c1, 2);

        assertThat(aMap.containsKey(c2), is(true));
    }

    private Map<Carrier, Boolean> getBackSliceData(Location somewhere, Date rightNow) throws Throwable {
        Map<Carrier, Boolean> result = new ConcurrentHashMap<>();

        AtomicBoolean done = new AtomicBoolean();
        done.set(false);

        mActivityRule.runOnUiThread(() -> mActivityRule.getActivity().getReceiver().getUserNearby(somewhere, rightNow, people -> {
            people.forEach(x -> result.put(x, false));
            done.set(true);
        }));

        while (!done.get()) { } // Busy wait

        return result;
    }

    @Test
    public void dataReallyComeAndGoFromServer() throws Throwable {
        // The following test uses the actual Firestore

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);
        Location somewhereInTheWorld = buildLocation(12, 73);
        Date rightNow = new Date(System.currentTimeMillis());

        mActivityRule.getActivity().getSender().registerLocation(aFakeCarrier, somewhereInTheWorld, rightNow);

        Thread.sleep(10000);

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));

        Map<Carrier, Boolean> result = getBackSliceData(somewhereInTheWorld, rightNow);

        assertThat(result.size(), is(1));
        assertThat(result.containsKey(aFakeCarrier), is(true));
    }

    private Map<Carrier, Integer> getBackRangeData(Location somewhere, Date rangeStart, Date rangeEnd) throws Throwable {
        AtomicBoolean done = new AtomicBoolean();
        done.set(false);

        Map<Carrier, Integer> result = new ConcurrentHashMap<>();

        // Get data back
        mActivityRule.runOnUiThread(() -> mActivityRule.getActivity().getReceiver().getUserNearbyDuring(somewhere, rangeStart, rangeEnd, contactFrequency -> {
            result.putAll(contactFrequency);
            done.set(true);
        }));

        while (!done.get()) { } // Busy wait

        return result;
    }

    @Test
    public void complexQueriesComeAndGoFromServer() throws Throwable {
        // The following test uses the actual Firestore

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);
        Carrier trulyHealthy = new Layman(Carrier.InfectionStatus.IMMUNE, 0f);

        Location somewhereInTheWorld = buildLocation(12, 73);

        Date rightNow = new Date(System.currentTimeMillis());
        Date aLittleLater = new Date(rightNow.getTime() + 10);

        mActivityRule.getActivity().getSender().registerLocation(aFakeCarrier, somewhereInTheWorld, rightNow);
        mActivityRule.getActivity().getSender().registerLocation(trulyHealthy, somewhereInTheWorld, aLittleLater);

        Thread.sleep(10000);

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));

        Map<Carrier, Integer> result = getBackRangeData(somewhereInTheWorld, rightNow, aLittleLater);

        assertThat(result.size(), is(2));
        assertThat(result.containsKey(aFakeCarrier), is(true));
        assertThat(result.containsKey(trulyHealthy), is(true));
        assertThat(result.get(aFakeCarrier), is(1));
        assertThat(result.get(trulyHealthy), is(1));

    }

    @Test
    public void repetitionsOfSameCarrierAreDetected() throws Throwable {
        // The following test uses the actual Firestore

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);

        Location somewhereInTheWorld = buildLocation(12, 73);

        Date rightNow = new Date(System.currentTimeMillis());
        Date aLittleLater = new Date(rightNow.getTime() + 10);

        mActivityRule.getActivity().getSender().registerLocation(aFakeCarrier, somewhereInTheWorld, rightNow);
        mActivityRule.getActivity().getSender().registerLocation(aFakeCarrier, somewhereInTheWorld, aLittleLater);

        Thread.sleep(10000);

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));

        Map<Carrier, Integer> result = getBackRangeData(somewhereInTheWorld, rightNow, aLittleLater);

        assertThat(result.size(), is(1));
        assertThat(result.containsKey(aFakeCarrier), is(true));
        assertThat(result.get(aFakeCarrier), is(2));

    }
}
