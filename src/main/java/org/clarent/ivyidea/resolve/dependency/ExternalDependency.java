package org.clarent.ivyidea.resolve.dependency;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author Guy Mahieu
 */

public abstract class ExternalDependency implements ResolvedDependency {

    private static final Logger LOGGER = Logger.getLogger(ExternalJarDependency.class.getName());

    private final Artifact artifact;
    private final File localFile;

    public ExternalDependency(Artifact artifact, File localFile) {
        this.artifact = artifact;
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void addTo(ModifiableRootModel moduleModel, Library.ModifiableModel libraryModel) {
        if (localFile == null) {
            LOGGER.warning("Not registering external " + getTypeName() + " dependency for module " + artifact.getModuleRevisionId() +  " as the file does not seem to exist.");
            return;
        }
        final String artifactPath = localFile.getAbsolutePath();
        if (isMissing()) {
            LOGGER.warning("Not registering external " + getTypeName() + " file dependency as the file does not seem to exist: " + artifactPath);
            return;
        }
        if (isAlreadyRegistered(libraryModel)) {
            LOGGER.info("Not re-registering external " + getTypeName() + " file dependency " + artifactPath + " as it is already part of the " + libraryModel.getName() + " library");
            return;
        }
        LOGGER.info("Registering external " + getTypeName() + " file dependency: " + artifactPath);
        libraryModel.addRoot(getUrlForLibrary(), getType());
    }

    @NotNull
    protected String getUrlForLibrary() {
        return VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, localFile.getAbsolutePath()) + JarFileSystem.JAR_SEPARATOR;
    }

    public boolean isMissing() {
        return localFile != null && !new File(localFile.getAbsolutePath()).exists();
    }

    protected boolean isAlreadyRegistered(Library.ModifiableModel libraryModel) {
        for (VirtualFile file : libraryModel.getFiles(getType())) {
            if (isSameDependency(file)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSameDependency(VirtualFile file) {
        // TODO: see if this naive check is good enough - is there a better way to do this?
        final String artifactPath = localFile.getAbsolutePath();
        final String existingDependencyPath = file.getFileSystem().extractPresentableUrl(file.getPath());
        // Compare the files not just the paths
        // TODO: Are these paths always absolute??
        return new File(existingDependencyPath).equals(new File(artifactPath));
    }

    protected abstract String getTypeName();

    protected abstract OrderRootType getType();
}