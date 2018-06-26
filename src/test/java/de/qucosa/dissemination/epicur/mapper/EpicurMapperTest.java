package de.qucosa.dissemination.epicur.mapper;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.qucosa.dissemination.epicur.EpicurBuilderException;
import de.qucosa.dissemination.epicur.EpicurDissMapper;

public class EpicurMapperTest {
    
    @Test
    public void Find_epicur_tag_in_mapped_document() throws ParserConfigurationException, SAXException, IOException, JDOMException, JAXBException, EpicurBuilderException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document mets = db.parse(getClass().getResourceAsStream("/hochschulschrift_TEST.xml"));
        EpicurDissMapper mapper = new EpicurDissMapper("http://test.##AGENT##.qucosa.de/id/##PID##", "", "", true);
        Document epicurRes = mapper.transformEpicurDiss(mets);
        
        Assert.assertEquals("epicur", epicurRes.getDocumentElement().getTagName());
    }

    private XMLSerializer serializeEpicur(Document document) {
        OutputFormat outputFormat = new OutputFormat(document);
        outputFormat.setOmitXMLDeclaration(true);
        StringWriter stringWriter = new StringWriter();
        XMLSerializer serialize = new XMLSerializer(stringWriter, outputFormat);
        return serialize;
    }
}
