/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package shared;

import java.lang.reflect.InvocationTargetException;

/*******************************************************************/
// Invoke different target method callers
/*******************************************************************/

public class Caller {
    private ClassLoader loader;
    private Class paramClass;
    private Class targetClass;
    private boolean passed = true;
    private Checker checker;

    public Caller(ClassLoader loader, Checker checker,
                  Class paramClass, Class targetClass) {
        this.loader = loader;
        this.paramClass = paramClass;
        this.targetClass = targetClass;
        this.checker = checker;
    }

    public boolean isPassed() {
        return passed;
    }

    public String call(String invoker) {
        try {
            Class clazz = loader.loadClass(invoker);

            String expectedBehavior = checker.check(clazz);

            String result = null;
            Throwable exc = null;
            try {
                java.lang.reflect.Method m = clazz.getDeclaredMethod("call", paramClass);
                result = (String) m.invoke(null, targetClass.newInstance());
            } catch (InvocationTargetException e) {
                exc = e.getCause();
            } catch (Throwable e) {
                exc = e;
            }

            if (result == null) {
                if (exc != null) {
                    result = exc.getClass().getName();
                } else {
                    result = "null";
                }
            }

            if (!(result.equals(expectedBehavior) || "".equals(expectedBehavior)) ) {
                passed = false;
                result = String.format("%s/%s", result, expectedBehavior);
            }

            return Checker.abbreviateResult(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
