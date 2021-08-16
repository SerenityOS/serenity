/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit;

import java.util.regex.Pattern;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * This class provides basic JavaFX property related utility methods.
 * Refer to the JavaFX conventions in the VisibleMemberTable comments.
 */
public class PropertyUtils {

    final TypeMirror jbObservableType;

    final Pattern fxMethodPatterns;

    final boolean javafx;

    final Types typeUtils;

    PropertyUtils(BaseConfiguration configuration) {
        BaseOptions options = configuration.getOptions();
        javafx = options.javafx();

        typeUtils = configuration.docEnv.getTypeUtils();

        // Disable strict check for JDK's without FX.
        TypeMirror jboType = options.disableJavaFxStrictChecks()
                ? null
                : configuration.utils.getSymbol("javafx.beans.Observable");

        jbObservableType = jboType != null
                ? configuration.docEnv.getTypeUtils().erasure(jboType)
                : null;

        fxMethodPatterns = javafx
                ? Pattern.compile("[sg]et\\p{Upper}.*||is\\p{Upper}.*")
                : null;
    }

    /**
     * Returns a base name for a property method. Supposing we
     * have {@code BooleanProperty acmeProperty()}, then "acme"
     * will be returned.
     * @param propertyMethod
     * @return the base name of a property method.
     */
    public String getBaseName(ExecutableElement propertyMethod) {
        String name = propertyMethod.getSimpleName().toString();
        String baseName = name.substring(0, name.indexOf("Property"));
        return baseName;
    }

    /**
     * Returns a property getter's name. Supposing we have a property
     * method {@code DoubleProperty acmeProperty()}, then "getAcme"
     * will be returned.
     * @param propertyMethod
     * @return the property getter's name.
     */
    public String getGetName(ExecutableElement propertyMethod) {
        String baseName = getBaseName(propertyMethod);
        String fnUppercased = "" +
                Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
        return "get" + fnUppercased;
    }

    /**
     * Returns an "is" method's name for a property method. Supposing
     * we have a property method {@code BooleanProperty acmeProperty()},
     * then "isAcme" will be returned.
     * @param propertyMethod
     * @return the property is getter's name.
     */
    public String getIsName(ExecutableElement propertyMethod) {
        String baseName = getBaseName(propertyMethod);
        String fnUppercased = "" +
                Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
        return "is" + fnUppercased;
    }

    /**
     * Returns true if a property method could have an "is" method, meaning
     * {@code isAcme} could exist for a property method.
     * @param propertyMethod
     * @return true if the property could have an "is" method, false otherwise.
     */
    public boolean hasIsMethod(ExecutableElement propertyMethod) {
        String propertyTypeName = propertyMethod.getReturnType().toString();
        return "boolean".equals(propertyTypeName) ||
                propertyTypeName.endsWith("BooleanProperty");
    }

    /**
     * Returns a property setter's name. Supposing we have a property
     * method {@code DoubleProperty acmeProperty()}, then "setAcme"
     * will be returned.
     * @param propertyMethod
     * @return the property setter's method name.
     */
    public String getSetName(ExecutableElement propertyMethod) {
        String baseName = getBaseName(propertyMethod);
        String fnUppercased = "" +
                Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
        return "set" + fnUppercased;
    }

    /**
     * Returns true if the given setter method is a valid property setter
     * method.
     * @param setterMethod
     * @return true if setter method, false otherwise.
     */
    public boolean isValidSetterMethod(ExecutableElement setterMethod) {
        return setterMethod.getReturnType().getKind() == TypeKind.VOID;
    }

    /**
     * Returns true if the method is a property method.
     * @param propertyMethod
     * @return true if the method is a property method, false otherwise.
     */
    public boolean isPropertyMethod(ExecutableElement propertyMethod) {
        if (!javafx ||
                !propertyMethod.getParameters().isEmpty() ||
                !propertyMethod.getTypeParameters().isEmpty()) {
            return false;
        }
        String methodName = propertyMethod.getSimpleName().toString();
        if (!methodName.endsWith("Property") ||
                fxMethodPatterns.matcher(methodName).matches()) {
            return false;
        }

        TypeMirror returnType = propertyMethod.getReturnType();
        if (jbObservableType == null) {
            // JavaFX references missing, make a lazy backward compatible check.
            return returnType.getKind() != TypeKind.VOID;
        } else {
            // Apply strict checks since JavaFX references are available
            returnType = typeUtils.erasure(propertyMethod.getReturnType());
            return typeUtils.isAssignable(returnType, jbObservableType);
        }
    }
}
