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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * ZoneRec hold information of time zone corresponding to each text
 * line of the "Zone" part.
 *
 * @since 1.4
 */
class ZoneRec {
    private int gmtOffset;
    private String ruleName;
    private int directSave;
    private Rule ruleRef;
    private String format;
    private boolean hasUntil;
    private int untilYear;
    private Month untilMonth;
    private RuleDay untilDay;
    private Time untilTime;
    private long untilInMillis;
    private String line;

    /**
     * @return the "UNTIL" value in milliseconds
     */
    Time getUntilTime() {
        return untilTime;
    }

    /**
     * @return the GMT offset value in milliseconds
     */
    int getGmtOffset() {
        return gmtOffset;
    }

    /**
     * @return the rule name to which this zone record refers
     */
    String getRuleName() {
        return ruleName;
    }

    /**
     * @return the amount of saving time directly defined in the
     * "RULES/SAVE" field.
     */
    int getDirectSave() {
        return directSave;
    }

    /**
     * @return true if this zone record has a reference to a rule
     */
    boolean hasRuleReference() {
        return ruleRef != null;
    }

    /**
     * Returns the "FORMAT" field string of this zone record. This
     * @return the "FORMAT" field
     */
    String getFormat() {
        return format;
    }

    /**
     * @return the year in the "UNTIL" field
     */
    int getUntilYear() {
        return untilYear;
    }

    /**
     * Returns the "UNTIL" field value in milliseconds from Janurary
     * 1, 1970 0:00 GMT.
     * @param currentSave the amount of daylight saving in
     * milliseconds that is used to adjust wall-clock time.
     * @return the milliseconds value of the "UNTIL" field
     */
    long getUntilTime(int currentSave) {
        if (untilTime.isWall()) {
            return untilInMillis - currentSave;
        }
        return untilInMillis;
    }

    /**
     * Returns the "UNTIL" time in milliseconds without adjusting GMT
     * offsets or daylight saving.
     * @return local "UNTIL" time in milliseconds
     */
    long getLocalUntilTime() {
        return Time.getLocalTime(untilYear,
                                 untilMonth,
                                 untilDay,
                                 untilTime.getTime());
    }

    /**
     * Returns the "UNTIL" time in milliseconds with adjusting GMT offsets and daylight saving.
     * @return the "UNTIL" time after the adjustment
     */
    long getLocalUntilTime(int save, int gmtOffset) {
        return Time.getLocalTime(untilYear,
                                 untilMonth,
                                 untilDay,
                                 save,
                                 gmtOffset,
                                 untilTime);
    }

    /**
     * @return the text line of this zone record
     */
    String getLine() {
        return line;
    }

    /**
     * Sets the specified text line to this zone record
     */
    void setLine(String line) {
        this.line = line;
    }

    /**
     * @return true if this zone record has the "UNTIL" field
     */
    boolean hasUntil() {
        return this.hasUntil;
    }

    /**
     * Adjusts the "UNTIL" time to GMT offset if this zone record has
     * it.  <code>untilTime</code> is not adjusted to daylight saving
     * in this method.
     */
    void adjustTime() {
        if (!hasUntil()) {
            return;
        }
        if (untilTime.isSTD() || untilTime.isWall()) {
            // adjust to gmt offset only here.  adjust to real
            // wall-clock time when tracking rules
            untilInMillis -= gmtOffset;
        }
    }

    /**
     * @return the reference to the Rule object
     */
    Rule getRuleRef() {
        return ruleRef;
    }

    /**
     * Resolves the reference to a Rule and adjusts its "UNTIL" time
     * to GMT offset.
     */
    void resolve(Zoneinfo zi) {
        if (ruleName != null && (!"-".equals(ruleName))) {
                ruleRef = zi.getRule(ruleName);
        }
        adjustTime();
    }

    /**
     * Parses a Zone text line that is described by a StringTokenizer.
     * @param tokens represents tokens of a Zone text line
     * @return the zone record produced by parsing the text
     */
    static ZoneRec parse(StringTokenizer tokens) {
        ZoneRec rec = new ZoneRec();
        try {
            rec.gmtOffset = (int) Time.parse(tokens.nextToken()).getTime();
            String token = tokens.nextToken();
            char c = token.charAt(0);
            if (c >= '0' && c <= '9') {
                rec.directSave = (int) Time.parse(token).getTime();
            } else {
                rec.ruleName = token;
            }
            rec.format = tokens.nextToken();
            if (tokens.hasMoreTokens()) {
                rec.hasUntil = true;
                rec.untilYear = Integer.parseInt(tokens.nextToken());
                if (tokens.hasMoreTokens()) {
                    rec.untilMonth = Month.parse(tokens.nextToken());
                } else {
                    rec.untilMonth = Month.JANUARY;
                }
                if (tokens.hasMoreTokens()) {
                    rec.untilDay = RuleDay.parse(tokens.nextToken());
                } else {
                    rec.untilDay = new RuleDay(1);
                }
                if (tokens.hasMoreTokens()) {
                    rec.untilTime = Time.parse(tokens.nextToken());
                } else {
                    rec.untilTime = Time.parse("0:00");
                }
                rec.untilInMillis = rec.getLocalUntilTime();
            }
        } catch (Exception e) {
            // TODO: error reporting
            e.printStackTrace();
        }
        return rec;
    }

    private static void panic(String msg) {
        Main.panic(msg);
    }
}
