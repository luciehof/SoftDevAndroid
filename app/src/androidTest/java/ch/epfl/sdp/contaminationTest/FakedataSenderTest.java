package ch.epfl.sdp.contaminationTest;

import android.location.Location;

import org.junit.Test;

import java.util.Calendar;

import ch.epfl.sdp.contamination.FakeDataSender;
import ch.epfl.sdp.contamination.RoundLocation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FakedataSenderTest {
    @Test(expected = Test.None.class)
    public void NoErrorOnCreatingAndInsertingValuesWithProvider(){
        FakeDataSender fDS = new FakeDataSender();
        fDS.sendALocationToFirebase(new RoundLocation("provider"), Calendar.getInstance().getTime());
    }
    @Test(expected = Test.None.class)
    public void NoErrorOnCreatingAndInsertingValuesWithLocation(){
        FakeDataSender fDS = new FakeDataSender();
        Location fakeLoc = new Location("hello");
        fakeLoc.setLatitude(12.124214124);
        fakeLoc.setLongitude(132.124214214);
        fDS.sendALocationToFirebase(new RoundLocation(fakeLoc), Calendar.getInstance().getTime());
    }
    @Test
    public void addingAnElementDoesAddAnElementToFirebase(){
        FakeDataSender fDS = new FakeDataSender();
        Location fakeLoc = new Location("hello");
        fakeLoc.setLatitude(12.124214124);
        fakeLoc.setLongitude(132.124214214);
        assertTrue(fDS.getMap().isEmpty());
        fDS.sendALocationToFirebase(new RoundLocation(fakeLoc), Calendar.getInstance().getTime());
        assertFalse(fDS.getMap().isEmpty());
    }
}
