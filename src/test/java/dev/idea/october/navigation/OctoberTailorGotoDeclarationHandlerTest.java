package dev.idea.october.navigation;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public class OctoberTailorGotoDeclarationHandlerTest extends BasePlatformTestCase {
    private Path physicalProjectRoot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        physicalProjectRoot = Files.createTempDirectory("idea-october-tailor-navigation");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        }
        finally {
            if (physicalProjectRoot != null) {
                FileUtil.delete(physicalProjectRoot);
            }
        }
    }

    public void testNavigatesFromTailorComponentToBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[coll<caret>ection posts]\nhandle = \"Blog\\Post\"\n==",
            "app/blueprints/blog/post.yaml",
            "handle: Blog\\Post\ntype: stream\nname: Blog posts\n"
        );
    }

    public void testNavigatesFromTailorHandleToBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[collection posts]\nhandle = \"Blog\\P<caret>ost\"\n==",
            "app/blueprints/blog/post.yaml",
            "handle: Blog\\Post\ntype: stream\nname: Blog posts\n"
        );
    }

    public void testNavigatesFromTailorPageAliasToBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[collection po<caret>sts]\nhandle = \"Blog\\Post\"\n==",
            "app/blueprints/blog/post.yaml",
            "handle: Blog\\Post\ntype: stream\nname: Blog posts\n"
        );
    }

    public void testNavigatesFromSectionToCurrentThemeBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[sec<caret>tion post]\nhandle = \"Blog\\Post\"\n==",
            "themes/bcc/blueprints/blog/post.yaml",
            "handle: Blog\\Post\ntype: stream\nname: Blog posts\n"
        );
    }

    public void testNavigatesFromGlobalToPluginBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[glo<caret>bal config]\nhandle = \"Site\\Config\"\n==",
            "plugins/acme/site/blueprints/config.yaml",
            "handle: Site\\Config\ntype: global\nname: Site config\n"
        );
    }

    public void testNavigatesFromSectionWithUnquotedHandleToPluginBlueprint() throws Exception {
        assertNavigatesToBlueprint(
            "[sec<caret>tion portfolio]\nhandle = Content\\Portfolios\n==",
            "plugins/gabion/contenter/blueprints/content/portfolios.yaml",
            "handle: Content\\Portfolios\ntype: structure\nname: Portfolios\n"
        );
    }

    public void testKeepsNavigationToRegularPhpComponents() throws Exception {
        writeFile(
            "plugins/acme/blog/Plugin.php",
            """
                <?php
                namespace Acme\\Blog;
                use Acme\\Blog\\Components\\Posts;
                class Plugin
                {
                    public function registerComponents()
                    {
                        return [Posts::class => 'Posts'];
                    }
                }
                """
        );
        assertNavigatesToTarget(
            "[Po<caret>sts]\n==",
            "plugins/acme/blog/components/Posts.php",
            "<?php\nnamespace Acme\\Blog\\Components;\nclass Posts {}\n"
        );
    }

    private void assertNavigatesToBlueprint(
        String pageText,
        String blueprintRelativePath,
        String blueprintText
    ) throws Exception {
        assertNavigatesToTarget(pageText, blueprintRelativePath, blueprintText);
    }

    private void assertNavigatesToTarget(String pageText, String targetRelativePath, String targetText) throws Exception {
        int caretOffset = pageText.indexOf("<caret>");
        String cleanPageText = pageText.replace("<caret>", "");
        Path targetPath = writeFile(targetRelativePath, targetText);
        Path pagePath = writeFile("themes/bcc/pages/blog.htm", cleanPageText);

        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        VirtualFile targetFile = localFileSystem.refreshAndFindFileByNioFile(targetPath);
        VirtualFile pageFile = localFileSystem.refreshAndFindFileByNioFile(pagePath);
        assertNotNull(targetFile);
        assertNotNull(pageFile);
        myFixture.configureFromExistingVirtualFile(pageFile);
        myFixture.getEditor().getCaretModel().moveToOffset(caretOffset);

        PsiFile page = myFixture.getFile();
        PsiElement source = page.findElementAt(caretOffset);
        assertNotNull(source);
        PsiElement[] targets = new OctoberPartialGotoDeclarationHandler()
            .getGotoDeclarationTargets(source, caretOffset, myFixture.getEditor());

        assertNotNull(targets);
        assertEquals(1, targets.length);
        PsiFile target = PsiManager.getInstance(getProject()).findFile(targetFile);
        assertNotNull(target);
        assertEquals(targetFile, targets[0].getContainingFile().getVirtualFile());
    }

    private Path writeFile(String relativePath, String contents) throws Exception {
        Path file = physicalProjectRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
        assertNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file));
        return file;
    }
}
