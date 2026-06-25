package dev.idea.october.navigation;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class OctoberComponentPropertyProvider {
    private OctoberComponentPropertyProvider() {
    }

    static @NotNull List<OctoberComponentProperty> listProperties(
        @NotNull VirtualFile currentFile,
        @NotNull String componentAlias
    ) {
        List<OctoberComponentProperty> properties = OctoberComponentPropertyResolver.listProperties(
            Path.of(currentFile.getPath()),
            componentAlias
        );
        if (!properties.isEmpty()) {
            return properties;
        }

        Optional<VirtualFile> componentFile = OctoberComponentAliasProvider.findComponentFile(currentFile, componentAlias);
        if (componentFile.isEmpty()) {
            return List.of();
        }

        try {
            String source = new String(componentFile.get().contentsToByteArray(), StandardCharsets.UTF_8);
            return OctoberComponentPropertyResolver.listPropertiesFromSource(source);
        }
        catch (IOException ignored) {
            return List.of();
        }
    }
}
