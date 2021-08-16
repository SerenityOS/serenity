/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package xwp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

public class DatatypeFactoryWrapper extends DatatypeFactory {
    private DatatypeFactory defaultImpl = DatatypeFactory.newDefaultInstance();

    @Override
    public Duration newDuration(String lexicalRepresentation) {
        return defaultImpl.newDuration(lexicalRepresentation);
    }

    @Override
    public Duration newDuration(long durationInMilliSeconds) {
        return defaultImpl.newDuration(durationInMilliSeconds);
    }

    @Override
    public Duration newDuration(boolean isPositive, BigInteger years, BigInteger months, BigInteger days,
            BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        return defaultImpl.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar() {
        return defaultImpl.newXMLGregorianCalendar();
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation) {
        return defaultImpl.newXMLGregorianCalendar(lexicalRepresentation);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal) {
        return defaultImpl.newXMLGregorianCalendar(cal);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(BigInteger year, int month, int day, int hour,
            int minute, int second, BigDecimal fractionalSecond, int timezone) {
        return defaultImpl.newXMLGregorianCalendar(year, month, day, hour, minute, second, fractionalSecond, timezone);
    }

}
