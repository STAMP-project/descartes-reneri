package eu.stamp_project.reneri;

import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestClassFinderTest {
    @Test
    public void testFinJUnit3TestClass() {
        CtClass<?> theClass = Launcher.parseClass(
                "package sources;\n" +
                        "\n" +
                        "\n" +
                        "import junit.framework.TestCase;\n" +
                        "\n" +
                        "public class JUnit3TestClass extends TestCase {\n" +
                        "\n" +
                        "    public void tearUp() {}\n" +
                        "\n" +
                        "    public void tearDown() {}\n" +
                        "\n" +
                        "    public void testSomething() {\n" +
                        "        fail();\n" +
                        "    }\n" +
                        "\n" +
                        "}"
        );
        assertTrue("It is not a JUnit 3 class as expected", TestClassFinder.isJunit3TestClass(theClass));
    }

    @Test
    public void testFinJUnit4TestClass() {
        CtClass<?> theClass = Launcher.parseClass(
                "package sources;\n" +
                        "\n" +
                        "import org.junit.Test\n" +
                        "\n" +
                        "public class JUnit4TestClass {\n" +
                        "\n" +
                        "    @Test\n" +
                        "    public void test() {\n" +
                        "\n" +
                        "    }\n" +
                        "\n" +
                        "}"
        );
        assertTrue("It is not a JUnit 4 class", TestClassFinder.isJunit4TestClass(theClass));
    }

    @Test
    public void testNotATest() {

        CtClass<?> theClass = Launcher.parseClass(
                "package sources;\n" +
                        "\n" +
                        "public class NoTest {\n" +
                        "\n" +
                        "    public boolean isTest() {\n" +
                        "        return false;\n" +
                        "    }\n" +
                        "\n" +
                        "}"
        );
        assertFalse("Should not be a test class", TestClassFinder.isTestClass(theClass));
    }

    @Test
    public void testMixedCaseWithStatic() {

        CtClass<?> testClass = Launcher.parseClass(
                "package source;\n" +
                        "import junit.framework.TestCase;\n" +
                        "import org.junit.Test;\n\n" +
                        "@SupressWarnings(\"javadoc\")"+
                        "public final class ExampleTest extends TestCase {\n" +
                            "@Test\n" +
                            "public static void testExample() throws Exception { fail(); }\n" +
                        "}");
        assertTrue("Should be a test class anyways", TestClassFinder.isTestClass(testClass));


    }


}