/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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


package build.tools.tzdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.time.*;
import java.time.Year;
import java.time.chrono.IsoChronology;
import java.time.temporal.TemporalAdjusters;
import build.tools.tzdb.ZoneOffsetTransitionRule.TimeDefinition;
import java.time.zone.ZoneRulesException;

/**
 * Compile and build time-zone rules from IANA timezone data
 *
 * @author Xueming Shen
 * @author Stephen Colebourne
 * @author Michael Nascimento Santos
 *
 * @since   9
 */

class TzdbZoneRulesProvider {

    /**
     * Creates an instance.
     *
     * @throws ZoneRulesException if unable to load
     */
    public TzdbZoneRulesProvider(List<Path> files) {
        try {
             load(files);
        } catch (Exception ex) {
            throw new ZoneRulesException("Unable to load TZDB time-zone rules", ex);
        }
    }

    public Set<String> getZoneIds() {
        return new TreeSet<String>(regionIds);
    }

    public Map<String, String> getAliasMap() {
        return links;
    }

    public ZoneRules getZoneRules(String zoneId) {
        Object obj = zones.get(zoneId);
        if (obj == null) {
            String zoneId0 = zoneId;
            if (links.containsKey(zoneId)) {
                zoneId = links.get(zoneId);
                obj = zones.get(zoneId);
            }
            if (obj == null) {
                // Timezone link can be located in 'backward' file and it
                // can refer to another link, so we need to check for
                // link one more time, before throwing an exception
                String zoneIdBack = zoneId;
                if (links.containsKey(zoneId)) {
                    zoneId = links.get(zoneId);
                    obj = zones.get(zoneId);
                }
                if (obj == null) {
                    throw new ZoneRulesException("Unknown time-zone ID: " + zoneIdBack);
                }
            }
        }
        if (obj instanceof ZoneRules) {
            return (ZoneRules)obj;
        }
        try {
            @SuppressWarnings("unchecked")
            ZoneRules zrules = buildRules(zoneId, (List<ZoneLine>)obj);
            zones.put(zoneId, zrules);
            return zrules;
        } catch (Exception ex) {
            throw new ZoneRulesException(
                "Invalid binary time-zone data: TZDB:" + zoneId, ex);
        }
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * All the regions that are available.
     */
    private List<String> regionIds = new ArrayList<>(600);

    /**
     * Zone region to rules mapping
     */
    private final Map<String, Object> zones = new ConcurrentSkipListMap<>();

    /**
     * compatibility list
     */
    private static Set<String> excludedZones;
    static {
        // (1) exclude EST, HST and MST. They are supported
        //     via the short-id mapping
        // (2) remove UTC and GMT
        // (3) remove ROC, which is not supported in j.u.tz
        excludedZones = new TreeSet<>();
        excludedZones.add("EST");
        excludedZones.add("HST");
        excludedZones.add("MST");
        excludedZones.add("GMT+0");
        excludedZones.add("GMT-0");
        excludedZones.add("ROC");
    }

    private Map<String, String> links = new TreeMap<>();
    private Map<String, List<RuleLine>> rules = new TreeMap<>();

    private void load(List<Path> files) throws IOException {

        for (Path file : files) {
            List<ZoneLine> openZone = null;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    //StringIterator itr = new StringIterator(line);
                    String[] tokens = split(line);
                    if (openZone != null &&               // continuing zone line
                        Character.isWhitespace(line.charAt(0)) &&
                        tokens.length > 0) {
                        ZoneLine zLine = new ZoneLine();
                        openZone.add(zLine);
                        if (zLine.parse(tokens, 0)) {
                            openZone = null;
                        }
                        continue;
                    }
                    if (line.startsWith("Zone")) {        // parse Zone line
                        String name = tokens[1];
                        if (excludedZones.contains(name)){
                            continue;
                        }
                        if (zones.containsKey(name)) {
                            throw new IllegalArgumentException(
                                "Duplicated zone name in file: " + name +
                                ", line: [" + line + "]");
                        }
                        openZone = new ArrayList<>(10);
                        zones.put(name, openZone);
                        regionIds.add(name);
                        ZoneLine zLine = new ZoneLine();
                        openZone.add(zLine);
                        if (zLine.parse(tokens, 2)) {
                            openZone = null;
                        }
                    } else if (line.startsWith("Rule")) { // parse Rule line
                        String name = tokens[1];
                        if (!rules.containsKey(name)) {
                            rules.put(name, new ArrayList<RuleLine>(10));
                        }
                        rules.get(name).add(new RuleLine().parse(tokens));
                    } else if (line.startsWith("Link")) { // parse link line
                        if (tokens.length >= 3) {
                            String realId = tokens[1];
                            String aliasId = tokens[2];
                            if (excludedZones.contains(aliasId)){
                                continue;
                            }
                            links.put(aliasId, realId);
                            regionIds.add(aliasId);
                        } else {
                            throw new IllegalArgumentException(
                                "Invalid Link line in file" +
                                file + ", line: [" + line + "]");
                        }
                    } else {
                        // skip unknown line
                    }
                }

            } catch (Exception ex) {
                throw new RuntimeException("Failed while processing file [" + file +
                                           "]", ex);
            }
        }
    }

    private String[] split(String str) {
        int off = 0;
        int end = str.length();
        ArrayList<String> list = new ArrayList<>(10);
        while (off < end) {
            char c = str.charAt(off);
            if (c == '\t' || c == ' ') {
                off++;
                continue;
            }
            if (c == '#') {    // comment
                break;
            }
            int start = off;
            while (off < end) {
                c = str.charAt(off);
                if (c == ' ' || c == '\t') {
                    break;
                }
                off++;
            }
            if (start != off) {
                list.add(str.substring(start, off));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Class representing a month-day-time in the TZDB file.
     */
    private static abstract class MonthDayTime {
        /** The month of the cutover. */
        Month month = Month.JANUARY;

        /** The day-of-month of the cutover. */
        int dayOfMonth = 1;

        /** Whether to adjust forwards. */
        boolean adjustForwards = true;

        /** The day-of-week of the cutover. */
        DayOfWeek dayOfWeek;

        /** The time of the cutover, in second of day */
        int secsOfDay = 0;

        /** Whether this is midnight end of day. */
        boolean endOfDay;

        /** The time definition of the cutover. */
        TimeDefinition timeDefinition = TimeDefinition.WALL;

        void adjustToForwards(int year) {
            if (adjustForwards == false && dayOfMonth > 0) {
                // weekDay<=monthDay case, don't have it in tzdb data for now
                LocalDate adjustedDate = LocalDate.of(year, month, dayOfMonth).minusDays(6);
                dayOfMonth = adjustedDate.getDayOfMonth();
                month = adjustedDate.getMonth();
                adjustForwards = true;
            }
        }

        LocalDateTime toDateTime(int year) {
            LocalDate date;
            if (dayOfMonth < 0) {
                int monthLen = month.length(IsoChronology.INSTANCE.isLeapYear(year));
                date = LocalDate.of(year, month, monthLen + 1 + dayOfMonth);
                if (dayOfWeek != null) {
                    date = date.with(TemporalAdjusters.previousOrSame(dayOfWeek));
                }
            } else {
                date = LocalDate.of(year, month, dayOfMonth);
                if (dayOfWeek != null) {
                    date = date.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                }
            }
            if (endOfDay) {
                date = date.plusDays(1);
            }
            return LocalDateTime.of(date, LocalTime.ofSecondOfDay(secsOfDay));
        }

        /**
         * Parses the MonthDaytime segment of a tzdb line.
         */
        private void parse(String[] tokens, int off) {
            month = parseMonth(tokens[off++]);
            if (off < tokens.length) {
                String dayRule = tokens[off++];
                if (dayRule.startsWith("last")) {
                    dayOfMonth = -1;
                    dayOfWeek = parseDayOfWeek(dayRule.substring(4));
                    adjustForwards = false;
                } else {
                    int index = dayRule.indexOf(">=");
                    if (index > 0) {
                        dayOfWeek = parseDayOfWeek(dayRule.substring(0, index));
                        dayRule = dayRule.substring(index + 2);
                    } else {
                        index = dayRule.indexOf("<=");
                        if (index > 0) {
                            dayOfWeek = parseDayOfWeek(dayRule.substring(0, index));
                            adjustForwards = false;
                            dayRule = dayRule.substring(index + 2);
                        }
                    }
                    dayOfMonth = Integer.parseInt(dayRule);
                    if (dayOfMonth < -28 || dayOfMonth > 31 || dayOfMonth == 0) {
                       throw new IllegalArgumentException(
                          "Day of month indicator must be between -28 and 31 inclusive excluding zero");
                    }
                }
                if (off < tokens.length) {
                    String timeStr = tokens[off++];
                    secsOfDay = parseSecs(timeStr);
                    if (secsOfDay == 86400) {
                        // time must be midnight when end of day flag is true
                        endOfDay = true;
                        secsOfDay = 0;
                    } else if (secsOfDay < 0 || secsOfDay > 86400) {
                        // beyond 0:00-24:00 range. Adjust the cutover date.
                        int beyondDays = secsOfDay / 86400;
                        secsOfDay %= 86400;
                        if (secsOfDay < 0) {
                            secsOfDay = 86400 + secsOfDay;
                            beyondDays -= 1;
                        }
                        LocalDate date = LocalDate.of(2004, month, dayOfMonth).plusDays(beyondDays);  // leap-year
                        month = date.getMonth();
                        dayOfMonth = date.getDayOfMonth();
                        if (dayOfWeek != null) {
                            dayOfWeek = dayOfWeek.plus(beyondDays);
                        }
                    }
                    timeDefinition = parseTimeDefinition(timeStr.charAt(timeStr.length() - 1));
                }
            }
        }

        int parseYear(String year, int defaultYear) {
            switch (year.toLowerCase()) {
            case "min":  return 1900;
            case "max":  return Year.MAX_VALUE;
            case "only": return defaultYear;
            }
            return Integer.parseInt(year);
        }

        Month parseMonth(String mon) {
            switch (mon) {
            case "Jan": return Month.JANUARY;
            case "Feb": return Month.FEBRUARY;
            case "Mar": return Month.MARCH;
            case "Apr": return Month.APRIL;
            case "May": return Month.MAY;
            case "Jun": return Month.JUNE;
            case "Jul": return Month.JULY;
            case "Aug": return Month.AUGUST;
            case "Sep": return Month.SEPTEMBER;
            case "Oct": return Month.OCTOBER;
            case "Nov": return Month.NOVEMBER;
            case "Dec": return Month.DECEMBER;
            }
            throw new IllegalArgumentException("Unknown month: " + mon);
        }

        DayOfWeek parseDayOfWeek(String dow) {
            switch (dow) {
            case "Mon": return DayOfWeek.MONDAY;
            case "Tue": return DayOfWeek.TUESDAY;
            case "Wed": return DayOfWeek.WEDNESDAY;
            case "Thu": return DayOfWeek.THURSDAY;
            case "Fri": return DayOfWeek.FRIDAY;
            case "Sat": return DayOfWeek.SATURDAY;
            case "Sun": return DayOfWeek.SUNDAY;
            }
            throw new IllegalArgumentException("Unknown day-of-week: " + dow);
        }

        String parseOptional(String str) {
            return str.equals("-") ? null : str;
        }

        static final boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private int parseSecs(String time) {
            if (time.equals("-")) {
                return 0;
            }
            // faster hack
            int secs = 0;
            int sign = 1;
            int off = 0;
            int len = time.length();
            if (off < len && time.charAt(off) == '-') {
                sign = -1;
                off++;
            }
            char c0, c1;
            if (off < len && isDigit(c0 = time.charAt(off++))) {
                int hour = c0 - '0';
                if (off < len && isDigit(c1 = time.charAt(off))) {
                    hour = hour * 10 + c1 - '0';
                    off++;
                }
                secs = hour * 60 * 60;
                if (off < len && time.charAt(off++) == ':') {
                    if (off + 1 < len &&
                        isDigit(c0 = time.charAt(off++)) &&
                        isDigit(c1 = time.charAt(off++))) {
                        // minutes
                        secs += ((c0 - '0') * 10 + c1 - '0') * 60;
                        if (off < len && time.charAt(off++) == ':') {
                            if (off + 1 < len &&
                                isDigit(c0 = time.charAt(off++)) &&
                                isDigit(c1 = time.charAt(off++))) {
                                // seconds
                                secs += ((c0 - '0') * 10 + c1 - '0');
                            }
                        }
                    }

                }
                return secs * sign;
            }
            throw new IllegalArgumentException("[" + time + "]");
        }

        int parseOffset(String str) {
            int secs = parseSecs(str);
            if (Math.abs(secs) > 18 * 60 * 60) {
                throw new IllegalArgumentException(
                    "Zone offset not in valid range: -18:00 to +18:00");
            }
            return secs;
        }

        int parsePeriod(String str) {
            return parseSecs(str);
        }

        TimeDefinition parseTimeDefinition(char c) {
            switch (c) {
            case 's':
            case 'S':
                // standard time
                return TimeDefinition.STANDARD;
            case 'u':
            case 'U':
            case 'g':
            case 'G':
            case 'z':
            case 'Z':
                // UTC
                return TimeDefinition.UTC;
            case 'w':
            case 'W':
            default:
                // wall time
                return TimeDefinition.WALL;
            }
        }
    }

    /**
     * Class representing a rule line in the TZDB file.
     */
    private static class RuleLine extends MonthDayTime {
        /** The start year. */
        int startYear;

        /** The end year. */
        int endYear;

        /** The amount of savings, in seconds. */
        int savingsAmount;

        /** The text name of the zone. */
        String text;

        /**
         * Converts this to a transition rule.
         *
         * @param standardOffset  the active standard offset, not null
         * @param savingsBeforeSecs  the active savings before the transition in seconds
         * @param negativeSavings minimum savings in the rule, usually zero, but negative if negative DST is
         *                   in effect.
         * @return the transition, not null
        */
        ZoneOffsetTransitionRule toTransitionRule(ZoneOffset stdOffset, int savingsBefore, int negativeSavings) {
            // rule shared by different zones, so don't change it
            Month month = this.month;
            int dayOfMonth = this.dayOfMonth;
            DayOfWeek dayOfWeek = this.dayOfWeek;
            boolean endOfDay = this.endOfDay;

            // optimize stored format
            if (dayOfMonth < 0) {
                if (month != Month.FEBRUARY) {    // not Month.FEBRUARY
                    dayOfMonth = month.maxLength() - 6;
                }
            }
            if (endOfDay && dayOfMonth > 0 &&
                (dayOfMonth == 28 && month == Month.FEBRUARY) == false) {
                LocalDate date = LocalDate.of(2004, month, dayOfMonth).plusDays(1);  // leap-year
                month = date.getMonth();
                dayOfMonth = date.getDayOfMonth();
                if (dayOfWeek != null) {
                    dayOfWeek = dayOfWeek.plus(1);
                }
                endOfDay = false;
            }

            // build rule
            return ZoneOffsetTransitionRule.of(
                    //month, dayOfMonth, dayOfWeek, time, endOfDay, timeDefinition,
                    month, dayOfMonth, dayOfWeek,
                    LocalTime.ofSecondOfDay(secsOfDay), endOfDay, timeDefinition,
                    stdOffset,
                    ZoneOffset.ofTotalSeconds(stdOffset.getTotalSeconds() + savingsBefore),
                    ZoneOffset.ofTotalSeconds(stdOffset.getTotalSeconds() + savingsAmount - negativeSavings));
        }

        RuleLine parse(String[] tokens) {
            startYear = parseYear(tokens[2], 0);
            endYear = parseYear(tokens[3], startYear);
            if (startYear > endYear) {
                throw new IllegalArgumentException(
                    "Invalid <Rule> line/Year order invalid:" + startYear + " > " + endYear);
            }
            //parseOptional(s.next());  // type is unused
            super.parse(tokens, 5);     // monthdaytime parsing
            savingsAmount = parsePeriod(tokens[8]);
            //rule.text = parseOptional(s.next());
            return this;
        }
    }

    /**
     * Class representing a linked set of zone lines in the TZDB file.
     */
    private static class ZoneLine extends MonthDayTime {
        /** The standard offset. */
        int stdOffsetSecs;

        /** The fixed savings amount. */
        int fixedSavingsSecs = 0;

        /** The savings rule. */
        String savingsRule;

        /** The text name of the zone. */
        String text;

        /** The cutover year */
        int year = Year.MAX_VALUE;

        /** The cutover date time */
        LocalDateTime ldt;

        /** The cutover date/time in epoch seconds/UTC */
        long ldtSecs = Long.MIN_VALUE;

        LocalDateTime toDateTime() {
            if (ldt == null) {
                ldt = toDateTime(year);
            }
            return ldt;
        }

        /**
         * Creates the date-time epoch second in the wall offset for the local
         * date-time at the end of the window.
         *
         * @param savingsSecs  the amount of savings in use in seconds
         * @return the created date-time epoch second in the wall offset, not null
         */
        long toDateTimeEpochSecond(int savingsSecs) {
            if (ldtSecs == Long.MIN_VALUE) {
                ldtSecs = toDateTime().toEpochSecond(ZoneOffset.UTC);
            }
            switch(timeDefinition) {
            case UTC:      return ldtSecs;
            case STANDARD: return ldtSecs - stdOffsetSecs;
            default:       return ldtSecs - (stdOffsetSecs + savingsSecs); // WALL
            }
        }

        boolean parse(String[] tokens, int off) {
            stdOffsetSecs = parseOffset(tokens[off++]);
            savingsRule = parseOptional(tokens[off++]);
            if (savingsRule != null && savingsRule.length() > 0 &&
                (savingsRule.charAt(0) == '-' || isDigit(savingsRule.charAt(0)))) {
                try {
                    fixedSavingsSecs = parsePeriod(savingsRule);
                    savingsRule = null;
                } catch (Exception ex) {
                    fixedSavingsSecs = 0;
                }
            }
            text = tokens[off++];
            if (off < tokens.length) {
                year = Integer.parseInt(tokens[off++]);
                if (off < tokens.length) {
                    super.parse(tokens, off);  // MonthDayTime
                }
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Class representing a rule line in the TZDB file for a particular year.
     */
    private static class TransRule implements Comparable<TransRule>
    {
        private int year;
        private RuleLine rule;

        /** The trans date/time */
        private LocalDateTime ldt;

        /** The trans date/time in epoch seconds (assume UTC) */
        long ldtSecs;

        TransRule(int year, RuleLine rule) {
            this.year = year;
            this.rule = rule;
            this.ldt = rule.toDateTime(year);
            this.ldtSecs = ldt.toEpochSecond(ZoneOffset.UTC);
        }

        ZoneOffsetTransition toTransition(ZoneOffset standardOffset, int savingsBeforeSecs, int negativeSavings) {
            // copy of code in ZoneOffsetTransitionRule to avoid infinite loop
            ZoneOffset wallOffset = ZoneOffset.ofTotalSeconds(
                standardOffset.getTotalSeconds() + savingsBeforeSecs);
            ZoneOffset offsetAfter = ZoneOffset.ofTotalSeconds(
                standardOffset.getTotalSeconds() + rule.savingsAmount - negativeSavings);
            LocalDateTime dt = rule.timeDefinition
                                   .createDateTime(ldt, standardOffset, wallOffset);
            return ZoneOffsetTransition.of(dt, wallOffset, offsetAfter);
        }

        long toEpochSecond(ZoneOffset stdOffset, int savingsBeforeSecs) {
            switch(rule.timeDefinition) {
            case UTC:      return ldtSecs;
            case STANDARD: return ldtSecs - stdOffset.getTotalSeconds();
            default:       return ldtSecs - (stdOffset.getTotalSeconds() + savingsBeforeSecs); // WALL
            }
        }

        /**
         * Tests if this a real transition with the active savings in seconds
         *
         * @param savingsBefore the active savings in seconds
         * @param negativeSavings minimum savings in the rule, usually zero, but negative if negative DST is
         *                   in effect.
         * @return true, if savings changes
         */
        boolean isTransition(int savingsBefore, int negativeSavings) {
            return rule.savingsAmount - negativeSavings != savingsBefore;
        }

        public int compareTo(TransRule other) {
            return (ldtSecs < other.ldtSecs)? -1 : ((ldtSecs == other.ldtSecs) ? 0 : 1);
        }
    }

    private ZoneRules buildRules(String zoneId, List<ZoneLine> zones) {
        if (zones.isEmpty()) {
            throw new IllegalStateException("No available zone window");
        }
        final List<ZoneOffsetTransition> standardTransitionList = new ArrayList<>(4);
        final List<ZoneOffsetTransition> transitionList = new ArrayList<>(256);
        final List<ZoneOffsetTransitionRule> lastTransitionRuleList = new ArrayList<>(2);

        final ZoneLine zone0 = zones.get(0);
        // initialize the standard offset, wallOffset and savings for loop

        //ZoneOffset stdOffset = zone0.standardOffset;
        ZoneOffset stdOffset = ZoneOffset.ofTotalSeconds(zone0.stdOffsetSecs);

        int savings = zone0.fixedSavingsSecs;
        ZoneOffset wallOffset = ZoneOffset.ofTotalSeconds(stdOffset.getTotalSeconds() + savings);

        // start ldt of each zone window
        LocalDateTime zoneStart = LocalDateTime.MIN;

        // first standard offset
        ZoneOffset firstStdOffset = stdOffset;
        // first wall offset
        ZoneOffset firstWallOffset = wallOffset;

        for (ZoneLine zone : zones) {
            // Adjust stdOffset, if negative DST is observed. It should be either
            // fixed amount, or expressed in the named Rules.
            int negativeSavings = Math.min(zone.fixedSavingsSecs, findNegativeSavings(zoneStart, zone));
            if (negativeSavings < 0) {
                zone.stdOffsetSecs += negativeSavings;
                if (zone.fixedSavingsSecs < 0) {
                    zone.fixedSavingsSecs = 0;
                }
            }

            // check if standard offset changed, update it if yes
            ZoneOffset stdOffsetPrev = stdOffset;  // for effectiveSavings check
            if (zone.stdOffsetSecs != stdOffset.getTotalSeconds()) {
                ZoneOffset stdOffsetNew = ZoneOffset.ofTotalSeconds(zone.stdOffsetSecs);
                standardTransitionList.add(
                    ZoneOffsetTransition.of(
                        LocalDateTime.ofEpochSecond(zoneStart.toEpochSecond(wallOffset),
                                                    0,
                                                    stdOffset),
                        stdOffset,
                        stdOffsetNew));
                stdOffset = stdOffsetNew;
            }

            LocalDateTime zoneEnd;
            if (zone.year == Year.MAX_VALUE) {
                zoneEnd = LocalDateTime.MAX;
            } else {
                zoneEnd = zone.toDateTime();
            }
            if (zoneEnd.compareTo(zoneStart) < 0) {
                throw new IllegalStateException("Windows must be in date-time order: " +
                        zoneEnd + " < " + zoneStart);
            }
            // calculate effective savings at the start of the window
            List<TransRule> trules = null;
            List<TransRule> lastRules = null;

            int effectiveSavings = zone.fixedSavingsSecs;
            if (zone.savingsRule != null) {
                List<RuleLine> tzdbRules = rules.get(zone.savingsRule);
                if (tzdbRules == null) {
                   throw new IllegalArgumentException("<Rule> not found: " +
                                                       zone.savingsRule);
                }
                trules = new ArrayList<>(256);
                lastRules = new ArrayList<>(2);
                int lastRulesStartYear = Year.MIN_VALUE;

                // merge the rules to transitions
                for (RuleLine rule : tzdbRules) {
                    if (rule.startYear > zoneEnd.getYear()) {
                        // rules will not be used for this zone entry
                        continue;
                    }
                    rule.adjustToForwards(2004);  // irrelevant, treat as leap year

                    int startYear = rule.startYear;
                    int endYear = rule.endYear;
                    if (zoneEnd.equals(LocalDateTime.MAX)) {
                        if (endYear == Year.MAX_VALUE) {
                            endYear = startYear;
                            lastRules.add(new TransRule(endYear, rule));
                        }
                        lastRulesStartYear = Math.max(startYear, lastRulesStartYear);
                    } else {
                        if (endYear == Year.MAX_VALUE) {
                            //endYear = zoneEnd.getYear();
                            endYear = zone.year;
                        }
                    }
                    int year = startYear;
                    while (year <= endYear) {
                        trules.add(new TransRule(year, rule));
                        year++;
                    }
                }

                // last rules, fill the gap years between different last rules
                if (zoneEnd.equals(LocalDateTime.MAX)) {
                    lastRulesStartYear = Math.max(lastRulesStartYear, zoneStart.getYear()) + 1;
                    for (TransRule rule : lastRules) {
                        if (rule.year <= lastRulesStartYear) {
                            int year = rule.year;
                            while (year <= lastRulesStartYear) {
                                trules.add(new TransRule(year, rule.rule));
                                year++;
                            }
                            rule.year = lastRulesStartYear;
                            rule.ldt = rule.rule.toDateTime(year);
                            rule.ldtSecs = rule.ldt.toEpochSecond(ZoneOffset.UTC);
                        }
                    }
                    Collections.sort(lastRules);
                }
                // sort the merged rules
                Collections.sort(trules);

                effectiveSavings = -negativeSavings;
                for (TransRule rule : trules) {
                    if (rule.toEpochSecond(stdOffsetPrev, savings) >
                        zoneStart.toEpochSecond(wallOffset)) {
                        // previous savings amount found, which could be the
                        // savings amount at the instant that the window starts
                        // (hence isAfter)
                        break;
                    }
                    effectiveSavings = rule.rule.savingsAmount - negativeSavings;
                }
            }
            // check if the start of the window represents a transition
            ZoneOffset effectiveWallOffset =
                ZoneOffset.ofTotalSeconds(stdOffset.getTotalSeconds() + effectiveSavings);

            if (!wallOffset.equals(effectiveWallOffset)) {
                transitionList.add(ZoneOffsetTransition.of(zoneStart,
                                                           wallOffset,
                                                           effectiveWallOffset));
            }
            savings = effectiveSavings;
            // apply rules within the window
            if (trules != null) {
                long zoneStartEpochSecs = zoneStart.toEpochSecond(wallOffset);
                for (TransRule trule : trules) {
                    if (trule.isTransition(savings, negativeSavings)) {
                        long epochSecs = trule.toEpochSecond(stdOffset, savings);
                        if (epochSecs < zoneStartEpochSecs ||
                            epochSecs >= zone.toDateTimeEpochSecond(savings)) {
                            continue;
                        }
                        transitionList.add(trule.toTransition(stdOffset, savings, negativeSavings));
                        savings = trule.rule.savingsAmount - negativeSavings;
                    }
                }
            }
            if (lastRules != null) {
                for (TransRule trule : lastRules) {
                    lastTransitionRuleList.add(trule.rule.toTransitionRule(stdOffset, savings, negativeSavings));
                    savings = trule.rule.savingsAmount - negativeSavings;
                }
            }

            // finally we can calculate the true end of the window, passing it to the next window
            wallOffset = ZoneOffset.ofTotalSeconds(stdOffset.getTotalSeconds() + savings);
            zoneStart = LocalDateTime.ofEpochSecond(zone.toDateTimeEpochSecond(savings),
                                                    0,
                                                    wallOffset);
        }
        return new ZoneRules(firstStdOffset,
                             firstWallOffset,
                             standardTransitionList,
                             transitionList,
                             lastTransitionRuleList);
    }

    /**
     * Find the minimum negative savings in named Rules for a Zone. Savings are only
     * looked at for the period of the subject Zone.
     *
     * @param zoneStart start LDT of the zone
     * @param zl ZoneLine to look at
     */
    private int findNegativeSavings(LocalDateTime zoneStart, ZoneLine zl) {
        int negativeSavings = 0;
        LocalDateTime zoneEnd = zl.toDateTime();

        if (zl.savingsRule != null) {
            List<RuleLine> rlines = rules.get(zl.savingsRule);
            if (rlines == null) {
                throw new IllegalArgumentException("<Rule> not found: " +
                        zl.savingsRule);
            }

            negativeSavings = Math.min(0, rlines.stream()
                    .filter(l -> windowOverlap(l, zoneStart.getYear(), zoneEnd.getYear()))
                    .map(l -> l.savingsAmount)
                    .min(Comparator.naturalOrder())
                    .orElse(0));
        }

        return negativeSavings;
    }

    private boolean windowOverlap(RuleLine ruleLine, int zoneStartYear, int zoneEndYear) {
        boolean overlap = zoneStartYear <= ruleLine.startYear && zoneEndYear >= ruleLine.startYear ||
                          zoneStartYear <= ruleLine.endYear && zoneEndYear >= ruleLine.endYear;

        return overlap;
    }
}
