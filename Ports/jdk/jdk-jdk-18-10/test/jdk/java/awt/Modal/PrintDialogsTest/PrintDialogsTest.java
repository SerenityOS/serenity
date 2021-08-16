/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8055836 8057694 8055752
 * @summary Check if Print and Page Setup dialogs lock other windows;
 *          check also correctness of modal behavior for other dialogs.
 *
 * @run applet/manual=yesno PrintDialogsTest.html
 */


import java.applet.Applet;
import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class PrintDialogsTest extends Applet implements ActionListener {

    private Button btnTest;
    private Checkbox  cbPage, cbPrint,
        cbNullDlg, cbNullFrm, cbHiddDlg, cbHiddFrm, cbDlg, cbFrm,
        cbModal, cbAppModal, cbTKModal, cbDocModal, cbModeless, cbNonModal;

    private CheckboxGroup groupDialog, groupParent, groupModType;


    public void actionPerformed(ActionEvent e) {

        if (!btnTest.equals(e.getSource())) { return; }

        boolean isPrintDlg = groupDialog.getSelectedCheckbox().equals(cbPrint);

        Test.DialogParent p = null;
        Checkbox cbParent = groupParent.getSelectedCheckbox();
        if (cbParent.equals(cbNullDlg)) {
            p = Test.DialogParent.NULL_DIALOG;
        } else if (cbParent.equals(cbNullFrm)) {
            p = Test.DialogParent.NULL_FRAME;
        } else if (cbParent.equals(cbHiddDlg)) {
            p = Test.DialogParent.HIDDEN_DIALOG;
        } else if (cbParent.equals(cbHiddFrm)) {
            p = Test.DialogParent.HIDDEN_FRAME;
        } else if (cbParent.equals(cbDlg)) {
            p = Test.DialogParent.DIALOG;
        } else if (cbParent.equals(cbFrm)) {
            p = Test.DialogParent.FRAME;
        }

        boolean modal = false;
        Dialog.ModalityType type = null;
        Checkbox cbModType = groupModType.getSelectedCheckbox();
        if (cbModType.equals(cbModal)) {
            modal = true;
        } else if (cbModType.equals(cbNonModal)) {
            modal = false;
        } else if (cbModType.equals(cbAppModal)) {
            type = Dialog.ModalityType.APPLICATION_MODAL;
        } else if (cbModType.equals(cbDocModal)) {
            type = Dialog.ModalityType.DOCUMENT_MODAL;
        } else if (cbModType.equals(cbTKModal)) {
            type = Dialog.ModalityType.TOOLKIT_MODAL;
        } else if (cbModType.equals(cbModeless)) {
            type = Dialog.ModalityType.MODELESS;
        }

        if (type == null) {
            (new Test(isPrintDlg, modal, p)).start();
        } else {
            (new Test(isPrintDlg, type,  p)).start();
        }
    }

    private void createGUI() {

        setLayout(new BorderLayout());

        setSize(350, 200);
        Panel panel = new Panel();
        panel.setLayout(new GridLayout(18, 1));

        btnTest = new Button("Start test");
        btnTest.addActionListener(this);
        panel.add(btnTest);


        panel.add(new Label("Dialog parent:"));
        groupParent = new CheckboxGroup();
        cbNullDlg = new Checkbox("NULL Dialog"  , groupParent, true );
        cbNullFrm = new Checkbox("NULL Frame"   , groupParent, false);
        cbHiddDlg = new Checkbox("Hidden Dialog", groupParent, false);
        cbHiddFrm = new Checkbox("Hidden Frame" , groupParent, false);
        cbDlg     = new Checkbox("Dialog"       , groupParent, false);
        cbFrm     = new Checkbox("Frame"        , groupParent, false);

        panel.add(cbNullDlg);
        panel.add(cbNullFrm);
        panel.add(cbHiddDlg);
        panel.add(cbHiddFrm);
        panel.add(cbDlg);
        panel.add(cbFrm);

        panel.add(new Label("Dialog modality type:"));
        groupModType = new CheckboxGroup();
        cbModal    = new Checkbox("Modal"            , groupModType, true );
        cbNonModal = new Checkbox("Non-modal"        , groupModType, false);
        cbAppModal = new Checkbox("Application modal", groupModType, false);
        cbDocModal = new Checkbox("Document modal"   , groupModType, false);
        cbTKModal  = new Checkbox("Toolkit modal"    , groupModType, false);
        cbModeless = new Checkbox("Modeless"         , groupModType, false);

        panel.add(cbModal);
        panel.add(cbNonModal);
        panel.add(cbAppModal);
        panel.add(cbDocModal);
        panel.add(cbTKModal);
        panel.add(cbModeless);
        add(panel);

        panel.add(new Label("Print dialog type:"));
        groupDialog = new CheckboxGroup();
        cbPage   = new Checkbox("Page Setup", groupDialog, true);
        cbPrint  = new Checkbox("Print", groupDialog, false);
        panel.add(cbPage);
        panel.add(cbPrint);

        validate();
        setVisible(true);
    }

    public void start() {
        try {
            EventQueue.invokeAndWait(this::createGUI);
        } catch (Exception e) {}
    }
}
