/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4880633
 * @summary Tests multi threaded access to the XMLEncoder
 * @author Mark Davidson
 */

import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;

public class Test4880633 implements ExceptionListener, Runnable {
    private static final int THREADS_COUNT = 10;
    private static final int THREAD_LENGTH = 90;

    public static void main(String[] args) {
        Runnable[] tests = new Runnable[THREADS_COUNT];
        for (int i = 0; i < tests.length; i++) {
            ValueObject object = new ValueObject();
            object.setA("Value a" + i);
            object.setAa("Value aa" + i);
            object.setAaa("Value aaa" + i);
            object.setAaaa("Value aaaa" + i);
            object.setAaaaa("Value aaaaa" + i);
            object.setAaaaaa("Value aaaaaa" + i);
            object.setAaaaaaa("Value aaaaaaa" + i);
            object.setAaaaaaaa("Value aaaaaaaa" + i);
            object.setAaaaaaaaa("Value aaaaaaaaa" + i);
            object.setAaaaaaaaaa("Value aaaaaaaaaa" + i);
            object.setAaaaaaaaaaa("Value aaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaa("Value aaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaa("Value aaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaa("Value aaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            object.setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa("Value aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + i);
            // set encoder thread to use this object
            tests[i] = new Test4880633(object);
        }
        //start all threads
        for (int i = 0; i < tests.length; i++) {
            Thread thread = new Thread(tests[i]);
            thread.setName("Bug Thread " + i);
            thread.start();
        }
    }

    private final Object object;

    public Test4880633(Object object) {
        this.object = object;
    }

    public void run() {
        // run thread a few time
        // object stays the same but NullPointerException appears randomly
        // on dual proccessor a lock is generated
        for (int i = 0; i < THREAD_LENGTH; i++) {
            // create XMLEncoder to ByteArrayOutputStream
            // this is to exclude file locking problems
            XMLEncoder encoder = new XMLEncoder(new ByteArrayOutputStream());
            encoder.setExceptionListener(this);
            // write the object
            // will see randomly null pointer exceptions
            // a bug as object is same through different encode phases
            encoder.writeObject(this.object);
            //close encoder
            encoder.close();
        }
        System.out.println(Thread.currentThread().getName() + " is finished");
    }

    public void exceptionThrown(Exception exception) {
        throw new Error("unexpected exception", exception);
    }

    public static class ValueObject {
        private String a;

        public void setA(String a) {
            this.a = a;
        }

        public String getA() {
            return this.a;
        }


        private String aa;

        public void setAa(String a) {
            this.aa = a;
        }

        public String getAa() {
            return this.aa;
        }


        private String aaa;

        public void setAaa(String a) {
            this.aaa = a;
        }

        public String getAaa() {
            return this.aaa;
        }


        private String aaaa;

        public void setAaaa(String a) {
            this.aaaa = a;
        }

        public String getAaaa() {
            return this.aaaa;
        }


        private String aaaaa;

        public void setAaaaa(String a) {
            this.aaaaa = a;
        }

        public String getAaaaa() {
            return this.aaaaa;
        }


        private String aaaaaa;

        public void setAaaaaa(String a) {
            this.aaaaaa = a;
        }

        public String getAaaaaa() {
            return this.aaaaaa;
        }


        private String aaaaaaa;

        public void setAaaaaaa(String a) {
            this.aaaaaaa = a;
        }

        public String getAaaaaaa() {
            return this.aaaaaaa;
        }


        private String aaaaaaaa;

        public void setAaaaaaaa(String a) {
            this.aaaaaaaa = a;
        }

        public String getAaaaaaaa() {
            return this.aaaaaaaa;
        }


        private String aaaaaaaaa;

        public void setAaaaaaaaa(String a) {
            this.aaaaaaaaa = a;
        }

        public String getAaaaaaaaa() {
            return this.aaaaaaaaa;
        }


        private String aaaaaaaaaa;

        public void setAaaaaaaaaa(String a) {
            this.aaaaaaaaaa = a;
        }

        public String getAaaaaaaaaa() {
            return this.aaaaaaaaaa;
        }


        private String aaaaaaaaaaa;

        public void setAaaaaaaaaaa(String a) {
            this.aaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaa() {
            return this.aaaaaaaaaaa;
        }


        private String aaaaaaaaaaaa;

        public void setAaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaa() {
            return this.aaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }


        private String aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;

        public void setAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(String a) {
            this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa = a;
        }

        public String getAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
            return this.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;
        }
    }
}
