package de.qucosa.dissemination.epicur.mapper;

import de.qucosa.dissemination.epicur.EpicurBuilderException;
import de.qucosa.dissemination.epicur.EpicurDissMapper;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class EpicurMapperTest {
    
    @Test
    public void Find_epicur_tag_in_mapped_document() throws ParserConfigurationException, SAXException, IOException, JDOMException, JAXBException, EpicurBuilderException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document mets = db.parse(getClass().getResourceAsStream("/qucosa-48666.xml"));
        EpicurDissMapper mapper = new EpicurDissMapper("http://test.##AGENT##.qucosa.de/id/##PID##", "", "", true);
        Document epicurRes = mapper.transformEpicurDiss(mets);
        
        Assert.assertEquals("epicur", epicurRes.getDocumentElement().getTagName());
    }
}
