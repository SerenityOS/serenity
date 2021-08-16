/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.Serializable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;

import java.util.Objects;

import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;
import sun.rmi.transport.LiveRef;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/*
 * @test
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @run testng/othervm FilterUSRTest
 * @summary Check objects exported with ObjectInputFilters via internal UnicastServerRef(2)
 */
public class FilterUSRTest {

    /**
     * Data to test serialFilter call counts.
     * - name
     * - Object
     * - expected count of calls to checkInput.
     *
     * @return array of test data
     */
    @DataProvider(name = "bindData")
    static Object[][] bindObjects() {
        Object[][] data = {
                {"SimpleString", "SimpleString", 0},
                {"String", new XX("now is the time"), 1},
                {"String[]", new XX(new String[3]), 3},
                {"Long[4]", new XX(new Long[4]), 3},
                {"RejectME", new XX(new RejectME()), -1},
        };
        return data;
    }

    /*
     * Test exporting an object with a serialFilter using UnicastServerRef.exportObject().
     * Send some objects and check the number of calls to the serialFilter.
     */
    @Test(dataProvider = "bindData")
    public void UnicastServerRef(String name, Object obj, int expectedFilterCount) throws RemoteException {
        try {
            RemoteImpl impl = RemoteImpl.create();
            UnicastServerRef ref = new UnicastServerRef(new LiveRef(0), impl.checker);

            Echo client = (Echo) ref.exportObject(impl, null, false);

            int count = client.filterCount(obj);
            System.out.printf("count: %d, obj: %s%n", count, obj);
            Assert.assertEquals(count, expectedFilterCount, "wrong number of filter calls");
        } catch (RemoteException rex) {
            if (expectedFilterCount == -1 &&
                    UnmarshalException.class.equals(rex.getCause().getClass()) &&
                    InvalidClassException.class.equals(rex.getCause().getCause().getClass())) {
                return; // normal expected exception
            }
            rex.printStackTrace();
            Assert.fail("unexpected remote exception", rex);
        } catch (Exception ex) {
            Assert.fail("unexpected exception", ex);
        }
    }

    /*
     * Test exporting an object with a serialFilter using UnicastServerRef2.exportObject()
     * with explicit (but null) SocketFactories.
     * Send some objects and check the number of calls to the serialFilter.
     */
    @Test(dataProvider = "bindData")
    public void UnicastServerRef2(String name, Object obj, int expectedFilterCount) throws RemoteException {
        try {
            RemoteImpl impl = RemoteImpl.create();
            UnicastServerRef2 ref = new UnicastServerRef2(0, null, null, impl.checker);

            Echo client = (Echo) ref.exportObject(impl, null, false);

            int count = client.filterCount(obj);
            System.out.printf("count: %d, obj: %s%n", count, obj);
            Assert.assertEquals(count, expectedFilterCount, "wrong number of filter calls");
        } catch (RemoteException rex) {
            if (expectedFilterCount == -1 &&
                    UnmarshalException.class.equals(rex.getCause().getClass()) &&
                    InvalidClassException.class.equals(rex.getCause().getCause().getClass())) {
                return; // normal expected exception
            }
            rex.printStackTrace();
            Assert.fail("unexpected remote exception", rex);
        } catch (Exception rex) {
            Assert.fail("unexpected exception", rex);
        }
    }

    /**
     * A simple Serializable holding an object that is passed by value.
     * It and its contents are checked by the filter.
     */
    static class XX implements Serializable {
        private static final long serialVersionUID = 362498820763181265L;

        final Object obj;

        XX(Object obj) {
            this.obj = obj;
        }

        public String toString() {
            return super.toString() + "//" + Objects.toString(obj);
        }
    }

    interface Echo extends Remote {
        int filterCount(Object obj) throws RemoteException;
    }

    /**
     * This remote object just counts the calls to the serialFilter
     * and returns it.  The caller can check the number against
     * what was expected for the object passed as an argument.
     * A new RemoteImpl is used for each test so the count starts at zero again.
     */
    static class RemoteImpl implements Echo {

        private static final long serialVersionUID = 1L;

        transient Checker checker;

        static RemoteImpl create() throws RemoteException {
            RemoteImpl impl = new RemoteImpl(new Checker());
            return impl;
        }

        private RemoteImpl(Checker checker) throws RemoteException {
            this.checker = checker;
        }

        public int filterCount(Object obj) throws RemoteException {
            return checker.count();
        }

    }

    /**
     * A ObjectInputFilter that just counts when it is called.
     */
    static class Checker implements ObjectInputFilter {
        int count;

        @Override
        public Status checkInput(ObjectInputFilter.FilterInfo info) {
            if (info.serialClass() == RejectME.class) {
                return Status.REJECTED;
            }
            count++;
            return Status.UNDECIDED;
        }

        public int count() {
            return count;
        }
    }

    /**
     * A class to be rejected by the filter.
     */
    static class RejectME implements Serializable {
        private static final long serialVersionUID = 2L;
    }

}
