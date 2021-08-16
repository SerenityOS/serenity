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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

public class TestSQLDataImpl implements SQLData {

    private final int stringPos = 0;
    private final int datePos = 1;
    private final int timePos = 2;
    private final int timestampPos = 3;
    private final int intPos = 4;
    private final int longPos = 5;
    private final int shortPos = 6;
    private final int bigDecimalPos = 7;
    private final int doublePos = 8;
    private final int booleanPos = 9;
    private final int floatPos = 10;
    private final int bytePos = 11;
    private final int bytesPos = 12;
    private final int MAX_TYPES = bytesPos + 1;
    private final Object[] types = new Object[MAX_TYPES];

    private final static byte[] b = {1, 2, 3};

    // attributes entries must line up with the ordering of the reading and
    // writing of the fields in readSQL and writeSQL
    public final static Object[] attributes = {"The Dark Knight",
        Date.valueOf(LocalDate.now()), Time.valueOf(LocalTime.now()),
        Timestamp.valueOf(LocalDateTime.now()), Integer.MAX_VALUE,
        Long.MAX_VALUE, Short.MIN_VALUE, BigDecimal.ONE,
        Double.MAX_VALUE, true, 1.5f, Byte.MAX_VALUE, b};

    private String sqlType;

    public TestSQLDataImpl(String type) {
        sqlType = type;
    }

    @Override
    public String getSQLTypeName() throws SQLException {
        return sqlType;
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {

        sqlType = typeName;
        types[stringPos] = stream.readString();
        types[datePos] = stream.readDate();
        types[timePos] = stream.readTime();
        types[timestampPos] = stream.readTimestamp();
        types[intPos] = stream.readInt();
        types[longPos] = stream.readLong();
        types[shortPos] = stream.readShort();
        types[bigDecimalPos] = stream.readBigDecimal();
        types[doublePos] = stream.readDouble();
        types[booleanPos] = stream.readBoolean();
        types[floatPos] = stream.readFloat();
        types[bytePos] = stream.readByte();
        types[bytesPos] = stream.readBytes();
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {

        stream.writeString((String) types[stringPos]);
        stream.writeDate((Date) types[datePos]);
        stream.writeTime((Time) types[timePos]);
        stream.writeTimestamp((Timestamp) types[timestampPos]);
        stream.writeInt((Integer) types[intPos]);
        stream.writeLong((Long) types[longPos]);
        stream.writeShort((Short) types[shortPos]);
        stream.writeBigDecimal((BigDecimal) types[bigDecimalPos]);
        stream.writeDouble((Double) types[doublePos]);
        stream.writeBoolean((Boolean) types[booleanPos]);
        stream.writeFloat((Float) types[floatPos]);
        stream.writeByte((Byte) types[bytePos]);
        stream.writeBytes((byte[]) types[bytesPos]);
    }

    public Object[] toArray() {

        Object[] result = Arrays.copyOf(types, types.length);
        return result;
    }

    @Override
    public String toString() {
        return "[" + sqlType + " " + types + "]";
    }

}
