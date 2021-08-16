/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * @test
 * @bug 4616517
 * @summary Receiver.send() does not work properly. Tests open/close behaviour
 *          of MidiDevices. For this test, it is essential that the MidiDevice
 *          picked from the list of devices (MidiSystem.getMidiDeviceInfo()) is
 *          the same as the one used by
 *          MidiSystem.getReceiver()/getTransmitter(). To achieve this, default
 *          provider properties for Receivers/Transmitters are used.
 */
public class OpenClose {

    private static boolean isTestExecuted;
    private static boolean isTestPassed;

    public static void main(String[] args) throws Exception {
        boolean failed = false;
        out("#4616517: Receiver.send() does not work properly");
        if (!isMidiInstalled()) {
            out("Soundcard does not exist or sound drivers not installed!");
            out("This test requires sound drivers for execution.");
            return;
        }
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice outDevice = null;
        MidiDevice inDevice = null;
        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
            if (! (device instanceof Synthesizer) &&
                ! (device instanceof Sequencer)) {
                if (device.getMaxReceivers() != 0) {
                    outDevice = device;
                }
                if (device.getMaxTransmitters() != 0) {
                    inDevice = device;
                }
            }
        }
        if (outDevice != null) {
            // set the default provider properties
            System.setProperty(Receiver.class.getName(),
                               "#" + outDevice.getDeviceInfo().getName());
        }
        if (inDevice != null) {
            System.setProperty(Transmitter.class.getName(),
                               "#" + inDevice.getDeviceInfo().getName());
        }
        out("Using MIDI OUT Device: " + outDevice);
        out("Using MIDI IN Device: " + inDevice);

        isTestExecuted = false;
        if (outDevice != null) {
            isTestExecuted = true;
            TestHelper testHelper = new ReceiverTestHelper(outDevice);
            try {
                doTest("Receiver", testHelper);
                failed |= testHelper.hasFailed();
            } catch (Exception e) {
                out("Exception occured, cannot test!");
                isTestExecuted = false;
            }
        }

        if (inDevice != null) {
            isTestExecuted = true;
            TestHelper testHelper = new TransmitterTestHelper(inDevice);
            try {
                doTest("Transmitter", testHelper);
                failed |= testHelper.hasFailed();
            } catch (Exception e) {
                out("Exception occured, cannot test!");
                isTestExecuted = false;
            }
        }

        isTestPassed = ! failed;

        if (isTestExecuted) {
            if (isTestPassed) {
                out("Test PASSED.");
            } else {
                throw new Exception("Test FAILED.");
            }
        } else {
            out("Test NOT FAILED");
        }
    }

    private static void doTest(String type,
                               TestHelper testHelper) throws Exception {
        /* Case 1:
           - MidiDevice.open()
           - MidiDevice.close()
        */
        out("checking " + type + " case 1...");
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 2a:
           - MidiSystem.get[Receiver|Transmitter]()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 2a...");
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");

        /* Case 2b:
           - MidiDevice.get[Receiver|Transmitter]()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 2b...");
        testHelper.checkClosed();

        testHelper.fetchObjectDevice();
        testHelper.checkClosed();

        testHelper.closeObjectDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 3a:
           - MidiSystem.get[Receiver|Transmitter]()
           - MidiDevice.open()
           - MidiDevice.close()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 3a...");
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");

        /* Case 3b:
           - MidiDevice.get[Receiver|Transmitter]()
           - MidiDevice.open()
           - MidiDevice.close()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 3b...");
        testHelper.checkClosed();

        testHelper.fetchObjectDevice();
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        testHelper.closeObjectDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 4a:
           - MidiSystem.get[Receiver|Transmitter]()
           - MidiDevice.open()
           - [Receiver|Transmitter].close()
           - MidiDevice.close()
        */
        out("checking " + type + " case 4a...");
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 4b:
           - MidiDevice.get[Receiver|Transmitter]()
           - MidiDevice.open()
           - [Receiver|Transmitter].close()
           - MidiDevice.close()
        */
        out("checking " + type + " case 4b...");
        testHelper.checkClosed();

        testHelper.fetchObjectDevice();
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.closeObjectDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 5a:
           - MidiDevice.open()
           - MidiSystem.get[Receiver|Transmitter]()
           - MidiDevice.close()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 5a...");
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");

        /* Case 5b:
           - MidiDevice.open()
           - MidiDevice.get[Receiver|Transmitter]()
           - MidiDevice.close()
           - [Receiver|Transmitter].close()
        */
        out("checking " + type + " case 5b...");
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.fetchObjectDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        testHelper.closeObjectDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 6a:
           - MidiDevice.open()
           - MidiSystem.get[Receiver|Transmitter]()
           - [Receiver|Transmitter].close()
           - MidiDevice.close()
        */
        out("checking " + type + " case 6a...");
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 6b:
           - MidiDevice.open()
           - MidiDevice.get[Receiver|Transmitter]()
           - [Receiver|Transmitter].close()
           - MidiDevice.close()
        */
        out("checking " + type + " case 6b...");
        testHelper.checkClosed();

        testHelper.openDevice();
        testHelper.checkOpen();

        testHelper.fetchObjectDevice();
        testHelper.checkOpen();

        testHelper.closeObjectDevice();
        testHelper.checkOpen();

        testHelper.closeDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 7:
           - MidiSystem.get[Receiver|Transmitter]() // 1
           - MidiDevice.get[Receiver|Transmitter]() // 2
           - [Receiver|Transmitter].close() // 2
           - [Receiver|Transmitter].close() // 1
        */
        out("checking " + type + " case 7...");
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.fetchObjectDevice();
        testHelper.checkOpen();

        testHelper.closeObjectDevice();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");

        /* Case 8:
           - MidiSystem.get[Receiver|Transmitter]() // 1
           - MidiDevice.get[Receiver|Transmitter]() // 2
           - [Receiver|Transmitter].close() // 1
           - [Receiver|Transmitter].close() // 2
        */
        out("checking " + type + " case 8...");
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.fetchObjectDevice();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        testHelper.closeObjectDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case 9:
           - MidiDevice.get[Receiver|Transmitter]() // 2
           - MidiSystem.get[Receiver|Transmitter]() // 1
           - [Receiver|Transmitter].close() // 2
           - [Receiver|Transmitter].close() // 1
        */
        out("checking " + type + " case 9...");
        testHelper.checkClosed();

        testHelper.fetchObjectDevice();
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeObjectDevice();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");

        /* Case 10:
           - MidiDevice.get[Receiver|Transmitter]() // 2
           - MidiSystem.get[Receiver|Transmitter]() // 1
           - [Receiver|Transmitter].close() // 1
           - [Receiver|Transmitter].close() // 2
        */
        out("checking " + type + " case 10...");
        testHelper.checkClosed();

        testHelper.fetchObjectDevice();
        testHelper.checkClosed();

        testHelper.fetchObjectMidiSystem();
        testHelper.checkOpen();

        testHelper.closeObjectMidiSystem();
        testHelper.checkClosed();

        testHelper.closeObjectDevice();
        testHelper.checkClosed();

        out("...OK");

        /* Case N - 1:
           - 10 x MidiSystem.get[Receiver|Transmitter]()
           - 10 x [Receiver|Transmitter].close()
        */
        out("checking " + type + " case N - 1...");
        TestHelper[] testHelpers = new TestHelper[10];
        for (int i = 0; i < 10; i++) {
            testHelpers[i] = (TestHelper) testHelper.clone();
        }
        testHelper.checkClosed();

        for (int i = 0; i < 10; i++) {
            testHelpers[i].fetchObjectMidiSystem();
            testHelper.checkOpen();
        }


        for (int i = 0; i < 9; i++) {
            testHelpers[i].closeObjectMidiSystem();
            testHelper.checkOpen();
        }

        testHelpers[9].closeObjectMidiSystem();
        testHelper.checkClosed();

        out("...OK");
    }

    private static void out(String message) {
        System.out.println(message);
    }

    private static abstract class TestHelper implements Cloneable {
        private MidiDevice device;
        private boolean failed;

        protected TestHelper(MidiDevice device) {
            this.device = device;
            failed = false;
        }

        protected MidiDevice getDevice() {
            return device;
        }

        public boolean hasFailed() {
            return failed;
        }

        public void openDevice() throws MidiUnavailableException {
            getDevice().open();
        }

        public void closeDevice() {
            getDevice().close();
        }

        public void checkOpen(){
            checkOpen(getDevice(), true);
        }

        public void checkClosed(){
            checkOpen(getDevice(), false);
        }

        private void checkOpen(MidiDevice device, boolean desiredState) {
            if (device.isOpen() != desiredState) {
                out("device should be " +
                                    getStateString(desiredState) + ", but isn't!");
                failed = true;
            }
        }


        private String getStateString(boolean state) {
            return state ? "open" : "closed";
        }


        public abstract void fetchObjectMidiSystem() throws MidiUnavailableException;
        public abstract void fetchObjectDevice() throws MidiUnavailableException;
        public abstract void closeObjectMidiSystem();
        public abstract void closeObjectDevice();

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    private static class ReceiverTestHelper extends TestHelper {
        private Receiver receiverMidiSystem;
        private Receiver receiverDevice;

        public ReceiverTestHelper(MidiDevice device) {
            super(device);
        }

        public void fetchObjectMidiSystem() throws MidiUnavailableException {
            receiverMidiSystem = MidiSystem.getReceiver();
        }


        public void fetchObjectDevice() throws MidiUnavailableException {
            receiverDevice = getDevice().getReceiver();
        }


        public void closeObjectMidiSystem() {
            receiverMidiSystem.close();
        }


        public void closeObjectDevice() {
            receiverDevice.close();
        }
    }

    private static class TransmitterTestHelper extends TestHelper {
        private Transmitter transmitterMidiSystem;
        private Transmitter transmitterDevice;

        public TransmitterTestHelper(MidiDevice device) {
            super(device);
        }

        public void fetchObjectMidiSystem() throws MidiUnavailableException {
            transmitterMidiSystem = MidiSystem.getTransmitter();
        }


        public void fetchObjectDevice() throws MidiUnavailableException {
            transmitterDevice = getDevice().getTransmitter();
        }


        public void closeObjectMidiSystem() {
            transmitterMidiSystem.close();
        }


        public void closeObjectDevice() {
            transmitterDevice.close();
        }
    }

    /**
     * Returns true if at least one MIDI (port) device is correctly installed on
     * the system.
     */
    public static boolean isMidiInstalled() {
        boolean result = false;
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devices.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                result = ! (device instanceof Sequencer) && ! (device instanceof Synthesizer);
            } catch (Exception e1) {
                System.err.println(e1);
            }
            if (result)
                break;
        }
        return result;
    }
}
