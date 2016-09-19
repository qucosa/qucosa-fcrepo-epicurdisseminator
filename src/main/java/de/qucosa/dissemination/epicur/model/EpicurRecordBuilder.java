package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.IdentifierType;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.ResourceType;

import java.util.List;

public class EpicurRecordBuilder {

    private IdentifierType identifier;
    private List<ResourceType> resources;

    public EpicurRecordBuilder identifier(String scheme, String identifier) {
        this.identifier = new IdentifierType();
        this.identifier.setScheme(scheme);
        this.identifier.setValue(identifier);
        return this;
    }

    public RecordType getEpicurRecordInstance() {
        RecordType recordType = new RecordType();
        recordType.setIdentifier(identifier);
        return recordType;
    }
}
