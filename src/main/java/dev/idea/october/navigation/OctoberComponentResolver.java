package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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

    public static @NotNull List<String> listComponentAliases(Path currentTemplatePath) {
        return listComponentAliasMetadata(currentTemplatePath).stream()
            .map(OctoberComponentAlias::alias)
            .toList();
    }

    public static @NotNull List<OctoberComponentAlias> listComponentAliasMetadata(Path currentTemplatePath) {
        if (currentTemplatePath == null) {
            return List.of();
        }

        Optional<Path> projectRoot = findProjectRoot(currentTemplatePath);
        if (projectRoot.isEmpty()) {
            return List.of();
        }

        Path pluginsRoot = projectRoot.get().resolve(PLUGINS_DIR);
        if (!Files.isDirectory(pluginsRoot)) {
            return List.of();
        }

        Map<String, OctoberComponentAlias> aliases = new java.util.TreeMap<>();
        for (OctoberComponentAlias alias : listRegisteredComponentAliases(pluginsRoot)) {
            aliases.put(alias.alias(), alias);
        }
        for (OctoberComponentAlias alias : listConventionalComponentAliases(pluginsRoot)) {
            aliases.putIfAbsent(alias.alias(), alias);
        }
        return List.copyOf(aliases.values());
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

    private static List<OctoberComponentAlias> listRegisteredComponentAliases(Path pluginsRoot) {
        try (Stream<Path> pluginFiles = Files.walk(pluginsRoot, 3)) {
            return pluginFiles
                .filter(path -> "Plugin.php".equals(path.getFileName().toString()))
                .flatMap(pluginFile -> listAliasesFromPluginFile(pluginFile).stream())
                .toList();
        }
        catch (IOException ignored) {
            return List.of();
        }
    }

    private static List<OctoberComponentAlias> listAliasesFromPluginFile(Path pluginFile) {
        String source;
        try {
            source = Files.readString(pluginFile);
        }
        catch (IOException ignored) {
            return List.of();
        }

        Optional<String> registerComponentsBody = OctoberPhpMethodBodyExtractor.findMethodBody(source, "registerComponents");
        if (registerComponentsBody.isEmpty()) {
            return List.of();
        }

        String owner = parseNamespace(source)
            .map(namespace -> namespace.replace('\\', '.'))
            .orElseGet(() -> ownerFromPluginPath(pluginFile));
        Set<OctoberComponentAlias> aliases = new TreeSet<>(
            java.util.Comparator.comparing(OctoberComponentAlias::alias)
        );
        Matcher classConstantMatcher = CLASS_CONSTANT_COMPONENT.matcher(registerComponentsBody.get());
        while (classConstantMatcher.find()) {
            aliases.add(new OctoberComponentAlias(classConstantMatcher.group(2), owner));
        }

        Matcher stringMatcher = STRING_COMPONENT.matcher(registerComponentsBody.get());
        while (stringMatcher.find()) {
            aliases.add(new OctoberComponentAlias(stringMatcher.group(2), owner));
        }

        return List.copyOf(aliases);
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
        String registerComponentsBody = OctoberPhpMethodBodyExtractor
            .findMethodBody(source, "registerComponents")
            .orElse("");

        Optional<Path> classConstantPath = findClassConstantRegistration(
            pluginFile,
            registerComponentsBody,
            imports,
            namespace,
            alias
        );
        if (classConstantPath.isPresent()) {
            return classConstantPath;
        }

        return findStringRegistration(pluginFile, registerComponentsBody, alias);
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

    private static List<OctoberComponentAlias> listConventionalComponentAliases(Path pluginsRoot) {
        try (Stream<Path> files = Files.walk(pluginsRoot, 5)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".php"))
                .filter(path -> isUnderComponentsDirectory(path))
                .map(path -> new OctoberComponentAlias(aliasFromComponentFile(path), ownerFromComponentPath(path)))
                .sorted(java.util.Comparator.comparing(OctoberComponentAlias::alias))
                .toList();
        }
        catch (IOException ignored) {
            return List.of();
        }
    }

    private static String aliasFromComponentFile(Path path) {
        String fileName = path.getFileName().toString();
        String alias = fileName.substring(0, fileName.length() - ".php".length());
        if (alias.endsWith("Component") && alias.length() > "Component".length()) {
            return alias.substring(0, alias.length() - "Component".length());
        }

        return alias;
    }

    private static String ownerFromComponentPath(Path path) {
        Path pluginRoot = findPluginRoot(path).orElse(null);
        return pluginRoot == null ? "" : ownerFromPluginRoot(pluginRoot);
    }

    private static String ownerFromPluginPath(Path pluginFile) {
        Path pluginRoot = pluginFile.getParent();
        return pluginRoot == null ? "" : ownerFromPluginRoot(pluginRoot);
    }

    private static String ownerFromPluginRoot(Path pluginRoot) {
        Path plugin = pluginRoot.getFileName();
        Path author = pluginRoot.getParent() == null ? null : pluginRoot.getParent().getFileName();
        if (author == null || plugin == null) {
            return "";
        }

        return author + "." + plugin;
    }

    private static Optional<Path> findPluginRoot(Path path) {
        Path current = path.toAbsolutePath().normalize();
        while (current != null) {
            Path parent = current.getParent();
            Path grandParent = parent == null ? null : parent.getParent();
            if (
                parent != null
                    && grandParent != null
                    && PLUGINS_DIR.equalsIgnoreCase(grandParent.getFileName().toString())
            ) {
                return Optional.of(current);
            }
            current = parent;
        }

        return Optional.empty();
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
