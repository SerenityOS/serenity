/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test AnnotationSecurityTest.java
 * @bug 6366543 6370080
 * @summary Test that having a security manager doesn't trigger a
 *          NotCompliantMBeanException
 * @author Daniel Fuchs, Yves Joan
 *
 * @modules java.desktop
 *          java.management
 *
 * @run clean AnnotationSecurityTest Described UnDescribed DescribedMBean
 *            UnDescribedMBean SqeDescriptorKey DescribedMX DescribedMXBean
 * @run build AnnotationSecurityTest Described UnDescribed DescribedMBean
 *            UnDescribedMBean SqeDescriptorKey DescribedMX DescribedMXBean
 * @run main/othervm -Djava.security.manager=allow AnnotationSecurityTest
 */
// -Djava.security.debug=access,domain,policy

import java.io.File;
import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import javax.management.MBeanServer;
import javax.management.ObjectName;
/**
 *
 * @author Sun Microsystems, 2005 - All rights reserved.
 */

public class AnnotationSecurityTest {

    /** Creates a new instance of AnnotationSecurityTest */
    public AnnotationSecurityTest() {
    }

    public static void main(String[] argv) {
        AnnotationSecurityTest test = new AnnotationSecurityTest();
        test.run();
    }


    public void run() {
        try {
            final String testSrc = System.getProperty("test.src");
            final String codeBase = System.getProperty("test.classes");
            final String policy = testSrc + File.separator +
                    "AnnotationSecurityTest.policy";
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final File pf = new File(policy);
            if (!pf.exists())
                throw new IOException("policy file not found: " + policy);
            if (!pf.canRead())
                throw new IOException("policy file not readable: " + policy);

            System.out.println("Policy="+policy);
            System.setProperty("java.security.policy",policy);
            System.setSecurityManager(new SecurityManager());

            // We check that 6370080 is fixed.
            //
            try {
                final Method m1 =
                        DescribedMBean.class.getMethod("getStringProp");
                final Method m2 =
                        DescribedMBean.class.getMethod("setStringProp",
                            String.class);
                m1.getAnnotations();
                m2.getAnnotations();
            } catch (SecurityException x) {
                System.err.println("ERROR: 6370080 not fixed.");
                throw new IllegalStateException("ERROR: 6370080 not fixed.",x);
            }

            // Do the test: we should be able to register these 3 MBeans....
            // We now test that the behaviour described in 6366543 does no
            // longer appears now that 6370080 is fixed.
            //

            final ObjectName name1 =
                    new ObjectName("defaultDomain:class=UnDescribed");
            UnDescribed unDescribedMBean = new UnDescribed();
            System.out.println("\nWe register an MBean where DescriptorKey is " +
                    "not used at all");
            mbs.registerMBean(unDescribedMBean, name1);
            System.out.println("\n\tOK - The MBean "
                    + name1 + " is registered = " + mbs.isRegistered(name1));

            final ObjectName name2 =
                    new ObjectName("defaultDomain:class=Described");
            final Described describedMBean = new Described();

            System.out.println("\nWe register an MBean with exactly the " +
                    "same management"
                    + " interface as above and where DescriptorKey is used.");
            mbs.registerMBean(describedMBean, name2);
            System.out.println("\n\tOK - The MBean "
                    + name2 + " is registered = " + mbs.isRegistered(name2));

            final ObjectName name3 =
                    new ObjectName("defaultDomain:class=DescribedMX");
            final DescribedMX describedMXBean = new DescribedMX();

            System.out.println("\nWe register an MXBean with exactly the " +
                    "same management"
                    + " interface as above and where DescriptorKey is used.");
            mbs.registerMBean(describedMXBean, name3);
            System.out.println("\n\tOK - The MXBean "
                    + name3 + " is registered = " + mbs.isRegistered(name3));

            System.out.println("\nAll three MBeans correctly registered...");


            // We check that we don't have getAttribute() permission - as
            // it's been voluntarily omitted from our policy file.
            // If we don't get the Security Exception there is probably
            // a security configuration pb...
            //
            try {
                // We don't have getAttribute() permission, so this must fail.
                System.err.println("Trying getStringProp() - should fail");
                mbs.getAttribute(name1,"StringProp");
                System.err.println("ERROR: didn't get expected SecurityException"
                        +"\n\t check security configuration & policy file: "+
                        policy);
                throw new RuntimeException("getStringProp() did not get a " +
                        "SecurityException!");
            } catch (SecurityException x) {
                // OK!
                System.err.println("getStringProp() - failed");
            }

         } catch (Exception t) {
            t.printStackTrace();
            if (t instanceof RuntimeException)
                throw (RuntimeException)t;
            else throw new RuntimeException(t);
        }
    }

}
