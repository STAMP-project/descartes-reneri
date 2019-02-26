package eu.stamp_project.reneri.datastructures;

import java.util.*;

public class DirectedGraph<T> {

    private HashMap<T, Set<T>> adjacencyList;

    public DirectedGraph() {
        adjacencyList = new HashMap<>();
    }

    public void addEdge(T from, T to) {
        Set<T> previous = adjacencyList.computeIfAbsent(from, (node) -> new HashSet<>());
        previous.add(to);
    }

    public void addNode(T node) {
        adjacencyList.putIfAbsent(node, new HashSet<>());
    }

    public Set<T> getClousure(T node) {
        if(!adjacencyList.containsKey(node)) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(node);
        while(!queue.isEmpty()) {
            T current = queue.remove();
            if(result.contains(current)) {
                continue;
            }
            result.add(current);
            queue.addAll(adjacencyList.getOrDefault(current, Collections.emptySet()));
        }
        return result;
    }
}
