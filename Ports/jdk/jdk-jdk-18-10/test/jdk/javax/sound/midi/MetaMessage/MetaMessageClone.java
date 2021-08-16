/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import  javax.sound.midi.MetaMessage;

/**
 * @test
 * @bug 4511796
 * @summary Check that MetaMessage.clone() works correctly
 */
public class MetaMessageClone {

    private static void printMsg(MetaMessage msg, byte[] data) {
        System.out.println(""+msg.getLength()+" total bytes, type="+msg.getType()+", dataLength="+data.length);
    }

    private static void checkClone(MetaMessage msg) throws Exception {
        System.out.print("Original: ");
        byte[] msgData=msg.getData();
        printMsg(msg, msgData);
        MetaMessage msg2=(MetaMessage) msg.clone();
        byte[] msg2Data=msg2.getData();
        System.out.print("Clone:    ");
        printMsg(msg2, msg2Data);

        if (msg2.getLength()!=msg.getLength()
            || msg.getType()!=msg2.getType()
            || msgData.length!=msg2Data.length) {
                throw new Exception("cloned MetaMessage is not equal.");
        }
        int max=Math.min(msgData.length, 10);
        for (int i=0; i<max; i++) {
            if (msgData[i]!=msg2Data[i]) {
                throw new Exception("Cloned MetaMessage data is not equal.");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // let's create some MetaMessages and check them
        MetaMessage msg=new MetaMessage();
        String text="a textmarker";
        msg.setMessage(1, text.getBytes(), text.length());
        checkClone(msg);
        msg.setMessage(0x2E, new byte[0], 0);
        checkClone(msg);
        byte[] data=new byte[17000];
        for (int i=0; i<30; data[i]=(byte) (i++ & 0xFF));
        msg.setMessage(0x02, data, 80); checkClone(msg);
        msg.setMessage(0x02, data, 160); checkClone(msg);
        msg.setMessage(0x02, data, 400); checkClone(msg);
        msg.setMessage(0x02, data, 1000); checkClone(msg);
        msg.setMessage(0x02, data, 10000); checkClone(msg);
        msg.setMessage(0x02, data, 17000); checkClone(msg);
    }
}
