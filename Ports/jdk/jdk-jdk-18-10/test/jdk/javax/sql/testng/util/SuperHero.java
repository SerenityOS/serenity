/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package util;

import java.io.Serializable;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class SuperHero implements SQLData, Serializable {

    private String first;
    private String last;
    private String type = "SUPERHERO";
    private Integer firstYear;
    private String secretIdentity;

    public SuperHero() {

    }

    public SuperHero(String sqlType, String fname, String lname, Integer year,
            String identity) {
        first = fname;
        last = lname;
        type = sqlType;
        firstYear = year;
        secretIdentity = identity;
    }

    @Override
    public String getSQLTypeName() throws SQLException {
        return type;
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {
        first = stream.readString();
        last = stream.readString();
        firstYear = stream.readInt();
        secretIdentity = stream.readString();
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeString(first);
        stream.writeString(last);
        stream.writeInt(firstYear);
        stream.writeString(secretIdentity);
    }

    @Override
    public String toString() {
        return "[ name =" + first + " " + last + " "
                + firstYear + " " + secretIdentity + " ]";
    }

    public void setIdentity(String identity) {
        secretIdentity = identity;
    }

    public String getIdentity() {
        return secretIdentity;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SuperHero) {
            SuperHero ss = (SuperHero) obj;
            return first.equals(ss.first) && last.equals(ss.last)
                    && firstYear.equals(ss.firstYear)
                    && type.equals(ss.type)
                    && secretIdentity.equals(ss.secretIdentity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((31 + first.hashCode()) * 31) * 31
                + ((31 + last.hashCode()) * 31) * 31
                + ((31 + firstYear.hashCode()) * 31) * 31
                + ((31 + type.hashCode()) * 31) * 31
                + secretIdentity.hashCode();
    }
}
