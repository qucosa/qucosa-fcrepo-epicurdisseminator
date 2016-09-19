/*
 * Copyright 2016 Saxon State and University Library Dresden (SLUB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qucosa.dissemination.epicur.servlet;

import de.dnb.xepicur.Epicur;
import de.qucosa.dissemination.epicur.model.EpicurBuilder;
import de.qucosa.dissemination.epicur.model.EpicurRecordBuilder;
import de.qucosa.dissemination.epicur.model.UpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;

public class EpicurDisseminationServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private JAXBContext jaxbContext;
    private Marshaller marshaller;

    @Override
    public void init() throws ServletException {
        try {
            jaxbContext = JAXBContext.newInstance(Epicur.class);
            marshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            EpicurBuilder epicurBuilder = new EpicurBuilder()
                    .buildAdministrativeDataSection(UpdateStatus.urn_new)
                    .addRecord(new EpicurRecordBuilder()
                            .identifier("urn:nbn:de", "urn:nbn:de:bsz:14-qucosa-201469")
                            .getEpicurRecordInstance());

            Epicur epicur = epicurBuilder.getEpicurInstance();

            marshaller.marshal(epicur, resp.getWriter());
        } catch (Exception e) {
            log.error(e.getMessage());
            resp.sendError(500);
        }
    }

}
