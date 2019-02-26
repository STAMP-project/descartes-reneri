package eu.stamp_project.reneri.datastructures;

import org.junit.Before;
import org.junit.Test;

import static eu.stamp_project.reneri.testutils.SameSetMatcher.*;

import static org.junit.Assert.*;

public class DirectedGraphTest {

    private DirectedGraph<String> graph;

    @Before
    public void setUp() {
        graph = new DirectedGraph<>();
    }

    @Test
    public void testEmptyGraph() {
        assertThat(graph.getClousure("A"), emptySet());
    }

    @Test
    public void testWitNoAGraph() {
        graph.addNode("B");
        assertThat(graph.getClousure("A"), emptySet());
    }

    @Test
    public void testGraphWithOneNode() {
        graph.addNode("A");
        assertThat(graph.getClousure("A"), sameSetAs("A"));
    }

    @Test
    public void testGraphWithOneEdge() {
        graph.addEdge("A", "B");

        assertThat(graph.getClousure("A"), sameSetAs("A", "B"));
    }

    @Test
    public void testTransitive() {
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        assertThat(graph.getClousure("A"), sameSetAs("A", "B", "C"));
    }

    @Test
    public void testLoop() {
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");
        assertThat(graph.getClousure("A"), sameSetAs("A", "B", "C"));
    }

    @Test
    public void testDisjointGraph() {
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("A", "D");
        graph.addEdge("B", "E");
        graph.addEdge("F", "G");
        assertThat(graph.getClousure("A"), sameSetAs("A", "B", "C", "D", "E"));
    }

}