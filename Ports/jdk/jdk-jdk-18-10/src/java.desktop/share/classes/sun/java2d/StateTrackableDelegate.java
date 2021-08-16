/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import sun.java2d.StateTrackable.State;
import static sun.java2d.StateTrackable.State.*;

/**
 * This class provides a basic pre-packaged implementation of the
 * complete {@link StateTrackable} interface with implementations
 * of the required methods in the interface and methods to manage
 * transitions in the state of the object.
 * Classes which wish to implement StateTrackable could create an
 * instance of this class and delegate all of their implementations
 * for {@code StateTrackable} methods to the corresponding methods
 * of this class.
 */
public final class StateTrackableDelegate implements StateTrackable {
    /**
     * The {@code UNTRACKABLE_DELEGATE} provides an implementation
     * of the StateTrackable interface that is permanently in the
     * {@link State#UNTRACKABLE UNTRACKABLE} state.
     */
    public static final StateTrackableDelegate UNTRACKABLE_DELEGATE =
        new StateTrackableDelegate(UNTRACKABLE);

    /**
     * The {@code IMMUTABLE_DELEGATE} provides an implementation
     * of the StateTrackable interface that is permanently in the
     * {@link State#IMMUTABLE IMMUTABLE} state.
     */
    public static final StateTrackableDelegate IMMUTABLE_DELEGATE =
        new StateTrackableDelegate(IMMUTABLE);

    /**
     * Returns a {@code StateTrackableDelegate} instance with the
     * specified initial {@link State State}.
     * If the specified {@code State} is
     * {@link State#UNTRACKABLE UNTRACKABLE} or
     * {@link State#IMMUTABLE IMMUTABLE}
     * then the approprirate static instance
     * {@link #UNTRACKABLE_DELEGATE} or {@link #IMMUTABLE_DELEGATE}
     * is returned.
     */
    public static StateTrackableDelegate createInstance(State state) {
        switch (state) {
        case UNTRACKABLE:
            return UNTRACKABLE_DELEGATE;
        case STABLE:
            return new StateTrackableDelegate(STABLE);
        case DYNAMIC:
            return new StateTrackableDelegate(DYNAMIC);
        case IMMUTABLE:
            return IMMUTABLE_DELEGATE;
        default:
            throw new InternalError("unknown state");
        }
    }

    private State theState;
    StateTracker theTracker;   // package private for easy access from tracker
    private int numDynamicAgents;

    /**
     * Constructs a StateTrackableDelegate object with the specified
     * initial State.
     */
    private StateTrackableDelegate(State state) {
        this.theState = state;
    }

    /**
     * @inheritDoc
     * @since 1.7
     */
    public State getState() {
        return theState;
    }

    /**
     * @inheritDoc
     * @since 1.7
     */
    public synchronized StateTracker getStateTracker() {
        StateTracker st = theTracker;
        if (st == null) {
            switch (theState) {
            case IMMUTABLE:
                st = StateTracker.ALWAYS_CURRENT;
                break;
            case STABLE:
                st = new StateTracker() {
                    public boolean isCurrent() {
                        return (theTracker == this);
                    }
                };
                break;
            case DYNAMIC:
                // We return the NEVER_CURRENT tracker, but that is
                // just temporary while we are in the DYNAMIC state.
                // NO BREAK
            case UNTRACKABLE:
                st = StateTracker.NEVER_CURRENT;
                break;
            }
            theTracker = st;
        }
        return st;
    }

    /**
     * This method provides an easy way for delegating classes to
     * change the overall {@link State State} of the delegate to
     * {@link State#IMMUTABLE IMMUTABLE}.
     * @throws IllegalStateException if the current state is
     *         {@link State#UNTRACKABLE UNTRACKABLE}
     * @see #setUntrackable
     * @since 1.7
     */
    public synchronized void setImmutable() {
        if (theState == UNTRACKABLE || theState == DYNAMIC) {
            throw new IllegalStateException("UNTRACKABLE or DYNAMIC "+
                                            "objects cannot become IMMUTABLE");
        }
        theState = IMMUTABLE;
        theTracker = null;
    }

    /**
     * This method provides an easy way for delegating classes to
     * change the overall {@link State State} of the delegate to
     * {@link State#UNTRACKABLE UNTRACKABLE}.
     * This method is typically called when references to the
     * internal data buffers have been made public.
     * @throws IllegalStateException if the current state is
     *         {@link State#IMMUTABLE IMMUTABLE}
     * @see #setImmutable
     * @since 1.7
     */
    public synchronized void setUntrackable() {
        if (theState == IMMUTABLE) {
            throw new IllegalStateException("IMMUTABLE objects cannot "+
                                            "become UNTRACKABLE");
        }
        theState = UNTRACKABLE;
        theTracker = null;
    }

    /**
     * This method provides an easy way for delegating classes to
     * manage temporarily setting the overall {@link State State}
     * of the delegate to {@link State#DYNAMIC DYNAMIC}
     * during well-defined time frames of dynamic pixel updating.
     * This method should be called once before each flow of control
     * that might dynamically update the pixels in an uncontrolled
     * or unpredictable fashion.
     * <p>
     * The companion method {@link #removeDynamicAgent} method should
     * also be called once after each such flow of control has ended.
     * Failing to call the remove method will result in this object
     * permanently becoming {@link State#DYNAMIC DYNAMIC}
     * and therefore effectively untrackable.
     * <p>
     * This method will only change the {@link State State} of the
     * delegate if it is currently {@link State#STABLE STABLE}.
     *
     * @throws IllegalStateException if the current state is
     *         {@link State#IMMUTABLE IMMUTABLE}
     * @since 1.7
     */
    public synchronized void addDynamicAgent() {
        if (theState == IMMUTABLE) {
            throw new IllegalStateException("Cannot change state from "+
                                            "IMMUTABLE");
        }
        ++numDynamicAgents;
        if (theState == STABLE) {
            theState = DYNAMIC;
            theTracker = null;
        }
    }

    /**
     * This method provides an easy way for delegating classes to
     * manage restoring the overall {@link State State} of the
     * delegate back to {@link State#STABLE STABLE}
     * after a well-defined time frame of dynamic pixel updating.
     * This method should be called once after each flow of control
     * that might dynamically update the pixels in an uncontrolled
     * or unpredictable fashion has ended.
     * <p>
     * The companion method {@link #addDynamicAgent} method should
     * have been called at some point before each such flow of
     * control began.
     * If this method is called without having previously called
     * the add method, the {@link State State} of this object
     * will become unreliable.
     * <p>
     * This method will only change the {@link State State} of the
     * delegate if the number of outstanding dynamic agents has
     * gone to 0 and it is currently
     * {@link State#DYNAMIC DYNAMIC}.
     *
     * @since 1.7
     */
    protected synchronized void removeDynamicAgent() {
        if (--numDynamicAgents == 0 && theState == DYNAMIC) {
            theState = STABLE;
            theTracker = null;
        }
    }

    /**
     * This method provides an easy way for delegating classes to
     * indicate that the contents have changed.
     * This method will invalidate outstanding StateTracker objects
     * so that any other agents which maintain cached information
     * about the pixels will know to refresh their cached copies.
     * This method should be called after every modification to
     * the data, such as any calls to any of the setElem methods.
     * <p>
     * Note that, for efficiency, this method does not check the
     * {@link State State} of the object to see if it is compatible
     * with being marked dirty
     * (i.e. not {@link State#IMMUTABLE IMMUTABLE}).
     * It is up to the callers to enforce the fact that an
     * {@code IMMUTABLE} delegate is never modified.
     * @since 1.7
     */
    public void markDirty() {
        theTracker = null;
    }
}
