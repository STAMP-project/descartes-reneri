package eu.stamp_project.reneri;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.version.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;


public class MavenPluginResolver {


    private MavenProject project;

    private MavenSession mavenSession;

    private PluginVersionResolver versionResolver;


    public MavenPluginResolver(MavenProject project,
                               MavenSession mavenSession,
                               PluginVersionResolver versionResolver) {
        this.project = project;
        this.mavenSession = mavenSession;
        this.versionResolver = versionResolver;
    }

    private static boolean hasVersion(Plugin plugin) {
        String version  = plugin.getVersion();
        return version != null && !version.trim().equals("");
    }

    private static boolean hasNewerVersion(Plugin one, Plugin other) {
        DefaultArtifactVersion oneVersion = new DefaultArtifactVersion(one.getVersion());
        DefaultArtifactVersion otherVersion = new DefaultArtifactVersion(other.getVersion());
        return oneVersion.compareTo(otherVersion) > 0;
    }

    public Plugin resolve(String groupId, String artifactId) throws PluginVersionResolutionException {
        return resolve(groupId, artifactId, null, null);
    }

    public Plugin resolve(String groupId, String artifactId, Xpp3Dom configuration) throws PluginVersionResolutionException {
        return resolve(groupId, artifactId, null, configuration);
    }

    public Plugin resolve(String groupId, String artifactId, String version, Xpp3Dom configuration) throws PluginVersionResolutionException {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        plugin.setConfiguration(configuration);
        return resolve(plugin);
    }

    public Plugin resolve(Plugin plugin) throws PluginVersionResolutionException {

        Plugin configured = project.getPlugin(plugin.getKey());

        if(configured != null) {

            if(!hasVersion(plugin) || (hasVersion(configured) && hasNewerVersion(configured, plugin))) {
                plugin.setVersion(configured.getVersion());
            }

            Xpp3Dom requestedConfiguration = (Xpp3Dom) plugin.getConfiguration();
            Xpp3Dom establishedConfiguration = (Xpp3Dom) configured.getConfiguration();

            if(requestedConfiguration == null || establishedConfiguration != null) {
                Xpp3Dom finalConfiguration = Xpp3Dom.mergeXpp3Dom(requestedConfiguration, establishedConfiguration);
                // It seems that Maven ignores nested configuration items when invoked form the command line, but not when invoked programatically
                removeNestedConfigurations(finalConfiguration);
                plugin.setConfiguration(finalConfiguration);
            }

        }

        List<Dependency> dependencies = configured.getDependencies();
        if(dependencies != null) {
            plugin.setDependencies(dependencies);
        }

        if(!hasVersion(plugin)) {
            setVersionFromMaven(plugin);
        }

        return plugin;
    }

    private void removeNestedConfigurations(Xpp3Dom configuration) {
        int index;
        while((index = findIndex(configuration, "configuration")) >= 0) {
            configuration.removeChild(index);
        }
    }

    private int findIndex(Xpp3Dom dom, String tag) {
        Xpp3Dom[] children = dom.getChildren();
        for ( int index = 0; index < children.length; index++ ) {
            if(children[index].getName().equals(tag)) {
                return index;
            }
        }
        return -1;

    }

    private void setVersionFromMaven(Plugin plugin) throws PluginVersionResolutionException {
        PluginVersionRequest request = new DefaultPluginVersionRequest(plugin, mavenSession);
        PluginVersionResult result = versionResolver.resolve(request);
        plugin.setVersion(result.getVersion());
    }

    // Other places to look for plugins:
    // project.getPluginManagement().getPlugins()
    // project.getBuildPlugins()
    // project.getPluginArtifacts()

}
