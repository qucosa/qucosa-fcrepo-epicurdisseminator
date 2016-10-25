package de.qucosa.dissemination.epicur.testsupport;

import de.dnb.xepicur.Epicur;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.HashMap;

public class XmlTestsupport {
    private static Marshaller epicurMarshaller;

    @BeforeClass
    public static void setup() throws JAXBException {
        epicurMarshaller = JAXBContext.newInstance(Epicur.class).createMarshaller();
        XMLUnit.setXpathNamespaceContext(
                new SimpleNamespaceContext(
                        new HashMap<String, String>() {{
                            put("e", "urn:nbn:de:1111-2004033116");
                        }}
                )
        );
    }

    protected String marshal(Object xmlObject) throws JAXBException {
        StringWriter resultWriter = new StringWriter();
        epicurMarshaller.marshal(xmlObject, resultWriter);
        return resultWriter.getBuffer().toString();
    }
}
