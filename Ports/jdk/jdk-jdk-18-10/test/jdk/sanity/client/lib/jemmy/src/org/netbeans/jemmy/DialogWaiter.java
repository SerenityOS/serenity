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
package org.netbeans.jemmy;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;

/**
 * A DialogWaiter is a utility class used to look or wait for Dialogs. It
 * contains methods to search for a Dialog among the currently showing Dialogs
 * as well as methods that wait for a Dialog to show within an allotted time
 * period.
 *
 * Searches and waits can either involve search criteria applied by a
 * ComponentChooser instance or a search criteria based on the Dialog title.
 * Searches and waits can both be restricted to dialogs owned by a given window.
 *
 * <BR><BR>Timeouts used: <BR>
 * DialogWaiter.WaitDialogTimeout - time to wait dialog displayed <BR>
 * DialogWaiter.AfterDialogTimeout - time to sleep after dialog has been
 * displayed <BR>
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class DialogWaiter extends WindowWaiter implements Timeoutable, Outputable {

    private final static long WAIT_TIME = 60000;
    private final static long AFTER_WAIT_TIME = 0;

    private Timeouts timeouts;
    private TestOut output;

    /**
     * Constructor.
     */
    public DialogWaiter() {
        super();
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
    }

    /**
     * Searches for a dialog. Search among the currently showing dialogs for one
     * that meets the search criteria applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first dialog that is showing and that meets
     * the search criteria. If no such dialog can be found, a {@code null}
     * reference is returned.
     */
    public static Dialog getDialog(ComponentChooser cc) {
        return (Dialog) WindowWaiter.getWindow(new DialogSubChooser(cc));
    }

    /**
     * Searches for a dialog. The search proceeds among the currently showing
     * dialogs for the {@code index+1}'th dialog that meets the criteria
     * defined and applied by the {@code ComonentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the dialog in the set of currently
     * displayed dialogs. The first index is 0.
     * @return a reference to the {@code index+1}'th dialog that is showing
     * and that meets the search criteria. If there are fewer than
     * {@code index+1} dialogs, a {@code null} reference is returned.
     */
    public static Dialog getDialog(ComponentChooser cc, int index) {
        return (Dialog) WindowWaiter.getWindow(new DialogSubChooser(cc), index);
    }

    /**
     * Searches for a dialog by title. The search proceeds among the currently
     * showing dialogs for the first with a suitable title.
     *
     * @param title Dialog title or subtitle.
     * @param ce If {@code true} and the search is case sensitive, then a
     * match occurs when the {@code title} argument is a substring of a
     * dialog title. If {@code false} and the search is case sensitive,
     * then the {@code title} argument and the dialog title must be the
     * same. If {@code true} and the search is case insensitive, then a
     * match occurs when the {@code title} argument is a substring of the
     * dialog title after changing both to upper case. If {@code false} and
     * the search is case insensitive, then a match occurs when the
     * {@code title} argument is a substring of the dialog title after
     * changing both to upper case.
     * @param cc If {@code true} the search is case sensitive; otherwise,
     * the search is case insensitive.
     * @return a reference to the first dialog that is showing and that has a
     * suitable title. If no such dialog can be found, a {@code null}
     * reference is returned.
     */
    public static Dialog getDialog(String title, boolean ce, boolean cc) {
        return (Dialog) WindowWaiter.getWindow(new DialogByTitleChooser(title, ce, cc));
    }

    /**
     * Searches for a dialog by title. The search is for the
     * {@code index+1}'th dialog among the currently showing dialogs that
     * possess a suitable title.
     *
     * @param title Dialog title or subtitle.
     * @param ce If {@code true} and the search is case sensitive, then a
     * match occurs when the {@code title} argument is a substring of a
     * dialog title. If {@code false} and the search is case sensitive,
     * then the {@code title} argument and the dialog title must be the
     * same. If {@code true} and the search is case insensitive, then a
     * match occurs when the {@code title} argument is a substring of the
     * dialog title after changing both to upper case. If {@code false} and
     * the search is case insensitive, then a match occurs when the
     * {@code title} argument is a substring of the dialog title after
     * changing both to upper case.
     * @param cc If {@code true} the search is case sensitive; otherwise,
     * the search is case insensitive.
     * @param index Ordinal index between appropriate dialogs
     * @return a reference to the {@code index+1}'th dialog that is showing
     * and that has a suitable title. If there are fewer than
     * {@code index+1} dialogs, a {@code null} reference is returned.
     */
    public static Dialog getDialog(String title, boolean ce, boolean cc, int index) {
        return getDialog(new DialogByTitleChooser(title, ce, cc), index);
    }

    /**
     * Searches for a dialog. Search among the currently showing dialogs for the
     * first that is both owned by the {@code java.awt.Window}
     * {@code owner} and that meets the search criteria applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param owner The owner window of the dialogs to be searched.
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first dialog that is showing, has a proper
     * owner window, and that meets the search criteria. If no such dialog can
     * be found, a {@code null} reference is returned.
     */
    public static Dialog getDialog(Window owner, ComponentChooser cc) {
        return (Dialog) WindowWaiter.getWindow(owner, new DialogSubChooser(cc));
    }

    /**
     * Searches for a dialog. The search proceeds among the currently showing
     * dialogs for the {@code index+1}'th dialog that is both owned by the
     * {@code java.awt.Window} {@code owner} and that meets the
     * criteria defined and applied by the {@code ComponentChooser}
     * parameter.
     *
     * @param owner The owner window of all the dialogs to be searched.
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @param index Ordinal index between appropriate dialogs
     * @return a reference to the {@code index+1}'th dialog that is
     * showing, has the proper window ownership, and that meets the search
     * criteria. If there are fewer than {@code index+1} dialogs, a
     * {@code null} reference is returned.
     */
    public static Dialog getDialog(Window owner, ComponentChooser cc, int index) {
        return (Dialog) WindowWaiter.getWindow(owner, new DialogSubChooser(cc), index);
    }

    /**
     * Searches for a dialog by title. The search proceeds among the currently
     * showing dialogs that are owned by the {@code java.awt.Window}
     * {@code owner} for the first with a suitable title.
     *
     * @param owner A window - owner of a dialods to be checked.
     * @param title Dialog title or subtitle.
     * @param ce If {@code true} and the search is case sensitive, then a
     * match occurs when the {@code title} argument is a substring of a
     * dialog title. If {@code false} and the search is case sensitive,
     * then the {@code title} argument and the dialog title must be the
     * same. If {@code true} and the search is case insensitive, then a
     * match occurs when the {@code title} argument is a substring of the
     * dialog title after changing both to upper case. If {@code false} and
     * the search is case insensitive, then a match occurs when the
     * {@code title} argument is a substring of the dialog title after
     * changing both to upper case.
     * @param cc If {@code true} the search is case sensitive; otherwise,
     * the search is case insensitive.
     * @return a reference to the first dialog that is showing, has the proper
     * window ownership, and a suitable title. If no such dialog can be found, a
     * {@code null} reference is returned.
     */
    public static Dialog getDialog(Window owner, String title, boolean ce, boolean cc) {
        return (Dialog) WindowWaiter.getWindow(owner, new DialogByTitleChooser(title, ce, cc));
    }

    /**
     * Searches for a dialog by title. The search is for the
     * {@code index+1}'th dialog among the currently showing dialogs that
     * are owned by the {@code java.awt.Window} {@code owner} and that
     * have a suitable title.
     *
     * @param owner ?title? Dialog title or subtitle.
     * @param title ?ce? If {@code true} and the search is case sensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of a dialog title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the dialog title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the dialog title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the dialog
     * title after changing both to upper case.
     * @param ce ?cc? If {@code true} the search is case sensitive;
     * otherwise, the search is case insensitive.
     * @param cc ?index? The ordinal index of the dialog in the set of currently
     * displayed dialogs with the proper window ownership and a suitable title.
     * The first index is 0.
     * @param index Ordinal index between appropriate dialogs
     * @return a reference to the {@code index+1}'th dialog that is
     * showing, has the proper window ownership, and a suitable title. If there
     * are fewer than {@code index+1} dialogs, a {@code null}
     * reference is returned.
     */
    public static Dialog getDialog(Window owner, String title, boolean ce, boolean cc, int index) {
        return getDialog(owner, new DialogByTitleChooser(title, ce, cc), index);
    }

    static {
        Timeouts.initDefault("DialogWaiter.WaitDialogTimeout", WAIT_TIME);
        Timeouts.initDefault("DialogWaiter.AfterDialogTimeout", AFTER_WAIT_TIME);
    }

    /**
     * Defines current timeouts.
     *
     * @param timeouts ?t? A collection of timeout assignments.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #getTimeouts
     */
    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        Timeouts times = timeouts.cloneThis();
        times.setTimeout("WindowWaiter.WaitWindowTimeout",
                timeouts.getTimeout("DialogWaiter.WaitDialogTimeout"));
        times.setTimeout("WindowWaiter.AfterWindowTimeout",
                timeouts.getTimeout("DialogWaiter.AfterDialogTimeout"));
        super.setTimeouts(times);
    }

    /**
     * Return current timeouts.
     *
     * @return the collection of current timeout assignments.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #setTimeouts
     */
    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Defines print output streams or writers.
     *
     * @param output ?out? Identify the streams or writers used for print
     * output.
     * @see org.netbeans.jemmy.TestOut
     * @see org.netbeans.jemmy.Outputable
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut output) {
        this.output = output;
        super.setOutput(output);
    }

    /**
     * Returns print output streams or writers.
     *
     * @return an object that contains references to objects for printing to
     * output and err streams.
     * @see org.netbeans.jemmy.TestOut
     * @see org.netbeans.jemmy.Outputable
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Waits for a dialog to show. Wait for the {@code index+1}'th dialog
     * that meets the criteria defined and applied by the
     * {@code ComonentChooser} parameter to show up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the dialog in the set of currently
     * displayed dialogs. The first index is 0.
     * @return a reference to the {@code index+1}'th dialog that shows and
     * that meets the search criteria. If fewer than {@code index+1}
     * dialogs show up in the allotted time period then a {@code null}
     * reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(ComponentChooser ch, int index)
            throws InterruptedException {
        setTimeouts(timeouts);
        return (Dialog) waitWindow(new DialogSubChooser(ch), index);
    }

    /**
     * Waits for a dialog to show. Wait for a dialog that meets the search
     * criteria applied by the {@code ComponentChooser} parameter to show
     * up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first dialog that shows and that meets the
     * search criteria. If no such dialog can be found within the time period
     * allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(ComponentChooser ch)
            throws InterruptedException {
        return waitDialog(ch, 0);
    }

    /**
     * Waits for a dialog to show. Wait for the {@code index+1}'th dialog
     * to show with a suitable title.
     *
     * @param title Dialog title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a dialog title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the dialog title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the dialog title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the dialog
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @param index The ordinal index of the dialog in the set of currently
     * displayed dialogs with the proper window ownership and a suitable title.
     * The first index is 0.
     * @return a reference to the {@code index+1}'th dialog to show and
     * that has a suitable title. If no such dialog can be found within the time
     * period allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(String title, boolean compareExactly, boolean compareCaseSensitive, int index)
            throws InterruptedException {
        return waitDialog(new DialogByTitleChooser(title, compareExactly, compareCaseSensitive), index);
    }

    /**
     * Waits for a dialog to show. Wait for the first dialog to show with a
     * suitable title.
     *
     * @param title Dialog title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a dialog title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the dialog title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the dialog title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the dialog
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @return a reference to the first dialog to show and that has a suitable
     * title. If no such dialog can be found within the time period allotted, a
     * {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(String title, boolean compareExactly, boolean compareCaseSensitive)
            throws InterruptedException {
        return waitDialog(title, compareExactly, compareCaseSensitive, 0);
    }

    /**
     * Waits for a dialog to show. Wait for the {@code index+1}'th dialog
     * to show that is both owned by the {@code java.awt.Window}
     * {@code owner} and that meets the criteria defined and applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param owner The owner window of all the dialogs to be searched.
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the dialog in the set of currently
     * displayed dialogs with the proper window ownership and a suitable title.
     * The first index is 0.
     * @return a reference to the {@code index+1}'th dialog to show that
     * has the proper window ownership, and that meets the search criteria. If
     * there are fewer than {@code index+1} dialogs, a {@code null}
     * reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(Window owner, ComponentChooser ch, int index)
            throws InterruptedException {
        setTimeouts(timeouts);
        return (Dialog) waitWindow(owner, new DialogSubChooser(ch), index);
    }

    /**
     * Waits for a dialog to show. Wait for the first dialog to show that is
     * both owned by the {@code java.awt.Window} {@code owner} and
     * that meets the criteria defined and applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param owner The owner window of all the dialogs to be searched.
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first dialog to show that has the proper
     * window ownership, and that meets the search criteria. If there is no such
     * dialog, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(Window owner, ComponentChooser ch)
            throws InterruptedException {
        return waitDialog(owner, ch, 0);
    }

    /**
     * Waits for a dialog to show. Wait for the {@code index+1}'th dialog
     * to show with the proper owner and a suitable title.
     *
     * @param owner The owner window of all the dialogs to be searched.
     * @param title Dialog title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a dialog title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the dialog title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the dialog title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the dialog
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @param index Ordinal index between appropriate dialogs
     * @return a reference to the {@code index+1}'th dialog to show that
     * has both the proper owner window and a suitable title. If no such dialog
     * can be found within the time period allotted, a {@code null}
     * reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Dialog waitDialog(Window owner, String title, boolean compareExactly, boolean compareCaseSensitive, int index)
            throws InterruptedException {
        return waitDialog(owner, new DialogByTitleChooser(title, compareExactly, compareCaseSensitive), index);
    }

    /**
     * Waits for a dialog to show. Wait for the first dialog to show with the
     * proper owner and a suitable title.
     *
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @param owner The owner window of all the dialogs to be searched.
     * @param title Dialog title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a dialog title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the dialog title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the dialog title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the dialog
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @return a reference to the first dialog to show and that has both the
     * proper owner and a suitable title. If no such dialog can be found within
     * the time period allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @exception InterruptedException
     */
    public Dialog waitDialog(Window owner, String title, boolean compareExactly, boolean compareCaseSensitive)
            throws InterruptedException {
        return waitDialog(owner, title, compareExactly, compareCaseSensitive, 0);
    }

    /**
     * @see org.netbeans.jemmy.Waiter#getWaitingStartedMessage()
     */
    @Override
    protected String getWaitingStartedMessage() {
        return "Start to wait dialog \"" + getComponentChooser().getDescription() + "\" opened";
    }

    /**
     * Overrides WindowWaiter.getTimeoutExpiredMessage. Returns the timeout
     * expired message value.
     *
     * @see org.netbeans.jemmy.Waiter#getTimeoutExpiredMessage(long)
     * @param spendedTime Time spent for waiting
     * @return A message string.
     */
    @Override
    protected String getTimeoutExpiredMessage(long spendedTime) {
        return ("Dialog \"" + getComponentChooser().getDescription() + "\" has not been opened in "
                + spendedTime + " milliseconds");
    }

    /**
     * Overrides WindowWaiter.getActionProducedMessage. Returns the action
     * produced message value.
     *
     * @see org.netbeans.jemmy.Waiter#getActionProducedMessage(long, Object)
     * @param spendedTime Time spent for waiting
     * @param result A result of the action
     * @return A message string.
     */
    @Override
    protected String getActionProducedMessage(long spendedTime, final Object result) {
        String resultToString;
        if (result instanceof Component) {
            // run toString in dispatch thread
            resultToString = new QueueTool().invokeSmoothly(
                    new QueueTool.QueueAction<String>("result.toString()") {
                @Override
                public String launch() {
                    return result.toString();
                }
            }
            );
        } else {
            resultToString = result.toString();
        }
        return ("Dialog \"" + getComponentChooser().getDescription() + "\" has been opened in "
                + spendedTime + " milliseconds"
                + "\n    " + resultToString);
    }

    /**
     * @see org.netbeans.jemmy.Waiter#getGoldenWaitingStartedMessage()
     */
    @Override
    protected String getGoldenWaitingStartedMessage() {
        return "Start to wait dialog \"" + getComponentChooser().getDescription() + "\" opened";
    }

    /**
     * @see org.netbeans.jemmy.Waiter#getGoldenTimeoutExpiredMessage()
     */
    @Override
    protected String getGoldenTimeoutExpiredMessage() {
        return "Dialog \"" + getComponentChooser().getDescription() + "\" has not been opened";
    }

    /**
     * @see org.netbeans.jemmy.Waiter#getGoldenActionProducedMessage()
     */
    @Override
    protected String getGoldenActionProducedMessage() {
        return "Dialog \"" + getComponentChooser().getDescription() + "\" has been opened";
    }

    private static class DialogSubChooser implements ComponentChooser {

        private ComponentChooser chooser;

        public DialogSubChooser(ComponentChooser c) {
            super();
            chooser = c;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Dialog) {
                return ((FIND_INVISIBLE_WINDOWS || (comp.isShowing() && comp.isVisible()))
                        && chooser.checkComponent(comp));
            } else {
                return false;
            }
        }

        @Override
        public String getDescription() {
            return chooser.getDescription();
        }

        @Override
        public String toString() {
            return "DialogSubChooser{" + "chooser=" + chooser + '}';
        }
    }

    private static class DialogByTitleChooser implements ComponentChooser {

        String title;
        boolean compareExactly;
        boolean compareCaseSensitive;

        public DialogByTitleChooser(String t, boolean ce, boolean cc) {
            super();
            title = t;
            compareExactly = ce;
            compareCaseSensitive = cc;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Dialog) {
                if ((FIND_INVISIBLE_WINDOWS || (comp.isShowing() && comp.isVisible()))
                        && ((Dialog) comp).getTitle() != null) {
                    String titleToComp = ((Dialog) comp).getTitle();
                    String contextToComp = title;
                    if (compareCaseSensitive) {
                        titleToComp = titleToComp.toUpperCase();
                        contextToComp = contextToComp.toUpperCase();
                    }
                    if (compareExactly) {
                        return titleToComp.equals(contextToComp);
                    } else {
                        return titleToComp.contains(contextToComp);
                    }
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return title;
        }

        @Override
        public String toString() {
            return "DialogByTitleChooser{" + "title=" + title + ", compareExactly=" + compareExactly + ", compareCaseSensitive=" + compareCaseSensitive + '}';
        }
    }
}
