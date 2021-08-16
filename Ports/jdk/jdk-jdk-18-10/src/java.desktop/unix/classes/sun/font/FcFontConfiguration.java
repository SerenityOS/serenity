/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

import sun.awt.FcFontManager;
import sun.awt.FontConfiguration;
import sun.awt.FontDescriptor;
import sun.awt.SunToolkit;
import sun.font.FontConfigManager.FcCompFont;
import sun.font.FontConfigManager.FontConfigFont;
import sun.font.FontConfigManager.FontConfigInfo;
import sun.util.logging.PlatformLogger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class FcFontConfiguration extends FontConfiguration {

    /** Version of the cache file format understood by this code.
     * Its part of the file name so that we can rev this at
     * any time, even in a minor JDK update.
     * It is stored as the value of the "version" property.
     * This is distinct from the version of "libfontconfig" that generated
     * the cached results, and which is the "fcversion" property in the file.
     * {@code FontConfiguration.getVersion()} also returns a version string,
     * and has meant the version of the fontconfiguration.properties file
     * that was read. Since this class doesn't use such files, then what
     * that really means is whether the methods on this class return
     * values that are compatible with the classes that do directly read
     * from such files. It is a compatible subset of version "1".
     */
    private static final String fileVersion = "1";
    private String fcInfoFileName = null;

    private FcCompFont[] fcCompFonts = null;

    public FcFontConfiguration(SunFontManager fm) {
        super(fm);
        init();
    }

    /* This isn't called but is needed to satisfy super-class contract. */
    public FcFontConfiguration(SunFontManager fm,
                               boolean preferLocaleFonts,
                               boolean preferPropFonts) {
        super(fm, preferLocaleFonts, preferPropFonts);
        init();
    }

    @Override
    public synchronized boolean init() {
        if (fcCompFonts != null) {
            return true;
        }

        setFontConfiguration();
        readFcInfo();
        FcFontManager fm = (FcFontManager) fontManager;
        FontConfigManager fcm = fm.getFontConfigManager();
        if (fcCompFonts == null) {
            fcCompFonts = fcm.loadFontConfig();
            if (fcCompFonts != null) {
                try {
                    writeFcInfo();
                } catch (Exception e) {
                    if (FontUtilities.debugFonts()) {
                        warning("Exception writing fcInfo " + e);
                    }
                }
            } else if (FontUtilities.debugFonts()) {
                warning("Failed to get info from libfontconfig");
            }
        } else {
            fcm.populateFontConfig(fcCompFonts);
        }

        if (fcCompFonts == null) {
            return false; // couldn't load fontconfig.
        }

        // NB already in a privileged block from SGE
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new Error("java.home property not set");
        }
        String javaLib = javaHome + File.separator + "lib";
        getInstalledFallbackFonts(javaLib);

        return true;
    }

    @Override
    public String getFallbackFamilyName(String fontName,
                                        String defaultFallback) {
        // maintain compatibility with old font.properties files, which either
        // had aliases for TimesRoman & Co. or defined mappings for them.
        String compatibilityName = getCompatibilityFamilyName(fontName);
        if (compatibilityName != null) {
            return compatibilityName;
        }
        return defaultFallback;
    }

    @Override
    protected String
        getFaceNameFromComponentFontName(String componentFontName) {
        return null;
    }

    @Override
    protected String
        getFileNameFromComponentFontName(String componentFontName) {
        return null;
    }

    @Override
    public String getFileNameFromPlatformName(String platformName) {
        /* Platform name is the file name, but rather than returning
         * the arg, return null*/
        return null;
    }

    @Override
    protected Charset getDefaultFontCharset(String fontName) {
        return Charset.forName("ISO8859_1");
    }

    @Override
    protected String getEncoding(String awtFontName,
                                 String characterSubsetName) {
        return "default";
    }

    @Override
    protected void initReorderMap() {
        reorderMap = new HashMap<>();
    }

    @Override
    protected FontDescriptor[] buildFontDescriptors(int fontIndex, int styleIndex) {
        CompositeFontDescriptor[] cfi = get2DCompositeFontInfo();
        int idx = fontIndex * NUM_STYLES + styleIndex;
        String[] componentFaceNames = cfi[idx].getComponentFaceNames();
        FontDescriptor[] ret = new FontDescriptor[componentFaceNames.length];
        for (int i = 0; i < componentFaceNames.length; i++) {
            ret[i] = new FontDescriptor(componentFaceNames[i], ISO_8859_1.newEncoder(), new int[0]);
        }

        return ret;
    }

    @Override
    public int getNumberCoreFonts() {
        return 1;
    }

    @Override
    public String[] getPlatformFontNames() {
        HashSet<String> nameSet = new HashSet<String>();
        FcFontManager fm = (FcFontManager) fontManager;
        FontConfigManager fcm = fm.getFontConfigManager();
        FcCompFont[] fcCompFonts = fcm.loadFontConfig();
        for (int i=0; i<fcCompFonts.length; i++) {
            for (int j=0; j<fcCompFonts[i].allFonts.length; j++) {
                nameSet.add(fcCompFonts[i].allFonts[j].fontFile);
            }
        }
        return nameSet.toArray(new String[0]);
    }

    @Override
    public String getExtraFontPath() {
        return null;
    }

    @Override
    public boolean needToSearchForFile(String fileName) {
        return false;
    }

    private FontConfigFont[] getFcFontList(FcCompFont[] fcFonts,
                                           String fontname, int style) {

        if (fontname.equals("dialog")) {
            fontname = "sansserif";
        } else if (fontname.equals("dialoginput")) {
            fontname = "monospaced";
        }
        for (int i=0; i<fcFonts.length; i++) {
            if (fontname.equals(fcFonts[i].jdkName) &&
                style == fcFonts[i].style) {
                return fcFonts[i].allFonts;
            }
        }
        return fcFonts[0].allFonts;
    }

    @Override
    public CompositeFontDescriptor[] get2DCompositeFontInfo() {

        FcFontManager fm = (FcFontManager) fontManager;
        FontConfigManager fcm = fm.getFontConfigManager();
        FcCompFont[] fcCompFonts = fcm.loadFontConfig();

        CompositeFontDescriptor[] result =
                new CompositeFontDescriptor[NUM_FONTS * NUM_STYLES];

        for (int fontIndex = 0; fontIndex < NUM_FONTS; fontIndex++) {
            String fontName = publicFontNames[fontIndex];

            for (int styleIndex = 0; styleIndex < NUM_STYLES; styleIndex++) {

                String faceName = fontName + "." + styleNames[styleIndex];
                FontConfigFont[] fcFonts =
                    getFcFontList(fcCompFonts,
                                  fontNames[fontIndex], styleIndex);

                int numFonts = fcFonts.length;
                // fall back fonts listed in the lib/fonts/fallback directory
                if (installedFallbackFontFiles != null) {
                    numFonts += installedFallbackFontFiles.length;
                }

                String[] fileNames = new String[numFonts];
                String[] faceNames = new String[numFonts];

                int index;
                for (index = 0; index < fcFonts.length; index++) {
                    fileNames[index] = fcFonts[index].fontFile;
                    faceNames[index] = fcFonts[index].fullName;
                }

                if (installedFallbackFontFiles != null) {
                    System.arraycopy(installedFallbackFontFiles, 0,
                                     fileNames, fcFonts.length,
                                     installedFallbackFontFiles.length);
                }

                result[fontIndex * NUM_STYLES + styleIndex]
                        = new CompositeFontDescriptor(
                            faceName,
                            1,
                            faceNames,
                            fileNames,
                            null, null);
            }
        }
        return result;
    }

    /**
     * Gets the OS version string from a Linux release-specific file.
     */
    private String getVersionString(File f) {
        try (Scanner sc  = new Scanner(f)) {
            return sc.findInLine("(\\d)+((\\.)(\\d)+)*");
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Sets the OS name and version from environment information.
     */
    @Override
    protected void setOsNameAndVersion() {

        super.setOsNameAndVersion();

        if (!osName.equals("Linux")) {
            return;
        }
        try {
            File f;
            if ((f = new File("/etc/lsb-release")).canRead()) {
                    /* Ubuntu and (perhaps others) use only lsb-release.
                     * Syntax and encoding is compatible with java properties.
                     * For Ubuntu the ID is "Ubuntu".
                     */
                    Properties props = new Properties();
                    props.load(new FileInputStream(f));
                    osName = props.getProperty("DISTRIB_ID");
                    osVersion =  props.getProperty("DISTRIB_RELEASE");
            } else if ((f = new File("/etc/redhat-release")).canRead()) {
                osName = "RedHat";
                osVersion = getVersionString(f);
            } else if ((f = new File("/etc/SuSE-release")).canRead()) {
                osName = "SuSE";
                osVersion = getVersionString(f);
            } else if ((f = new File("/etc/turbolinux-release")).canRead()) {
                osName = "Turbo";
                osVersion = getVersionString(f);
            } else if ((f = new File("/etc/fedora-release")).canRead()) {
                osName = "Fedora";
                osVersion = getVersionString(f);
            }
        } catch (Exception e) {
            if (FontUtilities.debugFonts()) {
                warning("Exception identifying Linux distro.");
            }
        }
    }

    private File getFcInfoFile() {
        if (fcInfoFileName == null) {
            // NB need security permissions to get true IP address, and
            // we should have those as the whole initialisation is in a
            // doPrivileged block. But in this case no exception is thrown,
            // and it returns the loop back address, and so we end up with
            // "localhost"
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostname = "localhost";
            }
            String userDir = System.getProperty("user.home");
            String version = System.getProperty("java.version");
            String fs = File.separator;
            String dir = userDir+fs+".java"+fs+"fonts"+fs+version;
            Locale locale = SunToolkit.getStartupLocale();
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            String name = "fcinfo-"+fileVersion+"-"+hostname+"-"+
                osName+"-"+osVersion+"-"+lang+"-"+country+".properties";
            fcInfoFileName = dir+fs+name;
        }
        return new File(fcInfoFileName);
    }

    private void writeFcInfo() {
        Properties props = new Properties();
        props.setProperty("version", fileVersion);
        FcFontManager fm = (FcFontManager) fontManager;
        FontConfigManager fcm = fm.getFontConfigManager();
        FontConfigInfo fcInfo = fcm.getFontConfigInfo();
        props.setProperty("fcversion", Integer.toString(fcInfo.fcVersion));
        if (fcInfo.cacheDirs != null) {
            for (int i=0;i<fcInfo.cacheDirs.length;i++) {
                if (fcInfo.cacheDirs[i] != null) {
                   props.setProperty("cachedir."+i,  fcInfo.cacheDirs[i]);
                }
            }
        }
        for (int i=0; i<fcCompFonts.length; i++) {
            FcCompFont fci = fcCompFonts[i];
            String styleKey = fci.jdkName+"."+fci.style;
            props.setProperty(styleKey+".length",
                              Integer.toString(fci.allFonts.length));
            for (int j=0; j<fci.allFonts.length; j++) {
                props.setProperty(styleKey+"."+j+".file",
                                  fci.allFonts[j].fontFile);
                if (fci.allFonts[j].fullName != null) {
                    props.setProperty(styleKey+"."+j+".fullName",
                                      fci.allFonts[j].fullName);
                }
            }
        }
        try {
            /* This writes into a temp file then renames when done.
             * Since the rename is an atomic action within the same
             * directory no client will ever see a partially written file.
             */
            File fcInfoFile = getFcInfoFile();
            File dir = fcInfoFile.getParentFile();
            dir.mkdirs();
            File tempFile = Files.createTempFile(dir.toPath(), "fcinfo", null).toFile();
            FileOutputStream fos = new FileOutputStream(tempFile);
            props.store(fos,
                      "JDK Font Configuration Generated File: *Do Not Edit*");
            fos.close();
            boolean renamed = tempFile.renameTo(fcInfoFile);
            if (!renamed && FontUtilities.debugFonts()) {
                System.out.println("rename failed");
                warning("Failed renaming file to "+ getFcInfoFile());
            }
        } catch (Exception e) {
            if (FontUtilities.debugFonts()) {
                warning("IOException writing to "+ getFcInfoFile());
            }
        }
    }

    /* We want to be able to use this cache instead of invoking
     * fontconfig except when we can detect the system cache has changed.
     * But there doesn't seem to be a way to find the location of
     * the system cache.
     */
    private void readFcInfo() {
        File fcFile = getFcInfoFile();
        if (!fcFile.exists()) {
            if (FontUtilities.debugFonts()) {
                warning("fontconfig info file " + fcFile.toString() + " does not exist");
            }
            return;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(fcFile)) {
            props.load(fis);
        } catch (IOException e) {
            if (FontUtilities.debugFonts()) {
                warning("IOException (" + e.getCause() + ") reading from " + fcFile.toString());
            }
            return;
        }
        String version = (String)props.get("version");
        if (version == null || !version.equals(fileVersion)) {
            if (FontUtilities.debugFonts()) {
                warning("fontconfig info file version mismatch (found: " + version +
                    ", expected: " + fileVersion + ")");
            }
            return;
        }

        // If there's a new, different fontconfig installed on the
        // system, we invalidate our fontconfig file.
        String fcVersionStr = (String)props.get("fcversion");
        if (fcVersionStr != null) {
            int fcVersion;
            try {
                fcVersion = Integer.parseInt(fcVersionStr);
                if (fcVersion != 0 &&
                    fcVersion != FontConfigManager.getFontConfigVersion()) {
                    if (FontUtilities.debugFonts()) {
                        warning("new, different fontconfig detected");
                    }
                    return;
                }
            } catch (Exception e) {
                if (FontUtilities.debugFonts()) {
                    warning("Exception parsing version " + fcVersionStr);
                }
                return;
            }
        }

        // If we can locate the fontconfig cache dirs, then compare the
        // time stamp of those with our properties file. If we are out
        // of date then re-generate.
        long lastModified = fcFile.lastModified();
        int cacheDirIndex = 0;
        while (cacheDirIndex<4) { // should never be more than 2 anyway.
            String dir = (String)props.get("cachedir."+cacheDirIndex);
            if (dir == null) {
                break;
            }
            File dirFile = new File(dir);
            if (dirFile.exists() && dirFile.lastModified() > lastModified) {
                if (FontUtilities.debugFonts()) {
                    warning("out of date cache directories detected");
                }
                return;
            }
            cacheDirIndex++;
        }

        String[] names = { "sansserif", "serif", "monospaced" };
        String[] fcnames = { "sans", "serif", "monospace" };
        int namesLen = names.length;
        int numStyles = 4;
        FcCompFont[] fci = new FcCompFont[namesLen*numStyles];

        try {
            for (int i=0; i<namesLen; i++) {
                for (int s=0; s<numStyles; s++) {
                    int index = i*numStyles+s;
                    fci[index] = new FcCompFont();
                    String key = names[i]+"."+s;
                    fci[index].jdkName = names[i];
                    fci[index].fcFamily = fcnames[i];
                    fci[index].style = s;
                    String lenStr = (String)props.get(key+".length");
                    int nfonts = Integer.parseInt(lenStr);
                    if (nfonts <= 0) {
                        if (FontUtilities.debugFonts()) {
                            warning("bad non-positive .length entry in fontconfig file " + fcFile.toString());
                        }
                        return; // bad file
                    }
                    fci[index].allFonts = new FontConfigFont[nfonts];
                    for (int f=0; f<nfonts; f++) {
                        fci[index].allFonts[f] = new FontConfigFont();
                        String fkey = key+"."+f+".fullName";
                        String fullName = (String)props.get(fkey);
                        fci[index].allFonts[f].fullName = fullName;
                        fkey = key+"."+f+".file";
                        String file = (String)props.get(fkey);
                        if (file == null) {
                            if (FontUtilities.debugFonts()) {
                                warning("missing file value for key " + fkey + " in fontconfig file " + fcFile.toString());
                            }
                            return; // bad file
                        }
                        fci[index].allFonts[f].fontFile = file;
                    }
                    fci[index].firstFont =  fci[index].allFonts[0];

                }
            }
            fcCompFonts = fci;
        } catch (Throwable t) {
            if (FontUtilities.debugFonts()) {
                warning(t.toString());
            }
        }

        if (FontUtilities.debugFonts()) {
            FontUtilities.logInfo("successfully parsed the fontconfig file at " + fcFile.toString());
        }
    }

    private static void warning(String msg) {
        PlatformLogger logger = PlatformLogger.getLogger("sun.awt.FontConfiguration");
        logger.warning(msg);
    }
}
