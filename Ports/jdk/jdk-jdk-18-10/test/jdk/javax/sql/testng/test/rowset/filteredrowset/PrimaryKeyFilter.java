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

import javax.sql.RowSet;
import javax.sql.rowset.Predicate;

/*
 * Simple implementation of Predicate which is used to filter rows based
 * on the Primary Key.
 */
public class PrimaryKeyFilter implements Predicate {

    private final int lo;
    private final int hi;
    private String colName = null;
    private int colNumber = -1;

    public PrimaryKeyFilter(int lo, int hi, int colNumber) {
        this.lo = lo;
        this.hi = hi;
        this.colNumber = colNumber;
    }

    public PrimaryKeyFilter(int lo, int hi, String colName) {
        this.lo = lo;
        this.hi = hi;
        this.colName = colName;
    }

    public boolean evaluate(Object value, String columnName) {

        boolean result = false;
        if (columnName.equalsIgnoreCase(this.colName)) {
            int columnValue = ((Integer) value);
            result = (columnValue >= this.lo) && (columnValue <= this.hi);
        }
        return result;
    }

    public boolean evaluate(Object value, int columnNumber) {

        boolean result = false;
        if (this.colNumber == columnNumber) {
            int columnValue = (Integer) value;
            result = (columnValue >= this.lo) && (columnValue <= this.hi);
        }
        return result;
    }

    public boolean evaluate(RowSet rs) {

        boolean result = false;
        try {
            int columnValue = -1;

            if (this.colNumber > 0) {
                columnValue = rs.getInt(this.colNumber);
            } else if (this.colName != null) {
                columnValue = rs.getInt(this.colName);
            }
            if ((columnValue >= this.lo) && (columnValue <= this.hi)) {
                result = true;
            }

        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
            result = false;
        }
        return result;
    }

}
