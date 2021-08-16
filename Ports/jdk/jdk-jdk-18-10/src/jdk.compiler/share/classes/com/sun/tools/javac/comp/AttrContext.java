/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.comp.DeferredAttr.AttributionMode;

/** Contains information specific to the attribute and enter
 *  passes, to be used in place of the generic field in environments.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AttrContext {

    /** The scope of local symbols.
     */
    WriteableScope scope = null;

    /** The number of enclosing `static' modifiers.
     */
    int staticLevel = 0;

    /** Is this an environment for a this(...) or super(...) call?
     */
    boolean isSelfCall = false;

    /** Are we evaluating the selector of a `super' or type name?
     */
    boolean selectSuper = false;

    /** Is the current target of lambda expression or method reference serializable or is this a
     *  serializable class?
     */
    boolean isSerializable = false;

    /** Is this a serializable lambda?
     */
    boolean isSerializableLambda = false;

    /** Is this a lambda environment?
     */
    boolean isLambda = false;

    /** Is this a speculative attribution environment?
     */
    AttributionMode attributionMode = AttributionMode.FULL;

    /**
     *  Is this an attribution environment for an anonymous class instantiated using <> ?
     */
    boolean isAnonymousDiamond = false;

    /**
     *  Is this an attribution environment for an instance creation expression?
     */
    boolean isNewClass = false;

    /** Indicate if the type being visited is a service implementation
     */
    boolean visitingServiceImplementation = false;

    /** Are arguments to current function applications boxed into an array for varargs?
     */
    Resolve.MethodResolutionPhase pendingResolutionPhase = null;

    /** A record of the lint/SuppressWarnings currently in effect
     */
    Lint lint;

    /** The variable whose initializer is being attributed
     * useful for detecting self-references in variable initializers
     */
    Symbol enclVar = null;

    /** ResultInfo to be used for attributing 'return' statement expressions
     * (set by Attr.visitMethod and Attr.visitLambda)
     */
    Attr.ResultInfo returnResult = null;

    /** ResultInfo to be used for attributing 'yield' statement expressions
     * (set by Attr.visitSwitchExpression)
     */
    Attr.ResultInfo yieldResult = null;

    /** Symbol corresponding to the site of a qualified default super call
     */
    Type defaultSuperCallSite = null;

    /** Tree that when non null, is to be preferentially used in diagnostics.
     *  Usually Env<AttrContext>.tree is the tree to be referred to in messages,
     *  but this may not be true during the window a method is looked up in enclosing
     *  contexts (JDK-8145466)
     */
    JCTree preferredTreeForDiagnostics;

    /** Duplicate this context, replacing scope field and copying all others.
     */
    AttrContext dup(WriteableScope scope) {
        AttrContext info = new AttrContext();
        info.scope = scope;
        info.staticLevel = staticLevel;
        info.isSelfCall = isSelfCall;
        info.selectSuper = selectSuper;
        info.pendingResolutionPhase = pendingResolutionPhase;
        info.lint = lint;
        info.enclVar = enclVar;
        info.returnResult = returnResult;
        info.yieldResult = yieldResult;
        info.defaultSuperCallSite = defaultSuperCallSite;
        info.isSerializable = isSerializable;
        info.isLambda = isLambda;
        info.isSerializableLambda = isSerializableLambda;
        info.attributionMode = attributionMode;
        info.isAnonymousDiamond = isAnonymousDiamond;
        info.isNewClass = isNewClass;
        info.preferredTreeForDiagnostics = preferredTreeForDiagnostics;
        info.visitingServiceImplementation = visitingServiceImplementation;
        return info;
    }

    /** Duplicate this context, copying all fields.
     */
    AttrContext dup() {
        return dup(scope);
    }

    public Iterable<Symbol> getLocalElements() {
        if (scope == null)
            return List.nil();
        return scope.getSymbols();
    }

    boolean lastResolveVarargs() {
        return pendingResolutionPhase != null &&
                pendingResolutionPhase.isVarargsRequired();
    }

    @Override
    public String toString() {
        return "AttrContext[" + scope.toString() + "]";
    }
}
