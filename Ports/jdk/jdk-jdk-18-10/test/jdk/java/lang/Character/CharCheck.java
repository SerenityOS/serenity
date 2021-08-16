/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author Alan Liu
 * @author John O'Conner
 */

import java.io.*;


/**
 * This class either loads or dumps the character properties of all Unicode
 * characters out to a file.  When loading, it compares the loaded data with
 * that obtained through the java.lang.Character API.  This allows detection of
 * changes to the character properties between versions of the Java VM.  A
 * typical usage would be to dump the properties under an early VM, and load
 * them under a later VM.
 *
 * Also: Check the current VM's character properties against those in a
 * Unicode database.  The database should be of the format
 * available on ftp.unicode.org/Public/UNIDATA.
 *
 */
public class CharCheck {
    static int differences = 0;

    public static void main(String args[]) throws Exception {

        if (args.length != 2 && args.length != 3) usage();
        if (args[0].equals("dump"))
           dump(Integer.parseInt(args[1], 16), new ObjectOutputStream(new FileOutputStream(args[2])));
        else if (args[0].equals("load"))
            load(Integer.parseInt(args[1], 16), new ObjectInputStream(new FileInputStream(args[2])));
        else if (args[0].equals("check"))
           check(Integer.parseInt(args[1], 16), new File(args[2]));
        else if (args[0].equals("char"))
            showChar(Integer.parseInt(args[1],16));
        else if (args[0].equals("fchar"))
            showFileChar(args[1], Integer.parseInt(args[2],16));
        else usage();
       if (differences != 0) {
            throw new RuntimeException("There are differences between Character properties and the specification.");
        }
    }

    static void usage() {
        System.err.println("Usage: java CharCheck <command>");
        System.err.println("where <command> is one of the following:");
        System.err.println("dump <plane> <file> - dumps the character properties of the given plane,");
                System.err.println("              read from the current VM, to the given file.");
        System.err.println("load <plane> <file> - loads the character properties from the given");
        System.err.println("              file and compares them to those of the given character plane");
                System.err.println("              in the current VM.");
        System.err.println("check <plane> <file> - compare the current VM's character properties");
                System.err.println("               in the given plane to those listed in the given file, ");
                System.err.println("               which should be in the format available on ");
                System.err.println("               ftp.unicode.org/Public/2.0-Update.");
        System.err.println("char <code> - show current VM properties of the given Unicode char.");
        System.err.println("fchar <file> <code> - show file properties of the given Unicode char.");
        System.exit(0);
    }

    static String getTypeName(int type) {
        return (type >= 0 && type < UnicodeSpec.generalCategoryList.length) ?
                        (UnicodeSpec.generalCategoryList[type][UnicodeSpec.LONG] + '(' + type + ')') :
                        ("<Illegal type value " + type + ">");
    }

    static int check(int plane, File specFile) throws Exception {

        String version = System.getProperty("java.version");
        System.out.println("Current VM version " + version);
        int rangeLimit = (plane << 16) | 0xFFFF;
        String record;
        UnicodeSpec[] spec = UnicodeSpec.readSpecFile(specFile, plane);
        int rangeStart = 0x0000;
        boolean isRange = false;

        lastCheck = (plane << 16) - 1;

        for (int currentSpec = 0; currentSpec < spec.length; currentSpec++) {
            int c = spec[currentSpec].getCodePoint();
            if (isRange) {
                // Must see end of range now
                if (spec[currentSpec].getName().endsWith("Last>")) {
                    for (int d=rangeStart; d<=c; d++)  {
                        checkOneChar(d, spec[currentSpec]);
                    }
                }
                else {
                    // No good -- First without Last
                    System.out.println("BAD FILE: First without last at '" + escape(rangeStart) + "'");
                }
                isRange = false;
            }
            else {
                // Look for a First, Last pair: This is a pair of entries like the following:
                //  4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;
                //  9FA5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
                if (spec[currentSpec].getName().endsWith("First>")) {
                    rangeStart = c;
                    isRange = true;
                }
                else {
                    checkOneChar(c, spec[currentSpec]);
                }
            }
        }

        // Check undefined chars at the end of the range

        while (lastCheck < rangeLimit) checkOneCharDefined(++lastCheck, "?", false);

        System.out.println("Total differences: "+differences);
        return differences;
    }

    static int lastCheck = -1;

    static final void checkOneCharDefined(int c, String name, boolean fileDefined) {
        if (Character.isDefined(c) != fileDefined)
            showDifference(c, name, "isDefined", ""+(!fileDefined), ""+fileDefined);
    }

    // In GenerateCharacter, the following ranges are handled specially.
    // Each is the start of a 26-character range with values 10..35.
    static final char NUMERIC_EXCEPTION[] = { '\u0041', '\u0061', '\uFF21', '\uFF41' };

    static void checkOneChar(int c, UnicodeSpec charSpec) {
        // Handle intervening ranges -- we assume that we will be called in monotonically
        // increasing order.  If the last char we checked is more than one before this
        // char, then check the intervening range -- it should all be undefined.
        int lowerLimit = (c & 0xFF0000);
        if (lastCheck >= lowerLimit && (lastCheck+1) != c) {
            for (int i=lastCheck+1; i<c; ++i)
                checkOneCharDefined(i, "?", false);
        }

        lastCheck = c;

        // isDefined should be true
        checkOneCharDefined(c, charSpec.getName(), true);

        // Check lower, upper, and titlecase conversion
        int upper = Character.toUpperCase(c);
        int lower = Character.toLowerCase(c);
        int title = Character.toTitleCase(c);
        int upperDB = charSpec.hasUpperMap() ? charSpec.getUpperMap() : c;
        int lowerDB = charSpec.hasLowerMap() ? charSpec.getLowerMap() : c;
        int titleDB = charSpec.hasTitleMap() ? charSpec.getTitleMap() : c;
        if (upper != upperDB) showDifference(c, charSpec.getName(), "upper", hex6(upper), hex6(upperDB));
        if (lower != lowerDB) showDifference(c, charSpec.getName(), "lower", hex6(lower), hex6(lowerDB));
        if (title != titleDB) showDifference(c, charSpec.getName(), "title", hex6(title), hex6(titleDB));

        // Check the character general category (type)
        int type = Character.getType(c);
        int typeDB = charSpec.getGeneralCategory();
        if (type != typeDB) {
            showDifference(c, charSpec.getName(), "type",
                UnicodeSpec.generalCategoryList[type][UnicodeSpec.SHORT],
                UnicodeSpec.generalCategoryList[typeDB][UnicodeSpec.SHORT]);
        }

        // Check the mirrored property
        boolean isMirrored = Character.isMirrored(c);
        boolean isMirroredDB = charSpec.isMirrored();
        if (isMirrored != isMirroredDB) {
                showDifference(c, charSpec.getName(), "isMirrored", ""+isMirrored, ""+isMirroredDB);
        }

        // Check the directionality property
        byte directionality = Character.getDirectionality(c);
        byte directionalityDB = charSpec.getBidiCategory();
        if (directionality != directionalityDB) {
            showDifference(c, charSpec.getName(), "directionality", ""+directionality, ""+directionalityDB);
        }

        // Check the decimal digit property
        int decimalDigit = Character.digit(c, 10);
        int decimalDigitDB = -1;
        if (charSpec.getGeneralCategory() == UnicodeSpec.DECIMAL_DIGIT_NUMBER) {
            decimalDigitDB = charSpec.getDecimalValue();
        }
        if (decimalDigit != decimalDigitDB)
            showDifference(c, charSpec.getName(), "decimal digit", ""+decimalDigit, ""+decimalDigitDB);

        // Check the numeric property
        int numericValue = Character.getNumericValue(c);
        int numericValueDB;
        if (charSpec.getNumericValue().length() == 0) {
            numericValueDB = -1;
            // Handle exceptions where Character deviates from the UCS spec
            for (int k=0; k<NUMERIC_EXCEPTION.length; ++k) {
                if (c >= NUMERIC_EXCEPTION[k] && c < (char)(NUMERIC_EXCEPTION[k]+26)) {
                    numericValueDB = c - NUMERIC_EXCEPTION[k] + 10;
                    break;
                }
            }
        }
        else {
            String strValue = charSpec.getNumericValue();
            int parsedNumericValue;
            if (strValue.equals("10000000000")
                || strValue.equals("1000000000000")) {
                System.out.println("Skipping strValue: " + strValue
                    + " for " + charSpec.getName()
                    + "(0x" + Integer.toHexString(c) + ")");
                parsedNumericValue = -2;
            } else {
                parsedNumericValue = strValue.indexOf('/') < 0 ?
                                     Integer.parseInt(strValue) : -2;
            }
            numericValueDB = parsedNumericValue < 0 ? -2 : parsedNumericValue;
        }
        if (numericValue != numericValueDB)
            showDifference(c, charSpec.getName(), "numeric value", ""+numericValue, ""+numericValueDB);
    }

    static void showDifference(int c, String name, String property, String vmValue, String dbValue) {
        System.out.println(escape("Mismatch at '" + hex6(c) + "' (" + name+ "): " +
                                         property + "=" + vmValue + ", db=" + dbValue));
        ++differences;
    }

    /**
     * Given a record containing ';'-separated fields, return the fieldno-th
     * field.  The first field is field 0.
     */
    static String getField(String record, int fieldno) {
        int i=0;
        int j=record.indexOf(';');
        while (fieldno > 0) {
            i=j+1;
            j=record.indexOf(';', i);
        }
        return record.substring(i, j);
    }

    static final int FIELD_COUNT = 15;

    /**
     * Given a record containing ';'-separated fields, return an array of
     * the fields.  It is assumed that there are FIELD_COUNT fields per record.
     */
    static void getFields(String record, String[] fields) {
        int i=0;
        int j=record.indexOf(';');
        fields[0] = record.substring(i, j);
        for (int n=1; n<FIELD_COUNT; ++n) {
            i=j+1;
            j=record.indexOf(';', i);
            fields[n] = (j<0) ? record.substring(i) : record.substring(i, j);
        }
    }

    /**
     * Given a record containing ';'-separated fields, return an array of
     * the fields.  It is assumed that there are FIELD_COUNT fields per record.
     */
    static String[] getFields(String record) {
        String[] fields = new String[FIELD_COUNT];
        getFields(record, fields);
        return fields;
    }

    static void dump(int plane, ObjectOutputStream out) throws Exception {
        String version = System.getProperty("java.version");
        System.out.println("Writing file version " + version);
        out.writeObject(version);

        long[] data = new long[0x20000];
        long[] onechar = new long[2];
        int j=0;
        int begin = plane<<16;
        int end = begin + 0xFFFF;
        for (int i = begin; i <= end; ++i) {
            getPackedCharacterData(i, onechar);
            data[j++] = onechar[0];
            data[j++] = onechar[1];
        }
        out.writeObject(data);
    }

    static long[] loadData(ObjectInputStream in) throws Exception {
        String version = System.getProperty("java.version");
        String inVersion = (String)in.readObject();
        System.out.println("Reading file version " + inVersion);
        System.out.println("Current version " + version);

        long[] data = (long[])in.readObject();
        if (data.length != 0x20000) {
            System.out.println("BAD ARRAY LENGTH: " + data.length);
        }
        return data;
    }

    static int load(int plane, ObjectInputStream in) throws Exception {
        long[] data = CharCheck.loadData(in);
        CharCheck.checkData(data, plane);
        return differences;
    }


    static int checkData(long[] data, int plane) {
        long[] onechar = new long[2];

        for (int i=0; i<0x10000; ++i) {
            int c = (plane << 16) | i;
            getPackedCharacterData(c, onechar);
            if (data[2*i] != onechar[0] || data[2*i+1] != onechar[1]) {
                long[] filechar = { data[2*i], data[2*i+1] };
                showDifference(c, onechar, filechar);
            }
        }
        System.out.println("Total differences: " + differences);
        return differences;
    }

    static String hex6(long n) {
        String q = Long.toHexString(n).toUpperCase();
        return "000000".substring(Math.min(6, q.length())) + q;
    }

    static void showChar(int c) {
        long[] chardata = new long[2];
        getPackedCharacterData(c, chardata);
        System.out.println("Current VM properties for '" + hex6(c) + "': " +
                           hex6(chardata[1]) + ' ' + hex6(chardata[0]));
        String[] data = unpackCharacterData(chardata);
        for (int i=0; i<data.length; ++i)
            System.out.println(" " + escape(data[i]));
    }

    static void showFileChar(String fileName, int c) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
        String inVersion = (String)in.readObject();
        System.out.println("Reading file version " + inVersion);

        long[] data = (long[])in.readObject();
        if (data.length != 0x20000) {
            System.out.println("BAD ARRAY LENGTH: " + data.length);
        }
        int offset = c & 0xFFFF;
        long[] chardata = { data[2*offset], data[2*offset+1] };
        String[] datap = unpackCharacterData(chardata);
        System.out.println(escape("File properties for '" + hex6(c)+ "':"));
        for (int i=0; i<datap.length; ++i)
            System.out.println(" " + escape(datap[i]));
    }

    /**
     * The packed character data encapsulates all the information obtainable
     * about a character in a single numeric value.
     *
     * data[0]:
     *
     *  5 bits for getType()
     *  6 bits for digit() -- add one
     *  6 bits for getNumericValue() -- add two
     * 15 bits for isXxx()
     *
     * 21 bits for toUpperCase()
     *
     *
     * data[1]:
     * 21 bits for toLowerCase()
     * 21 bits for toTitleCase()
     */
    static void getPackedCharacterData(int c, long[] data) {
        data[0] =
            (long)Character.getType(c) |
            ((long)(Character.digit(c, Character.MAX_RADIX) + 1) << 5) |
            ((long)(Character.getNumericValue(c) + 2) << 11) |
            (Character.isDefined(c) ? (1L<<17) : 0L) |
            (Character.isDigit(c) ? (1L<<18) : 0L) |
            (Character.isIdentifierIgnorable(c) ? (1L<<19) : 0L) |
            (Character.isISOControl(c) ? (1L<<20) : 0L) |
            (Character.isJavaIdentifierPart(c) ? (1L<<21) : 0L) |
            (Character.isJavaIdentifierStart(c) ? (1L<<22) : 0L) |
            (Character.isLetter(c) ? (1L<<23) : 0L) |
            (Character.isLetterOrDigit(c) ? (1L<<24) : 0L) |
            (Character.isLowerCase(c) ? (1L<<25) : 0L) |
            (Character.isSpaceChar(c) ? (1L<<26) : 0L) |
            (Character.isTitleCase(c) ? (1L<<27) : 0L) |
            (Character.isUnicodeIdentifierPart(c) ? (1L<<28) : 0L) |
            (Character.isUnicodeIdentifierStart(c) ? (1L<<29) : 0L) |
            (Character.isUpperCase(c) ? (1L<<30) : 0L) |
            (Character.isWhitespace(c) ? (1L<<31) : 0L) |
            ((long)Character.toUpperCase(c) << 32);
        data[1] = (long)Character.toLowerCase(c) |
                        ((long)Character.toTitleCase(c) << 21);
    }

    /**
     * Given a long, set the bits at the given offset and length to the given value.
     */
    static long setBits(long data, int offset, int length, long value) {
        long himask = -1L << (offset+length);
        long lomask = ~(-1L << offset);
        long lengthmask = ~(-1L << length);
        return (data & (himask | lomask)) | ((value & lengthmask) << offset);
    }

    /**
     * Given packed character data, change the attribute
     * toLower
     */
    static void setToLower(long[] data, int value) {
        data[0] = setBits(data[0], 48, 16, value);
    }

    /**
     * Given packed character data, change the attribute
     * toUpper
     */
    static void setToUpper(long[] data, int value) {
        data[0] = setBits(data[0], 32, 16, value);
    }

    /**
     * Given packed character data, change the attribute
     * toTitle
     */
    static void setToTitle(long[] data, int value) {
        data[1] = value;
    }

    /**
     * Given packed character data, change the attribute
     * getType
     */
    static void setGetType(long[] data, int value) {
        data[0] = setBits(data[0], 0, 5, value);
    }

    /**
     * Given packed character data, change the attribute
     * isDefined
     */
    static void setIsDefined(long[] data, boolean value) {
        data[0] = setBits(data[0], 17, 1, value?1:0);
    }

    /**
     * Given packed character data, change the attribute
     * isJavaIdentifierPart
     */
    static void setIsJavaIdentifierPart(long[] data, boolean value) {
        data[0] = setBits(data[0], 21, 1, value?1:0);
    }

    /**
     * Given packed character data, change the attribute
     * isJavaIdentifierStart
     */
    static void setIsJavaIdentifierStart(long[] data, boolean value) {
        data[0] = setBits(data[0], 22, 1, value?1:0);
    }

    static String[] unpackCharacterData(long[] dataL) {
        long data = dataL[0];
        String[] result = {
            "type=" + getTypeName((int)(data&0x1F)),
            "digit=" + (((data>>5)&0x3F)-1),
            "numeric=" + (((data>>11)&0x3F)-2),
            "isDefined=" + (((data>>17)&1)==1),
            "isDigit=" + (((data>>18)&1)==1),
            "isIdentifierIgnorable=" + (((data>>19)&1)==1),
            "isISOControl=" + (((data>>20)&1)==1),
            "isJavaIdentifierPart=" + (((data>>21)&1)==1),
            "isJavaIdentifierStart=" + (((data>>22)&1)==1),
            "isLetter=" + (((data>>23)&1)==1),
            "isLetterOrDigit=" + (((data>>24)&1)==1),
            "isLowerCase=" + (((data>>25)&1)==1),
            "isSpaceChar=" + (((data>>26)&1)==1),
            "isTitleCase=" + (((data>>27)&1)==1),
            "isUnicodeIdentifierPart=" + (((data>>28)&1)==1),
            "isUnicodeIdentifierStart=" + (((data>>29)&1)==1),
            "isUpperCase=" + (((data>>30)&1)==1),
            "isWhitespace=" + (((data>>31)&1)==1),
            "toUpper=" + hex6(((int)(data>>32) & 0X1FFFFF)),
            "toLower=" + hex6((int)(dataL[1] & 0x1FFFFF)),
                        "toTitle=" + hex6(((int)(dataL[1] >> 21) & 0x1FFFFF))
        };
        return result;
    }

    static String[] getCharacterData(int c) {
        long[] data = new long[2];
        getPackedCharacterData(c, data);
        return unpackCharacterData(data);
    }

    static void showDifference(int c, long[] currentData, long[] fileData) {
        System.out.println("Difference at " + hex6(c));
        String[] current = unpackCharacterData(currentData);
        String[] file = unpackCharacterData(fileData);
        for (int i=0; i<current.length; ++i) {
            if (!current[i].equals(file[i])) {
                System.out.println(escape(" current " + current[i] +
                                   ", file " + file[i]));
            }
        }
        ++differences;
    }

    static String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7F) buf.append(c);
            else {
                buf.append("\\u");
                String h = "000" + Integer.toHexString(c);
                if (h.length() > 4) h = h.substring(h.length() - 4);
                buf.append(h);
            }
        }
        return buf.toString();
    }

    static String escape(int c) {
        StringBuffer buf = new StringBuffer();
        if (c >= 0x20 && c <= 0x7F) buf.append(c);
        else {
            buf.append("\\u");
            String h = "000" + Integer.toHexString(c);
            if (h.length() > 4) h = h.substring(h.length() - 4);
            buf.append(h);
        }
        return buf.toString();
    }
}


//eof
