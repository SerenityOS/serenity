/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.lang.reflect.*;
import java.util.Objects;
import sun.reflect.misc.*;


/**
 * The {@code DefaultPersistenceDelegate} is a concrete implementation of
 * the abstract {@code PersistenceDelegate} class and
 * is the delegate used by default for classes about
 * which no information is available. The {@code DefaultPersistenceDelegate}
 * provides, version resilient, public API-based persistence for
 * classes that follow the JavaBeans conventions without any class specific
 * configuration.
 * <p>
 * The key assumptions are that the class has a nullary constructor
 * and that its state is accurately represented by matching pairs
 * of "setter" and "getter" methods in the order they are returned
 * by the Introspector.
 * In addition to providing code-free persistence for JavaBeans,
 * the {@code DefaultPersistenceDelegate} provides a convenient means
 * to effect persistent storage for classes that have a constructor
 * that, while not nullary, simply requires some property values
 * as arguments.
 *
 * @see #DefaultPersistenceDelegate(String[])
 * @see java.beans.Introspector
 *
 * @since 1.4
 *
 * @author Philip Milne
 */

public class DefaultPersistenceDelegate extends PersistenceDelegate {
    private static final String[] EMPTY = {};
    private final String[] constructor;
    private Boolean definesEquals;

    /**
     * Creates a persistence delegate for a class with a nullary constructor.
     *
     * @see #DefaultPersistenceDelegate(java.lang.String[])
     */
    public DefaultPersistenceDelegate() {
        this.constructor = EMPTY;
    }

    /**
     * Creates a default persistence delegate for a class with a
     * constructor whose arguments are the values of the property
     * names as specified by {@code constructorPropertyNames}.
     * The constructor arguments are created by
     * evaluating the property names in the order they are supplied.
     * To use this class to specify a single preferred constructor for use
     * in the serialization of a particular type, we state the
     * names of the properties that make up the constructor's
     * arguments. For example, the {@code Font} class which
     * does not define a nullary constructor can be handled
     * with the following persistence delegate:
     *
     * <pre>
     *     new DefaultPersistenceDelegate(new String[]{"name", "style", "size"});
     * </pre>
     *
     * @param  constructorPropertyNames The property names for the arguments of this constructor.
     *
     * @see #instantiate
     */
    public DefaultPersistenceDelegate(String[] constructorPropertyNames) {
        this.constructor = (constructorPropertyNames == null) ? EMPTY : constructorPropertyNames.clone();
    }

    private static boolean definesEquals(Class<?> type) {
        try {
            return type == type.getMethod("equals", Object.class).getDeclaringClass();
        }
        catch(NoSuchMethodException e) {
            return false;
        }
    }

    private boolean definesEquals(Object instance) {
        if (definesEquals != null) {
            return (definesEquals == Boolean.TRUE);
        }
        else {
            boolean result = definesEquals(instance.getClass());
            definesEquals = result ? Boolean.TRUE : Boolean.FALSE;
            return result;
        }
    }

    /**
     * If the number of arguments in the specified constructor is non-zero and
     * the class of {@code oldInstance} explicitly declares an "equals" method
     * this method returns the value of {@code oldInstance.equals(newInstance)}.
     * Otherwise, this method uses the superclass's definition which returns true if the
     * classes of the two instances are equal.
     *
     * @param oldInstance The instance to be copied.
     * @param newInstance The instance that is to be modified.
     * @return True if an equivalent copy of {@code newInstance} may be
     *         created by applying a series of mutations to {@code oldInstance}.
     *
     * @see #DefaultPersistenceDelegate(String[])
     */
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        // Assume the instance is either mutable or a singleton
        // if it has a nullary constructor.
        return (constructor.length == 0) || !definesEquals(oldInstance) ?
            super.mutatesTo(oldInstance, newInstance) :
            oldInstance.equals(newInstance);
    }

    /**
     * This default implementation of the {@code instantiate} method returns
     * an expression containing the predefined method name "new" which denotes a
     * call to a constructor with the arguments as specified in
     * the {@code DefaultPersistenceDelegate}'s constructor.
     *
     * @param  oldInstance The instance to be instantiated.
     * @param  out The code output stream.
     * @return An expression whose value is {@code oldInstance}.
     *
     * @throws NullPointerException if {@code out} is {@code null}
     *                              and this value is used in the method
     *
     * @see #DefaultPersistenceDelegate(String[])
     */
    protected Expression instantiate(Object oldInstance, Encoder out) {
        int nArgs = constructor.length;
        Class<?> type = oldInstance.getClass();
        Object[] constructorArgs = new Object[nArgs];
        for(int i = 0; i < nArgs; i++) {
            try {
                Method method = findMethod(type, this.constructor[i]);
                constructorArgs[i] = MethodUtil.invoke(method, oldInstance, new Object[0]);
            }
            catch (Exception e) {
                out.getExceptionListener().exceptionThrown(e);
            }
        }
        return new Expression(oldInstance, oldInstance.getClass(), "new", constructorArgs);
    }

    private Method findMethod(Class<?> type, String property) {
        if (property == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        PropertyDescriptor pd = getPropertyDescriptor(type, property);
        if (pd == null) {
            throw new IllegalStateException("Could not find property by the name " + property);
        }
        Method method = pd.getReadMethod();
        if (method == null) {
            throw new IllegalStateException("Could not find getter for the property " + property);
        }
        return method;
    }

    private void doProperty(Class<?> type, PropertyDescriptor pd, Object oldInstance, Object newInstance, Encoder out) throws Exception {
        Method getter = pd.getReadMethod();
        Method setter = pd.getWriteMethod();

        if (getter != null && setter != null) {
            Expression oldGetExp = new Expression(oldInstance, getter.getName(), new Object[]{});
            Expression newGetExp = new Expression(newInstance, getter.getName(), new Object[]{});
            Object oldValue = oldGetExp.getValue();
            Object newValue = newGetExp.getValue();
            out.writeExpression(oldGetExp);
            if (!Objects.equals(newValue, out.get(oldValue))) {
                // Search for a static constant with this value;
                Object e = (Object[])pd.getValue("enumerationValues");
                if (e instanceof Object[] && Array.getLength(e) % 3 == 0) {
                    Object[] a = (Object[])e;
                    for(int i = 0; i < a.length; i = i + 3) {
                        try {
                           Field f = type.getField((String)a[i]);
                           if (f.get(null).equals(oldValue)) {
                               out.remove(oldValue);
                               out.writeExpression(new Expression(oldValue, f, "get", new Object[]{null}));
                           }
                        }
                        catch (Exception ex) {}
                    }
                }
                invokeStatement(oldInstance, setter.getName(), new Object[]{oldValue}, out);
            }
        }
    }

    static void invokeStatement(Object instance, String methodName, Object[] args, Encoder out) {
        out.writeStatement(new Statement(instance, methodName, args));
    }

    // Write out the properties of this instance.
    private void initBean(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        for (Field field : type.getFields()) {
            if (!ReflectUtil.isPackageAccessible(field.getDeclaringClass())) {
                continue;
            }
            int mod = field.getModifiers();
            if (Modifier.isFinal(mod) || Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            try {
                Expression oldGetExp = new Expression(field, "get", new Object[] { oldInstance });
                Expression newGetExp = new Expression(field, "get", new Object[] { newInstance });
                Object oldValue = oldGetExp.getValue();
                Object newValue = newGetExp.getValue();
                out.writeExpression(oldGetExp);
                if (!Objects.equals(newValue, out.get(oldValue))) {
                    out.writeStatement(new Statement(field, "set", new Object[] { oldInstance, oldValue }));
                }
            }
            catch (Exception exception) {
                out.getExceptionListener().exceptionThrown(exception);
            }
        }
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type);
        } catch (IntrospectionException exception) {
            return;
        }
        // Properties
        for (PropertyDescriptor d : info.getPropertyDescriptors()) {
            if (d.isTransient()) {
                continue;
            }
            try {
                doProperty(type, d, oldInstance, newInstance, out);
            }
            catch (Exception e) {
                out.getExceptionListener().exceptionThrown(e);
            }
        }

        // Listeners
        /*
        Pending(milne). There is a general problem with the archival of
        listeners which is unresolved as of 1.4. Many of the methods
        which install one object inside another (typically "add" methods
        or setters) automatically install a listener on the "child" object
        so that its "parent" may respond to changes that are made to it.
        For example the JTable:setModel() method automatically adds a
        TableModelListener (the JTable itself in this case) to the supplied
        table model.

        We do not need to explicitly add these listeners to the model in an
        archive as they will be added automatically by, in the above case,
        the JTable's "setModel" method. In some cases, we must specifically
        avoid trying to do this since the listener may be an inner class
        that cannot be instantiated using public API.

        No general mechanism currently
        exists for differentiating between these kind of listeners and
        those which were added explicitly by the user. A mechanism must
        be created to provide a general means to differentiate these
        special cases so as to provide reliable persistence of listeners
        for the general case.
        */
        if (!java.awt.Component.class.isAssignableFrom(type)) {
            return; // Just handle the listeners of Components for now.
        }
        for (EventSetDescriptor d : info.getEventSetDescriptors()) {
            if (d.isTransient()) {
                continue;
            }
            Class<?> listenerType = d.getListenerType();


            // The ComponentListener is added automatically, when
            // Contatiner:add is called on the parent.
            if (listenerType == java.awt.event.ComponentListener.class) {
                continue;
            }

            // JMenuItems have a change listener added to them in
            // their "add" methods to enable accessibility support -
            // see the add method in JMenuItem for details. We cannot
            // instantiate this instance as it is a private inner class
            // and do not need to do this anyway since it will be created
            // and installed by the "add" method. Special case this for now,
            // ignoring all change listeners on JMenuItems.
            if (listenerType == javax.swing.event.ChangeListener.class &&
                type == javax.swing.JMenuItem.class) {
                continue;
            }

            EventListener[] oldL = new EventListener[0];
            EventListener[] newL = new EventListener[0];
            try {
                Method m = d.getGetListenerMethod();
                oldL = (EventListener[])MethodUtil.invoke(m, oldInstance, new Object[]{});
                newL = (EventListener[])MethodUtil.invoke(m, newInstance, new Object[]{});
            }
            catch (Exception e2) {
                try {
                    Method m = type.getMethod("getListeners", new Class<?>[]{Class.class});
                    oldL = (EventListener[])MethodUtil.invoke(m, oldInstance, new Object[]{listenerType});
                    newL = (EventListener[])MethodUtil.invoke(m, newInstance, new Object[]{listenerType});
                }
                catch (Exception e3) {
                    return;
                }
            }

            // Asssume the listeners are in the same order and that there are no gaps.
            // Eventually, this may need to do true differencing.
            String addListenerMethodName = d.getAddListenerMethod().getName();
            for (int i = newL.length; i < oldL.length; i++) {
                // System.out.println("Adding listener: " + addListenerMethodName + oldL[i]);
                invokeStatement(oldInstance, addListenerMethodName, new Object[]{oldL[i]}, out);
            }

            String removeListenerMethodName = d.getRemoveListenerMethod().getName();
            for (int i = oldL.length; i < newL.length; i++) {
                invokeStatement(oldInstance, removeListenerMethodName, new Object[]{newL[i]}, out);
            }
        }
    }

    /**
     * This default implementation of the {@code initialize} method assumes
     * all state held in objects of this type is exposed via the
     * matching pairs of "setter" and "getter" methods in the order
     * they are returned by the Introspector. If a property descriptor
     * defines a "transient" attribute with a value equal to
     * {@code Boolean.TRUE} the property is ignored by this
     * default implementation. Note that this use of the word
     * "transient" is quite independent of the field modifier
     * that is used by the {@code ObjectOutputStream}.
     * <p>
     * For each non-transient property, an expression is created
     * in which the nullary "getter" method is applied
     * to the {@code oldInstance}. The value of this
     * expression is the value of the property in the instance that is
     * being serialized. If the value of this expression
     * in the cloned environment {@code mutatesTo} the
     * target value, the new value is initialized to make it
     * equivalent to the old value. In this case, because
     * the property value has not changed there is no need to
     * call the corresponding "setter" method and no statement
     * is emitted. If not however, the expression for this value
     * is replaced with another expression (normally a constructor)
     * and the corresponding "setter" method is called to install
     * the new property value in the object. This scheme removes
     * default information from the output produced by streams
     * using this delegate.
     * <p>
     * In passing these statements to the output stream, where they
     * will be executed, side effects are made to the {@code newInstance}.
     * In most cases this allows the problem of properties
     * whose values depend on each other to actually help the
     * serialization process by making the number of statements
     * that need to be written to the output smaller. In general,
     * the problem of handling interdependent properties is reduced to
     * that of finding an order for the properties in
     * a class such that no property value depends on the value of
     * a subsequent property.
     *
     * @param type the type of the instances
     * @param oldInstance The instance to be copied.
     * @param newInstance The instance that is to be modified.
     * @param out The stream to which any initialization statements should be written.
     *
     * @throws NullPointerException if {@code out} is {@code null}
     *
     * @see java.beans.Introspector#getBeanInfo
     * @see java.beans.PropertyDescriptor
     */
    protected void initialize(Class<?> type,
                              Object oldInstance, Object newInstance,
                              Encoder out)
    {
        // System.out.println("DefulatPD:initialize" + type);
        super.initialize(type, oldInstance, newInstance, out);
        if (oldInstance.getClass() == type) { // !type.isInterface()) {
            initBean(type, oldInstance, newInstance, out);
        }
    }

    private static PropertyDescriptor getPropertyDescriptor(Class<?> type, String property) {
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                if (property.equals(pd.getName()))
                    return pd;
            }
        } catch (IntrospectionException exception) {
        }
        return null;
    }
}
