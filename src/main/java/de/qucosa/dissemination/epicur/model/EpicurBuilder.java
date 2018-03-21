package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.AdministrativeDataType;
import de.dnb.xepicur.DeliveryType;
import de.dnb.xepicur.Epicur;
import de.dnb.xepicur.FormatType;
import de.dnb.xepicur.IdentifierType;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.ResourceType;
import de.dnb.xepicur.UpdateStatusType;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EpicurBuilder {

    private static final Namespace MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static final Namespace METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private static final XPathExpression XPATH_QUCOSA_URN = XPathFactory.instance()
            .compile("//mods:mods/mods:identifier[@type='qucosa:urn']", Filters.fpassthrough(), null, MODS);
    private static final XPathExpression XPATH_URN_FALLBACK = XPathFactory.instance()
            .compile("//mods:mods/mods:identifier[@type='urn']", Filters.fpassthrough(), null, MODS);
    private static final XPathExpression XPATH_FILE = XPathFactory.instance()
            .compile("//mets:fileGrp[@USE='DOWNLOAD']/mets:file", Filters.fpassthrough(), null, METS);
    private static final XPathExpression XPATH_AGENT = XPathFactory.instance()
            .compile("//mets:agent[@ROLE='EDITOR' and @TYPE='ORGANIZATION']/mets:name", Filters.fpassthrough(), null, METS);

    private boolean encodePid = false;
    private String frontpageUrlPattern;
    private Document metsDocument;
    private String transferUrlPattern;
    private UpdateStatus updateStatus = UpdateStatus.urn_new;
    private Map<String, String> agentNameSubstitutions = Collections.emptyMap();

    public EpicurBuilder transferUrlPattern(String pattern) {
        transferUrlPattern = pattern;
        return this;
    }

    public EpicurBuilder frontpageUrlPattern(String pattern) {
        frontpageUrlPattern = pattern;
        return this;
    }

    public EpicurBuilder agentNameSubstitutions(Map<String, String> agentNameSubstitutions) {
        this.agentNameSubstitutions = agentNameSubstitutions;
        return this;
    }

    public EpicurBuilder encodePid(boolean encode) {
        encodePid = encode;
        return this;
    }

    public EpicurBuilder updateStatus(UpdateStatus status) {
        updateStatus = status;
        return this;
    }

    public EpicurBuilder mets(Document document) {
        metsDocument = document;
        return this;
    }

    public Epicur build() throws EpicurBuilderException {
        if (metsDocument == null) {
            throw new EpicurBuilderException("No METS document set");
        }

        Epicur epicur = new Epicur();
        try {
            epicur.setAdministrativeData(constructAdministrativeDataSection(updateStatus));
            epicur.getRecord().add(constructRecord(metsDocument));
        } catch (MetadataElementMissing metadataElementMissing) {
            throw new EpicurBuilderException("Error while building Epicur record", metadataElementMissing);
        }
        return epicur;
    }

    private IdentifierType extractRecordIdentifier(Document metsDocument) throws MetadataElementMissing {
        Element identifierElement = (Element) XPATH_QUCOSA_URN.evaluateFirst(metsDocument);
        if (identifierElement == null) {
            identifierElement = (Element) XPATH_URN_FALLBACK.evaluateFirst(metsDocument);
        }
        if (identifierElement == null) {
            throw new MetadataElementMissing("No URN identifier found");
        }
        String urnString = identifierElement.getTextTrim();

        IdentifierType identifier = new IdentifierType();
        identifier.setScheme("urn:nbn:de");
        identifier.setValue(urnString);
        return identifier;
    }

    private RecordType constructRecord(Document metsDocument) throws MetadataElementMissing {
        RecordType record = new RecordType();
        record.setIdentifier(extractRecordIdentifier(metsDocument));

        List<ResourceType> resources = constructResources();
        record.getResource().addAll(resources);

        return record;
    }

    private List<ResourceType> constructResources() throws MetadataElementMissing {
        Map<String, String> valuesMap = new HashMap<>();
        StrSubstitutor substitutor = new StrSubstitutor(valuesMap, "##", "##");
        List<ResourceType> resources = new LinkedList<>();

        String pid = extractObjectPID(metsDocument);
        if (pid != null && !pid.isEmpty()) valuesMap.put("PID", pid);

        String agent = extractAgentName(metsDocument);
        if (agent != null && !agent.isEmpty()) valuesMap.put("AGENT", agent);

        if (frontpageUrlPattern != null && !frontpageUrlPattern.isEmpty()) {
            String frontpageUrl = substitutor.replace(frontpageUrlPattern);
            resources.add(constructResourceType(
                    identifierFrontpageType(frontpageUrl),
                    formatType("text/html")));
        }

        @SuppressWarnings("unchecked")
        List<Element> fileElements = XPATH_FILE.evaluate(metsDocument);
        for (Element fileElement : fileElements) {
            String dsid = fileElement.getAttributeValue("ID");
            if (dsid != null && !dsid.isEmpty()) {
                valuesMap.put("DSID", dsid);
            }

            String transferUrl;
            if (transferUrlPattern != null && !transferUrlPattern.isEmpty()) {
                transferUrl = substitutor.replace(transferUrlPattern);
            } else {
                transferUrl = extractTransferUrl(fileElement);
            }

            resources.add(constructResourceType(
                    identifierTransferTargetType(transferUrl),
                    formatType(extractMimetype(fileElement))));
        }

        return resources;
    }

    private ResourceType constructResourceType(IdentifierType identifierType, FormatType formatType) {
        ResourceType resourceType = new ResourceType();
        resourceType.getIdentifierAndFormat().add(identifierType);
        resourceType.getIdentifierAndFormat().add(formatType);
        return resourceType;
    }

    private FormatType formatType(String mimetype) {
        FormatType formatType = new FormatType();
        formatType.setScheme("imt");
        formatType.setValue(mimetype);
        return formatType;
    }

    private IdentifierType identifierFrontpageType(String frontpageUrl) {
        IdentifierType frontpageIdentifier = new IdentifierType();
        frontpageIdentifier.setScheme("url");
        frontpageIdentifier.setType("frontpage");
        frontpageIdentifier.setRole("primary");
        frontpageIdentifier.setOrigin("original");
        frontpageIdentifier.setValue(frontpageUrl);
        return frontpageIdentifier;
    }

    private IdentifierType identifierTransferTargetType(String transferUrl) {
        IdentifierType transferIdentifier = new IdentifierType();
        transferIdentifier.setScheme("url");
        transferIdentifier.setTarget("transfer");
        transferIdentifier.setOrigin("original");
        transferIdentifier.setValue(transferUrl);
        return transferIdentifier;
    }

    private String extractMimetype(Element fileElement) throws MetadataElementMissing {
        String mimetype = fileElement.getAttributeValue("MIMETYPE");
        if (mimetype == null || mimetype.isEmpty()) {
            throw new MetadataElementMissing("Missing 'mimetype' attribute on <mets:file> element");
        }
        return mimetype;
    }

    private String extractTransferUrl(Element fileElement) throws MetadataElementMissing {
        Element fLocat = fileElement.getChild("FLocat", METS);
        if (fLocat == null) {
            throw new MetadataElementMissing("Missing <mets:FLocat> element");
        }
        String href = fLocat.getAttributeValue("href", XLINK);
        if (href == null) {
            throw new MetadataElementMissing("Missing 'href' attribute on <mets:FLocat> element");
        }
        return href;
    }

    private String extractObjectPID(Document metsDocument) {
        String pid = metsDocument.getRootElement().getAttributeValue("OBJID");
        if (pid != null && !pid.isEmpty() && encodePid) {
            try {
                pid = URLEncoder.encode(pid, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
                // UTF-8 is always supported
            }
        }
        return pid;
    }

    private String extractAgentName(Document metsDocument) {
        Element agentNameElement = (Element) XPATH_AGENT.evaluateFirst(metsDocument);
        if (agentNameElement != null) {
            String agentName = agentNameElement.getTextTrim();
            if (agentNameSubstitutions.containsKey(agentName)) {
                return agentNameSubstitutions.get(agentName);
            } else {
                return agentName;
            }
        }
        return null;
    }

    private AdministrativeDataType constructAdministrativeDataSection(UpdateStatus updateStatus) {
        UpdateStatusType updateStatusType = new UpdateStatusType();
        updateStatusType.setType(updateStatus.name());

        DeliveryType deliveryType = new DeliveryType();
        deliveryType.setUpdateStatus(updateStatusType);

        AdministrativeDataType administrativeDataType = new AdministrativeDataType();
        administrativeDataType.setDelivery(deliveryType);

        return administrativeDataType;
    }

}
