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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
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
import java.net.URI;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class EpicurDisseminationServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private CloseableHttpClient httpClient;
    private JAXBContext jaxbContext;
    private Marshaller marshaller;

    @Override
    public void init() throws ServletException {
        try {
            jaxbContext = JAXBContext.newInstance(Epicur.class);
            marshaller = jaxbContext.createMarshaller();
            httpClient = HttpClientBuilder
                    .create()
                    .setConnectionManager(new PoolingHttpClientConnectionManager())
                    .build();
        } catch (JAXBException e) {
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    public void destroy() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Error closing HTTP client: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CloseableHttpResponse httpResponse = null;
        try {
            String reqParameter = req.getParameter("metsurl");
            if (reqParameter == null || reqParameter.isEmpty()) {
                resp.sendError(SC_BAD_REQUEST, "Missing 'metsurl' parameter");
                return;
            }
            URI metsDocumentUri = URI.create(reqParameter);

            httpResponse = httpClient.execute(new HttpGet(metsDocumentUri));
            if (httpResponse.getStatusLine().getStatusCode() != SC_OK) {
                resp.sendError(
                        httpResponse.getStatusLine().getStatusCode(),
                        httpResponse.getStatusLine().getReasonPhrase());
                return;
            }
            Document metsDocument = new SAXBuilder().build(httpResponse.getEntity().getContent());

            EpicurBuilder epicurBuilder = new EpicurBuilder()
                    .buildAdministrativeDataSection(UpdateStatus.urn_new)
                    .addRecord(new EpicurRecordBuilder(metsDocument)
                            .addIdentifier()
                            .addResources()
                            .build());

            Epicur epicur = epicurBuilder.build();

            marshaller.marshal(epicur, resp.getWriter());
        } catch (Exception e) {
            log.error(e.getMessage());
            resp.sendError(SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (httpResponse != null) try {
                httpResponse.close();
            } catch (Exception ignored) {
            }
        }
    }

}
