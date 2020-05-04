package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public interface InfectionAnalyst {
    int RADIUS = 3;//maximum radius up to which infection may happen
    int WINDOW_FOR_INFECTION_DETECTION = 1200000; //[ms] Window of time during which a user should not meet a person to much to stay fit. actual : 20

    // MODEL: Being ill with a probability higher than this means becoming marked as INFECTED
    float CERTAINTY_APPROXIMATION_THRESHOLD = 0.9f;

    // MODEL: Being ill with a probability lower that this means becoming marked as HEALTHY
    float ABSENCE_APPROXIMATION_THRESHOLD = 0.1f;

    // MODEL: This parameter models the contagiousness of the disease
    float TRANSMISSION_FACTOR = 0.05f;

    //MODEL: This parameters models how long we are contagious before we remark our illness
    int UNINTENTIONAL_CONTAGION_TIME = 86400000; //[ms] actual : 24 hours

    //MODEL: This parameter models the immunity gain by a person who has been cured against the disease, 0 = 100% immune, 1 = 0% immune
    float IMMUNITY_FACTOR = 0.3f;

    /**
     * Updates the infection probability after staying at 'location' starting from startTime until 'endTime'
     * @param startTime
     * @param endTime
     */
    CompletableFuture<Void> updateInfectionPredictions(Location location, Date startTime, Date endTime);

    /**
     * Returns the instance of the Carrier whose status is modified by the Infection Analyst
     * @return
     */
    Carrier getCarrier();
    /**
     * This will update the carrier status. Gets called by UserInfectionActivity, i.e. when a user discovers his illness,
     * @return
     */
    boolean updateStatus(Carrier.InfectionStatus stat);
}