/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.openmbean;

import com.sun.jmx.mbeanserver.MXBeanLookup;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
   <p>An {@link InvocationHandler} that forwards getter methods to a
   {@link CompositeData}.  If you have an interface that contains
   only getter methods (such as {@code String getName()} or
   {@code boolean isActive()}) then you can use this class in
   conjunction with the {@link Proxy} class to produce an implementation
   of the interface where each getter returns the value of the
   corresponding item in a {@code CompositeData}.</p>

   <p>For example, suppose you have an interface like this:

   <blockquote>
   <pre>
   public interface NamedNumber {
       public int getNumber();
       public String getName();
   }
   </pre>
   </blockquote>

   and a {@code CompositeData} constructed like this:

   <blockquote>
   <pre>
   CompositeData cd =
       new {@link CompositeDataSupport}(
           someCompositeType,
           new String[] {"number", "name"},
           new Object[] {<b>5</b>, "five"}
       );
   </pre>
   </blockquote>

   then you can construct an object implementing {@code NamedNumber}
   and backed by the object {@code cd} like this:

   <blockquote>
   <pre>
   InvocationHandler handler =
       new CompositeDataInvocationHandler(cd);
   NamedNumber nn = (NamedNumber)
       Proxy.newProxyInstance(NamedNumber.class.getClassLoader(),
                              new Class[] {NamedNumber.class},
                              handler);
   </pre>
   </blockquote>

   A call to {@code nn.getNumber()} will then return <b>5</b>.

   <p>If the first letter of the property defined by a getter is a
   capital, then this handler will look first for an item in the
   {@code CompositeData} beginning with a capital, then, if that is
   not found, for an item beginning with the corresponding lowercase
   letter or code point.  For a getter called {@code getNumber()}, the
   handler will first look for an item called {@code Number}, then for
   {@code number}.  If the getter is called {@code getnumber()}, then
   the item must be called {@code number}.</p>

   <p>If the method given to {@link #invoke invoke} is the method
   {@code boolean equals(Object)} inherited from {@code Object}, then
   it will return true if and only if the argument is a {@code Proxy}
   whose {@code InvocationHandler} is also a {@code
   CompositeDataInvocationHandler} and whose backing {@code
   CompositeData} is equal (not necessarily identical) to this
   object's.  If the method given to {@code invoke} is the method
   {@code int hashCode()} inherited from {@code Object}, then it will
   return a value that is consistent with this definition of {@code
   equals}: if two objects are equal according to {@code equals}, then
   they will have the same {@code hashCode}.</p>

   @since 1.6
*/
public class CompositeDataInvocationHandler implements InvocationHandler {
    /**
       <p>Construct a handler backed by the given {@code
       CompositeData}.</p>

       @param compositeData the {@code CompositeData} that will supply
       information to getters.

       @throws IllegalArgumentException if {@code compositeData}
       is null.
    */
    public CompositeDataInvocationHandler(CompositeData compositeData) {
        this(compositeData, null);
    }

    /**
       <p>Construct a handler backed by the given {@code
       CompositeData}.</p>

       @param compositeData the {@code CompositeData} that will supply
       information to getters.

       @throws IllegalArgumentException if {@code compositeData}
       is null.
    */
    CompositeDataInvocationHandler(CompositeData compositeData,
                                   MXBeanLookup lookup) {
        if (compositeData == null)
            throw new IllegalArgumentException("compositeData");
        this.compositeData = compositeData;
        this.lookup = lookup;
    }

    /**
       Return the {@code CompositeData} that was supplied to the
       constructor.
       @return the {@code CompositeData} that this handler is backed
       by.  This is never null.
    */
    public CompositeData getCompositeData() {
        assert compositeData != null;
        return compositeData;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        final String methodName = method.getName();

        // Handle the methods from java.lang.Object
        if (method.getDeclaringClass() == Object.class) {
            if (methodName.equals("toString") && args == null)
                return "Proxy[" + compositeData + "]";
            else if (methodName.equals("hashCode") && args == null)
                return compositeData.hashCode() + 0x43444948;
            else if (methodName.equals("equals") && args.length == 1
                && method.getParameterTypes()[0] == Object.class)
                return equals(proxy, args[0]);
            else {
                /* Either someone is calling invoke by hand, or
                   it is a non-final method from Object overriden
                   by the generated Proxy.  At the time of writing,
                   the only non-final methods in Object that are not
                   handled above are finalize and clone, and these
                   are not overridden in generated proxies.  */
                // this plain Method.invoke is called only if the declaring class
                // is Object and so it's safe.
                return method.invoke(this, args);
            }
        }

        String propertyName = DefaultMXBeanMappingFactory.propertyName(method);
        if (propertyName == null) {
            throw new IllegalArgumentException("Method is not getter: " +
                                               method.getName());
        }
        Object openValue;
        if (compositeData.containsKey(propertyName))
            openValue = compositeData.get(propertyName);
        else {
            String decap = DefaultMXBeanMappingFactory.decapitalize(propertyName);
            if (compositeData.containsKey(decap))
                openValue = compositeData.get(decap);
            else {
                final String msg =
                    "No CompositeData item " + propertyName +
                    (decap.equals(propertyName) ? "" : " or " + decap) +
                    " to match " + methodName;
                throw new IllegalArgumentException(msg);
            }
        }
        MXBeanMapping mapping =
            MXBeanMappingFactory.DEFAULT.mappingForType(method.getGenericReturnType(),
                                   MXBeanMappingFactory.DEFAULT);
        return mapping.fromOpenValue(openValue);
    }

    /* This method is called when equals(Object) is
     * called on our proxy and hence forwarded to us.  For example, if we
     * are a proxy for an interface like this:
     * public interface GetString {
     *     public String string();
     * }
     * then we must compare equal to another CompositeDataInvocationHandler
     * proxy for the same interface and where string() returns the same value.
     *
     * You might think that we should also compare equal to another
     * object that implements GetString directly rather than using
     * Proxy, provided that its string() returns the same result as
     * ours, and in fact an earlier version of this class did that (by
     * converting the other object into a CompositeData and comparing
     * that with ours).  But in fact that doesn't make a great deal of
     * sense because there's absolutely no guarantee that the
     * resulting equals would be reflexive (otherObject.equals(this)
     * might be false even if this.equals(otherObject) is true), and,
     * especially, there's no way we could generate a hashCode() that
     * would be equal to otherObject.hashCode() when
     * this.equals(otherObject), because we don't know how
     * otherObject.hashCode() is computed.
     */
    private boolean equals(Object proxy, Object other) {
        if (other == null)
            return false;

        final Class<?> proxyClass = proxy.getClass();
        final Class<?> otherClass = other.getClass();
        if (proxyClass != otherClass)
            return false;
        InvocationHandler otherih = Proxy.getInvocationHandler(other);
        if (!(otherih instanceof CompositeDataInvocationHandler))
            return false;
        CompositeDataInvocationHandler othercdih =
            (CompositeDataInvocationHandler) otherih;
        return compositeData.equals(othercdih.compositeData);
    }

    private final CompositeData compositeData;
    private final MXBeanLookup lookup;
}
