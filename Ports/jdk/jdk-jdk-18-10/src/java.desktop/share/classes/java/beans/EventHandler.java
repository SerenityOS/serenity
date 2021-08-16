/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.reflect.misc.MethodUtil;
import sun.reflect.misc.ReflectUtil;

/**
 * The {@code EventHandler} class provides
 * support for dynamically generating event listeners whose methods
 * execute a simple statement involving an incoming event object
 * and a target object.
 * <p>
 * The {@code EventHandler} class is intended to be used by interactive tools, such as
 * application builders, that allow developers to make connections between
 * beans. Typically connections are made from a user interface bean
 * (the event <em>source</em>)
 * to an application logic bean (the <em>target</em>). The most effective
 * connections of this kind isolate the application logic from the user
 * interface.  For example, the {@code EventHandler} for a
 * connection from a {@code JCheckBox} to a method
 * that accepts a boolean value can deal with extracting the state
 * of the check box and passing it directly to the method so that
 * the method is isolated from the user interface layer.
 * <p>
 * Inner classes are another, more general way to handle events from
 * user interfaces.  The {@code EventHandler} class
 * handles only a subset of what is possible using inner
 * classes. However, {@code EventHandler} works better
 * with the long-term persistence scheme than inner classes.
 * Also, using {@code EventHandler} in large applications in
 * which the same interface is implemented many times can
 * reduce the disk and memory footprint of the application.
 * <p>
 * The reason that listeners created with {@code EventHandler}
 * have such a small
 * footprint is that the {@code Proxy} class, on which
 * the {@code EventHandler} relies, shares implementations
 * of identical
 * interfaces. For example, if you use
 * the {@code EventHandler create} methods to make
 * all the {@code ActionListener}s in an application,
 * all the action listeners will be instances of a single class
 * (one created by the {@code Proxy} class).
 * In general, listeners based on
 * the {@code Proxy} class require one listener class
 * to be created per <em>listener type</em> (interface),
 * whereas the inner class
 * approach requires one class to be created per <em>listener</em>
 * (object that implements the interface).
 *
 * <p>
 * You don't generally deal directly with {@code EventHandler}
 * instances.
 * Instead, you use one of the {@code EventHandler}
 * {@code create} methods to create
 * an object that implements a given listener interface.
 * This listener object uses an {@code EventHandler} object
 * behind the scenes to encapsulate information about the
 * event, the object to be sent a message when the event occurs,
 * the message (method) to be sent, and any argument
 * to the method.
 * The following section gives examples of how to create listener
 * objects using the {@code create} methods.
 *
 * <h2>Examples of Using EventHandler</h2>
 *
 * The simplest use of {@code EventHandler} is to install
 * a listener that calls a method on the target object with no arguments.
 * In the following example we create an {@code ActionListener}
 * that invokes the {@code toFront} method on an instance
 * of {@code javax.swing.JFrame}.
 *
 * <blockquote>
 *<pre>
 *myButton.addActionListener(
 *    (ActionListener)EventHandler.create(ActionListener.class, frame, "toFront"));
 *</pre>
 * </blockquote>
 *
 * When {@code myButton} is pressed, the statement
 * {@code frame.toFront()} will be executed.  One could get
 * the same effect, with some additional compile-time type safety,
 * by defining a new implementation of the {@code ActionListener}
 * interface and adding an instance of it to the button:
 *
 * <blockquote>
 *<pre>
//Equivalent code using an inner class instead of EventHandler.
 *myButton.addActionListener(new ActionListener() {
 *    public void actionPerformed(ActionEvent e) {
 *        frame.toFront();
 *    }
 *});
 *</pre>
 * </blockquote>
 *
 * The next simplest use of {@code EventHandler} is
 * to extract a property value from the first argument
 * of the method in the listener interface (typically an event object)
 * and use it to set the value of a property in the target object.
 * In the following example we create an {@code ActionListener} that
 * sets the {@code nextFocusableComponent} property of the target
 * (myButton) object to the value of the "source" property of the event.
 *
 * <blockquote>
 *<pre>
 *EventHandler.create(ActionListener.class, myButton, "nextFocusableComponent", "source")
 *</pre>
 * </blockquote>
 *
 * This would correspond to the following inner class implementation:
 *
 * <blockquote>
 *<pre>
//Equivalent code using an inner class instead of EventHandler.
 *new ActionListener() {
 *    public void actionPerformed(ActionEvent e) {
 *        myButton.setNextFocusableComponent((Component)e.getSource());
 *    }
 *}
 *</pre>
 * </blockquote>
 *
 * It's also possible to create an {@code EventHandler} that
 * just passes the incoming event object to the target's action.
 * If the fourth {@code EventHandler.create} argument is
 * an empty string, then the event is just passed along:
 *
 * <blockquote>
 *<pre>
 *EventHandler.create(ActionListener.class, target, "doActionEvent", "")
 *</pre>
 * </blockquote>
 *
 * This would correspond to the following inner class implementation:
 *
 * <blockquote>
 *<pre>
//Equivalent code using an inner class instead of EventHandler.
 *new ActionListener() {
 *    public void actionPerformed(ActionEvent e) {
 *        target.doActionEvent(e);
 *    }
 *}
 *</pre>
 * </blockquote>
 *
 * Probably the most common use of {@code EventHandler}
 * is to extract a property value from the
 * <em>source</em> of the event object and set this value as
 * the value of a property of the target object.
 * In the following example we create an {@code ActionListener} that
 * sets the "label" property of the target
 * object to the value of the "text" property of the
 * source (the value of the "source" property) of the event.
 *
 * <blockquote>
 *<pre>
 *EventHandler.create(ActionListener.class, myButton, "label", "source.text")
 *</pre>
 * </blockquote>
 *
 * This would correspond to the following inner class implementation:
 *
 * <blockquote>
 *<pre>
//Equivalent code using an inner class instead of EventHandler.
 *new ActionListener {
 *    public void actionPerformed(ActionEvent e) {
 *        myButton.setLabel(((JTextField)e.getSource()).getText());
 *    }
 *}
 *</pre>
 * </blockquote>
 *
 * The event property may be "qualified" with an arbitrary number
 * of property prefixes delimited with the "." character. The "qualifying"
 * names that appear before the "." characters are taken as the names of
 * properties that should be applied, left-most first, to
 * the event object.
 * <p>
 * For example, the following action listener
 *
 * <blockquote>
 *<pre>
 *EventHandler.create(ActionListener.class, target, "a", "b.c.d")
 *</pre>
 * </blockquote>
 *
 * might be written as the following inner class
 * (assuming all the properties had canonical getter methods and
 * returned the appropriate types):
 *
 * <blockquote>
 *<pre>
//Equivalent code using an inner class instead of EventHandler.
 *new ActionListener {
 *    public void actionPerformed(ActionEvent e) {
 *        target.setA(e.getB().getC().isD());
 *    }
 *}
 *</pre>
 * </blockquote>
 * The target property may also be "qualified" with an arbitrary number
 * of property prefixs delimited with the "." character.  For example, the
 * following action listener:
 * <pre>
 *   EventHandler.create(ActionListener.class, target, "a.b", "c.d")
 * </pre>
 * might be written as the following inner class
 * (assuming all the properties had canonical getter methods and
 * returned the appropriate types):
 * <pre>
 *   //Equivalent code using an inner class instead of EventHandler.
 *   new ActionListener {
 *     public void actionPerformed(ActionEvent e) {
 *         target.getA().setB(e.getC().isD());
 *    }
 *}
 *</pre>
 * <p>
 * As {@code EventHandler} ultimately relies on reflection to invoke
 * a method we recommend against targeting an overloaded method.  For example,
 * if the target is an instance of the class {@code MyTarget} which is
 * defined as:
 * <pre>
 *   public class MyTarget {
 *     public void doIt(String);
 *     public void doIt(Object);
 *   }
 * </pre>
 * Then the method {@code doIt} is overloaded.  EventHandler will invoke
 * the method that is appropriate based on the source.  If the source is
 * null, then either method is appropriate and the one that is invoked is
 * undefined.  For that reason we recommend against targeting overloaded
 * methods.
 *
 * @see java.lang.reflect.Proxy
 * @see java.util.EventObject
 *
 * @since 1.4
 *
 * @author Mark Davidson
 * @author Philip Milne
 * @author Hans Muller
 *
 */
public class EventHandler implements InvocationHandler {
    private Object target;
    private String action;
    private final String eventPropertyName;
    private final String listenerMethodName;
    @SuppressWarnings("removal")
    private final AccessControlContext acc = AccessController.getContext();

    /**
     * Creates a new {@code EventHandler} object;
     * you generally use one of the {@code create} methods
     * instead of invoking this constructor directly.  Refer to
     * {@link java.beans.EventHandler#create(Class, Object, String, String)
     * the general version of create} for a complete description of
     * the {@code eventPropertyName} and {@code listenerMethodName}
     * parameter.
     *
     * @param target the object that will perform the action
     * @param action the name of a (possibly qualified) property or method on
     *        the target
     * @param eventPropertyName the (possibly qualified) name of a readable property of the incoming event
     * @param listenerMethodName the name of the method in the listener interface that should trigger the action
     *
     * @throws NullPointerException if {@code target} is null
     * @throws NullPointerException if {@code action} is null
     *
     * @see EventHandler
     * @see #create(Class, Object, String, String, String)
     * @see #getTarget
     * @see #getAction
     * @see #getEventPropertyName
     * @see #getListenerMethodName
     */
    @ConstructorProperties({"target", "action", "eventPropertyName", "listenerMethodName"})
    public EventHandler(Object target, String action, String eventPropertyName, String listenerMethodName) {
        this.target = target;
        this.action = action;
        if (target == null) {
            throw new NullPointerException("target must be non-null");
        }
        if (action == null) {
            throw new NullPointerException("action must be non-null");
        }
        this.eventPropertyName = eventPropertyName;
        this.listenerMethodName = listenerMethodName;
    }

    /**
     * Returns the object to which this event handler will send a message.
     *
     * @return the target of this event handler
     * @see #EventHandler(Object, String, String, String)
     */
    public Object getTarget()  {
        return target;
    }

    /**
     * Returns the name of the target's writable property
     * that this event handler will set,
     * or the name of the method that this event handler
     * will invoke on the target.
     *
     * @return the action of this event handler
     * @see #EventHandler(Object, String, String, String)
     */
    public String getAction()  {
        return action;
    }

    /**
     * Returns the property of the event that should be
     * used in the action applied to the target.
     *
     * @return the property of the event
     *
     * @see #EventHandler(Object, String, String, String)
     */
    public String getEventPropertyName()  {
        return eventPropertyName;
    }

    /**
     * Returns the name of the method that will trigger the action.
     * A return value of {@code null} signifies that all methods in the
     * listener interface trigger the action.
     *
     * @return the name of the method that will trigger the action
     *
     * @see #EventHandler(Object, String, String, String)
     */
    public String getListenerMethodName()  {
        return listenerMethodName;
    }

    private Object applyGetters(Object target, String getters) {
        if (getters == null || getters.isEmpty()) {
            return target;
        }
        int firstDot = getters.indexOf('.');
        if (firstDot == -1) {
            firstDot = getters.length();
        }
        String first = getters.substring(0, firstDot);
        String rest = getters.substring(Math.min(firstDot + 1, getters.length()));

        try {
            Method getter = null;
            if (target != null) {
                getter = Statement.getMethod(target.getClass(),
                                      "get" + NameGenerator.capitalize(first),
                                      new Class<?>[]{});
                if (getter == null) {
                    getter = Statement.getMethod(target.getClass(),
                                   "is" + NameGenerator.capitalize(first),
                                   new Class<?>[]{});
                }
                if (getter == null) {
                    getter = Statement.getMethod(target.getClass(), first, new Class<?>[]{});
                }
            }
            if (getter == null) {
                throw new RuntimeException("No method called: " + first +
                                           " defined on " + target);
            }
            Object newTarget = MethodUtil.invoke(getter, target, new Object[]{});
            return applyGetters(newTarget, rest);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to call method: " + first +
                                       " on " + target, e);
        }
    }

    /**
     * Extract the appropriate property value from the event and
     * pass it to the action associated with
     * this {@code EventHandler}.
     *
     * @param proxy the proxy object
     * @param method the method in the listener interface
     * @return the result of applying the action to the target
     *
     * @see EventHandler
     */
    @SuppressWarnings("removal")
    public Object invoke(final Object proxy, final Method method, final Object[] arguments) {
        AccessControlContext acc = this.acc;
        if ((acc == null) && (System.getSecurityManager() != null)) {
            throw new SecurityException("AccessControlContext is not set");
        }
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                return invokeInternal(proxy, method, arguments);
            }
        }, acc);
    }

    private Object invokeInternal(Object proxy, Method method, Object[] arguments) {
        String methodName = method.getName();
        if (method.getDeclaringClass() == Object.class)  {
            // Handle the Object public methods.
            if (methodName.equals("hashCode"))  {
                return System.identityHashCode(proxy);
            } else if (methodName.equals("equals")) {
                return (proxy == arguments[0] ? Boolean.TRUE : Boolean.FALSE);
            } else if (methodName.equals("toString")) {
                return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
            }
        }

        if (listenerMethodName == null || listenerMethodName.equals(methodName)) {
            Class<?>[] argTypes = null;
            Object[] newArgs = null;

            if (eventPropertyName == null) {     // Nullary method.
                newArgs = new Object[]{};
                argTypes = new Class<?>[]{};
            }
            else {
                Object input = applyGetters(arguments[0], getEventPropertyName());
                newArgs = new Object[]{input};
                argTypes = new Class<?>[]{input == null ? null :
                                       input.getClass()};
            }
            try {
                int lastDot = action.lastIndexOf('.');
                if (lastDot != -1) {
                    target = applyGetters(target, action.substring(0, lastDot));
                    action = action.substring(lastDot + 1);
                }
                Method targetMethod = Statement.getMethod(
                             target.getClass(), action, argTypes);
                if (targetMethod == null) {
                    targetMethod = Statement.getMethod(target.getClass(),
                             "set" + NameGenerator.capitalize(action), argTypes);
                }
                if (targetMethod == null) {
                    String argTypeString = (argTypes.length == 0)
                        ? " with no arguments"
                        : " with argument " + argTypes[0];
                    throw new RuntimeException(
                        "No method called " + action + " on " +
                        target.getClass() + argTypeString);
                }
                return MethodUtil.invoke(targetMethod, target, newArgs);
            }
            catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                throw (th instanceof RuntimeException)
                        ? (RuntimeException) th
                        : new RuntimeException(th);
            }
        }
        return null;
    }

    /**
     * Creates an implementation of {@code listenerInterface} in which
     * <em>all</em> of the methods in the listener interface apply
     * the handler's {@code action} to the {@code target}. This
     * method is implemented by calling the other, more general,
     * implementation of the {@code create} method with both
     * the {@code eventPropertyName} and the {@code listenerMethodName}
     * taking the value {@code null}. Refer to
     * {@link java.beans.EventHandler#create(Class, Object, String, String)
     * the general version of create} for a complete description of
     * the {@code action} parameter.
     * <p>
     * To create an {@code ActionListener} that shows a
     * {@code JDialog} with {@code dialog.show()},
     * one can write:
     *
     *<blockquote>
     *<pre>
     *EventHandler.create(ActionListener.class, dialog, "show")
     *</pre>
     *</blockquote>
     *
     * @param <T> the type to create
     * @param listenerInterface the listener interface to create a proxy for
     * @param target the object that will perform the action
     * @param action the name of a (possibly qualified) property or method on
     *        the target
     * @return an object that implements {@code listenerInterface}
     *
     * @throws NullPointerException if {@code listenerInterface} is null
     * @throws NullPointerException if {@code target} is null
     * @throws NullPointerException if {@code action} is null
     * @throws IllegalArgumentException if creating a Proxy for
     *         {@code listenerInterface} fails for any of the restrictions
     *         specified by {@link Proxy#newProxyInstance}
     * @see #create(Class, Object, String, String)
     * @see Proxy#newProxyInstance
     */
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action)
    {
        return create(listenerInterface, target, action, null, null);
    }

    /**
    /**
     * Creates an implementation of {@code listenerInterface} in which
     * <em>all</em> of the methods pass the value of the event
     * expression, {@code eventPropertyName}, to the final method in the
     * statement, {@code action}, which is applied to the {@code target}.
     * This method is implemented by calling the
     * more general, implementation of the {@code create} method with
     * the {@code listenerMethodName} taking the value {@code null}.
     * Refer to
     * {@link java.beans.EventHandler#create(Class, Object, String, String)
     * the general version of create} for a complete description of
     * the {@code action} and {@code eventPropertyName} parameters.
     * <p>
     * To create an {@code ActionListener} that sets the
     * the text of a {@code JLabel} to the text value of
     * the {@code JTextField} source of the incoming event,
     * you can use the following code:
     *
     *<blockquote>
     *<pre>
     *EventHandler.create(ActionListener.class, label, "text", "source.text");
     *</pre>
     *</blockquote>
     *
     * This is equivalent to the following code:
     *<blockquote>
     *<pre>
//Equivalent code using an inner class instead of EventHandler.
     *new ActionListener() {
     *    public void actionPerformed(ActionEvent event) {
     *        label.setText(((JTextField)(event.getSource())).getText());
     *     }
     *};
     *</pre>
     *</blockquote>
     *
     * @param <T> the type to create
     * @param listenerInterface the listener interface to create a proxy for
     * @param target the object that will perform the action
     * @param action the name of a (possibly qualified) property or method on
     *        the target
     * @param eventPropertyName the (possibly qualified) name of a readable property of the incoming event
     *
     * @return an object that implements {@code listenerInterface}
     *
     * @throws NullPointerException if {@code listenerInterface} is null
     * @throws NullPointerException if {@code target} is null
     * @throws NullPointerException if {@code action} is null
     * @throws IllegalArgumentException if creating a Proxy for
     *         {@code listenerInterface} fails for any of the restrictions
     *         specified by {@link Proxy#newProxyInstance}
     * @see #create(Class, Object, String, String, String)
     * @see Proxy#newProxyInstance
     */
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action,
                               String eventPropertyName)
    {
        return create(listenerInterface, target, action, eventPropertyName, null);
    }

    /**
     * Creates an implementation of {@code listenerInterface} in which
     * the method named {@code listenerMethodName}
     * passes the value of the event expression, {@code eventPropertyName},
     * to the final method in the statement, {@code action}, which
     * is applied to the {@code target}. All of the other listener
     * methods do nothing.
     * <p>
     * The {@code eventPropertyName} string is used to extract a value
     * from the incoming event object that is passed to the target
     * method.  The common case is the target method takes no arguments, in
     * which case a value of null should be used for the
     * {@code eventPropertyName}.  Alternatively if you want
     * the incoming event object passed directly to the target method use
     * the empty string.
     * The format of the {@code eventPropertyName} string is a sequence of
     * methods or properties where each method or
     * property is applied to the value returned by the preceding method
     * starting from the incoming event object.
     * The syntax is: {@code propertyName{.propertyName}*}
     * where {@code propertyName} matches a method or
     * property.  For example, to extract the {@code point}
     * property from a {@code MouseEvent}, you could use either
     * {@code "point"} or {@code "getPoint"} as the
     * {@code eventPropertyName}.  To extract the "text" property from
     * a {@code MouseEvent} with a {@code JLabel} source use any
     * of the following as {@code eventPropertyName}:
     * {@code "source.text"},
     * {@code "getSource.text" "getSource.getText"} or
     * {@code "source.getText"}.  If a method can not be found, or an
     * exception is generated as part of invoking a method a
     * {@code RuntimeException} will be thrown at dispatch time.  For
     * example, if the incoming event object is null, and
     * {@code eventPropertyName} is non-null and not empty, a
     * {@code RuntimeException} will be thrown.
     * <p>
     * The {@code action} argument is of the same format as the
     * {@code eventPropertyName} argument where the last property name
     * identifies either a method name or writable property.
     * <p>
     * If the {@code listenerMethodName} is {@code null}
     * <em>all</em> methods in the interface trigger the {@code action} to be
     * executed on the {@code target}.
     * <p>
     * For example, to create a {@code MouseListener} that sets the target
     * object's {@code origin} property to the incoming {@code MouseEvent}'s
     * location (that's the value of {@code mouseEvent.getPoint()}) each
     * time a mouse button is pressed, one would write:
     *<blockquote>
     *<pre>
     *EventHandler.create(MouseListener.class, target, "origin", "point", "mousePressed");
     *</pre>
     *</blockquote>
     *
     * This is comparable to writing a {@code MouseListener} in which all
     * of the methods except {@code mousePressed} are no-ops:
     *
     *<blockquote>
     *<pre>
//Equivalent code using an inner class instead of EventHandler.
     *new MouseAdapter() {
     *    public void mousePressed(MouseEvent e) {
     *        target.setOrigin(e.getPoint());
     *    }
     *};
     * </pre>
     *</blockquote>
     *
     * @param <T> the type to create
     * @param listenerInterface the listener interface to create a proxy for
     * @param target the object that will perform the action
     * @param action the name of a (possibly qualified) property or method on
     *        the target
     * @param eventPropertyName the (possibly qualified) name of a readable property of the incoming event
     * @param listenerMethodName the name of the method in the listener interface that should trigger the action
     *
     * @return an object that implements {@code listenerInterface}
     *
     * @throws NullPointerException if {@code listenerInterface} is null
     * @throws NullPointerException if {@code target} is null
     * @throws NullPointerException if {@code action} is null
     * @throws IllegalArgumentException if creating a Proxy for
     *         {@code listenerInterface} fails for any of the restrictions
     *         specified by {@link Proxy#newProxyInstance}
     * @see EventHandler
     * @see Proxy#newProxyInstance
     */
    @SuppressWarnings("removal")
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action,
                               String eventPropertyName,
                               String listenerMethodName)
    {
        // Create this first to verify target/action are non-null
        final EventHandler handler = new EventHandler(target, action,
                                                     eventPropertyName,
                                                     listenerMethodName);
        if (listenerInterface == null) {
            throw new NullPointerException(
                          "listenerInterface must be non-null");
        }
        final ClassLoader loader = getClassLoader(listenerInterface);
        final Class<?>[] interfaces = {listenerInterface};
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @SuppressWarnings("unchecked")
            public T run() {
                return (T) Proxy.newProxyInstance(loader, interfaces, handler);
            }
        });
    }

    private static ClassLoader getClassLoader(Class<?> type) {
        ReflectUtil.checkPackageAccess(type);
        ClassLoader loader = type.getClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader(); // avoid use of BCP
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
        }
        return loader;
    }
}
