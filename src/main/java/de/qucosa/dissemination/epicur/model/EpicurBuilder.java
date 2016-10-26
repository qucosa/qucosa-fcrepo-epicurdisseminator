package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.AdministrativeDataType;
import de.dnb.xepicur.DeliveryType;
import de.dnb.xepicur.Epicur;
import de.dnb.xepicur.UpdateStatusType;
import org.jdom2.Document;

public class EpicurBuilder {

    private boolean encodePid = false;
    private String frontdoorUrlPattern;
    private Document metsDocument;
    private String transferUrlPattern;
    private UpdateStatus updateStatus = UpdateStatus.urn_new;

    public EpicurBuilder transferUrlPattern(String pattern) {
        transferUrlPattern = pattern;
        return this;
    }

    public EpicurBuilder frontdoorUrlPattern(String pattern) {
        frontdoorUrlPattern = pattern;
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

    public Epicur build() throws Exception {
        if (metsDocument == null) {
            throw new EpicurBuilderException("No METS document set");
        }

        Epicur epicur = new Epicur();
        epicur.getRecord().add(
                new EpicurRecordBuilder(metsDocument)
                        .addIdentifier()
                        .addResources(transferUrlPattern, frontdoorUrlPattern, encodePid)
                        .build());
        buildAdministrativeDataSection(epicur, updateStatus);
        return epicur;
    }

    private void buildAdministrativeDataSection(Epicur epicur, UpdateStatus updateStatus) {
        UpdateStatusType updateStatusType = new UpdateStatusType();
        updateStatusType.setType(updateStatus.name());

        DeliveryType deliveryType = new DeliveryType();
        deliveryType.setUpdateStatus(updateStatusType);

        AdministrativeDataType administrativeDataType = new AdministrativeDataType();
        administrativeDataType.setDelivery(deliveryType);

        epicur.setAdministrativeData(administrativeDataType);
    }

}
