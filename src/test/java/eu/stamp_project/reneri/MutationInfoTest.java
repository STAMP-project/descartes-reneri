package eu.stamp_project.reneri;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.*;

public class MutationInfoTest {


    public String getClassName(String testName) throws Exception {
        Matcher matcher = MutationInfo.getMatcher(testName);
        if(!matcher.matches()) {
            throw new Exception("Should have matched " + testName);
        }
        return matcher.group("class");
    }

    @Test()
    public void testMatchClassOnlyTestName() throws Exception {
        assertEquals("path.to.Class", getClassName("path.to.Class.path.to.Class"));
    }

    @Test
    public void testMatchMethodTestName() throws Exception {
        assertEquals("path.to.Class", getClassName("path.to.Class.method(path.to.Class)"));
    }

    @Test
    public void testMatchMethodTestNameParameterless() throws Exception {
        assertEquals("path.to.Class", getClassName("path.to.Class.method()"));
    }

    @Test
    public void testMatchMethodWithParametersTestName() throws Exception {
        assertEquals("path.to.Class", getClassName("path.to.Class.method[1](path.to.Class)"));
    }

    @Test
    public void testMatchSimpleMethodNameTestName() throws Exception {
        assertEquals("path.to.Class", getClassName("path.to.Class.method"));
    }


}