/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @key headful
 * @bug 8047367
 * @summary Check modality settings for Window and Dialog.
 *
 * @library /test/lib
 * @run main ModalitySettingsTest
 */



public class ModalitySettingsTest {

    private void doTest() throws Exception {

        Window w = new Window(new Frame());

        boolean unexpectedExc = false;

        try {
            Dialog d = new Dialog(w);
        } catch (IllegalArgumentException iae) {
        } catch (Exception e) {
            unexpectedExc = true;
        }

        assertFalse(unexpectedExc, "unexpected exception occured when a " +
            "Window instance was passed to Dialog constructor");

        Dialog d = new Dialog((Frame) null);
        assertTrue(d.getModalityType() == Dialog.ModalityType.MODELESS,
            "the default modality type returned by Dialog " +
            "differs from Dialog.ModalityType.MODELESS");

        Frame f = new Frame();
        assertTrue(f.getModalExclusionType() == Dialog.ModalExclusionType.NO_EXCLUDE,
            "the default modality exclusion type returned by Frame" +
            "differs from Dialog.ModalExclusionType.NO_EXCLUDE");

        w = new Window((Frame) null);
        assertTrue(w.getModalExclusionType() == Dialog.ModalExclusionType.NO_EXCLUDE,
            "the default modality exclusion type returned by Window " +
            "differs from Dialog.ModalExclusionType.NO_EXCLUDE");

        d = new Dialog((Frame) null);
        assertTrue(d.getModalExclusionType() == Dialog.ModalExclusionType.NO_EXCLUDE,
            "the default modality exclusion type returned by Dialog " +
            "differs from Dialog.ModalExclusionType.NO_EXCLUDE");

        d.setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        assertTrue(d.getModalityType() == Dialog.ModalityType.TOOLKIT_MODAL,
            "the modality type returned by Dialog " +
            "differs from Dialog.ModalityType.TOOLKIT_MODAL " +
            "after setting the modality type to that value");

        d.setModal(false);
        assertTrue(d.getModalityType() == Dialog.ModalityType.MODELESS,
            "the modality type returned by Dialog differs from " +
            "Dialog.ModalityType.MODELESS after calling setModal(false)");

        d.setModal(true);
        assertTrue(d.getModalityType() == Dialog.ModalityType.APPLICATION_MODAL,
            "the modality type returned by Dialog differs from "
            + "Dialog.ModalityType.APPLICATION_MODAL after calling setModal(true)");

        w.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        assertTrue(w.getModalExclusionType() ==
                Dialog.ModalExclusionType.APPLICATION_EXCLUDE,
            "getModalExclusionType method for Window did not return " +
            "Dialog.ModalExclusionType.APPLICATION_EXCLUDE after " +
            "setting it to that value");

        d = new Dialog((Frame) null);
        d.setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        assertTrue(d.isModal(), "method isModal for Dialog " +
            "returned false when the Dialog is toolkit modal");

        d.setModalityType(Dialog.ModalityType.MODELESS);
        assertFalse(d.isModal(), "method isModal for Dialog " +
            "returned true when the Dialog is MODELESS");

        d = new Dialog((Frame) null, (Dialog.ModalityType) null);
        assertTrue(d.getModalityType() == Dialog.ModalityType.MODELESS,
            "The modality type returned for a Dialog constructed " +
            "with null modality type differs from MODELESS");

        d = new Dialog((Frame) null);
        d.setModalityType(null);
        assertTrue(d.getModalityType() == Dialog.ModalityType.MODELESS,
            "the modality type returned for a Dialog set with null " +
            "modality type differs from MODELESS");

        d.setModalExclusionType(null);
        assertTrue(d.getModalExclusionType() == Dialog.ModalExclusionType.NO_EXCLUDE,
            "The exlcusion type returned for a Dialog set with null " +
            "exclusion type differs from NO_EXCLUDE");

        try {
            Dialog.ModalityType.valueOf("invalid");
        } catch (IllegalArgumentException iae) {
        } catch (Exception e) {
            unexpectedExc = true;
        }

        assertFalse(unexpectedExc, "unexpected exception occured when an " +
            "invalid value was passed to ModalityType.valueOf method");
    }

    public static void main(String[] args) throws Exception {
        ModalitySettingsTest test = new ModalitySettingsTest();
        test.doTest();
    }
}
