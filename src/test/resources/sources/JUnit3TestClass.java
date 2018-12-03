package sources;


import junit.framework.TestCase;

public class JUnit3TestClass extends TestCase {

    public void tearUp() {}

    public void tearDown() {}

    public void testSomething() {
        fail();
    }

}