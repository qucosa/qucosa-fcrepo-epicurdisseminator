package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.Epicur;
import de.dnb.xepicur.UpdateStatusType;
import de.qucosa.dissemination.epicur.testsupport.XmlTestsupport;
import org.custommonkey.xmlunit.XMLAssert;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EpicurBuilderTest extends XmlTestsupport {

    @Test
    public void Build_bare_epicur_dissemination_without_exceptions() throws Exception {
        EpicurBuilder epicurBuilder = new EpicurBuilder();
        Epicur epicur = epicurBuilder
                .mets(buildMetsDocument("agent", "4711", "urn:nbn:de:foo-4711"))
                .build();
        assertNotNull(epicur);
    }

    @Test
    public void Administrative_section_contains_update_status() throws Exception {
        EpicurBuilder epicurBuilder = new EpicurBuilder()
                .updateStatus(UpdateStatus.urn_new);
        Epicur epicur = epicurBuilder
                .mets(buildMetsDocument("agent", "4711", "urn:nbn:de:foo-4711"))
                .build();

        UpdateStatusType updateStatusType = epicur.getAdministrativeData().getDelivery().getUpdateStatus();
        assertEquals(UpdateStatus.urn_new.name(), updateStatusType.getType());
    }

    @Test
    public void Record_with_transfer_identifier_has_qucosa_URN() throws Exception {
        String scheme = "urn:nbn:de";
        String urn = scheme + ":foo-4711";
        String pid = "foo:4711";
        String agent = "agent";
        Document metsDocument = buildMetsDocument(agent, pid, urn);
        EpicurBuilder epicurBuilder = new EpicurBuilder()
                .mets(metsDocument);

        Epicur epicur = epicurBuilder.build();

        XMLAssert.assertXpathExists(
                "//e:record/e:identifier[@scheme='" + scheme + "' and text()='" + urn + "']",
                marshal(epicur));
    }

    @Test
    public void Record_has_frontpage_resource_identifier() throws Exception {
        String origin = "original";
        String pid = "foo:4711";
        String role = "primary";
        String scheme = "url";
        String type = "frontpage";
        String url = "http://example.com/id/";
        String agent = "agent";
        Document metsDocument = buildMetsDocument(agent, pid, "urn:some:foo");
        EpicurBuilder epicurBuilder = new EpicurBuilder()
                .frontpageUrlPattern(url + "##PID##")
                .mets(metsDocument);

        Epicur epicur = epicurBuilder.build();

        XMLAssert.assertXpathExists(
                String.format("//e:record/e:resource/e:identifier[@scheme='%s' and @type='%s' and @role='%s' and @origin='%s'" +
                                " and text()='%s']",
                        scheme, type, role, origin, url + pid), marshal(epicur));
    }

    @Test
    public void Record_has_agent_name_in_frontpage_resource_identifier() throws Exception {
        String agent = "agent";
        String origin = "original";
        String pid = "foo:4711";
        String role = "primary";
        String scheme = "url";
        String type = "frontpage";
        String urlPattern = "http://##AGENT##.example.com/id/##PID##";
        Document metsDocument = buildMetsDocument(agent, pid, "urn:some:foo");
        EpicurBuilder epicurBuilder = new EpicurBuilder()
                .frontpageUrlPattern(urlPattern)
                .mets(metsDocument);

        Epicur epicur = epicurBuilder.build();

        String expectedURL = "http://" + agent + ".example.com/id/" + pid;
        XMLAssert.assertXpathExists(
                String.format("//e:record/e:resource/e:identifier[@scheme='%s' and @type='%s' and @role='%s' and @origin='%s'" +
                                " and text()='%s']",
                        scheme, type, role, origin, expectedURL), marshal(epicur));
    }

    private Document buildMetsDocument(String agent, String pid, String urn) throws Exception {
        String mods = buildModsDocument(urn);
        String xml =
                "<m:mets OBJID=\"" + pid + "\" xmlns:m=\"http://www.loc.gov/METS/\">" +
                        "<m:metsHdr>" +
                        "<m:agent ROLE=\"EDITOR\" TYPE=\"ORGANIZATION\">" +
                        "<m:name>" + agent + "</m:name>" +
                        "</m:agent>" +
                        "</m:metsHdr>" +
                        "<m:dmdSec>" +
                        "<m:mdWrap MDTYPE=\"MODS\">" +
                        "<m:xmlData>" +
                        mods +
                        "</m:xmlData>" +
                        "</m:mdWrap>" +
                        "</m:dmdSec>" +
                        "</m:mets>";
        return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes()));
    }

    private String buildModsDocument(String urn) {
        return "<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">" +
                "<mods:identifier type=\"qucosa:urn\">" +
                urn +
                "</mods:identifier>" +
                "</mods:mods>";
    }

}
