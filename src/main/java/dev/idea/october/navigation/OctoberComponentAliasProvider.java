package dev.idea.october.navigation;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OctoberComponentAliasProvider {
    private static final Pattern CLASS_CONSTANT_COMPONENT =
        Pattern.compile("([A-Za-z_][A-Za-z0-9_\\\\]*)::class\\s*=>\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern STRING_COMPONENT =
        Pattern.compile("['\"]([A-Za-z_][A-Za-z0-9_\\\\]*)['\"]\\s*=>\\s*['\"]([^'\"]+)['\"]");

    private OctoberComponentAliasProvider() {
    }

    static @NotNull List<String> listComponentAliases(@NotNull VirtualFile currentFile) {
        return listComponentAliasMetadata(currentFile).stream()
            .map(OctoberComponentAlias::alias)
            .toList();
    }

    static @NotNull List<OctoberComponentAlias> listComponentAliasMetadata(@NotNull VirtualFile currentFile) {
        List<OctoberComponentAlias> aliases = OctoberComponentResolver.listComponentAliasMetadata(Path.of(currentFile.getPath()));
        if (!aliases.isEmpty()) {
            return aliases;
        }

        return listComponentAliasMetadataFromVirtualFile(currentFile);
    }

    static boolean hasComponentAlias(
        @NotNull VirtualFile currentFile,
        @NotNull String alias
    ) {
        if (OctoberComponentResolver.findComponent(Path.of(currentFile.getPath()), alias).isPresent()) {
            return true;
        }

        return listComponentAliasMetadataFromVirtualFile(currentFile).stream()
            .map(OctoberComponentAlias::alias)
            .toList()
            .contains(alias);
    }

    static @NotNull Optional<VirtualFile> findComponentFile(
        @NotNull VirtualFile currentFile,
        @NotNull String alias
    ) {
        Optional<java.nio.file.Path> resolvedPath = OctoberComponentResolver.findComponent(Path.of(currentFile.getPath()), alias);
        if (resolvedPath.isPresent()) {
            VirtualFile localFile = LocalFileSystem.getInstance().findFileByNioFile(resolvedPath.get());
            if (localFile != null) {
                return Optional.of(localFile);
            }
        }

        Optional<VirtualFile> projectRoot = findProjectRoot(currentFile);
        if (projectRoot.isEmpty()) {
            return Optional.empty();
        }

        VirtualFile pluginsRoot = projectRoot.get().findChild("plugins");
        if (pluginsRoot == null || !pluginsRoot.isDirectory()) {
            return Optional.empty();
        }

        Optional<VirtualFile> registeredComponent = findRegisteredComponentFile(pluginsRoot, alias);
        return registeredComponent.isPresent()
            ? registeredComponent
            : findConventionalComponentFile(pluginsRoot, alias);
    }

    private static @NotNull List<OctoberComponentAlias> listComponentAliasMetadataFromVirtualFile(@NotNull VirtualFile currentFile) {
        Optional<VirtualFile> projectRoot = findProjectRoot(currentFile);
        if (projectRoot.isEmpty()) {
            return List.of();
        }

        VirtualFile pluginsRoot = projectRoot.get().findChild("plugins");
        if (pluginsRoot == null || !pluginsRoot.isDirectory()) {
            return List.of();
        }

        Set<OctoberComponentAlias> aliases = new TreeSet<>(
            java.util.Comparator.comparing(OctoberComponentAlias::alias)
        );
        collectComponentAliases(pluginsRoot, aliases);
        return List.copyOf(aliases);
    }

    private static void collectComponentAliases(@NotNull VirtualFile current, Set<OctoberComponentAlias> aliases) {
        if (!current.isDirectory()) {
            if ("Plugin.php".equals(current.getName())) {
                aliases.addAll(readRegisteredAliases(current));
            }
            else if (current.getName().endsWith(".php") && isUnderComponentsDirectory(current)) {
                aliases.add(new OctoberComponentAlias(aliasFromComponentFile(current.getName()), ownerFromComponentFile(current)));
            }
            return;
        }

        for (VirtualFile child : current.getChildren()) {
            collectComponentAliases(child, aliases);
        }
    }

    private static @NotNull List<OctoberComponentAlias> readRegisteredAliases(@NotNull VirtualFile pluginFile) {
        String source;
        try {
            source = new String(pluginFile.contentsToByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException ignored) {
            return List.of();
        }

        Optional<String> registerComponentsBody = OctoberPhpMethodBodyExtractor.findMethodBody(source, "registerComponents");
        if (registerComponentsBody.isEmpty()) {
            return List.of();
        }

        String owner = namespaceFromSource(source).orElseGet(() -> ownerFromPluginFile(pluginFile));
        List<OctoberComponentAlias> aliases = new ArrayList<>();
        Matcher classConstantMatcher = CLASS_CONSTANT_COMPONENT.matcher(registerComponentsBody.get());
        while (classConstantMatcher.find()) {
            aliases.add(new OctoberComponentAlias(classConstantMatcher.group(2), owner));
        }

        Matcher stringMatcher = STRING_COMPONENT.matcher(registerComponentsBody.get());
        while (stringMatcher.find()) {
            aliases.add(new OctoberComponentAlias(stringMatcher.group(2), owner));
        }

        return aliases;
    }

    private static @NotNull Optional<VirtualFile> findRegisteredComponentFile(
        @NotNull VirtualFile pluginsRoot,
        @NotNull String alias
    ) {
        List<VirtualFile> pluginFiles = new ArrayList<>();
        collectPluginFiles(pluginsRoot, pluginFiles);
        for (VirtualFile pluginFile : pluginFiles) {
            Optional<VirtualFile> resolved = resolveFromPluginFile(pluginFile, alias);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return Optional.empty();
    }

    private static void collectPluginFiles(@NotNull VirtualFile current, List<VirtualFile> pluginFiles) {
        if (!current.isDirectory()) {
            if ("Plugin.php".equals(current.getName())) {
                pluginFiles.add(current);
            }
            return;
        }

        for (VirtualFile child : current.getChildren()) {
            collectPluginFiles(child, pluginFiles);
        }
    }

    private static Optional<VirtualFile> resolveFromPluginFile(
        @NotNull VirtualFile pluginFile,
        @NotNull String alias
    ) {
        String source;
        try {
            source = new String(pluginFile.contentsToByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException ignored) {
            return Optional.empty();
        }

        String registerComponentsBody = OctoberPhpMethodBodyExtractor
            .findMethodBody(source, "registerComponents")
            .orElse("");
        Map<String, String> imports = parseImports(source);
        String namespace = namespaceFromSource(source).orElse("");

        Matcher classConstantMatcher = CLASS_CONSTANT_COMPONENT.matcher(registerComponentsBody);
        while (classConstantMatcher.find()) {
            if (!alias.equals(classConstantMatcher.group(2))) {
                continue;
            }

            String fqn = resolveClassReference(classConstantMatcher.group(1), imports, namespace);
            Optional<VirtualFile> resolved = virtualFileFromFqn(pluginFile, fqn);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        Matcher stringMatcher = STRING_COMPONENT.matcher(registerComponentsBody);
        while (stringMatcher.find()) {
            if (!alias.equals(stringMatcher.group(2))) {
                continue;
            }

            Optional<VirtualFile> resolved = virtualFileFromFqn(pluginFile, stringMatcher.group(1));
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return Optional.empty();
    }

    private static @NotNull Optional<VirtualFile> virtualFileFromFqn(
        @NotNull VirtualFile pluginFile,
        @NotNull String fqn
    ) {
        VirtualFile pluginRoot = pluginFile.getParent();
        if (pluginRoot == null) {
            return Optional.empty();
        }

        String[] parts = fqn.split("\\\\");
        if (parts.length < 3) {
            return Optional.empty();
        }

        VirtualFile current = pluginRoot;
        for (int index = 2; index < parts.length; index++) {
            String segment = index == parts.length - 1 ? parts[index] + ".php" : parts[index];
            current = findChildCaseInsensitive(current, segment);
            if (current == null) {
                return Optional.empty();
            }
        }

        return current.isDirectory() ? Optional.empty() : Optional.of(current);
    }

    private static @NotNull Optional<VirtualFile> findConventionalComponentFile(
        @NotNull VirtualFile pluginsRoot,
        @NotNull String alias
    ) {
        List<VirtualFile> files = new ArrayList<>();
        collectComponentFiles(pluginsRoot, files);
        String plainName = alias + ".php";
        String componentName = alias + "Component.php";
        return files.stream()
            .filter(file -> file.getName().equalsIgnoreCase(plainName) || file.getName().equalsIgnoreCase(componentName))
            .findFirst();
    }

    private static void collectComponentFiles(@NotNull VirtualFile current, List<VirtualFile> files) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".php") && isUnderComponentsDirectory(current)) {
                files.add(current);
            }
            return;
        }

        for (VirtualFile child : current.getChildren()) {
            collectComponentFiles(child, files);
        }
    }

    private static VirtualFile findChildCaseInsensitive(@NotNull VirtualFile parent, @NotNull String expectedName) {
        VirtualFile direct = parent.findChild(expectedName);
        if (direct != null) {
            return direct;
        }

        String lowerExpectedName = expectedName.toLowerCase(Locale.ROOT);
        for (VirtualFile child : parent.getChildren()) {
            if (child.getName().toLowerCase(Locale.ROOT).equals(lowerExpectedName)) {
                return child;
            }
        }

        return null;
    }

    private static boolean isUnderComponentsDirectory(@NotNull VirtualFile file) {
        VirtualFile current = file.getParent();
        while (current != null) {
            if ("components".equalsIgnoreCase(current.getName())) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    private static String aliasFromComponentFile(@NotNull String fileName) {
        String alias = fileName.substring(0, fileName.length() - ".php".length());
        if (alias.endsWith("Component") && alias.length() > "Component".length()) {
            return alias.substring(0, alias.length() - "Component".length());
        }

        return alias;
    }

    private static Optional<String> namespaceFromSource(@NotNull String source) {
        Matcher matcher = Pattern.compile("(?m)^\\s*namespace\\s+([^;]+);").matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1).trim().replace('\\', '.')) : Optional.empty();
    }

    private static Map<String, String> parseImports(String source) {
        Map<String, String> imports = new HashMap<>();
        Matcher matcher = Pattern.compile("(?m)^\\s*use\\s+([^;]+);").matcher(source);
        while (matcher.find()) {
            String statement = matcher.group(1).trim();
            String[] aliased = statement.split("(?i)\\s+as\\s+");
            String fqn = aliased[0].trim();
            String alias = aliased.length > 1 ? aliased[1].trim() : simpleClassName(fqn);
            imports.put(alias, fqn);
        }
        return imports;
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

        String normalizedNamespace = namespace.replace('.', '\\');
        return normalizedNamespace.isBlank() ? normalized : normalizedNamespace + "\\" + normalized;
    }

    private static String simpleClassName(String fqn) {
        int separator = fqn.lastIndexOf('\\');
        return separator < 0 ? fqn : fqn.substring(separator + 1);
    }

    private static String ownerFromComponentFile(@NotNull VirtualFile file) {
        VirtualFile pluginRoot = findPluginRoot(file).orElse(null);
        return pluginRoot == null ? "" : ownerFromPluginRoot(pluginRoot);
    }

    private static String ownerFromPluginFile(@NotNull VirtualFile file) {
        VirtualFile pluginRoot = file.getParent();
        return pluginRoot == null ? "" : ownerFromPluginRoot(pluginRoot);
    }

    private static String ownerFromPluginRoot(@NotNull VirtualFile pluginRoot) {
        VirtualFile authorRoot = pluginRoot.getParent();
        if (authorRoot == null) {
            return "";
        }

        return authorRoot.getName() + "." + pluginRoot.getName();
    }

    private static Optional<VirtualFile> findPluginRoot(@NotNull VirtualFile file) {
        VirtualFile current = file;
        while (current != null) {
            VirtualFile parent = current.getParent();
            VirtualFile grandParent = parent == null ? null : parent.getParent();
            if (grandParent != null && "plugins".equalsIgnoreCase(grandParent.getName())) {
                return Optional.of(current);
            }
            current = parent;
        }

        return Optional.empty();
    }

    private static @NotNull Optional<VirtualFile> findProjectRoot(@NotNull VirtualFile currentFile) {
        VirtualFile current = currentFile.isDirectory() ? currentFile : currentFile.getParent();
        while (current != null) {
            VirtualFile parent = current.getParent();
            if (parent != null && "themes".equalsIgnoreCase(parent.getName())) {
                return Optional.ofNullable(parent.getParent());
            }
            if (current.findChild("themes") != null && current.findChild("plugins") != null) {
                return Optional.of(current);
            }
            current = parent;
        }

        return Optional.empty();
    }
}
