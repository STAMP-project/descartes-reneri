package eu.stamp_project.reneri.datastructures;

import java.util.HashMap;
import java.util.Optional;

public class Trie<T> {

    private class Node {

        public HashMap<String, Node> children = new HashMap<>();

        public T data;

        public String path;

        public Node next(String fragment) {
            return children.getOrDefault(fragment, null);
        }

        public void add(String step, Node node) {
            children.put(step, node);
        }
    }

    Node root = new Node();
    public Trie() { }

    public void add(String path, T data) throws IllegalArgumentException {
        add(path, data, "\\|");
    }

    public void add(String path, T data, String separator) throws IllegalArgumentException {
        if(data == null) {
            throw new IllegalArgumentException("Data can not be null");
        }
        Node current = root;
        for(String step : path.split(separator)) {
            Node candidate = current.next(step);
            if(candidate != null) {
                current = candidate;
            }
            else {
                Node nextNode = new Node();
                current.add(step , nextNode);
                current = nextNode;
            }
        }
        if(current.data != null) {
            throw new IllegalArgumentException(String.format("Data for path %s has been already added", path));
        }
        current.data = data;
    }

    public T getClosestMatch(String path, String separator) {
        Node current = root;
        for(String step : path.split(separator)) {
            Node next = current.next(step);
            if(next == null) {
               break;
            }
            current = next;
        }
        return current.data;
    }

    public T getClosestMatch(String path) { return getClosestMatch(path, "\\|"); }

}
