/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug     8149028
 * @author  a.stepanov
 * @summary check TIFFDirectory manipulation
 *          by means of TIFFImageReadParam
 * @run     main TIFFImageReadParamTest
 */


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.tiff.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class TIFFImageReadParamTest {

    private final static String FILENAME = "test.tiff";
    private final static int SZ = 100;
    private final static Color C = Color.RED;

    private final static String GEO_DATA = "test params";
    private final static int GEO_N = GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS;

    private final static String EXIF_DATA = "2000:01:01 00:00:01";
    private final static int EXIF_N = ExifTIFFTagSet.TAG_DATE_TIME_ORIGINAL;

    private final static String GPS_DATA =
        ExifGPSTagSet.STATUS_MEASUREMENT_IN_PROGRESS;
    private final static int GPS_N = ExifGPSTagSet.TAG_GPS_STATUS;

    private final static short FAX_DATA =
        FaxTIFFTagSet.CLEAN_FAX_DATA_ERRORS_UNCORRECTED;
    private final static int FAX_N = FaxTIFFTagSet.TAG_CLEAN_FAX_DATA;

    private ImageWriter getTIFFWriter() {

        java.util.Iterator<ImageWriter> writers =
            ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new RuntimeException("No writers available for TIFF format");
        }
        return writers.next();
    }

    private ImageReader getTIFFReader() {

        java.util.Iterator<ImageReader> readers =
            ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new RuntimeException("No readers available for TIFF format");
        }
        return readers.next();
    }

    private void check(boolean ok, String msg) {
        if (!ok) { throw new RuntimeException(msg); }
    }

    private void addASCIIField(TIFFDirectory d,
                               String        name,
                               String        data,
                               int           num) {

        String a[] = {data};
        d.addTIFFField(new TIFFField(
            new TIFFTag(name, num, 1 << TIFFTag.TIFF_ASCII),
                TIFFTag.TIFF_ASCII, 1, a));
    }

    private void checkASCIIValue(TIFFDirectory d,
                                 String        what,
                                 String        data,
                                 int           num) {

        TIFFField f = d.getTIFFField(num);
        check(f.getType() == TIFFTag.TIFF_ASCII, "field type != ASCII");
        check(f.getCount() == 1, "invalid " + what + " data count");
        check(f.getValueAsString(0).equals(data),
            "invalid " + what + " data");
    }


    private void writeImage() throws Exception {

        OutputStream s = new BufferedOutputStream(new FileOutputStream(FILENAME));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(s)) {
            ImageWriter writer = getTIFFWriter();
            writer.setOutput(ios);

            BufferedImage img =
                new BufferedImage(SZ, SZ, BufferedImage.TYPE_INT_RGB);
            Graphics g = img.getGraphics();
            g.setColor(C);
            g.fillRect(0, 0, SZ, SZ);
            g.dispose();

            IIOMetadata metadata = writer.getDefaultImageMetadata(
                new ImageTypeSpecifier(img), writer.getDefaultWriteParam());

            TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

            // add some extension tags
            addASCIIField(dir, "GeoAsciiParamsTag", GEO_DATA, GEO_N);
            addASCIIField(dir, "DateTimeOriginal", EXIF_DATA, EXIF_N);
            addASCIIField(dir, "GPSStatus", GPS_DATA, GPS_N);

            dir.addTIFFField(new TIFFField(new TIFFTag(
                "CleanFaxData", FAX_N, 1 << TIFFTag.TIFF_SHORT), FAX_DATA));

            IIOMetadata data = dir.getAsMetadata();

            writer.write(new IIOImage(img, null, data));

            ios.flush();
            writer.dispose();
        }
    }

    private void checkImage(BufferedImage img) {

        check(img.getWidth() == SZ, "invalid image width");
        check(img.getHeight() == SZ, "invalid image height");
        Color c = new Color(img.getRGB(SZ / 2, SZ / 2));
        check(c.equals(C), "invalid image color");
    }

    private TIFFDirectory getDir(TIFFTagSet[] add,
                                 TIFFTagSet[] remove) throws Exception {

        ImageReader reader = getTIFFReader();

        ImageInputStream s = ImageIO.createImageInputStream(new File(FILENAME));
        reader.setInput(s, false, false);

        int ni = reader.getNumImages(true);
        check(ni == 1, "invalid number of images: " + ni);

        TIFFImageReadParam param = new TIFFImageReadParam();
        for (TIFFTagSet ts: add) { param.addAllowedTagSet(ts); }
        for (TIFFTagSet ts: remove) { param.removeAllowedTagSet(ts); }

        IIOImage img = reader.readAll(0, param);

        // just in case, check image
        checkImage((BufferedImage) img.getRenderedImage());

        IIOMetadata metadata = img.getMetadata();
        TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

        reader.dispose();
        s.close();

        return dir;
    }

    private void simpleChecks() {

        TIFFImageReadParam param = new TIFFImageReadParam();

        java.util.List<TIFFTagSet> allowed = param.getAllowedTagSets();

        // see docs
        check(allowed.contains(BaselineTIFFTagSet.getInstance()),
            "must contain BaselineTIFFTagSet");
        check(allowed.contains(FaxTIFFTagSet.getInstance()),
            "must contain FaxTIFFTagSet");
        check(allowed.contains(ExifParentTIFFTagSet.getInstance()),
            "must contain ExifParentTIFFTagSet");
        check(allowed.contains(GeoTIFFTagSet.getInstance()),
            "must contain GeoTIFFTagSet");

        TIFFTagSet gps = ExifGPSTagSet.getInstance();
        param.addAllowedTagSet(gps);
        check(param.getAllowedTagSets().contains(gps),
            "must contain ExifGPSTagSet");

        param.removeAllowedTagSet(gps);
        check(!param.getAllowedTagSets().contains(gps),
            "must not contain ExifGPSTagSet");

        // check that repeating remove goes properly
        param.removeAllowedTagSet(gps);

        boolean ok = false;
        try { param.addAllowedTagSet(null); }
        catch (IllegalArgumentException e) { ok = true; }
        check(ok, "must not be able to add null tag set");

        ok = false;
        try { param.removeAllowedTagSet(null); }
        catch (IllegalArgumentException e) { ok = true; }
        check(ok, "must not be able to remove null tag set");
    }

    private void run() {

        simpleChecks();

        try {

            writeImage();

            TIFFTagSet
                empty[] = {},
                geo[]   = {  GeoTIFFTagSet.getInstance() },
                exif[]  = { ExifTIFFTagSet.getInstance() },
                gps[]   = {  ExifGPSTagSet.getInstance() },
                fax[]   = {  FaxTIFFTagSet.getInstance() };

            // default param state
            TIFFDirectory dir = getDir(empty, empty);
            // Geo and Fax are default allowed tag sets
            check(dir.containsTIFFField(GEO_N), "must contain Geo field");
            checkASCIIValue(dir, "Geo", GEO_DATA, GEO_N);
            check(dir.containsTIFFField(FAX_N), "must contain Fax field");
            check(
                (dir.getTIFFField(FAX_N).getCount() == 1) &&
                (dir.getTIFFField(FAX_N).getAsInt(0) == FAX_DATA),
                "invalid Fax field value");

            // corresponding tag sets are non-default
            check(!dir.containsTIFFField(EXIF_N), "must not contain Geo field");
            check(!dir.containsTIFFField(GPS_N), "must not contain GPS field");

            // remove Fax
            dir = getDir(empty, fax);
            check(!dir.containsTIFFField(FAX_N), "must not contain Fax field");

            // add EXIF, remove Geo
            dir = getDir(exif, geo);
            check(dir.containsTIFFField(EXIF_N), "must contain EXIF field");
            checkASCIIValue(dir, "EXIF", EXIF_DATA, EXIF_N);
            check(!dir.containsTIFFField(GEO_N), "must not contain Geo field");

            // add GPS
            dir = getDir(gps, empty);
            check(dir.containsTIFFField(GPS_N), "must contain GPS field");
            checkASCIIValue(dir, "GPS", GPS_DATA, GPS_N);

        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) {
        (new TIFFImageReadParamTest()).run();
    }
}
