/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;


/**
 * This class represents the scope containing the type variables of
 * a method.
 */
public class MethodScope extends AbstractScope<Method> {

    // constructor is private to enforce use of factory method
    private MethodScope(Method m){
        super(m);
    }

    // utility method; computes enclosing class, from which we can
    // derive enclosing scope.
    private Class<?> getEnclosingClass(){
        return getRecvr().getDeclaringClass();
    }

    /**
     * Overrides the abstract method in the superclass.
     * @return the enclosing scope
     */
    protected Scope computeEnclosingScope() {
        // the enclosing scope of a (generic) method is the scope of the
        // class in which it was declared.
        return ClassScope.make(getEnclosingClass());
    }

    /**
     * Factory method. Takes a {@code Method} object and creates a
     * scope for it.
     * @param m - A Method whose scope we want to obtain
     * @return The type-variable scope for the method m
     */
    public static MethodScope make(Method m) {
        return new MethodScope(m);
    }
}
