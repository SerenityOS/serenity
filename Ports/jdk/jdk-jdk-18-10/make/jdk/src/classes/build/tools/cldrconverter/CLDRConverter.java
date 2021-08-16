/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.cldrconverter;

import build.tools.cldrconverter.BundleGenerator.BundleType;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.ResourceBundle.Control;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;


/**
 * Converts locale data from "Locale Data Markup Language" format to
 * JRE resource bundle format. LDML is the format used by the Common
 * Locale Data Repository maintained by the Unicode Consortium.
 */
public class CLDRConverter {

    static final String LDML_DTD_SYSTEM_ID = "http://www.unicode.org/cldr/dtd/2.0/ldml.dtd";
    static final String SPPL_LDML_DTD_SYSTEM_ID = "http://www.unicode.org/cldr/dtd/2.0/ldmlSupplemental.dtd";
    static final String BCP47_LDML_DTD_SYSTEM_ID = "http://www.unicode.org/cldr/dtd/2.0/ldmlBCP47.dtd";


    private static String CLDR_BASE;
    static String LOCAL_LDML_DTD;
    static String LOCAL_SPPL_LDML_DTD;
    static String LOCAL_BCP47_LDML_DTD;
    private static String SOURCE_FILE_DIR;
    private static String SPPL_SOURCE_FILE;
    private static String SPPL_META_SOURCE_FILE;
    private static String NUMBERING_SOURCE_FILE;
    private static String METAZONES_SOURCE_FILE;
    private static String LIKELYSUBTAGS_SOURCE_FILE;
    private static String TIMEZONE_SOURCE_FILE;
    private static String WINZONES_SOURCE_FILE;
    private static String PLURALS_SOURCE_FILE;
    private static String DAYPERIODRULE_SOURCE_FILE;
    static String DESTINATION_DIR = "build/gensrc";

    static final String LOCALE_NAME_PREFIX = "locale.displayname.";
    static final String LOCALE_SEPARATOR = LOCALE_NAME_PREFIX + "separator";
    static final String LOCALE_KEYTYPE = LOCALE_NAME_PREFIX + "keytype";
    static final String LOCALE_KEY_PREFIX = LOCALE_NAME_PREFIX + "key.";
    static final String LOCALE_TYPE_PREFIX = LOCALE_NAME_PREFIX + "type.";
    static final String LOCALE_TYPE_PREFIX_CA = LOCALE_TYPE_PREFIX + "ca.";
    static final String CURRENCY_SYMBOL_PREFIX = "currency.symbol.";
    static final String CURRENCY_NAME_PREFIX = "currency.displayname.";
    static final String CALENDAR_NAME_PREFIX = "calendarname.";
    static final String CALENDAR_FIRSTDAY_PREFIX = "firstDay.";
    static final String CALENDAR_MINDAYS_PREFIX = "minDays.";
    static final String TIMEZONE_ID_PREFIX = "timezone.id.";
    static final String EXEMPLAR_CITY_PREFIX = "timezone.excity.";
    static final String ZONE_NAME_PREFIX = "timezone.displayname.";
    static final String METAZONE_ID_PREFIX = "metazone.id.";
    static final String PARENT_LOCALE_PREFIX = "parentLocale.";
    static final String META_EMPTY_ZONE_NAME = "EMPTY_ZONE";
    static final String[] EMPTY_ZONE = {"", "", "", "", "", ""};
    static final String META_ETCUTC_ZONE_NAME = "ETC_UTC";

    private static SupplementDataParseHandler handlerSuppl;
    private static LikelySubtagsParseHandler handlerLikelySubtags;
    private static WinZonesParseHandler handlerWinZones;
    static PluralsParseHandler handlerPlurals;
    static SupplementalMetadataParseHandler handlerSupplMeta;
    static NumberingSystemsParseHandler handlerNumbering;
    static MetaZonesParseHandler handlerMetaZones;
    static TimeZoneParseHandler handlerTimeZone;
    static DayPeriodRuleParseHandler handlerDayPeriodRule;
    private static BundleGenerator bundleGenerator;

    // java.base module related
    static boolean isBaseModule = false;
    static final Set<Locale> BASE_LOCALES = new HashSet<>();

    // "parentLocales" map
    private static final Map<String, SortedSet<String>> parentLocalesMap = new HashMap<>();
    private static final ResourceBundle.Control defCon =
        ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private static Set<String> AVAILABLE_TZIDS;
    private static String zoneNameTempFile;
    private static String tzDataDir;
    private static final Map<String, String> canonicalTZMap = new HashMap<>();

    // rules maps
    static Map<String, String> pluralRules;
    static Map<String, String> dayPeriodRules;

    static enum DraftType {
        UNCONFIRMED,
        PROVISIONAL,
        CONTRIBUTED,
        APPROVED;

        private static final Map<String, DraftType> map = new HashMap<>();
        static {
            for (DraftType dt : values()) {
                map.put(dt.getKeyword(), dt);
            }
        }
        static private DraftType defaultType = CONTRIBUTED;

        private final String keyword;

        private DraftType() {
            keyword = this.name().toLowerCase(Locale.ROOT);

        }

        static DraftType forKeyword(String keyword) {
            return map.get(keyword);
        }

        static DraftType getDefault() {
            return defaultType;
        }

        static void setDefault(String keyword) {
            defaultType = Objects.requireNonNull(forKeyword(keyword));
        }

        String getKeyword() {
            return keyword;
        }
    }

    static boolean USE_UTF8 = false;
    private static boolean verbose;

    private CLDRConverter() {
       // no instantiation
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            String currentArg = null;
            try {
                for (int i = 0; i < args.length; i++) {
                    currentArg = args[i];
                    switch (currentArg) {
                    case "-draft":
                        String draftDataType = args[++i];
                        try {
                            DraftType.setDefault(draftDataType);
                        } catch (NullPointerException e) {
                            severe("Error: incorrect draft value: %s%n", draftDataType);
                            System.exit(1);
                        }
                        info("Using the specified data type: %s%n", draftDataType);
                        break;

                    case "-base":
                        // base directory for input files
                        CLDR_BASE = args[++i];
                        if (!CLDR_BASE.endsWith("/")) {
                            CLDR_BASE += "/";
                        }
                        break;

                    case "-baselocales":
                        // base locales
                        setupBaseLocales(args[++i]);
                        break;

                    case "-basemodule":
                        // indicates java.base module resource generation
                        isBaseModule = true;
                        break;

                    case "-o":
                        // output directory
                        DESTINATION_DIR = args[++i];
                        break;

                    case "-utf8":
                        USE_UTF8 = true;
                        break;

                    case "-verbose":
                        verbose = true;
                        break;

                    case "-zntempfile":
                        zoneNameTempFile = args[++i];
                        break;

                    case "-tzdatadir":
                        tzDataDir = args[++i];
                        break;

                    case "-help":
                        usage();
                        System.exit(0);
                        break;

                    default:
                        throw new RuntimeException();
                    }
                }
            } catch (RuntimeException e) {
                severe("unknown or imcomplete arg(s): " + currentArg);
                usage();
                System.exit(1);
            }
        }

        // Set up path names
        LOCAL_LDML_DTD = CLDR_BASE + "/dtd/ldml.dtd";
        LOCAL_SPPL_LDML_DTD = CLDR_BASE + "/dtd/ldmlSupplemental.dtd";
        LOCAL_BCP47_LDML_DTD = CLDR_BASE + "/dtd/ldmlBCP47.dtd";
        SOURCE_FILE_DIR = CLDR_BASE + "/main";
        SPPL_SOURCE_FILE = CLDR_BASE + "/supplemental/supplementalData.xml";
        LIKELYSUBTAGS_SOURCE_FILE = CLDR_BASE + "/supplemental/likelySubtags.xml";
        NUMBERING_SOURCE_FILE = CLDR_BASE + "/supplemental/numberingSystems.xml";
        METAZONES_SOURCE_FILE = CLDR_BASE + "/supplemental/metaZones.xml";
        TIMEZONE_SOURCE_FILE = CLDR_BASE + "/bcp47/timezone.xml";
        SPPL_META_SOURCE_FILE = CLDR_BASE + "/supplemental/supplementalMetadata.xml";
        WINZONES_SOURCE_FILE = CLDR_BASE + "/supplemental/windowsZones.xml";
        PLURALS_SOURCE_FILE = CLDR_BASE + "/supplemental/plurals.xml";
        DAYPERIODRULE_SOURCE_FILE = CLDR_BASE + "/supplemental/dayPeriods.xml";

        if (BASE_LOCALES.isEmpty()) {
            setupBaseLocales("en-US");
        }

        bundleGenerator = new ResourceBundleGenerator();

        // Parse data independent of locales
        parseSupplemental();
        parseBCP47();

        // rules maps
        pluralRules = generateRules(handlerPlurals);
        dayPeriodRules = generateRules(handlerDayPeriodRule);

        List<Bundle> bundles = readBundleList();
        convertBundles(bundles);

        if (isBaseModule) {
            // Generate java.time.format.ZoneName.java
            generateZoneName();

            // Generate Windows tzmappings
            generateWindowsTZMappings();
        }
    }

    private static void usage() {
        errout("Usage: java CLDRConverter [options]%n"
                + "\t-help          output this usage message and exit%n"
                + "\t-verbose       output information%n"
                + "\t-draft [contributed | approved | provisional | unconfirmed]%n"
                + "\t\t       draft level for using data (default: contributed)%n"
                + "\t-base dir      base directory for CLDR input files%n"
                + "\t-basemodule    generates bundles that go into java.base module%n"
                + "\t-baselocales loc(,loc)*      locales that go into the base module%n"
                + "\t-o dir         output directory (default: ./build/gensrc)%n"
                + "\t-zntempfile    template file for java.time.format.ZoneName.java%n"
                + "\t-tzdatadir     tzdata directory for java.time.format.ZoneName.java%n"
                + "\t-utf8          use UTF-8 rather than \\uxxxx (for debug)%n");
    }

    static void info(String fmt, Object... args) {
        if (verbose) {
            System.out.printf(fmt, args);
        }
    }

    static void info(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    static void warning(String fmt, Object... args) {
        System.err.print("Warning: ");
        System.err.printf(fmt, args);
    }

    static void warning(String msg) {
        System.err.print("Warning: ");
        errout(msg);
    }

    static void severe(String fmt, Object... args) {
        System.err.print("Error: ");
        System.err.printf(fmt, args);
    }

    static void severe(String msg) {
        System.err.print("Error: ");
        errout(msg);
    }

    private static void errout(String msg) {
        if (msg.contains("%n")) {
            System.err.printf(msg);
        } else {
            System.err.println(msg);
        }
    }

    /**
     * Configure the parser to allow access to DTDs on the file system.
     */
    private static void enableFileAccess(SAXParser parser) throws SAXNotSupportedException {
        try {
            parser.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "file");
        } catch (SAXNotRecognizedException ignore) {
            // property requires >= JAXP 1.5
        }
    }

    private static List<Bundle> readBundleList() throws Exception {
        List<Bundle> retList = new ArrayList<>();
        Path path = FileSystems.getDefault().getPath(SOURCE_FILE_DIR);
        try (DirectoryStream<Path> dirStr = Files.newDirectoryStream(path)) {
            for (Path entry : dirStr) {
                String fileName = entry.getFileName().toString();
                if (fileName.endsWith(".xml")) {
                    String id = fileName.substring(0, fileName.indexOf('.'));
                    Locale cldrLoc = Locale.forLanguageTag(toLanguageTag(id));
                    StringBuilder sb = getCandLocales(cldrLoc);
                    if (sb.indexOf("root") == -1) {
                        sb.append("root");
                    }
                    retList.add(new Bundle(id, sb.toString(), null, null));
                }
            }
        }

        // Sort the bundles based on id. This will make sure all the parent bundles are
        // processed first, e.g., for en_GB bundle, en_001, and "root" comes before
        // en_GB. In order for "root" to come at the beginning, "root" is replaced with
        // empty string on comparison.
        retList.sort((o1, o2) -> {
            String id1 = o1.getID();
            String id2 = o2.getID();
            if(id1.equals("root")) {
                id1 = "";
            }
            if(id2.equals("root")) {
                id2 = "";
            }
            return id1.compareTo(id2);
        });
        return retList;
    }

    private static final Map<String, Map<String, Object>> cldrBundles = new HashMap<>();

    private static Map<String, SortedSet<String>> metaInfo = new HashMap<>();

    static {
        // For generating information on supported locales.
        metaInfo.put("AvailableLocales", new TreeSet<>());
    }

    static Map<String, Object> getCLDRBundle(String id) throws Exception {
        Map<String, Object> bundle = cldrBundles.get(id);
        if (bundle != null) {
            return bundle;
        }
        File file = new File(SOURCE_FILE_DIR + File.separator + id + ".xml");
        if (!file.exists()) {
            // Skip if the file doesn't exist.
            return Collections.emptyMap();
        }

        info("..... main directory .....");
        LDMLParseHandler handler = new LDMLParseHandler(id);
        parseLDMLFile(file, handler);

        bundle = handler.getData();
        cldrBundles.put(id, bundle);

        if (id.equals("root")) {
            // Calendar data (firstDayOfWeek & minDaysInFirstWeek)
            bundle = handlerSuppl.getData("root");
            if (bundle != null) {
                //merge two maps into one map
                Map<String, Object> temp = cldrBundles.remove(id);
                bundle.putAll(temp);
                cldrBundles.put(id, bundle);
            }
        }
        return bundle;
    }

    // Parsers for data in "supplemental" directory
    //
    private static void parseSupplemental() throws Exception {
        // Parse SupplementalData file and store the information in the HashMap
        // Calendar information such as firstDay and minDay are stored in
        // supplementalData.xml as of CLDR1.4. Individual territory is listed
        // with its ISO 3166 country code while default is listed using UNM49
        // region and composition numerical code (001 for World.)
        //
        // SupplementalData file also provides the "parent" locales which
        // are othrwise not to be fallen back. Process them here as well.
        //
        handlerSuppl = new SupplementDataParseHandler();
        parseLDMLFile(new File(SPPL_SOURCE_FILE), handlerSuppl);
        Map<String, Object> parentData = handlerSuppl.getData("root");
        parentData.keySet().stream()
                .filter(key -> key.startsWith(PARENT_LOCALE_PREFIX))
                .forEach(key -> {
                parentLocalesMap.put(key, new TreeSet<String>(
                    Arrays.asList(((String)parentData.get(key)).split(" "))));
            });

        // Parse numberingSystems to get digit zero character information.
        handlerNumbering = new NumberingSystemsParseHandler();
        parseLDMLFile(new File(NUMBERING_SOURCE_FILE), handlerNumbering);

        // Parse metaZones to create mappings between Olson tzids and CLDR meta zone names
        handlerMetaZones = new MetaZonesParseHandler();
        parseLDMLFile(new File(METAZONES_SOURCE_FILE), handlerMetaZones);

        // Parse likelySubtags
        handlerLikelySubtags = new LikelySubtagsParseHandler();
        parseLDMLFile(new File(LIKELYSUBTAGS_SOURCE_FILE), handlerLikelySubtags);

        // Parse supplementalMetadata
        // Currently interested in deprecated time zone ids and language aliases.
        handlerSupplMeta = new SupplementalMetadataParseHandler();
        parseLDMLFile(new File(SPPL_META_SOURCE_FILE), handlerSupplMeta);

        // Parse windowsZones
        handlerWinZones = new WinZonesParseHandler();
        parseLDMLFile(new File(WINZONES_SOURCE_FILE), handlerWinZones);

        // Parse plurals
        handlerPlurals = new PluralsParseHandler();
        parseLDMLFile(new File(PLURALS_SOURCE_FILE), handlerPlurals);

        // Parse day period rules
        handlerDayPeriodRule = new DayPeriodRuleParseHandler();
        parseLDMLFile(new File(DAYPERIODRULE_SOURCE_FILE), handlerDayPeriodRule);
    }

    // Parsers for data in "bcp47" directory
    //
    private static void parseBCP47() throws Exception {
        // Parse timezone
        handlerTimeZone = new TimeZoneParseHandler();
        parseLDMLFile(new File(TIMEZONE_SOURCE_FILE), handlerTimeZone);

        // canonical tz name map
        // alias -> primary
        handlerTimeZone.getData().forEach((k, v) -> {
            String[] ids = ((String)v).split("\\s");
            for (int i = 1; i < ids.length; i++) {
                canonicalTZMap.put(ids[i], ids[0]);
            }
        });
    }

    private static void parseLDMLFile(File srcfile, AbstractLDMLHandler<?> handler) throws Exception {
        info("..... Parsing " + srcfile.getName() + " .....");
        SAXParserFactory pf = SAXParserFactory.newInstance();
        pf.setValidating(true);
        SAXParser parser = pf.newSAXParser();
        enableFileAccess(parser);
        parser.parse(srcfile, handler);
    }

    private static StringBuilder getCandLocales(Locale cldrLoc) {
        List<Locale> candList = getCandidateLocales(cldrLoc);
        StringBuilder sb = new StringBuilder();
        for (Locale loc : candList) {
            if (!loc.equals(Locale.ROOT)) {
                sb.append(toLocaleName(loc.toLanguageTag()));
                sb.append(",");
            }
        }
        return sb;
    }

    private static List<Locale> getCandidateLocales(Locale cldrLoc) {
        List<Locale> candList = new ArrayList<>();
        candList = applyParentLocales("", defCon.getCandidateLocales("",  cldrLoc));
        return candList;
    }

    private static void convertBundles(List<Bundle> bundles) throws Exception {
        var availableLangTags = metaInfo.get("AvailableLocales");

        // parent locales map. The mappings are put in base metaInfo file
        // for now.
        if (isBaseModule) {
            metaInfo.putAll(parentLocalesMap);
        }

        for (Bundle bundle : bundles) {
            // Get the target map, which contains all the data that should be
            // visible for the bundle's locale

            Map<String, Object> targetMap = bundle.getTargetMap();
            EnumSet<Bundle.Type> bundleTypes = bundle.getBundleTypes();
            var id = bundle.getID();

            if (bundle.isRoot()) {
                // Add DateTimePatternChars because CLDR no longer supports localized patterns.
                targetMap.put("DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ");
            }

            // Now the map contains just the entries that need to be in the resources bundles.
            // Go ahead and generate them.
            if (bundleTypes.contains(Bundle.Type.LOCALENAMES)) {
                Map<String, Object> localeNamesMap = extractLocaleNames(targetMap, id);
                if (!localeNamesMap.isEmpty() || bundle.isRoot()) {
                    bundleGenerator.generateBundle("util", "LocaleNames", id, true, localeNamesMap, BundleType.OPEN);
                }
            }
            if (bundleTypes.contains(Bundle.Type.CURRENCYNAMES)) {
                Map<String, Object> currencyNamesMap = extractCurrencyNames(targetMap, id, bundle.getCurrencies());
                if (!currencyNamesMap.isEmpty() || bundle.isRoot()) {
                    bundleGenerator.generateBundle("util", "CurrencyNames", id, true, currencyNamesMap, BundleType.OPEN);
                }
            }
            if (bundleTypes.contains(Bundle.Type.TIMEZONENAMES)) {
                Map<String, Object> zoneNamesMap = extractZoneNames(targetMap, id);
                if (!zoneNamesMap.isEmpty() || bundle.isRoot()) {
                    bundleGenerator.generateBundle("util", "TimeZoneNames", id, true, zoneNamesMap, BundleType.TIMEZONE);
                }
            }
            if (bundleTypes.contains(Bundle.Type.CALENDARDATA)) {
                Map<String, Object> calendarDataMap = extractCalendarData(targetMap, id);
                if (!calendarDataMap.isEmpty() || bundle.isRoot()) {
                    bundleGenerator.generateBundle("util", "CalendarData", id, true, calendarDataMap, BundleType.PLAIN);
                }
            }
            if (bundleTypes.contains(Bundle.Type.FORMATDATA)) {
                Map<String, Object> formatDataMap = extractFormatData(targetMap, id);
                if (!formatDataMap.isEmpty() || bundle.isRoot()) {
                    bundleGenerator.generateBundle("text", "FormatData", id, true, formatDataMap, BundleType.PLAIN);
                }
            }

            // For AvailableLocales
            var langTag = toLanguageTag(id);
            availableLangTags.add(langTag);
            addLikelySubtags(langTag);
        }

        // Add extra language tags from likely subtags that meet the following conditions
        // 1. Its likely subtag is supported (already in the available langtag set)
        // 2. Neither of old obsolete ones (in/iw/ji)
        handlerLikelySubtags.getData().entrySet().stream()
            .filter(e -> availableLangTags.contains(e.getValue()))
            .map(Map.Entry::getKey)
            .filter(t -> !t.equals("in") && !t.equals("iw") && !t.equals("ji"))
            .forEach(availableLangTags::add);

        bundleGenerator.generateMetaInfo(metaInfo);
    }

    static final Map<String, String> aliases = new HashMap<>();

    /**
     * Translate the aliases into the real entries in the bundle map.
     */
    static void handleAliases(Map<String, Object> bundleMap) {
        Set<String> bundleKeys = bundleMap.keySet();
        try {
            for (String key : aliases.keySet()) {
                String targetKey = aliases.get(key);
                if (bundleKeys.contains(targetKey)) {
                    bundleMap.putIfAbsent(key, bundleMap.get(targetKey));
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CLDRConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * Returns the language portion of the given id.
     * If id is "root", "" is returned.
     */
    static String getLanguageCode(String id) {
        return "root".equals(id) ? "" : Locale.forLanguageTag(id.replaceAll("_", "-")).getLanguage();
    }

    /**
     * Examine if the id includes the country (territory) code. If it does, it returns
     * the country code.
     * Otherwise, it returns null. eg. when the id is "zh_Hans_SG", it return "SG".
     * It does NOT return UN M.49 code, e.g., '001', as those three digit numbers cannot
     * be translated into package names.
     */
    static String getCountryCode(String id) {
        String rgn = getRegionCode(id);
        return rgn.length() == 2 ? rgn: null;
    }

    /**
     * Examine if the id includes the region code. If it does, it returns
     * the region code.
     * Otherwise, it returns null. eg. when the id is "zh_Hans_SG", it return "SG".
     * It DOES return UN M.49 code, e.g., '001', as well as ISO 3166 two letter country codes.
     */
    static String getRegionCode(String id) {
        return Locale.forLanguageTag(id.replaceAll("_", "-")).getCountry();
    }

    private static class KeyComparator implements Comparator<String> {
        static KeyComparator INSTANCE = new KeyComparator();

        private KeyComparator() {
        }

        @Override
        public int compare(String o1, String o2) {
            int len1 = o1.length();
            int len2 = o2.length();
            if (!isDigit(o1.charAt(0)) && !isDigit(o2.charAt(0))) {
                // Shorter string comes first unless either starts with a digit.
                if (len1 < len2) {
                    return -1;
                }
                if (len1 > len2) {
                    return 1;
                }
            }
            return o1.compareTo(o2);
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }

    private static Map<String, Object> extractLocaleNames(Map<String, Object> map, String id) {
        Map<String, Object> localeNames = new TreeMap<>(KeyComparator.INSTANCE);
        for (String key : map.keySet()) {
            if (key.startsWith(LOCALE_NAME_PREFIX)) {
                switch (key) {
                    case LOCALE_SEPARATOR:
                        localeNames.put("ListCompositionPattern", map.get(key));
                        break;
                    case LOCALE_KEYTYPE:
                        localeNames.put("ListKeyTypePattern", map.get(key));
                        break;
                    default:
                        localeNames.put(key.substring(LOCALE_NAME_PREFIX.length()), map.get(key));
                        break;
                }
            }
        }

        if (id.equals("root")) {
            // Add display name pattern, which is not in CLDR
            localeNames.put("DisplayNamePattern", "{0,choice,0#|1#{1}|2#{1} ({2})}");
        }

        return localeNames;
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    private static Map<String, Object> extractCurrencyNames(Map<String, Object> map, String id, String names)
            throws Exception {
        Map<String, Object> currencyNames = new TreeMap<>(KeyComparator.INSTANCE);
        for (String key : map.keySet()) {
            if (key.startsWith(CURRENCY_NAME_PREFIX)) {
                currencyNames.put(key.substring(CURRENCY_NAME_PREFIX.length()), map.get(key));
            } else if (key.startsWith(CURRENCY_SYMBOL_PREFIX)) {
                currencyNames.put(key.substring(CURRENCY_SYMBOL_PREFIX.length()), map.get(key));
            }
        }
        return currencyNames;
    }

    private static Map<String, Object> extractZoneNames(Map<String, Object> map, String id) {
        Map<String, Object> names = new TreeMap<>(KeyComparator.INSTANCE);

        getAvailableZoneIds().stream().forEach(tzid -> {
            // If the tzid is deprecated, get the data for the replacement id
            String tzKey = Optional.ofNullable((String)handlerSupplMeta.get(tzid))
                                   .orElse(tzid);
            Object data = map.get(TIMEZONE_ID_PREFIX + tzKey);

            if (data instanceof String[]) {
                // Hack for UTC. UTC is an alias to Etc/UTC in CLDR
                if (tzid.equals("Etc/UTC") && !map.containsKey(TIMEZONE_ID_PREFIX + "UTC")) {
                    names.put(METAZONE_ID_PREFIX + META_ETCUTC_ZONE_NAME, data);
                    names.put(tzid, META_ETCUTC_ZONE_NAME);
                    names.put("UTC", META_ETCUTC_ZONE_NAME);
                } else {
                    names.put(tzid, data);
                }
            } else {
                String meta = handlerMetaZones.get(tzKey);
                if (meta != null) {
                    String metaKey = METAZONE_ID_PREFIX + meta;
                    data = map.get(metaKey);
                    if (data instanceof String[]) {
                        // Keep the metazone prefix here.
                        names.put(metaKey, data);
                        names.put(tzid, meta);
                    }
                }
            }
        });

        // exemplar cities.
        Map<String, Object> exCities = map.entrySet().stream()
            .filter(e -> e.getKey().startsWith(CLDRConverter.EXEMPLAR_CITY_PREFIX))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        names.putAll(exCities);

        // If there's no UTC entry at this point, add an empty one
        if (!names.isEmpty() && !names.containsKey("UTC")) {
            names.putIfAbsent(METAZONE_ID_PREFIX + META_EMPTY_ZONE_NAME, EMPTY_ZONE);
            names.put("UTC", META_EMPTY_ZONE_NAME);
        }

        // Finally some compatibility stuff
        ZoneId.SHORT_IDS.entrySet().stream()
            .filter(e -> !names.containsKey(e.getKey()) && names.containsKey(e.getValue()))
            .forEach(e -> {
                names.put(e.getKey(), names.get(e.getValue()));
            });

        return names;
    }

    /**
     * Extracts the language independent calendar data. Each of the two keys,
     * "firstDayOfWeek" and "minimalDaysInFirstWeek" has a string value consists of
     * one or multiple occurrences of:
     *  i: rg1 rg2 ... rgn;
     * where "i" is the data for the following regions (delimited by a space) after
     * ":", and ends with a ";".
     */
    private static Map<String, Object> extractCalendarData(Map<String, Object> map, String id) {
        Map<String, Object> calendarData = new LinkedHashMap<>();
        if (id.equals("root")) {
            calendarData.put("firstDayOfWeek",
                IntStream.range(1, 8)
                    .mapToObj(String::valueOf)
                    .filter(d -> map.keySet().contains(CALENDAR_FIRSTDAY_PREFIX + d))
                    .map(d -> d + ": " + map.get(CALENDAR_FIRSTDAY_PREFIX + d))
                    .collect(Collectors.joining(";")));
            calendarData.put("minimalDaysInFirstWeek",
                IntStream.range(0, 7)
                    .mapToObj(String::valueOf)
                    .filter(d -> map.keySet().contains(CALENDAR_MINDAYS_PREFIX + d))
                    .map(d -> d + ": " + map.get(CALENDAR_MINDAYS_PREFIX + d))
                    .collect(Collectors.joining(";")));
        }
        return calendarData;
    }

    static final String[] FORMAT_DATA_ELEMENTS = {
        "MonthNames",
        "standalone.MonthNames",
        "MonthAbbreviations",
        "standalone.MonthAbbreviations",
        "MonthNarrows",
        "standalone.MonthNarrows",
        "DayNames",
        "standalone.DayNames",
        "DayAbbreviations",
        "standalone.DayAbbreviations",
        "DayNarrows",
        "standalone.DayNarrows",
        "QuarterNames",
        "standalone.QuarterNames",
        "QuarterAbbreviations",
        "standalone.QuarterAbbreviations",
        "QuarterNarrows",
        "standalone.QuarterNarrows",
        "AmPmMarkers",
        "narrow.AmPmMarkers",
        "abbreviated.AmPmMarkers",
        "long.Eras",
        "Eras",
        "narrow.Eras",
        "field.era",
        "field.year",
        "field.month",
        "field.week",
        "field.weekday",
        "field.dayperiod",
        "field.hour",
        "timezone.hourFormat",
        "timezone.gmtFormat",
        "timezone.gmtZeroFormat",
        "timezone.regionFormat",
        "timezone.regionFormat.daylight",
        "timezone.regionFormat.standard",
        "field.minute",
        "field.second",
        "field.zone",
        "TimePatterns",
        "DatePatterns",
        "DateTimePatterns",
        "DateTimePatternChars",
        "PluralRules",
        "DayPeriodRules",
    };

    private static Map<String, Object> extractFormatData(Map<String, Object> map, String id) {
        Map<String, Object> formatData = new LinkedHashMap<>();
        for (CalendarType calendarType : CalendarType.values()) {
            if (calendarType == CalendarType.GENERIC) {
                continue;
            }
            String prefix = calendarType.keyElementName();
            for (String element : FORMAT_DATA_ELEMENTS) {
                String key = prefix + element;
                copyIfPresent(map, "java.time." + key, formatData);
                copyIfPresent(map, key, formatData);
            }
        }

        for (String key : map.keySet()) {
            // Copy available calendar names
            if (key.startsWith(CLDRConverter.LOCALE_TYPE_PREFIX_CA)) {
                String type = key.substring(CLDRConverter.LOCALE_TYPE_PREFIX_CA.length());
                for (CalendarType calendarType : CalendarType.values()) {
                    if (calendarType == CalendarType.GENERIC) {
                        continue;
                    }
                    if (type.equals(calendarType.lname())) {
                        Object value = map.get(key);
                        String dataKey = key.replace(LOCALE_TYPE_PREFIX_CA,
                                CALENDAR_NAME_PREFIX);
                        formatData.put(dataKey, value);
                        String ukey = CALENDAR_NAME_PREFIX + calendarType.uname();
                        if (!dataKey.equals(ukey)) {
                            formatData.put(ukey, value);
                        }
                    }
                }
            }
        }

        copyIfPresent(map, "DefaultNumberingSystem", formatData);

        @SuppressWarnings("unchecked")
        List<String> numberingScripts = (List<String>) map.remove("numberingScripts");
        if (numberingScripts != null) {
            for (String script : numberingScripts) {
                copyIfPresent(map, script + ".NumberElements", formatData);
                copyIfPresent(map, script + ".NumberPatterns", formatData);
            }
        } else {
            copyIfPresent(map, "NumberElements", formatData);
            copyIfPresent(map, "NumberPatterns", formatData);
        }
        copyIfPresent(map, "short.CompactNumberPatterns", formatData);
        copyIfPresent(map, "long.CompactNumberPatterns", formatData);

        // put extra number elements for available scripts into formatData, if it is "root"
        if (id.equals("root")) {
            handlerNumbering.keySet().stream()
                .filter(k -> !numberingScripts.contains(k))
                .forEach(k -> {
                    String[] ne = (String[])map.get("latn.NumberElements");
                    String[] neNew = Arrays.copyOf(ne, ne.length);
                    neNew[4] = handlerNumbering.get(k).substring(0, 1);
                    formatData.put(k + ".NumberElements", neNew);
                });
        }
        return formatData;
    }

    private static void copyIfPresent(Map<String, Object> src, String key, Map<String, Object> dest) {
        Object value = src.get(key);
        if (value != null) {
            dest.put(key, value);
        }
    }

    // --- code below here is adapted from java.util.Properties ---
    private static final String specialSaveCharsJava = "\"";
    private static final String specialSaveCharsProperties = "=: \t\r\n\f#!";

    /*
     * Converts unicodes to encoded &#92;uxxxx
     * and writes out any of the characters in specialSaveChars
     * with a preceding slash
     */
    static String saveConvert(String theString, boolean useJava) {
        if (theString == null) {
            return "";
        }

        String specialSaveChars;
        if (useJava) {
            specialSaveChars = specialSaveCharsJava;
        } else {
            specialSaveChars = specialSaveCharsProperties;
        }
        boolean escapeSpace = false;

        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len * 2);
        Formatter formatter = new Formatter(outBuffer, Locale.ROOT);

        for (int x = 0; x < len; x++) {
            char aChar = theString.charAt(x);
            switch (aChar) {
            case ' ':
                if (x == 0 || escapeSpace) {
                    outBuffer.append('\\');
                }
                outBuffer.append(' ');
                break;
            case '\\':
                outBuffer.append('\\');
                outBuffer.append('\\');
                break;
            case '\t':
                outBuffer.append('\\');
                outBuffer.append('t');
                break;
            case '\n':
                outBuffer.append('\\');
                outBuffer.append('n');
                break;
            case '\r':
                outBuffer.append('\\');
                outBuffer.append('r');
                break;
            case '\f':
                outBuffer.append('\\');
                outBuffer.append('f');
                break;
            default:
                if (aChar < 0x0020 || (!USE_UTF8 && aChar > 0x007e)) {
                    formatter.format("\\u%04x", (int)aChar);
                } else {
                    if (specialSaveChars.indexOf(aChar) != -1) {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(aChar);
                }
            }
        }
        return outBuffer.toString();
    }

    static String toLanguageTag(String locName) {
        if (locName.indexOf('_') == -1) {
            return locName;
        }
        String tag = locName.replaceAll("_", "-");
        Locale loc = Locale.forLanguageTag(tag);
        return loc.toLanguageTag();
    }

    private static void addLikelySubtags(String langTag) {
        String likelySubtag = handlerLikelySubtags.get(langTag);
        if (likelySubtag != null) {
            var availableLangTags = metaInfo.get("AvailableLocales");
            availableLangTags.add(likelySubtag.replaceFirst("-[A-Z][a-z]{3}", ""));
            availableLangTags.add(likelySubtag);
        }
    }

    private static String toLocaleName(String tag) {
        if (tag.indexOf('-') == -1) {
            return tag;
        }
        return tag.replaceAll("-", "_");
    }

    private static void setupBaseLocales(String localeList) {
        Arrays.stream(localeList.split(","))
            .map(Locale::forLanguageTag)
            .map(l -> Control.getControl(Control.FORMAT_DEFAULT)
                             .getCandidateLocales("", l))
            .forEach(BASE_LOCALES::addAll);
    }

    // applying parent locale rules to the passed candidates list
    // This has to match with the one in sun.util.cldr.CLDRLocaleProviderAdapter
    private static Map<Locale, Locale> childToParentLocaleMap = null;
    private static List<Locale> applyParentLocales(String baseName, List<Locale> candidates) {
        if (Objects.isNull(childToParentLocaleMap)) {
            childToParentLocaleMap = new HashMap<>();
            parentLocalesMap.keySet().forEach(key -> {
                String parent = key.substring(PARENT_LOCALE_PREFIX.length()).replaceAll("_", "-");
                parentLocalesMap.get(key).stream().forEach(child -> {
                    childToParentLocaleMap.put(Locale.forLanguageTag(child),
                        "root".equals(parent) ? Locale.ROOT : Locale.forLanguageTag(parent));
                });
            });
        }

        // check irregular parents
        for (int i = 0; i < candidates.size(); i++) {
            Locale l = candidates.get(i);
            Locale p = childToParentLocaleMap.get(l);
            if (!l.equals(Locale.ROOT) &&
                Objects.nonNull(p) &&
                !candidates.get(i+1).equals(p)) {
                List<Locale> applied = candidates.subList(0, i+1);
                if (applied.contains(p)) {
                    // avoid circular recursion (could happen with nb/no case)
                    continue;
                }
                applied.addAll(applyParentLocales(baseName, defCon.getCandidateLocales(baseName, p)));
                return applied;
            }
        }

        return candidates;
    }

    private static void generateZoneName() throws Exception {
        Files.createDirectories(Paths.get(DESTINATION_DIR, "java", "time", "format"));
        Files.write(Paths.get(DESTINATION_DIR, "java", "time", "format", "ZoneName.java"),
            Files.lines(Paths.get(zoneNameTempFile))
                .flatMap(l -> {
                    if (l.equals("%%%%ZIDMAP%%%%")) {
                        return zidMapEntry();
                    } else if (l.equals("%%%%MZONEMAP%%%%")) {
                        return handlerMetaZones.mzoneMapEntry();
                    } else if (l.equals("%%%%DEPRECATED%%%%")) {
                        return handlerSupplMeta.deprecatedMap();
                    } else if (l.equals("%%%%TZDATALINK%%%%")) {
                        return tzDataLinkEntry();
                    } else {
                        return Stream.of(l);
                    }
                })
                .collect(Collectors.toList()),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // This method assumes handlerMetaZones is already initialized
    private static Set<String> getAvailableZoneIds() {
        assert handlerMetaZones != null;
        if (AVAILABLE_TZIDS == null) {
            AVAILABLE_TZIDS = new HashSet<>(ZoneId.getAvailableZoneIds());
            AVAILABLE_TZIDS.addAll(handlerMetaZones.keySet());
            AVAILABLE_TZIDS.remove(MetaZonesParseHandler.NO_METAZONE_KEY);
        }

        return AVAILABLE_TZIDS;
    }

    private static Stream<String> zidMapEntry() {
        return getAvailableZoneIds().stream()
                .map(id -> {
                    String canonId = canonicalTZMap.getOrDefault(id, id);
                    String meta = handlerMetaZones.get(canonId);
                    String zone001 = handlerMetaZones.zidMap().get(meta);
                    return zone001 == null ? "" :
                            String.format("        \"%s\", \"%s\", \"%s\",",
                                            id, meta, zone001);
                })
                .filter(s -> !s.isEmpty())
                .sorted();
    }

    private static Stream<String> tzDataLinkEntry() {
        try {
            return Files.walk(Paths.get(tzDataDir), 1)
                .filter(p -> !Files.isDirectory(p))
                .flatMap(CLDRConverter::extractLinks)
                .sorted();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> extractLinks(Path tzFile) {
        try {
            return Files.lines(tzFile)
                .filter(l -> l.startsWith("Link"))
                .map(l -> l.replaceFirst("^Link[\\s]+(\\S+)\\s+(\\S+).*",
                                         "        \"$2\", \"$1\","));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Generate tzmappings for Windows. The format is:
    //
    // (Windows Zone Name):(REGION):(Java TZID)
    //
    // where:
    //   Windows Zone Name: arbitrary time zone name string used in Windows
    //   REGION: ISO3166 or UN M.49 code
    //   Java TZID: Java's time zone ID
    //
    // Note: the entries are alphabetically sorted, *except* the "world" region
    // code, i.e., "001". It should be the last entry for the same windows time
    // zone name entries. (cf. TimeZone_md.c)
    private static void generateWindowsTZMappings() throws Exception {
        Files.createDirectories(Paths.get(DESTINATION_DIR, "windows", "conf"));
        Files.write(Paths.get(DESTINATION_DIR, "windows", "conf", "tzmappings"),
            handlerWinZones.keySet().stream()
                .map(k -> k + ":" + handlerWinZones.get(k) + ":")
                .sorted(new Comparator<String>() {
                    public int compare(String t1, String t2) {
                        String[] s1 = t1.split(":");
                        String[] s2 = t2.split(":");
                        if (s1[0].equals(s2[0])) {
                            if (s1[1].equals("001")) {
                                return 1;
                            } else if (s2[1].equals("001")) {
                                return -1;
                            } else {
                                return s1[1].compareTo(s2[1]);
                            }
                        } else {
                            return s1[0].compareTo(s2[0]);
                        }
                    }
                })
                .collect(Collectors.toList()),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Generates rules map for Plural rules and DayPeriod rules. The key is the locale id,
     * and the value is rules, defined by the LDML spec. Each rule consists of {@code type:rule}
     * notation, concatenated with a ";" as a delimiter.
     * @param handler handler containing rules
     * @return the map
     */
    private static Map<String, String> generateRules(AbstractLDMLHandler<Map<String, String>> handler) {
        return handler.getData().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                Map<String, String> rules = e.getValue();
                return rules.entrySet().stream()
                    .map(rule -> rule.getKey() + ":" + rule.getValue().replaceFirst("@.*", ""))
                    .map(String::trim)
                    .collect(Collectors.joining(";"));
            }));
    }

    // for debug
    static void dumpMap(Map<String, Object> map) {
        map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                Object val = e.getValue();
                String valStr = null;

                if (val instanceof String[]) {
                    valStr = Arrays.asList((String[])val).toString();
                } else if (val != null) {
                    valStr = val.toString();
                }
                return e.getKey() + " = " + valStr;
            })
            .forEach(System.out::println);
    }
}
