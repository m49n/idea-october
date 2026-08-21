package dev.idea.october.navigation;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class OctoberTailorBlueprintResolver {
    private static final String BLUEPRINTS_DIR = "blueprints";
    private static final Key<Map<BlueprintContext, CachedValue<Map<String, Path>>>> PROJECT_INDEXES =
        Key.create("dev.idea.october.tailorBlueprintIndexes");

    private OctoberTailorBlueprintResolver() {
    }

    public static @NotNull Optional<Path> findBlueprint(
        Project project,
        Path currentTemplatePath,
        String handle
    ) {
        if (
            project == null
                || project.isDisposed()
                || currentTemplatePath == null
                || handle == null
                || handle.isBlank()
        ) {
            return Optional.empty();
        }

        Optional<OctoberProjectIndex> projectIndex = OctoberProjectIndex.fromPath(currentTemplatePath);
        if (projectIndex.isEmpty()) {
            return Optional.empty();
        }

        BlueprintContext context = new BlueprintContext(
            projectIndex.get().projectRoot(),
            findThemeRoot(currentTemplatePath)
        );
        return Optional.ofNullable(getBlueprintIndex(project, context).get(handle));
    }

    private static Map<String, Path> getBlueprintIndex(Project project, BlueprintContext context) {
        Map<BlueprintContext, CachedValue<Map<String, Path>>> indexes;
        synchronized (project) {
            indexes = project.getUserData(PROJECT_INDEXES);
            if (indexes == null) {
                indexes = new HashMap<>();
                project.putUserData(PROJECT_INDEXES, indexes);
            }
        }

        CachedValue<Map<String, Path>> cachedIndex;
        synchronized (indexes) {
            cachedIndex = indexes.computeIfAbsent(
                context,
                ignored -> CachedValuesManager.getManager(project).createCachedValue(
                    () -> CachedValueProvider.Result.create(
                        buildBlueprintIndex(project, context),
                        VirtualFileManager.getInstance()
                    ),
                    false
                )
            );
        }
        return cachedIndex.getValue();
    }

    private static Map<String, Path> buildBlueprintIndex(Project project, BlueprintContext context) {
        Map<String, Path> blueprints = new LinkedHashMap<>();
        indexRoot(project, context.projectRoot().resolve("app").resolve(BLUEPRINTS_DIR), blueprints);
        context.themeRoot().ifPresent(
            themeRoot -> indexRoot(project, themeRoot.resolve(BLUEPRINTS_DIR), blueprints)
        );
        indexPluginBlueprints(project, context.projectRoot().resolve("plugins"), blueprints);
        return Map.copyOf(blueprints);
    }

    private static void indexPluginBlueprints(Project project, Path pluginsRoot, Map<String, Path> blueprints) {
        if (!Files.isDirectory(pluginsRoot)) {
            return;
        }

        try (Stream<Path> directories = Files.walk(pluginsRoot, 3)) {
            directories
                .filter(Files::isDirectory)
                .filter(path -> hasFileName(path, BLUEPRINTS_DIR))
                .sorted()
                .forEach(path -> indexRoot(project, path, blueprints));
        }
        catch (IOException ignored) {
        }
    }

    private static void indexRoot(Project project, Path blueprintsRoot, Map<String, Path> blueprints) {
        if (!Files.isDirectory(blueprintsRoot)) {
            return;
        }

        try (Stream<Path> files = Files.walk(blueprintsRoot)) {
            files
                .filter(Files::isRegularFile)
                .filter(OctoberTailorBlueprintResolver::isYamlFile)
                .sorted()
                .forEach(path -> readHandle(project, path).ifPresent(handle -> blueprints.putIfAbsent(handle, path)));
        }
        catch (IOException ignored) {
        }
    }

    private static Optional<String> readHandle(Project project, Path blueprintPath) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(blueprintPath);
        if (virtualFile == null) {
            return Optional.empty();
        }

        return ReadAction.compute(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (!(psiFile instanceof YAMLFile yamlFile)) {
                return Optional.empty();
            }

            for (YAMLDocument document : yamlFile.getDocuments()) {
                YAMLValue topLevelValue = document.getTopLevelValue();
                if (!(topLevelValue instanceof YAMLMapping mapping)) {
                    continue;
                }

                YAMLKeyValue handle = mapping.getKeyValueByKey("handle");
                if (handle != null && handle.getValue() != null && !handle.getValueText().isBlank()) {
                    return Optional.of(handle.getValueText());
                }
            }

            return Optional.empty();
        });
    }

    private static Optional<Path> findThemeRoot(Path currentTemplatePath) {
        Path current = currentTemplatePath.toAbsolutePath().normalize();
        Path directory = Files.isDirectory(current) ? current : current.getParent();
        while (directory != null) {
            Path parent = directory.getParent();
            if (parent != null && hasFileName(parent, "themes")) {
                return Optional.of(directory);
            }
            directory = parent;
        }

        return Optional.empty();
    }

    private static boolean isYamlFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }

    private static boolean hasFileName(Path path, String expectedName) {
        Path fileName = path.getFileName();
        return fileName != null && expectedName.equalsIgnoreCase(fileName.toString());
    }

    private record BlueprintContext(@NotNull Path projectRoot, @NotNull Optional<Path> themeRoot) {
    }
}
