/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.InvocationTargetException;

/**
 * The {@code PrintingStatus} provides a dialog that displays progress
 * of the printing job and provides a way to abort it
 * <p>
 * Methods of these class are thread safe, although most Swing methods
 * are not. Please see
 * <A HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">Concurrency
 * in Swing</A> for more information.
 *
 * @author Alexander Potochkin
 * @since 1.6
 */

public class PrintingStatus {

    private final PrinterJob job;
    private final Component parent;
    private JDialog abortDialog;

    private JButton abortButton;
    private JLabel statusLabel;
    private MessageFormat statusFormat;
    private final AtomicBoolean isAborted = new AtomicBoolean(false);

    // the action that will abort printing
    @SuppressWarnings("serial") // anonymous class
    private final Action abortAction = new AbstractAction() {
        public void actionPerformed(ActionEvent ae) {
            if (!isAborted.get()) {
                isAborted.set(true);

                // update the status abortDialog to indicate aborting
                abortButton.setEnabled(false);
                abortDialog.setTitle(
                    UIManager.getString("PrintingDialog.titleAbortingText"));
                statusLabel.setText(
                    UIManager.getString("PrintingDialog.contentAbortingText"));

                // cancel the PrinterJob
                job.cancel();
            }
        }
    };

    private final WindowAdapter closeListener = new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
            abortAction.actionPerformed(null);
        }
    };

    /**
     * Creates PrintingStatus instance
     *
     * @param parent a <code>Component</code> object to be used
     *               as parent component for PrintingStatus dialog
     * @param job    a <code>PrinterJob</code> object to be cancelled
     *               using this <code>PrintingStatus</code> dialog
     * @return a <code>PrintingStatus</code> object
     */
    public static PrintingStatus
            createPrintingStatus(Component parent, PrinterJob job) {
        return new PrintingStatus(parent, job);
    }

    protected PrintingStatus(Component parent, PrinterJob job) {
        this.job = job;
        this.parent = parent;
    }

    private void init() {
        // prepare the status JOptionPane
        String progressTitle =
            UIManager.getString("PrintingDialog.titleProgressText");

        String dialogInitialContent =
            UIManager.getString("PrintingDialog.contentInitialText");

        // this one's a MessageFormat since it must include the page
        // number in its text
        statusFormat = new MessageFormat(
            UIManager.getString("PrintingDialog.contentProgressText"));

        String abortText =
            UIManager.getString("PrintingDialog.abortButtonText");
        String abortTooltip =
            UIManager.getString("PrintingDialog.abortButtonToolTipText");
        int abortMnemonic =
            getInt("PrintingDialog.abortButtonMnemonic", -1);
        int abortMnemonicIndex =
            getInt("PrintingDialog.abortButtonDisplayedMnemonicIndex", -1);

        abortButton = new JButton(abortText);
        abortButton.addActionListener(abortAction);

        abortButton.setToolTipText(abortTooltip);
        if (abortMnemonic != -1) {
            abortButton.setMnemonic(abortMnemonic);
        }
        if (abortMnemonicIndex != -1) {
            abortButton.setDisplayedMnemonicIndex(abortMnemonicIndex);
        }
        statusLabel = new JLabel(dialogInitialContent);
        JOptionPane abortPane = new JOptionPane(statusLabel,
            JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null, new Object[]{abortButton},
            abortButton);
        abortPane.getActionMap().put("close", abortAction);

        // The dialog should be centered over the viewport if the table is in one
        if (parent != null && parent.getParent() instanceof JViewport) {
            abortDialog =
                    abortPane.createDialog(parent.getParent(), progressTitle);
        } else {
            abortDialog = abortPane.createDialog(parent, progressTitle);
        }
        // clicking the X button should not hide the dialog
        abortDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        abortDialog.addWindowListener(closeListener);
    }

    /**
     * Shows PrintingStatus dialog.
     * if dialog is modal this method returns only
     * after <code>dispose()</code> was called otherwise returns immediately
     *
     * @param isModal <code>true</code> this dialog should be modal;
     *                <code>false</code> otherwise.
     * @see #dispose
     */
    public void showModal(final boolean isModal) {
        if (SwingUtilities.isEventDispatchThread()) {
            showModalOnEDT(isModal);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        showModalOnEDT(isModal);
                    }
                });
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            } catch(InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                   throw (RuntimeException) cause;
                } else if (cause instanceof Error) {
                   throw (Error) cause;
                } else {
                   throw new RuntimeException(cause);
                }
            }
        }
    }

    /**
     * The EDT part of the showModal method.
     *
     * This method is to be called on the EDT only.
     */
    private void showModalOnEDT(boolean isModal) {
        assert SwingUtilities.isEventDispatchThread();
        init();
        abortDialog.setModal(isModal);
        abortDialog.setVisible(true);
    }

    /**
     * Disposes modal PrintingStatus dialog
     *
     * @see #showModal(boolean)
     */
    public void dispose() {
        if (SwingUtilities.isEventDispatchThread()) {
            disposeOnEDT();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    disposeOnEDT();
                }
            });
        }
    }

    /**
     * The EDT part of the dispose method.
     *
     * This method is to be called on the EDT only.
     */
    private void disposeOnEDT() {
        assert SwingUtilities.isEventDispatchThread();
        if (abortDialog != null) {
            abortDialog.removeWindowListener(closeListener);
            abortDialog.dispose();
            abortDialog = null;
        }
    }

    /**
     * Returns whether the printng was aborted using this PrintingStatus
     *
     * @return whether the printng was aborted using this PrintingStatus
     */
    public boolean isAborted() {
        return isAborted.get();
    }

    /**
     * Returns printable which is used to track the current page being
     * printed in this PrintingStatus
     *
     * @param printable to be used to create notification printable
     * @return printable which is used to track the current page being
     *         printed in this PrintingStatus
     * @throws NullPointerException if <code>printable</code> is <code>null</code>
     */
    public Printable createNotificationPrintable(Printable printable) {
        return new NotificationPrintable(printable);
    }

    private class NotificationPrintable implements Printable {
        private final Printable printDelegatee;

        public NotificationPrintable(Printable delegatee) {
            if (delegatee == null) {
                throw new NullPointerException("Printable is null");
            }
            this.printDelegatee = delegatee;
        }

        public int print(final Graphics graphics,
                         final PageFormat pageFormat, final int pageIndex)
                throws PrinterException {

            final int retVal =
                printDelegatee.print(graphics, pageFormat, pageIndex);
            if (retVal != NO_SUCH_PAGE && !isAborted()) {
                if (SwingUtilities.isEventDispatchThread()) {
                    updateStatusOnEDT(pageIndex);
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            updateStatusOnEDT(pageIndex);
                        }
                    });
                }
            }
            return retVal;
        }

        /**
         * The EDT part of the print method.
         *
         * This method is to be called on the EDT only.
         */
        private void updateStatusOnEDT(int pageIndex) {
            assert SwingUtilities.isEventDispatchThread();
            Object[] pageNumber = new Object[]{
                pageIndex + 1};
            statusLabel.setText(statusFormat.format(pageNumber));
        }
    }

    /**
     * Duplicated from UIManager to make it visible
     */
    static int getInt(Object key, int defaultValue) {
        Object value = UIManager.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch(NumberFormatException nfe) {
            }
        }
        return defaultValue;
    }
}
