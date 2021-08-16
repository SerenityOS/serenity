/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

import java.lang.ref.Reference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.sun.beans.introspect.EventSetInfo;

/**
 * An EventSetDescriptor describes a group of events that a given Java
 * bean fires.
 * <P>
 * The given group of events are all delivered as method calls on a single
 * event listener interface, and an event listener object can be registered
 * via a call on a registration method supplied by the event source.
 *
 * @since 1.1
 */
public class EventSetDescriptor extends FeatureDescriptor {

    private MethodDescriptor[] listenerMethodDescriptors;
    private MethodDescriptor addMethodDescriptor;
    private MethodDescriptor removeMethodDescriptor;
    private MethodDescriptor getMethodDescriptor;

    private Reference<Method[]> listenerMethodsRef;
    private Reference<? extends Class<?>> listenerTypeRef;

    private boolean unicast;
    private boolean inDefaultEventSet = true;

    /**
     * Creates an {@code EventSetDescriptor} assuming that you are
     * following the most simple standard design pattern where a named
     * event "fred" is (1) delivered as a call on the single method of
     * interface FredListener, (2) has a single argument of type FredEvent,
     * and (3) where the FredListener may be registered with a call on an
     * addFredListener method of the source component and removed with a
     * call on a removeFredListener method.
     *
     * @param sourceClass  The class firing the event.
     * @param eventSetName  The programmatic name of the event.  E.g. "fred".
     *          Note that this should normally start with a lower-case character.
     * @param listenerType  The target interface that events
     *          will get delivered to.
     * @param listenerMethodName  The method that will get called when the event gets
     *          delivered to its target listener interface.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName,
                Class<?> listenerType, String listenerMethodName)
                throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType,
             new String[] { listenerMethodName },
             Introspector.ADD_PREFIX + getListenerClassName(listenerType),
             Introspector.REMOVE_PREFIX + getListenerClassName(listenerType),
             Introspector.GET_PREFIX + getListenerClassName(listenerType) + "s");

        String eventName = NameGenerator.capitalize(eventSetName) + "Event";
        Method[] listenerMethods = getListenerMethods();
        if (listenerMethods.length > 0) {
            Class<?>[] args = getParameterTypes(getClass0(), listenerMethods[0]);
            // Check for EventSet compliance. Special case for vetoableChange. See 4529996
            if (!"vetoableChange".equals(eventSetName) && !args[0].getName().endsWith(eventName)) {
                throw new IntrospectionException("Method \"" + listenerMethodName +
                                                 "\" should have argument \"" +
                                                 eventName + "\"");
            }
        }
    }

    private static String getListenerClassName(Class<?> cls) {
        String className = cls.getName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /**
     * Creates an {@code EventSetDescriptor} from scratch using
     * string names.
     *
     * @param sourceClass  The class firing the event.
     * @param eventSetName The programmatic name of the event set.
     *          Note that this should normally start with a lower-case character.
     * @param listenerType  The Class of the target interface that events
     *          will get delivered to.
     * @param listenerMethodNames The names of the methods that will get called
     *          when the event gets delivered to its target listener interface.
     * @param addListenerMethodName  The name of the method on the event source
     *          that can be used to register an event listener object.
     * @param removeListenerMethodName  The name of the method on the event source
     *          that can be used to de-register an event listener object.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public EventSetDescriptor(Class<?> sourceClass,
                String eventSetName,
                Class<?> listenerType,
                String[] listenerMethodNames,
                String addListenerMethodName,
                String removeListenerMethodName)
                throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType,
             listenerMethodNames, addListenerMethodName,
             removeListenerMethodName, null);
    }

    /**
     * This constructor creates an EventSetDescriptor from scratch using
     * string names.
     *
     * @param sourceClass  The class firing the event.
     * @param eventSetName The programmatic name of the event set.
     *          Note that this should normally start with a lower-case character.
     * @param listenerType  The Class of the target interface that events
     *          will get delivered to.
     * @param listenerMethodNames The names of the methods that will get called
     *          when the event gets delivered to its target listener interface.
     * @param addListenerMethodName  The name of the method on the event source
     *          that can be used to register an event listener object.
     * @param removeListenerMethodName  The name of the method on the event source
     *          that can be used to de-register an event listener object.
     * @param getListenerMethodName The method on the event source that
     *          can be used to access the array of event listener objects.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     * @since 1.4
     */
    public EventSetDescriptor(Class<?> sourceClass,
                String eventSetName,
                Class<?> listenerType,
                String[] listenerMethodNames,
                String addListenerMethodName,
                String removeListenerMethodName,
                String getListenerMethodName)
                throws IntrospectionException {
        if (sourceClass == null || eventSetName == null || listenerType == null) {
            throw new NullPointerException();
        }
        setName(eventSetName);
        setClass0(sourceClass);
        setListenerType(listenerType);

        Method[] listenerMethods = new Method[listenerMethodNames.length];
        for (int i = 0; i < listenerMethodNames.length; i++) {
            // Check for null names
            if (listenerMethodNames[i] == null) {
                throw new NullPointerException();
            }
            listenerMethods[i] = getMethod(listenerType, listenerMethodNames[i], 1);
        }
        setListenerMethods(listenerMethods);

        setAddListenerMethod(getMethod(sourceClass, addListenerMethodName, 1));
        setRemoveListenerMethod(getMethod(sourceClass, removeListenerMethodName, 1));

        // Be more forgiving of not finding the getListener method.
        Method method = Introspector.findMethod(sourceClass, getListenerMethodName, 0);
        if (method != null) {
            setGetListenerMethod(method);
        }
    }

    private static Method getMethod(Class<?> cls, String name, int args)
        throws IntrospectionException {
        if (name == null) {
            return null;
        }
        Method method = Introspector.findMethod(cls, name, args);
        if ((method == null) || Modifier.isStatic(method.getModifiers())) {
            throw new IntrospectionException("Method not found: " + name +
                                             " on class " + cls.getName());
        }
        return method;
    }

    /**
     * Creates an {@code EventSetDescriptor} from scratch using
     * {@code java.lang.reflect.Method} and {@code java.lang.Class} objects.
     *
     * @param eventSetName The programmatic name of the event set.
     * @param listenerType The Class for the listener interface.
     * @param listenerMethods  An array of Method objects describing each
     *          of the event handling methods in the target listener.
     * @param addListenerMethod  The method on the event source
     *          that can be used to register an event listener object.
     * @param removeListenerMethod  The method on the event source
     *          that can be used to de-register an event listener object.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public EventSetDescriptor(String eventSetName,
                Class<?> listenerType,
                Method[] listenerMethods,
                Method addListenerMethod,
                Method removeListenerMethod)
                throws IntrospectionException {
        this(eventSetName, listenerType, listenerMethods,
             addListenerMethod, removeListenerMethod, null);
    }

    /**
     * This constructor creates an EventSetDescriptor from scratch using
     * java.lang.reflect.Method and java.lang.Class objects.
     *
     * @param eventSetName The programmatic name of the event set.
     * @param listenerType The Class for the listener interface.
     * @param listenerMethods  An array of Method objects describing each
     *          of the event handling methods in the target listener.
     * @param addListenerMethod  The method on the event source
     *          that can be used to register an event listener object.
     * @param removeListenerMethod  The method on the event source
     *          that can be used to de-register an event listener object.
     * @param getListenerMethod The method on the event source
     *          that can be used to access the array of event listener objects.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     * @since 1.4
     */
    public EventSetDescriptor(String eventSetName,
                Class<?> listenerType,
                Method[] listenerMethods,
                Method addListenerMethod,
                Method removeListenerMethod,
                Method getListenerMethod)
                throws IntrospectionException {
        setName(eventSetName);
        setListenerMethods(listenerMethods);
        setAddListenerMethod(addListenerMethod);
        setRemoveListenerMethod( removeListenerMethod);
        setGetListenerMethod(getListenerMethod);
        setListenerType(listenerType);
    }

    EventSetDescriptor(String base, EventSetInfo info, Method... methods) {
        setName(Introspector.decapitalize(base));
        setListenerMethods(methods);
        setAddListenerMethod(info.getAddMethod());
        setRemoveListenerMethod(info.getRemoveMethod());
        setGetListenerMethod(info.getGetMethod());
        setListenerType(info.getListenerType());
        setUnicast(info.isUnicast());
    }

    /**
     * Creates an {@code EventSetDescriptor} from scratch using
     * {@code java.lang.reflect.MethodDescriptor} and {@code java.lang.Class}
     *  objects.
     *
     * @param eventSetName The programmatic name of the event set.
     * @param listenerType The Class for the listener interface.
     * @param listenerMethodDescriptors  An array of MethodDescriptor objects
     *           describing each of the event handling methods in the
     *           target listener.
     * @param addListenerMethod  The method on the event source
     *          that can be used to register an event listener object.
     * @param removeListenerMethod  The method on the event source
     *          that can be used to de-register an event listener object.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public EventSetDescriptor(String eventSetName,
                Class<?> listenerType,
                MethodDescriptor[] listenerMethodDescriptors,
                Method addListenerMethod,
                Method removeListenerMethod)
                throws IntrospectionException {
        setName(eventSetName);
        this.listenerMethodDescriptors = (listenerMethodDescriptors != null)
                ? listenerMethodDescriptors.clone()
                : null;
        setAddListenerMethod(addListenerMethod);
        setRemoveListenerMethod(removeListenerMethod);
        setListenerType(listenerType);
    }

    /**
     * Gets the {@code Class} object for the target interface.
     *
     * @return The Class object for the target interface that will
     * get invoked when the event is fired.
     */
    public Class<?> getListenerType() {
        return (this.listenerTypeRef != null)
                ? this.listenerTypeRef.get()
                : null;
    }

    private void setListenerType(Class<?> cls) {
        this.listenerTypeRef = getWeakReference(cls);
    }

    /**
     * Gets the methods of the target listener interface.
     *
     * @return An array of {@code Method} objects for the target methods
     * within the target listener interface that will get called when
     * events are fired.
     */
    public synchronized Method[] getListenerMethods() {
        Method[] methods = getListenerMethods0();
        if (methods == null) {
            if (listenerMethodDescriptors != null) {
                methods = new Method[listenerMethodDescriptors.length];
                for (int i = 0; i < methods.length; i++) {
                    methods[i] = listenerMethodDescriptors[i].getMethod();
                }
            }
            setListenerMethods(methods);
        }
        return methods;
    }

    private void setListenerMethods(Method[] methods) {
        if (methods == null) {
            return;
        }
        if (listenerMethodDescriptors == null) {
            listenerMethodDescriptors = new MethodDescriptor[methods.length];
            for (int i = 0; i < methods.length; i++) {
                listenerMethodDescriptors[i] = new MethodDescriptor(methods[i]);
            }
        }
        this.listenerMethodsRef = getSoftReference(methods);
    }

    private Method[] getListenerMethods0() {
        return (this.listenerMethodsRef != null)
                ? this.listenerMethodsRef.get()
                : null;
    }

    /**
     * Gets the {@code MethodDescriptor}s of the target listener interface.
     *
     * @return An array of {@code MethodDescriptor} objects for the target methods
     * within the target listener interface that will get called when
     * events are fired.
     */
    public synchronized MethodDescriptor[] getListenerMethodDescriptors() {
        return (this.listenerMethodDescriptors != null)
                ? this.listenerMethodDescriptors.clone()
                : null;
    }

    /**
     * Gets the method used to add event listeners.
     *
     * @return The method used to register a listener at the event source.
     */
    public synchronized Method getAddListenerMethod() {
        return getMethod(this.addMethodDescriptor);
    }

    private synchronized void setAddListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        addMethodDescriptor = new MethodDescriptor(method);
        setTransient(method.getAnnotation(Transient.class));
    }

    /**
     * Gets the method used to remove event listeners.
     *
     * @return The method used to remove a listener at the event source.
     */
    public synchronized Method getRemoveListenerMethod() {
        return getMethod(this.removeMethodDescriptor);
    }

    private synchronized void setRemoveListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        removeMethodDescriptor = new MethodDescriptor(method);
        setTransient(method.getAnnotation(Transient.class));
    }

    /**
     * Gets the method used to access the registered event listeners.
     *
     * @return The method used to access the array of listeners at the event
     *         source or null if it doesn't exist.
     * @since 1.4
     */
    public synchronized Method getGetListenerMethod() {
        return getMethod(this.getMethodDescriptor);
    }

    private synchronized void setGetListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        getMethodDescriptor = new MethodDescriptor(method);
        setTransient(method.getAnnotation(Transient.class));
    }

    /**
     * Mark an event set as unicast (or not).
     *
     * @param unicast  True if the event set is unicast.
     */
    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    /**
     * Normally event sources are multicast.  However there are some
     * exceptions that are strictly unicast.
     *
     * @return  {@code true} if the event set is unicast.
     *          Defaults to {@code false}.
     */
    public boolean isUnicast() {
        return unicast;
    }

    /**
     * Marks an event set as being in the "default" set (or not).
     * By default this is {@code true}.
     *
     * @param inDefaultEventSet {@code true} if the event set is in
     *                          the "default" set,
     *                          {@code false} if not
     */
    public void setInDefaultEventSet(boolean inDefaultEventSet) {
        this.inDefaultEventSet = inDefaultEventSet;
    }

    /**
     * Reports if an event set is in the "default" set.
     *
     * @return  {@code true} if the event set is in
     *          the "default" set.  Defaults to {@code true}.
     */
    public boolean isInDefaultEventSet() {
        return inDefaultEventSet;
    }

    /*
     * Package-private constructor
     * Merge two event set descriptors.  Where they conflict, give the
     * second argument (y) priority over the first argument (x).
     *
     * @param x  The first (lower priority) EventSetDescriptor
     * @param y  The second (higher priority) EventSetDescriptor
     */
    EventSetDescriptor(EventSetDescriptor x, EventSetDescriptor y) {
        super(x,y);
        listenerMethodDescriptors = x.listenerMethodDescriptors;
        if (y.listenerMethodDescriptors != null) {
            listenerMethodDescriptors = y.listenerMethodDescriptors;
        }

        listenerTypeRef = x.listenerTypeRef;
        if (y.listenerTypeRef != null) {
            listenerTypeRef = y.listenerTypeRef;
        }

        addMethodDescriptor = x.addMethodDescriptor;
        if (y.addMethodDescriptor != null) {
            addMethodDescriptor = y.addMethodDescriptor;
        }

        removeMethodDescriptor = x.removeMethodDescriptor;
        if (y.removeMethodDescriptor != null) {
            removeMethodDescriptor = y.removeMethodDescriptor;
        }

        getMethodDescriptor = x.getMethodDescriptor;
        if (y.getMethodDescriptor != null) {
            getMethodDescriptor = y.getMethodDescriptor;
        }

        unicast = y.unicast;
        if (!x.inDefaultEventSet || !y.inDefaultEventSet) {
            inDefaultEventSet = false;
        }
    }

    /*
     * Package-private dup constructor
     * This must isolate the new object from any changes to the old object.
     */
    EventSetDescriptor(EventSetDescriptor old) {
        super(old);
        if (old.listenerMethodDescriptors != null) {
            int len = old.listenerMethodDescriptors.length;
            listenerMethodDescriptors = new MethodDescriptor[len];
            for (int i = 0; i < len; i++) {
                listenerMethodDescriptors[i] = new MethodDescriptor(
                                        old.listenerMethodDescriptors[i]);
            }
        }
        listenerTypeRef = old.listenerTypeRef;

        addMethodDescriptor = old.addMethodDescriptor;
        removeMethodDescriptor = old.removeMethodDescriptor;
        getMethodDescriptor = old.getMethodDescriptor;

        unicast = old.unicast;
        inDefaultEventSet = old.inDefaultEventSet;
    }

    void appendTo(StringBuilder sb) {
        appendTo(sb, "unicast", this.unicast);
        appendTo(sb, "inDefaultEventSet", this.inDefaultEventSet);
        appendTo(sb, "listenerType", this.listenerTypeRef);
        appendTo(sb, "getListenerMethod", getMethod(this.getMethodDescriptor));
        appendTo(sb, "addListenerMethod", getMethod(this.addMethodDescriptor));
        appendTo(sb, "removeListenerMethod", getMethod(this.removeMethodDescriptor));
    }

    private static Method getMethod(MethodDescriptor descriptor) {
        return (descriptor != null)
                ? descriptor.getMethod()
                : null;
    }
}
