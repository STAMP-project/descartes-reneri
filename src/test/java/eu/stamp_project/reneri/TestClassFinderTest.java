package eu.stamp_project.reneri;

import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestClassFinderTest {


    private Optional<CtClass<?>> getClassFromResources(String name) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(new ResourceJavaFile(String.format("sources/%s.java", name)));
        CtModel model = launcher.buildModel();
        return model.getElements((CtClass<?> type) -> true).stream().findFirst();
    }

    @Test
    public void testFinJUnit3TestClass() {
        Optional<CtClass<?>> theClass = getClassFromResources("JUnit3TestClass");
        assertTrue(theClass.isPresent());
        assertTrue("It is not a JUnit 3 class as expected", TestClassFinder.isJunit3TestClass(theClass.get()));
    }

    @Test
    public void testFinJUnit4TestClass() {
        Optional<CtClass<?>> theClass = getClassFromResources("JUnit4TestClass");
        assertTrue(theClass.isPresent());
        assertTrue("It is not a JUnit 4 class", TestClassFinder.isJunit4TestClass(theClass.get()));
    }

    @Test
    public void testNotATest() {
        Optional<CtClass<?>> theClass = getClassFromResources("NoTest");
        assertTrue(theClass.isPresent());
        assertFalse("Should not be a test class", TestClassFinder.isTestClass(theClass.get()));
    }

}