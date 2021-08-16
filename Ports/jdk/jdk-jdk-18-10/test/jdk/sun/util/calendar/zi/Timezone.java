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

import java.util.ArrayList;
import java.util.List;

/**
 * Timezone represents all information of a single point of time to
 * generate its time zone database.
 *
 * @since 1.4
 */
class Timezone {
    /**
     * zone name of this time zone
     */
    private String name;

    /**
     * transition time values in UTC (millisecond)
     */
    private List<Long> transitions;

    /**
     * All offset values in millisecond
     * @see sun.util.calendar.ZoneInfo
     */
    private List<Integer> offsets;

    /**
     * Indices of GMT offset values (both raw and raw+saving)
     * at transitions
     */
    private List<Integer> gmtOffsets;

    /**
     * Indices of regular or "direct" saving time values
     * at transitions
     */
    private List<Integer> dstOffsets;

    /**
     * Zone records of this time zone
     */
    private List<ZoneRec> usedZoneRecs;

    /**
     * Rule records referred to by this time zone
     */
    private List<RuleRec> usedRuleRecs;

    /**
     * Type of DST rules in this time zone
     */
    private int dstType;
    static final int UNDEF_DST = 0;     // DST type not set yet
    static final int NO_DST = 1;        // never observed DST
    static final int LAST_DST = 2;      // last rule ends in DST (all year round DST-only)
    static final int X_DST = 3;         // used to observe DST
    static final int DST = 4;           // observing DST regularly

    /**
     * Raw GMT offset of this time zone in the last rule
     */
    private int rawOffset;

    /**
     * The CRC32 value of the transitions data
     */
    private int crc32;

    /**
     * The last ZoneRec
     */
    private ZoneRec lastZoneRec;

    /**
     * The last DST rules. lastRules[0] is the DST start
     * rule. lastRules[1] is the DST end rules.
     */
    private List<RuleRec> lastRules;

    /**
     * The amount of DST saving value (millisecond) in the last DST
     * rule.
     */
    private int lastSaving;

    /**
     * true if the raw offset will change in the future time.
     */
    private boolean willRawOffsetChange = false;


    /**
     * Constracts a Timezone object with the given zone name.
     * @param name the zone name
     */
    Timezone(String name) {
        this.name = name;
    }

    /**
     * @return the number of transitions
     */
    int getNTransitions() {
        if (transitions == null) {
            return 0;
        }
        return transitions.size();
    }

    /**
     * @return the zone name
     */
    String getName() {
        return name;
    }

    /**
     * Returns the list of all rule records that have been referred to
     * by this time zone.
     * @return the rule records list
     */
    List<RuleRec> getRules() {
        return usedRuleRecs;
    }

    /**
     * Returns the list of all zone records that have been referred to
     * by this time zone.
     * @return the zone records list
     */
    List<ZoneRec> getZones() {
        return usedZoneRecs;
    }

    /**
     * @return the transition table (list)
     */
    List<Long> getTransitions() {
        return transitions;
    }

    /**
     * @return the offsets list
     */
    List<Integer> getOffsets() {
        return offsets;
    }

    /**
     * @return the DST saving offsets list
     */
    List<Integer> getDstOffsets() {
        return dstOffsets;
    }

    /**
     * @return the GMT offsets list
     */
    List<Integer> getGmtOffsets() {
        return gmtOffsets;
    }

    /**
     * @return the checksum (crc32) value of the trasition table
     */
    int getCRC32() {
        return crc32;
    }

    /**
     * @return true if the GMT offset of this time zone would change
     * after the time zone database has been generated, false, otherwise.
     */
    boolean willGMTOffsetChange() {
        return willRawOffsetChange;
    }

    /**
     * @return the last known GMT offset value in milliseconds
     */
    int getRawOffset() {
        return rawOffset;
    }

    /**
     * Sets time zone's GMT offset to <code>offset</code>.
     * @param offset the GMT offset value in milliseconds
     */
    void setRawOffset(int offset) {
        rawOffset = offset;
    }

    /**
     * Sets time zone's GMT offset value to <code>offset</code>. If
     * <code>startTime</code> is future time, then the {@link
     * #willRawOffsetChange} value is set to true.
     * @param offset the GMT offset value in milliseconds
     * @param startTime the UTC time at which the GMT offset is in effective
     */
    void setRawOffset(int offset, long startTime) {
        // if this rawOffset is for the future time, let the run-time
        // look for the current GMT offset.
        if (startTime > Time.getCurrentTime()) {
            willRawOffsetChange = true;
        }
        setRawOffset(offset);
    }

    /**
     * Adds the specified transition information to the end of the transition table.
     * @param time the UTC time at which this transition happens
     * @param offset the total amount of the offset from GMT in milliseconds
     * @param dstOffset the amount of time in milliseconds saved at this transition
     */
    void addTransition(long time, int offset, int dstOffset) {
        if (transitions == null) {
            transitions = new ArrayList<Long>();
            offsets = new ArrayList<Integer>();
            dstOffsets = new ArrayList<Integer>();
        }
        transitions.add(time);
        offsets.add(offset);
        dstOffsets.add(dstOffset);
    }

    /**
     * Sets the type of historical daylight saving time
     * observation. For example, China used to observed daylight
     * saving time, but it no longer does. Then, X_DST is set to the
     * China time zone.
     * @param type the type of daylight saving time
     */
    void setDSTType(int type) {
        dstType = type;
    }

    /**
     * @return the type of historical daylight saving time
     * observation.
     */
    int getDSTType() {
        return dstType;
    }

    /**
     * Adds the specified zone record to the zone records list.
     * @param rec the zone record
     */
    void addUsedRec(ZoneRec rec) {
        if (usedZoneRecs == null) {
            usedZoneRecs = new ArrayList<ZoneRec>();
        }
        usedZoneRecs.add(rec);
    }

    /**
     * Adds the specified rule record to the rule records list.
     * @param rec the rule record
     */
    void addUsedRec(RuleRec rec) {
        if (usedRuleRecs == null) {
            usedRuleRecs = new ArrayList<RuleRec>();
        }
        // if the last used rec is the same as the given rec, avoid
        // putting the same rule.
        int n = usedRuleRecs.size();
        for (int i = 0; i < n; i++) {
            if (usedRuleRecs.get(i).equals(rec)) {
                return;
            }
        }
        usedRuleRecs.add(rec);
    }

    /**
     * Sets the last zone record for this time zone.
     * @param the last zone record
     */
    void setLastZoneRec(ZoneRec zrec) {
        lastZoneRec = zrec;
    }

    /**
     * @return the last zone record for this time zone.
     */
    ZoneRec getLastZoneRec() {
        return lastZoneRec;
    }

    /**
     * Sets the last rule records for this time zone. Those are used
     * for generating SimpleTimeZone parameters.
     * @param rules the last rule records
     */
    void setLastRules(List<RuleRec> rules) {
        int n = rules.size();
        if (n > 0) {
            lastRules = rules;
            RuleRec rec = rules.get(0);
            int offset = rec.getSave();
            if (offset > 0) {
                setLastDSTSaving(offset);
            } else {
                System.err.println("\t    No DST starting rule in the last rules.");
            }
        }
    }

    /**
     * @return the last rule records for this time zone.
     */
    List<RuleRec> getLastRules() {
        return lastRules;
    }

    /**
     * Sets the last daylight saving amount.
     * @param the daylight saving amount
     */
    void setLastDSTSaving(int offset) {
        lastSaving = offset;
    }

    /**
     * @return the last daylight saving amount.
     */
    int getLastDSTSaving() {
        return lastSaving;
    }

    /**
     * Calculates the CRC32 value from the transition table and sets
     * the value to <code>crc32</code>.
     */
    void checksum() {
        if (transitions == null) {
            crc32 = 0;
            return;
        }
        Checksum sum = new Checksum();
        for (int i = 0; i < transitions.size(); i++) {
            int offset = offsets.get(i);
            // adjust back to make the transition in local time
            sum.update(transitions.get(i) + offset);
            sum.update(offset);
            sum.update(dstOffsets.get(i));
        }
        crc32 = (int)sum.getValue();
    }

    /**
     * Removes unnecessary transitions for Java time zone support.
     */
    void optimize() {
        // if there is only one offset, delete all transitions. This
        // could happen if only time zone abbreviations changed.
        if (gmtOffsets.size() == 1) {
            transitions = null;
            usedRuleRecs =  null;
            setDSTType(NO_DST);
            return;
        }
        for (int i = 0; i < (transitions.size() - 2); i++) { // don't remove the last one
            if (transitions.get(i) == transitions.get(i+1)) {
                transitions.remove(i);
                offsets.remove(i);
                dstOffsets.remove(i);
                i--;
            }
        }

        for (int i = 0; i < (transitions.size() - 2); i++) { // don't remove the last one
            if (offsets.get(i) == offsets.get(i+1)
                && dstOffsets.get(i) == dstOffsets.get(i+1)) {
                transitions.remove(i+1);
                offsets.remove(i+1);
                dstOffsets.remove(i+1);
                i--;
            }
        }
    }

    /**
     * Stores the specified offset value from GMT in the GMT offsets
     * table and returns its index. The offset value includes the base
     * GMT offset and any additional daylight saving if applicable. If
     * the same value as the specified offset is already in the table,
     * its index is returned.
     * @param offset the offset value in milliseconds
     * @return the index to the offset value in the GMT offsets table.
     */
    int getOffsetIndex(int offset) {
        return getOffsetIndex(offset, 0);
    }

    /**
     * Stores the specified daylight saving value in the GMT offsets
     * table and returns its index. If the same value as the specified
     * offset is already in the table, its index is returned. If 0 is
     * specified, it's not stored in the table and -1 is returned.
     * @param offset the offset value in milliseconds
     * @return the index to the specified offset value in the GMT
     * offsets table, or -1 if 0 is specified.
     */
    int getDstOffsetIndex(int offset) {
        if (offset == 0) {
            return -1;
        }
        return getOffsetIndex(offset, 1);
    }

    private int getOffsetIndex(int offset, int index) {
        if (gmtOffsets == null) {
            gmtOffsets = new ArrayList<Integer>();
        }
        for (int i = index; i < gmtOffsets.size(); i++) {
            if (offset == gmtOffsets.get(i)) {
                return i;
            }
        }
        if (gmtOffsets.size() < index) {
            gmtOffsets.add(0);
        }
        gmtOffsets.add(offset);
        return gmtOffsets.size() - 1;
    }
}
