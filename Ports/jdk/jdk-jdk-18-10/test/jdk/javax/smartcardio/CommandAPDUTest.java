/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049021
 * @summary Test different constructors for CommandAPDU and check CLA,INS,NC,NE,
 * P1,and P2
 * @run testng CommandAPDUTest
 */
import java.nio.ByteBuffer;
import javax.smartcardio.CommandAPDU;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CommandAPDUTest {

    static final byte[] C1 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04,
        (byte) 0x00, (byte) 0x07, (byte) 0xA0, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x62, (byte) 0x81, (byte) 0x01, (byte) 0x00};
    static int cla, ins, nc, ne, p1, p2;
    static byte[] apdu, data;
    static CommandAPDU cm1, cm2, cm3, cm4, cm5, cm6, cm7, cm8, cm9;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //expected values of apdu, data, headers, nc, ne
        CommandAPDU capdu = new CommandAPDU(C1);
        apdu = capdu.getBytes();
        data = capdu.getData();

        cla = capdu.getCLA();
        if (cla != (C1[0] & 0xff)) {
            throw new RuntimeException("Failure: cla is not right");
        }

        ins = capdu.getINS();
        if (ins != (C1[1] & 0xff)) {
            throw new RuntimeException("Failure: ins is not right");
        }

        p1 = capdu.getP1();
        if (p1 != (C1[2] & 0xff)) {
            throw new RuntimeException("Failure: p1 is not right");
        }

        p2 = capdu.getP2();
        if (p2 != (C1[3] & 0xff)) {
            throw new RuntimeException("Failure: p2 is not right");
        }

        nc = capdu.getNc();
        ne = capdu.getNe();

        //Test on following constructors
        cm1 = new CommandAPDU(apdu);
        cm2 = new CommandAPDU(cla, ins, p1, p2);
        cm3 = new CommandAPDU(cla, ins, p1, p2, data);
        cm4 = new CommandAPDU(cla, ins, p1, p2, data, ne);
        cm5 = new CommandAPDU(cla, ins, p1, p2, ne);
        cm6 = new CommandAPDU(ByteBuffer.wrap(apdu));
        cm7 = new CommandAPDU(apdu, 0, apdu.length);
        cm8 = new CommandAPDU(cla, ins, p1, p2, data, 0, nc);
        cm9 = new CommandAPDU(cla, ins, p1, p2, data, 0, nc, ne);
    }

    @Test(dataProvider = "provider1")
    public static void testHeaders(CommandAPDU cm) {
        assertEquals(cla, cm.getCLA());
        assertEquals(ins, cm.getINS());
        assertEquals(p1, cm.getP1());
        assertEquals(p2, cm.getP2());
    }

    @Test(dataProvider = "provider2")
    public static void testAPDU(CommandAPDU cm) {
        assertEquals(apdu, cm.getBytes());
    }

    @Test(dataProvider = "provider3")
    public static void testData(CommandAPDU cm) {
        assertEquals(data, cm.getData());
    }

    @Test(dataProvider = "provider3")
    public static void testNC(CommandAPDU cm) {
        assertEquals(nc, cm.getNc());
    }

    @Test(dataProvider = "provider4")
    public static void testNE(CommandAPDU cm) {
        assertEquals(ne, cm.getNe());
    }

    @DataProvider
    public Object[][] provider1() {
        return new Object[][]{{cm1}, {cm2}, {cm3}, {cm4}, {cm5}, {cm6}, {cm7},
        {cm8}, {cm9}};
    }

    @DataProvider
    public Object[][] provider2() {
        return new Object[][]{{cm1}, {cm6}, {cm7}};
    }

    @DataProvider
    public Object[][] provider3() {
        return new Object[][]{{cm1}, {cm3}, {cm4}, {cm6}, {cm7}, {cm8}, {cm9}};
    }

    @DataProvider
    public Object[][] provider4() {
        return new Object[][]{{cm1}, {cm4}, {cm5}, {cm6}, {cm7}, {cm9}};
    }

}
