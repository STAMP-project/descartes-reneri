package eu.stamp_project.reneri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static eu.stamp_project.reneri.testutils.SameSetMatcher.sameSetAs;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TestCaseToClassNameTest {

    @Parameter
    public String testCaseName;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {"path.to.Class.testCase(path.to.Class)"},
                        {"path.to.Class.path.to.Class"},
                        {"path.to.Class.testCase[params](path.to.Class)"}
                }
        );

    }

    @Test()
    public void testGuessTestClass() {
        assertThat( MutationInfo.guessTestClasses(Collections.singleton(testCaseName)), sameSetAs("path.to.Class"));

    }


}