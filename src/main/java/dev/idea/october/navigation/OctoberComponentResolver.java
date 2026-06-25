package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class OctoberComponentResolver {
    private static final String PLUGINS_DIR = "plugins";
    private static final String THEMES_DIR = "themes";
    private static final String COMPONENTS_DIR = "components";
    private static final Pattern USE_STATEMENT = Pattern.compile("(?m)^\\s*use\\s+([^;]+);");
    private static final Pattern NAMESPACE = Pattern.compile("(?m)^\\s*namespace\\s+([^;]+);");
    private static final Pattern CLASS_CONSTANT_COMPONENT =
        Pattern.compile("([A-Za-z_][A-Za-z0-9_\\\\]*)::class\\s*=>\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern STRING_COMPONENT =
        Pattern.compile("['\"]([A-Za-z_][A-Za-z0-9_\\\\]*)['\"]\\s*=>\\s*['\"]([^'\"]+)['\"]");

    private OctoberComponentResolver() {
    }

    public static @NotNull Optional<Path> findComponent(Path currentTemplatePath, String alias) {
        if (currentTemplatePath == null || alias == null || alias.isBlank()) {
            return Optional.empty();
        }

        Optional<Path> projectRoot = findProjectRoot(currentTemplatePath);
        if (projectRoot.isEmpty()) {
            return Optional.empty();
        }

        Path pluginsRoot = projectRoot.get().resolve(PLUGINS_DIR);
        Optional<Path> registeredComponent = findRegisteredComponent(pluginsRoot, alias);
        if (registeredComponent.isPresent()) {
            return registeredComponent;
        }

        return findComponentByConventionalName(pluginsRoot, alias);
    }

    private static Optional<Path> findRegisteredComponent(Path pluginsRoot, String alias) {
        if (!Files.isDirectory(pluginsRoot)) {
            return Optional.empty();
        }

        try (Stream<Path> pluginFiles = Files.walk(pluginsRoot, 3)) {
            return pluginFiles
                .filter(path -> "Plugin.php".equals(path.getFileName().toString()))
                .map(pluginFile -> resolveFromPluginFile(pluginFile, alias))
                .flatMap(Optional::stream)
                .findFirst();
        }
        catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Path> resolveFromPluginFile(Path pluginFile, String alias) {
        String source;
        try {
            source = Files.readString(pluginFile);
        }
        catch (IOException ignored) {
            return Optional.empty();
        }

        Map<String, String> imports = parseImports(source);
        String namespace = parseNamespace(source).orElse("");

        Optional<Path> classConstantPath = findClassConstantRegistration(pluginFile, source, imports, namespace, alias);
        if (classConstantPath.isPresent()) {
            return classConstantPath;
        }

        return findStringRegistration(pluginFile, source, alias);
    }

    private static Optional<Path> findClassConstantRegistration(
        Path pluginFile,
        String source,
        Map<String, String> imports,
        String namespace,
        String alias
    ) {
        Matcher matcher = CLASS_CONSTANT_COMPONENT.matcher(source);
        while (matcher.find()) {
            if (!alias.equals(matcher.group(2))) {
                continue;
            }

            String classReference = matcher.group(1);
            String fqn = resolveClassReference(classReference, imports, namespace);
            Optional<Path> resolved = pathFromFqn(pluginFile, fqn);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return Optional.empty();
    }

    private static Optional<Path> findStringRegistration(Path pluginFile, String source, String alias) {
        Matcher matcher = STRING_COMPONENT.matcher(source);
        while (matcher.find()) {
            if (!alias.equals(matcher.group(2))) {
                continue;
            }

            Optional<Path> resolved = pathFromFqn(pluginFile, matcher.group(1));
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return Optional.empty();
    }

    private static Optional<Path> pathFromFqn(Path pluginFile, String fqn) {
        Path pluginRoot = pluginFile.getParent();
        if (pluginRoot == null) {
            return Optional.empty();
        }

        String[] parts = fqn.split("\\\\");
        if (parts.length < 3) {
            return Optional.empty();
        }

        Path current = pluginRoot;
        for (int i = 2; i < parts.length; i++) {
            String segment = i == parts.length - 1 ? parts[i] + ".php" : parts[i];
            current = current.resolve(segment);
        }

        return findExistingCaseInsensitivePath(current);
    }

    private static Optional<Path> findComponentByConventionalName(Path pluginsRoot, String alias) {
        if (!Files.isDirectory(pluginsRoot)) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.walk(pluginsRoot, 5)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> hasFileName(path, alias + ".php") || hasFileName(path, alias + "Component.php"))
                .filter(path -> isUnderComponentsDirectory(path))
                .findFirst();
        }
        catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isUnderComponentsDirectory(Path path) {
        for (Path segment : path) {
            if (COMPONENTS_DIR.equalsIgnoreCase(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Path> findProjectRoot(Path currentTemplatePath) {
        Path current = currentTemplatePath.toAbsolutePath().normalize();
        Path directory = Files.isDirectory(current) ? current : current.getParent();

        while (directory != null) {
            if (hasFileName(directory, THEMES_DIR)) {
                return Optional.ofNullable(directory.getParent());
            }
            if (Files.isDirectory(directory.resolve(THEMES_DIR)) && Files.isDirectory(directory.resolve(PLUGINS_DIR))) {
                return Optional.of(directory);
            }
            directory = directory.getParent();
        }

        return Optional.empty();
    }

    private static Map<String, String> parseImports(String source) {
        Map<String, String> imports = new HashMap<>();
        Matcher matcher = USE_STATEMENT.matcher(source);
        while (matcher.find()) {
            String statement = matcher.group(1).trim();
            String[] aliased = statement.split("(?i)\\s+as\\s+");
            String fqn = aliased[0].trim();
            String alias = aliased.length > 1 ? aliased[1].trim() : simpleClassName(fqn);
            imports.put(alias, fqn);
        }
        return imports;
    }

    private static Optional<String> parseNamespace(String source) {
        Matcher matcher = NAMESPACE.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1).trim()) : Optional.empty();
    }

    private static String resolveClassReference(String classReference, Map<String, String> imports, String namespace) {
        String normalized = classReference.startsWith("\\") ? classReference.substring(1) : classReference;
        if (normalized.contains("\\")) {
            return normalized;
        }

        String imported = imports.get(normalized);
        if (imported != null) {
            return imported;
        }

        return namespace.isBlank() ? normalized : namespace + "\\" + normalized;
    }

    private static String simpleClassName(String fqn) {
        int separator = fqn.lastIndexOf('\\');
        return separator < 0 ? fqn : fqn.substring(separator + 1);
    }

    private static Optional<Path> findExistingCaseInsensitivePath(Path expectedPath) {
        if (Files.isRegularFile(expectedPath)) {
            return Optional.of(expectedPath);
        }

        Path parent = expectedPath.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return Optional.empty();
        }

        String expectedName = expectedPath.getFileName().toString().toLowerCase(Locale.ROOT);
        try (Stream<Path> children = Files.list(parent)) {
            return children
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).equals(expectedName))
                .findFirst();
        }
        catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static boolean hasFileName(Path path, String expectedName) {
        Path fileName = path.getFileName();
        return fileName != null && expectedName.equalsIgnoreCase(fileName.toString());
    }
}
