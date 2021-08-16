/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5001857
 * @summary Test queryNames() and queryMBeans() with a buggy DynamicMBean
 * @author Daniel Fuchs
 *
 * @run clean MBeanInfoFailTest
 * @run build MBeanInfoFailTest
 * @run main MBeanInfoFailTest
 */

import javax.management.*;
import java.util.*;

public class MBeanInfoFailTest {

    public static class UnspeakableException extends RuntimeException {
        public UnspeakableException(String unspeakableMessage) {
            super(unspeakableMessage);
        }
    }

    public interface ThornyDevilMBean {
        public boolean isDormant();
        public void setDormant(boolean sleep);
    }

    public static class ThornyDevil
        extends StandardMBean implements ThornyDevilMBean {
        private boolean sleep=true;
        public ThornyDevil() throws NotCompliantMBeanException {
            super(ThornyDevilMBean.class);
        }
        public boolean isDormant() {
            return this.sleep;
        }
        public void setDormant(boolean sleep) {
            this.sleep = sleep;
        }
        public MBeanInfo getMBeanInfo() {
            if (isDormant()) return super.getMBeanInfo();
            throw new UnspeakableException("The Thorny Devil has awoken!");
        }
    }

    public static void printInstances(Set instances) {
        for (Iterator it1 = instances.iterator(); it1.hasNext();) {
            final ObjectInstance oi = (ObjectInstance)it1.next();
            final ObjectName     on = oi.getObjectName();
            final String         cn = oi.getClassName();
            System.err.println(String.valueOf(on) + ": class is " + cn);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Test queryNames() and queryMBeans() with a "+
                           "buggy DynamicMBean");
        try {
            final MBeanServer server = MBeanServerFactory.createMBeanServer();

            final ObjectName  troubleKeeper =
                new ObjectName("ThornyDevil:name=TroubleKeeper");
            final ObjectName  troubleMaker =
                new ObjectName("ThornyDevil:name=TroubleMaker");
            final ObjectName  thornyPattern =
                new ObjectName("ThornyDevil:*");

            server.createMBean(ThornyDevil.class.getName(),
                               troubleKeeper);
            server.createMBean(ThornyDevil.class.getName(),
                               troubleMaker);

            final MBeanInfo info1 = server.getMBeanInfo(troubleKeeper);
            System.out.println(String.valueOf(troubleKeeper)+" is a " +
                               info1.getClassName());
            final MBeanInfo info2 = server.getMBeanInfo(troubleMaker);
            System.out.println(String.valueOf(troubleMaker)+" is a " +
                               info2.getClassName());
            final Set thorny1 = server.queryNames(thornyPattern,null);
            if (thorny1.size() != 2) {
                System.err.println("queryNames(): " +
                                   "Expected to find 2 ThornyDevils before" +
                                   " trouble started! ");
                System.err.println("Found "+thorny1.size()+" instead: "+
                                   thorny1);
                System.exit(1);
            }
            final Set thornyM1 = server.queryMBeans(thornyPattern,null);
            if (thornyM1.size() != 2) {
                System.err.println("queryMBeans(): " +
                                   "Expected to find 2 ThornyDevils before" +
                                   " trouble started! ");
                System.err.println("Found "+thornyM1.size()+" instead: ");
                printInstances(thornyM1);
                System.exit(2);
            }
            for (Iterator it1 = thornyM1.iterator(); it1.hasNext();) {
                final ObjectInstance oi = (ObjectInstance)it1.next();
                final ObjectName     on = oi.getObjectName();
                final String         cn = oi.getClassName();
                if (cn == null || !ThornyDevil.class.getName().
                    equals(cn)) {
                    System.err.println("Expected no trouble yet!");
                    System.err.println(String.valueOf(on) + ": class is " +
                                       cn);
                    System.exit(3);
                }
                System.out.println(String.valueOf(on) + ": class is " + cn);
            }

            System.out.println("Starting trouble with "+troubleMaker+" ...");
            ThornyDevilMBean troubleMakerproxy =
                (ThornyDevilMBean) MBeanServerInvocationHandler.
                newProxyInstance(server, troubleMaker, ThornyDevilMBean.class,
                                 false);
            troubleMakerproxy.setDormant(false);

            try {
                final MBeanInfo mbi = server.getMBeanInfo(troubleMaker);
                System.err.println("No trouble started!: " + mbi);
                System.exit(2);
            } catch (Exception x) {
                if (x.getCause() instanceof UnspeakableException)
                    System.out.println("Trouble started as expected: "
                                       + x.getCause());
                else {
                    System.err.println("Unexpected trouble: " + x.getCause());
                    x.printStackTrace();
                    System.exit(3);
                }
            }

            final Set thorny2 = server.queryNames(thornyPattern,null);
            if (thorny2.size() != 2) {
                System.err.println("Expected to find 2 ThornyDevils after" +
                                   " trouble started! ");
                System.err.println("Found "+thorny2.size()+" instead: "+
                                   thorny2);
                System.exit(4);
            }

            final Set thornyM2 = server.queryMBeans(thornyPattern,null);
            if (thornyM2.size() != 2) {
                System.err.println("queryMBeans(): " +
                                   "Expected to find 2 ThornyDevils after" +
                                   " trouble started! ");
                System.err.println("Found "+thornyM2.size()+" instead: ");
                printInstances(thornyM2);
                System.exit(5);
            }
            for (Iterator it1 = thornyM2.iterator(); it1.hasNext();) {
                final ObjectInstance oi = (ObjectInstance)it1.next();
                final ObjectName     on = oi.getObjectName();
                final String         cn = oi.getClassName();
                if (!on.equals(troubleMaker)) {
                    if (cn == null || !ThornyDevil.class.getName().
                        equals(cn)) {
                        System.err.println("Expected no trouble for " + on);
                        System.err.println(String.valueOf(on) + ": class is " +
                                           cn);
                        System.exit(6);
                    }
                } else {
                    if (cn != null) {
                        System.err.println("Expected trouble for " + on);
                        System.err.println(String.valueOf(on) + ": class is " +
                                           cn);
                        System.exit(7);
                    }
                }
                System.out.println(String.valueOf(on) + ": class is " + cn);
            }

            System.out.println("Ahahah! " + troubleMaker +
                               " successfully thwarted!");
        } catch( Exception x) {
            System.err.println("Unexpected exception: " + x);
            x.printStackTrace();
            System.exit(8);
        }
    }

}
