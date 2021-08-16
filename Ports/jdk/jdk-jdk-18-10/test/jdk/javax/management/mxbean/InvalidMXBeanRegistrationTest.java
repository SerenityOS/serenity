/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6333554
 * @summary Ensure the registration of an invalid MXBean
 *          throws NotCompliantMBeanException.
 * @author Luis-Miguel Alventosa
 *
 * @run clean InvalidMXBeanRegistrationTest
 * @run build InvalidMXBeanRegistrationTest
 * @run main InvalidMXBeanRegistrationTest
 */

import javax.management.*;

public class InvalidMXBeanRegistrationTest {

    // JMX compliant MXBean interface
    //
    @MXBean(true)
    public interface Compliant1 {}

    // JMX compliant MXBean interface
    //
    public interface Compliant2MXBean {}

    // JMX non-compliant MXBean (NotCompliantMBeanException must be thrown)
    //
    public static class Compliant implements Compliant1, Compliant2MXBean {}

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        Compliant mxbean = new Compliant();
        boolean ok;
        try {
            mbs.registerMBean(mxbean, on);
            System.out.println("Didn't get expected NotCompliantMBeanException");
            ok = false;
        } catch (NotCompliantMBeanException e) {
            System.out.println("Got expected exception: " + e);
            ok = true;
        } catch (Exception e) {
            System.out.println("Got unexpected exception: " + e);
            ok = false;
        }
        if (ok)
            System.out.println("Test PASSED");
        else {
            System.out.println("Test FAILED");
        }
    }
}
