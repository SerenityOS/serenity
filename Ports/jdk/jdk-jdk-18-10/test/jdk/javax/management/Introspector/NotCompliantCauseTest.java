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
 * @test NotCompliantCauseTest.java
 * @bug 6374290
 * @summary Test that NotCompliantMBeanException has a cause in case of
 *          type mapping problems.
 * @author Daniel Fuchs, Alexander Shusherov
 *
 * @run clean NotCompliantCauseTest
 * @run build NotCompliantCauseTest
 * @run main NotCompliantCauseTest
 */
/*
 * NotCompliantCauseTest.java
 *
 * Created on January 20, 2006, 2:56 PM / dfuchs
 *
 */

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;

/**
 *
 * @author Sun Microsystems, 2005 - All rights reserved.
 */
public class NotCompliantCauseTest {

    /**
     * Creates a new instance of NotCompliantCauseTest
     */
    public NotCompliantCauseTest() {
    }

    /**
     * Test that NotCompliantMBeanException has a cause in case of
     * type mapping problems.
     **/
    public static void main(String[] args) {
        NotCompliantCauseTest instance = new NotCompliantCauseTest();

        instance.test1();
    }

    public static class RuntimeTestException extends RuntimeException {
        public RuntimeTestException(String msg) {
            super(msg);
        }
        public RuntimeTestException(String msg, Throwable cause) {
            super(msg,cause);
        }
        public RuntimeTestException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Test that NotCompliantMBeanException has a cause in case of
     * type mapping problems.
     **/
    void test1() {
        try {
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();
            ObjectName oname = new ObjectName("domain:type=test");

            mbs.createMBean(NotCompliant.class.getName(), oname);
            System.err.println("ERROR: expected " +
                    "NotCompliantMBeanException not thrown");
            throw new RuntimeTestException("NotCompliantMBeanException not thrown");
        } catch (RuntimeTestException e) {
            throw e;
        } catch (NotCompliantMBeanException e) {
            Throwable cause = e.getCause();
            if (cause == null)
                throw new RuntimeTestException("NotCompliantMBeanException " +
                        "doesn't have any cause.", e);
            while (cause.getCause() != null) {
                if (cause instanceof OpenDataException) break;
                cause = cause.getCause();
            }
            if (! (cause instanceof OpenDataException))
                throw new RuntimeTestException("NotCompliantMBeanException " +
                        "doesn't have expected cause ("+
                        OpenDataException.class.getName()+"): "+cause, e);
            System.err.println("SUCCESS: Found expected cause: " + cause);
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e);
            throw new RuntimeException("Unexpected exception: " + e,e);
        }
    }

    public interface NotCompliantMXBean {
        Object returnObject();
    }

    public static class NotCompliant implements NotCompliantMXBean {
        public Object returnObject() {
            return new Object();
        }
    }

}
