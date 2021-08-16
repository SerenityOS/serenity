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

/**
 *
 * Contains methods to search and wait Frame. A FrameWaiter is a utility class
 * used to look or wait for Frames. It contains methods to search for a Frame
 * among the currently showing Frames as well as methods that wait for a Frame
 * to show within an allotted time period.
 *
 * <BR><BR>Timeouts used: <BR>
 * FrameWaiter.WaitFrameTimeout - time to wait frame displayed. <BR>
 * FrameWaiter.AfterFrameTimeout - time to sleep after frame has been displayed.
 * <BR>
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class FrameWaiter extends WindowWaiter implements Timeoutable, Outputable {

    private final static long WAIT_TIME = 60000;
    private final static long AFTER_WAIT_TIME = 0;

    private Timeouts timeouts;
    private TestOut output;

    /**
     * Constructor.
     */
    public FrameWaiter() {
        super();
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
    }

    /**
     * Searches for a Frame. Search among the currently showing Frames for one
     * that meets the search criteria applied by the
     * {@code ComponentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first Frame that is showing and that meets the
     * search criteria. If no such Frame can be found, a {@code null}
     * reference is returned.
     */
    public static Frame getFrame(ComponentChooser cc) {
        return (Frame) WindowWaiter.getWindow(new FrameSubChooser(cc));
    }

    /**
     * Searches for a Frame. The search proceeds among the currently showing
     * Frames for the {@code index+1}'th Frame that meets the criteria
     * defined and applied by the {@code ComonentChooser} parameter.
     *
     * @param cc A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the Frame in the set of currently
     * displayed Frames. The first index is 0.
     * @return a reference to the {@code index+1}'th Frame that is showing
     * and that meets the search criteria. If there are fewer than
     * {@code index+1} Frames, a {@code null} reference is returned.
     */
    public static Frame getFrame(ComponentChooser cc, int index) {
        return (Frame) WindowWaiter.getWindow(new FrameSubChooser(cc), index);
    }

    /**
     * Searches for a Frame by title. The search proceeds among the currently
     * showing Frames for the first with a suitable title.
     *
     * @param title Frame title or subtitle.
     * @param ce If {@code true} and the search is case sensitive, then a
     * match occurs when the {@code title} argument is a substring of a
     * Frame title. If {@code false} and the search is case sensitive, then
     * the {@code title} argument and the Frame title must be the same. If
     * {@code true} and the search is case insensitive, then a match occurs
     * when the {@code title} argument is a substring of the Frame title
     * after changing both to upper case. If {@code false} and the search
     * is case insensitive, then a match occurs when the {@code title}
     * argument is a substring of the Frame title after changing both to upper
     * case.
     * @param cc If {@code true} the search is case sensitive; otherwise,
     * the search is case insensitive.
     * @return a reference to the first Frame that is showing and that has a
     * suitable title. If no such Frame can be found, a {@code null}
     * reference is returned.
     */
    public static Frame getFrame(String title, boolean ce, boolean cc) {
        return (Frame) WindowWaiter.getWindow(new FrameByTitleChooser(title, ce, cc));
    }

    /**
     * Searches for a Frame by title. The search is for the
     * {@code index+1}'th Frame among the currently showing Frames that
     * possess a suitable title.
     *
     * @param title Frame title or subtitle.
     * @param ce If {@code true} and the search is case sensitive, then a
     * match occurs when the {@code title} argument is a substring of a
     * Frame title. If {@code false} and the search is case sensitive, then
     * the {@code title} argument and the Frame title must be the same. If
     * {@code true} and the search is case insensitive, then a match occurs
     * when the {@code title} argument is a substring of the Frame title
     * after changing both to upper case. If {@code false} and the search
     * is case insensitive, then a match occurs when the {@code title}
     * argument is a substring of the Frame title after changing both to upper
     * case.
     * @param cc If {@code true} the search is case sensitive; otherwise,
     * the search is case insensitive.
     * @param index The ordinal index of the Frame in the set of currently
     * displayed Frames. The first index is 0.
     * @return a reference to the {@code index+1}'th Frame that is showing
     * and that has a suitable title. If there are fewer than
     * {@code index+1} Frames, a {@code null} reference is returned.
     */
    public static Frame getFrame(String title, boolean ce, boolean cc, int index) {
        return (Frame) WindowWaiter.getWindow(new FrameByTitleChooser(title, ce, cc), index);
    }

    static {
        Timeouts.initDefault("FrameWaiter.WaitFrameTimeout", WAIT_TIME);
        Timeouts.initDefault("FrameWaiter.AfterFrameTimeout", AFTER_WAIT_TIME);
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
        times.setTimeout("WindowWaiter.WaitWindowTimeout",
                timeouts.getTimeout("FrameWaiter.WaitFrameTimeout"));
        times.setTimeout("WindowWaiter.AfterWindowTimeout",
                timeouts.getTimeout("FrameWaiter.AfterFrameTimeout"));
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
     * Defines print output streams or writers.
     *
     * @param output Identify the streams or writers used for print output.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
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
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Waits for a Frame to show. Wait for the {@code index+1}'th Frame
     * that meets the criteria defined and applied by the
     * {@code ComonentChooser} parameter to show up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @param index The ordinal index of the Frame in the set of currently
     * displayed Frames. The first index is 0.
     * @return a reference to the {@code index+1}'th Frame that shows and
     * that meets the search criteria. If fewer than {@code index+1} Frames
     * show up in the allotted time period then a {@code null} reference is
     * returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Frame waitFrame(ComponentChooser ch, int index)
            throws InterruptedException {
        setTimeouts(timeouts);
        return (Frame) waitWindow(new FrameSubChooser(ch), index);
    }

    /**
     * Waits for a Frame to show. Wait for a Frame that meets the search
     * criteria applied by the {@code ComponentChooser} parameter to show
     * up.
     *
     * @param ch A component chooser used to define and apply the search
     * criteria.
     * @return a reference to the first Frame that shows and that meets the
     * search criteria. If no such Frame can be found within the time period
     * allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Frame waitFrame(ComponentChooser ch)
            throws InterruptedException {
        return waitFrame(ch, 0);
    }

    /**
     * Waits for a Frame to show. Wait for the {@code index+1}'th Frame to
     * show with a suitable title.
     *
     * @param title Frame title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a Frame title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the Frame title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the Frame title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the Frame
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @param index The ordinal index of the Frame in the set of currently
     * displayed Frames with the proper window ownership and a suitable title.
     * The first index is 0.
     * @return a reference to the {@code index+1}'th Frame to show and that
     * has a suitable title. If no such Frame can be found within the time
     * period allotted, a {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Frame waitFrame(String title, boolean compareExactly, boolean compareCaseSensitive, int index)
            throws InterruptedException {
        return waitFrame(new FrameByTitleChooser(title, compareExactly, compareCaseSensitive), index);
    }

    /**
     * Waits for a Frame to show. Wait for the first Frame to show with a
     * suitable title.
     *
     * @param title Frame title or subtitle.
     * @param compareExactly If {@code true} and the search is case
     * sensitive, then a match occurs when the {@code title} argument is a
     * substring of a Frame title. If {@code false} and the search is case
     * sensitive, then the {@code title} argument and the Frame title must
     * be the same. If {@code true} and the search is case insensitive,
     * then a match occurs when the {@code title} argument is a substring
     * of the Frame title after changing both to upper case. If
     * {@code false} and the search is case insensitive, then a match
     * occurs when the {@code title} argument is a substring of the Frame
     * title after changing both to upper case.
     * @param compareCaseSensitive If {@code true} the search is case
     * sensitive; otherwise, the search is case insensitive.
     * @return a reference to the first Frame to show and that has a suitable
     * title. If no such Frame can be found within the time period allotted, a
     * {@code null} reference is returned.
     * @throws TimeoutExpiredException
     * @see org.netbeans.jemmy.WindowWaiter#actionProduced(Object)
     * @exception InterruptedException
     */
    public Frame waitFrame(String title, boolean compareExactly, boolean compareCaseSensitive)
            throws InterruptedException {
        return waitFrame(title, compareExactly, compareCaseSensitive, 0);
    }

    /**
     * @see Waiter#getWaitingStartedMessage()
     */
    @Override
    protected String getWaitingStartedMessage() {
        return "Start to wait frame \"" + getComponentChooser().getDescription() + "\" opened";
    }

    /**
     * Overrides WindowWaiter.getTimeoutExpiredMessage. Returns the timeout
     * expired message value.
     *
     * @param timeSpent Time spent for waiting
     * @return a message tring
     * @see Waiter#getTimeoutExpiredMessage(long)
     */
    @Override
    protected String getTimeoutExpiredMessage(long timeSpent) {
        return ("Frame \"" + getComponentChooser().getDescription() + "\" has not been opened in "
                + timeSpent + " milliseconds");
    }

    /**
     * Overrides WindowWaiter.getActionProducedMessage. Returns the action
     * produced message value.
     *
     * @param timeSpent Time spent for waiting.
     * @param result A message string.
     * @return a message tring
     * @see Waiter#getActionProducedMessage(long, Object)
     */
    @Override
    protected String getActionProducedMessage(long timeSpent, final Object result) {
        String resultToString = null;
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
        return ("Frame \"" + getComponentChooser().getDescription() + "\" has been opened in "
                + timeSpent + " milliseconds"
                + "\n    " + resultToString);
    }

    /**
     * @see Waiter#getGoldenWaitingStartedMessage()
     */
    @Override
    protected String getGoldenWaitingStartedMessage() {
        return "Start to wait frame \"" + getComponentChooser().getDescription() + "\" opened";
    }

    /**
     * @see Waiter#getGoldenTimeoutExpiredMessage()
     */
    @Override
    protected String getGoldenTimeoutExpiredMessage() {
        return "Frame \"" + getComponentChooser().getDescription() + "\" has not been opened";
    }

    /**
     * @see Waiter#getGoldenActionProducedMessage()
     */
    @Override
    protected String getGoldenActionProducedMessage() {
        return "Frame \"" + getComponentChooser().getDescription() + "\" has been opened";
    }

    private static class FrameSubChooser implements ComponentChooser {

        private ComponentChooser chooser;

        public FrameSubChooser(ComponentChooser c) {
            super();
            chooser = c;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Frame) {
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
            return "FrameSubChooser{" + "chooser=" + chooser + '}';
        }
    }

    private static class FrameByTitleChooser implements ComponentChooser {

        String title;
        boolean compareExactly;
        boolean compareCaseSensitive;

        public FrameByTitleChooser(String t, boolean ce, boolean cc) {
            super();
            title = t;
            compareExactly = ce;
            compareCaseSensitive = cc;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Frame) {
                if ((FIND_INVISIBLE_WINDOWS || (comp.isShowing() && comp.isVisible()))
                        && ((Frame) comp).getTitle() != null) {
                    String titleToComp = ((Frame) comp).getTitle();
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
            return "FrameByTitleChooser{" + "title=" + title + ", compareExactly=" + compareExactly + ", compareCaseSensitive=" + compareCaseSensitive + '}';
        }
    }

}
