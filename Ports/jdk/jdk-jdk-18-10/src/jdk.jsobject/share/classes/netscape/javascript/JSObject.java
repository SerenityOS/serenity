/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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

package netscape.javascript;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * <p>
 * Allows Java code to manipulate JavaScript objects.
 * </p>
 *
 * <p>
 * When a JavaScript object is passed or returned to Java code, it
 * is wrapped in an instance of {@code JSObject}. When a
 * {@code JSObject} instance is passed to the JavaScript engine,
 * it is unwrapped back to its original JavaScript object. The
 * {@code JSObject} class provides a way to invoke JavaScript
 * methods and examine JavaScript properties.
 * </p>
 *
 * <p> Any data returned from the JavaScript engine to Java is
 * converted to Java data types. Certain data passed to the JavaScript
 * engine is converted to JavaScript data types.
 * </p>
 *
 */
@SuppressWarnings("deprecation")
public abstract class JSObject {
    /**
     * Constructs a new JSObject. Users should neither call this method nor
     * subclass JSObject.
     */
    protected JSObject()  {
    }

    /**
     * Calls a JavaScript method. Equivalent to
     * "this.methodName(args[0], args[1], ...)" in JavaScript.
     *
     * @param methodName The name of the JavaScript method to be invoked.
     * @param args the Java objects passed as arguments to the method.
     * @return Result of the method.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract Object call(String methodName, Object... args) throws JSException;

    /**
     * Evaluates a JavaScript expression. The expression is a string of
     * JavaScript source code which will be evaluated in the context given by
     * "this".
     *
     * @param s The JavaScript expression.
     * @return Result of the JavaScript evaluation.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract Object eval(String s) throws JSException;

    /**
     * Retrieves a named member of a JavaScript object. Equivalent to
     * "this.name" in JavaScript.
     *
     * @param name The name of the JavaScript property to be accessed.
     * @return The value of the propery.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract Object getMember(String name) throws JSException;

    /**
     * Sets a named member of a JavaScript object. Equivalent to
     * "this.name = value" in JavaScript.
     *
     * @param name The name of the JavaScript property to be accessed.
     * @param value The value of the propery.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract void setMember(String name, Object value) throws JSException;

    /**
     * Removes a named member of a JavaScript object. Equivalent
     * to "delete this.name" in JavaScript.
     *
     * @param name The name of the JavaScript property to be removed.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract void removeMember(String name) throws JSException;

    /**
     * Retrieves an indexed member of a JavaScript object. Equivalent to
     * "this[index]" in JavaScript.
     *
     * @param index The index of the array to be accessed.
     * @return The value of the indexed member.
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract Object getSlot(int index) throws JSException;

    /**
     * Sets an indexed member of a JavaScript object. Equivalent to
     * "this[index] = value" in JavaScript.
     *
     * @param index The index of the array to be accessed.
     * @param value The value to set
     * @throws JSException when an error is reported from the browser or
     * JavaScript engine.
     */
    public abstract void setSlot(int index, Object value) throws JSException;

}
