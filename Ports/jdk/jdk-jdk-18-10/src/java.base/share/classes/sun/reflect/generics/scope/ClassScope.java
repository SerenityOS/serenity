/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.scope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/**
 * This class represents the scope containing the type variables of
 * a class.
 */
public class ClassScope extends AbstractScope<Class<?>> implements Scope {

    // constructor is private to enforce use of factory method
    private ClassScope(Class<?> c){
        super(c);
    }

    /**
     * Overrides the abstract method in the superclass.
     * @return the enclosing scope
     */
    protected Scope computeEnclosingScope() {
        Class<?> receiver = getRecvr();

        Method m = receiver.getEnclosingMethod();
        if (m != null)
            // Receiver is a local or anonymous class enclosed in a
            // method.
            return MethodScope.make(m);

        Constructor<?> cnstr = receiver.getEnclosingConstructor();
        if (cnstr != null)
            // Receiver is a local or anonymous class enclosed in a
            // constructor.
            return ConstructorScope.make(cnstr);

        Class<?> c = receiver.getEnclosingClass();
        // if there is a declaring class, recvr is a member class
        // and its enclosing scope is that of the declaring class
        if (c != null)
            // Receiver is a local class, an anonymous class, or a
            // member class (static or not).
            return ClassScope.make(c);

        // otherwise, recvr is a top level class, and it has no real
        // enclosing scope.
        return DummyScope.make();
    }

    /**
     * Factory method. Takes a {@code Class} object and creates a
     * scope for it.
     * @param c - a Class whose scope we want to obtain
     * @return The type-variable scope for the class c
     */
    public static ClassScope make(Class<?> c) { return new ClassScope(c);}

}
