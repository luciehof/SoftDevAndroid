package ch.epfl.sdp;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;

import com.azimolabs.conditionwatcher.ConditionWatcher;
import com.azimolabs.conditionwatcher.Instruction;
import com.mapbox.mapboxsdk.maps.MapView;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;


//@RunWith(AndroidJUnit4.class)
public class TabActivityTest {


    @Rule
    public final ActivityTestRule<TabActivity> mActivityRule =
            new ActivityTestRule<>(TabActivity.class);

    @Test
    @Ignore
    public void testMapFragment() throws Exception {

        class LoadingDialogInstruction extends Instruction {

            private Boolean loaded = false;

            @Override
            public String getDescription() {
                return "wait for map to load";
            }

            @Override
            public boolean checkCondition() {

                // Injecting the Instrumentation instance is required
                // for your test to run with AndroidJUnitRunner.

                //System.out.println("dldldldl");
                MapFragment fragment = (MapFragment) mActivityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.mapView);
                if (fragment != null) {
                    //System.out.println("Run you");

                    fragment.OnDidFinishLoadingMapListener(new bidule());
                }
                return loaded;


            }

            class bidule implements MapView.OnDidFinishLoadingMapListener {

                @Override
                public void onDidFinishLoadingMap() {
                    loaded = true;
                }
            }
        }

        System.out.println("hello");
        //onView(withId(R.id.userIDText)).perform(typeText("from my unit test")).perform(closeSoftKeyboard());
        ConditionWatcher.waitForCondition(new LoadingDialogInstruction());
        // onView(withId(R.id.greetingMessage)).check(matches(withText("Hello from my unit test!")));
    }

    @Test
    public void testTabsInterfaceCorrectly() {
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabsDisplayCorrectly() {
        onView(withId(R.id.mapView)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabsMoveCorrectly() {
        onView(withId(R.id.history_tracker)).check(matches(not(hasFocus())));
        onView(withText(mActivityRule.getActivity().getString(R.string.tab_history))).perform(click());
        onView(withId(R.id.history_tracker)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

}