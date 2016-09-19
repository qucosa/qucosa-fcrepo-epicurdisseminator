package de.qucosa.dissemination.epicur.model;

import de.dnb.xepicur.AdministrativeDataType;
import de.dnb.xepicur.DeliveryType;
import de.dnb.xepicur.Epicur;
import de.dnb.xepicur.RecordType;
import de.dnb.xepicur.UpdateStatusType;

public class EpicurBuilder {

    private Epicur epicur;

    public EpicurBuilder buildAdministrativeDataSection(UpdateStatus updateStatus) {
        UpdateStatusType updateStatusType = new UpdateStatusType();
        updateStatusType.setType(updateStatus.name());

        DeliveryType deliveryType = new DeliveryType();
        deliveryType.setUpdateStatus(updateStatusType);

        AdministrativeDataType administrativeDataType = new AdministrativeDataType();
        administrativeDataType.setDelivery(deliveryType);

        epicurInstance().setAdministrativeData(administrativeDataType);

        return this;
    }

    public EpicurBuilder addRecord(RecordType record) {
        epicurInstance().getRecord().add(record);
        return this;
    }

    public Epicur getEpicurInstance() {
        return epicur;
    }

    private Epicur epicurInstance() {
        if (epicur == null) {
            epicur = new Epicur();
        }
        return epicur;
    }

}
