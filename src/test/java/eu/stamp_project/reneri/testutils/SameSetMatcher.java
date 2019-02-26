package eu.stamp_project.reneri.testutils;

import org.hamcrest.Description;

import org.hamcrest.TypeSafeMatcher;

import java.util.*;
import java.util.stream.Collector;

public class SameSetMatcher<T> extends TypeSafeMatcher<Set<T>> {

    private Set<T> set;

    public SameSetMatcher(Set<T> set) {
        Objects.requireNonNull(set, "Provided set can not be null");
        this.set = set;
    }

    private boolean hasRequiredSize(Set<T> item) {
        return item.size() == set.size();
    }

    private boolean containsRequiredElements(Set<T> item) {
        return item.containsAll(set);
    }

    private Set<T> unexpectedElements(Set<T> item) {
        HashSet<T> diff = new HashSet<>(item);
        diff.removeAll(set);
        return diff;
    }

    @Override
    protected boolean matchesSafely(Set<T> item) {
        return hasRequiredSize(item) && containsRequiredElements(item);
    }

    @Override
    protected void describeMismatchSafely(Set<T> item, Description mismatchDescription) {
        if(!hasRequiredSize(item)) {
            if(item.isEmpty()) {
                mismatchDescription.appendText("an empty set was given");
            }
            else {
                mismatchDescription.appendText("a set of size ").appendValue(item.size()).appendText(" was given");
            }
            return;
        }
        Set<T> diff = unexpectedElements(item);
        if(diff.isEmpty()) {
            return; // Should not happen
        }
        mismatchDescription.appendValueList("got a set with unexpected values ", ", ", "", diff);
    }

    @Override
    public void describeTo(Description description) {
        if(set.isEmpty()) {
            description.appendText("an empty set");
        }
        else {
            description.appendValueList("same set as {", ", ", "} ", set);
        }
    }

    public static <T> SameSetMatcher<T> sameSetAs(T... items) {
        return new SameSetMatcher<T>(new HashSet<T>(Arrays.asList(items)));
    }

    public static <T> SameSetMatcher<T> emptySet() {
        return new SameSetMatcher<>(Collections.emptySet());
    }

}
