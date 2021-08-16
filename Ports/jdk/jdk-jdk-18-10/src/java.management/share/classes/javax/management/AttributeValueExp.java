/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;


import com.sun.jmx.mbeanserver.Introspector;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * <p>Represents attributes used as arguments to relational constraints.
 * Instances of this class are usually obtained using {@link Query#attr(String)
 * Query.attr}.</p>
 *
 * <p>An <CODE>AttributeValueExp</CODE> may be used anywhere a
 * <CODE>ValueExp</CODE> is required.
 *
 * @since 1.5
 */
public class AttributeValueExp implements ValueExp  {


    /* Serial version */
    private static final long serialVersionUID = -7768025046539163385L;

    /**
     * @serial The name of the attribute
     */
    private String attr;

    /**
     * An <code>AttributeValueExp</code> with a null attribute.
     * @deprecated An instance created with this constructor cannot be
     * used in a query.
     */
    @Deprecated
    public AttributeValueExp() {
    }

    /**
     * Creates a new <CODE>AttributeValueExp</CODE> representing the
     * specified object attribute, named attr.
     *
     * @param attr the name of the attribute whose value is the value
     * of this {@link ValueExp}.
     */
    public AttributeValueExp(String attr) {
        this.attr = attr;
    }

    /**
     * Returns a string representation of the name of the attribute.
     *
     * @return the attribute name.
     */
    public String getAttributeName()  {
        return attr;
    }

    /**
     * <p>Applies the <CODE>AttributeValueExp</CODE> on an MBean.
     * This method calls {@link #getAttribute getAttribute(name)} and wraps
     * the result as a {@code ValueExp}.  The value returned by
     * {@code getAttribute} must be a {@code Number}, {@code String},
     * or {@code Boolean}; otherwise this method throws a
     * {@code BadAttributeValueExpException}, which will cause
     * the containing query to be false for this {@code name}.</p>
     *
     * @param name The name of the MBean on which the <CODE>AttributeValueExp</CODE> will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @throws BadStringOperationException {@inheritDoc}
     * @throws BadBinaryOpValueExpException {@inheritDoc}
     * @throws BadAttributeValueExpException {@inheritDoc}
     * @throws InvalidApplicationException  {@inheritDoc}
     */
    @Override
    public ValueExp apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
        BadAttributeValueExpException, InvalidApplicationException {
        Object result = getAttribute(name);

        if (result instanceof Number) {
            return new NumericValueExp((Number)result);
        } else if (result instanceof String) {
            return new StringValueExp((String)result);
        } else if (result instanceof Boolean) {
            return new BooleanValueExp((Boolean)result);
        } else {
            throw new BadAttributeValueExpException(result);
        }
    }

    /**
     * Returns the string representing its value.
     */
    @Override
    public String toString()  {
        return attr;
    }


    /**
     * Sets the MBean server on which the query is to be performed.
     *
     * @param s The MBean server on which the query is to be performed.
     *
     * @deprecated This method has no effect.  The MBean Server used to
     * obtain an attribute value is {@link QueryEval#getMBeanServer()}.
     */
    /* There is no need for this method, because if a query is being
       evaluted an AttributeValueExp can only appear inside a QueryExp,
       and that QueryExp will itself have done setMBeanServer.  */
    @Deprecated
    @Override
    public void setMBeanServer(MBeanServer s)  {
    }


    /**
     * <p>Return the value of the given attribute in the named MBean.
     * If the attempt to access the attribute generates an exception,
     * return null.</p>
     *
     * <p>The MBean Server used is the one returned by {@link
     * QueryEval#getMBeanServer()}.</p>
     *
     * @param name the name of the MBean whose attribute is to be returned.
     *
     * @return the value of the attribute, or null if it could not be
     * obtained.
     */
    protected Object getAttribute(ObjectName name) {
        try {
            // Get the value from the MBeanServer

            MBeanServer server = QueryEval.getMBeanServer();

            return server.getAttribute(name, attr);
        } catch (Exception re) {
            return null;
        }
    }
}
