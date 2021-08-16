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

import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
 * <code>ScriptEngine</code> is the fundamental interface whose methods must be
 * fully functional in every implementation of this specification.
 * <br><br>
 * These methods provide basic scripting functionality.  Applications written to this
 * simple interface are expected to work with minimal modifications in every implementation.
 * It includes methods that execute scripts, and ones that set and get values.
 * <br><br>
 * The values are key/value pairs of two types.  The first type of pairs consists of
 * those whose keys are reserved and defined in this specification or  by individual
 * implementations.  The values in the pairs with reserved keys have specified meanings.
 * <br><br>
 * The other type of pairs consists of those that create Java language Bindings, the values are
 * usually represented in scripts by the corresponding keys or by decorated forms of them.
 *
 * @author Mike Grogan
 * @since 1.6
 */

public interface ScriptEngine  {

    /**
     * Reserved key for a named value that passes
     * an array of positional arguments to a script.
     */
    public static final String ARGV="javax.script.argv";

    /**
     * Reserved key for a named value that is
     * the name of the file being executed.
     */
    public static final String FILENAME = "javax.script.filename";

    /**
     * Reserved key for a named value that is
     * the name of the <code>ScriptEngine</code> implementation.
     */
    public static final String ENGINE = "javax.script.engine";

    /**
     * Reserved key for a named value that identifies
     * the version of the <code>ScriptEngine</code> implementation.
     */
    public static final String ENGINE_VERSION = "javax.script.engine_version";

    /**
     * Reserved key for a named value that identifies
     * the short name of the scripting language.  The name is used by the
     * <code>ScriptEngineManager</code> to locate a <code>ScriptEngine</code>
     * with a given name in the <code>getEngineByName</code> method.
     */
    public static final String NAME = "javax.script.name";

    /**
     * Reserved key for a named value that is
     * the full name of Scripting Language supported by the implementation.
     */
    public static final String LANGUAGE = "javax.script.language";

    /**
     * Reserved key for the named value that identifies
     * the version of the scripting language supported by the implementation.
     */
    public static final String LANGUAGE_VERSION ="javax.script.language_version";


    /**
     * Causes the immediate execution of the script whose source is the String
     * passed as the first argument.  The script may be reparsed or recompiled before
     * execution.  State left in the engine from previous executions, including
     * variable values and compiled procedures may be visible during this execution.
     *
     * @param script The script to be executed by the script engine.
     *
     * @param context A <code>ScriptContext</code> exposing sets of attributes in
     * different scopes.  The meanings of the scopes <code>ScriptContext.GLOBAL_SCOPE</code>,
     * and <code>ScriptContext.ENGINE_SCOPE</code> are defined in the specification.
     * <br><br>
     * The <code>ENGINE_SCOPE</code> <code>Bindings</code> of the <code>ScriptContext</code> contains the
     * bindings of scripting variables to application objects to be used during this
     * script execution.
     *
     *
     * @return The value returned from the execution of the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if either argument is null.
     */
    public Object eval(String script, ScriptContext context) throws ScriptException;


    /**
     * Same as <code>eval(String, ScriptContext)</code> where the source of the script
     * is read from a <code>Reader</code>.
     *
     * @param reader The source of the script to be executed by the script engine.
     *
     * @param context The <code>ScriptContext</code> passed to the script engine.
     *
     * @return The value returned from the execution of the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if either argument is null.
     */
    public Object eval(Reader reader , ScriptContext context) throws ScriptException;

    /**
     * Executes the specified script.  The default <code>ScriptContext</code> for the <code>ScriptEngine</code>
     * is used.
     *
     * @param script The script language source to be executed.
     *
     * @return The value returned from the execution of the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if the argument is null.
     */
    public Object eval(String script) throws ScriptException;

    /**
     * Same as <code>eval(String)</code> except that the source of the script is
     * provided as a <code>Reader</code>
     *
     * @param reader The source of the script.
     *
     * @return The value returned by the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if the argument is null.
     */
    public Object eval(Reader reader) throws ScriptException;

    /**
     * Executes the script using the <code>Bindings</code> argument as the <code>ENGINE_SCOPE</code>
     * <code>Bindings</code> of the <code>ScriptEngine</code> during the script execution.  The
     * <code>Reader</code>, <code>Writer</code> and non-<code>ENGINE_SCOPE</code> <code>Bindings</code> of the
     * default <code>ScriptContext</code> are used. The <code>ENGINE_SCOPE</code>
     * <code>Bindings</code> of the <code>ScriptEngine</code> is not changed, and its
     * mappings are unaltered by the script execution.
     *
     * @param script The source for the script.
     *
     * @param n The <code>Bindings</code> of attributes to be used for script execution.
     *
     * @return The value returned by the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if either argument is null.
     */
    public Object eval(String script, Bindings n) throws ScriptException;

    /**
     * Same as <code>eval(String, Bindings)</code> except that the source of the script
     * is provided as a <code>Reader</code>.
     *
     * @param reader The source of the script.
     * @param n The <code>Bindings</code> of attributes.
     *
     * @return The value returned by the script.
     *
     * @throws ScriptException if an error occurs in script. ScriptEngines should create and throw
     * <code>ScriptException</code> wrappers for checked Exceptions thrown by underlying scripting
     * implementations.
     * @throws NullPointerException if either argument is null.
     */
    public Object eval(Reader reader , Bindings n) throws ScriptException;



    /**
     * Sets a key/value pair in the state of the ScriptEngine that may either create
     * a Java Language Binding to be used in the execution of scripts or be used in some
     * other way, depending on whether the key is reserved.  Must have the same effect as
     * <code>getBindings(ScriptContext.ENGINE_SCOPE).put</code>.
     *
     * @param key The name of named value to add
     * @param value The value of named value to add.
     *
     * @throws NullPointerException if key is null.
     * @throws IllegalArgumentException if key is empty.
     */
    public void put(String key, Object value);


    /**
     * Retrieves a value set in the state of this engine.  The value might be one
     * which was set using <code>setValue</code> or some other value in the state
     * of the <code>ScriptEngine</code>, depending on the implementation.  Must have the same effect
     * as <code>getBindings(ScriptContext.ENGINE_SCOPE).get</code>
     *
     * @param key The key whose value is to be returned
     * @return the value for the given key
     *
     * @throws NullPointerException if key is null.
     * @throws IllegalArgumentException if key is empty.
     */
    public Object get(String key);


    /**
     * Returns a scope of named values.  The possible scopes are:
     * <br><br>
     * <ul>
     * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The set of named values representing global
     * scope. If this <code>ScriptEngine</code> is created by a <code>ScriptEngineManager</code>,
     * then the manager sets global scope bindings. This may be <code>null</code> if no global
     * scope is associated with this <code>ScriptEngine</code></li>
     * <li><code>ScriptContext.ENGINE_SCOPE</code> - The set of named values representing the state of
     * this <code>ScriptEngine</code>.  The values are generally visible in scripts using
     * the associated keys as variable names.</li>
     * <li>Any other value of scope defined in the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     * </li>
     * </ul>
     * <br><br>
     * The <code>Bindings</code> instances that are returned must be identical to those returned by the
     * <code>getBindings</code> method of <code>ScriptContext</code> called with corresponding arguments on
     * the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     *
     * @param scope Either <code>ScriptContext.ENGINE_SCOPE</code> or <code>ScriptContext.GLOBAL_SCOPE</code>
     * which specifies the <code>Bindings</code> to return.  Implementations of <code>ScriptContext</code>
     * may define additional scopes.  If the default <code>ScriptContext</code> of the <code>ScriptEngine</code>
     * defines additional scopes, any of them can be passed to get the corresponding <code>Bindings</code>.
     *
     * @return The <code>Bindings</code> with the specified scope.
     *
     * @throws IllegalArgumentException if specified scope is invalid
     *
     */
    public Bindings getBindings(int scope);

    /**
     * Sets a scope of named values to be used by scripts.  The possible scopes are:
     *<br><br>
     * <ul>
     * <li><code>ScriptContext.ENGINE_SCOPE</code> - The specified <code>Bindings</code> replaces the
     * engine scope of the <code>ScriptEngine</code>.
     * </li>
     * <li><code>ScriptContext.GLOBAL_SCOPE</code> - The specified <code>Bindings</code> must be visible
     * as the <code>GLOBAL_SCOPE</code>.
     * </li>
     * <li>Any other value of scope defined in the default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     *</li>
     * </ul>
     * <br><br>
     * The method must have the same effect as calling the <code>setBindings</code> method of
     * <code>ScriptContext</code> with the corresponding value of <code>scope</code> on the default
     * <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     *
     * @param bindings The <code>Bindings</code> for the specified scope.
     * @param scope The specified scope.  Either <code>ScriptContext.ENGINE_SCOPE</code>,
     * <code>ScriptContext.GLOBAL_SCOPE</code>, or any other valid value of scope.
     *
     * @throws IllegalArgumentException if the scope is invalid
     * @throws NullPointerException if the bindings is null and the scope is
     * <code>ScriptContext.ENGINE_SCOPE</code>
     */
    public void setBindings(Bindings bindings, int scope);


    /**
     * Returns an uninitialized <code>Bindings</code>.
     *
     * @return A <code>Bindings</code> that can be used to replace the state of this <code>ScriptEngine</code>.
     **/
    public Bindings createBindings();


    /**
     * Returns the default <code>ScriptContext</code> of the <code>ScriptEngine</code> whose Bindings, Reader
     * and Writers are used for script executions when no <code>ScriptContext</code> is specified.
     *
     * @return The default <code>ScriptContext</code> of the <code>ScriptEngine</code>.
     */
    public ScriptContext getContext();

    /**
     * Sets the default <code>ScriptContext</code> of the <code>ScriptEngine</code> whose Bindings, Reader
     * and Writers are used for script executions when no <code>ScriptContext</code> is specified.
     *
     * @param context A <code>ScriptContext</code> that will replace the default <code>ScriptContext</code> in
     * the <code>ScriptEngine</code>.
     * @throws NullPointerException if context is null.
     */
    public void setContext(ScriptContext context);

    /**
     * Returns a <code>ScriptEngineFactory</code> for the class to which this <code>ScriptEngine</code> belongs.
     *
     * @return The <code>ScriptEngineFactory</code>
     */
    public ScriptEngineFactory getFactory();
}
