package eu.stamp_project.reneri.instrumentation;

import java.util.HashMap;
import java.util.Optional;

public class Trie<T> {

    private class Node {

        public HashMap<String, Node> children = new HashMap<>();

        public T data;

        public String path;

        public Optional<Node> next(String fragment) {
            Node nextNode =  children.getOrDefault(fragment, null);
            return (nextNode == null)? Optional.empty() : Optional.of(nextNode);
        }

        public void add(String step, Node node) {
            children.put(step, node);
        }
    }

    Node root = new Node();
    public Trie() { }

    public void add(String path, T data) throws IllegalArgumentException {
        System.out.println("DEBUG => " + path);

        add(path, data, "\\|");
    }

    public void add(String path, T data, String separator) throws IllegalArgumentException {
        if(data == null) {
            throw new IllegalArgumentException("Data can not be null");
        }
        Node current = root;
        for(String step : path.split(separator)) {
            Optional<Node> candidate = current.next(step);
            if(candidate.isPresent()) {
                current = candidate.get();
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
            Optional<Node> next = current.next(step);
            if(!next.isPresent()) {
               break;
            }
            current = next.get();
        }
        return current.data;
    }

    public T getClosestMatch(String path) { return getClosestMatch(path, "\\|"); }

}
