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

import java.io.ByteArrayOutputStream;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

/**
 * @test
 * @bug 4782924
 * @bug 4812168
 * @bug 4356787
 * @summary MIDI i/o. This is an interactive test! Start it and follow the
 *          instructions.
 * @run main/manual IOLoop
 */
public class IOLoop {
    private static final int LONG_SYSEX_LENGTH = 2000;

    private static Receiver receiver;
    private static Transmitter transmitter;
    private static MidiMessage receivedMessage;
    private static ByteArrayOutputStream baos;
    private static int expectedBytes;
    private static int receivedBytes;
    private static Object lock = new Object();
    private static long lastTimestamp;

    public static void main(String[] args) throws Exception {
        ShortMessage sMsg = new ShortMessage();
        SysexMessage syMsg = new SysexMessage();
        boolean isTestPassed = true;
        boolean sysExTestPassed = true;
        boolean isTestExecuted = true;

        out("To run this test successfully, you need to have attached");
        out("  your MIDI out port with the MIDI in port.");

        MidiDevice inDev = null;
        MidiDevice outDev = null;

        // setup
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

            int devNum = Integer.decode(args[0]).intValue();
            out("-> opening Transmitter from "+infos[devNum]);
            inDev = MidiSystem.getMidiDevice(infos[devNum]);
            inDev.open();
            transmitter = inDev.getTransmitter();
            Receiver testReceiver = new TestReceiver();
            transmitter.setReceiver(testReceiver);

            devNum = Integer.decode(args[1]).intValue();
            out("-> opening Receiver from "+infos[devNum]);
            outDev = MidiSystem.getMidiDevice(infos[devNum]);
            outDev.open();
            receiver = outDev.getReceiver();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Cannot test!");
            return;
        }

        // test
        sMsg.setMessage(ShortMessage.NOTE_OFF | 0, 27, 100);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.NOTE_OFF | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.NOTE_OFF | 15, 127, 127);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.NOTE_ON | 4, 27, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.NOTE_ON | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.NOTE_ON | 15, 127, 127);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.POLY_PRESSURE | 11, 98, 99);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.POLY_PRESSURE | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.POLY_PRESSURE | 15, 127, 127);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CONTROL_CHANGE | 13, 1, 63);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CONTROL_CHANGE | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CONTROL_CHANGE | 15, 127, 127);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.PROGRAM_CHANGE | 2, 120, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.PROGRAM_CHANGE | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.PROGRAM_CHANGE | 15, 127, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CHANNEL_PRESSURE | 6, 30, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CHANNEL_PRESSURE | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CHANNEL_PRESSURE | 15, 127, 0);
        isTestPassed &= testMessage(sMsg);

        sMsg.setMessage(ShortMessage.PITCH_BEND | 6, 56, 4);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.PITCH_BEND | 0, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.PITCH_BEND | 15, 127, 127);
        isTestPassed &= testMessage(sMsg);

        sMsg.setMessage(ShortMessage.MIDI_TIME_CODE, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.MIDI_TIME_CODE, 127, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_POSITION_POINTER, 1, 77);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_POSITION_POINTER, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_POSITION_POINTER, 127, 127);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_SELECT, 51, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_SELECT, 0, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SONG_SELECT, 127, 0);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.TUNE_REQUEST);
        isTestPassed &= testMessage(sMsg);

        sMsg.setMessage(ShortMessage.TIMING_CLOCK);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.START);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.CONTINUE);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.STOP);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.ACTIVE_SENSING);
        isTestPassed &= testMessage(sMsg);
        sMsg.setMessage(ShortMessage.SYSTEM_RESET);
        isTestPassed &= testMessage(sMsg);

        syMsg.setMessage(new byte[]{(byte) 0xF0, (byte) 0xF7}, 2);
        isTestPassed &= testMessage(syMsg);
        syMsg.setMessage(new byte[]{(byte) 0xF0, 0x01, (byte) 0xF7}, 3);
        isTestPassed &= testMessage(syMsg);
        syMsg.setMessage(new byte[]{(byte) 0xF0, 0x02, 0x03, (byte) 0xF7}, 4);
        isTestPassed &= testMessage(syMsg);
        syMsg.setMessage(new byte[]{(byte) 0xF0, 0x04, 0x05, 0x06, (byte) 0xF7}, 5);
        isTestPassed &= testMessage(syMsg);

        if (isTestPassed) {
            byte[] sysexArray = new byte[LONG_SYSEX_LENGTH];
            sysexArray[0] = (byte) 0xF0;
            for (int i = 1; i < sysexArray.length; i++) {
                sysexArray[i] = (byte) (i % 0x80);
            }
//          syMsg.setMessage(new byte[]{(byte) 0xF7, (byte) ShortMessage.START}, 2);
//          sMsg.setMessage(ShortMessage.START);
//          isTestPassed &= testMessage(syMsg, sMsg, DEFAULT_SLEEP_INTERVALL);
            for (int trial = sysexArray.length; trial > 4; trial -= 1234) {
                sleep(500);
                sysexArray[trial - 1] = (byte) 0xF7;
                syMsg.setMessage(sysexArray, trial);
                sysExTestPassed &= testMessage(syMsg);
                break;
            }
        }

        // cleanup
        receiver.close();
        transmitter.close();
        inDev.close();
        outDev.close();

        if (isTestExecuted) {
            if (isTestPassed && sysExTestPassed) {

                out("Test PASSED.");
            } else {
                if (isTestPassed
                    && !sysExTestPassed
                    && (System.getProperty("os.name").startsWith("Windows"))) {
                    out("Some Windows MIDI i/o drivers have a problem with larger ");
                    out("sys ex messages. The failing sys ex cases are OK, therefore.");
                    out("Test PASSED.");
                } else {
                    throw new Exception("Test FAILED.");
                }
            }
        } else {
            out("Test NOT FAILED");
        }
    }

    private static boolean testMessage(MidiMessage message) {
        receivedMessage = null;
        baos = new ByteArrayOutputStream();
        expectedBytes = message.getLength();
        receivedBytes = 0;
        System.out.print("Sending message " + getMessageString(message.getMessage())+"...");
        receiver.send(message, -1);
        /* sending 3 bytes can roughly be done in 1 millisecond,
         * so this estimate waits at max 3 times longer than the message takes,
         * plus a little offset to allow the MIDI subsystem some processing time
         */
        int offset = 300; // standard offset 100 millis
        if (message instanceof SysexMessage) {
            // add a little processing time to sysex messages
            offset += 1000;
        }
        if (receivedBytes < expectedBytes) {
            sleep(expectedBytes + offset);
        }
        boolean equal;
        byte[] data = baos.toByteArray();
        if (data.length > 0) {
            equal = messagesEqual(message.getMessage(), data);
        } else {
            equal = messagesEqual(message, receivedMessage);
            if (receivedMessage != null) {
                data = receivedMessage.getMessage();
            } else {
                data = null;
            }
        }
        if (!equal) {
            if ((message.getStatus() & 0xF0) == ShortMessage.PITCH_BEND) {
                out("NOT failed (may expose a bug in ALSA)");
                equal = true;
                sleep(100);
            }
            if ((message.getStatus() == 0xF6) && (message.getLength() == 1)) {
                out("NOT failed (may expose an issue on Solaris)");
                equal = true;
                sleep(100);
            }
            else if ((message.getStatus()) == 0xF0 && message.getLength() < 4) {
                out("NOT failed (not a correct sys ex message)");
                equal = true;
                sleep(200);
            } else {
                out("FAILED:");
                out("  received as " + getMessageString(data));
            }
        } else {
            System.out.println("OK");
        }
        return equal;
    }

    private static void sleep(int milliseconds) {
        synchronized(lock) {
            try {
                lock.wait(milliseconds);
            } catch (InterruptedException e) {
            }
        }
    }

    private static String getMessageString(byte[] data) {
        String s;
        if (data == null) {
            s = "<null>";
        } else if (data.length == 0) {
            s = "0-sized array";
        } else {
            int status = data[0] & 0xFF;
            if (data.length <= 3) {
                if (status < 240) {
                    s = "command 0x" + Integer.toHexString(status & 0xF0) + " channel " + (status & 0x0F);
                } else {
                    s = "status 0x" + Integer.toHexString(status);
                }
                if (data.length > 1) {
                    s += " data 0x" + Integer.toHexString(data[1] & 0xFF);
                    if (data.length > 2) {
                        s += " 0x" + Integer.toHexString(data[2] & 0xFF);
                    }
                }
            } else {
                s = "status " + Integer.toHexString(status)+" and length "+data.length+" bytes";
            }
        }
        return s;
    }

    private static boolean messagesEqual(MidiMessage m1, MidiMessage m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
        if (m1.getLength() != m2.getLength()) {
            return false;
        }
        byte[] array1 = m1.getMessage();
        byte[] array2 = m2.getMessage();
        return messagesEqual(array1, array2);
    }

    private static boolean messagesEqual(byte[] a1, byte[] a2) {
        if (a1.length != a2.length) return false;
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    private static void out(String s) {
        System.out.println(s);
        System.out.flush();
    }

    private static String canIn(MidiDevice dev) {
        if (dev.getMaxTransmitters() != 0) {
            return "IN ";
        }
        return "   ";
    }

    private static String canOut(MidiDevice dev) {
        if (dev.getMaxReceivers() != 0) {
            return "OUT ";
        }
        return "   ";
    }


    private static void checkTimestamp(long timestamp) {
        // out("checking timestamp...");
        if (timestamp < 1) {
            out("timestamp 0 or negative!");
        }
        if (timestamp < lastTimestamp) {
            out("timestamp not progressive!");
        }
        lastTimestamp = timestamp;
    }

    private static class TestReceiver implements Receiver {
        public void send(MidiMessage message, long timestamp) {
            //System.out.print(""+message.getLength()+"..");
            checkTimestamp(timestamp);
            try {
                receivedMessage = message;
                if (message.getStatus() == 0xF0
                    || (message.getLength() > 3 && message.getStatus() != 0xF7)) {
                    // sys ex message
                    byte[] data = message.getMessage();
                    baos.write(data);
                    receivedBytes += data.length;
                }
                else if (message.getStatus() == 0xF7) {
                    // sys ex cont'd message
                    byte[] data = message.getMessage();
                    // ignore the prepended 0xF7
                    baos.write(data, 1, data.length-1);
                    receivedBytes += (data.length - 1);
                } else {
                    receivedBytes += message.getLength();
                }
                if (receivedBytes >= expectedBytes) {
                    synchronized(lock) {
                        lock.notify();
                    }
                }
                System.out.print(""+receivedBytes+"..");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void close() {
        }
    }
}
