package eu.stamp_project.reneri;

import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class InheritanceGraphTest {

    @Test
    public void testSimpleGraph() {

        String code =
                "public class Container {" +
                        "public class Base {}\n" +
                        "public class A extends Base {}\n" +
                        "public class B extends A    {}\n" +
                        "public class C extends B    {}\n" +
                        "public class D extends A    {}\n" +
                "}";

        CtClass<?> container = Launcher.parseClass(code);
        Set<CtClass<?>> classes = container.getNestedTypes().stream().map(type -> (CtClass<?>) type).collect(Collectors.toSet());
        InheritanceGraph graph = new InheritanceGraph(classes);

        assertEquals(5, graph.getInheritanceClousure("Container$Base").size());

    }

}