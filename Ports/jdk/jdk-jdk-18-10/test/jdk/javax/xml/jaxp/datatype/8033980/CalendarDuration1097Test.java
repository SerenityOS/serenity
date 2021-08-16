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

/**
 * @test
 * @bug 8033980
 * @summary verify that offset in minutes in getTimeZone is converted correctly
 * Note that JDK did not have this bug.
 * @run main CalendarDuration1097Test
 */

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 *
 * @author Joe Wang huizhe.wang@oracle.com
 */
public class CalendarDuration1097Test {

    /**
     * main method.
     *
     * @param args Standard args.
     */
    public static void main(String[] args) {
        try {
            String dateTimeString = "0001-01-01T00:00:00.0000000-05:00";
            DatatypeFactory dtf = DatatypeFactory.newInstance();
            XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar( dateTimeString );
            System.out.println( "Expected: 0001-01-01T00:00:00.0000000-05:00");
            System.out.println( "Actual:" + cal.toString() );
            System.out.println( "toXMLFormat:" + cal.toXMLFormat() );
            String test = cal.toString();
            if (test.indexOf("E-7") > -1) {
                throw new RuntimeException("Expected: 0001-01-01T00:00:00.0000000-05:00");
            }
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

}
