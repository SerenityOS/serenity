/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.imageio.plugins.tiff;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing the tags found in an Exif GPS Info IFD.
 *
 * <p> The definitions of the data types referenced by the field
 * definitions may be found in the {@link TIFFTag TIFFTag} class.
 *
 * @since 9
 * @see   ExifTIFFTagSet
 */
public final class ExifGPSTagSet extends TIFFTagSet {
    private static ExifGPSTagSet theInstance = null;

    /**
     * A tag indicating the GPS tag version (type BYTE, count = 4).
     *
     * @see #GPS_VERSION_2_2
     */
    public static final int TAG_GPS_VERSION_ID = 0;

    /**
     * A value to be used with the "GPSVersionID" tag to indicate GPS version
     * 2.2.  The value equals the US-ASCII encoding of the byte array
     * {@code {'2', '2', '0', '0'}}.
     *
     * @see #TAG_GPS_VERSION_ID
     */
    public static final String GPS_VERSION_2_2 = "2200";

    /**
     * A tag indicating the North or South latitude (type ASCII, count = 2).
     *
     * @see #LATITUDE_REF_NORTH
     * @see #LATITUDE_REF_SOUTH
     */
    public static final int TAG_GPS_LATITUDE_REF = 1;

    /**
     * A tag indicating the Latitude (type RATIONAL, count = 3).
     */
    public static final int TAG_GPS_LATITUDE = 2;

    /**
     * A tag indicating the East or West Longitude (type ASCII, count = 2).
     *
     * @see #LONGITUDE_REF_EAST
     * @see #LONGITUDE_REF_WEST
     */
    public static final int TAG_GPS_LONGITUDE_REF = 3;

    /**
     * A tag indicating the Longitude (type RATIONAL, count = 3).
     */
    public static final int TAG_GPS_LONGITUDE = 4;

    /**
     * A tag indicating the Altitude reference (type BYTE, count = 1);
     *
     * @see #ALTITUDE_REF_SEA_LEVEL
     * @see #ALTITUDE_REF_SEA_LEVEL_REFERENCE
     */
    public static final int TAG_GPS_ALTITUDE_REF = 5;

    /**
     * A tag indicating the Altitude (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_ALTITUDE = 6;

    /**
     * A tag indicating the GPS time (atomic clock) (type RATIONAL, count = 3).
     */
    public static final int TAG_GPS_TIME_STAMP = 7;

    /**
     * A tag indicating the GPS satellites used for measurement (type ASCII).
     */
    public static final int TAG_GPS_SATELLITES = 8;

    /**
     * A tag indicating the GPS receiver status (type ASCII, count = 2).
     *
     * @see #STATUS_MEASUREMENT_IN_PROGRESS
     * @see #STATUS_MEASUREMENT_INTEROPERABILITY
     */
    public static final int TAG_GPS_STATUS = 9;

    /**
     * A tag indicating the GPS measurement mode (type ASCII, count = 2).
     *
     * @see #MEASURE_MODE_2D
     * @see #MEASURE_MODE_3D
     */
    public static final int TAG_GPS_MEASURE_MODE = 10;

    /**
     * A tag indicating the Measurement precision (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_DOP = 11;

    /**
     * A tag indicating the Speed unit (type ASCII, count = 2).
     *
     * @see #SPEED_REF_KILOMETERS_PER_HOUR
     * @see #SPEED_REF_MILES_PER_HOUR
     * @see #SPEED_REF_KNOTS
     */
    public static final int TAG_GPS_SPEED_REF = 12;

    /**
     * A tag indicating the Speed of GPS receiver (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_SPEED = 13;

    /**
     * A tag indicating the Reference for direction of movement (type ASCII,
     * count = 2).
     *
     * @see #DIRECTION_REF_TRUE
     * @see #DIRECTION_REF_MAGNETIC
     */
    public static final int TAG_GPS_TRACK_REF = 14;

    /**
     * A tag indicating the Direction of movement (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_TRACK = 15;

    /**
     * A tag indicating the Reference for direction of image (type ASCII,
     * count = 2).
     *
     * @see #DIRECTION_REF_TRUE
     * @see #DIRECTION_REF_MAGNETIC
     */
    public static final int TAG_GPS_IMG_DIRECTION_REF = 16;

    /**
     * A tag indicating the Direction of image (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_IMG_DIRECTION = 17;

    /**
     * A tag indicating the Geodetic survey data used (type ASCII).
     */
    public static final int TAG_GPS_MAP_DATUM = 18;

    /**
     * A tag indicating the Reference for latitude of destination (type
     * ASCII, count = 2).
     *
     * @see #LATITUDE_REF_NORTH
     * @see #LATITUDE_REF_SOUTH
     */
    public static final int TAG_GPS_DEST_LATITUDE_REF = 19;

    /**
     * A tag indicating the Latitude of destination (type RATIONAL, count = 3).
     */
    public static final int TAG_GPS_DEST_LATITUDE = 20;

    /**
     * A tag indicating the Reference for longitude of destination (type
     * ASCII, count = 2).
     *
     * @see #LONGITUDE_REF_EAST
     * @see #LONGITUDE_REF_WEST
     */
    public static final int TAG_GPS_DEST_LONGITUDE_REF = 21;

    /**
     * A tag indicating the Longitude of destination (type RATIONAL,
     * count = 3).
     */
    public static final int TAG_GPS_DEST_LONGITUDE = 22;

    /**
     * A tag indicating the Reference for bearing of destination (type ASCII,
     * count = 2).
     *
     * @see #DIRECTION_REF_TRUE
     * @see #DIRECTION_REF_MAGNETIC
     */
    public static final int TAG_GPS_DEST_BEARING_REF = 23;

    /**
     * A tag indicating the Bearing of destination (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_DEST_BEARING = 24;

    /**
     * A tag indicating the Reference for distance to destination (type ASCII,
     * count = 2).
     *
     * @see #DEST_DISTANCE_REF_KILOMETERS
     * @see #DEST_DISTANCE_REF_MILES
     * @see #DEST_DISTANCE_REF_KNOTS
     */
    public static final int TAG_GPS_DEST_DISTANCE_REF = 25;

    /**
     * A tag indicating the Distance to destination (type RATIONAL, count = 1).
     */
    public static final int TAG_GPS_DEST_DISTANCE = 26;

    /**
     * A tag indicating the Name of GPS processing method (type UNDEFINED).
     */
    public static final int TAG_GPS_PROCESSING_METHOD = 27;

    /**
     * A tag indicating the Name of GPS area (type UNDEFINED).
     */
    public static final int TAG_GPS_AREA_INFORMATION = 28;

    /**
     * A tag indicating the GPS date (type ASCII, count 11).
     */
    public static final int TAG_GPS_DATE_STAMP = 29;

    /**
     * A tag indicating the GPS differential correction (type SHORT,
     * count = 1).
     *
     * @see #DIFFERENTIAL_CORRECTION_NONE
     * @see #DIFFERENTIAL_CORRECTION_APPLIED
     */
    public static final int TAG_GPS_DIFFERENTIAL = 30;

    /**
     * A value to be used with the "GPSLatitudeRef" and
     * "GPSDestLatitudeRef" tags.
     *
     * @see #TAG_GPS_LATITUDE_REF
     * @see #TAG_GPS_DEST_LATITUDE_REF
     */
    public static final String LATITUDE_REF_NORTH = "N";

    /**
     * A value to be used with the "GPSLatitudeRef" and
     * "GPSDestLatitudeRef" tags.
     *
     * @see #TAG_GPS_LATITUDE_REF
     * @see #TAG_GPS_DEST_LATITUDE_REF
     */
    public static final String LATITUDE_REF_SOUTH = "S";

    /**
     * A value to be used with the "GPSLongitudeRef" and
     * "GPSDestLongitudeRef" tags.
     *
     * @see #TAG_GPS_LONGITUDE_REF
     * @see #TAG_GPS_DEST_LONGITUDE_REF
     */
    public static final String LONGITUDE_REF_EAST = "E";

    /**
     * A value to be used with the "GPSLongitudeRef" and
     * "GPSDestLongitudeRef" tags.
     *
     * @see #TAG_GPS_LONGITUDE_REF
     * @see #TAG_GPS_DEST_LONGITUDE_REF
     */
    public static final String LONGITUDE_REF_WEST = "W";

    /**
     * A value to be used with the "GPSAltitudeRef" tag.
     *
     * @see #TAG_GPS_ALTITUDE_REF
     */
    public static final int ALTITUDE_REF_SEA_LEVEL = 0;

    /**
     * A value to be used with the "GPSAltitudeRef" tag.
     *
     * @see #TAG_GPS_ALTITUDE_REF
     */
    public static final int ALTITUDE_REF_SEA_LEVEL_REFERENCE = 1;

    /**
     * A value to be used with the "GPSStatus" tag.
     *
     * @see #TAG_GPS_STATUS
     */
    public static final String STATUS_MEASUREMENT_IN_PROGRESS = "A";

    /**
     * A value to be used with the "GPSStatus" tag.
     *
     * @see #TAG_GPS_STATUS
     */
    public static final String STATUS_MEASUREMENT_INTEROPERABILITY = "V";

    /**
     * A value to be used with the "GPSMeasureMode" tag.
     *
     * @see #TAG_GPS_MEASURE_MODE
     */
    public static final String MEASURE_MODE_2D = "2";

    /**
     * A value to be used with the "GPSMeasureMode" tag.
     *
     * @see #TAG_GPS_MEASURE_MODE
     */
    public static final String MEASURE_MODE_3D = "3";

    /**
     * A value to be used with the "GPSSpeedRef" tag.
     *
     * @see #TAG_GPS_SPEED_REF
     */
    public static final String SPEED_REF_KILOMETERS_PER_HOUR = "K";

    /**
     * A value to be used with the "GPSSpeedRef" tag.
     *
     * @see #TAG_GPS_SPEED_REF
     */
    public static final String SPEED_REF_MILES_PER_HOUR = "M";

    /**
     * A value to be used with the "GPSSpeedRef" tag.
     *
     * @see #TAG_GPS_SPEED_REF
     */
    public static final String SPEED_REF_KNOTS = "N";

    /**
     * A value to be used with the "GPSTrackRef", "GPSImgDirectionRef",
     * and "GPSDestBearingRef" tags.
     *
     * @see #TAG_GPS_TRACK_REF
     * @see #TAG_GPS_IMG_DIRECTION_REF
     * @see #TAG_GPS_DEST_BEARING_REF
     */
    public static final String DIRECTION_REF_TRUE = "T";

    /**
     * A value to be used with the "GPSTrackRef", "GPSImgDirectionRef",
     * and "GPSDestBearingRef" tags.
     *
     * @see #TAG_GPS_TRACK_REF
     * @see #TAG_GPS_IMG_DIRECTION_REF
     * @see #TAG_GPS_DEST_BEARING_REF
     */
    public static final String DIRECTION_REF_MAGNETIC = "M";

    /**
     * A value to be used with the "GPSDestDistanceRef" tag.
     *
     * @see #TAG_GPS_DEST_DISTANCE_REF
     */
    public static final String DEST_DISTANCE_REF_KILOMETERS = "K";

    /**
     * A value to be used with the "GPSDestDistanceRef" tag.
     *
     * @see #TAG_GPS_DEST_DISTANCE_REF
     */
    public static final String DEST_DISTANCE_REF_MILES = "M";

    /**
     * A value to be used with the "GPSDestDistanceRef" tag.
     *
     * @see #TAG_GPS_DEST_DISTANCE_REF
     */
    public static final String DEST_DISTANCE_REF_KNOTS = "N";

    /**
     * A value to be used with the "GPSDifferential" tag.
     *
     * @see #TAG_GPS_DIFFERENTIAL
     */
    public static final int DIFFERENTIAL_CORRECTION_NONE = 0;

    /**
     * A value to be used with the "GPSDifferential" tag.
     *
     * @see #TAG_GPS_DIFFERENTIAL
     */
    public static final int DIFFERENTIAL_CORRECTION_APPLIED = 1;

    static class GPSVersionID extends TIFFTag {
        public GPSVersionID() {
            super("GPSVersionID",
                  TAG_GPS_VERSION_ID,
                  1 << TIFFTag.TIFF_BYTE);
        }
    }

    static class GPSLatitudeRef extends TIFFTag {
        public GPSLatitudeRef() {
            super("GPSLatitudeRef",
                  TAG_GPS_LATITUDE_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSLatitude extends TIFFTag {
        public GPSLatitude() {
            super("GPSLatitude",
                  TAG_GPS_LATITUDE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSLongitudeRef extends TIFFTag {
        public GPSLongitudeRef() {
            super("GPSLongitudeRef",
                  TAG_GPS_LONGITUDE_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSLongitude extends TIFFTag {
        public GPSLongitude() {
            super("GPSLongitude",
                  TAG_GPS_LONGITUDE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSAltitudeRef extends TIFFTag {
        public GPSAltitudeRef() {
            super("GPSAltitudeRef",
                  TAG_GPS_ALTITUDE_REF,
                  1 << TIFFTag.TIFF_BYTE);

            addValueName(ALTITUDE_REF_SEA_LEVEL, "Sea level");
            addValueName(ALTITUDE_REF_SEA_LEVEL_REFERENCE,
                         "Sea level reference (negative value)");
        }
    }

    static class GPSAltitude extends TIFFTag {
        public GPSAltitude() {
            super("GPSAltitude",
                  TAG_GPS_ALTITUDE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSTimeStamp extends TIFFTag {
        public GPSTimeStamp() {
            super("GPSTimeStamp",
                  TAG_GPS_TIME_STAMP,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSSatellites extends TIFFTag {
        public GPSSatellites() {
            super("GPSSatellites",
                  TAG_GPS_SATELLITES,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSStatus extends TIFFTag {
        public GPSStatus() {
            super("GPSStatus",
                  TAG_GPS_STATUS,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSMeasureMode extends TIFFTag {
        public GPSMeasureMode() {
            super("GPSMeasureMode",
                  TAG_GPS_MEASURE_MODE,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDOP extends TIFFTag {
        public GPSDOP() {
            super("GPSDOP",
                  TAG_GPS_DOP,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSSpeedRef extends TIFFTag {
        public GPSSpeedRef() {
            super("GPSSpeedRef",
                  TAG_GPS_SPEED_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSSpeed extends TIFFTag {
        public GPSSpeed() {
            super("GPSSpeed",
                  TAG_GPS_SPEED,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSTrackRef extends TIFFTag {
        public GPSTrackRef() {
            super("GPSTrackRef",
                  TAG_GPS_TRACK_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSTrack extends TIFFTag {
        public GPSTrack() {
            super("GPSTrack",
                  TAG_GPS_TRACK,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSImgDirectionRef extends TIFFTag {
        public GPSImgDirectionRef() {
            super("GPSImgDirectionRef",
                  TAG_GPS_IMG_DIRECTION_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSImgDirection extends TIFFTag {
        public GPSImgDirection() {
            super("GPSImgDirection",
                  TAG_GPS_IMG_DIRECTION,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSMapDatum extends TIFFTag {
        public GPSMapDatum() {
            super("GPSMapDatum",
                  TAG_GPS_MAP_DATUM,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDestLatitudeRef extends TIFFTag {
        public GPSDestLatitudeRef() {
            super("GPSDestLatitudeRef",
                  TAG_GPS_DEST_LATITUDE_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDestLatitude extends TIFFTag {
        public GPSDestLatitude() {
            super("GPSDestLatitude",
                  TAG_GPS_DEST_LATITUDE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSDestLongitudeRef extends TIFFTag {
        public GPSDestLongitudeRef() {
            super("GPSDestLongitudeRef",
                  TAG_GPS_DEST_LONGITUDE_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDestLongitude extends TIFFTag {
        public GPSDestLongitude() {
            super("GPSDestLongitude",
                  TAG_GPS_DEST_LONGITUDE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSDestBearingRef extends TIFFTag {
        public GPSDestBearingRef() {
            super("GPSDestBearingRef",
                  TAG_GPS_DEST_BEARING_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDestBearing extends TIFFTag {
        public GPSDestBearing() {
            super("GPSDestBearing",
                  TAG_GPS_DEST_BEARING,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSDestDistanceRef extends TIFFTag {
        public GPSDestDistanceRef() {
            super("GPSDestDistanceRef",
                  TAG_GPS_DEST_DISTANCE_REF,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDestDistance extends TIFFTag {
        public GPSDestDistance() {
            super("GPSDestDistance",
                  TAG_GPS_DEST_DISTANCE,
                  1 << TIFFTag.TIFF_RATIONAL);
        }
    }

    static class GPSProcessingMethod extends TIFFTag {
        public GPSProcessingMethod() {
            super("GPSProcessingMethod",
                  TAG_GPS_PROCESSING_METHOD,
                  1 << TIFFTag.TIFF_UNDEFINED);
        }
    }

    static class GPSAreaInformation extends TIFFTag {
        public GPSAreaInformation() {
            super("GPSAreaInformation",
                  TAG_GPS_AREA_INFORMATION,
                  1 << TIFFTag.TIFF_UNDEFINED);
        }
    }

    static class GPSDateStamp extends TIFFTag {
        public GPSDateStamp() {
            super("GPSDateStamp",
                  TAG_GPS_DATE_STAMP,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    static class GPSDifferential extends TIFFTag {
        public GPSDifferential() {
            super("GPSDifferential",
                  TAG_GPS_DIFFERENTIAL,
                  1 << TIFFTag.TIFF_SHORT);
            addValueName(DIFFERENTIAL_CORRECTION_NONE,
                         "Measurement without differential correction");
            addValueName(DIFFERENTIAL_CORRECTION_APPLIED,
                         "Differential correction applied");

        }
    }

    private static List<TIFFTag> initTags() {
        ArrayList<TIFFTag> tags = new ArrayList<TIFFTag>(31);

        tags.add(new GPSVersionID());
        tags.add(new GPSLatitudeRef());
        tags.add(new GPSLatitude());
        tags.add(new GPSLongitudeRef());
        tags.add(new GPSLongitude());
        tags.add(new GPSAltitudeRef());
        tags.add(new GPSAltitude());
        tags.add(new GPSTimeStamp());
        tags.add(new GPSSatellites());
        tags.add(new GPSStatus());
        tags.add(new GPSMeasureMode());
        tags.add(new GPSDOP());
        tags.add(new GPSSpeedRef());
        tags.add(new GPSSpeed());
        tags.add(new GPSTrackRef());
        tags.add(new GPSTrack());
        tags.add(new GPSImgDirectionRef());
        tags.add(new GPSImgDirection());
        tags.add(new GPSMapDatum());
        tags.add(new GPSDestLatitudeRef());
        tags.add(new GPSDestLatitude());
        tags.add(new GPSDestLongitudeRef());
        tags.add(new GPSDestLongitude());
        tags.add(new GPSDestBearingRef());
        tags.add(new GPSDestBearing());
        tags.add(new GPSDestDistanceRef());
        tags.add(new GPSDestDistance());
        tags.add(new GPSProcessingMethod());
        tags.add(new GPSAreaInformation());
        tags.add(new GPSDateStamp());
        tags.add(new GPSDifferential());
        return tags;
    }

    private ExifGPSTagSet() {
        super(initTags());
    }

    /**
     * Returns a shared instance of an {@code ExifGPSTagSet}.
     *
     * @return an {@code ExifGPSTagSet} instance.
     */
    public synchronized static ExifGPSTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifGPSTagSet();
        }
        return theInstance;
    }
}
