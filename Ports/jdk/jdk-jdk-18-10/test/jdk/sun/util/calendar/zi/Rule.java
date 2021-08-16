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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Rule manipulates Rule records.
 *
 * @since 1.4
 */
class Rule {

    private List<RuleRec> list;
    private String name;

    /**
     * Constructs a Rule which consists of a Rule record list. The
     * specified name is given to this Rule.
     * @param name the Rule name
     */
    Rule(String name) {
        this.name = name;
        list = new ArrayList<RuleRec>();
    }

    /**
     * Added a RuleRec to the Rule record list.
     */
    void add(RuleRec rec) {
        list.add(rec);
    }

    /**
     * @return the Rule name
     */
    String getName() {
        return name;
    }

    /**
     * Gets all rule records that cover the given year.
     *
     * @param year the year number for which the rule is applicable.
     * @return rules in List that are collated in time. If no rule is found, an empty
     * List is returned.
     */
    List<RuleRec> getRules(int year) {
        List<RuleRec> rules = new ArrayList<RuleRec>(3);
        for (RuleRec rec : list) {
            if (year >= rec.getFromYear() && year <= rec.getToYear()) {
                if ((rec.isOdd() && year % 2 == 0) || (rec.isEven() && year % 2 == 1))
                    continue;
                rules.add(rec);
            }
        }
        int n = rules.size();
        if (n <= 1) {
            return rules;
        }
        if (n == 2) {
            RuleRec rec1 = rules.get(0);
            RuleRec rec2 = rules.get(1);
            if (rec1.getMonthNum() > rec2.getMonthNum()) {
                rules.set(0, rec2);
                rules.set(1, rec1);
            } else if (rec1.getMonthNum() == rec2.getMonthNum()) {
                // TODO: it's not accurate to ignore time types (STD, WALL, UTC)
                long t1 = Time.getLocalTime(year, rec1.getMonth(),
                                            rec1.getDay(), rec1.getTime().getTime());
                long t2 = Time.getLocalTime(year, rec2.getMonth(),
                                            rec2.getDay(), rec2.getTime().getTime());
                if (t1 > t2) {
                    rules.set(0, rec2);
                    rules.set(1, rec1);
                }
            }
            return rules;
        }

        final int y = year;
        RuleRec[] recs = new RuleRec[rules.size()];
        rules.toArray(recs);

        Arrays.sort(recs, new Comparator<RuleRec>() {
                public int compare(RuleRec r1, RuleRec r2) {
                    int n = r1.getMonthNum() - r2.getMonthNum();
                    if (n != 0) {
                        return n;
                    }
                    // TODO: it's not accurate to ignore time types (STD, WALL, UTC)
                    long t1 = Time.getLocalTime(y, r1.getMonth(),
                                                r1.getDay(), r1.getTime().getTime());
                    long t2 = Time.getLocalTime(y, r2.getMonth(),
                                                r2.getDay(), r2.getTime().getTime());
                    return Long.compare(t1, t2);
                }
                public boolean equals(Object o) {
                    return this == o;
                }
            });
        rules.clear();
        for (int i = 0; i < n; i++) {
            if (i != 0 && recs[i -1].getSave() == recs[i].getSave()) {
                // we have two recs back to back with same saving for the same year.
                if (recs[i].isLastRule()) {
                    continue;
                } else if (recs[i - 1].isLastRule()) {
                    rules.remove(rules.size() - 1);
                }
            }
            rules.add(recs[i]);
        }
        return rules;
    }

    /**
     * Gets rule records that have either "max" or cover the endYear
     * value in its DST schedule.
     *
     * @return rules that contain last DST schedule. An empty
     * ArrayList is returned if no last rules are found.
     */
    List<RuleRec> getLastRules() {
        RuleRec start = null;
        RuleRec end = null;

        for (int i = 0; i < list.size(); i++) {
            RuleRec rec = list.get(i);
            if (rec.isLastRule()) {
                if (rec.getSave() > 0) {
                    start = rec;
                } else {
                    end = rec;
                }
            }
        }
        if (start == null || end == null) {
            int endYear = Zoneinfo.getEndYear();
            for (int i  = 0; i < list.size(); i++) {
                RuleRec rec = list.get(i);
                if (endYear >= rec.getFromYear() && endYear <= rec.getToYear()) {
                    if (start == null && rec.getSave() > 0) {
                        start = rec;
                    } else {
                        if (end == null && rec.getSave() == 0) {
                            end = rec;
                        }
                    }
                }
            }
        }

        List<RuleRec> r = new ArrayList<RuleRec>(2);
        if (start == null || end == null) {
            if (start != null || end != null) {
                Main.warning("found last rules for "+name+" inconsistent.");
            }
            return r;
        }

        r.add(start);
        r.add(end);
        return r;
    }
}
