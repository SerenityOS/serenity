/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.event;

import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;


/**
 * Contains methods for registering/deregistering listeners to be notified of
 * events fired when objects named in a context changes.
 *
 *<h2>Target</h2>
 * The name parameter in the {@code addNamingListener()} methods is referred
 * to as the <em>target</em>. The target, along with the scope, identify
 * the object(s) that the listener is interested in.
 * It is possible to register interest in a target that does not exist, but
 * there might be limitations in the extent to which this can be
 * supported by the service provider and underlying protocol/service.
 *<p>
 * If a service only supports registration for existing
 * targets, an attempt to register for a nonexistent target
 * results in a {@code NameNotFoundException} being thrown as early as possible,
 * preferably at the time {@code addNamingListener()} is called, or if that is
 * not possible, the listener will receive the exception through the
 * {@code NamingExceptionEvent}.
 *<p>
 * Also, for service providers that only support registration for existing
 * targets, when the target that a listener has registered for is
 * subsequently removed from the namespace, the listener is notified
 * via a {@code NamingExceptionEvent} (containing a
 *{@code NameNotFoundException}).
 *<p>
 * An application can use the method {@code targetMustExist()} to check
 * whether an {@code EventContext} supports registration
 * of nonexistent targets.
 *
 *<h2>Event Source</h2>
 * The {@code EventContext} instance on which you invoke the
 * registration methods is the <em>event source</em> of the events that are
 * (potentially) generated.
 * The source is <em>not necessarily</em> the object named by the target.
 * Only when the target is the empty name is the object named by the target
 * the source.
 * In other words, the target,
 * along with the scope parameter, are used to identify
 * the object(s) that the listener is interested in, but the event source
 * is the {@code EventContext} instance with which the listener
 * has registered.
 *<p>
 * For example, suppose a listener makes the following registration:
 *<blockquote><pre>
 *      NamespaceChangeListener listener = ...;
 *      src.addNamingListener("x", SUBTREE_SCOPE, listener);
 *</pre></blockquote>
 * When an object named "x/y" is subsequently deleted, the corresponding
 * {@code NamingEvent} ({@code evt})  must contain:
 *<blockquote><pre>
 *      evt.getEventContext() == src
 *      evt.getOldBinding().getName().equals("x/y")
 *</pre></blockquote>
 *<p>
 * Furthermore, listener registration/deregistration is with
 * the {@code EventContext}
 * <em>instance</em>, and not with the corresponding object in the namespace.
 * If the program intends at some point to remove a listener, then it needs to
 * keep a reference to the {@code EventContext} instance on
 * which it invoked {@code addNamingListener()} (just as
 * it needs to keep a reference to the listener in order to remove it
 * later). It cannot expect to do a {@code lookup()} and get another instance of
 * an {@code EventContext} on which to perform the deregistration.
 *<h2>Lifetime of Registration</h2>
 * A registered listener becomes deregistered when:
 *<ul>
 *<li>It is removed using {@code removeNamingListener()}.
 *<li>An exception is thrown while collecting information about the events.
 *  That is, when the listener receives a {@code NamingExceptionEvent}.
 *<li>{@code Context.close()} is invoked on the {@code EventContext}
 * instance with which it has registered.
 </ul>
 * Until that point, an {@code EventContext} instance that has outstanding
 * listeners will continue to exist and be maintained by the service provider.
 *
 *<h2>Listener Implementations</h2>
 * The registration/deregistration methods accept an instance of
 * {@code NamingListener}. There are subinterfaces of {@code NamingListener}
 * for different of event types of {@code NamingEvent}.
 * For example, the {@code ObjectChangeListener}
 * interface is for the {@code NamingEvent.OBJECT_CHANGED} event type.
 * To register interest in multiple event types, the listener implementation
 * should implement multiple {@code NamingListener} subinterfaces and use a
 * single invocation of {@code addNamingListener()}.
 * In addition to reducing the number of method calls and possibly the code size
 * of the listeners, this allows some service providers to optimize the
 * registration.
 *
 *<h2>Threading Issues</h2>
 *
 * Like {@code Context} instances in general, instances of
 * {@code EventContext} are not guaranteed to be thread-safe.
 * Care must be taken when multiple threads are accessing the same
 * {@code EventContext} concurrently.
 * See the
 * <a href=package-summary.html#THREADING>package description</a>
 * for more information on threading issues.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 * @since 1.3
 */

public interface EventContext extends Context {
    /**
     * Constant for expressing interest in events concerning the object named
     * by the target.
     *<p>
     * The value of this constant is {@code 0}.
     */
    public static final int OBJECT_SCOPE = 0;

    /**
     * Constant for expressing interest in events concerning objects
     * in the context named by the target,
     * excluding the context named by the target.
     *<p>
     * The value of this constant is {@code 1}.
     */
    public static final int ONELEVEL_SCOPE = 1;

    /**
     * Constant for expressing interest in events concerning objects
     * in the subtree of the object named by the target, including the object
     * named by the target.
     *<p>
     * The value of this constant is {@code 2}.
     */
    public static final int SUBTREE_SCOPE = 2;


    /**
     * Adds a listener for receiving naming events fired
     * when the object(s) identified by a target and scope changes.
     *
     * The event source of those events is this context. See the
     * class description for a discussion on event source and target.
     * See the descriptions of the constants {@code OBJECT_SCOPE},
     * {@code ONELEVEL_SCOPE}, and {@code SUBTREE_SCOPE} to see how
     * {@code scope} affects the registration.
     *<p>
     * {@code target} needs to name a context only when {@code scope} is
     * {@code ONELEVEL_SCOPE}.
     * {@code target} may name a non-context if {@code scope} is either
     * {@code OBJECT_SCOPE} or {@code SUBTREE_SCOPE}.  Using
     * {@code SUBTREE_SCOPE} for a non-context might be useful,
     * for example, if the caller does not know in advance whether {@code target}
     * is a context and just wants to register interest in the (possibly
     * degenerate subtree) rooted at {@code target}.
     *<p>
     * When the listener is notified of an event, the listener may
     * in invoked in a thread other than the one in which
     * {@code addNamingListener()} is executed.
     * Care must be taken when multiple threads are accessing the same
     * {@code EventContext} concurrently.
     * See the
     * <a href=package-summary.html#THREADING>package description</a>
     * for more information on threading issues.
     *
     * @param target A nonnull name to be resolved relative to this context.
     * @param scope One of {@code OBJECT_SCOPE}, {@code ONELEVEL_SCOPE}, or
     * {@code SUBTREE_SCOPE}.
     * @param l  The nonnull listener.
     * @throws NamingException If a problem was encountered while
     * adding the listener.
     * @see #removeNamingListener
     */
    void addNamingListener(Name target, int scope, NamingListener l)
        throws NamingException;

    /**
     * Adds a listener for receiving naming events fired
     * when the object named by the string target name and scope changes.
     *
     * See the overload that accepts a {@code Name} for details.
     *
     * @param target The nonnull string name of the object resolved relative
     * to this context.
     * @param scope One of {@code OBJECT_SCOPE}, {@code ONELEVEL_SCOPE}, or
     * {@code SUBTREE_SCOPE}.
     * @param l  The nonnull listener.
     * @throws NamingException If a problem was encountered while
     * adding the listener.
     * @see #removeNamingListener
     */
    void addNamingListener(String target, int scope, NamingListener l)
        throws NamingException;

    /**
     * Removes a listener from receiving naming events fired
     * by this {@code EventContext}.
     * The listener may have registered more than once with this
     * {@code EventContext}, perhaps with different target/scope arguments.
     * After this method is invoked, the listener will no longer
     * receive events with this {@code EventContext} instance
     * as the event source (except for those events already in the process of
     * being dispatched).
     * If the listener was not, or is no longer, registered with
     * this {@code EventContext} instance, this method does not do anything.
     *
     * @param l  The nonnull listener.
     * @throws NamingException If a problem was encountered while
     * removing the listener.
     * @see #addNamingListener
     */
    void removeNamingListener(NamingListener l) throws NamingException;

    /**
     * Determines whether a listener can register interest in a target
     * that does not exist.
     *
     * @return true if the target must exist; false if the target need not exist.
     * @throws NamingException If the context's behavior in this regard cannot
     * be determined.
     */
    boolean targetMustExist() throws NamingException;
}
