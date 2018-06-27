/*
 * Copyright (C) 2017 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.dissemination.epicur;

import de.dnb.xepicur.Epicur;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class EpicurDissMapper {
    private Logger logger = LoggerFactory.getLogger(EpicurDissMapper.class);

    private org.jdom2.Document metsDoc = null;
    
    private EpicurBuilder epicurBuilder = new EpicurBuilder();

    private String transferUrlPattern;

    private String frontpageUrlPattern;

    private boolean transferUrlPidencode;

    private static final String XEPICUR_SCHEMA_LOCATION = "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd";

    private Map<String, String> agentNameSubstitutions;

    public EpicurDissMapper(String transferUrlPattern, String frontpageUrlPattern, String agentNameSubstitutions, boolean transferUrlPidencode) {
        this.transferUrlPattern = transferUrlPattern;
        this.frontpageUrlPattern = frontpageUrlPattern;
        this.agentNameSubstitutions = decodeSubstitutions(agentNameSubstitutions);
        this.transferUrlPidencode = transferUrlPidencode;
    }

    public Document transformEpicurDiss(Document metsDoc) throws JAXBException, EpicurBuilderException,
            ParserConfigurationException, SAXException, IOException {

        try {
            this.metsDoc = new SAXBuilder().build(jdom2Build(metsDoc));
        } catch (JDOMException e) {
            logger.error("Cannot JDOM 2 document build.", e);
        }

        epicurBuilder
            .encodePid(transferUrlPidencode)
            .agentNameSubstitutions(agentNameSubstitutions)
            .frontpageUrlPattern(frontpageUrlPattern)
            .mets(this.metsDoc)
            .transferUrlPattern(transferUrlPattern)
            .updateStatus(UpdateStatus.urn_new);
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document epicurDoc;
        StringWriter stringWriter = new StringWriter();
        Marshaller marshaller = JAXBContext.newInstance(Epicur.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XEPICUR_SCHEMA_LOCATION);
        marshaller.marshal(epicurBuilder.build(), stringWriter);

        epicurDoc = db.parse(new ByteArrayInputStream(stringWriter.toString().getBytes("UTF-8")));

        return epicurDoc;
    }

    private InputStream jdom2Build(Document metsDoc) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = new OutputFormat(metsDoc);
        XMLSerializer serializer = new XMLSerializer(outputStream, format);
        serializer.serialize(metsDoc);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private Map<String, String> decodeSubstitutions(String parameterValue) {
        HashMap<String, String> result = new HashMap<String, String>();

        if (parameterValue != null && !parameterValue.isEmpty()) {

            for (String substitution : parameterValue.split(";")) {
                String[] s = substitution.split("=");
                result.put(s[0].trim(), s[1].trim());
            }
        }

        return result;
    }
}
