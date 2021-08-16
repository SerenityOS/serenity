/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.rowset.filteredrowset;

import java.sql.SQLException;
import javax.sql.RowSet;
import javax.sql.rowset.Predicate;

/*
 * Simple implementation of Predicate which is used to filter rows based
 * on a City.
 */
public class CityFilter implements Predicate {

    private final String[] cities;
    private String colName = null;
    private int colNumber = -1;

    public CityFilter(String[] cities, String colName) {
        this.cities = cities;
        this.colName = colName;
    }

    public CityFilter(String[] cities, int colNumber) {
        this.cities = cities;
        this.colNumber = colNumber;
    }

    public boolean evaluate(Object value, String colName) {

        if (colName.equalsIgnoreCase(this.colName)) {
            for (String city : cities) {
                if (city.equalsIgnoreCase((String) value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean evaluate(Object value, int colNumber) {

        if (colNumber == this.colNumber) {
            for (String city : this.cities) {
                if (city.equalsIgnoreCase((String) value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean evaluate(RowSet rs) {

        boolean result = false;

        if (rs == null) {
            return false;
        }

        try {
            for (String city : cities) {

                String val = "";
                if (colNumber > 0) {
                    val = (String) rs.getObject(colNumber);
                } else if (colName != null) {
                    val = (String) rs.getObject(colName);
                }

                if (val.equalsIgnoreCase(city)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            result = false;
        }
        return result;
    }
}
