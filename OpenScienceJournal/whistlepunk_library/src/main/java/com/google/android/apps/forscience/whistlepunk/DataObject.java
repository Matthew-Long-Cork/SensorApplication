package com.google.android.apps.forscience.whistlepunk;
//
// added: this class was added to accommodate for the sending of data to the database.
//
public class DataObject {

    String Id;
    Float dataValue;

    // constructor declaration
    public DataObject( String sensorId, Float dataValue){
        this.Id = sensorId;
        this.dataValue = dataValue;
    }

    public String getDataId() { return Id; }

    public void setDataId(String sensorId) {
        this.Id = sensorId;
    }

    public Float getDataValue() {
        return dataValue;
    }

    public void setDataValue(Float dataValue) {
        this.dataValue = dataValue;
    }
}
