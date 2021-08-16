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

import java.util.Optional;

/**
 *
 * Runs actions with or without waiting.
 *
 * <BR><BR>Timeouts used: <BR>
 * ActionProducer.MaxActionTime - time action should be finished in. <BR>
 *
 * @see Action
 * @see Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ActionProducer<R, P> extends Thread
        implements Action<R, P>, Waitable<Optional<R>, P>, Timeoutable {

    private final static long ACTION_TIMEOUT = 10000;

    private Action<R, P> action;
    private boolean needWait = true;
    private P parameter;
    private boolean finished;
    private R result = null;
    private Timeouts timeouts;
    private Waiter<Optional<R>, P> waiter;
    private TestOut output;
    private Throwable exception;

    /**
     * Creates a producer for an action.
     *
     * @param a Action implementation.
     */
    public ActionProducer(Action<R, P> a) {
        super();
        waiter = new Waiter<>(this);
        action = a;
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        finished = false;
        exception = null;
    }

    /**
     * Creates a producer for an action.
     *
     * @param a Action implementation.
     * @param nw Defines if {@code produceAction} method should wait for
     * the end of action.
     */
    public ActionProducer(Action<R, P> a, boolean nw) {
        super();
        waiter = new Waiter<>(this);
        action = a;
        needWait = nw;
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        finished = false;
        exception = null;
    }

    /**
     * Creates a producer. {@code produceAction} must be overridden.
     */
    protected ActionProducer() {
        super();
        waiter = new Waiter<>(this);
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        finished = false;
        exception = null;
    }

    /**
     * Creates a producer. {@code produceAction} must be overridden.
     *
     * @param nw Defines if {@code produceAction} method should wait for
     * the end of action.
     */
    protected ActionProducer(boolean nw) {
        super();
        waiter = new Waiter<>(this);
        needWait = nw;
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        finished = false;
        exception = null;
    }

    static {
        Timeouts.initDefault("ActionProducer.MaxActionTime", ACTION_TIMEOUT);
    }

    /**
     * Set all the time outs used by sleeps or waits used by the launched
     * action.
     *
     * @param ts An object containing timeout information.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #getTimeouts
     */
    @Override
    public void setTimeouts(Timeouts ts) {
        timeouts = ts;
    }

    /**
     * Get all the time outs used by sleeps or waits used by the launched
     * action.
     *
     * @return an object containing information about timeouts.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #setTimeouts
     */
    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Identity of the streams or writers used for print output.
     *
     * @param out An object containing print output assignments for output and
     * error streams.
     * @see org.netbeans.jemmy.TestOut
     * @see org.netbeans.jemmy.Outputable
     */
    public void setOutput(TestOut out) {
        output = out;
        waiter.setOutput(output);
    }

    /**
     * Returns the exception value.
     *
     * @return a Throwable object representing the exception value
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * Defines action priority in terms of thread priority. Increase (decrease)
     * parameter value to Thread.MIN_PRIORITY(MAX_PRIORITY) in case if it is
     * less(more) then it.
     *
     * @param newPriority New thread priority.
     */
    public void setActionPriority(int newPriority) {
        int priority;
        if (newPriority < Thread.MIN_PRIORITY) {
            priority = MIN_PRIORITY;
        } else if (newPriority > Thread.MAX_PRIORITY) {
            priority = MAX_PRIORITY;
        } else {
            priority = newPriority;
        }
        try {
            setPriority(priority);
        } catch (IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the result of a launched action.
     *
     * @return a launched action's result. without waiting in case if
     * {@code getFinished()}
     * @see #getFinished()
     */
    public R getResult() {
        return result;
    }

    /**
     * Check if a launched action has finished.
     *
     * @return {@code true} if the launched action has completed, either
     * normally or with an exception;  {@code false} otherwise.
     */
    public boolean getFinished() {
        synchronized (this) {
            return finished;
        }
    }

    /**
     * Does nothing; the method should be overridden by inheritors.
     *
     * @param obj An object used to modify execution. This might be a
     * {@code java.lang.String[]} that lists a test's command line
     * arguments.
     * @return An object - result of the action.
     * @see org.netbeans.jemmy.Action
     */
    @Override
    public R launch(P obj) {
        return null;
    }

    /**
     * @return this {@code ActionProducer}'s description.
     * @see Action
     */
    @Override
    public String getDescription() {
        if (action != null) {
            return action.getDescription();
        } else {
            return "Unknown action";
        }
    }

    /**
     * Starts execution. Uses ActionProducer.MaxActionTime timeout.
     *
     * @param obj Parameter to be passed into action's
     * {@code launch(Object)} method. This parameter might be a
     * {@code java.lang.String[]} that lists a test's command line
     * arguments.
     * @param actionTimeOrigin is used for timeout reporting, if non-null.
     * @return        {@code launch(Object)} result.
     * @throws TimeoutExpiredException
     * @exception InterruptedException
     */
    public R produceAction(P obj, String actionTimeOrigin) throws InterruptedException {
        parameter = obj;
        synchronized (this) {
            finished = false;
        }
        start();
        if (needWait) {
            waiter.setTimeoutsToCloneOf(timeouts, "ActionProducer.MaxActionTime", actionTimeOrigin);
            try {
                waiter.waitAction(null);
            } catch (TimeoutExpiredException e) {
                output.printError("Timeout for \"" + getDescription()
                        + "\" action has been expired. Thread has been interrupted.");
                interrupt();
                throw (e);
            }
        }
        return result;
    }

    /**
     * Launch an action in a separate thread of execution. When the action
     * finishes, record that fact. If the action finishes normally, store it's
     * result. Use {@code getFinished()} and {@code getResult} to
     * answer questions about test completion and return value, respectively.
     *
     * @see #getFinished()
     * @see #getResult()
     * @see java.lang.Runnable
     */
    @Override
    public final void run() {
        result = null;
        try {
            result = launchAction(parameter);
        } catch (Throwable e) {
            exception = e;
        }
        synchronized (this) {
            finished = true;
        }
    }

    /**
     * Inquire for a reference to the object returned by a launched action.
     *
     * @param obj Not used.
     * @return the result returned when a launched action finishes normally.
     * @see org.netbeans.jemmy.Waitable
     */
    @Override
    public final Optional<R> actionProduced(P obj) {
        synchronized (this) {
            if (finished) {
                return Optional.ofNullable(result);
            } else {
                return null;
            }
        }
    }

    /**
     * Launch some action. Pass the action parameters and get it's return value,
     * too.
     *
     * @param obj Parameter used to configure the execution of whatever this
     * {@code ActionProducer} puts into execution.
     * @return the return value of the action.
     */
    private R launchAction(P obj) {
        if (action != null) {
            return action.launch(obj);
        } else {
            return launch(obj);
        }
    }

    @Override
    public String toString() {
        return "ActionProducer{" + "action=" + action + ", needWait=" + needWait + ", parameter=" + parameter + ", finished=" + finished + ", result=" + result + ", exception=" + exception + '}';
    }
}
