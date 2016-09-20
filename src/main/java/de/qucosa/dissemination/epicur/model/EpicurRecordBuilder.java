package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.IdentifierType;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.ResourceType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.List;

public class EpicurRecordBuilder {

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.instance();
    private static final XPathExpression X_PATH_EXPRESSION = X_PATH_FACTORY.compile("/mods:mods/mods:identifier[@type='qucosa:urn']",
            Filters.fpassthrough(), null, Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3"));
    private final Document metsDocument;

    private IdentifierType identifier;
    private List<ResourceType> resources;

    public EpicurRecordBuilder(Document metsDocument) {
        this.metsDocument = metsDocument;
    }

    public EpicurRecordBuilder addIdentifier() {
        Element identifierElement = (Element) X_PATH_EXPRESSION.evaluateFirst(metsDocument);
        String urnString = identifierElement.getTextTrim();
        this.identifier = new IdentifierType();
        this.identifier.setScheme("urn:nbn:de");
        this.identifier.setValue(urnString);
        return this;
    }

    public EpicurRecordBuilder addResources() {
        return this;
    }

    public RecordType build() {
        RecordType recordType = new RecordType();
        recordType.setIdentifier(identifier);
        return recordType;
    }

}
