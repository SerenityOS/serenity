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
import java.awt.Frame;
import java.awt.Window;
import java.util.stream.Stream;

/**
 * A WindowWaiter is a utility class used to look or wait for Windows. It
 * contains methods to search for a Window among the currently showing Windows
 * as well as methods that wait for a Window to show within an allotted time
 * period.
 *
 * Searches and waits always involve search criteria applied by a
 * ComponentChooser instance. Searches and waits can both be restricted to
 * windows owned by a given window.
 *
 * <BR>Timeouts used: <BR>
 * WindowWaiter.WaitWindowTimeout - time to wait window displayed <BR>
 * WindowWaiter.AfterWindowTimeout - time to sleep after window has been
 * dispayed <BR>
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class WindowWaiter extends Waiter<Window, Void> implements Timeoutable {

    public static boolean FIND_INVISIBLE_WINDOWS = false;

    private final static long WAIT_TIME = 60000;
    private final static long AFTER_WAIT_TIME = 0;

    private ComponentChooser chooser;
    private Window owner = null;
    private int index = 0;
    private Timeouts timeouts;

    /**
     * Constructor.
     */
    public WindowWaiter() {
        super();
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
    }

    /**
     * Searches for a window. The search proceeds among the currently showing
     * windows for the {@code index+1}'th window that is both owned by the
     * {@code java.awt.Window} {@code owner} and that meets the
     * criteria defined and applied by the {@code ComponentChooser}
     * parameter.
     *
     * @param owner The owner window of all the windows to be searched.
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the window in the set of currently
     * displayed windows with the proper window ownership and a suitable title.
     * The first index is 0.
     * @return a reference to the {@code index+1}'th window that is
     * showing, has the proper window ownership, and that meets the search
     * criteria. If there are fewer than {@code index+1} windows, a
     * {@code null} reference is returned.
     */
    public static Window getWindow(Window owner, ComponentChooser cc, int index) {
        return getAWindow(owner, new IndexChooser(cc, index));
    }

    /**
     * Searches for a window. Search among the currently showing windows for the
     * first that is both owned by the {@code java.awt.Window}
     * {@code owner} and that meets the search criteria applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param owner The owner window of the windows to be searched.
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first window that is showing, has a proper
     * owner window, and that meets the search criteria. If no such window can
     * be found, a {@code null} reference is returned.
     */
    public static Window getWindow(Window owner, ComponentChooser cc) {
        return getWindow(owner, cc, 0);
    }

    /**
     * Searches for a window. The search proceeds among the currently showing
     * windows for the {@code index+1}'th window that meets the criteria
     * defined and applied by the {@code ComonentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the window in the set of currently
     * displayed windows. The first index is 0.
     * @return a reference to the {@code index+1}'th window that is showing
     * and that meets the search criteria. If there are fewer than
     * {@code index+1} windows, a {@code null} reference is returned.
     */
    public static Window getWindow(ComponentChooser cc, int index) {
        return getAWindow(new IndexChooser(cc, index));
    }

    /**
     * Searches for a window. Search among the currently showing windows for one
     * that meets the search criteria applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first window that is showing and that meets
     * the search criteria. If no such window can be found, a {@code null}
     * reference is returned.
     */
    public static Window getWindow(ComponentChooser cc) {
        return getWindow(cc, 0);
    }

    static {
        Timeouts.initDefault("WindowWaiter.WaitWindowTimeout", WAIT_TIME);
        Timeouts.initDefault("WindowWaiter.AfterWindowTimeout", AFTER_WAIT_TIME);
    }

    /**
     * Defines current timeouts.
     *
     * @param timeouts A collection of timeout assignments.
     * @see org.netbeans.jemmy.Timeoutable
     * @see org.netbeans.jemmy.Timeouts
     * @see #getTimeouts
     */
    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        Timeouts times = timeouts.cloneThis();
        times.setTimeout("Waiter.WaitingTime",
                timeouts.getTimeout("WindowWaiter.WaitWindowTimeout"));
        times.setTimeout("Waiter.AfterWaitingTime",
                timeouts.getTimeout("WindowWaiter.AfterWindowTimeout"));
        setWaitingTimeOrigin("WindowWaiter.WaitWindowTimeout");
        super.setTimeouts(times);
    }

    /**
     * Return current timeouts.
     *
     * @return the collection of current timeout assignments.
     * @see org.netbeans.jemmy.Timeoutable
     * @see org.netbeans.jemmy.Timeouts
     * @see #setTimeouts
     */
    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Action producer--get a window. Get a window. The search uses constraints
     * on window ownership, ordinal index, and search criteria defined by an
     * instance of {@code org.netbeans.jemmy.ComponentChooser}.
     *
     * @param obj Not used.
     * @return the window waited upon. If a window cannot be found then a
     * {@code null} reference is returned.
     * @see org.netbeans.jemmy.Action
     */
    @Override
    public Window actionProduced(Void obj) {
        return WindowWaiter.getWindow(owner, chooser, index);
    }

    /**
     * Waits for a window to show. Wait for the {@code index+1}'th window
     * that meets the criteria defined and applied by the
     * {@code ComonentChooser} parameter to show up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the window in the set of currently
     * displayed windows. The first index is 0.
     * @return a reference to the {@code index+1}'th window that shows and
     * that meets the search criteria. If fewer than {@code index+1}
     * windows show up in the allotted time period then a {@code null}
     * reference is returned.
     * @throws TimeoutExpiredException
     * @see #actionProduced(Object)
     * @exception InterruptedException
     */
    public Window waitWindow(ComponentChooser ch, int index)
            throws InterruptedException {
        chooser = ch;
        owner = null;
        this.index = index;
        return waitWindow();
    }

    /**
     * Waits for a window to show. Wait for a window that meets the search
     * criteria applied by the {@code ComponentChooser} parameter to show
     * up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first window that shows and that meets the
     * search criteria. If no such window can be found within the time period
     * allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see #actionProduced(Object)
     * @exception InterruptedException
     */
    public Window waitWindow(ComponentChooser ch)
            throws InterruptedException {
        return waitWindow(ch, 0);
    }

    /**
     * Waits for a window to show. Wait for the {@code index+1}'th window
     * to show that is both owned by the {@code java.awt.Window}
     * {@code o} and that meets the criteria defined and applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param o The owner window of all the windows to be searched.
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the window in the set of currently
     * displayed windows with the proper window ownership and a suitable title.
     * The first index is 0.
     * @return a reference to the {@code index+1}'th window to show that
     * has the proper window ownership, and that meets the search criteria. If
     * there are fewer than {@code index+1} windows, a {@code null}
     * reference is returned.
     * @throws TimeoutExpiredException
     * @see #actionProduced(Object)
     * @exception InterruptedException
     */
    public Window waitWindow(Window o, ComponentChooser ch, int index)
            throws InterruptedException {
        owner = o;
        chooser = ch;
        this.index = index;
        return waitAction(null);
    }

    /**
     * Waits for a window to show. Wait for the first window to show that is
     * both owned by the {@code java.awt.Window} {@code o} and that
     * meets the criteria defined and applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param o The owner window of all the windows to be searched.
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first window to show that has the proper
     * window ownership, and that meets the search criteria. If there is no such
     * window, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see #actionProduced(Object)
     * @exception InterruptedException
     */
    public Window waitWindow(Window o, ComponentChooser ch)
            throws InterruptedException {
        return waitWindow(o, ch, 0);
    }

    /**
     * Wait till the count of windows which meet the the search criteria becomes
     * equal to count.
     *
     * @param ch a component chooser used to define and apply the search
     * criteria.
     * @param count the number of expected windows meeting the search criteria.
     * @throws InterruptedException
     */
    public static void waitWindowCount(ComponentChooser ch, int count)
            throws InterruptedException {
        waitWindowCount(null, ch, count);
    }

    /**
     * Wait till the count of windows which meet the the search criteria becomes
     * equal to count.
     *
     * @param owner The owner window of all the windows to be checked
     * @param ch a component chooser used to define and apply the search
     * criteria.
     * @param count the number of expected windows meeting the search criteria.
     * @throws InterruptedException
     */
    public static void waitWindowCount(Window owner, ComponentChooser ch, int count)
            throws InterruptedException {
        Waiter<String, Void> stateWaiter = new Waiter<>(new Waitable<String, Void>() {
            @Override
            public String actionProduced(Void obj) {
                return countWindows(owner, ch) == count ? "" : null;
            }

            @Override
            public String getDescription() {
                return "Wait till the count of windows matching the criteria "
                        + "specified by ComponentChooser reaches :" + count;
            }

            @Override
            public String toString() {
                return "Operator.waitState.Waitable{description = "
                        + getDescription() + '}';
            }
        });
        stateWaiter.waitAction(null);
    }

    /**
     * Counts all the windows owned by the owner window which match the
     * criterion specified by component chooser.
     *
     * @param owner The owner window of all the windows to be checked
     * @param ch A component chooser used to define and apply the search
     * criteria
     * @return the number of matched windows
     */
    public static int countWindows(Window owner, ComponentChooser ch) {
        return new QueueTool().invokeAndWait(new QueueTool.QueueAction<Integer>(null) {

            @Override
            public Integer launch() {
                Window[] windows;
                if (owner == null) {
                    windows = Window.getWindows();
                } else {
                    windows = owner.getOwnedWindows();
                }
                return (int) Stream.of(windows)
                        .filter(x -> ch.checkComponent(x)).count();
            }
        });
    }

    /**
     * Counts all the windows which match the criterion specified by component
     * chooser.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria
     * @return the number of matched windows
     */
    public static int countWindows(ComponentChooser ch) {
        return countWindows(null, ch);
    }

    @Override
    public String getDescription() {
        return chooser.getDescription();
    }

    @Override
    public String toString() {
        return "WindowWaiter{" + "chooser=" + chooser + ", owner=" + owner + ", index=" + index + '}';
    }

    /**
     * Method can be used by a subclass to define chooser.
     *
     * @param ch a chooser specifying searching criteria.
     * @see #getComponentChooser
     */
    protected void setComponentChooser(ComponentChooser ch) {
        chooser = ch;
    }

    /**
     * Method can be used by a subclass to define chooser.
     *
     * @return a chooser specifying searching criteria.
     * @see #setComponentChooser
     */
    protected ComponentChooser getComponentChooser() {
        return chooser;
    }

    /**
     * Method can be used by a subclass to define window owner.
     *
     * @param owner Window-owner of the set of windows.
     * @see #getOwner
     */
    protected void setOwner(Window owner) {
        this.owner = owner;
    }

    /**
     * Method can be used by a subclass to define window owner.
     *
     * @return Window-owner of the set of windows.
     * @see #setOwner
     */
    protected Window getOwner() {
        return owner;
    }

    /**
     * @see org.netbeans.jemmy.Waiter#getWaitingStartedMessage()
     */
    @Override
    protected String getWaitingStartedMessage() {
        return "Start to wait window \"" + chooser.getDescription() + "\" opened";
    }

    /**
     * Overrides Waiter.getTimeoutExpiredMessage.
     *
     * @see org.netbeans.jemmy.Waiter#getTimeoutExpiredMessage(long)
     * @param timeSpent time from waiting start (milliseconds)
     * @return a message.
     */
    @Override
    protected String getTimeoutExpiredMessage(long timeSpent) {
        return ("Window \"" + chooser.getDescription() + "\" has not been opened in "
                + timeSpent + " milliseconds");
    }

    /**
     * Overrides Waiter.getActionProducedMessage.
     *
     * @see org.netbeans.jemmy.Waiter#getActionProducedMessage(long, Object)
     * @param timeSpent time from waiting start (milliseconds)
     * @param result result of Waitable.actionproduced method.
     * @return a message.
     */
    @Override
    protected String getActionProducedMessage(long timeSpent, final Object result) {
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
        return ("Window \"" + chooser.getDescription() + "\" has been opened in "
                + timeSpent + " milliseconds"
                + "\n    " + resultToString);
    }

    /**
     * @return a message.
     * @see org.netbeans.jemmy.Waiter#getGoldenWaitingStartedMessage()
     */
    @Override
    protected String getGoldenWaitingStartedMessage() {
        return "Start to wait window \"" + chooser.getDescription() + "\" opened";
    }

    /**
     * @return a message.
     * @see org.netbeans.jemmy.Waiter#getGoldenTimeoutExpiredMessage()
     */
    @Override
    protected String getGoldenTimeoutExpiredMessage() {
        return "Window \"" + chooser.getDescription() + "\" has not been opened";
    }

    /**
     * @return a message.
     * @see org.netbeans.jemmy.Waiter#getGoldenActionProducedMessage()
     */
    @Override
    protected String getGoldenActionProducedMessage() {
        return "Window \"" + chooser.getDescription() + "\" has been opened";
    }

    private static Window getAWindow(Window owner, ComponentChooser cc) {
        if (owner == null) {
            return WindowWaiter.getAWindow(cc);
        } else {
            Window result = null;
            Window[] windows = owner.getOwnedWindows();
            for (Window window : windows) {
                if (cc.checkComponent(window)) {
                    return window;
                }
                if ((result = WindowWaiter.getWindow(window, cc)) != null) {
                    return result;
                }
            }
            return null;
        }
    }

    private static Window getAWindow(ComponentChooser cc) {
        Window result = null;
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            if (cc.checkComponent(frame)) {
                return frame;
            }
            if ((result = WindowWaiter.getWindow(frame, cc)) != null) {
                return result;
            }
        }
        return null;
    }

    private Window waitWindow()
            throws InterruptedException {
        return waitAction(null);
    }

    private static class IndexChooser implements ComponentChooser {

        private int curIndex = 0;
        private int index;
        private ComponentChooser chooser;

        public IndexChooser(ComponentChooser ch, int i) {
            index = i;
            chooser = ch;
            curIndex = 0;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if ((FIND_INVISIBLE_WINDOWS || (comp.isShowing() && comp.isVisible()))
                    && chooser.checkComponent(comp)) {
                if (curIndex == index) {
                    return true;
                }
                curIndex++;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return chooser.getDescription();
        }

        @Override
        public String toString() {
            return "IndexChooser{" + "curIndex=" + curIndex + ", index=" + index + ", chooser=" + chooser + '}';
        }
    }
}
