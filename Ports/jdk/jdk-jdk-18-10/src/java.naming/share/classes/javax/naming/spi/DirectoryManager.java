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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.NamingException;
import javax.naming.CannotProceedException;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;

import com.sun.naming.internal.ResourceManager;
import com.sun.naming.internal.FactoryEnumeration;


/**
  * This class contains methods for supporting {@code DirContext}
  * implementations.
  *<p>
  * This class is an extension of {@code NamingManager}.  It contains methods
  * for use by service providers for accessing object factories and
  * state factories, and for getting continuation contexts for
  * supporting federation.
  *<p>
  * {@code DirectoryManager} is safe for concurrent access by multiple threads.
  *<p>
  * Except as otherwise noted,
  * a {@code Name}, {@code Attributes}, or environment parameter
  * passed to any method is owned by the caller.
  * The implementation will not modify the object or keep a reference
  * to it, although it may keep a reference to a clone or copy.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see DirObjectFactory
  * @see DirStateFactory
  * @since 1.3
  */

public class DirectoryManager extends NamingManager {

    /*
     * Disallow anyone from creating one of these.
     */
    DirectoryManager() {}

    /**
      * Creates a context in which to continue a {@code DirContext} operation.
      * Operates just like {@code NamingManager.getContinuationContext()},
      * only the continuation context returned is a {@code DirContext}.
      *
      * @param cpe
      *         The non-null exception that triggered this continuation.
      * @return A non-null {@code DirContext} object for continuing the operation.
      * @throws NamingException If a naming exception occurred.
      *
      * @see NamingManager#getContinuationContext(CannotProceedException)
      */
    @SuppressWarnings("unchecked")
    public static DirContext getContinuationDirContext(
            CannotProceedException cpe) throws NamingException {

        Hashtable<Object,Object> env = (Hashtable<Object,Object>)cpe.getEnvironment();
        if (env == null) {
            env = new Hashtable<>(7);
        } else {
            // Make a (shallow) copy of the environment.
            env = (Hashtable<Object,Object>) env.clone();
        }
        env.put(CPE, cpe);

        return (new ContinuationDirContext(cpe, env));
    }

    /**
      * Creates an instance of an object for the specified object,
      * attributes, and environment.
      * <p>
      * This method is the same as {@code NamingManager.getObjectInstance}
      * except for the following differences:
      *<ul>
      *<li>
      * It accepts an {@code Attributes} parameter that contains attributes
      * associated with the object. The {@code DirObjectFactory} might use these
      * attributes to save having to look them up from the directory.
      *<li>
      * The object factories tried must implement either
      * {@code ObjectFactory} or {@code DirObjectFactory}.
      * If it implements {@code DirObjectFactory},
      * {@code DirObjectFactory.getObjectInstance()} is used, otherwise,
      * {@code ObjectFactory.getObjectInstance()} is used.
      *</ul>
      * Service providers that implement the {@code DirContext} interface
      * should use this method, not {@code NamingManager.getObjectInstance()}.
      *
      * @param refInfo The possibly null object for which to create an object.
      * @param name The name of this object relative to {@code nameCtx}.
      *         Specifying a name is optional; if it is
      *         omitted, {@code name} should be null.
      * @param nameCtx The context relative to which the {@code name}
      *         parameter is specified.  If null, {@code name} is
      *         relative to the default initial context.
      * @param environment The possibly null environment to
      *         be used in the creation of the object factory and the object.
      * @param attrs The possibly null attributes associated with refInfo.
      *         This might not be the complete set of attributes for refInfo;
      *         you might be able to read more attributes from the directory.
      * @return An object created using {@code refInfo} and {@code attrs}; or
      *         {@code refInfo} if an object cannot be created by
      *         a factory.
      * @throws NamingException If a naming exception was encountered
      *         while attempting to get a URL context, or if one of the
      *         factories accessed throws a NamingException.
      * @throws Exception If one of the factories accessed throws an
      *         exception, or if an error was encountered while loading
      *         and instantiating the factory and object classes.
      *         A factory should only throw an exception if it does not want
      *         other factories to be used in an attempt to create an object.
      *         See {@code DirObjectFactory.getObjectInstance()}.
      * @see NamingManager#getURLContext
      * @see DirObjectFactory
      * @see DirObjectFactory#getObjectInstance
      * @since 1.3
      */
    public static Object
        getObjectInstance(Object refInfo, Name name, Context nameCtx,
                          Hashtable<?,?> environment, Attributes attrs)
        throws Exception {

            ObjectFactory factory;

            ObjectFactoryBuilder builder = getObjectFactoryBuilder();
            if (builder != null) {
                // builder must return non-null factory
                factory = builder.createObjectFactory(refInfo, environment);
                if (factory instanceof DirObjectFactory) {
                    return ((DirObjectFactory)factory).getObjectInstance(
                        refInfo, name, nameCtx, environment, attrs);
                } else {
                    return factory.getObjectInstance(refInfo, name, nameCtx,
                        environment);
                }
            }

            // use reference if possible
            Reference ref = null;
            if (refInfo instanceof Reference) {
                ref = (Reference) refInfo;
            } else if (refInfo instanceof Referenceable) {
                ref = ((Referenceable)(refInfo)).getReference();
            }

            Object answer;

            if (ref != null) {
                String f = ref.getFactoryClassName();
                if (f != null) {
                    // if reference identifies a factory, use exclusively

                    factory = getObjectFactoryFromReference(ref, f);
                    if (factory instanceof DirObjectFactory) {
                        return ((DirObjectFactory)factory).getObjectInstance(
                            ref, name, nameCtx, environment, attrs);
                    } else if (factory != null) {
                        return factory.getObjectInstance(ref, name, nameCtx,
                                                         environment);
                    }
                    // No factory found, so return original refInfo.
                    // Will reach this point if factory class is not in
                    // class path and reference does not contain a URL for it
                    return refInfo;

                } else {
                    // if reference has no factory, check for addresses
                    // containing URLs
                    // ignore name & attrs params; not used in URL factory

                    answer = processURLAddrs(ref, name, nameCtx, environment);
                    if (answer != null) {
                        return answer;
                    }
                }
            }

            // try using any specified factories
            answer = createObjectFromFactories(refInfo, name, nameCtx,
                                               environment, attrs);
            return (answer != null) ? answer : refInfo;
    }

    private static Object createObjectFromFactories(Object obj, Name name,
            Context nameCtx, Hashtable<?,?> environment, Attributes attrs)
        throws Exception {

        FactoryEnumeration factories = ResourceManager.getFactories(
            Context.OBJECT_FACTORIES, environment, nameCtx);

        if (factories == null)
            return null;

        ObjectFactory factory;
        Object answer = null;
        // Try each factory until one succeeds
        while (answer == null && factories.hasMore()) {
            factory = (ObjectFactory)factories.next();
            if (factory instanceof DirObjectFactory) {
                answer = ((DirObjectFactory)factory).
                    getObjectInstance(obj, name, nameCtx, environment, attrs);
            } else {
                answer =
                    factory.getObjectInstance(obj, name, nameCtx, environment);
            }
        }
        return answer;
    }

    /**
      * Retrieves the state of an object for binding when given the original
      * object and its attributes.
      * <p>
      * This method is like {@code NamingManager.getStateToBind} except
      * for the following differences:
      *<ul>
      *<li>It accepts an {@code Attributes} parameter containing attributes
      *    that were passed to the {@code DirContext.bind()} method.
      *<li>It returns a non-null {@code DirStateFactory.Result} instance
      *    containing the object to be bound, and the attributes to
      *    accompany the binding. Either the object or the attributes may be null.
      *<li>
      * The state factories tried must each implement either
      * {@code StateFactory} or {@code DirStateFactory}.
      * If it implements {@code DirStateFactory}, then
      * {@code DirStateFactory.getStateToBind()} is called; otherwise,
      * {@code StateFactory.getStateToBind()} is called.
      *</ul>
      *
      * Service providers that implement the {@code DirContext} interface
      * should use this method, not {@code NamingManager.getStateToBind()}.
      *<p>
      * See NamingManager.getStateToBind() for a description of how
      * the list of state factories to be tried is determined.
      *<p>
      * The object returned by this method is owned by the caller.
      * The implementation will not subsequently modify it.
      * It will contain either a new {@code Attributes} object that is
      * likewise owned by the caller, or a reference to the original
      * {@code attrs} parameter.
      *
      * @param obj The non-null object for which to get state to bind.
      * @param name The name of this object relative to {@code nameCtx},
      *         or null if no name is specified.
      * @param nameCtx The context relative to which the {@code name}
      *         parameter is specified, or null if {@code name} is
      *         relative to the default initial context.
      * @param environment The possibly null environment to
      *         be used in the creation of the state factory and
      *         the object's state.
      * @param attrs The possibly null Attributes that is to be bound with the
      *         object.
      * @return A non-null DirStateFactory.Result containing
      *  the object and attributes to be bound.
      *  If no state factory returns a non-null answer, the result will contain
      *  the object ({@code obj}) itself with the original attributes.
      * @throws NamingException If a naming exception was encountered
      *         while using the factories.
      *         A factory should only throw an exception if it does not want
      *         other factories to be used in an attempt to create an object.
      *         See {@code DirStateFactory.getStateToBind()}.
      * @see DirStateFactory
      * @see DirStateFactory#getStateToBind
      * @see NamingManager#getStateToBind
      * @since 1.3
      */
    public static DirStateFactory.Result
        getStateToBind(Object obj, Name name, Context nameCtx,
                       Hashtable<?,?> environment, Attributes attrs)
        throws NamingException {

        // Get list of state factories
        FactoryEnumeration factories = ResourceManager.getFactories(
            Context.STATE_FACTORIES, environment, nameCtx);

        if (factories == null) {
            // no factories to try; just return originals
            return new DirStateFactory.Result(obj, attrs);
        }

        // Try each factory until one succeeds
        StateFactory factory;
        Object objanswer;
        DirStateFactory.Result answer = null;
        while (answer == null && factories.hasMore()) {
            factory = (StateFactory)factories.next();
            if (factory instanceof DirStateFactory) {
                answer = ((DirStateFactory)factory).
                    getStateToBind(obj, name, nameCtx, environment, attrs);
            } else {
                objanswer =
                    factory.getStateToBind(obj, name, nameCtx, environment);
                if (objanswer != null) {
                    answer = new DirStateFactory.Result(objanswer, attrs);
                }
            }
        }

        return (answer != null) ? answer :
            new DirStateFactory.Result(obj, attrs); // nothing new
    }
}
