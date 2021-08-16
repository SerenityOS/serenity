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

/**
 * This interface is implemented by classes which contain complex state
 * so that other objects can track whether or not their state has changed
 * since earlier interactions with the object.
 * <p>
 * The suggested usage pattern for code that manages some trackable data
 * is as follows:
 * <pre>
 * class Trackable implements StateTrackable {
 *     TrackedInfo data;
 *     State curState = STABLE;
 *     StateTracker curTracker = null;
 *     // Hypothetical method to return a static piece of our tracked data.
 *     // Assume that Datum is either a copy of some piece of the tracked
 *     // data or that it is itself immutable.
 *     public Datum getSomeDatum(int key) {
 *         // No need to modify the state for this type of "get" call.
 *         return data.getDatum(key);
 *     }
 *     // Hypothetical method to return a raw reference to our tracked data.
 *     public TrackedInfo getRawHandleToInfo() {
 *         // Since we are returning a raw reference to our tracked
 *         // data and since we can not track what the caller will
 *         // do with that reference, we can no longer track the
 *         // state of this data.
 *         synchronized (this) {
 *             // Note: modifying both curState and curTracker requires
 *             // synchronization against the getStateTracker method.
 *             curState = UNTRACKABLE;
 *             curTracker = null;
 *         }
 *         return data;
 *     }
 *     // Hypothetical method to set a single piece of data to some
 *     // new static value.
 *     public void setSomeDatum(int key, Datum datum) {
 *         data.setDatum(key, datum);
 *         // We do not need to change state for this, we simply
 *         // invalidate the outstanding StateTracker objects.
 *         // Note: setting curTracker to null requires no synchronization.
 *         curTracker = null;
 *     }
 *     // getStateTracker must be synchronized against any code that
 *     // changes the State.
 *     public synchronized StateTracker getStateTracker() {
 *         StateTracker st = curTracker;
 *         if (st == null) {
 *             switch (curState) {
 *                 case IMMUTABLE:   st = StateTracker.ALWAYS_CURRENT; break;
 *                 case STABLE:      st = new Tracker(this); break;
 *                 case DYNAMIC:     st = StateTracker.NEVER_CURRENT; break;
 *                 case UNTRACKABLE: st = StateTracker.NEVER_CURRENT; break;
 *             }
 *             curTracker = st;
 *         }
 *         return st;
 *     }
 *
 *     static class Tracker implements StateTracker {
 *         Trackable theTrackable;
 *         public Tracker(Trackable t) {
 *             theTrackable = t;
 *         }
 *         public boolean isCurrent() {
 *             return (theTrackable.curTracker == this);
 *         }
 *     }
 * }
 * </pre>
 * Note that the mechanism shown above for invalidating outstanding
 * StateTracker objects is not the most theoretically conservative
 * way to implement state tracking in a "set" method.
 * There is a small window of opportunity after the data has changed
 * before the outstanding StateTracker objects are invalidated and
 * where they will indicate that the data is still the same as when
 * they were instantiated.
 * While this is technically inaccurate, it is acceptable since the more
 * conservative approaches to state management are much more complex and
 * cost much more in terms of performance for a very small gain in
 * correctness.
 * For example:
 * <p>
 * The most conservative approach would be to synchronize all accesses
 * and all modifications to the data, including its State.
 * This would require synchronized blocks around some potentially large
 * bodies of code which would impact the multi-threaded scalability of
 * the implementation.
 * Further, if data is to be coordinated or transferred between two
 * trackable objects then both would need to be synchronized raising
 * the possibility of deadlock unless some strict rules of priority
 * for the locking of the objects were established and followed
 * religiously.
 * Either or both of these drawbacks makes such an implementation
 * infeasible.
 * <p>
 * A less conservative approach would be to change the state of the
 * trackable object to DYNAMIC during all modifications of the data
 * and then to change it back to STABLE after those modifications
 * are complete.
 * While this state transition more accurately reflects the temporary
 * loss of tracking during the modification phase, in reality the
 * time period of the modifications would be small in most cases
 * and the 2 changes of state would each require synchronization.
 * <p>
 * In comparison the act of setting the {@code curTracker}
 * reference to null in the usage pattern above effectively invalidates
 * all outstanding {@code Tracker} objects as soon as possible
 * after the change to the data and requires very little code and no
 * synchronization to implement.
 * <p>
 * In the end it is up to the implementor of a StateTrackable object
 * how fine the granularity of State updates should be managed based
 * on the frequency and atomicity of the modifications and the
 * consequences of returning an inaccurate State for a particularly
 * small window of opportunity.
 * Most implementations are likely to follow the liberal, but efficient
 * guidelines found in the usage pattern proposed above.
 *
 * @since 1.7
 */
public interface StateTrackable {
    /**
     * An enumeration describing the current state of a trackable
     * object.
     * These values describe how often the complex data contained
     * in a trackable object can be changed and whether or not it
     * makes sense to try to track the data in its current state.
     * @see StateTrackable#getState
     * @since 1.7
     */
    public enum State {
        /**
         * The complex data will never change again.
         * Information related to the current contents of the complex
         * data can be calculated and cached indefinitely with no
         * further checks to see if the information is stale.
         */
        IMMUTABLE,

        /**
         * The complex data is currently stable, but could change at
         * some point in the future.
         * Information related to the current contents of the complex
         * data can be calculated and cached, but a StateTracker should
         * be used to verify the freshness of such precalculated data
         * before each future use.
         */
        STABLE,

        /**
         * The complex data is currently in flux and is frequently
         * changing.
         * While information related to the current contents of the
         * complex data could be calculated and cached, there is a
         * reasonably high probability that the cached information
         * would be found to be out of date by the next time it is
         * used.
         * It may also be the case that the current contents are
         * temporarily untrackable, but that they may become trackable
         * again in the future.
         */
        DYNAMIC,

        /**
         * The complex data can currently be changed by external
         * references and agents in a way that cannot be tracked.
         * If any information about the current contents of the complex
         * data were to be cached, there would be no way to determine
         * whether or not that cached information was out of date.
         */
        UNTRACKABLE,
    };

    /**
     * Returns the general state of the complex data held by this
     * object.
     * This return value can be used to determine if it makes
     * strategic sense to try and cache information about the current
     * contents of this object.
     * The StateTracker returned from the getStateTracker() method
     * will further aid in determining when the data has been
     * changed so that the caches can be verified upon future uses.
     * @return the current state of trackability of the complex
     * data stored in this object.
     * @see #getStateTracker
     * @since 1.7
     */
    public State getState();

    /**
     * Returns an object which can track future changes to the
     * complex data stored in this object.
     * If an external agent caches information about the complex
     * data of this object, it should first get a StateTracker
     * object from this method so that it can check if such
     * information is current upon future uses.
     * Note that a valid StateTracker will always be returned
     * regardless of the return value of getState(), but in some
     * cases the StateTracker may be a trivial implementation
     * which always returns the same value from its
     * {@link StateTracker#isCurrent isCurrent} method.
     * <ul>
     * <li>If the current state is {@link State#IMMUTABLE IMMUTABLE},
     * this StateTracker and any future StateTracker objects
     * returned from this method will always indicate that
     * the state has not changed.</li>
     * <li>If the current state is {@link State#UNTRACKABLE UNTRACKABLE},
     * this StateTracker and any future StateTracker objects
     * returned from this method will always indicate that
     * the state has changed.</li>
     * <li>If the current state is {@link State#DYNAMIC DYNAMIC},
     * this StateTracker may always indicate that the current
     * state has changed, but another StateTracker returned
     * from this method in the future when the state has changed
     * to {@link State#STABLE STABLE} will correctly track changes.</li>
     * <li>Otherwise the current state is {@link State#STABLE STABLE}
     * and this StateTracker will indicate whether or not the
     * data has changed since the time at which it was fetched
     * from the object.</li>
     * </ul>
     * @return an object implementing the StateTracker interface
     * that tracks whether changes have been made to the complex
     * contents of this object since it was returned.
     * @see State
     * @see #getState
     * @since 1.7
     */
    public StateTracker getStateTracker();
}
