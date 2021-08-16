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


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.*;


public class Test {

    class CustomFrame extends Frame {
        public CustomFrame() {
            super();
            setTitle("Frame");
            setSize(150, 100);
            Button dummy = new Button("Dummy");
            add(dummy);
        }
    }

    class CustomWindow extends Window {

        private void GUI() {
            setSize(150, 100);
            Button dummy = new Button("Dummy");
            add(dummy);
        }

        public CustomWindow(Dialog d) {
            super(d);
            GUI();
        }

        public CustomWindow(Frame f) {
            super(f);
            GUI();
        }
    }

    private class CustomDialog extends Dialog implements ActionListener {

        private Button open, close;

        private void GUI() {
            setTitle("Dialog");
            setSize(150, 100);

            Panel p = new Panel();
            p.setLayout(new GridLayout(1, 2));
            open = new Button("Open");
            open.addActionListener(this);
            p.add(open);
            close = new Button("Finish");
            close.addActionListener(this);
            p.add(close);
            add(p);
        }

        public CustomDialog(Dialog d) {
            super(d);
            GUI();
        }

        public CustomDialog(Frame f) {
            super(f);
            GUI();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (open.equals(e.getSource())) {
                if (isPrintDialog) {
                    PrinterJob.getPrinterJob().printDialog();
                } else {
                    PrinterJob.getPrinterJob().pageDialog(new PageFormat());
                }
            } else if (close.equals(e.getSource())) {
                if (parentDialog != null) { parentDialog.dispose(); }
                if ( parentFrame != null) {  parentFrame.dispose(); }
                if (parent != null) { parent.dispose(); }
                if (dialog != null) { dialog.dispose(); }
                if ( frame != null) {  frame.dispose(); }
                if (window != null) { window.dispose(); }
            }
        }
    }

    class ParentDialog extends Dialog {
        public ParentDialog() {
            super((Frame) null);
            setTitle("Dialog");
            setSize(150, 100);
            Button dummy = new Button("Dummy");
            add(dummy);
        }
    }

    private CustomFrame  frame;
    private CustomWindow window;
    private CustomDialog dialog;
    private ParentDialog parent;

    private boolean isPrintDialog;

    private final Dialog.ModalityType modalityType;
    private final boolean setModal;

    public enum DialogParent
        {NULL_FRAME, HIDDEN_FRAME, NULL_DIALOG, HIDDEN_DIALOG, FRAME, DIALOG};
    private final DialogParent dialogParent;

    private Dialog parentDialog;
    private Frame  parentFrame;

    public Test(boolean             isPrintDlg,
                Dialog.ModalityType type,
                DialogParent        p){
        isPrintDialog = isPrintDlg;
        modalityType = type;
        setModal = false;
        dialogParent = p;
        EventQueue.invokeLater( this::createGUI );
    }

    public Test(boolean      isPrintDlg,
                boolean      modal,
                DialogParent p) {
        isPrintDialog = isPrintDlg;
        modalityType = null;
        setModal = modal;
        dialogParent = p;
        EventQueue.invokeLater( this::createGUI );
    }

    private void createGUI() {

        Window p;

        if (dialogParent == DialogParent.DIALOG) {
            parent = new ParentDialog();
            window = new CustomWindow(parent);
            p = parent;
        } else {
            frame = new CustomFrame();
            window = new CustomWindow(frame);
            p = frame;
        }

        int x = 50, y = 50;
        p.setLocation(x, y);

        y += (50 + p.getHeight());
        window.setLocation(x, y);

        switch (dialogParent) {
            case NULL_DIALOG:
                dialog = new CustomDialog((Dialog) null);
                break;
            case NULL_FRAME:
                dialog = new CustomDialog((Frame) null);
                break;
            case HIDDEN_DIALOG:
                parentDialog = new Dialog((Frame) null);
                dialog = new CustomDialog(parentDialog);
                break;
            case HIDDEN_FRAME:
                parentFrame = new Frame();
                dialog = new CustomDialog(parentFrame);
                break;
            case DIALOG:
                dialog = new CustomDialog(parent);
            case FRAME:
                dialog = new CustomDialog(frame);
                break;
        }

        y += (50 + dialog.getHeight());
        dialog.setLocation(x, y);

        if (modalityType == null) {
            dialog.setModal(setModal);
        } else {
            dialog.setModalityType(modalityType);
        }
    }

    public void start() {
        EventQueue.invokeLater(() -> {
            if (parent != null)     { parent.setVisible(true); }
            else if (frame != null) {  frame.setVisible(true); }
        });
        EventQueue.invokeLater(() -> { window.setVisible(true); });
        EventQueue.invokeLater(() -> { dialog.setVisible(true); });
    }
}
