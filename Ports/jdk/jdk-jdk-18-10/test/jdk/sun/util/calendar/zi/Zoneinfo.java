/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Zoneinfo provides javazic compiler front-end functionality.
 * @since 1.4
 */
class Zoneinfo {

    private static final int minYear = 1900;
    private static final int maxYear = 2037;
    private static final long minTime = Time.getLocalTime(minYear, Month.JANUARY, 1, 0);
    private static int startYear = minYear;
    private static int endYear = maxYear;

    /**
     * True if javazic should generate a list of SimpleTimeZone
     * instances for the SimpleTimeZone-based time zone support.
     */
    static boolean isYearForTimeZoneDataSpecified = false;

    /**
     * Zone name to Zone mappings
     */
    private Map<String,Zone> zones;

    /**
     * Rule name to Rule mappings
     */
    private Map<String,Rule> rules;

    /**
     * Alias name to real name mappings
     */
    private Map<String,String> aliases;

    /**
     * Constracts a Zoneinfo.
     */
    Zoneinfo() {
        zones = new HashMap<String,Zone>();
        rules = new HashMap<String,Rule>();
        aliases = new HashMap<String,String>();
    }

    /**
     * Adds the given zone to the list of Zones.
     * @param zone Zone to be added to the list.
     */
    void add(Zone zone) {
        String name = zone.getName();
        zones.put(name, zone);
    }

    /**
     * Adds the given rule to the list of Rules.
     * @param rule Rule to be added to the list.
     */
    void add(Rule rule) {
        String name = rule.getName();
        rules.put(name, rule);
    }

    /**
     * Puts the specifid name pair to the alias table.
     * @param name1 an alias time zone name
     * @param name2 the real time zone of the alias name
     */
    void putAlias(String name1, String name2) {
        aliases.put(name1, name2);
    }

    /**
     * Sets the given year for SimpleTimeZone list output.
     * This method is called when the -S option is specified.
     * @param year the year for which SimpleTimeZone list should be generated
     */
    static void setYear(int year) {
        setStartYear(year);
        setEndYear(year);
        isYearForTimeZoneDataSpecified = true;
    }

    /**
     * Sets the start year.
     * @param year the start year value
     * @throws IllegalArgumentException if the specified year value is
     * smaller than the minimum year or greater than the end year.
     */
    static void setStartYear(int year) {
        if (year < minYear || year > endYear) {
            throw new IllegalArgumentException("invalid start year specified: " + year);
        }
        startYear = year;
    }

    /**
     * @return the start year value
     */
    static int getStartYear() {
        return startYear;
    }

    /**
     * Sets the end year.
     * @param year the end year value
     * @throws IllegalArgumentException if the specified year value is
     * smaller than the start year or greater than the maximum year.
     */
    static void setEndYear(int year) {
        if (year < startYear || year > maxYear) {
            throw new IllegalArgumentException();
        }
        endYear = year;
    }

    /**
     * @return the end year value
     */
    static int getEndYear() {
        return endYear;
    }

    /**
     * @return the minimum year value
     */
    static int getMinYear() {
        return minYear;
    }

    /**
     * @return the maximum year value
     */
    static int getMaxYear() {
        return maxYear;
    }

    /**
     * @return the alias table
     */
    Map<String,String> getAliases() {
        return aliases;
    }

    /**
     * @return the Zone list
     */
    Map<String,Zone> getZones() {
        return zones;
    }

    /**
     * @return a Zone specified by name.
     * @param name a zone name
     */
    Zone getZone(String name) {
        return zones.get(name);
    }

    /**
     * @return a Rule specified by name.
     * @param name a rule name
     */
    Rule getRule(String name) {
        return rules.get(name);
    }

    private static String line;

    private static int lineNum;

    /**
     * Parses the specified time zone data file and creates a Zoneinfo
     * that has all Rules, Zones and Links (aliases) information.
     * @param fname the time zone data file name
     * @return a Zoneinfo object
     */
    static Zoneinfo parse(String fname) {
        BufferedReader in = null;
        try {
            FileReader fr = new FileReader(fname);
            in = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            panic("can't open file: "+fname);
        }
        Zoneinfo zi = new Zoneinfo();
        boolean continued = false;
        Zone zone = null;
        String l;
        lineNum = 0;

        try {
            while ((line = in.readLine()) != null) {
                lineNum++;
                // skip blank and comment lines
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }

                // trim trailing comments
                int rindex = line.lastIndexOf('#');
                if (rindex != -1) {
                    // take the data part of the line
                    l = line.substring(0, rindex);
                } else {
                    l = line;
                }

                StringTokenizer tokens = new StringTokenizer(l);
                if (!tokens.hasMoreTokens()) {
                    continue;
                }
                String token = tokens.nextToken();

                if (continued || "Zone".equals(token)) {
                    if (zone == null) {
                        if (!tokens.hasMoreTokens()) {
                            panic("syntax error: zone no more token");
                        }
                        token = tokens.nextToken();
                        // if the zone name is in "GMT+hh" or "GMT-hh"
                        // format, ignore it due to spec conflict.
                        if (token.startsWith("GMT+") || token.startsWith("GMT-")) {
                            continue;
                        }
                        zone = new Zone(token);
                    } else {
                        // no way to push the current token back...
                        tokens = new StringTokenizer(l);
                    }

                    ZoneRec zrec = ZoneRec.parse(tokens);
                    zrec.setLine(line);
                    zone.add(zrec);
                    if ((continued = zrec.hasUntil()) == false) {
                        if (Zone.isTargetZone(zone.getName())) {
                            // zone.resolve(zi);
                            zi.add(zone);
                        }
                        zone = null;
                    }
                } else if ("Rule".equals(token)) {
                    if (!tokens.hasMoreTokens()) {
                        panic("syntax error: rule no more token");
                    }
                    token = tokens.nextToken();
                    Rule rule = zi.getRule(token);
                    if (rule == null) {
                        rule = new Rule(token);
                        zi.add(rule);
                    }
                    RuleRec rrec = RuleRec.parse(tokens);
                    rrec.setLine(line);
                    rule.add(rrec);
                } else if ("Link".equals(token)) {
                    // Link <newname> <oldname>
                    try {
                        String name1 = tokens.nextToken();
                        String name2 = tokens.nextToken();

                        // if the zone name is in "GMT+hh" or "GMT-hh"
                        // format, ignore it due to spec conflict with
                        // custom time zones. Also, ignore "ROC" for
                        // PC-ness.
                        if (name2.startsWith("GMT+") || name2.startsWith("GMT-")
                            || "ROC".equals(name2)) {
                            continue;
                        }
                        zi.putAlias(name2, name1);
                    } catch (Exception e) {
                        panic("syntax error: no more token for Link");
                    }
                }
            }
            in.close();
        } catch (IOException ex) {
            panic("IO error: " + ex.getMessage());
        }

        return zi;
    }

    /**
     * Interprets a zone and constructs a Timezone object that
     * contains enough information on GMT offsets and DST schedules to
     * generate a zone info database.
     *
     * @param zoneName the zone name for which a Timezone object is
     * constructed.
     *
     * @return a Timezone object that contains all GMT offsets and DST
     * rules information.
     */
    Timezone phase2(String zoneName) {
        Timezone tz = new Timezone(zoneName);
        Zone zone = getZone(zoneName);
        zone.resolve(this);

        // TODO: merge phase2's for the regular and SimpleTimeZone ones.
        if (isYearForTimeZoneDataSpecified) {
            ZoneRec zrec = zone.get(zone.size()-1);
            tz.setLastZoneRec(zrec);
            tz.setRawOffset(zrec.getGmtOffset());
            if (zrec.hasRuleReference()) {
                /*
                 * This part assumes that the specified year is covered by
                 * the rules referred to by the last zone record.
                 */
                List<RuleRec> rrecs = zrec.getRuleRef().getRules(startYear);

                if (rrecs.size() == 2) {
                    // make sure that one is a start rule and the other is
                    // an end rule.
                    RuleRec r0 = rrecs.get(0);
                    RuleRec r1 = rrecs.get(1);
                    if (r0.getSave() == 0 && r1.getSave() > 0) {
                        rrecs.set(0, r1);
                        rrecs.set(1, r0);
                    } else if (!(r0.getSave() > 0 && r1.getSave() == 0)) {
                        rrecs = null;
                        Main.error(zoneName + ": rules for " +  startYear + " not found.");
                    }
                } else {
                    rrecs = null;
                }
                if (rrecs != null) {
                    tz.setLastRules(rrecs);
                }
            }
            return tz;
        }

        int gmtOffset;
        int year = minYear;
        int fromYear = year;
        long fromTime = Time.getLocalTime(startYear,
                                          Month.JANUARY,
                                          1, 0);

        // take the index 0 for the GMT offset of the last zone record
        ZoneRec zrec = zone.get(zone.size()-1);
        tz.getOffsetIndex(zrec.getGmtOffset());

        int lastGmtOffsetValue = -1;
        ZoneRec prevzrec = null;
        int currentSave = 0;
        boolean usedZone;
        for (int zindex = 0; zindex < zone.size(); zindex++) {
            zrec = zone.get(zindex);
            usedZone = false;
            gmtOffset = zrec.getGmtOffset();
            int stdOffset = zrec.getDirectSave();

            if (gmtOffset != lastGmtOffsetValue) {
                tz.setRawOffset(gmtOffset, fromTime);
                lastGmtOffsetValue = gmtOffset;
            }
            // If this is the last zone record, take the last rule info.
            if (!zrec.hasUntil()) {
                if (zrec.hasRuleReference()) {
                    tz.setLastRules(zrec.getRuleRef().getLastRules());
                } else if (stdOffset != 0) {
                    // in case the last rule is all year round DST-only
                    // (Asia/Amman once announced this rule.)
                    tz.setLastDSTSaving(stdOffset);
                }
            }
            if (!zrec.hasRuleReference()) {
                if (!zrec.hasUntil() || zrec.getUntilTime(stdOffset) >= fromTime) {
                    tz.addTransition(fromTime,
                                     tz.getOffsetIndex(gmtOffset+stdOffset),
                                     tz.getDstOffsetIndex(stdOffset));
                    usedZone = true;
                }
                currentSave = stdOffset;
                // optimization in case the last rule is fixed.
                if (!zrec.hasUntil()) {
                    if (tz.getNTransitions() > 0) {
                        if (stdOffset == 0) {
                            tz.setDSTType(Timezone.X_DST);
                        } else {
                            tz.setDSTType(Timezone.LAST_DST);
                        }
                        long time = Time.getLocalTime(maxYear,
                                                      Month.JANUARY, 1, 0);
                        time -= zrec.getGmtOffset();
                        tz.addTransition(time,
                                         tz.getOffsetIndex(gmtOffset+stdOffset),
                                         tz.getDstOffsetIndex(stdOffset));
                        tz.addUsedRec(zrec);
                    } else {
                        tz.setDSTType(Timezone.NO_DST);
                    }
                    break;
                }
            } else {
                Rule rule = zrec.getRuleRef();
                boolean fromTimeUsed = false;
                currentSave = 0;
            year_loop:
                for (year = getMinYear(); year <= endYear; year++) {
                    if (zrec.hasUntil() && year > zrec.getUntilYear()) {
                        break;
                    }
                    List<RuleRec> rules = rule.getRules(year);
                    if (rules.size() > 0) {
                        for (int i = 0; i < rules.size(); i++) {
                            RuleRec rrec = rules.get(i);
                            long transition = rrec.getTransitionTime(year,
                                                                     gmtOffset,
                                                                     currentSave);
                            if (zrec.hasUntil()) {
                                if (transition >= zrec.getUntilTime(currentSave)) {
                                    // If the GMT offset changed from the previous one,
                                    // record fromTime as a transition.
                                    if (!fromTimeUsed && prevzrec != null
                                        && gmtOffset != prevzrec.getGmtOffset()) {
                                        tz.addTransition(fromTime,
                                                         tz.getOffsetIndex(gmtOffset+currentSave),
                                                         tz.getDstOffsetIndex(currentSave));
                                        fromTimeUsed = true; // for consistency
                                    }
                                    break year_loop;
                                }
                            }

                            if (fromTimeUsed == false) {
                                if (fromTime <= transition) {
                                    fromTimeUsed = true;

                                    if (fromTime != minTime) {
                                        int prevsave;

                                        // See if until time in the previous
                                        // ZoneRec is the same thing as the
                                        // local time in the next rule.
                                        // (examples are Asia/Ashkhabad in 1991,
                                        // Europe/Riga in 1989)

                                        if (i > 0) {
                                            prevsave = rules.get(i-1).getSave();
                                        } else {
                                            List<RuleRec> prevrules = rule.getRules(year-1);

                                            if (prevrules.size() > 0) {
                                                prevsave = prevrules.get(prevrules.size()-1).getSave();
                                            } else {
                                                prevsave = 0;
                                            }
                                        }

                                        if (rrec.isSameTransition(prevzrec, prevsave, gmtOffset)) {
                                            currentSave = rrec.getSave();
                                            tz.addTransition(fromTime,
                                                         tz.getOffsetIndex(gmtOffset+currentSave),
                                                         tz.getDstOffsetIndex(currentSave));
                                            tz.addUsedRec(rrec);
                                            usedZone = true;
                                            continue;
                                        }
                                        if (!prevzrec.hasRuleReference()
                                            || rule != prevzrec.getRuleRef()
                                            || (rule == prevzrec.getRuleRef()
                                                && gmtOffset != prevzrec.getGmtOffset())) {
                                            int save = (fromTime == transition) ? rrec.getSave() : currentSave;
                                            tz.addTransition(fromTime,
                                                         tz.getOffsetIndex(gmtOffset+save),
                                                         tz.getDstOffsetIndex(save));
                                            tz.addUsedRec(rrec);
                                            usedZone = true;
                                        }
                                    } else {  // fromTime == minTime
                                        int save = rrec.getSave();
                                        tz.addTransition(minTime,
                                                         tz.getOffsetIndex(gmtOffset),
                                                         tz.getDstOffsetIndex(0));

                                        tz.addTransition(transition,
                                                         tz.getOffsetIndex(gmtOffset+save),
                                                         tz.getDstOffsetIndex(save));

                                        tz.addUsedRec(rrec);
                                        usedZone = true;
                                    }
                                } else if (year == fromYear && i == rules.size()-1) {
                                    int save = rrec.getSave();
                                    tz.addTransition(fromTime,
                                                     tz.getOffsetIndex(gmtOffset+save),
                                                     tz.getDstOffsetIndex(save));
                                }
                            }

                            currentSave = rrec.getSave();
                            if (fromTime < transition) {
                                tz.addTransition(transition,
                                                 tz.getOffsetIndex(gmtOffset+currentSave),
                                                 tz.getDstOffsetIndex(currentSave));
                                tz.addUsedRec(rrec);
                                usedZone = true;
                            }
                        }
                    } else {
                        if (year == fromYear) {
                            tz.addTransition(fromTime,
                                             tz.getOffsetIndex(gmtOffset+currentSave),
                                             tz.getDstOffsetIndex(currentSave));
                            fromTimeUsed = true;
                        }
                        if (year == endYear && !zrec.hasUntil()) {
                            if (tz.getNTransitions() > 0) {
                                // Assume that this Zone stopped DST
                                tz.setDSTType(Timezone.X_DST);
                                long time = Time.getLocalTime(maxYear, Month.JANUARY,
                                                              1, 0);
                                time -= zrec.getGmtOffset();
                                tz.addTransition(time,
                                                 tz.getOffsetIndex(gmtOffset),
                                                 tz.getDstOffsetIndex(0));
                                usedZone = true;
                            } else {
                                tz.setDSTType(Timezone.NO_DST);
                            }
                        }
                    }
                }
            }
            if (usedZone) {
                tz.addUsedRec(zrec);
            }
            if (zrec.hasUntil() && zrec.getUntilTime(currentSave) > fromTime) {
                fromTime = zrec.getUntilTime(currentSave);
                fromYear = zrec.getUntilYear();
                year = zrec.getUntilYear();
            }
            prevzrec = zrec;
        }

        if (tz.getDSTType() == Timezone.UNDEF_DST) {
            tz.setDSTType(Timezone.DST);
        }
        tz.optimize();
        tz.checksum();
        return tz;
    }

    private static void panic(String msg) {
        Main.panic(msg);
    }
}
