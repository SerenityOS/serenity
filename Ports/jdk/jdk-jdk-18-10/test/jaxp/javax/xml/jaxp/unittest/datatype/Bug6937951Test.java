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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6937951
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.Bug6937951Test
 * @run testng/othervm datatype.Bug6937951Test
 * @summary Test midnight is same as the start of the next day in XMLGregorianCalendar.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6937951Test {

    @Test
    public void test() throws DatatypeConfigurationException {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar c1 = dtf.newXMLGregorianCalendar("1999-12-31T24:00:00");
        XMLGregorianCalendar c2 = dtf.newXMLGregorianCalendar("2000-01-01T00:00:00");
        System.out.println("c1: " + c1.getYear() + "-" + c1.getMonth() + "-" + c1.getDay() + "T" + c1.getHour());
        System.out.println(c1.equals(c2) ? "pass" : "fail"); // fails
        if (!c1.equals(c2))
            Assert.fail("hour 24 needs to be treated as equal to hour 0 of the next day");
        if (c1.getYear() != 2000 && c1.getHour() != 0)
            Assert.fail("hour 24 needs to be treated as equal to hour 0 of the next day");

    }

}
