/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;

import javax.accessibility.AccessibleContext;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.DialogWaiter;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeouts;

/**
 * <BR><BR>Timeouts used: <BR>
 * DialogWaiter.WaitDialogTimeout - time to wait dialog displayed <BR>
 * DialogWaiter.AfterDialogTimeout - time to sleep after dialog has been
 * dispayed <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JDialogOperator extends DialogOperator {

    /**
     * Constructor.
     *
     * @param w a component
     */
    public JDialogOperator(JDialog w) {
        super(w);
    }

    /**
     * Constructs a JDialogOperator object.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @param env an operator to copy environment from.
     */
    public JDialogOperator(ComponentChooser chooser, int index, Operator env) {
        this(waitJDialog(new JDialogFinder(chooser),
                index,
                env.getTimeouts(),
                env.getOutput()));
        copyEnvironment(env);
    }

    /**
     * Constructs a JDialogOperator object.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JDialogOperator(ComponentChooser chooser, int index) {
        this(chooser, index, Operator.getEnvironmentOperator());
    }

    /**
     * Constructs a JDialogOperator object.
     *
     * @param chooser a component chooser specifying searching criteria.
     */
    public JDialogOperator(ComponentChooser chooser) {
        this(chooser, 0);
    }

    /**
     * Constructs a JDialogOperator object.
     *
     * @param owner window - owner
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JDialogOperator(WindowOperator owner, ComponentChooser chooser, int index) {
        this((JDialog) owner.
                waitSubWindow(new JDialogFinder(chooser),
                        index));
        copyEnvironment(owner);
    }

    /**
     * Constructs a JDialogOperator object.
     *
     * @param owner window - owner
     * @param chooser a component chooser specifying searching criteria.
     */
    public JDialogOperator(WindowOperator owner, ComponentChooser chooser) {
        this(owner, chooser, 0);
    }

    /**
     * Constructor. Waits for the dialog with "title" subtitle. Uses owner's
     * timeout and output for waiting and to init operator.
     *
     * @param owner Operator pointing to a window owner.
     * @param title The desired title.
     * @param index Ordinal index. The first dialog has {@code index} 0.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public JDialogOperator(WindowOperator owner, String title, int index) {
        this(waitJDialog(owner,
                new JDialogFinder(new DialogByTitleFinder(title,
                        owner.getComparator())),
                index));
        copyEnvironment(owner);
    }

    /**
     * Constructor. Waits for the dialog with "title" subtitle. Uses owner's
     * timeout and output for waiting and to init operator.
     *
     * @param owner Operator pointing to a window owner.
     * @param title The desired title.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public JDialogOperator(WindowOperator owner, String title) {
        this(owner, title, 0);
    }

    /**
     * Constructor. Waits for the index'th dialog between owner's children. Uses
     * owner'th timeout and output for waiting and to init operator.
     *
     * @param owner Operator pointing to a window owner.
     * @param index Ordinal component index.
     *
     */
    public JDialogOperator(WindowOperator owner, int index) {
        this(waitJDialog(owner,
                new JDialogFinder(),
                index));
        copyEnvironment(owner);
    }

    /**
     * Constructor. Waits for the first dialog between owner's children. Uses
     * owner'th timeout and output for waiting and to init operator.
     *
     * @param owner Operator pointing to a window owner.
     *
     */
    public JDialogOperator(WindowOperator owner) {
        this(owner, 0);
    }

    /**
     * Constructor. Waits for the dialog with "title" subtitle. Constructor can
     * be used in complicated cases when output or timeouts should differ from
     * default.
     *
     * @param title a window title
     * @param index Ordinal component index.
     * @param env an operator to copy environment from.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public JDialogOperator(String title, int index, Operator env) {
        this(new JDialogFinder(new DialogByTitleFinder(title,
                env.getComparator())),
                index, env);
    }

    /**
     * Constructor. Waits for the dialog with "title" subtitle. Uses current
     * timeouts and output values.
     *
     * @param title a window title
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see JemmyProperties#getCurrentTimeouts()
     * @see JemmyProperties#getCurrentOutput()
     *
     */
    public JDialogOperator(String title, int index) {
        this(title, index,
                ComponentOperator.getEnvironmentOperator());
    }

    /**
     * Constructor. Waits for the dialog with "title" subtitle. Uses current
     * timeouts and output values.
     *
     * @param title a window title
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see JemmyProperties#getCurrentTimeouts()
     * @see JemmyProperties#getCurrentOutput()
     *
     */
    public JDialogOperator(String title) {
        this(title, 0);
    }

    /**
     * Constructor. Waits for the index'th dialog. Uses current timeout and
     * output for waiting and to init operator.
     *
     * @param index Ordinal component index.
     *
     */
    public JDialogOperator(int index) {
        this(waitJDialog(new JDialogFinder(),
                index,
                ComponentOperator.getEnvironmentOperator().getTimeouts(),
                ComponentOperator.getEnvironmentOperator().getOutput()));
        copyEnvironment(ComponentOperator.getEnvironmentOperator());
    }

    /**
     * Constructor. Waits for the first dialog. Uses current timeout and output
     * for waiting and to init operator.
     *
     */
    public JDialogOperator() {
        this(0);
    }

    /**
     * Searches an index'th dialog.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(ComponentChooser chooser, int index) {
        return (JDialog) DialogWaiter.getDialog(new JDialogFinder(chooser), index);
    }

    /**
     * Searches a dialog.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(ComponentChooser chooser) {
        return findJDialog(chooser, 0);
    }

    /**
     * Searches an index'th dialog by title.
     *
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(String title, boolean ce, boolean cc, int index) {
        return ((JDialog) DialogWaiter.
                getDialog(new JDialogFinder(new DialogByTitleFinder(title,
                        new DefaultStringComparator(ce, cc))),
                        index));
    }

    /**
     * Searches a dialog by title.
     *
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(String title, boolean ce, boolean cc) {
        return findJDialog(title, ce, cc, 0);
    }

    /**
     * Searches an index'th dialog between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(Window owner, ComponentChooser chooser, int index) {
        return (JDialog) DialogWaiter.getDialog(owner, new JDialogFinder(chooser), index);
    }

    /**
     * Searches a dialog between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param chooser a component chooser specifying searching criteria.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(Window owner, ComponentChooser chooser) {
        return findJDialog(owner, chooser, 0);
    }

    /**
     * Searches an index'th dialog by title between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(Window owner, String title, boolean ce, boolean cc, int index) {
        return ((JDialog) DialogWaiter.
                getDialog(owner,
                        new JDialogFinder(new DialogByTitleFinder(title,
                                new DefaultStringComparator(ce, cc))),
                        index));
    }

    /**
     * Searches a dialog by title between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return JDialog instance or null if component was not found.
     */
    public static JDialog findJDialog(Window owner, String title, boolean ce, boolean cc) {
        return findJDialog(owner, title, ce, cc, 0);
    }

    /**
     * Waits an index'th dialog.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(ComponentChooser chooser, int index) {
        return (waitJDialog(chooser, index,
                JemmyProperties.getCurrentTimeouts(),
                JemmyProperties.getCurrentOutput()));
    }

    /**
     * Waits a dialog.
     *
     * @param chooser a component chooser specifying searching criteria.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(ComponentChooser chooser) {
        return waitJDialog(chooser, 0);
    }

    /**
     * Waits an index'th dialog by title.
     *
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(String title, boolean ce, boolean cc, int index) {
        return (waitJDialog(new JDialogFinder(new DialogByTitleFinder(title,
                new DefaultStringComparator(ce, cc))),
                index));
    }

    /**
     * Waits a dialog by title.
     *
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(String title, boolean ce, boolean cc) {
        return waitJDialog(title, ce, cc, 0);
    }

    /**
     * Waits an index'th dialog between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(Window owner, ComponentChooser chooser, int index) {
        return (waitJDialog(owner, chooser, index,
                JemmyProperties.getCurrentTimeouts(),
                JemmyProperties.getCurrentOutput()));
    }

    /**
     * Waits a dialog between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param chooser a component chooser specifying searching criteria.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(Window owner, ComponentChooser chooser) {
        return waitJDialog(owner, chooser, 0);
    }

    /**
     * Waits an index'th dialog by title between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @param index an index between appropriate ones.
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(Window owner, String title, boolean ce, boolean cc, int index) {
        return (waitJDialog(owner, new JDialogFinder(new DialogByTitleFinder(title,
                new DefaultStringComparator(ce, cc))),
                index));
    }

    /**
     * Waits a dialog by title between owner's owned windows.
     *
     * @param owner Window - dialog owner.
     * @param title Dialog title
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return JDialog instance or null if component was not found.
     *
     */
    public static JDialog waitJDialog(Window owner, String title, boolean ce, boolean cc) {
        return waitJDialog(owner, title, ce, cc, 0);
    }

    /**
     * Searhs for modal dialog currently staying on top.
     *
     * @return dialog or null if no modal dialog is currently displayed.
     */
    public static Dialog getTopModalDialog() {
        return (DialogWaiter.getDialog(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                if (comp instanceof Dialog) {
                    Dialog dialog = (Dialog) comp;
                    if (dialog.isModal()) {
                        Window[] ow = dialog.getOwnedWindows();
                        for (Window anOw : ow) {
                            if (anOw.isVisible()) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Upper modal dialog";
            }

            @Override
            public String toString() {
                return "JDialogOperator.getTopModalDialog.ComponentChooser{description = " + getDescription() + '}';
            }
        }));
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JDialog.getAccessibleContext()} through queue
     */
    public AccessibleContext getAccessibleContext() {
        return (runMapping(new MapAction<AccessibleContext>("getAccessibleContext") {
            @Override
            public AccessibleContext map() {
                return getSource().getAccessibleContext();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getContentPane()} through queue
     */
    public Container getContentPane() {
        return (runMapping(new MapAction<Container>("getContentPane") {
            @Override
            public Container map() {
                return ((JDialog) getSource()).getContentPane();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getDefaultCloseOperation()} through queue
     */
    public int getDefaultCloseOperation() {
        return (runMapping(new MapIntegerAction("getDefaultCloseOperation") {
            @Override
            public int map() {
                return ((JDialog) getSource()).getDefaultCloseOperation();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getGlassPane()} through queue
     */
    public Component getGlassPane() {
        return (runMapping(new MapAction<Component>("getGlassPane") {
            @Override
            public Component map() {
                return ((JDialog) getSource()).getGlassPane();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getJMenuBar()} through queue
     */
    public JMenuBar getJMenuBar() {
        return (runMapping(new MapAction<JMenuBar>("getJMenuBar") {
            @Override
            public JMenuBar map() {
                return ((JDialog) getSource()).getJMenuBar();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getLayeredPane()} through queue
     */
    public JLayeredPane getLayeredPane() {
        return (runMapping(new MapAction<JLayeredPane>("getLayeredPane") {
            @Override
            public JLayeredPane map() {
                return ((JDialog) getSource()).getLayeredPane();
            }
        }));
    }

    /**
     * Maps {@code JDialog.getRootPane()} through queue
     */
    public JRootPane getRootPane() {
        return (runMapping(new MapAction<JRootPane>("getRootPane") {
            @Override
            public JRootPane map() {
                return ((JDialog) getSource()).getRootPane();
            }
        }));
    }

    /**
     * Maps {@code JDialog.setContentPane(Container)} through queue
     */
    public void setContentPane(final Container container) {
        runMapping(new MapVoidAction("setContentPane") {
            @Override
            public void map() {
                ((JDialog) getSource()).setContentPane(container);
            }
        });
    }

    /**
     * Maps {@code JDialog.setDefaultCloseOperation(int)} through queue
     */
    public void setDefaultCloseOperation(final int i) {
        runMapping(new MapVoidAction("setDefaultCloseOperation") {
            @Override
            public void map() {
                ((JDialog) getSource()).setDefaultCloseOperation(i);
            }
        });
    }

    /**
     * Maps {@code JDialog.setGlassPane(Component)} through queue
     */
    public void setGlassPane(final Component component) {
        runMapping(new MapVoidAction("setGlassPane") {
            @Override
            public void map() {
                ((JDialog) getSource()).setGlassPane(component);
            }
        });
    }

    /**
     * Maps {@code JDialog.setJMenuBar(JMenuBar)} through queue
     */
    public void setJMenuBar(final JMenuBar jMenuBar) {
        runMapping(new MapVoidAction("setJMenuBar") {
            @Override
            public void map() {
                ((JDialog) getSource()).setJMenuBar(jMenuBar);
            }
        });
    }

    /**
     * Maps {@code JDialog.setLayeredPane(JLayeredPane)} through queue
     */
    public void setLayeredPane(final JLayeredPane jLayeredPane) {
        runMapping(new MapVoidAction("setLayeredPane") {
            @Override
            public void map() {
                ((JDialog) getSource()).setLayeredPane(jLayeredPane);
            }
        });
    }

    /**
     * Maps {@code JDialog.setLocationRelativeTo(Component)} through queue
     */
    public void setLocationRelativeTo(final Component component) {
        runMapping(new MapVoidAction("setLocationRelativeTo") {
            @Override
            public void map() {
                ((JDialog) getSource()).setLocationRelativeTo(component);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * A method to be used from subclasses. Uses timeouts and output passed as
     * parameters during the waiting.
     *
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @param timeouts timeouts to be used during the waiting.
     * @param output an output to be used during the waiting.
     * @return Component instance or null if component was not found.
     */
    protected static JDialog waitJDialog(ComponentChooser chooser, int index,
            Timeouts timeouts, TestOut output) {
        try {
            DialogWaiter waiter = new DialogWaiter();
            waiter.setTimeouts(timeouts);
            waiter.setOutput(output);
            return ((JDialog) waiter.
                    waitDialog(new JDialogFinder(chooser), index));
        } catch (InterruptedException e) {
            output.printStackTrace(e);
            return null;
        }
    }

    /**
     * A method to be used from subclasses. Uses {@code owner}'s timeouts
     * and output during the waiting.
     *
     * @param owner a window - dialog owner.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Component instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    protected static JDialog waitJDialog(WindowOperator owner, ComponentChooser chooser, int index) {
        return (waitJDialog((Window) owner.getSource(),
                chooser, index,
                owner.getTimeouts(), owner.getOutput()));
    }

    /**
     * A method to be used from subclasses. Uses timeouts and output passed as
     * parameters during the waiting.
     *
     * @param owner a window - dialog owner.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @param timeouts timeouts to be used during the waiting.
     * @param output an output to be used during the waiting.
     * @return Component instance or null if component was not found.
     */
    protected static JDialog waitJDialog(Window owner, ComponentChooser chooser, int index,
            Timeouts timeouts, TestOut output) {
        try {
            DialogWaiter waiter = new DialogWaiter();
            waiter.setTimeouts(timeouts);
            waiter.setOutput(output);
            return ((JDialog) waiter.
                    waitDialog(owner, new JDialogFinder(chooser), index));
        } catch (InterruptedException e) {
            JemmyProperties.getCurrentOutput().printStackTrace(e);
            return null;
        }
    }

    /**
     * Checks component type.
     */
    public static class JDialogFinder extends Finder {

        /**
         * Constructs JDialogFinder.
         *
         * @param sf other searching criteria.
         */
        public JDialogFinder(ComponentChooser sf) {
            super(JDialog.class, sf);
        }

        /**
         * Constructs JDialogFinder.
         */
        public JDialogFinder() {
            super(JDialog.class);
        }
    }
}
