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

/**
 *
 * Waits for something defined by Waitable interface to be happened.
 *
 * <BR><BR>Timeouts used: <BR>
 * Waiter.TimeDelta - time delta to check actionProduced result.<BR>
 * Waiter.WaitingTime - maximal waiting time<BR>
 * Waiter.AfterWaitingTime - time to sleep after waiting has been finished.<BR>
 *
 * @see Timeouts
 * @see Waitable
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class Waiter<R, P> implements Waitable<R, P>, Timeoutable, Outputable {

    private final static long TIME_DELTA = 10;
    private final static long WAIT_TIME = 60000;
    private final static long AFTER_WAIT_TIME = 0;

    private final Waitable<R, P> waitable;
    private long startTime = 0;
    private long endTime = -1;
    private R result;
    private Timeouts timeouts;
    private String waitingTimeOrigin;
    private TestOut out;

    /**
     * Replace the fine-grained timeouts with a global flag which can be set,
     * for instance, by a separate thread when a global timeout runs out.
     */
    public static volatile boolean USE_GLOBAL_TIMEOUT = false;
    public static volatile boolean globalTimeoutExpired = false;

    /**
     * Constructor.
     *
     * @param w Waitable object defining waiting criteria.
     */
    public Waiter(Waitable<R, P> w) {
        super();
        if (w == null) {
            throw new NullPointerException("Waitable cannot be null");
        }
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        waitable = w;
    }

    /**
     * Can be used from subclass. actionProduced() method must be overriden in
     * a class that uses this super constructor. actionProduced() method in
     * Waiter will throw UnsupportedOperationException whenever invoked.
     */
    protected Waiter() {
        super();
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        waitable = null;
    }

    static {
        Timeouts.initDefault("Waiter.TimeDelta", TIME_DELTA);
        Timeouts.initDefault("Waiter.WaitingTime", WAIT_TIME);
        Timeouts.initDefault("Waiter.AfterWaitingTime", AFTER_WAIT_TIME);
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
    }

    /**
     * Like {@link #setTimeouts(Timeouts)}, but clones the timeouts first, then
     * sets "Waiter.WaitingTime" to the timeout whose name is passed in. This
     * name is remembered for display in timeout error messages so people know
     * what to adjust.
     *
     * @param timeouts to be cloned and in which to look up "useAsWaitingTime".
     * @param useAsWaitingTime the name of the timeout to apply to
     * "Waiter.WaitingTime".
     * @param waitingTimeOrigin overrides {@code useAsWaitingTime} in timeout
     * reporting if non-null.
     * @return the cloned timeouts.
     */
    public Timeouts setTimeoutsToCloneOf(Timeouts timeouts,
            String useAsWaitingTime, String waitingTimeOrigin) {
        Timeouts t = timeouts.cloneThis();
        t.setTimeout("Waiter.WaitingTime", t.getTimeout(useAsWaitingTime));
        setTimeouts(t);
        setWaitingTimeOrigin((null != waitingTimeOrigin) ? waitingTimeOrigin : useAsWaitingTime);
        return t;
    }

    /**
     * @see #setTimeoutsToCloneOf(Timeouts, String, String)
     */
    public Timeouts setTimeoutsToCloneOf(Timeouts timeouts,
            String useAsWaitingTime) {
        return setTimeoutsToCloneOf(timeouts, useAsWaitingTime, null);
    }

    /**
     * Sets the origin of the current "Waiter.WaitingTime" to be shown in
     * timeout error messages
     *
     * @param origin is the name of the origin.
     */
    public void setWaitingTimeOrigin(String origin) {
        waitingTimeOrigin = origin;
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
     * @param out Identify the streams or writers used for print output.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut out) {
        this.out = out;
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
        return out;
    }

    /**
     * Waits for not null result of actionProduced method of Waitable
     * implementation passed into constructor.
     *
     * @param waitableObject Object to be passed into actionProduced method.
     * @return non null result of action.
     * @throws TimeoutExpiredException
     * @exception InterruptedException
     */
    public R waitAction(P waitableObject)
            throws InterruptedException {
        startTime = System.currentTimeMillis();
        out.printTrace(getWaitingStartedMessage());
        out.printGolden(getGoldenWaitingStartedMessage());
        long timeDelta = timeouts.getTimeout("Waiter.TimeDelta");
        while ((result = actionProduced(waitableObject)) == null) {
            Thread.sleep(timeDelta);
            if (timeoutExpired()) {
                out.printError(getTimeoutExpiredMessage(timeFromStart()));
                out.printGolden(getGoldenTimeoutExpiredMessage());
                throw (new TimeoutExpiredException(getActualDescription()));
            }
        }
        endTime = System.currentTimeMillis();
        out.printTrace(getActionProducedMessage(endTime - startTime, result));
        out.printGolden(getGoldenActionProducedMessage());
        Thread.sleep(timeouts.getTimeout("Waiter.AfterWaitingTime"));
        return result;
    }

    /**
     * This method delegates call to the waitable passed in constructor. If a
     * subclass was created using protected no-parameters constructor, it should
     * implement its own actionProduced method() as this one will throw an
     * UnsupportedOperationException.
     * @see Waitable
     * @param obj
     */
    @Override
    public R actionProduced(P obj) {
        if (waitable != null) {
            return waitable.actionProduced(obj);
        } else {
            throw new UnsupportedOperationException("actionProduced() return "
                    + "value is not defined. It used to return Boolean.TRUE "
                    + "in previous versions of Jemmy.");
        }
    }

    /**
     * @see Waitable
     */
    @Override
    public String getDescription() {
        return "Unknown waiting";
    }

    @Override
    public String toString() {
        return "Waiter{" + "description = " + getDescription() + ", waitable=" + waitable + ", startTime=" + startTime + ", endTime=" + endTime + ", result=" + result + ", waitingTimeOrigin=" + waitingTimeOrigin + '}';
    }

    /**
     * Returns message to be printed before waiting start.
     *
     * @return a message.
     */
    protected String getWaitingStartedMessage() {
        return "Start to wait action \"" + getActualDescription() + "\"";
    }

    /**
     * Returns message to be printed when waiting timeout has been expired.
     *
     * @param timeSpent time from waiting start (milliseconds)
     * @return a message.
     */
    protected String getTimeoutExpiredMessage(long timeSpent) {
        return ("\"" + getActualDescription() + "\" action has not been produced in "
                + timeSpent + " milliseconds");
    }

    /**
     * Returns message to be printed when waiting has been successfully
     * finished.
     *
     * @param timeSpent time from waiting start (milliseconds)
     * @param result result of Waitable.actionproduced method.
     * @return a message.
     */
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
        return ("\"" + getActualDescription() + "\" action has been produced in "
                + timeSpent + " milliseconds with result "
                + "\n    : " + resultToString);
    }

    /**
     * Returns message to be printed int golden output before waiting start.
     *
     * @return a message.
     */
    protected String getGoldenWaitingStartedMessage() {
        return "Start to wait action \"" + getActualDescription() + "\"";
    }

    /**
     * Returns message to be printed int golden output when waiting timeout has
     * been expired.
     *
     * @return a message.
     */
    protected String getGoldenTimeoutExpiredMessage() {
        return "\"" + getActualDescription() + "\" action has not been produced";
    }

    /**
     * Returns message to be printed int golden output when waiting has been
     * successfully finished.
     *
     * @return a message.
     */
    protected String getGoldenActionProducedMessage() {
        return "\"" + getActualDescription() + "\" action has been produced";
    }

    /**
     * Returns time from waiting start.
     *
     * @return Time spent for waiting already.
     */
    protected long timeFromStart() {
        return System.currentTimeMillis() - startTime;
    }

    private String getActualDescription() {
        final String suffix = (null == waitingTimeOrigin) ? "" : " (" + waitingTimeOrigin + ")";
        if (waitable != null) {
            return waitable.getDescription() + suffix;
        } else {
            return getDescription() + suffix;
        }
    }

    private boolean timeoutExpired() {
        if (USE_GLOBAL_TIMEOUT) {
            return globalTimeoutExpired;
        }
        return timeFromStart() > timeouts.getTimeout("Waiter.WaitingTime");
    }

}
