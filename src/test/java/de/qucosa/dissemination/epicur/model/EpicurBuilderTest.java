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
    public void Build_bare_epicur_dissemination_without_exceptions() {
        EpicurBuilder epicurBuilder = new EpicurBuilder();
        Epicur epicur = epicurBuilder.build();
        assertNotNull(epicur);
    }

    @Test
    public void Administrative_section_contains_update_status() {
        EpicurBuilder epicurBuilder = new EpicurBuilder();
        epicurBuilder.buildAdministrativeDataSection(UpdateStatus.urn_new);

        Epicur epicur = epicurBuilder.build();

        UpdateStatusType updateStatusType = epicur.getAdministrativeData().getDelivery().getUpdateStatus();
        assertEquals(UpdateStatus.urn_new.name(), updateStatusType.getType());
    }

    @Test
    public void Record_with_transfer_identifier_has_qucosa_URN() throws Exception {
        String scheme = "urn:nbn:de";
        String urn = scheme + ":foo-4711";
        Document metsDocument = buildMetsDocument(urn);
        EpicurBuilder epicurBuilder = new EpicurBuilder();
        epicurBuilder.addRecord(new EpicurRecordBuilder(metsDocument).addIdentifier().build());

        Epicur epicur = epicurBuilder.build();

        XMLAssert.assertXpathExists(
                "//e:record/e:identifier[@scheme='" + scheme + "' and text()='" + urn + "']",
                marshal(epicur));
    }

    private Document buildMetsDocument(String urn) throws Exception {
        String mods = buildModsDocument(urn);
        String xml =
                "<m:mets xmlns:m=\"http://www.loc.gov/METS/\">" +
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
