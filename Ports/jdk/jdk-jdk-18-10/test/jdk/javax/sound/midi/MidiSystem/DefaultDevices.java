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

import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.spi.MidiDeviceProvider;

import com.sun.media.sound.JDK13Services;

/**
 * @test
 * @bug 4776511
 * @bug 4934509
 * @bug 4938236
 * @modules java.desktop/com.sun.media.sound
 * @run main/timeout=600 DefaultDevices
 * @summary RFE: Setting the default MixerProvider
 */
/** Test the retrieving of MidiDevices with default device properties.
 * This is a part of the test for 4776511.
 * The test also functions as a unit test for 4934509: SPEC: Document
 * explicitely MidiSystem.getReceiver's behavior
 * and a regession test for 4938236: Crash when opening synthesizer implicitly
 * The test has been updated to reflect a fix for 6411624: MidiSystem.getSequencer()
 * doesn't throw MidiUnavaivableException if no audio card installed (see also
 * 6422786: regression test javax/sound/midi/MidiSystem/DefaultDevices.java fails)
 */
public class DefaultDevices {

    private static final String ERROR_PROVIDER_CLASS_NAME = "abc";
    private static final String ERROR_INSTANCE_NAME = "def";

    private static final Class RECEIVER_CLASS = javax.sound.midi.Receiver.class;
    private static final Class TRANSMITTER_CLASS = javax.sound.midi.Transmitter.class;
    private static final Class SEQUENCER_CLASS = javax.sound.midi.Sequencer.class;
    private static final Class SYNTHESIZER_CLASS = javax.sound.midi.Synthesizer.class;

    public static void main(String[] args) throws Exception {
        boolean allOk = true;
        MidiDevice.Info[] infos;

        out("\nTesting MidiDevices retrieved via MidiSystem");
        infos = MidiSystem.getMidiDeviceInfo();
        allOk &= testDevices(infos, null);

        out("\nTesting MidiDevices retrieved from MidiDeviceProviders");
        List providers = JDK13Services.getProviders(MidiDeviceProvider.class);
        for (int i = 0; i < providers.size(); i++) {
            MidiDeviceProvider provider = (MidiDeviceProvider)providers.get(i);
            infos = provider.getDeviceInfo();
            allOk &= testDevices(infos, provider.getClass().getName());
        }

        if (!allOk) {
            throw new Exception("Test failed");
        } else {
            out("Test passed");
        }
    }

    private static boolean testDevices(MidiDevice.Info[] infos,
            String providerClassName) {
        boolean allOk = true;

        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = null;
            try {
                device = MidiSystem.getMidiDevice(infos[i]);
            } catch (MidiUnavailableException e) {
                out("Exception thrown; Test NOT failed.");
                e.printStackTrace(System.out);
                out("");
            }
            out("\nTesting device: " + device);
            if (device instanceof Sequencer) {
                allOk &= testDevice(device, SEQUENCER_CLASS, providerClassName, true, true);
                // incorrect cases
                allOk &= testDevice(device, SYNTHESIZER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, RECEIVER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, TRANSMITTER_CLASS, providerClassName, false, false);
            }
            if (device instanceof Synthesizer) {
                allOk &= testDevice(device, SYNTHESIZER_CLASS, providerClassName, true, true);
                allOk &= testDevice(device, RECEIVER_CLASS, providerClassName, false, true);
                // incorrect cases
                allOk &= testDevice(device, TRANSMITTER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, SEQUENCER_CLASS, providerClassName, false, false);
            }
            if (device instanceof Receiver) {
                allOk &= testDevice(device, RECEIVER_CLASS, providerClassName, true, true);
                // incorrect cases
                allOk &= testDevice(device, TRANSMITTER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, SYNTHESIZER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, SEQUENCER_CLASS, providerClassName, false, false);
            }
            if (device instanceof Transmitter) {
                allOk &= testDevice(device, TRANSMITTER_CLASS, providerClassName, true, true);
                // incorrect cases
                allOk &= testDevice(device, RECEIVER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, SYNTHESIZER_CLASS, providerClassName, false, false);
                allOk &= testDevice(device, SEQUENCER_CLASS, providerClassName, false, false);
            }
        }
        return allOk;
    }

    private static boolean testDevice(MidiDevice device, Class type,
            String providerClassName, boolean testWrong, boolean expectedResult) {
        boolean allOk = true;
        String instanceName = device.getDeviceInfo().getName();

        // no error
        allOk &= testDevice(device, type, providerClassName,
                            instanceName, expectedResult);

        if (testWrong) {
            // erroneous provider class name, correct instance name
            allOk &= testDevice(device, type, ERROR_PROVIDER_CLASS_NAME,
                                instanceName, expectedResult);

            // correct provider class name, erroneous instance name
            // we presume that provider provides only one class of requested type
            allOk &= testDevice(device, type, providerClassName,
                                ERROR_INSTANCE_NAME, expectedResult);
        }

        return allOk;
    }

    private static boolean testDevice(MidiDevice device, Class type,
            String providerClassName, String instanceName,
            boolean expectedResult) {
        boolean allOk = true;

        try {
            String propertyName = type.getName();
            String propertyValue = (providerClassName != null) ? providerClassName: "" ;
            propertyValue += "#" + instanceName;
            out("property: " + propertyName + "="+ propertyValue);
            System.setProperty(propertyName, propertyValue);
            Object reference = null;
            Object result = null;
            if (type == SEQUENCER_CLASS) {
                reference = device;
                result = MidiSystem.getSequencer();
            } else if (type == SYNTHESIZER_CLASS) {
                reference = device;
                result = MidiSystem.getSynthesizer();
            } else if (type == RECEIVER_CLASS) {
                reference = device.getReceiver();
                result = MidiSystem.getReceiver();
            } else if (type == TRANSMITTER_CLASS) {
                reference = device.getTransmitter();
                result = MidiSystem.getTransmitter();
            }
            out("result: " + result);
            boolean rightDevice = (reference.getClass() == result.getClass());
            if (rightDevice != expectedResult) {
                out("\nERROR: type " + type + " failed:"
                        + " class should" + (expectedResult ? "" : " NOT") + " be '"
                        + reference.getClass()
                        + "' but is '" + result.getClass() + "'!\n");
                allOk = false;
            }
            if (expectedResult
                    && reference instanceof MidiDevice
                    && result instanceof MidiDevice) {
                MidiDevice referenceDevice = (MidiDevice)reference;
                MidiDevice resultDevice = (MidiDevice)result;
                if (!referenceDevice.getDeviceInfo().getName().equals(
                                    resultDevice.getDeviceInfo().getName())) {
                    out("\nERROR: type " + type + " failed: name should be '"
                            + referenceDevice.getDeviceInfo().getName()
                            + "' but is '"
                            + resultDevice.getDeviceInfo().getName() + "'!\n");
                    allOk = false;
                }
            }
            if (result instanceof Receiver) {
                ((Receiver)result).close();
            } else if (result instanceof Transmitter) {
                ((Transmitter)result).close();
            } else if (result instanceof Synthesizer) {
                ((Synthesizer)result).close();
            } else if (result instanceof Sequencer) {
                ((Sequencer)result).close();
            }
        } catch (Exception e) {
            out("Exception thrown; Test NOT failed.");
            e.printStackTrace(System.out);
            out("");
        }
        return allOk;
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
