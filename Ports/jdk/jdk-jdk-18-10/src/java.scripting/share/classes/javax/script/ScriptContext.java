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

package javax.script;
import java.util.List;
import java.io.Writer;
import java.io.Reader;

/**
 * The interface whose implementing classes are used to connect Script Engines
 * with objects, such as scoped Bindings, in hosting applications.  Each scope is a set
 * of named attributes whose values can be set and retrieved using the
 * <code>ScriptContext</code> methods. ScriptContexts also expose Readers and Writers
 * that can be used by the ScriptEngines for input and output.
 *
 * @author Mike Grogan
 * @since 1.6
 */
public interface ScriptContext {


    /**
     * EngineScope attributes are visible during the lifetime of a single
     * <code>ScriptEngine</code> and a set of attributes is maintained for each
     * engine.
     */
    public static final int ENGINE_SCOPE = 100;

    /**
     * GlobalScope attributes are visible to all engines created by same ScriptEngineFactory.
     */
    public static final int GLOBAL_SCOPE = 200;


    /**
     * Associates a <code>Bindings</code> instance with a particular scope in this
     * <code>ScriptContext</code>.  Calls to the <code>getAttribute</code> and
     * <code>setAttribute</code> methods must map to the <code>get</code> and
     * <code>put</code> methods of the <code>Bindings</code> for the specified scope.
     *
     * @param  bindings The <code>Bindings</code> to associate with the given scope
     * @param scope The scope
     *
     * @throws IllegalArgumentException If no <code>Bindings</code> is defined for the
     * specified scope value in ScriptContexts of this type.
     * @throws NullPointerException if value of scope is <code>ENGINE_SCOPE</code> and
     * the specified <code>Bindings</code> is null.
     *
     */
    public void setBindings(Bindings bindings, int scope);

    /**
     * Gets the <code>Bindings</code>  associated with the given scope in this
     * <code>ScriptContext</code>.
     *
     * @return The associated <code>Bindings</code>.  Returns <code>null</code> if it has not
     * been set.
     *
     * @param scope The scope
     * @throws IllegalArgumentException If no <code>Bindings</code> is defined for the
     * specified scope value in <code>ScriptContext</code> of this type.
     */
    public Bindings getBindings(int scope);

    /**
     * Sets the value of an attribute in a given scope. If the scope is <code>GLOBAL_SCOPE</code>
     * and no Bindings is set for <code>GLOBAL_SCOPE</code>, then setAttribute call is a no-op.
     *
     * @param name The name of the attribute to set
     * @param value The value of the attribute
     * @param scope The scope in which to set the attribute
     *
     * @throws IllegalArgumentException
     *         if the name is empty or if the scope is invalid.
     * @throws NullPointerException if the name is null.
     */
    public void setAttribute(String name, Object value, int scope);

    /**
     * Gets the value of an attribute in a given scope.
     *
     * @param name The name of the attribute to retrieve.
     * @param scope The scope in which to retrieve the attribute.
     * @return The value of the attribute. Returns <code>null</code> is the name
     * does not exist in the given scope.
     *
     * @throws IllegalArgumentException
     *         if the name is empty or if the value of scope is invalid.
     * @throws NullPointerException if the name is null.
     */
    public Object getAttribute(String name, int scope);

    /**
     * Remove an attribute in a given scope.
     *
     * @param name The name of the attribute to remove
     * @param scope The scope in which to remove the attribute
     *
     * @return The removed value.
     * @throws IllegalArgumentException
     *         if the name is empty or if the scope is invalid.
     * @throws NullPointerException if the name is null.
     */
    public Object removeAttribute(String name, int scope);

    /**
     * Retrieves the value of the attribute with the given name in
     * the scope occurring earliest in the search order.  The order
     * is determined by the numeric value of the scope parameter (lowest
     * scope values first.)
     *
     * @param name The name of the attribute to retrieve.
     * @return The value of the attribute in the lowest scope for
     * which an attribute with the given name is defined.  Returns
     * null if no attribute with the name exists in any scope.
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is empty.
     */
    public Object getAttribute(String name);


    /**
     * Get the lowest scope in which an attribute is defined.
     * @param name Name of the attribute
     * .
     * @return The lowest scope.  Returns -1 if no attribute with the given
     * name is defined in any scope.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if name is empty.
     */
    public int getAttributesScope(String name);

    /**
     * Returns the <code>Writer</code> for scripts to use when displaying output.
     *
     * @return The <code>Writer</code>.
     */
    public Writer getWriter();


    /**
     * Returns the <code>Writer</code> used to display error output.
     *
     * @return The <code>Writer</code>
     */
    public Writer getErrorWriter();

    /**
     * Sets the <code>Writer</code> for scripts to use when displaying output.
     *
     * @param writer The new <code>Writer</code>.
     */
    public void setWriter(Writer writer);


    /**
     * Sets the <code>Writer</code> used to display error output.
     *
     * @param writer The <code>Writer</code>.
     */
    public void setErrorWriter(Writer writer);

    /**
     * Returns a <code>Reader</code> to be used by the script to read
     * input.
     *
     * @return The <code>Reader</code>.
     */
    public Reader getReader();


    /**
     * Sets the <code>Reader</code> for scripts to read input
     * .
     * @param reader The new <code>Reader</code>.
     */
    public void setReader(Reader reader);

    /**
     * Returns immutable <code>List</code> of all the valid values for
     * scope in the ScriptContext.
     *
     * @return list of scope values
     */
    public List<Integer> getScopes();
}
