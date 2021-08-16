/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import javax.sound.midi.SysexMessage;

import static javax.sound.midi.SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
import static javax.sound.midi.SysexMessage.SYSTEM_EXCLUSIVE;

/**
 * @test
 * @bug 8221445
 * @summary Checks basic functionality of javax.sound.midi.SysexMessage class
 */
public class Basic {

    public static void main(final String[] args) throws Exception {
        byte[] dataExclusive = {(byte) (SYSTEM_EXCLUSIVE)};
        byte[] dataSpecialExclusive = {(byte) (SPECIAL_SYSTEM_EXCLUSIVE)};
        byte[] empty = {};

        ////////////////////////////
        // Constructors
        ////////////////////////////
        SysexMessage msg = new SysexMessage(dataExclusive, 1);
        test(msg, SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage(dataSpecialExclusive, 1);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage(SYSTEM_EXCLUSIVE, empty, 0);
        test(msg, SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage(SPECIAL_SYSTEM_EXCLUSIVE, empty, 0);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage(SYSTEM_EXCLUSIVE, dataSpecialExclusive, 1);
        test(msg, SYSTEM_EXCLUSIVE, dataSpecialExclusive, 2);
        msg = new SysexMessage(SPECIAL_SYSTEM_EXCLUSIVE, dataExclusive, 1);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, dataExclusive, 2);

        ////////////////////////////
        // SysexMessage.setMessage()
        ////////////////////////////
        msg = new SysexMessage();
        msg.setMessage(dataExclusive, 1);
        test(msg, SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage();
        msg.setMessage(dataSpecialExclusive, 1);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage();
        msg.setMessage(SYSTEM_EXCLUSIVE, empty, 0);
        test(msg, SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage();
        msg.setMessage(SPECIAL_SYSTEM_EXCLUSIVE, empty, 0);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, empty, 1);
        msg = new SysexMessage();
        msg.setMessage(SYSTEM_EXCLUSIVE, dataSpecialExclusive, 1);
        test(msg, SYSTEM_EXCLUSIVE, dataSpecialExclusive, 2);
        msg = new SysexMessage();
        msg.setMessage(SPECIAL_SYSTEM_EXCLUSIVE, dataExclusive, 1);
        test(msg, SPECIAL_SYSTEM_EXCLUSIVE, dataExclusive, 2);
    }

    static void test(SysexMessage msg, int status, byte[] data, int length) {
        if (msg.getStatus() != status) {
            System.err.println("Expected status: " + status);
            System.err.println("Actual status: " + msg.getStatus());
            throw new RuntimeException();
        }
        if (msg.getLength() != length) {
            System.err.println("Expected length: " + length);
            System.err.println("Actual length: " + msg.getLength());
            throw new RuntimeException();
        }
        if (!Arrays.equals(msg.getData(), data)) {
            System.err.println("Expected data: " + Arrays.toString(data));
            System.err.println("Actual data: " + Arrays.toString(msg.getData()));
            throw new RuntimeException();
        }
    }
}
