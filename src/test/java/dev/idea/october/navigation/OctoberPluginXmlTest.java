package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberPluginXmlTest {
    @Test
    void registersOctoberTemplateCompletionBeforeStandardMarkupCompletion() throws Exception {
        NodeList contributors = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(Path.of("src/main/resources/META-INF/plugin.xml").toFile())
            .getElementsByTagName("completion.contributor");

        boolean hasFirstOctoberXmlContributor = false;
        boolean hasFirstOctoberHtmlContributor = false;
        for (int index = 0; index < contributors.getLength(); index++) {
            Element contributor = (Element) contributors.item(index);
            if (
                "XML".equals(contributor.getAttribute("language"))
                    && "dev.idea.october.navigation.OctoberPartialCompletionContributor"
                        .equals(contributor.getAttribute("implementationClass"))
                    && "first".equals(contributor.getAttribute("order"))
            ) {
                hasFirstOctoberXmlContributor = true;
            }
            if (
                "HTML".equals(contributor.getAttribute("language"))
                    && "dev.idea.october.navigation.OctoberPartialCompletionContributor"
                        .equals(contributor.getAttribute("implementationClass"))
                    && "first".equals(contributor.getAttribute("order"))
            ) {
                hasFirstOctoberHtmlContributor = true;
            }
        }

        assertTrue(hasFirstOctoberXmlContributor);
        assertTrue(hasFirstOctoberHtmlContributor);
    }
}
