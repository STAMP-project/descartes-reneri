package eu.stamp_project.reneri.codeanalysis;

import eu.stamp_project.reneri.utils.FileUtils;
import javassist.*;
import javassist.bytecode.*;
import org.junit.Test;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.tree.MethodInsnNode;
import spoon.reflect.visitor.TokenWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

import static eu.stamp_project.reneri.testutils.SameSetMatcher.sameSetAs;

public class EntryPointFinderTest {

    private ClassPool getPoolFor(Class<?> theClass) {
        ClassPool pool = new ClassPool();
        pool.appendClassPath(new ClassClassPath(theClass));
        return pool;
    }

    static class SimpleAccessor {
        private int x = 3;
        public int m() { return x; }
    }

    @Test
    public void testSimpleAccessor() throws Exception {
        ClassPool pool = getPoolFor(SimpleAccessor.class);
        EntryPointFinder finder = new EntryPointFinder(pool);
        Set<CtMethod> accessors = finder.findAccessors("x", SimpleAccessor.class.getTypeName());
        assertThat(accessors, sameSetAs(pool.getMethod(SimpleAccessor.class.getTypeName(), "m")));
    }


    static class TwoAccessors {

        private int x;

        public void m() { getX(); }

        public void n() { getX(); }

        private int getX() { return 1 + x; }

    }

    @Test
    public void testTwoAccessors () throws Exception {
        ClassPool pool = getPoolFor(TwoAccessors.class);
        EntryPointFinder finder = new EntryPointFinder(pool);
        String className = TwoAccessors.class.getTypeName();
        assertThat(
                finder.findAccessors("x", className),
                sameSetAs(
                        pool.getMethod(className, "n"),
                        pool.getMethod(className, "m")
                ));
    }

    static class DirectAccess { public int x; }

    @Test
    public void testDirectAccess() throws Exception {
        ClassPool pool = getPoolFor(DirectAccess.class);
        EntryPointFinder finder = new EntryPointFinder(pool);
        assertNull(finder.findAccessors("x", DirectAccess.class.getTypeName()));
    }

    static class NoAccessor {
        private int x = 3;

        public void m() {}

        private int getX() { return x; }
    }

    @Test
    public void testNoAccessor() throws Exception {
        ClassPool pool = getPoolFor(NoAccessor.class);
        EntryPointFinder finder = new EntryPointFinder(pool);
        assertThat(finder.findAccessors("x", NoAccessor.class.getTypeName()), empty());
    }





}