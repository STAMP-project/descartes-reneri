package eu.stamp_project.reneri;

//TODO: If the number of spoon extensions grows put in a dedicated package

import spoon.compiler.SpoonFile;
import spoon.compiler.SpoonFolder;
import spoon.support.compiler.VirtualFolder;

import java.io.File;
import java.io.InputStream;


public class ResourceJavaFile implements SpoonFile {

    private String resourceName;

    public ResourceJavaFile(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public InputStream getContent() {
       return getClass().getClassLoader().getResourceAsStream(resourceName);
    }

    @Override
    public boolean isJava() { return true; }

    @Override
    public boolean isActualFile() { return false; }

    @Override
    public SpoonFolder getParent() { return new VirtualFolder(); }

    @Override
    public String getName() { return resourceName; }

    @Override
    public boolean isFile() { return true; }

    @Override
    public boolean isArchive() { return false; }

    @Override
    public String getPath() { return resourceName; }

    @Override
    public File getFileSystemParent() { return null; }

    @Override
    public File toFile() { return null; }
}
