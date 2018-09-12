package org.sofwerx.torgi.gnss;

/**
 * The GNSS constellation type
 */
public enum Constellation {
    Unknown(0),
    GPS(1),
    SBAS(2),
    Glonass(3),
    QZSS(4),
    Beidou(5),
    Galileo(6);

    private int value;

    public static int size() { return Constellation.size(); }

    Constellation(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Constellation get(int value) {
        if ((value > 0) && (value < Constellation.values().length)) {
            for (Constellation constellation:Constellation.values()) {
                if (constellation.value == value)
                    return constellation;
            }
        }
        return Unknown;
    }
}
