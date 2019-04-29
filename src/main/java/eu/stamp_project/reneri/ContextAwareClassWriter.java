package eu.stamp_project.reneri;

import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassWriter;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static eu.stamp_project.reneri.utils.ExceptionUtils.propagate;

public class ContextAwareClassWriter  extends ClassWriter {

    private List<String> classpath;

    public ContextAwareClassWriter(ClassReader reader, int flags, List<String> classpath) {
        super(reader, flags);
        this.classpath = classpath;
    }

    @Override
    protected ClassLoader getClassLoader() {
        return new URLClassLoader(
                classpath.stream().map(
                        str -> propagate(() -> new File(str).toURI().toURL())
                ).toArray(URL[]::new),
                super.getClassLoader()
        );
    }

    public List<String> getClasspath() {
        return classpath;
    }
}
