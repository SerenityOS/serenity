/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import sun.font.CompositeFontDescriptor;
import sun.font.SunFontManager;
import sun.font.FontManagerFactory;
import sun.font.FontUtilities;
import sun.util.logging.PlatformLogger;

/**
 * Provides the definitions of the five logical fonts: Serif, SansSerif,
 * Monospaced, Dialog, and DialogInput. The necessary information
 * is obtained from fontconfig files.
 */
public abstract class FontConfiguration {

    //static global runtime env
    protected static String osVersion;
    protected static String osName;
    protected static String encoding; // canonical name of default nio charset
    protected static Locale startupLocale = null;
    protected static Hashtable<String, String> localeMap = null;
    private static FontConfiguration fontConfig;
    private static PlatformLogger logger;
    protected static boolean isProperties = true;

    protected SunFontManager fontManager;
    protected boolean preferLocaleFonts;
    protected boolean preferPropFonts;

    private File fontConfigFile;
    private boolean foundOsSpecificFile;
    private boolean inited;
    private String javaLib;

    /* A default FontConfiguration must be created before an alternate
     * one to ensure proper static initialisation takes place.
     */
    public FontConfiguration(SunFontManager fm) {
        if (FontUtilities.debugFonts()) {
            FontUtilities.logInfo("Creating standard Font Configuration");
        }
        if (FontUtilities.debugFonts() && logger == null) {
            logger = PlatformLogger.getLogger("sun.awt.FontConfiguration");
        }
        fontManager = fm;
        setOsNameAndVersion();  /* static initialization */
        setEncoding();          /* static initialization */
        /* Separating out the file location from the rest of the
         * initialisation, so the caller has the option of doing
         * something else if a suitable file isn't found.
         */
        findFontConfigFile();
    }

    public synchronized boolean init() {
        if (!inited) {
            this.preferLocaleFonts = false;
            this.preferPropFonts = false;
            setFontConfiguration();
            readFontConfigFile(fontConfigFile);
            initFontConfig();
            inited = true;
        }
        return true;
    }

    public FontConfiguration(SunFontManager fm,
                             boolean preferLocaleFonts,
                             boolean preferPropFonts) {
        fontManager = fm;
        if (FontUtilities.debugFonts()) {
            FontUtilities.logInfo("Creating alternate Font Configuration");
        }
        this.preferLocaleFonts = preferLocaleFonts;
        this.preferPropFonts = preferPropFonts;
        /* fontConfig should be initialised by default constructor, and
         * its data tables can be shared, since readFontConfigFile doesn't
         * update any other state. Also avoid a doPrivileged block.
         */
        initFontConfig();
    }

    /**
     * Fills in this instance's osVersion and osName members. By
     * default uses the system properties os.name and os.version;
     * subclasses may override.
     */
    protected void setOsNameAndVersion() {
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
    }

    private void setEncoding() {
        encoding = Charset.defaultCharset().name();
        startupLocale = SunToolkit.getStartupLocale();
    }

    /////////////////////////////////////////////////////////////////////
    // methods for loading the FontConfig file                         //
    /////////////////////////////////////////////////////////////////////

    public boolean foundOsSpecificFile() {
        return foundOsSpecificFile;
    }

    /* Smoke test to see if we can trust this configuration by testing if
     * the first slot of a composite font maps to an installed file.
     */
    public boolean fontFilesArePresent() {
        init();
        short fontNameID = compFontNameIDs[0][0][0];
        short fileNameID = getComponentFileID(fontNameID);
        final String fileName = mapFileName(getComponentFileName(fileNameID));
        @SuppressWarnings("removal")
        Boolean exists = java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Boolean>() {
                 public Boolean run() {
                     try {
                         File f = new File(fileName);
                         return Boolean.valueOf(f.exists());
                     }
                     catch (Exception e) {
                         return Boolean.FALSE;
                     }
                 }
                });
        return exists.booleanValue();
    }

    private void findFontConfigFile() {

        foundOsSpecificFile = true; // default assumption.
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new Error("java.home property not set");
        }
        javaLib = javaHome + File.separator + "lib";
        String javaConfFonts = javaHome +
                               File.separator + "conf" +
                               File.separator + "fonts";
        String userConfigFile = System.getProperty("sun.awt.fontconfig");
        if (userConfigFile != null) {
            fontConfigFile = new File(userConfigFile);
        } else {
            fontConfigFile = findFontConfigFile(javaConfFonts);
            if (fontConfigFile == null) {
                fontConfigFile = findFontConfigFile(javaLib);
            }
        }
    }

    private void readFontConfigFile(File f) {
        /* This is invoked here as readFontConfigFile is only invoked
         * once per VM, and always in a privileged context, thus the
         * directory containing installed fall back fonts is accessed
         * from this context
         */
        getInstalledFallbackFonts(javaLib);

        if (f != null) {
            try {
                FileInputStream in = new FileInputStream(f.getPath());
                if (isProperties) {
                    loadProperties(in);
                } else {
                    loadBinary(in);
                }
                in.close();
                if (FontUtilities.debugFonts()) {
                    logger.config("Read logical font configuration from " + f);
                }
            } catch (IOException e) {
                if (FontUtilities.debugFonts()) {
                    logger.config("Failed to read logical font configuration from " + f);
                }
            }
        }
        String version = getVersion();
        if (!"1".equals(version) && FontUtilities.debugFonts()) {
            logger.config("Unsupported fontconfig version: " + version);
        }
    }

    protected void getInstalledFallbackFonts(String javaLib) {
        String fallbackDirName = javaLib + File.separator +
            "fonts" + File.separator + "fallback";

        File fallbackDir = new File(fallbackDirName);
        if (fallbackDir.exists() && fallbackDir.isDirectory()) {
            String[] ttfs = fallbackDir.list(fontManager.getTrueTypeFilter());
            String[] t1s = fallbackDir.list(fontManager.getType1Filter());
            int numTTFs = (ttfs == null) ? 0 : ttfs.length;
            int numT1s = (t1s == null) ? 0 : t1s.length;
            int len = numTTFs + numT1s;
            if (numTTFs + numT1s == 0) {
                return;
            }
            installedFallbackFontFiles = new String[len];
            for (int i=0; i<numTTFs; i++) {
                installedFallbackFontFiles[i] =
                    fallbackDir + File.separator + ttfs[i];
            }
            for (int i=0; i<numT1s; i++) {
                installedFallbackFontFiles[i+numTTFs] =
                    fallbackDir + File.separator + t1s[i];
            }
            fontManager.registerFontsInDir(fallbackDirName);
        }
    }

    private File findImpl(String fname) {
        File f = new File(fname + ".properties");
        if (FontUtilities.debugFonts()) {
            logger.info("Looking for text fontconfig file : " + f);
        }
        if (f.canRead()) {
            if (FontUtilities.debugFonts()) {
                logger.info("Found file : " + f);
            }
            isProperties = true;
            return f;
        }
        f = new File(fname + ".bfc");
        if (FontUtilities.debugFonts()) {
            logger.info("Looking for binary fontconfig file : " + f);
        }
        if (f.canRead()) {
            if (FontUtilities.debugFonts()) {
                logger.info("Found file : " + f);
            }
            isProperties = false;
            return f;
        }
        return null;
    }

    private File findFontConfigFile(String dir) {
        if (!(new File(dir)).exists()) {
            return null;
        }
        String baseName = dir + File.separator + "fontconfig";
        File configFile;
        String osMajorVersion = null;
        if (osVersion != null && osName != null) {
            configFile = findImpl(baseName + "." + osName + "." + osVersion);
            if (configFile != null) {
                return configFile;
            }
            int decimalPointIndex = osVersion.indexOf('.');
            if (decimalPointIndex != -1) {
                osMajorVersion = osVersion.substring(0, osVersion.indexOf('.'));
                configFile = findImpl(baseName + "." + osName + "." + osMajorVersion);
                if (configFile != null) {
                    return configFile;
                }
            }
        }
        if (osName != null) {
            configFile = findImpl(baseName + "." + osName);
            if (configFile != null) {
                return configFile;
            }
        }
        if (osVersion != null) {
            configFile = findImpl(baseName + "." + osVersion);
            if (configFile != null) {
                return configFile;
            }
            if (osMajorVersion != null) {
                configFile = findImpl(baseName + "." + osMajorVersion);
                if (configFile != null) {
                    return configFile;
                }
            }
        }
        foundOsSpecificFile = false;

        configFile = findImpl(baseName);
        if (configFile != null) {
            return configFile;
        }
        if (FontUtilities.debugFonts()) {
            logger.info("Did not find a fontconfig file.");
        }
        return null;
    }

    /* Initialize the internal data tables from binary format font
     * configuration file.
     */
    public static void loadBinary(InputStream inStream) throws IOException {
        DataInputStream in = new DataInputStream(inStream);
        head = readShortTable(in, HEAD_LENGTH);
        int[] tableSizes = new int[INDEX_TABLEEND];
        for (int i = 0; i < INDEX_TABLEEND; i++) {
            tableSizes[i] = head[i + 1] - head[i];
        }
        table_scriptIDs       = readShortTable(in, tableSizes[INDEX_scriptIDs]);
        table_scriptFonts     = readShortTable(in, tableSizes[INDEX_scriptFonts]);
        table_elcIDs          = readShortTable(in, tableSizes[INDEX_elcIDs]);
        table_sequences        = readShortTable(in, tableSizes[INDEX_sequences]);
        table_fontfileNameIDs = readShortTable(in, tableSizes[INDEX_fontfileNameIDs]);
        table_componentFontNameIDs = readShortTable(in, tableSizes[INDEX_componentFontNameIDs]);
        table_filenames       = readShortTable(in, tableSizes[INDEX_filenames]);
        table_awtfontpaths    = readShortTable(in, tableSizes[INDEX_awtfontpaths]);
        table_exclusions      = readShortTable(in, tableSizes[INDEX_exclusions]);
        table_proportionals   = readShortTable(in, tableSizes[INDEX_proportionals]);
        table_scriptFontsMotif   = readShortTable(in, tableSizes[INDEX_scriptFontsMotif]);
        table_alphabeticSuffix   = readShortTable(in, tableSizes[INDEX_alphabeticSuffix]);
        table_stringIDs       = readShortTable(in, tableSizes[INDEX_stringIDs]);

        //StringTable cache
        stringCache = new String[table_stringIDs.length + 1];

        int len = tableSizes[INDEX_stringTable];
        byte[] bb = new byte[len * 2];
        table_stringTable = new char[len];
        in.read(bb);
        int i = 0, j = 0;
        while (i < len) {
           table_stringTable[i++] = (char)(bb[j++] << 8 | (bb[j++] & 0xff));
        }
        if (verbose) {
            dump();
        }
    }

    /* Generate a binary format font configuration from internal data
     * tables.
     */
    public static void saveBinary(OutputStream out) throws IOException {
        sanityCheck();

        DataOutputStream dataOut = new DataOutputStream(out);
        writeShortTable(dataOut, head);
        writeShortTable(dataOut, table_scriptIDs);
        writeShortTable(dataOut, table_scriptFonts);
        writeShortTable(dataOut, table_elcIDs);
        writeShortTable(dataOut, table_sequences);
        writeShortTable(dataOut, table_fontfileNameIDs);
        writeShortTable(dataOut, table_componentFontNameIDs);
        writeShortTable(dataOut, table_filenames);
        writeShortTable(dataOut, table_awtfontpaths);
        writeShortTable(dataOut, table_exclusions);
        writeShortTable(dataOut, table_proportionals);
        writeShortTable(dataOut, table_scriptFontsMotif);
        writeShortTable(dataOut, table_alphabeticSuffix);
        writeShortTable(dataOut, table_stringIDs);
        //stringTable
        dataOut.writeChars(new String(table_stringTable));
        out.close();
        if (verbose) {
            dump();
        }
    }

    //private static boolean loadingProperties;
    private static short stringIDNum;
    private static short[] stringIDs;
    private static StringBuilder stringTable;

    public static void loadProperties(InputStream in) throws IOException {
        //loadingProperties = true;
        //StringID starts from "1", "0" is reserved for "not defined"
        stringIDNum = 1;
        stringIDs = new short[1000];
        stringTable = new StringBuilder(4096);

        if (verbose && logger == null) {
            logger = PlatformLogger.getLogger("sun.awt.FontConfiguration");
        }
        new PropertiesHandler().load(in);

        //loadingProperties = false;
        stringIDs = null;
        stringTable = null;
    }


    /////////////////////////////////////////////////////////////////////
    // methods for initializing the FontConfig                         //
    /////////////////////////////////////////////////////////////////////

    /**
     *  set initLocale, initEncoding and initELC for this FontConfig object
     *  currently we just simply use the startup locale and encoding
     */
    private void initFontConfig() {
        initLocale = startupLocale;
        initEncoding = encoding;
        if (preferLocaleFonts && !willReorderForStartupLocale()) {
            preferLocaleFonts = false;
        }
        initELC = getInitELC();
        initAllComponentFonts();
    }

    //"ELC" stands for "Encoding.Language.Country". This method returns
    //the ID of the matched elc setting of "initLocale" in elcIDs table.
    //If no match is found, it returns the default ID, which is
    //"NULL.NULL.NULL" in elcIDs table.
    private short getInitELC() {
        if (initELC != -1) {
            return initELC;
        }
        HashMap <String, Integer> elcIDs = new HashMap<String, Integer>();
        for (int i = 0; i < table_elcIDs.length; i++) {
            elcIDs.put(getString(table_elcIDs[i]), i);
        }
        String language = initLocale.getLanguage();
        String country = initLocale.getCountry();
        String elc;
        if (elcIDs.containsKey(elc=initEncoding + "." + language + "." + country)
            || elcIDs.containsKey(elc=initEncoding + "." + language)
            || elcIDs.containsKey(elc=initEncoding)) {
            initELC = elcIDs.get(elc).shortValue();
        } else {
            initELC = elcIDs.get("NULL.NULL.NULL").shortValue();
        }
        int i = 0;
        while (i < table_alphabeticSuffix.length) {
            if (initELC == table_alphabeticSuffix[i]) {
                alphabeticSuffix = getString(table_alphabeticSuffix[i + 1]);
                return initELC;
            }
            i += 2;
        }
        return initELC;
    }

    public static boolean verbose;
    private short    initELC = -1;
    private Locale   initLocale;
    private String   initEncoding;
    private String   alphabeticSuffix;

    private short[][][] compFontNameIDs = new short[NUM_FONTS][NUM_STYLES][];
    private int[][][] compExclusions = new int[NUM_FONTS][][];
    private int[] compCoreNum = new int[NUM_FONTS];

    private Set<Short> coreFontNameIDs = new HashSet<Short>();
    private Set<Short> fallbackFontNameIDs = new HashSet<Short>();

    private void initAllComponentFonts() {
        short[] fallbackScripts = getFallbackScripts();
        for (int fontIndex = 0; fontIndex < NUM_FONTS; fontIndex++) {
            short[] coreScripts = getCoreScripts(fontIndex);
            compCoreNum[fontIndex] = coreScripts.length;
            /*
            System.out.println("coreScriptID=" + table_sequences[initELC * 5 + fontIndex]);
            for (int i = 0; i < coreScripts.length; i++) {
            System.out.println("  " + i + " :" + getString(table_scriptIDs[coreScripts[i]]));
            }
            */
            //init exclusionRanges
            int[][] exclusions = new int[coreScripts.length][];
            for (int i = 0; i < coreScripts.length; i++) {
                exclusions[i] = getExclusionRanges(coreScripts[i]);
            }
            compExclusions[fontIndex] = exclusions;
            //init componentFontNames
            for (int styleIndex = 0; styleIndex < NUM_STYLES; styleIndex++) {
                int index;
                short[] nameIDs = new short[coreScripts.length + fallbackScripts.length];
                //core
                for (index = 0; index < coreScripts.length; index++) {
                    nameIDs[index] = getComponentFontID(coreScripts[index],
                                               fontIndex, styleIndex);
                    if (preferLocaleFonts && localeMap != null &&
                            fontManager.usingAlternateFontforJALocales()) {
                        nameIDs[index] = remapLocaleMap(fontIndex, styleIndex,
                                                        coreScripts[index], nameIDs[index]);
                    }
                    if (preferPropFonts) {
                        nameIDs[index] = remapProportional(fontIndex, nameIDs[index]);
                    }
                    //System.out.println("nameid=" + nameIDs[index]);
                    coreFontNameIDs.add(nameIDs[index]);
                }
                //fallback
                for (int i = 0; i < fallbackScripts.length; i++) {
                    short id = getComponentFontID(fallbackScripts[i],
                                               fontIndex, styleIndex);
                    if (preferLocaleFonts && localeMap != null &&
                            fontManager.usingAlternateFontforJALocales()) {
                        id = remapLocaleMap(fontIndex, styleIndex, fallbackScripts[i], id);
                    }
                    if (preferPropFonts) {
                        id = remapProportional(fontIndex, id);
                    }
                    if (contains(nameIDs, id, index)) {
                        continue;
                    }
                    /*
                      System.out.println("fontIndex=" + fontIndex + ", styleIndex=" + styleIndex
                           + ", fbIndex=" + i + ",fbS=" + fallbackScripts[i] + ", id=" + id);
                    */
                    fallbackFontNameIDs.add(id);
                    nameIDs[index++] = id;
                }
                if (index < nameIDs.length) {
                    short[] newNameIDs = new short[index];
                    System.arraycopy(nameIDs, 0, newNameIDs, 0, index);
                    nameIDs = newNameIDs;
                }
                compFontNameIDs[fontIndex][styleIndex] = nameIDs;
            }
        }
   }

   private short remapLocaleMap(int fontIndex, int styleIndex, short scriptID, short fontID) {
        String scriptName = getString(table_scriptIDs[scriptID]);

        String value = localeMap.get(scriptName);
        if (value == null) {
            String fontName = fontNames[fontIndex];
            String styleName = styleNames[styleIndex];
            value = localeMap.get(fontName + "." + styleName + "." + scriptName);
        }
        if (value == null) {
            return fontID;
        }

        for (int i = 0; i < table_componentFontNameIDs.length; i++) {
            String name = getString(table_componentFontNameIDs[i]);
            if (value.equalsIgnoreCase(name)) {
                fontID = (short)i;
                break;
            }
        }
        return fontID;
    }

    public static boolean hasMonoToPropMap() {
        return table_proportionals != null && table_proportionals.length != 0;
    }

    private short remapProportional(int fontIndex, short id) {
    if (preferPropFonts &&
        table_proportionals.length != 0 &&
        fontIndex != 2 &&         //"monospaced"
        fontIndex != 4) {         //"dialoginput"
            int i = 0;
            while (i < table_proportionals.length) {
                if (table_proportionals[i] == id) {
                    return table_proportionals[i + 1];
                }
                i += 2;
            }
        }
        return id;
    }

    /////////////////////////////////////////////////////////////////////
    // Methods for handling font and style names                       //
    /////////////////////////////////////////////////////////////////////
    protected static final int NUM_FONTS = 5;
    protected static final int NUM_STYLES = 4;
    protected static final String[] fontNames
            = {"serif", "sansserif", "monospaced", "dialog", "dialoginput"};
    protected static final String[] publicFontNames
            = {Font.SERIF, Font.SANS_SERIF, Font.MONOSPACED, Font.DIALOG,
               Font.DIALOG_INPUT};
    protected static final String[] styleNames
            = {"plain", "bold", "italic", "bolditalic"};

    /**
     * Checks whether the given font family name is a valid logical font name.
     * The check is case insensitive.
     */
    public static boolean isLogicalFontFamilyName(String fontName) {
        return isLogicalFontFamilyNameLC(fontName.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Checks whether the given font family name is a valid logical font name.
     * The check is case sensitive.
     */
    public static boolean isLogicalFontFamilyNameLC(String fontName) {
        for (int i = 0; i < fontNames.length; i++) {
            if (fontName.equals(fontNames[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given style name is a valid logical font style name.
     */
    private static boolean isLogicalFontStyleName(String styleName) {
        for (int i = 0; i < styleNames.length; i++) {
            if (styleName.equals(styleNames[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given font face name is a valid logical font name.
     * The check is case insensitive.
     */
    public static boolean isLogicalFontFaceName(String fontName) {
        return isLogicalFontFaceNameLC(fontName.toLowerCase(Locale.ENGLISH));
    }

   /**
    * Checks whether the given font face name is a valid logical font name.
    * The check is case sensitive.
    */
    public static boolean isLogicalFontFaceNameLC(String fontName) {
        int period = fontName.indexOf('.');
        if (period >= 0) {
            String familyName = fontName.substring(0, period);
            String styleName = fontName.substring(period + 1);
            return isLogicalFontFamilyName(familyName) &&
                    isLogicalFontStyleName(styleName);
        } else {
            return isLogicalFontFamilyName(fontName);
        }
    }

    protected static int getFontIndex(String fontName) {
        return getArrayIndex(fontNames, fontName);
    }

    protected static int getStyleIndex(String styleName) {
        return getArrayIndex(styleNames, styleName);
    }

    private static int getArrayIndex(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) {
                return i;
            }
        }
        assert false;
        return 0;
    }

    protected static int getStyleIndex(int style) {
        switch (style) {
            case Font.PLAIN:
                return 0;
            case Font.BOLD:
                return 1;
            case Font.ITALIC:
                return 2;
            case Font.BOLD | Font.ITALIC:
                return 3;
            default:
                return 0;
        }
    }

    protected static String getFontName(int fontIndex) {
        return fontNames[fontIndex];
    }

    protected static String getStyleName(int styleIndex) {
        return styleNames[styleIndex];
    }

    /**
     * Returns the font face name for the given logical font
     * family name and style.
     * The style argument is interpreted as in java.awt.Font.Font.
     */
    public static String getLogicalFontFaceName(String familyName, int style) {
        assert isLogicalFontFamilyName(familyName);
        return familyName.toLowerCase(Locale.ENGLISH) + "." + getStyleString(style);
    }

    /**
     * Returns the string typically used in properties files
     * for the given style.
     * The style argument is interpreted as in java.awt.Font.Font.
     */
    public static String getStyleString(int style) {
        return getStyleName(getStyleIndex(style));
    }

    /**
     * Returns a fallback name for the given font name. For a few known
     * font names, matching logical font names are returned. For all
     * other font names, defaultFallback is returned.
     * defaultFallback differs between AWT and 2D.
     */
    public abstract String getFallbackFamilyName(String fontName, String defaultFallback);

    /**
     * Returns the 1.1 equivalent for some old 1.0 font family names for
     * which we need to maintain compatibility in some configurations.
     * Returns null for other font names.
     */
    protected String getCompatibilityFamilyName(String fontName) {
        fontName = fontName.toLowerCase(Locale.ENGLISH);
        if (fontName.equals("timesroman")) {
            return "serif";
        } else if (fontName.equals("helvetica")) {
            return "sansserif";
        } else if (fontName.equals("courier")) {
            return "monospaced";
        }
        return null;
    }

    protected static String[] installedFallbackFontFiles = null;

    /**
     * Maps a file name given in the font configuration file
     * to a format appropriate for the platform.
     */
    protected String mapFileName(String fileName) {
        return fileName;
    }

    //////////////////////////////////////////////////////////////////////
    //  reordering                                                      //
    //////////////////////////////////////////////////////////////////////

    /* Mappings from file encoding to font config name for font supporting
     * the corresponding language. This is filled in by initReorderMap()
     */
    protected HashMap<String, Object> reorderMap = null;

    /* Platform-specific mappings */
    protected abstract void initReorderMap();

    /* Move item at index "src" to "dst", shuffling all values in
     * between down
     */
    private void shuffle(String[] seq, int src, int dst) {
        if (dst >= src) {
            return;
        }
        String tmp = seq[src];
        for (int i=src; i>dst; i--) {
            seq[i] = seq[i-1];
        }
        seq[dst] = tmp;
    }

    /* Called to determine if there's a re-order sequence for this locale/
     * encoding. If there's none then the caller can "bail" and avoid
     * unnecessary work
     */
    public static boolean willReorderForStartupLocale() {
        return getReorderSequence() != null;
    }

    private static Object getReorderSequence() {
        if (fontConfig.reorderMap == null) {
             fontConfig.initReorderMap();
        }
        HashMap<String, Object> reorderMap = fontConfig.reorderMap;

        /* Find the most specific mapping */
        String language = startupLocale.getLanguage();
        String country = startupLocale.getCountry();
        Object val = reorderMap.get(encoding + "." + language + "." + country);
        if (val == null) {
            val = reorderMap.get(encoding + "." + language);
        }
        if (val == null) {
            val = reorderMap.get(encoding);
        }
        return val;
    }

    /* This method reorders the sequence such that the matches for the
     * file encoding are moved ahead of other elements.
     * If an encoding uses more than one font, they are all moved up.
     */
     private void reorderSequenceForLocale(String[] seq) {
        Object val =  getReorderSequence();
        if (val instanceof String) {
            for (int i=0; i< seq.length; i++) {
                if (seq[i].equals(val)) {
                    shuffle(seq, i, 0);
                    return;
                }
            }
        } else if (val instanceof String[]) {
            String[] fontLangs = (String[])val;
            for (int l=0; l<fontLangs.length;l++) {
                for (int i=0; i<seq.length;i++) {
                    if (seq[i].equals(fontLangs[l])) {
                        shuffle(seq, i, l);
                    }
                }
            }
        }
    }

    private static Vector<String> splitSequence(String sequence) {
        //String.split would be more convenient, but incurs big performance penalty
        Vector<String> parts = new Vector<>();
        int start = 0;
        int end;
        while ((end = sequence.indexOf(',', start)) >= 0) {
            parts.add(sequence.substring(start, end));
            start = end + 1;
        }
        if (sequence.length() > start) {
            parts.add(sequence.substring(start, sequence.length()));
        }
        return parts;
    }

    protected String[] split(String sequence) {
        Vector<String> v = splitSequence(sequence);
        return v.toArray(new String[0]);
    }

    ////////////////////////////////////////////////////////////////////////
    // Methods for extracting information from the fontconfig data for AWT//
    ////////////////////////////////////////////////////////////////////////
    private Hashtable<String, Charset> charsetRegistry = new Hashtable<>(5);

    /**
     * Returns FontDescriptors describing the physical fonts used for the
     * given logical font name and style. The font name is interpreted
     * in a case insensitive way.
     * The style argument is interpreted as in java.awt.Font.Font.
     */
    public FontDescriptor[] getFontDescriptors(String fontName, int style) {
        assert isLogicalFontFamilyName(fontName);
        fontName = fontName.toLowerCase(Locale.ENGLISH);
        int fontIndex = getFontIndex(fontName);
        int styleIndex = getStyleIndex(style);
        return getFontDescriptors(fontIndex, styleIndex);
    }
    private FontDescriptor[][][] fontDescriptors =
        new FontDescriptor[NUM_FONTS][NUM_STYLES][];

    private FontDescriptor[] getFontDescriptors(int fontIndex, int styleIndex) {
        FontDescriptor[] descriptors = fontDescriptors[fontIndex][styleIndex];
        if (descriptors == null) {
            descriptors = buildFontDescriptors(fontIndex, styleIndex);
            fontDescriptors[fontIndex][styleIndex] = descriptors;
        }
        return descriptors;
    }

    protected FontDescriptor[] buildFontDescriptors(int fontIndex, int styleIndex) {
        String fontName = fontNames[fontIndex];
        String styleName = styleNames[styleIndex];

        short[] scriptIDs = getCoreScripts(fontIndex);
        short[] nameIDs = compFontNameIDs[fontIndex][styleIndex];
        String[] sequence = new String[scriptIDs.length];
        String[] names = new String[scriptIDs.length];
        for (int i = 0; i < sequence.length; i++) {
            names[i] = getComponentFontName(nameIDs[i]);
            sequence[i] = getScriptName(scriptIDs[i]);
            if (alphabeticSuffix != null && "alphabetic".equals(sequence[i])) {
                sequence[i] = sequence[i] + "/" + alphabeticSuffix;
            }
        }
        int[][] fontExclusionRanges = compExclusions[fontIndex];

        FontDescriptor[] descriptors = new FontDescriptor[names.length];

        for (int i = 0; i < names.length; i++) {
            String awtFontName;
            String encoding;

            awtFontName = makeAWTFontName(names[i], sequence[i]);

            // look up character encoding
            encoding = getEncoding(names[i], sequence[i]);
            if (encoding == null) {
                encoding = "default";
            }
            CharsetEncoder enc
                    = getFontCharsetEncoder(encoding.trim(), awtFontName);

            // we already have the exclusion ranges
            int[] exclusionRanges = fontExclusionRanges[i];

            // create descriptor
            descriptors[i] = new FontDescriptor(awtFontName, enc, exclusionRanges);
        }
        return descriptors;
    }

    /**
     * Returns the AWT font name for the given platform font name and
     * character subset.
     */
    protected String makeAWTFontName(String platformFontName,
            String characterSubsetName) {
        return platformFontName;
    }

    /**
     * Returns the java.io name of the platform character encoding for the
     * given AWT font name and character subset. May return "default"
     * to indicate that getDefaultFontCharset should be called to obtain
     * a charset encoder.
     */
    protected abstract String getEncoding(String awtFontName,
            String characterSubsetName);

    private CharsetEncoder getFontCharsetEncoder(final String charsetName,
            String fontName) {

        Charset fc = null;
        if (charsetName.equals("default")) {
            fc = charsetRegistry.get(fontName);
        } else {
            fc = charsetRegistry.get(charsetName);
        }
        if (fc != null) {
            return fc.newEncoder();
        }

        if (!charsetName.startsWith("sun.awt.") && !charsetName.equals("default")) {
            fc = Charset.forName(charsetName);
        } else {
            @SuppressWarnings("removal")
            Class<?> fcc = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                    public Class<?> run() {
                        try {
                            return Class.forName(charsetName, true,
                                                 ClassLoader.getSystemClassLoader());
                        } catch (ClassNotFoundException e) {
                        }
                        return null;
                    }
                });

            if (fcc != null) {
                try {
                    fc = (Charset) fcc.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                }
            }
        }
        if (fc == null) {
            fc = getDefaultFontCharset(fontName);
        }

        if (charsetName.equals("default")){
            charsetRegistry.put(fontName, fc);
        } else {
            charsetRegistry.put(charsetName, fc);
        }
        return fc.newEncoder();
    }

    protected abstract Charset getDefaultFontCharset(
            String fontName);

    /* This retrieves the platform font directories (path) calculated
     * by setAWTFontPathSequence(String[]). The default implementation
     * returns null, its expected that X11 platforms may return
     * non-null.
     */
    public HashSet<String> getAWTFontPathSet() {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////
    // methods for extracting information from the fontconfig data for 2D //
    ////////////////////////////////////////////////////////////////////////

    /**
     * Returns an array of composite font descriptors for all logical font
     * faces.
     */
    public CompositeFontDescriptor[] get2DCompositeFontInfo() {
        CompositeFontDescriptor[] result =
                new CompositeFontDescriptor[NUM_FONTS * NUM_STYLES];
        String defaultFontFile = fontManager.getDefaultFontFile();
        String defaultFontFaceName = fontManager.getDefaultFontFaceName();

        for (int fontIndex = 0; fontIndex < NUM_FONTS; fontIndex++) {
            String fontName = publicFontNames[fontIndex];

            // determine exclusion ranges for font
            // AWT uses separate exclusion range array per component font.
            // 2D packs all range boundaries into one array.
            // Both use separate entries for lower and upper boundary.
            int[][] exclusions = compExclusions[fontIndex];
            int numExclusionRanges = 0;
            for (int i = 0; i < exclusions.length; i++) {
                numExclusionRanges += exclusions[i].length;
            }
            int[] exclusionRanges = new int[numExclusionRanges];
            int[] exclusionRangeLimits = new int[exclusions.length];
            int exclusionRangeIndex = 0;
            int exclusionRangeLimitIndex = 0;
            for (int i = 0; i < exclusions.length; i++) {
                int[] componentRanges = exclusions[i];
                for (int j = 0; j < componentRanges.length; ) {
                    int value = componentRanges[j];
                    exclusionRanges[exclusionRangeIndex++] = componentRanges[j++];
                    exclusionRanges[exclusionRangeIndex++] = componentRanges[j++];
                }
                exclusionRangeLimits[i] = exclusionRangeIndex;
            }
            // other info is per style
            for (int styleIndex = 0; styleIndex < NUM_STYLES; styleIndex++) {
                int maxComponentFontCount = compFontNameIDs[fontIndex][styleIndex].length;
                // fall back fonts listed in the lib/fonts/fallback directory
                if (installedFallbackFontFiles != null) {
                    maxComponentFontCount += installedFallbackFontFiles.length;
                }
                String faceName = fontName + "." + styleNames[styleIndex];

                // determine face names and file names of component fonts
                String[] componentFaceNames = new String[maxComponentFontCount];
                String[] componentFileNames = new String[maxComponentFontCount];

                int index;
                for (index = 0; index < compFontNameIDs[fontIndex][styleIndex].length; index++) {
                    short fontNameID = compFontNameIDs[fontIndex][styleIndex][index];
                    short fileNameID = getComponentFileID(fontNameID);
                    componentFaceNames[index] = getFaceNameFromComponentFontName(getComponentFontName(fontNameID));
                    componentFileNames[index] = mapFileName(getComponentFileName(fileNameID));
                    if (componentFileNames[index] == null ||
                        needToSearchForFile(componentFileNames[index])) {
                        componentFileNames[index] = getFileNameFromComponentFontName(getComponentFontName(fontNameID));
                    }
                    /*
                    System.out.println(publicFontNames[fontIndex] + "." + styleNames[styleIndex] + "."
                        + getString(table_scriptIDs[coreScripts[index]]) + "=" + componentFileNames[index]);
                    */
                }

                if (installedFallbackFontFiles != null) {
                    for (int ifb=0; ifb<installedFallbackFontFiles.length; ifb++) {
                        componentFaceNames[index] = null;
                        componentFileNames[index] = installedFallbackFontFiles[ifb];
                        index++;
                    }
                }

                if (index < maxComponentFontCount) {
                    String[] newComponentFaceNames = new String[index];
                    System.arraycopy(componentFaceNames, 0, newComponentFaceNames, 0, index);
                    componentFaceNames = newComponentFaceNames;
                    String[] newComponentFileNames = new String[index];
                    System.arraycopy(componentFileNames, 0, newComponentFileNames, 0, index);
                    componentFileNames = newComponentFileNames;
                }
                // exclusion range limit array length must match component face name
                // array length - native code relies on this

                int[] clippedExclusionRangeLimits = exclusionRangeLimits;
                if (index != clippedExclusionRangeLimits.length) {
                    int len = exclusionRangeLimits.length;
                    clippedExclusionRangeLimits = new int[index];
                    System.arraycopy(exclusionRangeLimits, 0, clippedExclusionRangeLimits, 0, len);
                    //padding for various fallback fonts
                    for (int i = len; i < index; i++) {
                        clippedExclusionRangeLimits[i] = exclusionRanges.length;
                    }
                }
                /*
                System.out.println(faceName + ":");
                for (int i = 0; i < componentFileNames.length; i++) {
                    System.out.println("    " + componentFaceNames[i]
                         + "  -> " + componentFileNames[i]);
                }
                */
                result[fontIndex * NUM_STYLES + styleIndex]
                        = new CompositeFontDescriptor(
                            faceName,
                            compCoreNum[fontIndex],
                            componentFaceNames,
                            componentFileNames,
                            exclusionRanges,
                            clippedExclusionRangeLimits);
            }
        }
        return result;
    }

    protected abstract String getFaceNameFromComponentFontName(String componentFontName);
    protected abstract String getFileNameFromComponentFontName(String componentFontName);

    /*
    public class 2dFont {
        public String platformName;
        public String fontfileName;
    }
    private 2dFont [] componentFonts = null;
    */

    /* Used on Linux to test if a file referenced in a font configuration
     * file exists in the location that is expected. If it does, no need
     * to search for it. If it doesn't then unless its a fallback font,
     * return that expensive code should be invoked to search for the font.
     */
    HashMap<String, Boolean> existsMap;
    public boolean needToSearchForFile(String fileName) {
        if (!FontUtilities.isLinux) {
            return false;
        } else if (existsMap == null) {
           existsMap = new HashMap<String, Boolean>();
        }
        Boolean exists = existsMap.get(fileName);
        if (exists == null) {
            /* call getNumberCoreFonts() to ensure these are initialised, and
             * if this file isn't for a core component, ie, is a for a fallback
             * font which very typically isn't available, then can't afford
             * to take the start-up penalty to search for it.
             */
            getNumberCoreFonts();
            if (!coreFontFileNames.contains(fileName)) {
                exists = Boolean.TRUE;
            } else {
                exists = Boolean.valueOf((new File(fileName)).exists());
                existsMap.put(fileName, exists);
                if (FontUtilities.debugFonts() &&
                    exists == Boolean.FALSE) {
                    logger.warning("Couldn't locate font file " + fileName);
                }
            }
        }
        return exists == Boolean.FALSE;
    }

    private int numCoreFonts = -1;
    private String[] componentFonts = null;
    HashMap <String, String> filenamesMap = new HashMap<String, String>();
    HashSet <String> coreFontFileNames = new HashSet<String>();

    /* Return the number of core fonts. Note this isn't thread safe but
     * a calling thread can call this and getPlatformFontNames() in either
     * order.
     */
    public int getNumberCoreFonts() {
        if (numCoreFonts == -1) {
            numCoreFonts = coreFontNameIDs.size();
            Short[] emptyShortArray = new Short[0];
            Short[] core = coreFontNameIDs.toArray(emptyShortArray);
            Short[] fallback = fallbackFontNameIDs.toArray(emptyShortArray);

            int numFallbackFonts = 0;
            int i;
            for (i = 0; i < fallback.length; i++) {
                if (coreFontNameIDs.contains(fallback[i])) {
                    fallback[i] = null;
                    continue;
                }
                numFallbackFonts++;
            }
            componentFonts = new String[numCoreFonts + numFallbackFonts];
            String filename = null;
            for (i = 0; i < core.length; i++) {
                short fontid = core[i];
                short fileid = getComponentFileID(fontid);
                componentFonts[i] = getComponentFontName(fontid);
                String compFileName = getComponentFileName(fileid);
                if (compFileName != null) {
                    coreFontFileNames.add(compFileName);
                }
                filenamesMap.put(componentFonts[i], mapFileName(compFileName));
            }
            for (int j = 0; j < fallback.length; j++) {
                if (fallback[j] != null) {
                    short fontid = fallback[j];
                    short fileid = getComponentFileID(fontid);
                    componentFonts[i] = getComponentFontName(fontid);
                    filenamesMap.put(componentFonts[i],
                                     mapFileName(getComponentFileName(fileid)));
                    i++;
                }
            }
        }
        return numCoreFonts;
    }

    /* Return all platform font names used by this font configuration.
     * The first getNumberCoreFonts() entries are guaranteed to be the
     * core fonts - ie no fall back only fonts.
     */
    public String[] getPlatformFontNames() {
        if (numCoreFonts == -1) {
            getNumberCoreFonts();
        }
        return componentFonts;
    }

    /**
     * Returns a file name for the physical font represented by this platform font name,
     * if the font configuration has such information available, or null if the
     * information is unavailable. The file name returned is just a hint; a null return
     * value doesn't necessarily mean that the font is unavailable, nor does a non-null
     * return value guarantee that the file exists and contains the physical font.
     * The file name can be an absolute or a relative path name.
     */
    public String getFileNameFromPlatformName(String platformName) {
        // get2DCompositeFontInfo
        //     ->  getFileNameFromComponentfontName()  (W/M)
        //       ->   getFileNameFromPlatformName()
        // it's a waste of time on Win32, but I have to give X11 a chance to
        // call getFileNameFromXLFD()
        return filenamesMap.get(platformName);
    }

    /**
     * Returns a configuration specific path to be appended to the font
     * search path.
     */
    public String getExtraFontPath() {
        return getString(head[INDEX_appendedfontpath]);
    }

    public String getVersion() {
        return getString(head[INDEX_version]);
    }

    /* subclass support */
    protected static FontConfiguration getFontConfiguration() {
        return fontConfig;
    }

    protected void setFontConfiguration() {
        fontConfig = this;      /* static initialization */
    }

    //////////////////////////////////////////////////////////////////////
    // FontConfig data tables and the index constants in binary file    //
    //////////////////////////////////////////////////////////////////////
    /* The binary font configuration file begins with a short[] "head", which
     * contains the offsets to the starts of the individual data table which
     * immediately follow. The current implementation includes the tables shown
     * below.
     *
     * (00) table_scriptIDs    :stringIDs of all defined CharacterSubsetNames
     * (01) table_scriptFonts  :scriptID x fontIndex x styleIndex->
     *                          PlatformFontNameID mapping. Each scriptID might
     *                          have 1 or 20 entries depends on if it is defined
     *                          via a "allfonts.CharacterSubsetname" or a list of
     *                          "LogicalFontName.StyleName.CharacterSubsetName"
     *                          entries, positive entry means it's a "allfonts"
     *                          entry, a negative value means this is a offset to
     *                          a NUM_FONTS x NUM_STYLES subtable.
     * (02) table_elcIDs       :stringIDs of all defined ELC names, string
     *                          "NULL.NULL.NULL" is used for "default"
     * (03) table_sequences    :elcID x logicalFont -> scriptIDs table defined
     *                          by "sequence.allfonts/LogicalFontName.ELC" in
     *                          font configuration file, each "elcID" has
     *                          NUM_FONTS (5) entries in this table.
     * (04) table_fontfileNameIDs
     *                         :stringIDs of all defined font file names
     * (05) table_componentFontNameIDs
     *                         :stringIDs of all defined PlatformFontNames
     * (06) table_filenames    :platformFontNamesID->fontfileNameID mapping
     *                          table, the index is the platformFontNamesID.
     * (07) table_awtfontpaths :CharacterSubsetNames->awtfontpaths mapping table,
     *                          the index is the CharacterSubsetName's stringID
     *                          and content is the stringID of awtfontpath.
     * (08) table_exclusions   :scriptID -> exclusionRanges mapping table,
     *                          the index is the scriptID and the content is
                                a id of an exclusionRanges int[].
     * (09) table_proportionals:list of pairs of PlatformFontNameIDs, stores
     *                          the replacement info defined by "proportional"
     *                          keyword.
     * (10) table_scriptFontsMotif
     *                         :same as (01) except this table stores the
     *                          info defined with ".motif" keyword
     * (11) table_alphabeticSuffix
     *                         :elcID -> stringID of alphabetic/XXXX entries
     * (12) table_stringIDs    :The index of this table is the string ID, the
     *                          content is the "start index" of this string in
     *                          stringTable, use the start index of next entry
     *                          as the "end index".
     * (13) table_stringTable  :The real storage of all character strings defined
     *                          /used this font configuration, need a pair of
     *                          "start" and "end" indices to access.
     * (14) reserved
     * (15) table_fallbackScripts
     *                         :stringIDs of fallback CharacterSubsetnames, stored
     *                          in the order of they are defined in sequence.fallback.
     * (16) table_appendedfontpath
     *                         :stringtID of the "appendedfontpath" defined.
     * (17) table_version   :stringID of the version number of this fontconfig file.
     */
    private static final int HEAD_LENGTH = 20;
    private static final int INDEX_scriptIDs = 0;
    private static final int INDEX_scriptFonts = 1;
    private static final int INDEX_elcIDs = 2;
    private static final int INDEX_sequences = 3;
    private static final int INDEX_fontfileNameIDs = 4;
    private static final int INDEX_componentFontNameIDs = 5;
    private static final int INDEX_filenames = 6;
    private static final int INDEX_awtfontpaths = 7;
    private static final int INDEX_exclusions = 8;
    private static final int INDEX_proportionals = 9;
    private static final int INDEX_scriptFontsMotif = 10;
    private static final int INDEX_alphabeticSuffix = 11;
    private static final int INDEX_stringIDs = 12;
    private static final int INDEX_stringTable = 13;
    private static final int INDEX_TABLEEND = 14;
    private static final int INDEX_fallbackScripts = 15;
    private static final int INDEX_appendedfontpath = 16;
    private static final int INDEX_version = 17;

    private static short[] head;
    private static short[] table_scriptIDs;
    private static short[] table_scriptFonts;
    private static short[] table_elcIDs;
    private static short[] table_sequences;
    private static short[] table_fontfileNameIDs;
    private static short[] table_componentFontNameIDs;
    private static short[] table_filenames;
    protected static short[] table_awtfontpaths;
    private static short[] table_exclusions;
    private static short[] table_proportionals;
    private static short[] table_scriptFontsMotif;
    private static short[] table_alphabeticSuffix;
    private static short[] table_stringIDs;
    private static char[]  table_stringTable;

    /**
     * Checks consistencies of complied fontconfig data. This method
     * is called only at the build-time from
     * build.tools.compilefontconfig.CompileFontConfig.
     */
    private static void sanityCheck() {
        int errors = 0;

        //This method will only be called during build time, do we
        //need do PrivilegedAction?
        @SuppressWarnings("removal")
        String osName = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("os.name");
            }
        });

        //componentFontNameID starts from "1"
        for (int ii = 1; ii < table_filenames.length; ii++) {
            if (table_filenames[ii] == -1) {
                // The corresponding finename entry for a component
                // font name is mandatory on Windows, but it's
                // optional on Solaris and Linux.
                if (osName.contains("Windows")) {
                    System.err.println("\n Error: <filename."
                                       + getString(table_componentFontNameIDs[ii])
                                       + "> entry is missing!!!");
                    errors++;
                } else {
                    if (verbose && !isEmpty(table_filenames)) {
                        System.err.println("\n Note: 'filename' entry is undefined for \""
                                           + getString(table_componentFontNameIDs[ii])
                                           + "\"");
                    }
                }
            }
        }
        for (int ii = 0; ii < table_scriptIDs.length; ii++) {
            short fid = table_scriptFonts[ii];
            if (fid == 0) {
                System.out.println("\n Error: <allfonts."
                                   + getString(table_scriptIDs[ii])
                                   + "> entry is missing!!!");
                errors++;
                continue;
            } else if (fid < 0) {
                fid = (short)-fid;
                for (int iii = 0; iii < NUM_FONTS; iii++) {
                    for (int iij = 0; iij < NUM_STYLES; iij++) {
                        int jj = iii * NUM_STYLES + iij;
                        short ffid = table_scriptFonts[fid + jj];
                        if (ffid == 0) {
                            System.err.println("\n Error: <"
                                           + getFontName(iii) + "."
                                           + getStyleName(iij) + "."
                                           + getString(table_scriptIDs[ii])
                                           + "> entry is missing!!!");
                            errors++;
                        }
                    }
                }
            }
        }
        if (errors != 0) {
            System.err.println("!!THERE ARE " + errors + " ERROR(S) IN "
                               + "THE FONTCONFIG FILE, PLEASE CHECK ITS CONTENT!!\n");
            System.exit(1);
        }
    }

    private static boolean isEmpty(short[] a) {
        for (short s : a) {
            if (s != -1) {
                return false;
            }
        }
        return true;
    }

    //dump the fontconfig data tables
    private static void dump() {
        System.out.println("\n----Head Table------------");
        for (int ii = 0; ii < HEAD_LENGTH; ii++) {
            System.out.println("  " + ii + " : " + head[ii]);
        }
        System.out.println("\n----scriptIDs-------------");
        printTable(table_scriptIDs, 0);
        System.out.println("\n----scriptFonts----------------");
        for (int ii = 0; ii < table_scriptIDs.length; ii++) {
            short fid = table_scriptFonts[ii];
            if (fid >= 0) {
                System.out.println("  allfonts."
                                   + getString(table_scriptIDs[ii])
                                   + "="
                                   + getString(table_componentFontNameIDs[fid]));
            }
        }
        for (int ii = 0; ii < table_scriptIDs.length; ii++) {
            short fid = table_scriptFonts[ii];
            if (fid < 0) {
                fid = (short)-fid;
                for (int iii = 0; iii < NUM_FONTS; iii++) {
                    for (int iij = 0; iij < NUM_STYLES; iij++) {
                        int jj = iii * NUM_STYLES + iij;
                        short ffid = table_scriptFonts[fid + jj];
                        System.out.println("  "
                                           + getFontName(iii) + "."
                                           + getStyleName(iij) + "."
                                           + getString(table_scriptIDs[ii])
                                           + "="
                                           + getString(table_componentFontNameIDs[ffid]));
                    }
                }

            }
        }
        System.out.println("\n----elcIDs----------------");
        printTable(table_elcIDs, 0);
        System.out.println("\n----sequences-------------");
        for (int ii = 0; ii< table_elcIDs.length; ii++) {
            System.out.println("  " + ii + "/" + getString(table_elcIDs[ii]));
            short[] ss = getShortArray(table_sequences[ii * NUM_FONTS + 0]);
            for (int jj = 0; jj < ss.length; jj++) {
                System.out.println("     " + getString(table_scriptIDs[ss[jj]]));
            }
        }
        System.out.println("\n----fontfileNameIDs-------");
        printTable(table_fontfileNameIDs, 0);

        System.out.println("\n----componentFontNameIDs--");
        printTable(table_componentFontNameIDs, 1);
        System.out.println("\n----filenames-------------");
        for (int ii = 0; ii < table_filenames.length; ii++) {
            if (table_filenames[ii] == -1) {
                System.out.println("  " + ii + " : null");
            } else {
                System.out.println("  " + ii + " : "
                   + getString(table_fontfileNameIDs[table_filenames[ii]]));
            }
        }
        System.out.println("\n----awtfontpaths---------");
        for (int ii = 0; ii < table_awtfontpaths.length; ii++) {
            System.out.println("  " + getString(table_scriptIDs[ii])
                               + " : "
                               + getString(table_awtfontpaths[ii]));
        }
        System.out.println("\n----proportionals--------");
        for (int ii = 0; ii < table_proportionals.length; ii++) {
            System.out.println("  "
                   + getString(table_componentFontNameIDs[table_proportionals[ii++]])
                   + " -> "
                   + getString(table_componentFontNameIDs[table_proportionals[ii]]));
        }
        int i = 0;
        System.out.println("\n----alphabeticSuffix----");
        while (i < table_alphabeticSuffix.length) {
          System.out.println("    " + getString(table_elcIDs[table_alphabeticSuffix[i++]])
                             + " -> " + getString(table_alphabeticSuffix[i++]));
        }
        System.out.println("\n----String Table---------");
        System.out.println("    stringID:    Num =" + table_stringIDs.length);
        System.out.println("    stringTable: Size=" + table_stringTable.length * 2);

        System.out.println("\n----fallbackScriptIDs---");
        short[] fbsIDs = getShortArray(head[INDEX_fallbackScripts]);
        for (int ii = 0; ii < fbsIDs.length; ii++) {
          System.out.println("  " + getString(table_scriptIDs[fbsIDs[ii]]));
        }
        System.out.println("\n----appendedfontpath-----");
        System.out.println("  " + getString(head[INDEX_appendedfontpath]));
        System.out.println("\n----Version--------------");
        System.out.println("  " + getString(head[INDEX_version]));
    }


    //////////////////////////////////////////////////////////////////////
    // Data table access methods                                        //
    //////////////////////////////////////////////////////////////////////

    /* Return the fontID of the platformFontName defined in this font config
     * by "LogicalFontName.StyleName.CharacterSubsetName" entry or
     * "allfonts.CharacterSubsetName" entry in properties format fc file.
     */
    protected static short getComponentFontID(short scriptID, int fontIndex, int styleIndex) {
        short fid = table_scriptFonts[scriptID];
        //System.out.println("fid=" + fid + "/ scriptID=" + scriptID + ", fi=" + fontIndex + ", si=" + styleIndex);
        if (fid >= 0) {
            //"allfonts"
            return fid;
        } else {
            return table_scriptFonts[-fid + fontIndex * NUM_STYLES + styleIndex];
        }
    }

    /* Same as getCompoentFontID() except this method returns the fontID define by
     * "xxxx.motif" entry.
     */
    protected static short getComponentFontIDMotif(short scriptID, int fontIndex, int styleIndex) {
        if (table_scriptFontsMotif.length == 0) {
            return 0;
        }
        short fid = table_scriptFontsMotif[scriptID];
        if (fid >= 0) {
            //"allfonts" > 0 or "not defined" == 0
            return fid;
        } else {
            return table_scriptFontsMotif[-fid + fontIndex * NUM_STYLES + styleIndex];
        }
    }

    private static int[] getExclusionRanges(short scriptID) {
        short exID = table_exclusions[scriptID];
        if (exID == 0) {
            return EMPTY_INT_ARRAY;
        } else {
            char[] exChar = getString(exID).toCharArray();
            int[] exInt = new int[exChar.length / 2];
            int i = 0;
            for (int j = 0; j < exInt.length; j++) {
                exInt[j] = (exChar[i++] << 16) + (exChar[i++] & 0xffff);
            }
            return exInt;
        }
    }

    private static boolean contains(short[] IDs, short id, int limit) {
        for (int i = 0; i < limit; i++) {
            if (IDs[i] == id) {
                return true;
            }
        }
        return false;
    }

    /* Return the PlatformFontName from its fontID*/
    protected static String getComponentFontName(short id) {
        if (id < 0) {
            return null;
        }
        return getString(table_componentFontNameIDs[id]);
    }

    private static String getComponentFileName(short id) {
        if (id < 0) {
            return null;
        }
        return getString(table_fontfileNameIDs[id]);
    }

    //componentFontID -> componentFileID
    private static short getComponentFileID(short nameID) {
        return table_filenames[nameID];
    }

    private static String getScriptName(short scriptID) {
        return getString(table_scriptIDs[scriptID]);
    }

   private HashMap<String, Short> reorderScripts;
   protected short[] getCoreScripts(int fontIndex) {
        short elc = getInitELC();
        /*
          System.out.println("getCoreScripts: elc=" + elc + ", fontIndex=" + fontIndex);
          short[] ss = getShortArray(table_sequences[elc * NUM_FONTS + fontIndex]);
          for (int i = 0; i < ss.length; i++) {
              System.out.println("     " + getString((short)table_scriptIDs[ss[i]]));
          }
          */
        short[] scripts = getShortArray(table_sequences[elc * NUM_FONTS + fontIndex]);
        if (preferLocaleFonts) {
            if (reorderScripts == null) {
                reorderScripts = new HashMap<String, Short>();
            }
            String[] ss = new String[scripts.length];
            for (int i = 0; i < ss.length; i++) {
                ss[i] = getScriptName(scripts[i]);
                reorderScripts.put(ss[i], scripts[i]);
            }
            reorderSequenceForLocale(ss);
            for (int i = 0; i < ss.length; i++) {
                scripts[i] = reorderScripts.get(ss[i]);
            }
        }
         return scripts;
    }

    private static short[] getFallbackScripts() {
        return getShortArray(head[INDEX_fallbackScripts]);
    }

    private static void printTable(short[] list, int start) {
        for (int i = start; i < list.length; i++) {
            System.out.println("  " + i + " : " + getString(list[i]));
        }
    }

    private static short[] readShortTable(DataInputStream in, int len )
        throws IOException {
        if (len == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        short[] data = new short[len];
        byte[] bb = new byte[len * 2];
        in.read(bb);
        int i = 0,j = 0;
        while (i < len) {
            data[i++] = (short)(bb[j++] << 8 | (bb[j++] & 0xff));
        }
        return data;
    }

    private static void writeShortTable(DataOutputStream out, short[] data)
        throws IOException {
        for (short val : data) {
            out.writeShort(val);
        }
    }

    private static short[] toList(HashMap<String, Short> map) {
        short[] list = new short[map.size()];
        Arrays.fill(list, (short) -1);
        for (Entry<String, Short> entry : map.entrySet()) {
            list[entry.getValue()] = getStringID(entry.getKey());
        }
        return list;
    }

    //runtime cache
    private static String[] stringCache;
    protected static String getString(short stringID) {
        if (stringID == 0)
            return null;
        /*
        if (loadingProperties) {
            return stringTable.substring(stringIDs[stringID],
                                         stringIDs[stringID+1]);
        }
        */
        //sync if we want it to be MT-enabled
        if (stringCache[stringID] == null){
            stringCache[stringID] =
              new String (table_stringTable,
                          table_stringIDs[stringID],
                          table_stringIDs[stringID+1] - table_stringIDs[stringID]);
        }
        return stringCache[stringID];
    }

    private static short[] getShortArray(short shortArrayID) {
        String s = getString(shortArrayID);
        char[] cc = s.toCharArray();
        short[] ss = new short[cc.length];
        for (int i = 0; i < cc.length; i++) {
            ss[i] = (short)(cc[i] & 0xffff);
        }
        return ss;
    }

    private static short getStringID(String s) {
        if (s == null) {
            return (short)0;
        }
        short pos0 = (short)stringTable.length();
        stringTable.append(s);
        short pos1 = (short)stringTable.length();

        stringIDs[stringIDNum] = pos0;
        stringIDs[stringIDNum + 1] = pos1;
        stringIDNum++;
        if (stringIDNum + 1 >= stringIDs.length) {
            short[] tmp = new short[stringIDNum + 1000];
            System.arraycopy(stringIDs, 0, tmp, 0, stringIDNum);
            stringIDs = tmp;
        }
        return (short)(stringIDNum - 1);
    }

    private static short getShortArrayID(short[] sa) {
        char[] cc = new char[sa.length];
        for (int i = 0; i < sa.length; i ++) {
            cc[i] = (char)sa[i];
        }
        String s = new String(cc);
        return getStringID(s);
    }

    //utility "empty" objects
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final short[] EMPTY_SHORT_ARRAY = new short[0];
    private static final String UNDEFINED_COMPONENT_FONT = "unknown";

    //////////////////////////////////////////////////////////////////////////
    //Convert the FontConfig data in Properties file to binary data tables  //
    //////////////////////////////////////////////////////////////////////////
    static class PropertiesHandler {
        public void load(InputStream in) throws IOException {
            initLogicalNameStyle();
            initHashMaps();
            FontProperties fp = new FontProperties();
            fp.load(in);
            initBinaryTable();
        }

        private void initBinaryTable() {
            //(0)
            head = new short[HEAD_LENGTH];
            head[INDEX_scriptIDs] = (short)HEAD_LENGTH;

            table_scriptIDs = toList(scriptIDs);
            //(1)a: scriptAllfonts scriptID/allfonts -> componentFontNameID
            //   b: scriptFonts    scriptID -> componentFontNameID[20]
            //if we have a "allfonts.script" def, then we just put
            //the "-platformFontID" value in the slot, otherwise the slot
            //value is "offset" which "offset" is where 20 entries located
            //in the table attached.
            head[INDEX_scriptFonts] = (short)(head[INDEX_scriptIDs]  + table_scriptIDs.length);
            int len = table_scriptIDs.length + scriptFonts.size() * 20;
            table_scriptFonts = new short[len];

            for (Entry<Short, Short> entry : scriptAllfonts.entrySet()) {
                table_scriptFonts[entry.getKey().intValue()] = entry.getValue();
            }
            int off = table_scriptIDs.length;
            for (Entry<Short, Short[]> entry : scriptFonts.entrySet()) {
                table_scriptFonts[entry.getKey().intValue()] = (short)-off;
                Short[] v = entry.getValue();
                for (int i = 0; i < 20; i++) {
                    if (v[i] != null) {
                        table_scriptFonts[off++] = v[i];
                    } else {
                        table_scriptFonts[off++] = 0;
                    }
                }
            }

            //(2)
            head[INDEX_elcIDs] = (short)(head[INDEX_scriptFonts]  + table_scriptFonts.length);
            table_elcIDs = toList(elcIDs);

            //(3) sequences  elcID -> XXXX[1|5] -> scriptID[]
            head[INDEX_sequences] = (short)(head[INDEX_elcIDs]  + table_elcIDs.length);
            table_sequences = new short[elcIDs.size() * NUM_FONTS];
            for (Entry<Short, short[]> entry : sequences.entrySet()) {
                //table_sequences[entry.getKey().intValue()] = (short)-off;
                int k = entry.getKey().intValue();
                short[] v = entry.getValue();
                /*
                  System.out.println("elc=" + k + "/" + getString((short)table_elcIDs[k]));
                  short[] ss = getShortArray(v[0]);
                  for (int i = 0; i < ss.length; i++) {
                  System.out.println("     " + getString((short)table_scriptIDs[ss[i]]));
                  }
                  */
                if (v.length == 1) {
                    //the "allfonts" entries
                    for (int i = 0; i < NUM_FONTS; i++) {
                        table_sequences[k * NUM_FONTS + i] = v[0];
                    }
                } else {
                    for (int i = 0; i < NUM_FONTS; i++) {
                        table_sequences[k * NUM_FONTS + i] = v[i];
                    }
                }
            }
            //(4)
            head[INDEX_fontfileNameIDs] = (short)(head[INDEX_sequences]  + table_sequences.length);
            table_fontfileNameIDs = toList(fontfileNameIDs);

            //(5)
            head[INDEX_componentFontNameIDs] = (short)(head[INDEX_fontfileNameIDs]  + table_fontfileNameIDs.length);
            table_componentFontNameIDs = toList(componentFontNameIDs);

            //(6)componentFontNameID -> filenameID
            head[INDEX_filenames] = (short)(head[INDEX_componentFontNameIDs]  + table_componentFontNameIDs.length);
            table_filenames = new short[table_componentFontNameIDs.length];
            Arrays.fill(table_filenames, (short) -1);

            for (Entry<Short, Short> entry : filenames.entrySet()) {
                table_filenames[entry.getKey()] = entry.getValue();
            }

            //(7)scriptID-> awtfontpath
            //the paths are stored as scriptID -> stringID in awtfontpahts
            head[INDEX_awtfontpaths] = (short)(head[INDEX_filenames]  + table_filenames.length);
            table_awtfontpaths = new short[table_scriptIDs.length];
            for (Entry<Short, Short> entry : awtfontpaths.entrySet()) {
                table_awtfontpaths[entry.getKey()] = entry.getValue();
            }

            //(8)exclusions
            head[INDEX_exclusions] = (short)(head[INDEX_awtfontpaths]  + table_awtfontpaths.length);
            table_exclusions = new short[scriptIDs.size()];
            for (Entry<Short, int[]> entry : exclusions.entrySet()) {
                int[] exI = entry.getValue();
                char[] exC = new char[exI.length * 2];
                int j = 0;
                for (int i = 0; i < exI.length; i++) {
                    exC[j++] = (char) (exI[i] >> 16);
                    exC[j++] = (char) (exI[i] & 0xffff);
                }
                table_exclusions[entry.getKey()] = getStringID(new String (exC));
            }
            //(9)proportionals
            head[INDEX_proportionals] = (short)(head[INDEX_exclusions]  + table_exclusions.length);
            table_proportionals = new short[proportionals.size() * 2];
            int j = 0;
            for (Entry<Short, Short> entry : proportionals.entrySet()) {
                table_proportionals[j++] = entry.getKey();
                table_proportionals[j++] = entry.getValue();
            }

            //(10) see (1) for info, the only difference is "xxx.motif"
            head[INDEX_scriptFontsMotif] = (short)(head[INDEX_proportionals] + table_proportionals.length);
            if (scriptAllfontsMotif.size() != 0 || scriptFontsMotif.size() != 0) {
                len = table_scriptIDs.length + scriptFontsMotif.size() * 20;
                table_scriptFontsMotif = new short[len];

                for (Entry<Short, Short> entry : scriptAllfontsMotif.entrySet()) {
                    table_scriptFontsMotif[entry.getKey().intValue()] =
                      (short)entry.getValue();
                }
                off = table_scriptIDs.length;
                for (Entry<Short, Short[]> entry : scriptFontsMotif.entrySet()) {
                    table_scriptFontsMotif[entry.getKey().intValue()] = (short)-off;
                    Short[] v = entry.getValue();
                    int i = 0;
                    while (i < 20) {
                        if (v[i] != null) {
                            table_scriptFontsMotif[off++] = v[i];
                        } else {
                            table_scriptFontsMotif[off++] = 0;
                        }
                        i++;
                    }
                }
            } else {
                table_scriptFontsMotif = EMPTY_SHORT_ARRAY;
            }

            //(11)short[] alphabeticSuffix
            head[INDEX_alphabeticSuffix] = (short)(head[INDEX_scriptFontsMotif] + table_scriptFontsMotif.length);
            table_alphabeticSuffix = new short[alphabeticSuffix.size() * 2];
            j = 0;
            for (Entry<Short, Short> entry : alphabeticSuffix.entrySet()) {
                table_alphabeticSuffix[j++] = entry.getKey();
                table_alphabeticSuffix[j++] = entry.getValue();
            }

            //(15)short[] fallbackScriptIDs; just put the ID in head
            head[INDEX_fallbackScripts] = getShortArrayID(fallbackScriptIDs);

            //(16)appendedfontpath
            head[INDEX_appendedfontpath] = getStringID(appendedfontpath);

            //(17)version
            head[INDEX_version] = getStringID(version);

            //(12)short[] StringIDs
            head[INDEX_stringIDs] = (short)(head[INDEX_alphabeticSuffix] + table_alphabeticSuffix.length);
            table_stringIDs = new short[stringIDNum + 1];
            System.arraycopy(stringIDs, 0, table_stringIDs, 0, stringIDNum + 1);

            //(13)StringTable
            head[INDEX_stringTable] = (short)(head[INDEX_stringIDs] + stringIDNum + 1);
            table_stringTable = stringTable.toString().toCharArray();
            //(14)
            head[INDEX_TABLEEND] = (short)(head[INDEX_stringTable] + stringTable.length());

            //StringTable cache
            stringCache = new String[table_stringIDs.length];
        }

        //////////////////////////////////////////////
        private HashMap<String, Short> scriptIDs;
        //elc -> Encoding.Language.Country
        private HashMap<String, Short> elcIDs;
        //componentFontNameID starts from "1", "0" reserves for "undefined"
        private HashMap<String, Short> componentFontNameIDs;
        private HashMap<String, Short> fontfileNameIDs;
        private HashMap<String, Integer> logicalFontIDs;
        private HashMap<String, Integer> fontStyleIDs;

        //componentFontNameID -> fontfileNameID
        private HashMap<Short, Short>  filenames;

        //elcID -> allfonts/logicalFont -> scriptID list
        //(1)if we have a "allfonts", then the length of the
        //   value array is "1", otherwise it's 5, each font
        //   must have their own individual entry.
        //scriptID list "short[]" is stored as an ID
        private HashMap<Short, short[]> sequences;

        //scriptID ->logicFontID/fontStyleID->componentFontNameID,
        //a 20-entry array (5-name x 4-style) for each script
        private HashMap<Short, Short[]> scriptFonts;

        //scriptID -> componentFontNameID
        private HashMap<Short, Short> scriptAllfonts;

        //scriptID -> exclusionRanges[]
        private HashMap<Short, int[]> exclusions;

        //scriptID -> fontpath
        private HashMap<Short, Short> awtfontpaths;

        //fontID -> fontID
        private HashMap<Short, Short> proportionals;

        //scriptID -> componentFontNameID
        private HashMap<Short, Short> scriptAllfontsMotif;

        //scriptID ->logicFontID/fontStyleID->componentFontNameID,
        private HashMap<Short, Short[]> scriptFontsMotif;

        //elcID -> stringID of alphabetic/XXXX
        private HashMap<Short, Short> alphabeticSuffix;

        private short[] fallbackScriptIDs;
        private String version;
        private String appendedfontpath;

        private void initLogicalNameStyle() {
            logicalFontIDs = new HashMap<String, Integer>();
            fontStyleIDs = new HashMap<String, Integer>();
            logicalFontIDs.put("serif",      0);
            logicalFontIDs.put("sansserif",  1);
            logicalFontIDs.put("monospaced", 2);
            logicalFontIDs.put("dialog",     3);
            logicalFontIDs.put("dialoginput",4);
            fontStyleIDs.put("plain",      0);
            fontStyleIDs.put("bold",       1);
            fontStyleIDs.put("italic",     2);
            fontStyleIDs.put("bolditalic", 3);
        }

        private void initHashMaps() {
            scriptIDs = new HashMap<String, Short>();
            elcIDs = new HashMap<String, Short>();
            componentFontNameIDs = new HashMap<String, Short>();
            /*Init these tables to allow componentFontNameID, fontfileNameIDs
              to start from "1".
            */
            componentFontNameIDs.put("", Short.valueOf((short)0));

            fontfileNameIDs = new HashMap<String, Short>();
            filenames = new HashMap<Short, Short>();
            sequences = new HashMap<Short, short[]>();
            scriptFonts = new HashMap<Short, Short[]>();
            scriptAllfonts = new HashMap<Short, Short>();
            exclusions = new HashMap<Short, int[]>();
            awtfontpaths = new HashMap<Short, Short>();
            proportionals = new HashMap<Short, Short>();
            scriptFontsMotif = new HashMap<Short, Short[]>();
            scriptAllfontsMotif = new HashMap<Short, Short>();
            alphabeticSuffix = new HashMap<Short, Short>();
            fallbackScriptIDs = EMPTY_SHORT_ARRAY;
            /*
              version
              appendedfontpath
            */
        }

        private int[] parseExclusions(String key, String exclusions) {
            if (exclusions == null) {
                return EMPTY_INT_ARRAY;
            }
            // range format is xxxx-XXXX,yyyyyy-YYYYYY,.....
            int numExclusions = 1;
            int pos = 0;
            while ((pos = exclusions.indexOf(',', pos)) != -1) {
                numExclusions++;
                pos++;
            }
            int[] exclusionRanges = new int[numExclusions * 2];
            pos = 0;
            int newPos = 0;
            for (int j = 0; j < numExclusions * 2; ) {
                String lower, upper;
                int lo = 0, up = 0;
                try {
                    newPos = exclusions.indexOf('-', pos);
                    lower = exclusions.substring(pos, newPos);
                    pos = newPos + 1;
                    newPos = exclusions.indexOf(',', pos);
                    if (newPos == -1) {
                        newPos = exclusions.length();
                    }
                    upper = exclusions.substring(pos, newPos);
                    pos = newPos + 1;
                    int lowerLength = lower.length();
                    int upperLength = upper.length();
                    if (lowerLength != 4 && lowerLength != 6
                        || upperLength != 4 && upperLength != 6) {
                        throw new Exception();
                    }
                    lo = Integer.parseInt(lower, 16);
                    up = Integer.parseInt(upper, 16);
                    if (lo > up) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    if (FontUtilities.debugFonts() &&
                        logger != null) {
                        logger.config("Failed parsing " + key +
                                  " property of font configuration.");

                    }
                    return EMPTY_INT_ARRAY;
                }
                exclusionRanges[j++] = lo;
                exclusionRanges[j++] = up;
            }
            return exclusionRanges;
        }

        private Short getID(HashMap<String, Short> map, String key) {
            Short ret = map.get(key);
            if ( ret == null) {
                map.put(key, (short)map.size());
                return map.get(key);
            }
            return ret;
        }

        @SuppressWarnings("serial") // JDK-implementation class
        class FontProperties extends Properties {
            public synchronized Object put(Object k, Object v) {
                parseProperty((String)k, (String)v);
                return null;
            }
        }

        private void parseProperty(String key, String value) {
            if (key.startsWith("filename.")) {
                //the only special case is "MingLiu_HKSCS" which has "_" in its
                //facename, we don't want to replace the "_" with " "
                key = key.substring(9);
                if (!"MingLiU_HKSCS".equals(key)) {
                    key = key.replace('_', ' ');
                }
                Short faceID = getID(componentFontNameIDs, key);
                Short fileID = getID(fontfileNameIDs, value);
                //System.out.println("faceID=" + faceID + "/" + key + " -> "
                //    + "fileID=" + fileID + "/" + value);
                filenames.put(faceID, fileID);
            } else if (key.startsWith("exclusion.")) {
                key = key.substring(10);
                exclusions.put(getID(scriptIDs,key), parseExclusions(key,value));
            } else if (key.startsWith("sequence.")) {
                key = key.substring(9);
                boolean hasDefault = false;
                boolean has1252 = false;

                //get the scriptID list
                String[] ss = splitSequence(value).toArray(EMPTY_STRING_ARRAY);
                short [] sa = new short[ss.length];
                for (int i = 0; i < ss.length; i++) {
                    if ("alphabetic/default".equals(ss[i])) {
                        //System.out.println(key + " -> " + ss[i]);
                        ss[i] = "alphabetic";
                        hasDefault = true;
                    } else if ("alphabetic/1252".equals(ss[i])) {
                        //System.out.println(key + " -> " + ss[i]);
                        ss[i] = "alphabetic";
                        has1252 = true;
                    }
                    sa[i] = getID(scriptIDs, ss[i]).shortValue();
                    //System.out.println("scriptID=" + si[i] + "/" + ss[i]);
                }
                //convert the "short[] -> string -> stringID"
                short scriptArrayID = getShortArrayID(sa);
                Short elcID = null;
                int dot = key.indexOf('.');
                if (dot == -1) {
                    if ("fallback".equals(key)) {
                        fallbackScriptIDs = sa;
                        return;
                    }
                    if ("allfonts".equals(key)) {
                        elcID = getID(elcIDs, "NULL.NULL.NULL");
                    } else {
                        if (logger != null) {
                            logger.config("Error sequence def: <sequence." + key + ">");
                        }
                        return;
                    }
                } else {
                    elcID = getID(elcIDs, key.substring(dot + 1));
                    //System.out.println("elcID=" + elcID + "/" + key.substring(dot + 1));
                    key = key.substring(0, dot);
                }
                short[] scriptArrayIDs = null;
                if ("allfonts".equals(key)) {
                    scriptArrayIDs = new short[1];
                    scriptArrayIDs[0] = scriptArrayID;
                } else {
                    scriptArrayIDs = sequences.get(elcID);
                    if (scriptArrayIDs == null) {
                       scriptArrayIDs = new short[5];
                    }
                    Integer fid = logicalFontIDs.get(key);
                    if (fid == null) {
                        if (logger != null) {
                            logger.config("Unrecognizable logicfont name " + key);
                        }
                        return;
                    }
                    //System.out.println("sequence." + key + "/" + id);
                    scriptArrayIDs[fid.intValue()] = scriptArrayID;
                }
                sequences.put(elcID, scriptArrayIDs);
                if (hasDefault) {
                    alphabeticSuffix.put(elcID, getStringID("default"));
                } else
                if (has1252) {
                    alphabeticSuffix.put(elcID, getStringID("1252"));
                }
            } else if (key.startsWith("allfonts.")) {
                key = key.substring(9);
                if (key.endsWith(".motif")) {
                    key = key.substring(0, key.length() - 6);
                    //System.out.println("motif: all." + key + "=" + value);
                    scriptAllfontsMotif.put(getID(scriptIDs,key), getID(componentFontNameIDs,value));
                } else {
                    scriptAllfonts.put(getID(scriptIDs,key), getID(componentFontNameIDs,value));
                }
            } else if (key.startsWith("awtfontpath.")) {
                key = key.substring(12);
                //System.out.println("scriptID=" + getID(scriptIDs, key) + "/" + key);
                awtfontpaths.put(getID(scriptIDs, key), getStringID(value));
            } else if ("version".equals(key)) {
                version = value;
            } else if ("appendedfontpath".equals(key)) {
                appendedfontpath = value;
            } else if (key.startsWith("proportional.")) {
                key = key.substring(13).replace('_', ' ');
                //System.out.println(key + "=" + value);
                proportionals.put(getID(componentFontNameIDs, key),
                                  getID(componentFontNameIDs, value));
            } else {
                //"name.style.script(.motif)", we don't care anything else
                int dot1, dot2;
                boolean isMotif = false;

                dot1 = key.indexOf('.');
                if (dot1 == -1) {
                    if (logger != null) {
                        logger.config("Failed parsing " + key +
                                  " property of font configuration.");

                    }
                    return;
                }
                dot2 = key.indexOf('.', dot1 + 1);
                if (dot2 == -1) {
                    if (logger != null) {
                        logger.config("Failed parsing " + key +
                                  " property of font configuration.");

                    }
                    return;
                }
                if (key.endsWith(".motif")) {
                    key = key.substring(0, key.length() - 6);
                    isMotif = true;
                    //System.out.println("motif: " + key + "=" + value);
                }
                Integer nameID = logicalFontIDs.get(key.substring(0, dot1));
                Integer styleID = fontStyleIDs.get(key.substring(dot1+1, dot2));
                Short scriptID = getID(scriptIDs, key.substring(dot2 + 1));
                if (nameID == null || styleID == null) {
                    if (logger != null) {
                        logger.config("unrecognizable logicfont name/style at " + key);
                    }
                    return;
                }
                Short[] pnids;
                if (isMotif) {
                    pnids = scriptFontsMotif.get(scriptID);
                } else {
                    pnids = scriptFonts.get(scriptID);
                }
                if (pnids == null) {
                    pnids =  new Short[20];
                }
                pnids[nameID.intValue() * NUM_STYLES + styleID.intValue()]
                  = getID(componentFontNameIDs, value);
                /*
                System.out.println("key=" + key + "/<" + nameID + "><" + styleID
                                     + "><" + scriptID + ">=" + value
                                     + "/" + getID(componentFontNameIDs, value));
                */
                if (isMotif) {
                    scriptFontsMotif.put(scriptID, pnids);
                } else {
                    scriptFonts.put(scriptID, pnids);
                }
            }
        }
    }
}
