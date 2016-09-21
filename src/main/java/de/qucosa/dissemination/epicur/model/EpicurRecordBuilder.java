package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.FormatType;
import de.dnb.xepicur.IdentifierType;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.ResourceType;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EpicurRecordBuilder {
    private static final Namespace MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static final Namespace METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private static final XPathExpression XPATH_QUCOSA_URN = XPathFactory.instance()
            .compile("//mods:mods/mods:identifier[@type='qucosa:urn']", Filters.fpassthrough(), null, MODS);
    private static final XPathExpression XPATH_URN_FALLBACK = XPathFactory.instance()
            .compile("//mods:mods/mods:identifier[@type='urn']", Filters.fpassthrough(), null, MODS);
    private static final XPathExpression XPATH_FILE = XPathFactory.instance()
            .compile("//mets:fileGrp[@USE='DOWNLOAD']/mets:file", Filters.fpassthrough(), null, METS);

    private final Document metsDocument;
    private final Map<String, String> valuesMap = new HashMap<>();
    private final StrSubstitutor substitutor = new StrSubstitutor(valuesMap, "##", "##");

    private IdentifierType identifier;
    private List<ResourceType> resources = new LinkedList<>();

    public EpicurRecordBuilder(Document metsDocument) {
        this.metsDocument = metsDocument;
    }

    public EpicurRecordBuilder addIdentifier() throws MetadataElementMissing {
        Element identifierElement = (Element) XPATH_QUCOSA_URN.evaluateFirst(metsDocument);
        if (identifierElement == null) {
            identifierElement = (Element) XPATH_URN_FALLBACK.evaluateFirst(metsDocument);
        }
        if (identifierElement == null) {
            throw new MetadataElementMissing("No URN identifier found");
        }

        String urnString = identifierElement.getTextTrim();
        this.identifier = new IdentifierType();
        this.identifier.setScheme("urn:nbn:de");
        this.identifier.setValue(urnString);
        return this;
    }

    public EpicurRecordBuilder addResources(String transferUrlPattern, boolean transferUrlPidencode) throws Exception {
        String pid = metsDocument.getRootElement().getAttributeValue("OBJID");
        if (pid != null && !pid.isEmpty()) {
            if (transferUrlPidencode) {
                pid = URLEncoder.encode(pid, "UTF-8");
            }
            valuesMap.put("PID", pid);
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

            resources.add(resourceType(
                    identifierType(transferUrl),
                    formatType(extractMimetype(fileElement))));
        }
        return this;
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

    private ResourceType resourceType(IdentifierType fileIdentifier, FormatType formatType) {
        ResourceType resourceType = new ResourceType();
        resourceType.getIdentifierAndFormat().add(fileIdentifier);
        resourceType.getIdentifierAndFormat().add(formatType);
        return resourceType;
    }

    private FormatType formatType(String mimetype) {
        FormatType formatType = new FormatType();
        formatType.setScheme("imt");
        formatType.setValue(mimetype);
        return formatType;
    }

    private IdentifierType identifierType(String transferUrl) {
        IdentifierType fileIdentifier = new IdentifierType();
        fileIdentifier.setScheme("url");
        fileIdentifier.setTarget("transfer");
        fileIdentifier.setOrigin("original");
        fileIdentifier.setValue(transferUrl);
        return fileIdentifier;
    }


    public RecordType build() {
        RecordType recordType = new RecordType();
        recordType.setIdentifier(identifier);
        recordType.getResource().addAll(resources);
        return recordType;
    }

}
