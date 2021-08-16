/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package datatype;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 7042647
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.Bug7042647Test
 * @run testng/othervm datatype.Bug7042647Test
 * @summary Test getFirstDayOfWeek is correct after converting XMLGregorianCalendar to a GregorianCalendar.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug7042647Test {

    @Test
    public void test() throws DatatypeConfigurationException {
        XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(1970, 1, 1, 0, 0, 0, 0, 0);
        GregorianCalendar calendar = xmlCalendar.toGregorianCalendar();
        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        Calendar defaultCalendar = Calendar.getInstance();
        int defaultFirstDayOfWeek = defaultCalendar.getFirstDayOfWeek();
        if (firstDayOfWeek != defaultFirstDayOfWeek) {
            Assert.fail("Failed firstDayOfWeek=" + firstDayOfWeek + " != defaultFirstDayOfWeek=" + defaultFirstDayOfWeek);
        } else {
            System.out.println("Success firstDayOfWeek=" + firstDayOfWeek + " == defaultFirstDayOfWeek=" + defaultFirstDayOfWeek);
        }
    }

}
