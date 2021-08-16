/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

 /*
 * @test
 * @bug 8024352
 * @modules java.management
 * @run main MBeanOperationInfoImpactRangeTest
 * @summary Check that MBeanOperationInfo throws an IllegalArgumentException when impact
 * value is not among INFO,ACTION,ACTION_INFO,UNKNOWN
 */
import javax.management.MBeanOperationInfo;

public class MBeanOperationInfoImpactRangeTest {

    private void checkInRange(int impact) {
        int impactValue;

        System.out.println("checking that no exception is thrown when a "
                + "value in range is passed, impact value is :" + impact );
        MBeanOperationInfo mbi = new MBeanOperationInfo("IRC", "impact Range"
                + " check", null, null, impact);
        impactValue = mbi.getImpact();
        if(impactValue != impact)
            throw new RuntimeException("unexpected impact value :" + impactValue);
        System.out.println("given value is :" + impactValue);
        System.out.println("Success no exception thrown");
        System.out.println(mbi.toString());

    }

    private void checkOutOfRange(int impact) {
        int impactValue;

        try {
            System.out.println("checking that exception is thrown when a value"
                    + " out of range is passed, impact value is :" + impact);
            MBeanOperationInfo mbi = new MBeanOperationInfo("IRC", "impact Range"
                    + " check", null, null, impact);
            impactValue = mbi.getImpact();
            System.out.println("IllegalArgumentException not thrown"
                    + " when a value out of range is passed ,"
                    + " given value is :" + impactValue);
            throw new RuntimeException("Test failed !!");
            // throwing RuntimeException for notifying the unusual behaviour
        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException thrown as expected, "
                    + "illegal value given as impact :" + impact);
            System.out.println("success");
        }

    }

    public static void main(String Args[]) {

        // valid range for impact is {INFO=0,ACTION=1,ACTION_INFO=2,UNKNOWN=3}
        /* MBeanOperationInfo should throw IllegalArgumentException when impact
        value is given out of range*/
        MBeanOperationInfoImpactRangeTest impactRangeTest = new MBeanOperationInfoImpactRangeTest();

        impactRangeTest.checkInRange(MBeanOperationInfo.INFO);
        impactRangeTest.checkInRange(MBeanOperationInfo.ACTION);
        impactRangeTest.checkInRange(MBeanOperationInfo.ACTION_INFO);
        impactRangeTest.checkInRange(MBeanOperationInfo.UNKNOWN);
        impactRangeTest.checkOutOfRange(-1);
        impactRangeTest.checkOutOfRange(4);

        System.out.println("Test Passed");


    }
}