package dev.idea.october.navigation;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public class OctoberTailorBlueprintResolverTest extends BasePlatformTestCase {
    private Path projectRoot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        projectRoot = Files.createTempDirectory("idea-october-tailor-resolver");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        }
        finally {
            if (projectRoot != null) {
                FileUtil.delete(projectRoot);
            }
        }
    }

    public void testFindsAppBlueprintByHandle() throws Exception {
        Path page = createPage();
        Path blueprint = writeBlueprint(
            "app/blueprints/blog/post.yaml",
            "handle: Blog\\Post\ntype: stream\nname: Blog posts\n"
        );

        assertEquals(blueprint, findBlueprint(page, "Blog\\Post"));
    }

    public void testFindsBlueprintInCurrentTheme() throws Exception {
        Path page = createPage();
        Path blueprint = writeBlueprint(
            "themes/bcc/blueprints/pages/home.yml",
            "handle: 'Pages\\Home'\ntype: single\nname: Home\n"
        );

        assertEquals(blueprint, findBlueprint(page, "Pages\\Home"));
    }

    public void testFindsPluginBlueprintByHandle() throws Exception {
        Path page = createPage();
        Path blueprint = writeBlueprint(
            "plugins/acme/blog/blueprints/category.yaml",
            "handle: \"Blog\\\\Category\"\ntype: structure\nname: Categories\n"
        );

        assertEquals(blueprint, findBlueprint(page, "Blog\\Category"));
    }

    public void testReadsIndentedTopLevelYamlMapping() throws Exception {
        Path page = createPage();
        Path blueprint = writeBlueprint(
            "app/blueprints/blog/indented.yaml",
            "  handle: Blog\\Indented\n  type: stream\n  name: Indented\n"
        );

        assertEquals(blueprint, findBlueprint(page, "Blog\\Indented"));
    }

    public void testInvalidatesCachedIndexAfterBlueprintIsAddedThroughVfs() throws Exception {
        Path page = createPage();
        assertTrue(OctoberTailorBlueprintResolver.findBlueprint(getProject(), page, "Blog\\Category").isEmpty());

        Path blueprint = writeBlueprint(
            "plugins/acme/blog/blueprints/category.yaml",
            "handle: Blog\\Category\ntype: structure\nname: Categories\n"
        );

        assertEquals(blueprint, findBlueprint(page, "Blog\\Category"));
    }

    public void testIgnoresYamlFilesWithDifferentHandle() throws Exception {
        Path page = createPage();
        writeBlueprint(
            "app/blueprints/blog/post.yaml",
            "handle: Blog\\Article\ntype: stream\nname: Articles\n"
        );

        assertTrue(OctoberTailorBlueprintResolver.findBlueprint(getProject(), page, "Blog\\Post").isEmpty());
    }

    private Path findBlueprint(Path page, String handle) {
        return OctoberTailorBlueprintResolver.findBlueprint(getProject(), page, handle).orElseThrow();
    }

    private Path createPage() throws Exception {
        Path page = projectRoot.resolve("themes/bcc/pages/blog.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "[collection posts]\nhandle = \"Blog\\Post\"\n==");
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(page);
        return page;
    }

    private Path writeBlueprint(String relativePath, String contents) throws Exception {
        Path blueprint = projectRoot.resolve(relativePath);
        Files.createDirectories(blueprint.getParent());
        Files.writeString(blueprint, contents);
        assertNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(blueprint));
        return blueprint;
    }
}
