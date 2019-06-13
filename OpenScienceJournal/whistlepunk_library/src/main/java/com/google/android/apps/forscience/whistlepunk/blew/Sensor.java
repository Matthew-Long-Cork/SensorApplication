package com.google.android.apps.forscience.whistlepunk.blew;

import com.google.android.apps.forscience.whistlepunk.DataObject;

import java.util.UUID;
import static com.google.android.apps.forscience.whistlepunk.blew.BleSensorService.*;
import static java.lang.Math.floorDiv;
import static java.lang.Math.pow;


public enum Sensor{
    LUX(LUX_SERV, LUX_WRITE, LUX_READ){
        @Override
        public String parseString(byte[] value){
            Integer sfloat = shortUnsignedAtOffset(value, 0);
            int mantissa = sfloat & 0x0FFF;
            int exponent = (sfloat >> 12) & 0xFF;

            double magnitude = pow(2.0f, exponent);
            double output = (mantissa * magnitude) / 100.0f;

            return "SensTag/Luxometer : " + output;
        }
    },
    TEMP_AMB(TEMP_SERV, TEMP_WRITE, TEMP_READ){
        public String sensorID;
        @Override
        public String parseJson(byte[] values){
            return "{AmbienceTemperature: " + parseFloat(values) + "}";
        }

        @Override
        public float parseFloat(byte[] value) {
            return (float)(shortSignedAtOffset(value, 2) / 128.0);
        }
    },
    TEMP_OBJ(TEMP_SERV, TEMP_WRITE, TEMP_READ){
        public String sensorID;
        @Override
        public String parseJson(byte[] values){
           return "{ObjectTemperature: " + parseFloat(values) + "}";
        }

        @Override
        public float parseFloat(byte[] value) {
            return (float)(shortSignedAtOffset(value, 0) / 128.0);
        }
    },
    /*ACC(ACC_SERV, ACC_WRITE, ACC_READ){
       *//* @Override Nixya Ne Ponyatno
        public String convert(final byte[] value) {
            *//**//*
             * The accelerometer has the range [-2g, 2g] with unit (1/64)g.
             * To convert from unit (1/64)g to unit g we divide by 64.
             * (g = 9.81 m/s^2)
             * The z value is multiplied with -1 to coincide with how we have arbitrarily defined the positive y direction. (illustrated by the apps accelerometer
             * image)
             *//**//*
            DeviceActivity da = DeviceActivity.getInstance();

            if (da.isSensorTag2()) {
                // Range 8G
                final float SCALE = (float) 4096.0;

                int x = (value[0]<<8) + value[1];
                int y = (value[2]<<8) + value[3];
                int z = (value[4]<<8) + value[5];
                return new Point3D(x / SCALE, y / SCALE, z / SCALE);
            } else {
                Point3D v;
                Integer x = (int) value[0];
                Integer y = (int) value[1];
                Integer z = (int) value[2] * -1;

                if (da.firmwareRevision().contains("1.5"))
                {
                    // Range 8G
                    final float SCALE = (float) 64.0;
                    v = new Point3D(x / SCALE, y / SCALE, z / SCALE);
                } else {
                    // Range 2G
                    final float SCALE = (float) 16.0;
                    v = new Point3D(x / SCALE, y / SCALE, z / SCALE);
                }
                return v;
            }
        }*//*
    },*/
    HUM(HUM_SERV, HUM_WRITE, HUM_READ){
        @Override
        public String parseString(byte[] value) {
            int hum = shortUnsignedAtOffset(value, 2);
            hum -= (hum % 4);
            float output = (-6f) + 125f * (hum / 65535f);

            return "{\"Humidity Sensor\" : \"" + output + "\"}";
        }
    },
    BAR(BAR_SERV, BAR_WRITE, BAR_READ){
        @Override
        public String parseString(byte[] value) {
            return  "Barometer: " + parseFloat(value);
        }

        @Override
        public float parseFloat(byte[] value){
            if (value.length > 4) {
                int val = twentyFourBitUnsignedAtOffset(value, 2);
                double output = (double) val / 100.0;
                return (float)output;
            }
            else {
                int mantissa;
                int exponent;
                Integer sfloat = shortUnsignedAtOffset(value, 2);

                mantissa = sfloat & 0x0FFF;
                exponent = (sfloat >> 12) & 0xFF;

                double output;
                double magnitude = pow(2.0f, exponent);
                output = (mantissa * magnitude) / 100.0f;

                return (float) output;
            }
        }
    };

    private UUID serv, write, read;

    Sensor(UUID serv, UUID write, UUID read) {
        this.serv = serv;
        this.write = write;
        this.read = read;
    }

    public UUID getServ() {
        return serv;
    }

    public void setServ(UUID serv) {
        this.serv = serv;
    }

    public UUID getWrite() {
        return write;
    }

    public void setWrite(UUID write) {
        this.write = write;
    }

    public UUID getRead() {
        return read;
    }

    public void setRead(UUID read) {
        this.read = read;
    }

    private static Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset+1]; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset+1] & 0xFF;
        return (upperByte << 8) + lowerByte;
    }
    private static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer mediumByte = (int) c[offset+1] & 0xFF;
        Integer upperByte = (int) c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    public String parseJson(byte[] value) {
        throw new UnsupportedOperationException("Error: Override this method.");
    }

    public String parseString(byte[] value) {
        throw new UnsupportedOperationException("Error: Override this method.");
    }

    public float parseFloat(byte[] value){
        throw new UnsupportedOperationException("Error: Override this method.");
    }

    public DataObject parseDataObject(byte[] value){
        throw new UnsupportedOperationException("Error: Override this method.");
    }

}
