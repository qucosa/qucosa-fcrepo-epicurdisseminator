package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.FormatType;
import de.dnb.xepicur.IdentifierType;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.ResourceType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.LinkedList;
import java.util.List;

public class EpicurRecordBuilder {


    private static final Namespace MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static final Namespace METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    private static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.instance();
    private static final XPathExpression XPATH_IDENTIFIER =
            X_PATH_FACTORY.compile("//mods:mods/mods:identifier[@type='qucosa:urn']", Filters.fpassthrough(), null, MODS);
    private static final XPathExpression XPATH_FILE =
            X_PATH_FACTORY.compile("//mets:fileGrp[@USE='DOWNLOAD']/mets:file", Filters.fpassthrough(), null, METS);

    private final Document metsDocument;

    private IdentifierType identifier;
    private List<ResourceType> resources = new LinkedList<>();

    public EpicurRecordBuilder(Document metsDocument) {
        this.metsDocument = metsDocument;
    }

    public EpicurRecordBuilder addIdentifier() {
        Element identifierElement = (Element) XPATH_IDENTIFIER.evaluateFirst(metsDocument);
        String urnString = identifierElement.getTextTrim();
        this.identifier = new IdentifierType();
        this.identifier.setScheme("urn:nbn:de");
        this.identifier.setValue(urnString);
        return this;
    }

    public EpicurRecordBuilder addResources() throws Exception {
        @SuppressWarnings("unchecked")
        List<Element> fileElements = XPATH_FILE.evaluate(metsDocument);
        for (Element fileElement : fileElements) {
            resources.add(resourceType(
                    identifierType(extractTransferUrl(fileElement)),
                    formatType(extractMimetype(fileElement))));
        }
        return this;
    }

    private String extractMimetype(Element fileElement) throws MetsFormatException {
        String mimetype = fileElement.getAttributeValue("MIMETYPE");
        if (mimetype == null || mimetype.isEmpty()) {
            throw new MetsFormatException("Missing 'mimetype' attribute on <mets:file> element");
        }
        return mimetype;
    }

    private String extractTransferUrl(Element fileElement) throws MetsFormatException {
        Element fLocat = fileElement.getChild("FLocat", METS);
        if (fLocat == null) {
            throw new MetsFormatException("Missing <mets:FLocat> element");
        }
        String href = fLocat.getAttributeValue("href", XLINK);
        if (href == null) {
            throw new MetsFormatException("Missing 'href' attribute on <mets:FLocat> element");
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
