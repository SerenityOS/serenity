/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.beans;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

/**
 * A MethodDescriptor describes a particular method that a Java Bean
 * supports for external access from other components.
 *
 * @since 1.1
 */

public class MethodDescriptor extends FeatureDescriptor {

    private final MethodRef methodRef = new MethodRef();

    private String[] paramNames;

    private List<WeakReference<Class<?>>> params;

    private ParameterDescriptor[] parameterDescriptors;

    /**
     * Constructs a {@code MethodDescriptor} from a
     * {@code Method}.
     *
     * @param method    The low-level method information.
     */
    public MethodDescriptor(Method method) {
        this(method, null);
    }


    /**
     * Constructs a {@code MethodDescriptor} from a
     * {@code Method} providing descriptive information for each
     * of the method's parameters.
     *
     * @param method    The low-level method information.
     * @param parameterDescriptors  Descriptive information for each of the
     *                          method's parameters.
     */
    public MethodDescriptor(Method method,
                ParameterDescriptor[] parameterDescriptors) {
        setName(method.getName());
        setMethod(method);
        this.parameterDescriptors = (parameterDescriptors != null)
                ? parameterDescriptors.clone()
                : null;
    }

    /**
     * Gets the method that this MethodDescriptor encapsulates.
     *
     * @return The low-level description of the method
     */
    public synchronized Method getMethod() {
        Method method = this.methodRef.get();
        if (method == null) {
            Class<?> cls = getClass0();
            String name = getName();
            if ((cls != null) && (name != null)) {
                Class<?>[] params = getParams();
                if (params == null) {
                    for (int i = 0; i < 3; i++) {
                        // Find methods for up to 2 params. We are guessing here.
                        // This block should never execute unless the classloader
                        // that loaded the argument classes disappears.
                        method = Introspector.findMethod(cls, name, i, null);
                        if (method != null) {
                            break;
                        }
                    }
                } else {
                    method = Introspector.findMethod(cls, name, params.length, params);
                }
                setMethod(method);
            }
        }
        return method;
    }

    private synchronized void setMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        setParams(getParameterTypes(getClass0(), method));
        this.methodRef.set(method);
    }

    private synchronized void setParams(Class<?>[] param) {
        if (param == null) {
            return;
        }
        paramNames = new String[param.length];
        params = new ArrayList<>(param.length);
        for (int i = 0; i < param.length; i++) {
            paramNames[i] = param[i].getName();
            params.add(new WeakReference<Class<?>>(param[i]));
        }
    }

    // pp getParamNames used as an optimization to avoid method.getParameterTypes.
    String[] getParamNames() {
        return paramNames;
    }

    private synchronized Class<?>[] getParams() {
        Class<?>[] clss = new Class<?>[params.size()];

        for (int i = 0; i < params.size(); i++) {
            Reference<? extends Class<?>> ref = (Reference<? extends Class<?>>)params.get(i);
            Class<?> cls = ref.get();
            if (cls == null) {
                return null;
            } else {
                clss[i] = cls;
            }
        }
        return clss;
    }

    /**
     * Gets the ParameterDescriptor for each of this MethodDescriptor's
     * method's parameters.
     *
     * @return The locale-independent names of the parameters.  May return
     *          a null array if the parameter names aren't known.
     */
    public ParameterDescriptor[] getParameterDescriptors() {
        return (this.parameterDescriptors != null)
                ? this.parameterDescriptors.clone()
                : null;
    }

    private static Method resolve(Method oldMethod, Method newMethod) {
        if (oldMethod == null) {
            return newMethod;
        }
        if (newMethod == null) {
            return oldMethod;
        }
        return !oldMethod.isSynthetic() && newMethod.isSynthetic() ? oldMethod : newMethod;
    }

    /*
     * Package-private constructor
     * Merge two method descriptors.  Where they conflict, give the
     * second argument (y) priority over the first argument (x).
     * @param x  The first (lower priority) MethodDescriptor
     * @param y  The second (higher priority) MethodDescriptor
     */

    MethodDescriptor(MethodDescriptor x, MethodDescriptor y) {
        super(x, y);

        this.methodRef.set(resolve(x.methodRef.get(), y.methodRef.get()));
        params = x.params;
        if (y.params != null) {
            params = y.params;
        }
        paramNames = x.paramNames;
        if (y.paramNames != null) {
            paramNames = y.paramNames;
        }

        parameterDescriptors = x.parameterDescriptors;
        if (y.parameterDescriptors != null) {
            parameterDescriptors = y.parameterDescriptors;
        }
    }

    /*
     * Package-private dup constructor
     * This must isolate the new object from any changes to the old object.
     */
    MethodDescriptor(MethodDescriptor old) {
        super(old);

        this.methodRef.set(old.getMethod());
        params = old.params;
        paramNames = old.paramNames;

        if (old.parameterDescriptors != null) {
            int len = old.parameterDescriptors.length;
            parameterDescriptors = new ParameterDescriptor[len];
            for (int i = 0; i < len ; i++) {
                parameterDescriptors[i] = new ParameterDescriptor(old.parameterDescriptors[i]);
            }
        }
    }

    void appendTo(StringBuilder sb) {
        appendTo(sb, "method", this.methodRef.get());
        if (this.parameterDescriptors != null) {
            sb.append("; parameterDescriptors={");
            for (ParameterDescriptor pd : this.parameterDescriptors) {
                sb.append(pd).append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("}");
        }
    }
}
