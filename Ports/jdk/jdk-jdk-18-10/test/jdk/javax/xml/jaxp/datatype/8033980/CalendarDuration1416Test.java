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
 * @summary test that invalid durations are caught
 * @run main CalendarDuration1416Test
 */

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;


/**
 *
 * @author Joe Wang huizhe.wang@oracle.com
 */
public class CalendarDuration1416Test {

    /**
     * main method.
     *
     * @param args Standard args.
     */
    public static void main(String[] args) {
        test1416("PT1D1H");
        test1416("PT1D1H30M");
        test1416("PT1D1H30S");
    }

    static void test1416(String d) {
        try
        {
            DatatypeFactory dtf = DatatypeFactory.newInstance();
            dtf.newDuration(d);
            throw new Error("no bug for " + d);
        } catch (DatatypeConfigurationException ex) {
            fail(ex.getMessage());
        }
        catch (NullPointerException e) {
            fail("NPE bug ! " + d);

        }
        catch(IllegalArgumentException e)
        {
            System.out.println("OK, BUG FIXED for " + d);
        }

    }

    static void fail(String errMessage) {
        throw new RuntimeException(errMessage);
    }
}
