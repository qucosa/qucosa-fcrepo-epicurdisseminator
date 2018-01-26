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
import de.qucosa.dissemination.epicur.model.UpdateStatus;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class EpicurDisseminationServlet extends HttpServlet {
    private static final String PARAM_FRONTPAGE_URL_PATTERN = "frontpage.url.pattern";
    private static final String REQUEST_PARAM_METS_URL = "metsurl";
    private static final String PARAM_TRANSFER_URL_PATTERN = "transfer.url.pattern";
    private static final String PARAM_TRANSFER_URL_PIDENCODE = "transfer.url.pidencode";
    private static final String XEPICUR_SCHEMA_LOCATION =
            "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private CloseableHttpClient httpClient;
    private GenericObjectPool<Marshaller> marshallerPool;

    @Override
    public void init() {
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .build();

        marshallerPool = new GenericObjectPool<>(new BasePooledObjectFactory<Marshaller>() {
            @Override
            public Marshaller create() throws Exception {
                Marshaller marshaller = JAXBContext.newInstance(Epicur.class).createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XEPICUR_SCHEMA_LOCATION);
                return marshaller;
            }

            @Override
            public PooledObject<Marshaller> wrap(Marshaller marshaller) {
                return new DefaultPooledObject<>(marshaller);
            }
        });
    }

    @Override
    public void destroy() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Error closing HTTP client: " + e.getMessage());
        }

        marshallerPool.clear();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        URI metsDocumentUri;
        boolean transferUrlPidencode;
        String transferUrlPattern;
        String frontpageUrlPattern;
        try {
            String reqParameter = req.getParameter(REQUEST_PARAM_METS_URL);
            if (reqParameter == null || reqParameter.isEmpty()) {
                resp.sendError(SC_BAD_REQUEST, "Missing parameter '" + REQUEST_PARAM_METS_URL + "'");
                return;
            }
            metsDocumentUri = URI.create(reqParameter);

            ServletConfig config = getServletConfig();

            transferUrlPattern = getParameterValue(config, PARAM_TRANSFER_URL_PATTERN);
            frontpageUrlPattern = getParameterValue(config, PARAM_FRONTPAGE_URL_PATTERN);
            transferUrlPidencode = isParameterSet(config, PARAM_TRANSFER_URL_PIDENCODE);

        } catch (Exception e) {
            log.error(e.getMessage());
            resp.sendError(SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try (CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(metsDocumentUri))) {
            if (httpResponse.getStatusLine().getStatusCode() != SC_OK) {
                resp.sendError(
                        httpResponse.getStatusLine().getStatusCode(),
                        httpResponse.getStatusLine().getReasonPhrase());
                return;
            }
            Document metsDocument = new SAXBuilder().build(httpResponse.getEntity().getContent());

            EpicurBuilder epicurBuilder = new EpicurBuilder()
                    .encodePid(transferUrlPidencode)
                    .frontpageUrlPattern(frontpageUrlPattern)
                    .mets(metsDocument)
                    .transferUrlPattern(transferUrlPattern)
                    .updateStatus(UpdateStatus.urn_new);
            Epicur epicur = epicurBuilder.build();

            StringWriter stringWriter = new StringWriter();

            Marshaller marshaller = marshallerPool.borrowObject();
            marshaller.marshal(epicur, stringWriter);
            marshallerPool.returnObject(marshaller);

            resp.setCharacterEncoding(Charset.defaultCharset().name());
            resp.setContentType("application/xml");
            resp.getOutputStream().print(stringWriter.toString());

        } catch (Exception e) {
            log.error("Error while writing XML content: " + e.getMessage());
            if (!resp.isCommitted()) {
                resp.sendError(SC_INTERNAL_SERVER_ERROR);
            } else {
                log.warn("Response already committed. Cannot send error code.");
            }
        }
    }

    private boolean isParameterSet(ServletConfig config, String name) {
        String p = config.getServletContext().getInitParameter(name);
        return p != null
                && !p.isEmpty() || (System.getProperty(name) != null);
    }

    private String getParameterValue(ServletConfig config, String name) {
        String v = config.getServletContext().getInitParameter(name);
        if (v == null || v.isEmpty()) {
            v = System.getProperty(name);
        }
        return v;
    }
}
