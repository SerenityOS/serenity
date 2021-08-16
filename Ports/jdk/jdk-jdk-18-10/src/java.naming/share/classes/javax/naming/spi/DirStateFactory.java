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
package javax.naming.spi;

import javax.naming.*;
import javax.naming.directory.Attributes;
import java.util.Hashtable;

/**
  * This interface represents a factory for obtaining the state of an
  * object and corresponding attributes for binding.
  *<p>
  * The JNDI framework allows for object implementations to
  * be loaded in dynamically via {@code object factories}.
  * <p>
  * A {@code DirStateFactory} extends {@code StateFactory}
  * by allowing an {@code Attributes} instance
  * to be supplied to and be returned by the {@code getStateToBind()} method.
  * {@code DirStateFactory} implementations are intended to be used by
  * {@code DirContext} service providers.
  * When a caller binds an object using {@code DirContext.bind()},
  * he might also specify a set of attributes to be bound with the object.
  * The object and attributes to be bound are passed to
  * the {@code getStateToBind()} method of a factory.
  * If the factory processes the object and attributes, it returns
  * a corresponding pair of object and attributes to be bound.
  * If the factory does not process the object, it must return null.
  *<p>
  * For example, a caller might bind a printer object with some printer-related
  * attributes.
  *<blockquote><pre>
  * ctx.rebind("inky", printer, printerAttrs);
  *</pre></blockquote>
  * An LDAP service provider for {@code ctx} uses a {@code DirStateFactory}
  * (indirectly via {@code DirectoryManager.getStateToBind()})
  * and gives it {@code printer} and {@code printerAttrs}. A factory for
  * an LDAP directory might turn {@code printer} into a set of attributes
  * and merge that with {@code printerAttrs}. The service provider then
  * uses the resulting attributes to create an LDAP entry and updates
  * the directory.
  *
  * <p> Since {@code DirStateFactory} extends {@code StateFactory}, it
  * has two {@code getStateToBind()} methods, where one
  * differs from the other by the attributes
  * argument. {@code DirectoryManager.getStateToBind()} will only use
  * the form that accepts the attributes argument, while
  * {@code NamingManager.getStateToBind()} will only use the form that
  * does not accept the attributes argument.
  *
  * <p> Either form of the {@code getStateToBind()} method of a
  * DirStateFactory may be invoked multiple times, possibly using different
  * parameters.  The implementation is thread-safe.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see DirectoryManager#getStateToBind
  * @see DirObjectFactory
  * @since 1.3
  */
public interface DirStateFactory extends StateFactory {
/**
 * Retrieves the state of an object for binding given the object and attributes
 * to be transformed.
 *<p>
 * {@code DirectoryManager.getStateToBind()}
 * successively loads in state factories. If a factory implements
 * {@code DirStateFactory}, {@code DirectoryManager} invokes this method;
 * otherwise, it invokes {@code StateFactory.getStateToBind()}.
 * It does this until a factory produces a non-null answer.
 *<p>
 * When an exception is thrown by a factory,
 * the exception is passed on to the caller
 * of {@code DirectoryManager.getStateToBind()}. The search for other factories
 * that may produce a non-null answer is halted.
 * A factory should only throw an exception if it is sure that
 * it is the only intended factory and that no other factories
 * should be tried.
 * If this factory cannot create an object using the arguments supplied,
 * it should return null.
 * <p>
 * The {@code name} and {@code nameCtx} parameters may
 * optionally be used to specify the name of the object being created.
 * See the description of "Name and Context Parameters" in
 * {@link ObjectFactory#getObjectInstance ObjectFactory.getObjectInstance()}
 * for details.
 * If a factory uses {@code nameCtx} it should synchronize its use
 * against concurrent access, since context implementations are not
 * guaranteed to be thread-safe.
 *<p>
 * The {@code name}, {@code inAttrs}, and {@code environment} parameters
 * are owned by the caller.
 * The implementation will not modify these objects or keep references
 * to them, although it may keep references to clones or copies.
 * The object returned by this method is owned by the caller.
 * The implementation will not subsequently modify it.
 * It will contain either a new {@code Attributes} object that is
 * likewise owned by the caller, or a reference to the original
 * {@code inAttrs} parameter.
 *
 * @param obj A possibly null object whose state is to be retrieved.
 * @param name The name of this object relative to {@code nameCtx},
 *              or null if no name is specified.
 * @param nameCtx The context relative to which the {@code name}
 *              parameter is specified, or null if {@code name} is
 *              relative to the default initial context.
 * @param environment The possibly null environment to
 *              be used in the creation of the object's state.
 * @param inAttrs The possibly null attributes to be bound with the object.
 *      The factory must not modify {@code inAttrs}.
 * @return A {@code Result} containing the object's state for binding
 * and the corresponding
 * attributes to be bound; null if the object don't use this factory.
 * @throws NamingException If this factory encountered an exception
 * while attempting to get the object's state, and no other factories are
 * to be tried.
 *
 * @see DirectoryManager#getStateToBind
 */
    public Result getStateToBind(Object obj, Name name, Context nameCtx,
                                 Hashtable<?,?> environment,
                                 Attributes inAttrs)
        throws NamingException;


        /**
         * An object/attributes pair for returning the result of
         * DirStateFactory.getStateToBind().
         */
    public static class Result {
        /**
         * The possibly null object to be bound.
         */
        private Object obj;


        /**
         * The possibly null attributes to be bound.
         */
        private Attributes attrs;

        /**
          * Constructs an instance of Result.
          *
          * @param obj The possibly null object to be bound.
          * @param outAttrs The possibly null attributes to be bound.
          */
        public Result(Object obj, Attributes outAttrs) {
            this.obj = obj;
            this.attrs = outAttrs;
        }

        /**
         * Retrieves the object to be bound.
         * @return The possibly null object to be bound.
         */
        public Object getObject() { return obj; };

        /**
         * Retrieves the attributes to be bound.
         * @return The possibly null attributes to be bound.
         */
        public Attributes getAttributes() { return attrs; };

    }
}
