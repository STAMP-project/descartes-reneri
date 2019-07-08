package eu.stamp_project.reneri.amplification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public abstract class TargetMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public MavenProject getProject() {
        return project;
    }

    @Parameter(property = "transformations", defaultValue = "${project.build.directory/mutations.json}")
    private File transformations;

    public File getTransformations() {
        return transformations;
    }

    @Parameter(property = "output", defaultValue = "${project.build.directory}")
    private File output;

    public File getOutput() {
        return output;
    }

    private Gson gson = new GsonBuilder().create();

    protected Gson getGson() {
        return gson;
    }

    protected void inspectTransformations() throws MojoExecutionException {
        try {

            JsonReader reader = new JsonReader(new FileReader(getTransformations()));
            readFile(reader);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Bad JSON file", exc);
        }
    }

    protected void readFile(JsonReader reader) throws IOException {
        reader.beginObject();

        while (JsonToken.NAME.equals(reader.peek())) {

            String name = reader.nextName();
            if (!name.equals("mutations")) {
                reader.skipValue();
                continue;
            }

            reader.beginArray();
            while(JsonToken.BEGIN_OBJECT.equals(reader.peek())) {
                readMutation(reader);
            }
            reader.endArray();

        }

        reader.endObject();
    }

    protected abstract void readMutation(JsonReader reader) throws IOException;

    protected File getOutputFile(String name) {
        return output.toPath().resolve(name).toFile();
    }
}
