/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4822050
 * @run main/timeout=1000 Test4822050
 * @summary Tests concurrent decoding
 * @author Sergey Malenkov
 */

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.JLabel;

public class Test4822050 implements ExceptionListener, Runnable {
    private static final int THREADS = 40;
    private static final int ATTEMPTS = 100;

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baos);
        encoder.writeObject(new JLabel("hello")); // NON-NLS: test message
        encoder.close();

        byte[] buffer = baos.toByteArray();
        for (int i = 0; i < THREADS; i++)
            start(buffer);
    }

    private static void start(byte[] buffer) {
        Thread thread = new Thread(new Test4822050(buffer));
        thread.start();
    }


    private byte[] buffer;

    public Test4822050(byte[] buffer) {
        this.buffer = buffer;
    }

    public void exceptionThrown(Exception exception) {
        throw new Error(exception);
    }

    public void run() {
        for (int i = 0; i < ATTEMPTS; i++)
            parse();
    }

    private void parse() {
        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(this.buffer));
        decoder.readObject();
        decoder.close();
    }
}
