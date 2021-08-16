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

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.plaf.FontUIResource;

import sun.awt.FontConfiguration;
import sun.awt.SunToolkit;
import sun.awt.util.ThreadGroupUtils;
import sun.java2d.FontSupport;
import sun.util.logging.PlatformLogger;

/**
 * The base implementation of the {@link FontManager} interface. It implements
 * the platform independent, shared parts of OpenJDK's FontManager
 * implementations. The platform specific parts are declared as abstract
 * methods that have to be implemented by specific implementations.
 */
public abstract class SunFontManager implements FontSupport, FontManagerForSGE {

    private static class TTFilter implements FilenameFilter {
        public boolean accept(File dir,String name) {
            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.ttf */
                return false;
            } else {
                return(name.startsWith(".ttf", offset) ||
                       name.startsWith(".TTF", offset) ||
                       name.startsWith(".ttc", offset) ||
                       name.startsWith(".TTC", offset) ||
                       name.startsWith(".otf", offset) ||
                       name.startsWith(".OTF", offset));
            }
        }
    }

    private static class T1Filter implements FilenameFilter {
        public boolean accept(File dir,String name) {
            if (noType1Font) {
                return false;
            }
            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.pfa */
                return false;
            } else {
                return(name.startsWith(".pfa", offset) ||
                       name.startsWith(".pfb", offset) ||
                       name.startsWith(".PFA", offset) ||
                       name.startsWith(".PFB", offset));
            }
        }
    }

     private static class TTorT1Filter implements FilenameFilter {
        public boolean accept(File dir, String name) {

            /* all conveniently have the same suffix length */
            int offset = name.length()-4;
            if (offset <= 0) { /* must be at least A.ttf or A.pfa */
                return false;
            } else {
                boolean isTT =
                    name.startsWith(".ttf", offset) ||
                    name.startsWith(".TTF", offset) ||
                    name.startsWith(".ttc", offset) ||
                    name.startsWith(".TTC", offset) ||
                    name.startsWith(".otf", offset) ||
                    name.startsWith(".OTF", offset);
                if (isTT) {
                    return true;
                } else if (noType1Font) {
                    return false;
                } else {
                    return(name.startsWith(".pfa", offset) ||
                           name.startsWith(".pfb", offset) ||
                           name.startsWith(".PFA", offset) ||
                           name.startsWith(".PFB", offset));
                }
            }
        }
    }

    private static Font2DHandle FONT_HANDLE_NULL = new Font2DHandle(null);

    public static final int FONTFORMAT_NONE = -1;
    public static final int FONTFORMAT_TRUETYPE = 0;
    public static final int FONTFORMAT_TYPE1 = 1;
    public static final int FONTFORMAT_TTC = 2;
    public static final int FONTFORMAT_COMPOSITE = 3;
    public static final int FONTFORMAT_NATIVE = 4;

    /* Pool of 20 font file channels chosen because some UTF-8 locale
     * composite fonts can use up to 16 platform fonts (including the
     * Lucida fall back). This should prevent channel thrashing when
     * dealing with one of these fonts.
     * The pool array stores the fonts, rather than directly referencing
     * the channels, as the font needs to do the open/close work.
     */
    // MACOSX begin -- need to access these in subclass
    protected static final int CHANNELPOOLSIZE = 20;
    protected FileFont[] fontFileCache = new FileFont[CHANNELPOOLSIZE];
    // MACOSX end
    private int lastPoolIndex = 0;

    /* Need to implement a simple linked list scheme for fast
     * traversal and lookup.
     * Also want to "fast path" dialog so there's minimal overhead.
     */
    /* There are at exactly 20 composite fonts: 5 faces (but some are not
     * usually different), in 4 styles. The array may be auto-expanded
     * later if more are needed, eg for user-defined composites or locale
     * variants.
     */
    private int maxCompFont = 0;
    private CompositeFont [] compFonts = new CompositeFont[20];
    private ConcurrentHashMap<String, CompositeFont>
        compositeFonts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PhysicalFont>
        physicalFonts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PhysicalFont>
        registeredFonts = new ConcurrentHashMap<>();

    /* given a full name find the Font. Remind: there's duplication
     * here in that this contains the content of compositeFonts +
     * physicalFonts.
     */
    // MACOSX begin -- need to access this in subclass
    protected ConcurrentHashMap<String, Font2D>
        fullNameToFont = new ConcurrentHashMap<>();
    // MACOSX end

    /* TrueType fonts have localised names. Support searching all
     * of these before giving up on a name.
     */
    private HashMap<String, TrueTypeFont> localeFullNamesToFont;

    private PhysicalFont defaultPhysicalFont;

    static boolean longAddresses;
    private boolean loaded1dot0Fonts = false;
    boolean loadedAllFonts = false;
    boolean loadedAllFontFiles = false;
    String[] jreOtherFontFiles;
    boolean noOtherJREFontFiles = false; // initial assumption.

    public static String jreLibDirName;
    public static String jreFontDirName;
    private static HashSet<String> missingFontFiles = null;
    private String defaultFontName;
    private String defaultFontFileName;
    protected HashSet<String> registeredFontFiles = new HashSet<>();

    private ArrayList<String> badFonts;
    /* fontPath is the location of all fonts on the system, excluding the
     * JRE's own font directory but including any path specified using the
     * sun.java2d.fontpath property. Together with that property,  it is
     * initialised by the getPlatformFontPath() method
     * This call must be followed by a call to registerFontDirs(fontPath)
     * once any extra debugging path has been appended.
     */
    protected String fontPath;
    private FontConfiguration fontConfig;
    /* discoveredAllFonts is set to true when all fonts on the font path are
     * discovered. This usually also implies opening, validating and
     * registering, but an implementation may be optimized to avold this.
     * So see also "loadedAllFontFiles"
     */
    private boolean discoveredAllFonts = false;

    /* No need to keep consing up new instances - reuse a singleton.
     * The trade-off is that these objects don't get GC'd.
     */
    private static final FilenameFilter ttFilter = new TTFilter();
    private static final FilenameFilter t1Filter = new T1Filter();

    private Font[] allFonts;
    private String[] allFamilies; // cache for default locale only
    private Locale lastDefaultLocale;

    public static boolean noType1Font;

    /* Used to indicate required return type from toArray(..); */
    private static String[] STR_ARRAY = new String[0];

    /**
     * Deprecated, unsupported hack - actually invokes a bug!
     * Left in for a customer, don't remove.
     */
    private boolean usePlatformFontMetrics = false;

    /**
     * Returns the global SunFontManager instance. This is similar to
     * {@link FontManagerFactory#getInstance()} but it returns a
     * SunFontManager instance instead. This is only used in internal classes
     * where we can safely assume that a SunFontManager is to be used.
     *
     * @return the global SunFontManager instance
     */
    public static SunFontManager getInstance() {
        FontManager fm = FontManagerFactory.getInstance();
        return (SunFontManager) fm;
    }

    public FilenameFilter getTrueTypeFilter() {
        return ttFilter;
    }

    public FilenameFilter getType1Filter() {
        return t1Filter;
    }

    /* After we reach MAXSOFTREFCNT, use weak refs for created fonts.
     * This means that a small number of created fonts as used in a UI app
     * will not be eagerly collected, but an app that create many will
     * have them collected more frequently to reclaim storage.
     */
    private static int maxSoftRefCnt = 10;

    static {
        initStatic();
    }

    @SuppressWarnings("removal")
    private static void initStatic() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                FontManagerNativeLibrary.load();

                // JNI throws an exception if a class/method/field is not found,
                // so there's no need to do anything explicit here.
                initIDs();

                switch (StrikeCache.nativeAddressSize) {
                case 8: longAddresses = true; break;
                case 4: longAddresses = false; break;
                default: throw new RuntimeException("Unexpected address size");
                }

                noType1Font = "true".equals(System.getProperty("sun.java2d.noType1Font"));
                jreLibDirName = System.getProperty("java.home","") + File.separator + "lib";
                jreFontDirName = jreLibDirName + File.separator + "fonts";

                maxSoftRefCnt = Integer.getInteger("sun.java2d.font.maxSoftRefs", 10);
                return null;
            }
        });
    }

    /**
     * If the module image layout changes the location of JDK fonts,
     * this will be updated to reflect that.
     */
    public static final String getJDKFontDir() {
        return jreFontDirName;
    }

    public TrueTypeFont getEUDCFont() {
        // Overridden in Windows.
        return null;
    }

    /* Initialise ptrs used by JNI methods */
    private static native void initIDs();

    @SuppressWarnings("removal")
    protected SunFontManager() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                File badFontFile =
                    new File(jreFontDirName + File.separator + "badfonts.txt");
                if (badFontFile.exists()) {
                    badFonts = new ArrayList<>();
                    try (FileInputStream fis = new FileInputStream(badFontFile);
                         BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                        while (true) {
                            String name = br.readLine();
                            if (name == null) {
                                break;
                            } else {
                                if (FontUtilities.debugFonts()) {
                                    FontUtilities.logWarning("read bad font: " + name);
                                }
                                badFonts.add(name);
                            }
                        }
                    } catch (IOException e) {
                    }
                }

                /* Here we get the fonts in jre/lib/fonts and register
                 * them so they are always available and preferred over
                 * other fonts. This needs to be registered before the
                 * composite fonts as otherwise some native font that
                 * corresponds may be found as we don't have a way to
                 * handle two fonts of the same name, so the JRE one
                 * must be the first one registered. Pass "true" to
                 * registerFonts method as on-screen these JRE fonts
                 * always go through the JDK rasteriser.
                 */
                if (FontUtilities.isLinux) {
                    /* Linux font configuration uses these fonts */
                    registerFontDir(jreFontDirName);
                }
                registerFontsInDir(jreFontDirName, true, Font2D.JRE_RANK,
                                   true, false);

                /* Create the font configuration and get any font path
                 * that might be specified.
                 */
                fontConfig = createFontConfiguration();

                String[] fontInfo = getDefaultPlatformFont();
                defaultFontName = fontInfo[0];
                if (defaultFontName == null && FontUtilities.debugFonts()) {
                    FontUtilities.logWarning("defaultFontName is null");
                }
                defaultFontFileName = fontInfo[1];

                String extraFontPath = fontConfig.getExtraFontPath();

                /* In prior releases the debugging font path replaced
                 * all normally located font directories except for the
                 * JRE fonts dir. This directory is still always located
                 * and placed at the head of the path but as an
                 * augmentation to the previous behaviour the
                 * changes below allow you to additionally append to
                 * the font path by starting with append: or prepend by
                 * starting with a prepend: sign. Eg: to append
                 * -Dsun.java2d.fontpath=append:/usr/local/myfonts
                 * and to prepend
                 * -Dsun.java2d.fontpath=prepend:/usr/local/myfonts Disp
                 *
                 * If there is an appendedfontpath it in the font
                 * configuration it is used instead of searching the
                 * system for dirs.
                 * The behaviour of append and prepend is then similar
                 * to the normal case. ie it goes after what
                 * you prepend and * before what you append. If the
                 * sun.java2d.fontpath property is used, but it
                 * neither the append or prepend syntaxes is used then
                 * as except for the JRE dir the path is replaced and it
                 * is up to you to make sure that all the right
                 * directories are located. This is platform and
                 * locale-specific so its almost impossible to get
                 * right, so it should be used with caution.
                 */
                boolean prependToPath = false;
                boolean appendToPath = false;
                String dbgFontPath = System.getProperty("sun.java2d.fontpath");

                if (dbgFontPath != null) {
                    if (dbgFontPath.startsWith("prepend:")) {
                        prependToPath = true;
                        dbgFontPath =
                            dbgFontPath.substring("prepend:".length());
                    } else if (dbgFontPath.startsWith("append:")) {
                        appendToPath = true;
                        dbgFontPath =
                            dbgFontPath.substring("append:".length());
                    }
                }

                if (FontUtilities.debugFonts()) {
                    FontUtilities.logInfo("JRE font directory: " + jreFontDirName);
                    FontUtilities.logInfo("Extra font path: " + extraFontPath);
                    FontUtilities.logInfo("Debug font path: " + dbgFontPath);
                }

                if (dbgFontPath != null) {
                    /* In debugging mode we register all the paths
                     * Caution: this is a very expensive call on Solaris:-
                     */
                    fontPath = getPlatformFontPath(noType1Font);

                    if (extraFontPath != null) {
                        fontPath = extraFontPath + File.pathSeparator + fontPath;
                    }
                    if (appendToPath) {
                        fontPath += File.pathSeparator + dbgFontPath;
                    } else if (prependToPath) {
                        fontPath = dbgFontPath + File.pathSeparator + fontPath;
                    } else {
                        fontPath = dbgFontPath;
                    }
                    registerFontDirs(fontPath);
                } else if (extraFontPath != null) {
                    /* If the font configuration contains an
                     * "appendedfontpath" entry, it is interpreted as a
                     * set of locations that should always be registered.
                     * It may be additional to locations normally found
                     * for that place, or it may be locations that need
                     * to have all their paths registered to locate all
                     * the needed platform names.
                     * This is typically when the same .TTF file is
                     * referenced from multiple font.dir files and all
                     * of these must be read to find all the native
                     * (XLFD) names for the font, so that X11 font APIs
                     * can be used for as many code points as possible.
                     */
                    registerFontDirs(extraFontPath);
                }

                initCompositeFonts(fontConfig, null);

                return null;
            }
        });

        boolean platformFont = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        String prop = System.getProperty("java2d.font.usePlatformFont");
                        String env = System.getenv("JAVA2D_USEPLATFORMFONT");
                        return "true".equals(prop) || env != null;
                    }
            });

        if (platformFont) {
            usePlatformFontMetrics = true;
            System.out.println("Enabling platform font metrics for win32. This is an unsupported option.");
            System.out.println("This yields incorrect composite font metrics as reported by 1.1.x releases.");
            System.out.println("It is appropriate only for use by applications which do not use any Java 2");
            System.out.println("functionality. This property will be removed in a later release.");
        }
    }

    public Font2DHandle getNewComposite(String family, int style,
                                        Font2DHandle handle) {

        if (!(handle.font2D instanceof CompositeFont)) {
            return handle;
        }

        CompositeFont oldComp = (CompositeFont)handle.font2D;
        PhysicalFont oldFont = oldComp.getSlotFont(0);

        if (family == null) {
            family = oldFont.getFamilyName(null);
        }
        if (style == -1) {
            style = oldComp.getStyle();
        }

        Font2D newFont = findFont2D(family, style, NO_FALLBACK);
        if (!(newFont instanceof PhysicalFont)) {
            newFont = oldFont;
        }
        PhysicalFont physicalFont = (PhysicalFont)newFont;
        CompositeFont dialog2D =
            (CompositeFont)findFont2D("dialog", style, NO_FALLBACK);
        if (dialog2D == null) { /* shouldn't happen */
            return handle;
        }
        CompositeFont compFont = new CompositeFont(physicalFont, dialog2D);
        Font2DHandle newHandle = new Font2DHandle(compFont);
        return newHandle;
    }

    protected void registerCompositeFont(String compositeName,
                                      String[] componentFileNames,
                                      String[] componentNames,
                                      int numMetricsSlots,
                                      int[] exclusionRanges,
                                      int[] exclusionMaxIndex,
                                      boolean defer) {

        CompositeFont cf = new CompositeFont(compositeName,
                                             componentFileNames,
                                             componentNames,
                                             numMetricsSlots,
                                             exclusionRanges,
                                             exclusionMaxIndex, defer, this);
        addCompositeToFontList(cf, Font2D.FONT_CONFIG_RANK);
        synchronized (compFonts) {
            compFonts[maxCompFont++] = cf;
        }
    }

    /* This variant is used only when the application specifies
     * a variant of composite fonts which prefers locale specific or
     * proportional fonts.
     */
    protected static void registerCompositeFont(String compositeName,
                                                String[] componentFileNames,
                                                String[] componentNames,
                                                int numMetricsSlots,
                                                int[] exclusionRanges,
                                                int[] exclusionMaxIndex,
                                                boolean defer,
                                                ConcurrentHashMap<String, Font2D>
                                                altNameCache) {

        CompositeFont cf = new CompositeFont(compositeName,
                                             componentFileNames,
                                             componentNames,
                                             numMetricsSlots,
                                             exclusionRanges,
                                             exclusionMaxIndex, defer,
                                             SunFontManager.getInstance());

        /* if the cache has an existing composite for this case, make
         * its handle point to this new font.
         * This ensures that when the altNameCache that is passed in
         * is the global mapNameCache - ie we are running as an application -
         * that any statically created java.awt.Font instances which already
         * have a Font2D instance will have that re-directed to the new Font
         * on subsequent uses. This is particularly important for "the"
         * default font instance, or similar cases where a UI toolkit (eg
         * Swing) has cached a java.awt.Font. Note that if Swing is using
         * a custom composite APIs which update the standard composites have
         * no effect - this is typically the case only when using the Windows
         * L&F where these APIs would conflict with that L&F anyway.
         */
        Font2D oldFont =altNameCache.get(compositeName.toLowerCase(Locale.ENGLISH));
        if (oldFont instanceof CompositeFont) {
            oldFont.handle.font2D = cf;
        }
        altNameCache.put(compositeName.toLowerCase(Locale.ENGLISH), cf);
    }

    private void addCompositeToFontList(CompositeFont f, int rank) {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Add to Family " + f.familyName +
                        ", Font " + f.fullName + " rank=" + rank);
        }
        f.setRank(rank);
        compositeFonts.put(f.fullName, f);
        fullNameToFont.put(f.fullName.toLowerCase(Locale.ENGLISH), f);

        FontFamily family = FontFamily.getFamily(f.familyName);
        if (family == null) {
            family = new FontFamily(f.familyName, true, rank);
        }
        family.setFont(f, f.style);
    }

    /*
     * Systems may have fonts with the same name.
     * We want to register only one of such fonts (at least until
     * such time as there might be APIs which can accommodate > 1).
     * Rank is 1) font configuration fonts, 2) JRE fonts, 3) OT/TT fonts,
     * 4) Type1 fonts, 5) native fonts.
     *
     * If the new font has the same name as the old font, the higher
     * ranked font gets added, replacing the lower ranked one.
     * If the fonts are of equal rank, then make a special case of
     * font configuration rank fonts, which are on closer inspection,
     * OT/TT fonts such that the larger font is registered. This is
     * a heuristic since a font may be "larger" in the sense of more
     * code points, or be a larger "file" because it has more bitmaps.
     * So it is possible that using filesize may lead to less glyphs, and
     * using glyphs may lead to lower quality display. Probably number
     * of glyphs is the ideal, but filesize is information we already
     * have and is good enough for the known cases.
     * Also don't want to register fonts that match JRE font families
     * but are coming from a source other than the JRE.
     * This will ensure that we will algorithmically style the JRE
     * plain font and get the same set of glyphs for all styles.
     *
     * Note that this method returns a value
     * if it returns the same object as its argument that means this
     * font was newly registered.
     * If it returns a different object it means this font already exists,
     * and you should use that one.
     * If it returns null means this font was not registered and none
     * in that name is registered. The caller must find a substitute
     */
    // MACOSX begin -- need to access this in subclass
    protected PhysicalFont addToFontList(PhysicalFont f, int rank) {
    // MACOSX end

        String fontName = f.fullName;
        String familyName = f.familyName;
        if (fontName == null || fontName.isEmpty()) {
            return null;
        }
        if (compositeFonts.containsKey(fontName)) {
            /* Don't register any font that has the same name as a composite */
            return null;
        }
        f.setRank(rank);
        if (!physicalFonts.containsKey(fontName)) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Add to Family " + familyName +
                            ", Font " + fontName + " rank=" + rank);
            }
            physicalFonts.put(fontName, f);
            FontFamily family = FontFamily.getFamily(familyName);
            if (family == null) {
                family = new FontFamily(familyName, false, rank);
                family.setFont(f, f.style);
            } else {
                family.setFont(f, f.style);
            }
            fullNameToFont.put(fontName.toLowerCase(Locale.ENGLISH), f);
            return f;
        } else {
            PhysicalFont newFont = f;
            PhysicalFont oldFont = physicalFonts.get(fontName);
            if (oldFont == null) {
                return null;
            }
            /* If the new font is of an equal or higher rank, it is a
             * candidate to replace the current one, subject to further tests.
             */
            if (oldFont.getRank() >= rank) {

                /* All fonts initialise their mapper when first
                 * used. If the mapper is non-null then this font
                 * has been accessed at least once. In that case
                 * do not replace it. This may be overly stringent,
                 * but its probably better not to replace a font that
                 * someone is already using without a compelling reason.
                 * Additionally the primary case where it is known
                 * this behaviour is important is in certain composite
                 * fonts, and since all the components of a given
                 * composite are usually initialised together this
                 * is unlikely. For this to be a problem, there would
                 * have to be a case where two different composites used
                 * different versions of the same-named font, and they
                 * were initialised and used at separate times.
                 * In that case we continue on and allow the new font to
                 * be installed, but replaceFont will continue to allow
                 * the original font to be used in Composite fonts.
                 */
                if (oldFont.mapper != null && rank > Font2D.FONT_CONFIG_RANK) {
                    return oldFont;
                }

                /* Normally we require a higher rank to replace a font,
                 * but as a special case, if the two fonts are the same rank,
                 * and are instances of TrueTypeFont we want the
                 * more complete (larger) one.
                 */
                if (oldFont.getRank() == rank) {
                    if (oldFont instanceof TrueTypeFont &&
                        newFont instanceof TrueTypeFont) {
                        TrueTypeFont oldTTFont = (TrueTypeFont)oldFont;
                        TrueTypeFont newTTFont = (TrueTypeFont)newFont;
                        if (oldTTFont.fileSize >= newTTFont.fileSize) {
                            return oldFont;
                        }
                    } else {
                        return oldFont;
                    }
                }
                /* Don't replace ever JRE fonts.
                 * This test is in case a font configuration references
                 * a Lucida font, which has been mapped to a Lucida
                 * from the host O/S. The assumption here is that any
                 * such font configuration file is probably incorrect, or
                 * the host O/S version is for the use of AWT.
                 * In other words if we reach here, there's a possible
                 * problem with our choice of font configuration fonts.
                 */
                if (oldFont.platName.startsWith(jreFontDirName)) {
                    if (FontUtilities.isLogging()) {
                        FontUtilities.logWarning("Unexpected attempt to replace a JRE " +
                                       " font " + fontName + " from " + oldFont.platName +
                                       " with " + newFont.platName);
                    }
                    return oldFont;
                }

                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Replace in Family " + familyName +
                                    ",Font " + fontName + " new rank="+rank +
                                    " from " + oldFont.platName +
                                    " with " + newFont.platName);
                }
                replaceFont(oldFont, newFont);
                physicalFonts.put(fontName, newFont);
                fullNameToFont.put(fontName.toLowerCase(Locale.ENGLISH),
                                   newFont);

                FontFamily family = FontFamily.getFamily(familyName);
                if (family == null) {
                    family = new FontFamily(familyName, false, rank);
                    family.setFont(newFont, newFont.style);
                } else {
                    family.setFont(newFont, newFont.style);
                }
                return newFont;
            } else {
                return oldFont;
            }
        }
    }

    public Font2D[] getRegisteredFonts() {
        PhysicalFont[] physFonts = getPhysicalFonts();
        int mcf = maxCompFont; /* for MT-safety */
        Font2D[] regFonts = new Font2D[physFonts.length+mcf];
        System.arraycopy(compFonts, 0, regFonts, 0, mcf);
        System.arraycopy(physFonts, 0, regFonts, mcf, physFonts.length);
        return regFonts;
    }

    protected PhysicalFont[] getPhysicalFonts() {
        return physicalFonts.values().toArray(new PhysicalFont[0]);
    }


    /* The class FontRegistrationInfo is used when a client says not
     * to register a font immediately. This mechanism is used to defer
     * initialisation of all the components of composite fonts at JRE
     * start-up. The CompositeFont class is "aware" of this and when it
     * is first used it asks for the registration of its components.
     * Also in the event that any physical font is requested the
     * deferred fonts are initialised before triggering a search of the
     * system.
     * Two maps are used. One to track the deferred fonts. The
     * other to track the fonts that have been initialised through this
     * mechanism.
     */

    private static final class FontRegistrationInfo {

        String fontFilePath;
        String[] nativeNames;
        int fontFormat;
        boolean javaRasterizer;
        int fontRank;

        FontRegistrationInfo(String fontPath, String[] names, int format,
                             boolean useJavaRasterizer, int rank) {
            this.fontFilePath = fontPath;
            this.nativeNames = names;
            this.fontFormat = format;
            this.javaRasterizer = useJavaRasterizer;
            this.fontRank = rank;
        }
    }

    private final ConcurrentHashMap<String, FontRegistrationInfo>
        deferredFontFiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Font2DHandle>
        initialisedFonts = new ConcurrentHashMap<>();

    /* Remind: possibly enhance initialiseDeferredFonts() to be
     * optionally given a name and a style and it could stop when it
     * finds that font - but this would be a problem if two of the
     * fonts reference the same font face name (cf the Solaris
     * euro fonts).
     */
    protected synchronized void initialiseDeferredFonts() {
        for (String fileName : deferredFontFiles.keySet()) {
            initialiseDeferredFont(fileName);
        }
    }

    protected synchronized void registerDeferredJREFonts(String jreDir) {
        for (FontRegistrationInfo info : deferredFontFiles.values()) {
            if (info.fontFilePath != null &&
                info.fontFilePath.startsWith(jreDir)) {
                initialiseDeferredFont(info.fontFilePath);
            }
        }
    }

    public boolean isDeferredFont(String fileName) {
        return deferredFontFiles.containsKey(fileName);
    }

    PhysicalFont findJREDeferredFont(String name, int style) {

        /* Iterate over the deferred font files looking for any in the
         * jre directory that we didn't recognise, open each of these.
         * In almost all installations this will quickly fall through
         * because jreOtherFontFiles will be empty.
         * noOtherJREFontFiles is used so we can skip this block as soon
         * as its determined that it's not needed - almost always after the
         * very first time through.
         */
        if (noOtherJREFontFiles) {
            return null;
        }
        synchronized (jreFontDirName) {
            if (jreOtherFontFiles == null) {
                HashSet<String> otherFontFiles = new HashSet<>();
                for (String deferredFile : deferredFontFiles.keySet()) {
                    File file = new File(deferredFile);
                    String dir = file.getParent();
                    /* skip names which aren't absolute, aren't in the JRE
                     * directory, or are known Lucida fonts.
                     */
                    if (dir == null || !dir.equals(jreFontDirName)) {
                        continue;
                    }
                    otherFontFiles.add(deferredFile);
                }
                jreOtherFontFiles = otherFontFiles.toArray(STR_ARRAY);
                if (jreOtherFontFiles.length == 0) {
                    noOtherJREFontFiles = true;
                }
            }

            for (int i=0; i<jreOtherFontFiles.length;i++) {
                String fileName = jreOtherFontFiles[i];
                if (fileName == null) {
                    continue;
                }
                jreOtherFontFiles[i] = null;
                PhysicalFont physicalFont = initialiseDeferredFont(fileName);
                if (physicalFont != null &&
                    (physicalFont.getFontName(null).equalsIgnoreCase(name) ||
                     physicalFont.getFamilyName(null).equalsIgnoreCase(name))
                    && physicalFont.style == style) {
                    return physicalFont;
                }
            }
        }

        return null;
    }

    private PhysicalFont findOtherDeferredFont(String name, int style) {
        for (String fileName : deferredFontFiles.keySet()) {
            PhysicalFont physicalFont = initialiseDeferredFont(fileName);
            if (physicalFont != null &&
                (physicalFont.getFontName(null).equalsIgnoreCase(name) ||
                physicalFont.getFamilyName(null).equalsIgnoreCase(name)) &&
                physicalFont.style == style) {
                return physicalFont;
            }
        }
        return null;
    }

    private PhysicalFont findDeferredFont(String name, int style) {
        PhysicalFont physicalFont = findJREDeferredFont(name, style);
        if (physicalFont != null) {
            return physicalFont;
        } else {
            return findOtherDeferredFont(name, style);
        }
    }

    public void registerDeferredFont(String fileNameKey,
                                     String fullPathName,
                                     String[] nativeNames,
                                     int fontFormat,
                                     boolean useJavaRasterizer,
                                     int fontRank) {
        FontRegistrationInfo regInfo =
            new FontRegistrationInfo(fullPathName, nativeNames, fontFormat,
                                     useJavaRasterizer, fontRank);
        deferredFontFiles.put(fileNameKey, regInfo);
    }


    public synchronized
         PhysicalFont initialiseDeferredFont(String fileNameKey) {

        if (fileNameKey == null) {
            return null;
        }
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Opening deferred font file " + fileNameKey);
        }

        PhysicalFont physicalFont = null;
        FontRegistrationInfo regInfo = deferredFontFiles.get(fileNameKey);
        if (regInfo != null) {
            deferredFontFiles.remove(fileNameKey);
            physicalFont = registerFontFile(regInfo.fontFilePath,
                                            regInfo.nativeNames,
                                            regInfo.fontFormat,
                                            regInfo.javaRasterizer,
                                            regInfo.fontRank);

            if (physicalFont != null) {
                /* Store the handle, so that if a font is bad, we
                 * retrieve the substituted font.
                 */
                initialisedFonts.put(fileNameKey, physicalFont.handle);
            } else {
                initialisedFonts.put(fileNameKey, FONT_HANDLE_NULL);
            }
        } else {
            Font2DHandle handle = initialisedFonts.get(fileNameKey);
            if (handle == null) {
                /* Probably shouldn't happen, but just in case */
                initialisedFonts.put(fileNameKey, FONT_HANDLE_NULL);
            } else {
                physicalFont = (PhysicalFont)(handle.font2D);
            }
        }
        return physicalFont;
    }

    public boolean isRegisteredFontFile(String name) {
        return registeredFonts.containsKey(name);
    }

    public PhysicalFont getRegisteredFontFile(String name) {
        return registeredFonts.get(name);
    }

    /* Note that the return value from this method is not always
     * derived from this file, and may be null. See addToFontList for
     * some explanation of this.
     */
    public PhysicalFont registerFontFile(String fileName,
                                         String[] nativeNames,
                                         int fontFormat,
                                         boolean useJavaRasterizer,
                                         int fontRank) {

        PhysicalFont regFont = registeredFonts.get(fileName);
        if (regFont != null) {
            return regFont;
        }

        PhysicalFont physicalFont = null;
        try {
            switch (fontFormat) {

            case FONTFORMAT_TRUETYPE:
                int fn = 0;
                TrueTypeFont ttf;
                do {
                    ttf = new TrueTypeFont(fileName, nativeNames, fn++,
                                           useJavaRasterizer);
                    PhysicalFont pf = addToFontList(ttf, fontRank);
                    if (physicalFont == null) {
                        physicalFont = pf;
                    }
                }
                while (fn < ttf.getFontCount());
                break;

            case FONTFORMAT_TYPE1:
                Type1Font t1f = new Type1Font(fileName, nativeNames);
                physicalFont = addToFontList(t1f, fontRank);
                break;

            case FONTFORMAT_NATIVE:
                NativeFont nf = new NativeFont(fileName, false);
                physicalFont = addToFontList(nf, fontRank);
                break;
            default:

            }
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Registered file " + fileName + " as font " +
                                physicalFont + " rank="  + fontRank);
            }
        } catch (FontFormatException ffe) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Unusable font: " + fileName + " " + ffe.toString());
            }
        }
        if (physicalFont != null &&
            fontFormat != FONTFORMAT_NATIVE) {
            registeredFonts.put(fileName, physicalFont);
        }
        return physicalFont;
    }

    public void registerFonts(String[] fileNames,
                              String[][] nativeNames,
                              int fontCount,
                              int fontFormat,
                              boolean useJavaRasterizer,
                              int fontRank, boolean defer) {

        for (int i=0; i < fontCount; i++) {
            if (defer) {
                registerDeferredFont(fileNames[i],fileNames[i], nativeNames[i],
                                     fontFormat, useJavaRasterizer, fontRank);
            } else {
                registerFontFile(fileNames[i], nativeNames[i],
                                 fontFormat, useJavaRasterizer, fontRank);
            }
        }
    }

    /*
     * This is the Physical font used when some other font on the system
     * can't be located. There has to be at least one font or the font
     * system is not useful and the graphics environment cannot sustain
     * the Java platform.
     */
    public PhysicalFont getDefaultPhysicalFont() {
        if (defaultPhysicalFont == null) {
            String defaultFontName = getDefaultFontFaceName();
            // findFont2D will load all fonts
            Font2D font2d = findFont2D(defaultFontName, Font.PLAIN, NO_FALLBACK);
            if (font2d != null) {
                if (font2d instanceof PhysicalFont) {
                    defaultPhysicalFont = (PhysicalFont)font2d;
                } else {
                    if (FontUtilities.isLogging()) {
                        FontUtilities.logWarning("Font returned by findFont2D for default font name " +
                                     defaultFontName + " is not a physical font: " + font2d.getFontName(null));
                    }
                }
            }
            if (defaultPhysicalFont == null) {
                /* Because of the findFont2D call above, if we reach here, we
                 * know all fonts have already been loaded, just accept any
                 * match at this point. If this fails we are in real trouble
                 * and I don't know how to recover from there being absolutely
                 * no fonts anywhere on the system.
                 */
                defaultPhysicalFont = physicalFonts.values().stream().findFirst()
                    .orElseThrow(()->new Error("Probable fatal error: No physical fonts found."));
            }
        }
        return defaultPhysicalFont;
    }

    public Font2D getDefaultLogicalFont(int style) {
        return findFont2D("dialog", style, NO_FALLBACK);
    }

    /*
     * return String representation of style prepended with "."
     * This is useful for performance to avoid unnecessary string operations.
     */
    private static String dotStyleStr(int num) {
        switch(num){
          case Font.BOLD:
            return ".bold";
          case Font.ITALIC:
            return ".italic";
          case Font.ITALIC | Font.BOLD:
            return ".bolditalic";
          default:
            return ".plain";
        }
    }

    /* This is implemented only on windows and is called from code that
     * executes only on windows. This isn't pretty but its not a precedent
     * in this file. This very probably should be cleaned up at some point.
     */
    protected void
        populateFontFileNameMap(HashMap<String,String> fontToFileMap,
                                HashMap<String,String> fontToFamilyNameMap,
                                HashMap<String,ArrayList<String>>
                                familyToFontListMap,
                                Locale locale) {
    }

    /* Obtained from Platform APIs (windows only)
     * Map from lower-case font full name to basename of font file.
     * Eg "arial bold" -> ARIALBD.TTF.
     * For TTC files, there is a mapping for each font in the file.
     */
    private HashMap<String,String> fontToFileMap = null;

    /* Obtained from Platform APIs (windows only)
     * Map from lower-case font full name to the name of its font family
     * Eg "arial bold" -> "Arial"
     */
    private HashMap<String,String> fontToFamilyNameMap = null;

    /* Obtained from Platform APIs (windows only)
     * Map from a lower-case family name to a list of full names of
     * the member fonts, eg:
     * "arial" -> ["Arial", "Arial Bold", "Arial Italic","Arial Bold Italic"]
     */
    private HashMap<String,ArrayList<String>> familyToFontListMap= null;

    /* The directories which contain platform fonts */
    private String[] pathDirs = null;

    private boolean haveCheckedUnreferencedFontFiles;

    @SuppressWarnings("removal")
    private String[] getFontFilesFromPath(boolean noType1) {
        final FilenameFilter filter;
        if (noType1) {
            filter = ttFilter;
        } else {
            filter = new TTorT1Filter();
        }
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            public String[] run() {
                if (pathDirs.length == 1) {
                    File dir = new File(pathDirs[0]);
                    String[] files = dir.list(filter);
                    if (files == null) {
                        return new String[0];
                    }
                    for (int f=0; f<files.length; f++) {
                        files[f] = files[f].toLowerCase();
                    }
                    return files;
                } else {
                    ArrayList<String> fileList = new ArrayList<>();
                    for (int i = 0; i< pathDirs.length; i++) {
                        File dir = new File(pathDirs[i]);
                        String[] files = dir.list(filter);
                        if (files == null) {
                            continue;
                        }
                        for (int f = 0; f < files.length ; f++) {
                            fileList.add(files[f].toLowerCase());
                        }
                    }
                    return fileList.toArray(STR_ARRAY);
                }
            }
        });
    }

    /* This is needed since some windows registry names don't match
     * the font names.
     * - UPC styled font names have a double space, but the
     * registry entry mapping to a file doesn't.
     * - Marlett is in a hidden file not listed in the registry
     * - The registry advertises that the file david.ttf contains a
     * font with the full name "David Regular" when in fact its
     * just "David".
     * Directly fix up these known cases as this is faster.
     * If a font which doesn't match these known cases has no file,
     * it may be a font that has been temporarily added to the known set
     * or it may be an installed font with a missing registry entry.
     * Installed fonts are those in the windows font directories.
     * Make a best effort attempt to locate these.
     * We obtain the list of TrueType fonts in these directories and
     * filter out all the font files we already know about from the registry.
     * What remains may be "bad" fonts, duplicate fonts, or perhaps the
     * missing font(s) we are looking for.
     * Open each of these files to find out.
     */
    private void resolveWindowsFonts() {

        ArrayList<String> unmappedFontNames = null;
        for (String font : fontToFamilyNameMap.keySet()) {
            String file = fontToFileMap.get(font);
            if (file == null) {
                if (font.indexOf("  ") > 0) {
                    String newName = font.replaceFirst("  ", " ");
                    file = fontToFileMap.get(newName);
                    /* If this name exists and isn't for a valid name
                     * replace the mapping to the file with this font
                     */
                    if (file != null &&
                        !fontToFamilyNameMap.containsKey(newName)) {
                        fontToFileMap.remove(newName);
                        fontToFileMap.put(font, file);
                    }
                } else if (font.equals("marlett")) {
                    fontToFileMap.put(font, "marlett.ttf");
                } else if (font.equals("david")) {
                    file = fontToFileMap.get("david regular");
                    if (file != null) {
                        fontToFileMap.remove("david regular");
                        fontToFileMap.put("david", file);
                    }
                } else {
                    if (unmappedFontNames == null) {
                        unmappedFontNames = new ArrayList<>();
                    }
                    unmappedFontNames.add(font);
                }
            }
        }

        if (unmappedFontNames != null) {
            HashSet<String> unmappedFontFiles = new HashSet<>();

            /* Every font key in fontToFileMap ought to correspond to a
             * font key in fontToFamilyNameMap. Entries that don't seem
             * to correspond are likely fonts that were named differently
             * by GDI than in the registry. One known cause of this is when
             * Windows has had its regional settings changed so that from
             * GDI we get a localised (eg Chinese or Japanese) name for the
             * font, but the registry retains the English version of the name
             * that corresponded to the "install" locale for windows.
             * Since we are in this code block because there are unmapped
             * font names, we can look to find unused font->file mappings
             * and then open the files to read the names. We don't generally
             * want to open font files, as its a performance hit, but this
             * occurs only for a small number of fonts on specific system
             * configs - ie is believed that a "true" Japanese windows would
             * have JA names in the registry too.
             * Clone fontToFileMap and remove from the clone all keys which
             * match a fontToFamilyNameMap key. What remains maps to the
             * files we want to open to find the fonts GDI returned.
             * A font in such a file is added to the fontToFileMap after
             * checking its one of the unmappedFontNames we are looking for.
             * The original name that didn't map is removed from fontToFileMap
             * so essentially this "fixes up" fontToFileMap to use the same
             * name as GDI.
             * Also note that typically the fonts for which this occurs in
             * CJK locales are TTC fonts and not all fonts in a TTC may have
             * localised names. Eg MSGOTHIC.TTC contains 3 fonts and one of
             * them "MS UI Gothic" has no JA name whereas the other two do.
             * So not every font in these files is unmapped or new.
             */
            @SuppressWarnings("unchecked")
            HashMap<String,String> ffmapCopy =
                (HashMap<String,String>)(fontToFileMap.clone());
            for (String key : fontToFamilyNameMap.keySet()) {
                ffmapCopy.remove(key);
            }
            for (String key : ffmapCopy.keySet()) {
                unmappedFontFiles.add(ffmapCopy.get(key));
                fontToFileMap.remove(key);
            }

            resolveFontFiles(unmappedFontFiles, unmappedFontNames);

            /* If there are still unmapped font names, this means there's
             * something that wasn't in the registry. We need to get all
             * the font files directly and look at the ones that weren't
             * found in the registry.
             */
            if (unmappedFontNames.size() > 0) {

                /* getFontFilesFromPath() returns all lower case names.
                 * To compare we also need lower case
                 * versions of the names from the registry.
                 */
                ArrayList<String> registryFiles = new ArrayList<>();

                for (String regFile : fontToFileMap.values()) {
                    registryFiles.add(regFile.toLowerCase());
                }
                /* We don't look for Type1 files here as windows will
                 * not enumerate these, so aren't useful in reconciling
                 * GDI's unmapped files. We do find these later when
                 * we enumerate all fonts.
                 */
                for (String pathFile : getFontFilesFromPath(true)) {
                    if (!registryFiles.contains(pathFile)) {
                        unmappedFontFiles.add(pathFile);
                    }
                }

                resolveFontFiles(unmappedFontFiles, unmappedFontNames);
            }

            /* remove from the set of names that will be returned to the
             * user any fonts that can't be mapped to files.
             */
            if (unmappedFontNames.size() > 0) {
                int sz = unmappedFontNames.size();
                for (int i=0; i<sz; i++) {
                    String name = unmappedFontNames.get(i);
                    String familyName = fontToFamilyNameMap.get(name);
                    if (familyName != null) {
                        ArrayList<String> family = familyToFontListMap.get(familyName);
                        if (family != null) {
                            if (family.size() <= 1) {
                                familyToFontListMap.remove(familyName);
                            }
                        }
                    }
                    fontToFamilyNameMap.remove(name);
                    if (FontUtilities.isLogging()) {
                        FontUtilities.logInfo("No file for font:" + name);
                    }
                }
            }
        }
    }

    /**
     * In some cases windows may have fonts in the fonts folder that
     * don't show up in the registry or in the GDI calls to enumerate fonts.
     * The only way to find these is to list the directory. We invoke this
     * only in getAllFonts/Families, so most searches for a specific
     * font that is satisfied by the GDI/registry calls don't take the
     * additional hit of listing the directory. This hit is small enough
     * that its not significant in these 'enumerate all the fonts' cases.
     * The basic approach is to cross-reference the files windows found
     * with the ones in the directory listing approach, and for each
     * in the latter list that is missing from the former list, register it.
     */
    private synchronized void checkForUnreferencedFontFiles() {
        if (haveCheckedUnreferencedFontFiles) {
            return;
        }
        haveCheckedUnreferencedFontFiles = true;
        if (!FontUtilities.isWindows) {
            return;
        }
        /* getFontFilesFromPath() returns all lower case names.
         * To compare we also need lower case
         * versions of the names from the registry.
         */
        ArrayList<String> registryFiles = new ArrayList<>();
        for (String regFile : fontToFileMap.values()) {
            registryFiles.add(regFile.toLowerCase());
        }

        /* To avoid any issues with concurrent modification, create
         * copies of the existing maps, add the new fonts into these
         * and then replace the references to the old ones with the
         * new maps. ConcurrentHashmap is another option but its a lot
         * more changes and with this exception, these maps are intended
         * to be static.
         */
        HashMap<String,String> fontToFileMap2 = null;
        HashMap<String,String> fontToFamilyNameMap2 = null;
        HashMap<String,ArrayList<String>> familyToFontListMap2 = null;;

        for (String pathFile : getFontFilesFromPath(false)) {
            if (!registryFiles.contains(pathFile)) {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Found non-registry file : " + pathFile);
                }
                PhysicalFont f = registerFontFile(getPathName(pathFile));
                if (f == null) {
                    continue;
                }
                if (fontToFileMap2 == null) {
                    fontToFileMap2 = new HashMap<>(fontToFileMap);
                    fontToFamilyNameMap2 = new HashMap<>(fontToFamilyNameMap);
                    familyToFontListMap2 = new HashMap<>(familyToFontListMap);
                }
                String fontName = f.getFontName(null);
                String family = f.getFamilyName(null);
                String familyLC = family.toLowerCase();
                fontToFamilyNameMap2.put(fontName, family);
                fontToFileMap2.put(fontName, pathFile);
                ArrayList<String> fonts = familyToFontListMap2.get(familyLC);
                if (fonts == null) {
                    fonts = new ArrayList<>();
                } else {
                    fonts = new ArrayList<>(fonts);
                }
                fonts.add(fontName);
                familyToFontListMap2.put(familyLC, fonts);
            }
        }
        if (fontToFileMap2 != null) {
            fontToFileMap = fontToFileMap2;
            familyToFontListMap = familyToFontListMap2;
            fontToFamilyNameMap = fontToFamilyNameMap2;
        }
    }

    private void resolveFontFiles(HashSet<String> unmappedFiles,
                                  ArrayList<String> unmappedFonts) {

        Locale l = SunToolkit.getStartupLocale();

        for (String file : unmappedFiles) {
            try {
                int fn = 0;
                TrueTypeFont ttf;
                String fullPath = getPathName(file);
                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Trying to resolve file " + fullPath);
                }
                do {
                    ttf = new TrueTypeFont(fullPath, null, fn++, false);
                    //  prefer the font's locale name.
                    String fontName = ttf.getFontName(l).toLowerCase();
                    if (unmappedFonts.contains(fontName)) {
                        fontToFileMap.put(fontName, file);
                        unmappedFonts.remove(fontName);
                        if (FontUtilities.isLogging()) {
                            FontUtilities.logInfo("Resolved absent registry entry for " +
                                            fontName + " located in " + fullPath);
                        }
                    }
                }
                while (fn < ttf.getFontCount());
            } catch (Exception e) {
            }
        }
    }

    /* Hardwire the English names and expected file names of fonts
     * commonly used at start up. Avoiding until later even the small
     * cost of calling platform APIs to locate these can help.
     * The code that registers these fonts needs to "bail" if any
     * of the files do not exist, so it will verify the existence of
     * all non-null file names first.
     * They are added in to a map with nominally the first
     * word in the name of the family as the key. In all the cases
     * we are using the family name is a single word, and as is
     * more or less required the family name is the initial sequence
     * in a full name. So lookup first finds the matching description,
     * then registers the whole family, returning the right font.
     */
    public static class FamilyDescription {
        public String familyName;
        public String plainFullName;
        public String boldFullName;
        public String italicFullName;
        public String boldItalicFullName;
        public String plainFileName;
        public String boldFileName;
        public String italicFileName;
        public String boldItalicFileName;
        boolean failed;
    }

    static volatile HashMap<String, FamilyDescription> platformFontMap;

    /**
     * default implementation does nothing.
     */
    public HashMap<String, FamilyDescription> populateHardcodedFileNameMap() {
        return new HashMap<>(0);
    }

    @SuppressWarnings("removal")
    Font2D findFontFromPlatformMap(String lcName, int style) {
        HashMap<String, FamilyDescription> platformFontMap = SunFontManager.platformFontMap;
        if (platformFontMap == null) {
            platformFontMap = populateHardcodedFileNameMap();
            SunFontManager.platformFontMap = platformFontMap;
        }

        if (platformFontMap == null || platformFontMap.size() == 0) {
            return null;
        }

        int spaceIndex = lcName.indexOf(' ');
        String firstWord = lcName;
        if (spaceIndex > 0) {
            firstWord = lcName.substring(0, spaceIndex);
        }

        FamilyDescription fd = platformFontMap.get(firstWord);
        if (fd == null || fd.failed) {
            return null;
        }
        /* Once we've established that its at least the first word,
         * we need to dig deeper to make sure its a match for either
         * a full name, or the family name, to make sure its not
         * a request for some other font that just happens to start
         * with the same first word.
         */
        int styleIndex = -1;
        if (lcName.equalsIgnoreCase(fd.plainFullName)) {
            styleIndex = 0;
        } else if (lcName.equalsIgnoreCase(fd.boldFullName)) {
            styleIndex = 1;
        } else if (lcName.equalsIgnoreCase(fd.italicFullName)) {
            styleIndex = 2;
        } else if (lcName.equalsIgnoreCase(fd.boldItalicFullName)) {
            styleIndex = 3;
        }
        if (styleIndex == -1 && !lcName.equalsIgnoreCase(fd.familyName)) {
            return null;
        }

        String plainFile = null, boldFile = null,
            italicFile = null, boldItalicFile = null;

        boolean failure = false;
        /* In a terminal server config, its possible that getPathName()
         * will return null, if the file doesn't exist, hence the null
         * checks on return. But in the normal client config we need to
         * follow this up with a check to see if all the files really
         * exist for the non-null paths.
         */
         getPlatformFontDirs(noType1Font);

        if (fd.plainFileName != null) {
            plainFile = getPathName(fd.plainFileName);
            if (plainFile == null) {
                failure = true;
            }
        }

        if (fd.boldFileName != null) {
            boldFile = getPathName(fd.boldFileName);
            if (boldFile == null) {
                failure = true;
            }
        }

        if (fd.italicFileName != null) {
            italicFile = getPathName(fd.italicFileName);
            if (italicFile == null) {
                failure = true;
            }
        }

        if (fd.boldItalicFileName != null) {
            boldItalicFile = getPathName(fd.boldItalicFileName);
            if (boldItalicFile == null) {
                failure = true;
            }
        }

        if (failure) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Hardcoded file missing looking for " + lcName);
            }
            fd.failed = true;
            return null;
        }

        /* Some of these may be null,as not all styles have to exist */
        final String[] files = {
            plainFile, boldFile, italicFile, boldItalicFile } ;

        failure = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
             public Boolean run() {
                 for (int i=0; i<files.length; i++) {
                     if (files[i] == null) {
                         continue;
                     }
                     File f = new File(files[i]);
                     if (!f.exists()) {
                         return Boolean.TRUE;
                     }
                 }
                 return Boolean.FALSE;
             }
         });

        if (failure) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Hardcoded file missing looking for " + lcName);
            }
            fd.failed = true;
            return null;
        }

        /* If we reach here we know that we have all the files we
         * expect, so all should be fine so long as the contents
         * are what we'd expect. Now on to registering the fonts.
         * Currently this code only looks for TrueType fonts, so format
         * and rank can be specified without looking at the filename.
         */
        Font2D font = null;
        for (int f=0;f<files.length;f++) {
            if (files[f] == null) {
                continue;
            }
            PhysicalFont pf =
                registerFontFile(files[f], null,
                                 FONTFORMAT_TRUETYPE, false, Font2D.TTF_RANK);
            if (f == styleIndex) {
                font = pf;
            }
        }


        /* Two general cases need a bit more work here.
         * 1) If font is null, then it was perhaps a request for a
         * non-existent font, such as "Tahoma Italic", or a family name -
         * where family and full name of the plain font differ.
         * Fall back to finding the closest one in the family.
         * This could still fail if a client specified "Segoe" instead of
         * "Segoe UI".
         * 2) The request is of the form "MyFont Bold", style=Font.ITALIC,
         * and so we want to see if there's a Bold Italic font, or
         * "MyFamily", style=Font.BOLD, and we may have matched the plain,
         * but now need to revise that to the BOLD font.
         */
        FontFamily fontFamily = FontFamily.getFamily(fd.familyName);
        if (fontFamily != null) {
            if (font == null) {
                font = fontFamily.getFont(style);
                if (font == null) {
                    font = fontFamily.getClosestStyle(style);
                }
            } else if (style > 0 && style != font.style) {
                style |= font.style;
                font = fontFamily.getFont(style);
                if (font == null) {
                    font = fontFamily.getClosestStyle(style);
                }
            }
        }

        return font;
    }
    private synchronized HashMap<String,String> getFullNameToFileMap() {
        if (fontToFileMap == null) {

            pathDirs = getPlatformFontDirs(noType1Font);

            fontToFileMap = new HashMap<>(100);
            fontToFamilyNameMap = new HashMap<>(100);
            familyToFontListMap = new HashMap<>(50);
            populateFontFileNameMap(fontToFileMap,
                                    fontToFamilyNameMap,
                                    familyToFontListMap,
                                    Locale.ENGLISH);
            if (FontUtilities.isWindows) {
                resolveWindowsFonts();
            }
            if (FontUtilities.isLogging()) {
                logPlatformFontInfo();
            }
        }
        return fontToFileMap;
    }

    private void logPlatformFontInfo() {
        PlatformLogger logger = FontUtilities.getLogger();
        for (int i=0; i< pathDirs.length;i++) {
            logger.info("fontdir="+pathDirs[i]);
        }
        for (String keyName : fontToFileMap.keySet()) {
            logger.info("font="+keyName+" file="+ fontToFileMap.get(keyName));
        }
        for (String keyName : fontToFamilyNameMap.keySet()) {
            logger.info("font="+keyName+" family="+
                        fontToFamilyNameMap.get(keyName));
        }
        for (String keyName : familyToFontListMap.keySet()) {
            logger.info("family="+keyName+ " fonts="+
                        familyToFontListMap.get(keyName));
        }
    }

    /* Note this return list excludes logical fonts and JRE fonts */
    protected String[] getFontNamesFromPlatform() {
        if (getFullNameToFileMap().size() == 0) {
            return null;
        }
        checkForUnreferencedFontFiles();
        /* This odd code with TreeMap is used to preserve a historical
         * behaviour wrt the sorting order .. */
        ArrayList<String> fontNames = new ArrayList<>();
        for (ArrayList<String> a : familyToFontListMap.values()) {
            for (String s : a) {
                fontNames.add(s);
            }
        }
        return fontNames.toArray(STR_ARRAY);
    }

    public boolean gotFontsFromPlatform() {
        return getFullNameToFileMap().size() != 0;
    }

    public String getFileNameForFontName(String fontName) {
        String fontNameLC = fontName.toLowerCase(Locale.ENGLISH);
        return fontToFileMap.get(fontNameLC);
    }

    private PhysicalFont registerFontFile(String file) {
        if (new File(file).isAbsolute() &&
            !registeredFonts.containsKey(file)) {
            int fontFormat = FONTFORMAT_NONE;
            int fontRank = Font2D.UNKNOWN_RANK;
            if (ttFilter.accept(null, file)) {
                fontFormat = FONTFORMAT_TRUETYPE;
                fontRank = Font2D.TTF_RANK;
            } else if
                (t1Filter.accept(null, file)) {
                fontFormat = FONTFORMAT_TYPE1;
                fontRank = Font2D.TYPE1_RANK;
            }
            if (fontFormat == FONTFORMAT_NONE) {
                return null;
            }
            return registerFontFile(file, null, fontFormat, false, fontRank);
        }
        return null;
    }

    /* Used to register any font files that are found by platform APIs
     * that weren't previously found in the standard font locations.
     * the isAbsolute() check is needed since that's whats stored in the
     * set, and on windows, the fonts in the system font directory that
     * are in the fontToFileMap are just basenames. We don't want to try
     * to register those again, but we do want to register other registry
     * installed fonts.
     */
    protected void registerOtherFontFiles(HashSet<String> registeredFontFiles) {
        if (getFullNameToFileMap().size() == 0) {
            return;
        }
        for (String file : fontToFileMap.values()) {
            registerFontFile(file);
        }
    }

    public boolean
        getFamilyNamesFromPlatform(TreeMap<String,String> familyNames,
                                   Locale requestedLocale) {
        if (getFullNameToFileMap().size() == 0) {
            return false;
        }
        checkForUnreferencedFontFiles();
        for (String name : fontToFamilyNameMap.values()) {
            familyNames.put(name.toLowerCase(requestedLocale), name);
        }
        return true;
    }

    /* Path may be absolute or a base file name relative to one of
     * the platform font directories
     */
    private String getPathName(final String s) {
        File f = new File(s);
        if (f.isAbsolute()) {
            return s;
        } else if (pathDirs.length==1) {
            return pathDirs[0] + File.separator + s;
        } else {
            @SuppressWarnings("removal")
            String path = AccessController.doPrivileged(
                 new PrivilegedAction<String>() {
                     public String run() {
                         for (int p = 0; p < pathDirs.length; p++) {
                             File f = new File(pathDirs[p] +File.separator+ s);
                             if (f.exists()) {
                                 return f.getAbsolutePath();
                             }
                         }
                         return null;
                     }
                });
            if (path != null) {
                return path;
            }
        }
        return s; // shouldn't happen, but harmless
    }

    /* lcName is required to be lower case for use as a key.
     * lcName may be a full name, or a family name, and style may
     * be specified in addition to either of these. So be sure to
     * get the right one. Since an app *could* ask for "Foo Regular"
     * and later ask for "Foo Italic", if we don't register all the
     * styles, then logic in findFont2D may try to style the original
     * so we register the entire family if we get a match here.
     * This is still a big win because this code is invoked where
     * otherwise we would register all fonts.
     * It's also useful for the case where "Foo Bold" was specified with
     * style Font.ITALIC, as we would want in that case to try to return
     * "Foo Bold Italic" if it exists, and it is only by locating "Foo Bold"
     * and opening it that we really "know" it's Bold, and can look for
     * a font that supports that and the italic style.
     * The code in here is not overtly windows-specific but in fact it
     * is unlikely to be useful as is on other platforms. It is maintained
     * in this shared source file to be close to its sole client and
     * because so much of the logic is intertwined with the logic in
     * findFont2D.
     */
    private Font2D findFontFromPlatform(String lcName, int style) {
        if (getFullNameToFileMap().size() == 0) {
            return null;
        }

        ArrayList<String> family = null;
        String fontFile = null;
        String familyName = fontToFamilyNameMap.get(lcName);
        if (familyName != null) {
            fontFile = fontToFileMap.get(lcName);
            family = familyToFontListMap.get
                (familyName.toLowerCase(Locale.ENGLISH));
        } else {
            family = familyToFontListMap.get(lcName); // is lcName is a family?
            if (family != null && family.size() > 0) {
                String lcFontName = family.get(0).toLowerCase(Locale.ENGLISH);
                if (lcFontName != null) {
                    familyName = fontToFamilyNameMap.get(lcFontName);
                }
            }
        }
        if (family == null || familyName == null) {
            return null;
        }
        String [] fontList = family.toArray(STR_ARRAY);
        if (fontList.length == 0) {
            return null;
        }

        /* first check that for every font in this family we can find
         * a font file. The specific reason for doing this is that
         * in at least one case on Windows a font has the face name "David"
         * but the registry entry is "David Regular". That is the "unique"
         * name of the font but in other cases the registry contains the
         * "full" name. See the specifications of name ids 3 and 4 in the
         * TrueType 'name' table.
         * In general this could cause a problem that we fail to register
         * if we all members of a family that we may end up mapping to
         * the wrong font member: eg return Bold when Plain is needed.
         */
        for (int f=0;f<fontList.length;f++) {
            String fontNameLC = fontList[f].toLowerCase(Locale.ENGLISH);
            String fileName = fontToFileMap.get(fontNameLC);
            if (fileName == null) {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Platform lookup : No file for font " +
                                    fontList[f] + " in family " +familyName);
                }
                return null;
            }
        }

        /* Currently this code only looks for TrueType fonts, so format
         * and rank can be specified without looking at the filename.
         */
        PhysicalFont physicalFont = null;
        if (fontFile != null) {
            physicalFont = registerFontFile(getPathName(fontFile), null,
                                            FONTFORMAT_TRUETYPE, false,
                                            Font2D.TTF_RANK);
        }
        /* Register all fonts in this family. */
        for (int f=0;f<fontList.length;f++) {
            String fontNameLC = fontList[f].toLowerCase(Locale.ENGLISH);
            String fileName = fontToFileMap.get(fontNameLC);
            if (fontFile != null && fontFile.equals(fileName)) {
                continue;
            }
            /* Currently this code only looks for TrueType fonts, so format
             * and rank can be specified without looking at the filename.
             */
            registerFontFile(getPathName(fileName), null,
                             FONTFORMAT_TRUETYPE, false, Font2D.TTF_RANK);
        }

        Font2D font = null;
        FontFamily fontFamily = FontFamily.getFamily(familyName);
        /* Handle case where request "MyFont Bold", style=Font.ITALIC */
        if (physicalFont != null) {
            style |= physicalFont.style;
        }
        if (fontFamily != null) {
            font = fontFamily.getFont(style);
            if (font == null) {
                font = fontFamily.getClosestStyle(style);
            }
        }
        return font;
    }

    private ConcurrentHashMap<String, Font2D> fontNameCache =
        new ConcurrentHashMap<>();

    /*
     * The client supplies a name and a style.
     * The name could be a family name, or a full name.
     * A font may exist with the specified style, or it may
     * exist only in some other style. For non-native fonts the scaler
     * may be able to emulate the required style.
     */
    public Font2D findFont2D(String name, int style, int fallback) {
        if (name == null) return null;
        String lowerCaseName = name.toLowerCase(Locale.ENGLISH);
        String mapName = lowerCaseName + dotStyleStr(style);

        /* If preferLocaleFonts() or preferProportionalFonts() has been
         * called we may be using an alternate set of composite fonts in this
         * app context. The presence of a pre-built name map indicates whether
         * this is so, and gives access to the alternate composite for the
         * name.
         */
        Font2D font = fontNameCache.get(mapName);
        if (font != null) {
            return font;
        }
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Search for font: " + name);
        }

        // The check below is just so that the bitmap fonts being set by
        // AWT and Swing thru the desktop properties do not trigger the
        // the load fonts case. The two bitmap fonts are now mapped to
        // appropriate equivalents for serif and sansserif.
        // Note that the cost of this comparison is only for the first
        // call until the map is filled.
        if (FontUtilities.isWindows) {
            if (lowerCaseName.equals("ms sans serif")) {
                name = "sansserif";
            } else if (lowerCaseName.equals("ms serif")) {
                name = "serif";
            }
        }

        /* This isn't intended to support a client passing in the
         * string default, but if a client passes in null for the name
         * the java.awt.Font class internally substitutes this name.
         * So we need to recognise it here to prevent a loadFonts
         * on the unrecognised name. The only potential problem with
         * this is it would hide any real font called "default"!
         * But that seems like a potential problem we can ignore for now.
         */
        if (lowerCaseName.equals("default")) {
            name = "dialog";
        }

        /* First see if its a family name. */
        FontFamily family = FontFamily.getFamily(name);
        if (family != null) {
            font = family.getFontWithExactStyleMatch(style);
            if (font == null) {
                font = findDeferredFont(name, style);
            }
            if (font == null) {
                font = findFontFromPlatform(lowerCaseName, style);
            }
            if (font == null) {
                font = family.getFont(style);
            }
            if (font == null) {
                font = family.getClosestStyle(style);
            }
            if (font != null) {
                fontNameCache.put(mapName, font);
                return font;
            }
        }

        /* If it wasn't a family name, it should be a full name of
         * either a composite, or a physical font
         */
        font = fullNameToFont.get(lowerCaseName);
        if (font != null) {
            /* Check that the requested style matches the matched font's style.
             * But also match style automatically if the requested style is
             * "plain". This because the existing behaviour is that the fonts
             * listed via getAllFonts etc always list their style as PLAIN.
             * This does lead to non-commutative behaviours where you might
             * start with "Lucida Sans Regular" and ask for a BOLD version
             * and get "Lucida Sans DemiBold" but if you ask for the PLAIN
             * style of "Lucida Sans DemiBold" you get "Lucida Sans DemiBold".
             * This consistent however with what happens if you have a bold
             * version of a font and no plain version exists - alg. styling
             * doesn't "unbolden" the font.
             */
            if (font.style == style || style == Font.PLAIN) {
                fontNameCache.put(mapName, font);
                return font;
            } else {
                /* If it was a full name like "Lucida Sans Regular", but
                 * the style requested is "bold", then we want to see if
                 * there's the appropriate match against another font in
                 * that family before trying to load all fonts, or applying a
                 * algorithmic styling
                 */
                family = FontFamily.getFamily(font.getFamilyName(null));
                if (family != null) {
                    Font2D familyFont = family.getFont(style|font.style);
                    /* We exactly matched the requested style, use it! */
                    if (familyFont != null) {
                        fontNameCache.put(mapName, familyFont);
                        return familyFont;
                    } else {
                        /* This next call is designed to support the case
                         * where bold italic is requested, and if we must
                         * style, then base it on either bold or italic -
                         * not on plain!
                         */
                        familyFont = family.getClosestStyle(style|font.style);
                        if (familyFont != null) {
                            /* The next check is perhaps one
                             * that shouldn't be done. ie if we get this
                             * far we have probably as close a match as we
                             * are going to get. We could load all fonts to
                             * see if somehow some parts of the family are
                             * loaded but not all of it.
                             */
                            if (familyFont.canDoStyle(style|font.style)) {
                                fontNameCache.put(mapName, familyFont);
                                return familyFont;
                            }
                        }
                    }
                }
            }
        }

        if (FontUtilities.isWindows) {

            font = findFontFromPlatformMap(lowerCaseName, style);
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("findFontFromPlatformMap returned " + font);
            }

            if (font != null) {
                fontNameCache.put(mapName, font);
                return font;
            }
            /* Don't want Windows to return a font from C:\Windows\Fonts
             * if someone has installed a font with the same name
             * in the JRE.
             */
            if (deferredFontFiles.size() > 0) {
                font = findJREDeferredFont(lowerCaseName, style);
                if (font != null) {
                    fontNameCache.put(mapName, font);
                    return font;
                }
            }
            font = findFontFromPlatform(lowerCaseName, style);
            if (font != null) {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Found font via platform API for request:\"" +
                                    name + "\":, style="+style+
                                    " found font: " + font);
                }
                fontNameCache.put(mapName, font);
                return font;
            }
        }

        /* If reach here and no match has been located, then if there are
         * uninitialised deferred fonts, load as many of those as needed
         * to find the deferred font. If none is found through that
         * search continue on.
         * There is possibly a minor issue when more than one
         * deferred font implements the same font face. Since deferred
         * fonts are only those in font configuration files, this is a
         * controlled situation, the known case being Solaris euro_fonts
         * versions of Arial, Times New Roman, Courier New. However
         * the larger font will transparently replace the smaller one
         *  - see addToFontList() - when it is needed by the composite font.
         */
        if (deferredFontFiles.size() > 0) {
            font = findDeferredFont(name, style);
            if (font != null) {
                fontNameCache.put(mapName, font);
                return font;
            }
        }

        /* We check for application registered fonts before
         * explicitly loading all fonts as if necessary the registration
         * code will have done so anyway. And we don't want to needlessly
         * load the actual files for all fonts.
         * Just as for installed fonts we check for family before fullname.
         * We do not add these fonts to fontNameCache for the
         * app context case which eliminates the overhead of a per context
         * cache for these.
         */

        if (fontsAreRegistered) {
            Hashtable<String, FontFamily> familyTable = createdByFamilyName;
            Hashtable<String, Font2D> nameTable = createdByFullName;

            family = familyTable.get(lowerCaseName);
            if (family != null) {
                font = family.getFontWithExactStyleMatch(style);
                if (font == null) {
                    font = family.getFont(style);
                }
                if (font == null) {
                    font = family.getClosestStyle(style);
                }
                if (font != null) {
                    if (fontsAreRegistered) {
                        fontNameCache.put(mapName, font);
                    }
                    return font;
                }
            }
            font = nameTable.get(lowerCaseName);
            if (font != null) {
                if (fontsAreRegistered) {
                    fontNameCache.put(mapName, font);
                }
                return font;
            }
        }

        /* If reach here and no match has been located, then if all fonts
         * are not yet loaded, do so, and then recurse.
         */
        if (!loadedAllFonts) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Load fonts looking for:" + name);
            }
            loadFonts();
            loadedAllFonts = true;
            return findFont2D(name, style, fallback);
        }

        if (!loadedAllFontFiles) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Load font files looking for:" + name);
            }
            loadFontFiles();
            loadedAllFontFiles = true;
            return findFont2D(name, style, fallback);
        }

        /* The primary name is the locale default - ie not US/English but
         * whatever is the default in this locale. This is the way it always
         * has been but may be surprising to some developers if "Arial Regular"
         * were hard-coded in their app and yet "Arial Regular" was not the
         * default name. Fortunately for them, as a consequence of the JDK
         * supporting returning names and family names for arbitrary locales,
         * we also need to support searching all localised names for a match.
         * But because this case of the name used to reference a font is not
         * the same as the default for this locale is rare, it makes sense to
         * search a much shorter list of default locale names and only go to
         * a longer list of names in the event that no match was found.
         * So add here code which searches localised names too.
         * As in 1.4.x this happens only after loading all fonts, which
         * is probably the right order.
         */
        if ((font = findFont2DAllLocales(name, style)) != null) {
            fontNameCache.put(mapName, font);
            return font;
        }

        /* Perhaps its a "compatibility" name - timesroman, helvetica,
         * or courier, which 1.0 apps used for logical fonts.
         * We look for these "late" after a loadFonts as we must not
         * hide real fonts of these names.
         * Map these appropriately:
         * On windows this means according to the rules specified by the
         * FontConfiguration : do it only for encoding==Cp1252
         *
         * REMIND: this is something we plan to remove.
         */
        if (FontUtilities.isWindows) {
            String compatName =
                getFontConfiguration().getFallbackFamilyName(name, null);
            if (compatName != null) {
                font = findFont2D(compatName, style, fallback);
                fontNameCache.put(mapName, font);
                return font;
            }
        } else if (lowerCaseName.equals("timesroman")) {
            font = findFont2D("serif", style, fallback);
            fontNameCache.put(mapName, font);
            return font;
        } else if (lowerCaseName.equals("helvetica")) {
            font = findFont2D("sansserif", style, fallback);
            fontNameCache.put(mapName, font);
            return font;
        } else if (lowerCaseName.equals("courier")) {
            font = findFont2D("monospaced", style, fallback);
            fontNameCache.put(mapName, font);
            return font;
        }
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("No font found for:" + name);
        }

        switch (fallback) {
        case PHYSICAL_FALLBACK: return getDefaultPhysicalFont();
        case LOGICAL_FALLBACK: return getDefaultLogicalFont(style);
        default: return null;
        }
    }

    /*
     * Workaround for apps which are dependent on a font metrics bug
     * in JDK 1.1. This is an unsupported win32 private setting.
     * Left in for a customer - do not remove.
     */
    public boolean usePlatformFontMetrics() {
        return usePlatformFontMetrics;
    }

    public int getNumFonts() {
        return physicalFonts.size()+maxCompFont;
    }

    private static boolean fontSupportsEncoding(Font font, String encoding) {
        return FontUtilities.getFont2D(font).supportsEncoding(encoding);
    }

    protected abstract String getFontPath(boolean noType1Fonts);

    Thread fileCloser = null;
    Vector<File> tmpFontFiles = null;

    private int createdFontCount = 0;

    @SuppressWarnings("removal")
    public Font2D[] createFont2D(File fontFile, int fontFormat, boolean all,
                                 boolean isCopy, CreatedFontTracker tracker)
    throws FontFormatException {

        List<Font2D> fList = new ArrayList<>();
        int cnt = 1;
        String fontFilePath = fontFile.getPath();
        FileFont font2D = null;
        final File fFile = fontFile;
        final CreatedFontTracker _tracker = tracker;
        boolean weakRefs = false;
        int maxStrikes = 0;
        synchronized (this) {
            if (createdFontCount < maxSoftRefCnt) {
                createdFontCount++;
            } else {
                  weakRefs = true;
                      maxStrikes = 10;
            }
        }
        try {
            switch (fontFormat) {
            case Font.TRUETYPE_FONT:
                font2D = new TrueTypeFont(fontFilePath, null, 0, true);
                font2D.setUseWeakRefs(weakRefs, maxStrikes);
                fList.add(font2D);
                if (!all) {
                    break;
                }
                cnt = ((TrueTypeFont)font2D).getFontCount();
                int index = 1;
                while (index < cnt) {
                    font2D = new TrueTypeFont(fontFilePath, null, index++, true);
                    font2D.setUseWeakRefs(weakRefs, maxStrikes);
                    fList.add(font2D);
                }
                break;
            case Font.TYPE1_FONT:
                font2D = new Type1Font(fontFilePath, null, isCopy);
                font2D.setUseWeakRefs(weakRefs, maxStrikes);
                fList.add(font2D);
                break;
            default:
                throw new FontFormatException("Unrecognised Font Format");
            }
        } catch (FontFormatException e) {
            if (isCopy) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        if (_tracker != null) {
                            _tracker.subBytes((int)fFile.length());
                        }
                        fFile.delete();
                        return null;
                    }
                });
            }
            throw(e);
        }
        if (isCopy) {
            FileFont.setFileToRemove(fList, fontFile, cnt, tracker);
            synchronized (FontManager.class) {

                if (tmpFontFiles == null) {
                    tmpFontFiles = new Vector<File>();
                }
                tmpFontFiles.add(fontFile);

                if (fileCloser == null) {
                    final Runnable fileCloserRunnable = new Runnable() {
                        public void run() {
                            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                public Void run() {
                                    for (int i = 0;i < CHANNELPOOLSIZE; i++) {
                                        if (fontFileCache[i] != null) {
                                            try {
                                                fontFileCache[i].close();
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                    if (tmpFontFiles != null) {
                                        File[] files = new File[tmpFontFiles.size()];
                                        files = tmpFontFiles.toArray(files);
                                        for (int f=0; f<files.length;f++) {
                                            try {
                                                files[f].delete();
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                    return null;
                                }
                            });
                        }
                    };
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        ThreadGroup rootTG = ThreadGroupUtils.getRootThreadGroup();
                        fileCloser = new Thread(rootTG, fileCloserRunnable,
                                                "FileCloser", 0, false);
                        fileCloser.setContextClassLoader(null);
                        Runtime.getRuntime().addShutdownHook(fileCloser);
                        return null;
                    });
                }
            }
        }
        return fList.toArray(new Font2D[0]);
    }

    /* remind: used in X11GraphicsEnvironment and called often enough
     * that we ought to obsolete this code
     */
    public synchronized String getFullNameByFileName(String fileName) {
        PhysicalFont[] physFonts = getPhysicalFonts();
        for (int i=0;i<physFonts.length;i++) {
            if (physFonts[i].platName.equals(fileName)) {
                return (physFonts[i].getFontName(null));
            }
        }
        return null;
    }

    /*
     * This is called when font is determined to be invalid/bad.
     * It designed to be called (for example) by the font scaler
     * when in processing a font file it is discovered to be incorrect.
     * This is different than the case where fonts are discovered to
     * be incorrect during initial verification, as such fonts are
     * never registered.
     * Handles to this font held are re-directed to a default font.
     * This default may not be an ideal substitute buts it better than
     * crashing This code assumes a PhysicalFont parameter as it doesn't
     * make sense for a Composite to be "bad".
     */
    public synchronized void deRegisterBadFont(Font2D font2D) {
        if (!(font2D instanceof PhysicalFont)) {
            /* We should never reach here, but just in case */
            return;
        } else {
            if (FontUtilities.isLogging()) {
                FontUtilities.logSevere("Deregister bad font: " + font2D);
            }
            replaceFont((PhysicalFont)font2D, getDefaultPhysicalFont());
        }
    }

    /*
     * This encapsulates all the work that needs to be done when a
     * Font2D is replaced by a different Font2D.
     */
    public synchronized void replaceFont(PhysicalFont oldFont,
                                         PhysicalFont newFont) {

        if (oldFont.handle.font2D != oldFont) {
            /* already done */
            return;
        }

        /* If we try to replace the font with itself, that won't work,
         * so pick any alternative physical font
         */
        if (oldFont == newFont) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logSevere("Can't replace bad font with itself " + oldFont);
            }
            PhysicalFont[] physFonts = getPhysicalFonts();
            for (int i=0; i<physFonts.length;i++) {
                if (physFonts[i] != newFont) {
                    newFont = physFonts[i];
                    break;
                }
            }
            if (oldFont == newFont) {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logSevere("This is bad. No good physicalFonts found.");
                }
                return;
            }
        }

        /* eliminate references to this font, so it won't be located
         * by future callers, and will be eligible for GC when all
         * references are removed
         */
        oldFont.handle.font2D = newFont;
        physicalFonts.remove(oldFont.fullName);
        fullNameToFont.remove(oldFont.fullName.toLowerCase(Locale.ENGLISH));
        FontFamily.remove(oldFont);
        if (localeFullNamesToFont != null) {
            Map.Entry<?, ?>[] mapEntries = localeFullNamesToFont.entrySet().
                toArray(new Map.Entry<?, ?>[0]);
            /* Should I be replacing these, or just I just remove
             * the names from the map?
             */
            for (int i=0; i<mapEntries.length;i++) {
                if (mapEntries[i].getValue() == oldFont) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, PhysicalFont> tmp = (Map.Entry<String, PhysicalFont>)mapEntries[i];
                        tmp.setValue(newFont);
                    } catch (Exception e) {
                        /* some maps don't support this operation.
                         * In this case just give up and remove the entry.
                         */
                        localeFullNamesToFont.remove(mapEntries[i].getKey());
                    }
                }
            }
        }

        for (int i=0; i<maxCompFont; i++) {
            /* Deferred initialization of composites shouldn't be
             * a problem for this case, since a font must have been
             * initialised to be discovered to be bad.
             * Some JRE composites on Solaris use two versions of the same
             * font. The replaced font isn't bad, just "smaller" so there's
             * no need to make the slot point to the new font.
             * Since composites have a direct reference to the Font2D (not
             * via a handle) making this substitution is not safe and could
             * cause an additional problem and so this substitution is
             * warranted only when a font is truly "bad" and could cause
             * a crash. So we now replace it only if its being substituted
             * with some font other than a fontconfig rank font
             * Since in practice a substitution will have the same rank
             * this may never happen, but the code is safer even if its
             * also now a no-op.
             * The only obvious "glitch" from this stems from the current
             * implementation that when asked for the number of glyphs in a
             * composite it lies and returns the number in slot 0 because
             * composite glyphs aren't contiguous. Since we live with that
             * we can live with the glitch that depending on how it was
             * initialised a composite may return different values for this.
             * Fixing the issues with composite glyph ids is tricky as
             * there are exclusion ranges and unlike other fonts even the
             * true "numGlyphs" isn't a contiguous range. Likely the only
             * solution is an API that returns an array of glyph ranges
             * which takes precedence over the existing API. That might
             * also need to address excluding ranges which represent a
             * code point supported by an earlier component.
             */
            if (newFont.getRank() > Font2D.FONT_CONFIG_RANK) {
                compFonts[i].replaceComponentFont(oldFont, newFont);
            }
        }
    }

    private synchronized void loadLocaleNames() {
        if (localeFullNamesToFont != null) {
            return;
        }
        localeFullNamesToFont = new HashMap<>();
        Font2D[] fonts = getRegisteredFonts();
        for (int i=0; i<fonts.length; i++) {
            if (fonts[i] instanceof TrueTypeFont) {
                TrueTypeFont ttf = (TrueTypeFont)fonts[i];
                String[] fullNames = ttf.getAllFullNames();
                for (int n=0; n<fullNames.length; n++) {
                    localeFullNamesToFont.put(fullNames[n], ttf);
                }
                FontFamily family = FontFamily.getFamily(ttf.familyName);
                if (family != null) {
                    FontFamily.addLocaleNames(family, ttf.getAllFamilyNames());
                }
            }
        }
    }

    /* This replicate the core logic of findFont2D but operates on
     * all the locale names. This hasn't been merged into findFont2D to
     * keep the logic simpler and reduce overhead, since this case is
     * almost never used. The main case in which it is called is when
     * a bogus font name is used and we need to check all possible names
     * before returning the default case.
     */
    private Font2D findFont2DAllLocales(String name, int style) {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Searching localised font names for:" + name);
        }

        /* If reach here and no match has been located, then if we have
         * not yet built the map of localeFullNamesToFont for TT fonts, do so
         * now. This method must be called after all fonts have been loaded.
         */
        if (localeFullNamesToFont == null) {
            loadLocaleNames();
        }
        String lowerCaseName = name.toLowerCase();
        Font2D font = null;

        /* First see if its a family name. */
        FontFamily family = FontFamily.getLocaleFamily(lowerCaseName);
        if (family != null) {
          font = family.getFont(style);
          if (font == null) {
            font = family.getClosestStyle(style);
          }
          if (font != null) {
              return font;
          }
        }

        /* If it wasn't a family name, it should be a full name. */
        synchronized (this) {
            font = localeFullNamesToFont.get(name);
        }
        if (font != null) {
            if (font.style == style || style == Font.PLAIN) {
                return font;
            } else {
                family = FontFamily.getFamily(font.getFamilyName(null));
                if (family != null) {
                    Font2D familyFont = family.getFont(style);
                    /* We exactly matched the requested style, use it! */
                    if (familyFont != null) {
                        return familyFont;
                    } else {
                        familyFont = family.getClosestStyle(style);
                        if (familyFont != null) {
                            /* The next check is perhaps one
                             * that shouldn't be done. ie if we get this
                             * far we have probably as close a match as we
                             * are going to get. We could load all fonts to
                             * see if somehow some parts of the family are
                             * loaded but not all of it.
                             * This check is commented out for now.
                             */
                            if (!familyFont.canDoStyle(style)) {
                                familyFont = null;
                            }
                            return familyFont;
                        }
                    }
                }
            }
        }
        return font;
    }

    /* Supporting "alternate" composite fonts on 2D graphics objects
     * is accessed by the application by calling methods on the local
     * GraphicsEnvironment. The overall implementation is described
     * in one place, here, since otherwise the implementation is spread
     * around it may be difficult to track.
     * The methods below call into SunGraphicsEnvironment which creates a
     * new FontConfiguration instance. The FontConfiguration class,
     * and its platform sub-classes are updated to take parameters requesting
     * these behaviours. This is then used to create new composite font
     * instances. Since this calls the initCompositeFont method in
     * SunGraphicsEnvironment it performs the same initialization as is
     * performed normally. There may be some duplication of effort, but
     * that code is already written to be able to perform properly if called
     * to duplicate work. The main difference is that if we detect we are
     * running in an applet/browser/Java plugin environment these new fonts
     * are not placed in the "default" maps but into an AppContext instance.
     * The font lookup mechanism in java.awt.Font.getFont2D() is also updated
     * so that look-up for composite fonts will in that case always
     * do a lookup rather than returning a cached result.
     * This is inefficient but necessary else singleton java.awt.Font
     * instances would not retrieve the correct Font2D for the appcontext.
     * sun.font.FontManager.findFont2D is also updated to that it uses
     * a name map cache specific to that appcontext.
     *
     * Getting an AppContext is expensive, so there is a global variable
     * that records whether these methods have ever been called and can
     * avoid the expense for almost all applications. Once the correct
     * CompositeFont is associated with the Font, everything should work
     * through existing mechanisms.
     * A special case is that GraphicsEnvironment.getAllFonts() must
     * return an AppContext specific list.
     *
     * Calling the methods below is "heavyweight" but it is expected that
     * these methods will be called very rarely.
     *
     * If _usingAlternateComposites is true, we are not in an "applet"
     * environment and the (single) application has selected
     * an alternate composite font behaviour.
     *
     * - Printing: The implementation delegates logical fonts to an AWT
     * mechanism which cannot use these alternate configurations.
     * We can detect that alternate fonts are in use and back-off to 2D, but
     * that uses outlines. Much of this can be fixed with additional work
     * but that may have to wait. The results should be correct, just not
     * optimal.
     */
    private boolean _usingAlternateComposites = false;

    private static boolean gAltJAFont = false;
    private boolean gLocalePref = false;
    private boolean gPropPref = false;

    /* Its used by the FontMetrics caching code which in such
     * a case cannot retrieve a cached metrics solely on the basis of
     * the Font.equals() method since it needs to also check if the Font2D
     * is the same.
     * We also use non-standard composites for Swing native L&F fonts on
     * Windows. In that case the policy is that the metrics reported are
     * based solely on the physical font in the first slot which is the
     * visible java.awt.Font. So in that case the metrics cache which tests
     * the Font does what we want. In the near future when we expand the GTK
     * logical font definitions we may need to revisit this if GTK reports
     * combined metrics instead. For now though this test can be simple.
     */
    public boolean usingAlternateCompositeFonts() {
        return _usingAlternateComposites;
    }

    /* Modifies the behaviour of a subsequent call to preferLocaleFonts()
     * to use Mincho instead of Gothic for dialoginput in JA locales
     * on windows. Not needed on other platforms.
     */
    public synchronized void useAlternateFontforJALocales() {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Entered useAlternateFontforJALocales().");
        }

        if (!FontUtilities.isWindows) {
            return;
        }
        gAltJAFont = true;
    }

    public boolean usingAlternateFontforJALocales() {
        return gAltJAFont;
    }

    public synchronized void preferLocaleFonts() {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Entered preferLocaleFonts().");
        }

        /* Test if re-ordering will have any effect */
        if (!FontConfiguration.willReorderForStartupLocale()) {
            return;
        }
        if (gLocalePref == true) {
            return;
        }
        gLocalePref = true;
        createCompositeFonts(fontNameCache, gLocalePref, gPropPref);
        _usingAlternateComposites = true;
    }

    public synchronized void preferProportionalFonts() {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Entered preferProportionalFonts().");
        }

        /* If no proportional fonts are configured, there's no need
         * to take any action.
         */
        if (!FontConfiguration.hasMonoToPropMap()) {
            return;
        }
        if (gPropPref == true) {
            return;
        }
        gPropPref = true;
        createCompositeFonts(fontNameCache, gLocalePref, gPropPref);
        _usingAlternateComposites = true;
    }

    private static HashSet<String> installedNames = null;
    private static HashSet<String> getInstalledNames() {
        if (installedNames == null) {
           Locale l = getSystemStartupLocale();
           SunFontManager fontManager = SunFontManager.getInstance();
           String[] installedFamilies =
               fontManager.getInstalledFontFamilyNames(l);
           Font[] installedFonts = fontManager.getAllInstalledFonts();
           HashSet<String> names = new HashSet<>();
           for (int i=0; i<installedFamilies.length; i++) {
               names.add(installedFamilies[i].toLowerCase(l));
           }
           for (int i=0; i<installedFonts.length; i++) {
               names.add(installedFonts[i].getFontName(l).toLowerCase(l));
           }
           installedNames = names;
        }
        return installedNames;
    }

    private static final Object regFamilyLock  = new Object();
    private Hashtable<String,FontFamily> createdByFamilyName;
    private Hashtable<String,Font2D>     createdByFullName;
    private boolean fontsAreRegistered = false;

    public boolean registerFont(Font font) {
        /* This method should not be called with "null".
         * It is the caller's responsibility to ensure that.
         */
        if (font == null) {
            return false;
        }

        /* Initialise these objects only once we start to use this API */
        synchronized (regFamilyLock) {
            if (createdByFamilyName == null) {
                createdByFamilyName = new Hashtable<String,FontFamily>();
                createdByFullName = new Hashtable<String,Font2D>();
            }
        }

        if (! FontAccess.getFontAccess().isCreatedFont(font)) {
            return false;
        }
        /* We want to ensure that this font cannot override existing
         * installed fonts. Check these conditions :
         * - family name is not that of an installed font
         * - full name is not that of an installed font
         * - family name is not the same as the full name of an installed font
         * - full name is not the same as the family name of an installed font
         * The last two of these may initially look odd but the reason is
         * that (unfortunately) Font constructors do not distinuguish these.
         * An extreme example of such a problem would be a font which has
         * family name "Dialog.Plain" and full name of "Dialog".
         * The one arguably overly stringent restriction here is that if an
         * application wants to supply a new member of an existing family
         * It will get rejected. But since the JRE can perform synthetic
         * styling in many cases its not necessary.
         * We don't apply the same logic to registered fonts. If apps want
         * to do this lets assume they have a reason. It won't cause problems
         * except for themselves.
         */
        HashSet<String> names = getInstalledNames();
        Locale l = getSystemStartupLocale();
        String familyName = font.getFamily(l).toLowerCase();
        String fullName = font.getFontName(l).toLowerCase();
        if (names.contains(familyName) || names.contains(fullName)) {
            return false;
        }

        /* Checks passed, now register the font */
        Hashtable<String, FontFamily> familyTable = createdByFamilyName;
        Hashtable<String, Font2D> fullNameTable = createdByFullName;
        fontsAreRegistered = true;

        /* Create the FontFamily and add font to the tables */
        Font2D font2D = FontUtilities.getFont2D(font);
        int style = font2D.getStyle();
        FontFamily family = familyTable.get(familyName);
        if (family == null) {
            family = new FontFamily(font.getFamily(l));
            familyTable.put(familyName, family);
        }
        /* Remove name cache entries if not using app contexts.
         * To accommodate a case where code may have registered first a plain
         * family member and then used it and is now registering a bold family
         * member, we need to remove all members of the family, so that the
         * new style can get picked up rather than continuing to synthesise.
         */
        if (fontsAreRegistered) {
            removeFromCache(family.getFont(Font.PLAIN));
            removeFromCache(family.getFont(Font.BOLD));
            removeFromCache(family.getFont(Font.ITALIC));
            removeFromCache(family.getFont(Font.BOLD|Font.ITALIC));
            removeFromCache(fullNameTable.get(fullName));
        }
        family.setFont(font2D, style);
        fullNameTable.put(fullName, font2D);
        return true;
    }

    /* Remove from the name cache all references to the Font2D */
    private void removeFromCache(Font2D font) {
        if (font == null) {
            return;
        }
        String[] keys = fontNameCache.keySet().toArray(STR_ARRAY);
        for (int k=0; k<keys.length;k++) {
            if (fontNameCache.get(keys[k]) == font) {
                fontNameCache.remove(keys[k]);
            }
        }
    }

    // It may look odd to use TreeMap but its more convenient to the caller.
    public TreeMap<String, String> getCreatedFontFamilyNames() {

        Hashtable<String,FontFamily> familyTable;
        if (fontsAreRegistered) {
            familyTable = createdByFamilyName;
        } else {
            return null;
        }

        Locale l = getSystemStartupLocale();
        synchronized (familyTable) {
            TreeMap<String, String> map = new TreeMap<String, String>();
            for (FontFamily f : familyTable.values()) {
                Font2D font2D = f.getFont(Font.PLAIN);
                if (font2D == null) {
                    font2D = f.getClosestStyle(Font.PLAIN);
                }
                String name = font2D.getFamilyName(l);
                map.put(name.toLowerCase(l), name);
            }
            return map;
        }
    }

    public Font[] getCreatedFonts() {

        Hashtable<String,Font2D> nameTable;
        if (fontsAreRegistered) {
            nameTable = createdByFullName;
        } else {
            return null;
        }

        Locale l = getSystemStartupLocale();
        synchronized (nameTable) {
            Font[] fonts = new Font[nameTable.size()];
            int i=0;
            for (Font2D font2D : nameTable.values()) {
                fonts[i++] = new Font(font2D.getFontName(l), Font.PLAIN, 1);
            }
            return fonts;
        }
    }


    protected String[] getPlatformFontDirs(boolean noType1Fonts) {

        /* First check if we already initialised path dirs */
        if (pathDirs != null) {
            return pathDirs;
        }

        String path = getPlatformFontPath(noType1Fonts);
        StringTokenizer parser =
            new StringTokenizer(path, File.pathSeparator);
        ArrayList<String> pathList = new ArrayList<>();
        try {
            while (parser.hasMoreTokens()) {
                pathList.add(parser.nextToken());
            }
        } catch (NoSuchElementException e) {
        }
        pathDirs = pathList.toArray(new String[0]);
        return pathDirs;
    }

    /**
     * Returns an array of two strings. The first element is the
     * name of the font. The second element is the file name.
     */
    protected abstract String[] getDefaultPlatformFont();

    // Begin: Refactored from SunGraphicsEnviroment.

    /*
     * helper function for registerFonts
     */
    private void addDirFonts(String dirName, File dirFile,
                             FilenameFilter filter,
                             int fontFormat, boolean useJavaRasterizer,
                             int fontRank,
                             boolean defer, boolean resolveSymLinks) {
        String[] ls = dirFile.list(filter);
        if (ls == null || ls.length == 0) {
            return;
        }
        String[] fontNames = new String[ls.length];
        String[][] nativeNames = new String[ls.length][];
        int fontCount = 0;

        for (int i=0; i < ls.length; i++ ) {
            File theFile = new File(dirFile, ls[i]);
            String fullName = null;
            if (resolveSymLinks) {
                try {
                    fullName = theFile.getCanonicalPath();
                } catch (IOException e) {
                }
            }
            if (fullName == null) {
                fullName = dirName + File.separator + ls[i];
            }

            // REMIND: case compare depends on platform
            if (registeredFontFiles.contains(fullName)) {
                continue;
            }

            if (badFonts != null && badFonts.contains(fullName)) {
                if (FontUtilities.debugFonts()) {
                    FontUtilities.logWarning("skip bad font " + fullName);
                }
                continue; // skip this font file.
            }

            registeredFontFiles.add(fullName);

            if (FontUtilities.debugFonts()
                && FontUtilities.getLogger().isLoggable(PlatformLogger.Level.INFO)) {
                String message = "Registering font " + fullName;
                String[] natNames = getNativeNames(fullName, null);
                if (natNames == null) {
                    message += " with no native name";
                } else {
                    message += " with native name(s) " + natNames[0];
                    for (int nn = 1; nn < natNames.length; nn++) {
                        message += ", " + natNames[nn];
                    }
                }
                FontUtilities.logInfo(message);
            }
            fontNames[fontCount] = fullName;
            nativeNames[fontCount++] = getNativeNames(fullName, null);
        }
        registerFonts(fontNames, nativeNames, fontCount, fontFormat,
                         useJavaRasterizer, fontRank, defer);
        return;
    }

    protected String[] getNativeNames(String fontFileName,
                                      String platformName) {
        return null;
    }

    /**
     * Returns a file name for the physical font represented by this platform
     * font name. The default implementation tries to obtain the file name
     * from the font configuration.
     * Subclasses may override to provide information from other sources.
     */
    protected String getFileNameFromPlatformName(String platformFontName) {
        return fontConfig.getFileNameFromPlatformName(platformFontName);
    }

    /**
     * Return the default font configuration.
     */
    public FontConfiguration getFontConfiguration() {
        return fontConfig;
    }

    /* A call to this method should be followed by a call to
     * registerFontDirs(..)
     */
    public String getPlatformFontPath(boolean noType1Font) {
        if (fontPath == null) {
            fontPath = getFontPath(noType1Font);
        }
        return fontPath;
    }

    @SuppressWarnings("removal")
    protected void loadFonts() {
        if (discoveredAllFonts) {
            return;
        }
        /* Use lock specific to the font system */
        synchronized (this) {
            if (FontUtilities.debugFonts()) {
                Thread.dumpStack();
                FontUtilities.logInfo("SunGraphicsEnvironment.loadFonts() called");
            }
            initialiseDeferredFonts();

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    if (fontPath == null) {
                        fontPath = getPlatformFontPath(noType1Font);
                        registerFontDirs(fontPath);
                    }
                    if (fontPath != null) {
                        // this will find all fonts including those already
                        // registered. But we have checks in place to prevent
                        // double registration.
                        if (! gotFontsFromPlatform()) {
                            registerFontsOnPath(fontPath, false,
                                                Font2D.UNKNOWN_RANK,
                                                false, true);
                            loadedAllFontFiles = true;
                        }
                    }
                    registerOtherFontFiles(registeredFontFiles);
                    discoveredAllFonts = true;
                    return null;
                }
            });
        }
    }

    protected void registerFontDirs(String pathName) {
        return;
    }

    private void registerFontsOnPath(String pathName,
                                     boolean useJavaRasterizer, int fontRank,
                                     boolean defer, boolean resolveSymLinks) {

        StringTokenizer parser = new StringTokenizer(pathName,
                File.pathSeparator);
        try {
            while (parser.hasMoreTokens()) {
                registerFontsInDir(parser.nextToken(),
                        useJavaRasterizer, fontRank,
                        defer, resolveSymLinks);
            }
        } catch (NoSuchElementException e) {
        }
    }

    /* Called to register fall back fonts */
    public void registerFontsInDir(String dirName) {
        registerFontsInDir(dirName, true, Font2D.JRE_RANK, true, false);
    }

    // MACOSX begin -- need to access this in subclass
    protected void registerFontsInDir(String dirName, boolean useJavaRasterizer,
    // MACOSX end
                                    int fontRank,
                                    boolean defer, boolean resolveSymLinks) {
        File pathFile = new File(dirName);
        addDirFonts(dirName, pathFile, ttFilter,
                    FONTFORMAT_TRUETYPE, useJavaRasterizer,
                    fontRank==Font2D.UNKNOWN_RANK ?
                    Font2D.TTF_RANK : fontRank,
                    defer, resolveSymLinks);
        addDirFonts(dirName, pathFile, t1Filter,
                    FONTFORMAT_TYPE1, useJavaRasterizer,
                    fontRank==Font2D.UNKNOWN_RANK ?
                    Font2D.TYPE1_RANK : fontRank,
                    defer, resolveSymLinks);
    }

    protected void registerFontDir(String path) {
    }

    /**
     * Returns file name for default font, either absolute
     * or relative as needed by registerFontFile.
     */
    public synchronized String getDefaultFontFile() {
        return defaultFontFileName;
    }

    /**
     * Whether registerFontFile expects absolute or relative
     * font file names.
     */
    protected boolean useAbsoluteFontFileNames() {
        return true;
    }

    /**
     * Creates this environment's FontConfiguration.
     */
    protected abstract FontConfiguration createFontConfiguration();

    public abstract FontConfiguration
    createFontConfiguration(boolean preferLocaleFonts,
                            boolean preferPropFonts);

    /**
     * Returns face name for default font, or null if
     * no face names are used for CompositeFontDescriptors
     * for this platform.
     */
    public synchronized String getDefaultFontFaceName() {
        return defaultFontName;
    }

    @SuppressWarnings("removal")
    public void loadFontFiles() {
        loadFonts();
        if (loadedAllFontFiles) {
            return;
        }
        /* Use lock specific to the font system */
        synchronized (this) {
            if (FontUtilities.debugFonts()) {
                Thread.dumpStack();
                FontUtilities.logInfo("loadAllFontFiles() called");
            }
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    if (fontPath == null) {
                        fontPath = getPlatformFontPath(noType1Font);
                    }
                    if (fontPath != null) {
                        // this will find all fonts including those already
                        // registered. But we have checks in place to prevent
                        // double registration.
                        registerFontsOnPath(fontPath, false,
                                            Font2D.UNKNOWN_RANK,
                                            false, true);
                    }
                    loadedAllFontFiles = true;
                    return null;
                }
            });
        }
    }

    /*
     * This method asks the font configuration API for all platform names
     * used as components of composite/logical fonts and iterates over these
     * looking up their corresponding file name and registers these fonts.
     * It also ensures that the fonts are accessible via platform APIs.
     * The composites themselves are then registered.
     */
    private void
        initCompositeFonts(FontConfiguration fontConfig,
                           ConcurrentHashMap<String, Font2D>  altNameCache) {
        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("Initialising composite fonts");
        }

        int numCoreFonts = fontConfig.getNumberCoreFonts();
        String[] fcFonts = fontConfig.getPlatformFontNames();
        for (int f=0; f<fcFonts.length; f++) {
            String platformFontName = fcFonts[f];
            String fontFileName =
                getFileNameFromPlatformName(platformFontName);
            String[] nativeNames = null;
            if (fontFileName == null
                || fontFileName.equals(platformFontName)) {
                /* No file located, so register using the platform name,
                 * i.e. as a native font.
                 */
                fontFileName = platformFontName;
            } else {
                if (f < numCoreFonts) {
                    /* If platform APIs also need to access the font, add it
                     * to a set to be registered with the platform too.
                     * This may be used to add the parent directory to the X11
                     * font path if its not already there. See the docs for the
                     * subclass implementation.
                     * This is now mainly for the benefit of X11-based AWT
                     * But for historical reasons, 2D initialisation code
                     * makes these calls.
                     * If the fontconfiguration file is properly set up
                     * so that all fonts are mapped to files and all their
                     * appropriate directories are specified, then this
                     * method will be low cost as it will return after
                     * a test that finds a null lookup map.
                     */
                    addFontToPlatformFontPath(platformFontName);
                }
                nativeNames = getNativeNames(fontFileName, platformFontName);
            }
            /* Uncomment these two lines to "generate" the XLFD->filename
             * mappings needed to speed start-up on Solaris.
             * Augment this with the appendedpathname and the mappings
             * for native (F3) fonts
             */
            //String platName = platformFontName.replaceAll(" ", "_");
            //System.out.println("filename."+platName+"="+fontFileName);
            registerFontFile(fontFileName, nativeNames,
                             Font2D.FONT_CONFIG_RANK, true);


        }
        /* This registers accumulated paths from the calls to
         * addFontToPlatformFontPath(..) and any specified by
         * the font configuration. Rather than registering
         * the fonts it puts them in a place and form suitable for
         * the Toolkit to pick up and use if a toolkit is initialised,
         * and if it uses X11 fonts.
         */
        registerPlatformFontsUsedByFontConfiguration();

        CompositeFontDescriptor[] compositeFontInfo
                = fontConfig.get2DCompositeFontInfo();
        for (int i = 0; i < compositeFontInfo.length; i++) {
            CompositeFontDescriptor descriptor = compositeFontInfo[i];
            String[] componentFileNames = descriptor.getComponentFileNames();
            String[] componentFaceNames = descriptor.getComponentFaceNames();

            /* It would be better eventually to handle this in the
             * FontConfiguration code which should also remove duplicate slots
             */
            if (missingFontFiles != null) {
                for (int ii=0; ii<componentFileNames.length; ii++) {
                    if (missingFontFiles.contains(componentFileNames[ii])) {
                        componentFileNames[ii] = getDefaultFontFile();
                        componentFaceNames[ii] = getDefaultFontFaceName();
                    }
                }
            }

            /* FontConfiguration needs to convey how many fonts it has added
             * as fallback component fonts which should not affect metrics.
             * The core component count will be the number of metrics slots.
             * This does not preclude other mechanisms for adding
             * fall back component fonts to the composite.
             */
            if (altNameCache != null) {
                SunFontManager.registerCompositeFont(
                    descriptor.getFaceName(),
                    componentFileNames, componentFaceNames,
                    descriptor.getCoreComponentCount(),
                    descriptor.getExclusionRanges(),
                    descriptor.getExclusionRangeLimits(),
                    true,
                    altNameCache);
            } else {
                registerCompositeFont(descriptor.getFaceName(),
                                      componentFileNames, componentFaceNames,
                                      descriptor.getCoreComponentCount(),
                                      descriptor.getExclusionRanges(),
                                      descriptor.getExclusionRangeLimits(),
                                      true);
            }
            if (FontUtilities.debugFonts()) {
                FontUtilities.logInfo("registered " + descriptor.getFaceName());
            }
        }
    }

    /**
     * Notifies graphics environment that the logical font configuration
     * uses the given platform font name. The graphics environment may
     * use this for platform specific initialization.
     */
    protected void addFontToPlatformFontPath(String platformFontName) {
    }

    protected void registerFontFile(String fontFileName, String[] nativeNames,
                                    int fontRank, boolean defer) {
//      REMIND: case compare depends on platform
        if (registeredFontFiles.contains(fontFileName)) {
            return;
        }
        int fontFormat;
        if (ttFilter.accept(null, fontFileName)) {
            fontFormat = FONTFORMAT_TRUETYPE;
        } else if (t1Filter.accept(null, fontFileName)) {
            fontFormat = FONTFORMAT_TYPE1;
        } else {
            fontFormat = FONTFORMAT_NATIVE;
        }
        registeredFontFiles.add(fontFileName);
        if (defer) {
            registerDeferredFont(fontFileName, fontFileName, nativeNames,
                                 fontFormat, false, fontRank);
        } else {
            registerFontFile(fontFileName, nativeNames, fontFormat, false,
                             fontRank);
        }
    }

    protected void registerPlatformFontsUsedByFontConfiguration() {
    }

    /*
     * A GE may verify whether a font file used in a fontconfiguration
     * exists. If it doesn't then either we may substitute the default
     * font, or perhaps elide it altogether from the composite font.
     * This makes some sense on windows where the font file is only
     * likely to be in one place. But on other OSes, eg Linux, the file
     * can move around depending. So there we probably don't want to assume
     * its missing and so won't add it to this list.
     * If this list - missingFontFiles - is non-null then the composite
     * font initialisation logic tests to see if a font file is in that
     * set.
     * Only one thread should be able to add to this set so we don't
     * synchronize.
     */
    protected void addToMissingFontFileList(String fileName) {
        if (missingFontFiles == null) {
            missingFontFiles = new HashSet<>();
        }
        missingFontFiles.add(fileName);
    }

    /*
     * This is for use only within getAllFonts().
     * Fonts listed in the fontconfig files for windows were all
     * on the "deferred" initialisation list. They were registered
     * either in the course of the application, or in the call to
     * loadFonts() within getAllFonts(). The fontconfig file specifies
     * the names of the fonts using the English names. If there's a
     * different name in the execution locale, then the platform will
     * report that, and we will construct the font with both names, and
     * thereby enumerate it twice. This happens for Japanese fonts listed
     * in the windows fontconfig, when run in the JA locale. The solution
     * is to rely (in this case) on the platform's font->file mapping to
     * determine that this name corresponds to a file we already registered.
     * This works because
     * - we know when we get here all deferred fonts are already initialised
     * - when we register a font file, we register all fonts in it.
     * - we know the fontconfig fonts are all in the windows registry
     */
    private boolean isNameForRegisteredFile(String fontName) {
        String fileName = getFileNameForFontName(fontName);
        if (fileName == null) {
            return false;
        }
        return registeredFontFiles.contains(fileName);
    }

    /*
     * This invocation is not in a privileged block because
     * all privileged operations (reading files and properties)
     * was conducted on the creation of the GE
     */
    public void
        createCompositeFonts(ConcurrentHashMap<String, Font2D> altNameCache,
                             boolean preferLocale,
                             boolean preferProportional) {

        FontConfiguration fontConfig =
            createFontConfiguration(preferLocale, preferProportional);
        initCompositeFonts(fontConfig, altNameCache);
    }

    /**
     * Returns all fonts installed in this environment.
     */
    public Font[] getAllInstalledFonts() {
        if (allFonts == null) {
            loadFonts();
            TreeMap<String, Font2D> fontMapNames = new TreeMap<>();
            /* warning: the number of composite fonts could change dynamically
             * if applications are allowed to create them. "allfonts" could
             * then be stale.
             */
            Font2D[] allfonts = getRegisteredFonts();
            for (int i=0; i < allfonts.length; i++) {
                if (!(allfonts[i] instanceof NativeFont)) {
                    fontMapNames.put(allfonts[i].getFontName(null),
                                     allfonts[i]);
                }
            }

            String[] platformNames = getFontNamesFromPlatform();
            if (platformNames != null) {
                for (int i=0; i<platformNames.length; i++) {
                    if (!isNameForRegisteredFile(platformNames[i])) {
                        fontMapNames.put(platformNames[i], null);
                    }
                }
            }

            String[] fontNames = fontMapNames.keySet().toArray(new String[0]);
            Font[] fonts = new Font[fontNames.length];
            for (int i=0; i < fontNames.length; i++) {
                fonts[i] = new Font(fontNames[i], Font.PLAIN, 1);
                Font2D f2d = fontMapNames.get(fontNames[i]);
                if (f2d  != null) {
                    FontAccess.getFontAccess().setFont2D(fonts[i], f2d.handle);
                }
            }
            allFonts = fonts;
        }

        Font []copyFonts = new Font[allFonts.length];
        System.arraycopy(allFonts, 0, copyFonts, 0, allFonts.length);
        return copyFonts;
    }

    /**
     * Get a list of installed fonts in the requested {@link Locale}.
     * The list contains the fonts Family Names.
     * If Locale is null, the default locale is used.
     *
     * @param requestedLocale, if null the default locale is used.
     * @return list of installed fonts in the system.
     */
    public String[] getInstalledFontFamilyNames(Locale requestedLocale) {
        if (requestedLocale == null) {
            requestedLocale = Locale.getDefault();
        }
        if (allFamilies != null && lastDefaultLocale != null &&
            requestedLocale.equals(lastDefaultLocale)) {
                String[] copyFamilies = new String[allFamilies.length];
                System.arraycopy(allFamilies, 0, copyFamilies,
                                 0, allFamilies.length);
                return copyFamilies;
        }

        TreeMap<String,String> familyNames = new TreeMap<String,String>();
        //  these names are always there and aren't localised
        String str;
        str = Font.SERIF;         familyNames.put(str.toLowerCase(), str);
        str = Font.SANS_SERIF;    familyNames.put(str.toLowerCase(), str);
        str = Font.MONOSPACED;    familyNames.put(str.toLowerCase(), str);
        str = Font.DIALOG;        familyNames.put(str.toLowerCase(), str);
        str = Font.DIALOG_INPUT;  familyNames.put(str.toLowerCase(), str);

        /* Platform APIs may be used to get the set of available family
         * names for the current default locale so long as it is the same
         * as the start-up system locale, rather than loading all fonts.
         */
        if (requestedLocale.equals(getSystemStartupLocale()) &&
            getFamilyNamesFromPlatform(familyNames, requestedLocale)) {
            /* Augment platform names with JRE font family names */
            getJREFontFamilyNames(familyNames, requestedLocale);
        } else {
            loadFontFiles();
            Font2D[] physicalfonts = getPhysicalFonts();
            for (int i=0; i < physicalfonts.length; i++) {
                if (!(physicalfonts[i] instanceof NativeFont)) {
                    String name =
                        physicalfonts[i].getFamilyName(requestedLocale);
                    familyNames.put(name.toLowerCase(requestedLocale), name);
                }
            }
        }

        // Add any native font family names here
        addNativeFontFamilyNames(familyNames, requestedLocale);

        String[] retval = familyNames.values().toArray(new String[0]);
        if (requestedLocale.equals(Locale.getDefault())) {
            lastDefaultLocale = requestedLocale;
            allFamilies = new String[retval.length];
            System.arraycopy(retval, 0, allFamilies, 0, allFamilies.length);
        }
        return retval;
    }

    // Provides an aperture to add native font family names to the map
    protected void addNativeFontFamilyNames(TreeMap<String, String> familyNames, Locale requestedLocale) { }

    @SuppressWarnings("removal")
    public void register1dot0Fonts() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                String type1Dir = "/usr/openwin/lib/X11/fonts/Type1";
                registerFontsInDir(type1Dir, true, Font2D.TYPE1_RANK,
                                   false, false);
                return null;
            }
        });
    }

    /* Really we need only the JRE fonts family names, but there's little
     * overhead in doing this the easy way by adding all the currently
     * known fonts.
     */
    protected void getJREFontFamilyNames(TreeMap<String,String> familyNames,
                                         Locale requestedLocale) {
        registerDeferredJREFonts(jreFontDirName);
        Font2D[] physicalfonts = getPhysicalFonts();
        for (int i=0; i < physicalfonts.length; i++) {
            if (!(physicalfonts[i] instanceof NativeFont)) {
                String name =
                    physicalfonts[i].getFamilyName(requestedLocale);
                familyNames.put(name.toLowerCase(requestedLocale), name);
            }
        }
    }

    /**
     * Default locale can be changed but we need to know the initial locale
     * as that is what is used by native code. Changing Java default locale
     * doesn't affect that.
     * Returns the locale in use when using native code to communicate
     * with platform APIs. On windows this is known as the "system" locale,
     * and it is usually the same as the platform locale, but not always,
     * so this method also checks an implementation property used only
     * on windows and uses that if set.
     */
    private static Locale systemLocale = null;
    @SuppressWarnings("removal")
    private static Locale getSystemStartupLocale() {
        if (systemLocale == null) {
            systemLocale = AccessController.doPrivileged(new PrivilegedAction<Locale>() {
                public Locale run() {
                    /* On windows the system locale may be different than the
                     * user locale. This is an unsupported configuration, but
                     * in that case we want to return a dummy locale that will
                     * never cause a match in the usage of this API. This is
                     * important because Windows documents that the family
                     * names of fonts are enumerated using the language of
                     * the system locale. BY returning a dummy locale in that
                     * case we do not use the platform API which would not
                     * return us the names we want.
                     */
                    String fileEncoding = System.getProperty("file.encoding", "");
                    String sysEncoding = System.getProperty("sun.jnu.encoding");
                    if (sysEncoding != null && !sysEncoding.equals(fileEncoding)) {
                        return Locale.ROOT;
                    }

                    String language = System.getProperty("user.language", "en");
                    String country  = System.getProperty("user.country","");
                    String variant  = System.getProperty("user.variant","");
                    return new Locale(language, country, variant);
                }
            });
        }
        return systemLocale;
    }

    void addToPool(FileFont font) {

        FileFont fontFileToClose = null;
        int freeSlot = -1;

        synchronized (fontFileCache) {
            /* Avoid duplicate entries in the pool, and don't close() it,
             * since this method is called only from within open().
             * Seeing a duplicate is most likely to happen if the thread
             * was interrupted during a read, forcing perhaps repeated
             * close and open calls and it eventually it ends up pointing
             * at the same slot.
             */
            for (int i=0;i<CHANNELPOOLSIZE;i++) {
                if (fontFileCache[i] == font) {
                    return;
                }
                if (fontFileCache[i] == null && freeSlot < 0) {
                    freeSlot = i;
                }
            }
            if (freeSlot >= 0) {
                fontFileCache[freeSlot] = font;
                return;
            } else {
                /* replace with new font. */
                fontFileToClose = fontFileCache[lastPoolIndex];
                fontFileCache[lastPoolIndex] = font;
                /* lastPoolIndex is updated so that the least recently opened
                 * file will be closed next.
                 */
                lastPoolIndex = (lastPoolIndex+1) % CHANNELPOOLSIZE;
            }
        }
        /* Need to close the font file outside of the synchronized block,
         * since its possible some other thread is in an open() call on
         * this font file, and could be holding its lock and the pool lock.
         * Releasing the pool lock allows that thread to continue, so it can
         * then release the lock on this font, allowing the close() call
         * below to proceed.
         * Also, calling close() is safe because any other thread using
         * the font we are closing() synchronizes all reading, so we
         * will not close the file while its in use.
         */
        if (fontFileToClose != null) {
            fontFileToClose.close();
        }
    }

    protected FontUIResource getFontConfigFUIR(String family, int style,
                                               int size)
    {
        return new FontUIResource(family, style, size);
    }
}
