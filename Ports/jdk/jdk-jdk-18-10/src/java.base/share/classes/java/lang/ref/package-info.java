/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides reference-object classes, which support a limited degree
 * of interaction with the garbage collector.  A program may use a
 * reference object to maintain a reference to some other object in
 * such a way that the latter object may still be reclaimed by the
 * collector.  A program may also arrange to be notified some time
 * after the collector has determined that the reachability of a given
 * object has changed.
 *
 *<h2>Package Specification</h2>
 *
 * A <em>reference object</em> encapsulates a reference to some other
 * object so that the reference itself may be examined and manipulated
 * like any other object.  Three types of reference objects are
 * provided, each weaker than the last: <em>soft</em>, <em>weak</em>,
 * and <em>phantom</em>.  Each type corresponds to a different level
 * of reachability, as defined below.  Soft references are for
 * implementing memory-sensitive caches, weak references are for
 * implementing canonicalizing mappings that do not prevent their keys
 * (or values) from being reclaimed, and phantom references are for
 * scheduling post-mortem cleanup actions.
 * Post-mortem cleanup actions can be registered and managed by a
 * {@link java.lang.ref.Cleaner}.
 *
 * <p> Each reference-object type is implemented by a subclass of the
 * abstract base {@link java.lang.ref.Reference} class.
 * An instance of one of these subclasses encapsulates a single
 * reference to a particular object, called the <em>referent</em>.
 * Every reference object provides methods for getting and clearing
 * the reference.  Aside from the clearing operation reference objects
 * are otherwise immutable, so no {@code set} operation is
 * provided.  A program may further subclass these subclasses, adding
 * whatever fields and methods are required for its purposes, or it
 * may use these subclasses without change.
 *
 * <h3>Notification</h3>
 *
 * A program may request to be notified of changes in an object's
 * reachability by <em>registering</em> an appropriate reference
 * object with a <em>reference queue</em> at the time the reference
 * object is created.  Some time after the garbage collector
 * determines that the reachability of the referent has changed to the
 * value corresponding to the type of the reference, it will clear the
 * reference and add it to the associated queue.  At this point, the
 * reference is considered to be <em>enqueued</em>.  The program may remove
 * references from a queue either by polling or by blocking until a
 * reference becomes available.  Reference queues are implemented by
 * the {@link java.lang.ref.ReferenceQueue} class.
 *
 * <p> The relationship between a registered reference object and its
 * queue is one-sided.  That is, a queue does not keep track of the
 * references that are registered with it.  If a registered reference
 * becomes unreachable itself, then it will never be enqueued.  It is
 * the responsibility of the program using reference objects to ensure
 * that the objects remain reachable for as long as the program is
 * interested in their referents.
 *
 * <p> While some programs will choose to dedicate a thread to
 * removing reference objects from one or more queues and processing
 * them, this is by no means necessary.  A tactic that often works
 * well is to examine a reference queue in the course of performing
 * some other fairly-frequent action.  For example, a hashtable that
 * uses weak references to implement weak keys could poll its
 * reference queue each time the table is accessed.  This is how the
 * {@link java.util.WeakHashMap} class works.  Because
 * the {@link java.lang.ref.ReferenceQueue#poll
 * ReferenceQueue.poll} method simply checks an internal data
 * structure, this check will add little overhead to the hashtable
 * access methods.
 *
 * <a id="reachability"></a>
 * <h3>Reachability</h3>
 *
 * Going from strongest to weakest, the different levels of
 * reachability reflect the life cycle of an object.  They are
 * operationally defined as follows:
 *
 * <ul>
 *
 * <li> An object is <em>strongly reachable</em> if it can be reached
 * by some thread without traversing any reference objects.  A
 * newly-created object is strongly reachable by the thread that
 * created it.
 *
 * <li> An object is <em>softly reachable</em> if it is not strongly
 * reachable but can be reached by traversing a soft reference.
 *
 * <li> An object is <em>weakly reachable</em> if it is neither
 * strongly nor softly reachable but can be reached by traversing a
 * weak reference.  When the weak references to a weakly-reachable
 * object are cleared, the object becomes eligible for finalization.
 *
 * <li> An object is <em>phantom reachable</em> if it is neither
 * strongly, softly, nor weakly reachable, it has been finalized, and
 * some phantom reference refers to it.
 *
 * <li> Finally, an object is <em>unreachable</em>, and therefore
 * eligible for reclamation, when it is not reachable in any of the
 * above ways.
 *
 * </ul>
 *
 * @author        Mark Reinhold
 * @since         1.2
 */
package java.lang.ref;
