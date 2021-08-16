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
 * @bug 5092515
 * @summary Test how to unwrap a user specific exception
 * @author Shanliang JIANG
 *
 * @run clean MBeanServerInvocationHandlerExceptionTest
 * @run build MBeanServerInvocationHandlerExceptionTest
 * @run main MBeanServerInvocationHandlerExceptionTest
 */


import java.io.IOException;
import javax.management.*;

public class MBeanServerInvocationHandlerExceptionTest {
    public static void main(String[] args) throws Exception {
        System.out.println(">>> Test how for the MBeanServerInvocationHandler to "+
                           "unwrap a user specific exception.");

        final MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        final ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(new Test(), name);
        TestMBean proxy = (TestMBean)
            MBeanServerInvocationHandler.newProxyInstance(mbs,
                                                          name,
                                                          TestMBean.class,
                                                          false);

        // test the method "getter"
        System.out.println(">>> Test the method getter to get an IOException.");
        try {
            proxy.getIOException();
        } catch (IOException e) {
            System.out.println(">>> Test passed: got expected exception:");
            // e.printStackTrace(System.out);
        } catch (Throwable t) {
            System.out.println(">>> Test failed: got wrong exception:");
            t.printStackTrace(System.out);

            throw new RuntimeException("Did not get an expected IOException.");
        }

        // test the method "setter"
        System.out.println(">>> Test the method setter to get a RuntimeException.");
        try {
            proxy.setRuntimeException("coucou");
        } catch (UnsupportedOperationException ue) {
            System.out.println(">>> Test passed: got expected exception:");
            //ue.printStackTrace(System.out);
        } catch (Throwable t) {
            System.out.println(">>> Test failed: got wrong exception:");
            t.printStackTrace(System.out);

            throw new RuntimeException("Did not get an expected Runtimeexception.");
        }

        // test the method "invoke"
        System.out.println(">>> Test the method invoke to get an Error.");
        try {
            proxy.invokeError();
        } catch (AssertionError ae) {
            System.out.println(">>> Test passed: got expected exception:");
            //ue.printStackTrace(System.out);
        } catch (Throwable t) {
            System.out.println(">>> Test failed: got wrong exception:");
            t.printStackTrace(System.out);

            throw new RuntimeException("Did not get an expected Error.");
        }
    }

    public static interface TestMBean {
        public Object getIOException() throws IOException;

        public void setRuntimeException(String s) throws RuntimeException;

        public void invokeError() throws Error;

    }

    public static class Test implements TestMBean {
        public Object getIOException() throws IOException {
            throw new IOException("oops");
        }

        public void setRuntimeException(String s) throws RuntimeException {
            throw new UnsupportedOperationException(s);
        }

        public void invokeError() throws Error {
            throw new AssertionError();
        }
    }
}
