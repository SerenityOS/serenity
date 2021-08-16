/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This is a tool to generate categoryNames and categoryMap which are used in
 * CharSet.java.
 */

package build.tools.generatebreakiteratordata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

class CharacterCategory {

    /**
     * A list of Unicode category names.
     */
    static final String[] categoryNames = {
        "Ll",        /* Letter, Lowercase */
        "Lu",        /* Letter, Uppercase */
        "Lt",        /* Letter, Titlecase */
        "Lo",        /* Letter, Other */
        "Lm",        /* Letter, Modifier */
        "Nd",        /* Number, Decimal Digit */
        "Nl",        /* Number, Letter */
        "No",        /* Number, Other */
        "Ps",        /* Punctuation, Open */
        "Pe",        /* Punctuation, Close */
        "Pi",        /* Punctuation, Initial quote */
        "Pf",        /* Punctuation, Final quote */
        "Pd",        /* Punctuation, Dash */
        "Pc",        /* Punctuation, Connector */
        "Po",        /* Punctuation, Other */
        "Sc",        /* Symbol, Currency */
        "Sm",        /* Symbol, Math */
        "So",         /* Symbol, Other */
        "Mn",        /* Mark, Non-Spacing */
        "Mc",        /* Mark, Spacing Combining */
        "Me",        /* Mark, Enclosing */
        "Zl",        /* Separator, Line */
        "Zp",        /* Separator, Paragraph */
        "Zs",        /* Separator, Space */
        "Cc",        /* Other, Control */
        "Cf",        /* Other, Format */
        "--",        /* Dummy, ignored */
        // Don't add anything after the Dummy entry!!
    };

    /**
     * A array of Unicode code points for each category.
     */
    private static int[][] categoryMap;


    /**
     * Generates CategoryMap for GenerateBreakIteratorData.
     */
    static void makeCategoryMap(String filename) {
        /* Overwrite specfile name */
        specfile = filename;

        /* Generate data in current format (1.5.0) */
        generateNewData();

        /* Copy generated data to cateogyMap */
        categoryMap = new int[categoryNames.length-1][];
        for (int i = 0; i < categoryNames.length-1; i++) {
            int len = newListCount[BMP][i] + newListCount[nonBMP][i];
            categoryMap[i] = new int[len];
            System.arraycopy(newList[i], 0, categoryMap[i], 0, len);
        }
    }

    /**
     * Returns categoryMap for the given category.
     */
    static int[] getCategoryMap(int category) {
        return categoryMap[category];
    }


    /**
     * Only used for debugging and generating a test program.
     */
    public static void main(String[] args) {
        /* Parses command-line options */
        processArgs(args);

        /* Generates data in current format (1.5.0) */
        generateNewData();

        /*
         * Generates data in older format (1.4.X and earlier) and creates
         * the old CategoryMap if "oldFilename" is not null.
         */
        if (!oldDatafile.equals("")) {
            generateOldData();
            generateOldDatafile();
        }

        /* Displays summary of generated data */
         showSummary();

        /*
         * Generates a test program which compares the new data and the return
         * values of Character.getType().
         * and the old data and the new data.
         */
        generateTestProgram();
    }


    /**
     * Spec (Unicode data file)
     */
    private static String specfile = "UnicodeData.txt";

    /**
     * Output directory
     */
    private static String outputDir = "";

    /**
     * Old data filename
     */
    private static String oldDatafile = "";

    /**
     * Parses the specified arguments and sets up the variables.
     */
    private static void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg =args[i];
            if (arg.equals("-spec")) {
                specfile = args[++i];
            } else if (arg.equals("-old")) {
                oldDatafile = args[++i];
            } else if (arg.equals("-o")) {
                outputDir = args[++i];
            } else {
                System.err.println("Usage: java CharacterCategory [-spec specfile]");
                System.exit(1);
            }
        }
    }


    /**
     * Displays summary of generated data
     */
    private static void showSummary() {
        int oldSum = 0;
        int newSum = 0;
        int oldSuppSum = 0;
        int newSuppSum = 0;

        for (int i = 0; i < categoryNames.length-1; i++) {
            int newNum = newListCount[BMP][i] + newListCount[nonBMP][i];

            if (oldTotalCount[i] != newNum) {
                System.err.println("Error: The number of generated data is different between the new approach and the old approach.");
            }
            if (oldListCount[SURROGATE][i] != newListCount[nonBMP][i]) {
                System.err.println("Error: The number of generated supplementarycharacters is different between the new approach and the old approach.");
            }

            System.out.println("    " + categoryNames[i] + ": " +
                               oldTotalCount[i] +
                               "(" + oldListCount[BEFORE][i] +
                               " + " + oldListCount[SURROGATE][i] +
                               " + " + oldListCount[AFTER][i] + ")" +
                               " --- " + newNum +
                               "(" + newListCount[BMP][i] +
                               " + " + newListCount[nonBMP][i] + ")");

            oldSum += oldListCount[BEFORE][i] * 2 +
                      oldListCount[SURROGATE][i] * 4 +
                      oldListCount[AFTER][i] * 2;
            newSum += newNum * 4 ;
            oldSuppSum += oldListCount[SURROGATE][i] * 4;
            newSuppSum += newListCount[nonBMP][i] * 4;
        }

        System.out.println("\nTotal buffer sizes are:\n    " +
                           oldSum + "bytes(Including " + oldSuppSum +
                           "bytes for supplementary characters)\n    " +
                           newSum + "bytes(Including " + newSuppSum +
                           "bytes for supplementary characters)");

        if (!ignoredOld.toString().equals(ignoredNew.toString())) {
            System.err.println("Ignored categories: Error: List mismatch: " +
                                ignoredOld + " vs. " + ignoredNew);
        } else {
            System.out.println("\nIgnored categories: " + ignoredOld);
            System.out.println("Please confirm that they aren't used in BreakIteratorRules.");
        }
    }


    private static final int HighSurrogate_CodeUnit_Start = 0xD800;
    private static final int LowSurrogate_CodeUnit_Start  = 0xDC00;
    private static final int Supplementary_CodePoint_Start    = 0x10000;


    private static StringBuffer ignoredOld = new StringBuffer();
    private static int[] oldTotalCount = new int[categoryNames.length];
    private static int[][] oldListCount = new int[3][categoryNames.length];
    private static int[][] oldListLen = new int[3][categoryNames.length];
    private static StringBuffer[][] oldList = new StringBuffer[3][categoryNames.length];

    private static final int BEFORE = 0;
    private static final int SURROGATE = 1;
    private static final int AFTER = 2;

    /**
     * Makes CategoryMap in ordler format which had been used by JDK 1.4.X and
     * earlier versions.
     */
    private static void generateOldData() {
        /* Initialize arrays. */
        for (int i = 0; i<categoryNames.length; i++) {
            for (int j = BEFORE; j <= AFTER; j++) {
                oldListCount[j][i] = 0;
                oldList[j][i] = new StringBuffer();
                oldListLen[j][i] = 17;
            }
        }

        storeOldData();

        if (oldTotalCount[categoryNames.length-1] != 1) {
            System.err.println("This should not happen. Unicode data which belongs to an undefined category exists");
            System.exit(1);
        }
    }

    private static void storeOldData() {
        try {
            FileReader fin = new FileReader(specfile);
            BufferedReader bin = new BufferedReader(fin);

            String prevCode = "????";
            String line;
            int prevIndex = categoryNames.length - 1;
            int prevCodeValue = -1;
            int curCodeValue = 0;
            boolean setFirst = false;

            while ((line = bin.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(line, ";");
                String code = st.nextToken();

                char c = code.charAt(0);
                if (c == '#' || c == '/') {
                    continue;
                }

                int i = Integer.valueOf(code, 16).intValue();

                String characterName = st.nextToken();
                String category = st.nextToken();

                int index;
                for (index = 0; index < categoryNames.length; index++) {
                    if (category.equals(categoryNames[index])) {
                        break;
                    }
                }

                if (index != categoryNames.length) {
                    curCodeValue = Integer.parseInt(code, 16);
                    if (prevIndex != index) {
                        appendOldChar(prevIndex, prevCodeValue, prevCode);
                        appendOldChar(index, curCodeValue, code);
                        prevIndex = index;
                    } else if (prevCodeValue != curCodeValue - 1) {
                        if (setFirst && characterName.endsWith(" Last>")) {
                            setFirst = false;
                        } else {
                            appendOldChar(prevIndex, prevCodeValue, prevCode);
                            appendOldChar(index, curCodeValue, code);
                        }
                    }
                    prevCodeValue = curCodeValue;
                    prevCode = code;
                    if (characterName.endsWith(" First>")) {
                        setFirst = true;
                    }
                } else {
                    if (ignoredOld.indexOf(category) == -1) {
                        ignoredOld.append(category);
                        ignoredOld.append(' ');
                    }
                }
            }
            appendOldChar(prevIndex, prevCodeValue, prevCode);

            bin.close();
            fin.close();
        }
        catch (Exception e) {
            throw new InternalError(e.toString());
        }
    }

    private static void appendOldChar(int index, int code, String s) {
        int range;
        if (code < HighSurrogate_CodeUnit_Start) {
            range = BEFORE;
        } else if (code < Supplementary_CodePoint_Start) {
            range = AFTER;
        } else {
            range = SURROGATE;
        }

        if (oldListLen[range][index] > 64) {
            oldList[range][index].append("\"\n                + \"");
            oldListLen[range][index] = 19;
        }

        if (code == 0x22 || code == 0x5c) {
            oldList[range][index].append('\\');
            oldList[range][index].append((char)code);
            oldListLen[range][index] += 2;
        } else if (code > 0x20 && code < 0x7F) {
            oldList[range][index].append((char)code);
            oldListLen[range][index] ++;
        } else {
            if (range == SURROGATE) {// Need to convert code point to code unit
                oldList[range][index].append(toCodeUnit(code));
                oldListLen[range][index] += 12;
            } else {
                oldList[range][index].append("\\u");
                oldList[range][index].append(s);
                oldListLen[range][index] += 6;
            }
        }
        oldListCount[range][index] ++;
        oldTotalCount[index]++;
    }

    private static String toCodeUnit(int i) {
        StringBuffer sb = new StringBuffer();
        sb.append("\\u");
        sb.append(Integer.toString((i - Supplementary_CodePoint_Start) / 0x400 + HighSurrogate_CodeUnit_Start, 16).toUpperCase());
        sb.append("\\u");
        sb.append(Integer.toString(i % 0x400 + LowSurrogate_CodeUnit_Start, 16).toUpperCase());
        return sb.toString();
    }

    private static int toCodePoint(String s) {
        char c1 = s.charAt(0);

        if (s.length() == 1 || !Character.isHighSurrogate(c1)) {
            return (int)c1;
        } else {
            char c2 = s.charAt(1);
            if (s.length() != 2 || !Character.isLowSurrogate(c2)) {
                return -1;
            }
            return Character.toCodePoint(c1, c2);
        }
    }


    private static StringBuffer ignoredNew = new StringBuffer();
    private static int[] newTotalCount = new int[categoryNames.length];
    private static int[][] newListCount = new int[2][categoryNames.length];
    private static int[][] newList = new int[categoryNames.length][];

    private static final int BMP = 0;
    private static final int nonBMP = 1;

    /**
     * Makes CategoryMap in newer format which is used by JDK 1.5.0.
     */
    private static void generateNewData() {
        /* Initialize arrays. */
        for (int i = 0; i<categoryNames.length; i++) {
            newList[i] = new int[10];
        }

        storeNewData();

        if (newListCount[BMP][categoryNames.length-1] != 1) {
            System.err.println("This should not happen. Unicode data which belongs to an undefined category exists");
            System.exit(1);
        }
    }

    private static void storeNewData() {
        try {
            FileReader fin = new FileReader(specfile);
            BufferedReader bin = new BufferedReader(fin);

            String line;
            int prevIndex = categoryNames.length - 1;
            int prevCodeValue = -1;
            int curCodeValue = 0;
            boolean setFirst = false;

            while ((line = bin.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(line, ";");
                String code = st.nextToken();

                char c = code.charAt(0);
                if (c == '#' || c == '/') {
                    continue;
                }

                int i = Integer.valueOf(code, 16).intValue();

                String characterName = st.nextToken();
                String category = st.nextToken();

                int index;
                for (index = 0; index < categoryNames.length; index++) {
                    if (category.equals(categoryNames[index])) {
                        break;
                    }
                }

                if (index != categoryNames.length) {
                    curCodeValue = Integer.parseInt(code, 16);
                    if (prevIndex == index) {
                        if (setFirst) {
                            if (characterName.endsWith(" Last>")) {
                                setFirst = false;
                            } else {
                                System.err.println("*** Error 1 at " + code);
                            }
                        } else {
                            if (characterName.endsWith(" First>")) {
                                setFirst = true;
                            } else if (characterName.endsWith(" Last>")) {
                                System.err.println("*** Error 2 at " + code);
                            } else {
                                if (prevCodeValue != curCodeValue - 1) {
                                    appendNewChar(prevIndex, prevCodeValue);
                                    appendNewChar(index, curCodeValue);
                                }
                            }
                        }
                    } else {
                        if (setFirst) {
                            System.err.println("*** Error 3 at " + code);
                        } else if (characterName.endsWith(" First>")) {
                            setFirst = true;
                        } else if (characterName.endsWith(" Last>")) {
                            System.err.println("*** Error 4 at " + code);
                        }
                        appendNewChar(prevIndex, prevCodeValue);
                        appendNewChar(index, curCodeValue);
                        prevIndex = index;
                    }
                    prevCodeValue = curCodeValue;
                } else {
                    if (ignoredNew.indexOf(category) == -1) {
                        ignoredNew.append(category);
                        ignoredNew.append(' ');
                    }
                }
            }
            appendNewChar(prevIndex, prevCodeValue);

            bin.close();
            fin.close();
        }
        catch (Exception e) {
            System.err.println("Error occurred on accessing " + specfile);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void appendNewChar(int index, int code) {
        int bufLen = newList[index].length;
        if (newTotalCount[index] == bufLen) {
            int[] tmpBuf = new int[bufLen + 10];
            System.arraycopy(newList[index], 0, tmpBuf, 0, bufLen);
            newList[index] = tmpBuf;
        }

        newList[index][newTotalCount[index]++] = code;
        if (code < 0x10000) {
            newListCount[BMP][index]++;
        } else {
            newListCount[nonBMP][index]++;
        }
    }


    /* Generates the old CategoryMap. */
    private static void generateOldDatafile() {
        try {
            FileWriter fout = new FileWriter(oldDatafile);
            BufferedWriter bout = new BufferedWriter(fout);

            bout.write("\n    //\n    // The following String[][] can be used in CharSet.java as is.\n    //\n\n    private static final String[][] categoryMap = {\n");
            for (int i = 0; i < categoryNames.length - 1; i++) {
                if (oldTotalCount[i] != 0) {
                    bout.write("        { \"" + categoryNames[i] + "\",");

                    /* 0x0000-0xD7FF */
                    if (oldListCount[BEFORE][i] != 0) {
                        bout.write(" \"");

                        bout.write(oldList[BEFORE][i].toString() + "\"\n");
                    }

                    /* 0xD800-0xFFFF */
                    if (oldListCount[AFTER][i] != 0) {
                        if (oldListCount[BEFORE][i] != 0) {
                            bout.write("                + \"");
                        } else {
                            bout.write(" \"");
                        }
                        bout.write(oldList[AFTER][i].toString() + "\"\n");
                    }

                    /* 0xD800DC00(0x10000)-0xDBFF0xDFFFF(0x10FFFF) */
                    if (oldListCount[SURROGATE][i] != 0) {
                        if (oldListCount[BEFORE][i] != 0 || oldListCount[AFTER][i] != 0) {
                            bout.write("                + \"");
                        } else {
                            bout.write(" \"");
                        }
                        bout.write(oldList[SURROGATE][i].toString() + "\"\n");
                    }
                    bout.write("        },\n");

                }
            }
            bout.write("    };\n\n");
            bout.close();
            fout.close();
        }
        catch (Exception e) {
            System.err.println("Error occurred on accessing " + oldDatafile);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n" + oldDatafile + " has been generated.");
    }


    /**
     * Test program to be generated
     */
    private static final String outfile = "CharacterCategoryTest.java";

    /*
     * Generates a test program which compare the generated date (newer one)
     * with the return values of Characger.getType().
     */
    private static void generateTestProgram() {
        try {
            FileWriter fout = new FileWriter(outfile);
            BufferedWriter bout = new BufferedWriter(fout);

            bout.write(collationMethod);
            bout.write("\n    //\n    // The following arrays can be used in CharSet.java as is.\n    //\n\n");

            bout.write("    private static final String[] categoryNames = {");
            for (int i = 0; i < categoryNames.length - 1; i++) {
                if (i % 10 == 0) {
                    bout.write("\n        ");
                }
                bout.write("\"" + categoryNames[i] + "\", ");
            }
            bout.write("\n    };\n\n");

            bout.write("    private static final int[][] categoryMap = {\n");

            for (int i = 0; i < categoryNames.length - 1; i++) {
                StringBuffer sb = new StringBuffer("        { /*  Data for \"" + categoryNames[i] + "\" category */");

                for (int j = 0; j < newTotalCount[i]; j++) {
                    if (j % 8 == 0) {
                        sb.append("\n        ");
                    }
                    sb.append(" 0x");
                    sb.append(Integer.toString(newList[i][j], 16).toUpperCase());
                    sb.append(',');
                }
                sb.append("\n        },\n");
                bout.write(sb.toString());
            }

            bout.write("    };\n");

            bout.write("\n}\n");

            bout.close();
            fout.close();
        }
        catch (Exception e) {
            System.err.println("Error occurred on accessing " + outfile);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n" + outfile + " has been generated.");
    }

    static String collationMethod =
"public class CharacterCategoryTest {\n\n" +
"    static final int SIZE = 0x110000;\n" +
"    static final String[] category = {\n" +
"       \"Cn\", \"Lu\", \"Ll\", \"Lt\", \"Lm\", \"Lo\", \"Mn\", \"Me\",\n" +
"       \"Mc\", \"Nd\", \"Nl\", \"No\", \"Zs\", \"Zl\", \"Zp\", \"Cc\",\n" +
"       \"Cf\", \"\",   \"Co\", \"Cs\", \"Pd\", \"Ps\", \"Pe\", \"Pc\",\n" +
"       \"Po\", \"Sm\", \"Sc\", \"Sk\", \"So\", \"Pi\", \"Pf\"\n" +
"    };\n\n" +
"    public static void main(String[] args) {\n" +
"        boolean err = false;\n" +
"        byte[] b = new byte[SIZE];\n" +
"        for (int i = 0; i < SIZE; i++) {\n" +
"            b[i] = 0;\n" +
"        }\n" +
"        for (int i = 0; i < categoryMap.length; i++) {\n" +
"            byte categoryNum = 0;\n" +
"            String categoryName = categoryNames[i];\n" +
"            for (int j = 0; j < category.length; j++) {\n" +
"                if (categoryName.equals(category[j])) {\n" +
"                    categoryNum = (byte)j;\n" +
"                    break;\n" +
"                }\n" +
"            }\n" +
"            int[] values = categoryMap[i];\n" +
"            for (int j = 0; j < values.length;) {\n" +
"                int firstChar = values[j++];\n" +
"                int lastChar = values[j++];\n" +
"                for (int k = firstChar; k <= lastChar; k++) {\n" +
"                    b[k] = categoryNum;\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"        for (int i = 0; i < SIZE; i++) {\n" +
"            int characterType = Character.getType(i);\n" +
"            if (b[i] != characterType) {\n" +
"                /* Co, Cs and Sk categories are ignored in CharacterCategory. */\n" +
"                if (characterType == Character.PRIVATE_USE ||\n" +
"                    characterType == Character.SURROGATE ||\n" +
"                    characterType == Character.MODIFIER_SYMBOL) {\n" +
"                    continue;\n" +
"                }\n" +
"                err = true;\n" +
"                System.err.println(\"Category conflict for a character(0x\" +\n" +
"                                   Integer.toHexString(i) +\n" +
"                                   \"). CharSet.categoryMap:\" +\n" +
"                                   category[b[i]] +\n" +
"                                   \"  Character.getType():\" +\n" +
"                                   category[characterType]);\n" +
"            }\n" +
"        }\n\n" +
"        if (err) {\n" +
"            throw new RuntimeException(\"Conflict occurred between Charset.categoryMap and Character.getType()\");\n" +
"        }\n" +
"    }\n";

}
