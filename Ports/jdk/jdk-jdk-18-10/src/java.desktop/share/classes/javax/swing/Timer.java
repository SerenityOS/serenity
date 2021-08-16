/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EventListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.event.EventListenerList;

/**
 * Fires one or more {@code ActionEvent}s at specified
 * intervals. An example use is an animation object that uses a
 * <code>Timer</code> as the trigger for drawing its frames.
 *<p>
 * Setting up a timer
 * involves creating a <code>Timer</code> object,
 * registering one or more action listeners on it,
 * and starting the timer using
 * the <code>start</code> method.
 * For example,
 * the following code creates and starts a timer
 * that fires an action event once per second
 * (as specified by the first argument to the <code>Timer</code> constructor).
 * The second argument to the <code>Timer</code> constructor
 * specifies a listener to receive the timer's action events.
 *
 *<pre>
 *  int delay = 1000; //milliseconds
 *  ActionListener taskPerformer = new ActionListener() {
 *      public void actionPerformed(ActionEvent evt) {
 *          <em>//...Perform a task...</em>
 *      }
 *  };
 *  new Timer(delay, taskPerformer).start();</pre>
 *
 * <p>
 * {@code Timers} are constructed by specifying both a delay parameter
 * and an {@code ActionListener}. The delay parameter is used
 * to set both the initial delay and the delay between event
 * firing, in milliseconds. Once the timer has been started,
 * it waits for the initial delay before firing its
 * first <code>ActionEvent</code> to registered listeners.
 * After this first event, it continues to fire events
 * every time the between-event delay has elapsed, until it
 * is stopped.
 * <p>
 * After construction, the initial delay and the between-event
 * delay can be changed independently, and additional
 * <code>ActionListeners</code> may be added.
 * <p>
 * If you want the timer to fire only the first time and then stop,
 * invoke <code>setRepeats(false)</code> on the timer.
 * <p>
 * Although all <code>Timer</code>s perform their waiting
 * using a single, shared thread
 * (created by the first <code>Timer</code> object that executes),
 * the action event handlers for <code>Timer</code>s
 * execute on another thread -- the event-dispatching thread.
 * This means that the action handlers for <code>Timer</code>s
 * can safely perform operations on Swing components.
 * However, it also means that the handlers must execute quickly
 * to keep the GUI responsive.
 *
 * <p>
 * In v 1.3, another <code>Timer</code> class was added
 * to the Java platform: <code>java.util.Timer</code>.
 * Both it and <code>javax.swing.Timer</code>
 * provide the same basic functionality,
 * but <code>java.util.Timer</code>
 * is more general and has more features.
 * The <code>javax.swing.Timer</code> has two features
 * that can make it a little easier to use with GUIs.
 * First, its event handling metaphor is familiar to GUI programmers
 * and can make dealing with the event-dispatching thread
 * a bit simpler.
 * Second, its
 * automatic thread sharing means that you don't have to
 * take special steps to avoid spawning
 * too many threads.
 * Instead, your timer uses the same thread
 * used to make cursors blink,
 * tool tips appear,
 * and so on.
 *
 * <p>
 * You can find further documentation
 * and several examples of using timers by visiting
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/timer.html"
 * target = "_top">How to Use Timers</a>,
 * a section in <em>The Java Tutorial.</em>
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see java.util.Timer
 *
 *
 * @author Dave Moore
 * @since 1.2
 */
@SuppressWarnings("serial")
public class Timer implements Serializable
{
    /*
     * NOTE: all fields need to be handled in readResolve
     */

    /**
     * The collection of registered listeners
     */
    protected EventListenerList listenerList = new EventListenerList();

    // The following field strives to maintain the following:
    //    If coalesce is true, only allow one Runnable to be queued on the
    //    EventQueue and be pending (ie in the process of notifying the
    //    ActionListener). If we didn't do this it would allow for a
    //    situation where the app is taking too long to process the
    //    actionPerformed, and thus we'ld end up queing a bunch of Runnables
    //    and the app would never return: not good. This of course implies
    //    you can get dropped events, but such is life.
    // notify is used to indicate if the ActionListener can be notified, when
    // the Runnable is processed if this is true it will notify the listeners.
    // notify is set to true when the Timer fires and the Runnable is queued.
    // It will be set to false after notifying the listeners (if coalesce is
    // true) or if the developer invokes stop.
    private final transient AtomicBoolean notify = new AtomicBoolean(false);

    private volatile int     initialDelay, delay;
    private volatile boolean repeats = true, coalesce = true;

    private final transient Runnable doPostEvent;

    private static volatile boolean logTimers;

    private final transient Lock lock = new ReentrantLock();

    // This field is maintained by TimerQueue.
    // eventQueued can also be reset by the TimerQueue, but will only ever
    // happen in applet case when TimerQueues thread is destroyed.
    // access to this field is synchronized on getLock() lock.
    transient TimerQueue.DelayedTimer delayedTimer = null;

    private volatile String actionCommand;

    /**
     * Creates a {@code Timer} and initializes both the initial delay and
     * between-event delay to {@code delay} milliseconds. If {@code delay}
     * is less than or equal to zero, the timer fires as soon as it
     * is started. If <code>listener</code> is not <code>null</code>,
     * it's registered as an action listener on the timer.
     *
     * @param delay milliseconds for the initial and between-event delay
     * @param listener  an initial listener; can be <code>null</code>
     *
     * @see #addActionListener
     * @see #setInitialDelay
     * @see #setRepeats
     */
    public Timer(int delay, ActionListener listener) {
        super();
        this.delay = delay;
        this.initialDelay = delay;

        doPostEvent = new DoPostEvent();

        if (listener != null) {
            addActionListener(listener);
        }
    }

    /*
     * The timer's AccessControlContext.
     */
     @SuppressWarnings("removal")
     private transient volatile AccessControlContext acc =
            AccessController.getContext();

    /**
      * Returns the acc this timer was constructed with.
      */
     @SuppressWarnings("removal")
     final AccessControlContext getAccessControlContext() {
       if (acc == null) {
           throw new SecurityException(
                   "Timer is missing AccessControlContext");
       }
       return acc;
     }

    /**
     * DoPostEvent is a runnable class that fires actionEvents to
     * the listeners on the EventDispatchThread, via invokeLater.
     * @see Timer#post
     */
    class DoPostEvent implements Runnable
    {
        public void run() {
            if (logTimers) {
                System.out.println("Timer ringing: " + Timer.this);
            }
            if(notify.get()) {
                fireActionPerformed(new ActionEvent(Timer.this, 0, getActionCommand(),
                                                    System.currentTimeMillis(),
                                                    0));
                if (coalesce) {
                    cancelEvent();
                }
            }
        }

        Timer getTimer() {
            return Timer.this;
        }
    }

    /**
     * Adds an action listener to the <code>Timer</code>.
     *
     * @param listener the listener to add
     *
     * @see #Timer
     */
    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }


    /**
     * Removes the specified action listener from the <code>Timer</code>.
     *
     * @param listener the listener to remove
     */
    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }


    /**
     * Returns an array of all the action listeners registered
     * on this timer.
     *
     * @return all of the timer's <code>ActionListener</code>s or an empty
     *         array if no action listeners are currently registered
     *
     * @see #addActionListener
     * @see #removeActionListener
     *
     * @since 1.4
     */
    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }


    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param e the action event to fire
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i=listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }

    /**
     * Returns an array of all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s
     * upon this <code>Timer</code>.
     * <code><em>Foo</em>Listener</code>s
     * are registered using the <code>add<em>Foo</em>Listener</code> method.
     * <p>
     * You can specify the <code>listenerType</code> argument
     * with a class literal, such as <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a <code>Timer</code>
     * instance <code>t</code>
     * for its action listeners
     * with the following code:
     *
     * <pre>ActionListener[] als = (ActionListener[])(t.getListeners(ActionListener.class));</pre>
     *
     * If no such listeners exist,
     * this method returns an empty array.
     *
     * @param <T> the type of {@code EventListener} class being requested
     * @param listenerType  the type of listeners requested;
     *          this parameter should specify an interface
     *          that descends from <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s
     *          on this timer,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code> doesn't
     *          specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getActionListeners
     * @see #addActionListener
     * @see #removeActionListener
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /**
     * Returns the timer queue.
     */
    private TimerQueue timerQueue() {
        return TimerQueue.sharedInstance();
    }


    /**
     * Enables or disables the timer log. When enabled, a message
     * is posted to <code>System.out</code> whenever the timer goes off.
     *
     * @param flag  <code>true</code> to enable logging
     * @see #getLogTimers
     */
    public static void setLogTimers(boolean flag) {
        logTimers = flag;
    }


    /**
     * Returns <code>true</code> if logging is enabled.
     *
     * @return <code>true</code> if logging is enabled; otherwise, false
     * @see #setLogTimers
     */
    public static boolean getLogTimers() {
        return logTimers;
    }


    /**
     * Sets the <code>Timer</code>'s between-event delay, the number of milliseconds
     * between successive action events. This does not affect the initial delay
     * property, which can be set by the {@code setInitialDelay} method.
     *
     * @param delay the delay in milliseconds
     * @see #setInitialDelay
     */
    public void setDelay(int delay) {
        checkDelay(delay, "Invalid delay: ");
            this.delay = delay;
        }

    private static void checkDelay(int delay, String message) {
        if (delay < 0) {
            throw new IllegalArgumentException(message + delay);
    }
    }

    /**
     * Returns the delay, in milliseconds,
     * between firings of action events.
     *
     * @return the delay, in milliseconds, between firings of action events
     * @see #setDelay
     * @see #getInitialDelay
     */
    public int getDelay() {
        return delay;
    }


    /**
     * Sets the <code>Timer</code>'s initial delay, the time
     * in milliseconds to wait after the timer is started
     * before firing the first event. Upon construction, this
     * is set to be the same as the between-event delay,
     * but then its value is independent and remains unaffected
     * by changes to the between-event delay.
     *
     * @param initialDelay the initial delay, in milliseconds
     * @see #setDelay
     */
    public void setInitialDelay(int initialDelay) {
        checkDelay(initialDelay, "Invalid initial delay: ");
            this.initialDelay = initialDelay;
        }


    /**
     * Returns the {@code Timer}'s initial delay.
     *
     * @return the {@code Timer}'s intial delay, in milliseconds
     * @see #setInitialDelay
     * @see #setDelay
     */
    public int getInitialDelay() {
        return initialDelay;
    }


    /**
     * If <code>flag</code> is <code>false</code>,
     * instructs the <code>Timer</code> to send only one
     * action event to its listeners.
     *
     * @param flag specify <code>false</code> to make the timer
     *             stop after sending its first action event
     */
    public void setRepeats(boolean flag) {
        repeats = flag;
    }


    /**
     * Returns <code>true</code> (the default)
     * if the <code>Timer</code> will send
     * an action event
     * to its listeners multiple times.
     *
     * @return true if the {@code Timer} will send an action event to its
     *              listeners multiple times
     * @see #setRepeats
     */
    public boolean isRepeats() {
        return repeats;
    }


    /**
     * Sets whether the <code>Timer</code> coalesces multiple pending
     * <code>ActionEvent</code> firings.
     * A busy application may not be able
     * to keep up with a <code>Timer</code>'s event generation,
     * causing multiple
     * action events to be queued.  When processed,
     * the application sends these events one after the other, causing the
     * <code>Timer</code>'s listeners to receive a sequence of
     * events with no delay between them. Coalescing avoids this situation
     * by reducing multiple pending events to a single event.
     * <code>Timer</code>s
     * coalesce events by default.
     *
     * @param flag specify <code>false</code> to turn off coalescing
     */
    public void setCoalesce(boolean flag) {
        boolean old = coalesce;
        coalesce = flag;
        if (!old && coalesce) {
            // We must do this as otherwise if the Timer once notified
            // in !coalese mode notify will be stuck to true and never
            // become false.
            cancelEvent();
        }
    }


    /**
     * Returns {@code true} if the {@code Timer} coalesces
     * multiple pending action events.
     *
     * @return true if the {@code Timer} coalesces multiple pending
     *              action events
     * @see #setCoalesce
     */
    public boolean isCoalesce() {
        return coalesce;
    }


    /**
     * Sets the string that will be delivered as the action command
     * in <code>ActionEvent</code>s fired by this timer.
     * <code>null</code> is an acceptable value.
     *
     * @param command the action command
     * @since 1.6
     */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }


    /**
     * Returns the string that will be delivered as the action command
     * in <code>ActionEvent</code>s fired by this timer. May be
     * <code>null</code>, which is also the default.
     *
     * @return the action command used in firing events
     * @since 1.6
     */
    public String getActionCommand() {
        return actionCommand;
    }


    /**
     * Starts the <code>Timer</code>,
     * causing it to start sending action events
     * to its listeners.
     *
     * @see #stop
     */
     public void start() {
        timerQueue().addTimer(this, getInitialDelay());
    }


    /**
     * Returns {@code true} if the {@code Timer} is running.
     *
     * @return true if the {@code Timer} is running, false otherwise
     * @see #start
     */
    public boolean isRunning() {
        return timerQueue().containsTimer(this);
    }


    /**
     * Stops the <code>Timer</code>,
     * causing it to stop sending action events
     * to its listeners.
     *
     * @see #start
     */
    public void stop() {
        getLock().lock();
        try {
            cancelEvent();
            timerQueue().removeTimer(this);
        } finally {
            getLock().unlock();
        }
    }


    /**
     * Restarts the <code>Timer</code>,
     * canceling any pending firings and causing
     * it to fire with its initial delay.
     */
    public void restart() {
        getLock().lock();
        try {
            stop();
            start();
        } finally {
            getLock().unlock();
        }
    }


    /**
     * Resets the internal state to indicate this Timer shouldn't notify
     * any of its listeners. This does not stop a repeatable Timer from
     * firing again, use <code>stop</code> for that.
     */
    void cancelEvent() {
        notify.set(false);
    }


    @SuppressWarnings("removal")
    void post() {
         if (notify.compareAndSet(false, true) || !coalesce) {
             AccessController.doPrivileged(new PrivilegedAction<Void>() {
                 public Void run() {
                     SwingUtilities.invokeLater(doPostEvent);
                     return null;
                }
            }, getAccessControlContext());
        }
    }

    Lock getLock() {
        return lock;
    }

    @SuppressWarnings("removal")
    @Serial
    private void readObject(ObjectInputStream in)
        throws ClassNotFoundException, IOException
    {
        this.acc = AccessController.getContext();
        ObjectInputStream.GetField f = in.readFields();

        EventListenerList newListenerList = (EventListenerList)
                f.get("listenerList", null);
        if (newListenerList == null) {
            throw new InvalidObjectException("Null listenerList");
        }
        listenerList = newListenerList;

        int newInitialDelay = f.get("initialDelay", 0);
        checkDelay(newInitialDelay, "Invalid initial delay: ");
        initialDelay = newInitialDelay;

        int newDelay = f.get("delay", 0);
        checkDelay(newDelay, "Invalid delay: ");
        delay = newDelay;

        repeats = f.get("repeats", false);
        coalesce = f.get("coalesce", false);
        actionCommand = (String) f.get("actionCommand", null);
    }

    /*
     * We have to use readResolve because we can not initialize final
     * fields for deserialized object otherwise
     */
    @Serial
    private Object readResolve() {
        Timer timer = new Timer(getDelay(), null);
        timer.listenerList = listenerList;
        timer.initialDelay = initialDelay;
        timer.delay = delay;
        timer.repeats = repeats;
        timer.coalesce = coalesce;
        timer.actionCommand = actionCommand;
        return timer;
    }
}
