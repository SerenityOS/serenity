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

package stream;

import static jaxp.library.JAXPTestUtilities.USER_DIR;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6688002
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.Bug6688002Test
 * @run testng/othervm stream.Bug6688002Test
 * @summary Test single instance of XMLOutputFactory/XMLInputFactory create multiple Writer/Readers in parallel.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6688002Test {

    private static final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    private static final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    private static final int NO_THREADS = 3;

    @Test
    public void testMultiThread() throws Exception {
        Thread[] threads = new Thread[NO_THREADS];
        for (int i = 0; i < NO_THREADS; i++) {
            threads[i] = new Thread(new MyRunnable(i));
        }
        for (int i = 0; i < NO_THREADS; i++) {
            threads[i].start();
        }
        for (int i = 0; i < NO_THREADS; i++) {
            threads[i].join();
        }
    }

    public class MyRunnable implements Runnable {
        final String no;

        MyRunnable(int no) {
            this.no = String.valueOf(no);
        }

        public void run() {
            try {
                FileOutputStream fos = new FileOutputStream(USER_DIR + no);
                XMLStreamWriter w = getWriter(fos);
                // System.out.println("Writer="+w+" Thread="+Thread.currentThread());
                w.writeStartDocument();
                w.writeStartElement("hello");
                for (int j = 0; j < 50; j++) {
                    w.writeStartElement("a" + j);
                    w.writeEndElement();
                }
                w.writeEndElement();
                w.writeEndDocument();
                w.close();
                fos.close();

                FileInputStream fis = new FileInputStream(USER_DIR + no);
                XMLStreamReader r = getReader(fis);
                while (r.hasNext()) {
                    r.next();
                }
                r.close();
                fis.close();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    public static/* synchronized */XMLStreamReader getReader(InputStream is) throws Exception {
        return inputFactory.createXMLStreamReader(is);
        // return XMLStreamReaderFactory.create(null, is, true);
    }

    public static/* synchronized */XMLStreamWriter getWriter(OutputStream os) throws Exception {
        return outputFactory.createXMLStreamWriter(os);
        // return XMLStreamWriterFactory.createXMLStreamWriter(os);
    }

}
