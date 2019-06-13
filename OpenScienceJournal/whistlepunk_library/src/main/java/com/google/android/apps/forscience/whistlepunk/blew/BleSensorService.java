package com.google.android.apps.forscience.whistlepunk.blew;

import java.util.EnumMap;
import java.util.UUID;

public class BleSensorService {
    //SensorTag 2560 Sensor Services
    public static final UUID
            LUX_SERV = UUID.fromString("f000aa70-0451-4000-b000-000000000000"),
            LUX_READ = UUID.fromString("f000aa71-0451-4000-b000-000000000000"),
            LUX_WRITE = UUID.fromString("f000aa72-0451-4000-b000-000000000000"),

    TEMP_SERV = UUID.fromString("f000aa00-0451-4000-b000-000000000000"),
            TEMP_READ = UUID.fromString("f000aa01-0451-4000-b000-000000000000"),
            TEMP_WRITE = UUID.fromString("f000aa02-0451-4000-b000-000000000000"),

    ACC_SERV = UUID.fromString("f000aa80-0451-4000-b000-000000000000"),
            ACC_READ = UUID.fromString("f000aa81-0451-4000-b000-000000000000"),
            ACC_WRITE = UUID.fromString("f000aa82-0451-4000-b000-000000000000"),

    HUM_SERV = UUID.fromString("f000aa20-0451-4000-b000-000000000000"),
            HUM_READ = UUID.fromString("f000aa21-0451-4000-b000-000000000000"),
            HUM_WRITE = UUID.fromString("f000aa22-0451-4000-b000-000000000000"),

    BAR_SERV = UUID.fromString("f000aa40-0451-4000-b000-000000000000"),
            BAR_READ = UUID.fromString("f000aa41-0451-4000-b000-000000000000"),
            BAR_WRITE = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
}
