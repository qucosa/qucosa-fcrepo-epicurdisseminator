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

package de.qucosa.dissemination.epicur.mapper;

import de.qucosa.dissemination.epicur.EpicurBuilderException;
import de.qucosa.dissemination.epicur.EpicurDissMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class EpicurMapperTest {
    private Logger logger = LoggerFactory.getLogger(EpicurMapperTest.class);
    
    @Test
    public void Find_epicur_tag_in_mapped_document() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = null;
        Document mets = null;
        Document epicurRes = null;

        try {
            db = dbf.newDocumentBuilder();
            mets = db.parse(getClass().getResourceAsStream("/hochschulschrift_TEST.xml"));
            EpicurDissMapper mapper = new EpicurDissMapper("http://test.##AGENT##.qucosa.de/id/##PID##", "", "", true);
            epicurRes = mapper.transformEpicurDiss(mets);
        } catch (SAXException | ParserConfigurationException e) {
            logger.error("Cannot parse mets xml.", e);
        } catch (IOException e) {
            logger.error("", e);
        } catch (JAXBException | EpicurBuilderException | TransformerException e) {
            logger.error("Connaot transform epicur dissemination.", e);
        }


        Assert.assertEquals("epicur", epicurRes.getDocumentElement().getTagName());
    }
}
