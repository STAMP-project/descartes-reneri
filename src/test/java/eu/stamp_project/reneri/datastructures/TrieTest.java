package eu.stamp_project.reneri.datastructures;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TrieTest {

    Trie<String> trie;

    @Before
    public void setUp() {
        trie = new Trie<>();
    }

    @Test
    public void testSingleAdditionExactMatch() throws Exception {
        final String data = "something";
        final String path = "path.to.Class|method|1|variable";
        trie.add(path, data);
        assertEquals("Wrong result for a single addition and query with exect path", data, trie.getClosestMatch(path));
    }

    @Test
    public void testSingleAdditionSubmatch() throws Exception {
        final String data = "something";
        final String path = "path.to.Class|method|1";
        trie.add(path, data);
        assertEquals("Wrong result single addition and submatch", data, trie.getClosestMatch(path + "|#size"));
    }

    @Test
    public void testMultipleInsertionsSingleValue() throws Exception {

        final String root = "path.to.CLass|method|1";
        final String data = "something";

        trie.add(root, data);

        final int fieldCount = 3;
        for(int field = 0; field < fieldCount; field++) {
            assertEquals("Wrong value after multiple insertions", data, trie.getClosestMatch(root + "|" + field));
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertNull() {
        trie.add("path", null);
        fail("IllegalArgumentException should be thrown when inserting a null value");
    }

}