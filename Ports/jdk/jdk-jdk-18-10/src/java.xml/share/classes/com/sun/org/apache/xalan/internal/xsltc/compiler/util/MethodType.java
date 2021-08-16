/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: Oct 2017
 */
public final class MethodType extends Type {
    private final Type _resultType;
    private final List<Type> _argsType;

    public MethodType(Type resultType) {
        _argsType = null;
        _resultType = resultType;
    }

    public MethodType(Type resultType, Type arg1) {
        if (arg1 != Type.Void) {
            _argsType = new ArrayList<>();
            _argsType.add(arg1);
        }
        else {
            _argsType = null;
        }
        _resultType = resultType;
    }

    public MethodType(Type resultType, Type arg1, Type arg2) {
        _argsType = new ArrayList<>(2);
        _argsType.add(arg1);
        _argsType.add(arg2);
        _resultType = resultType;
    }

    public MethodType(Type resultType, Type arg1, Type arg2, Type arg3) {
        _argsType = new ArrayList<>(3);
        _argsType.add(arg1);
        _argsType.add(arg2);
        _argsType.add(arg3);
        _resultType = resultType;
    }

    public MethodType(Type resultType, List<Type> argsType) {
        _resultType = resultType;
        _argsType = argsType.size() > 0 ? argsType : null;
    }

    public String toString() {
        StringBuffer result = new StringBuffer("method{");
        if (_argsType != null) {
            final int count = _argsType.size();
            for (int i=0; i<count; i++) {
                result.append(_argsType.get(i));
                if (i != (count-1)) result.append(',');
            }
        }
        else {
            result.append("void");
        }
        result.append('}');
        return result.toString();
    }

    public String toSignature() {
        return toSignature("");
    }

    /**
     * Returns the signature of this method that results by adding
     * <code>lastArgSig</code> to the end of the argument list.
     */
    public String toSignature(String lastArgSig) {
        final StringBuffer buffer = new StringBuffer();
        buffer.append('(');
        if (_argsType != null) {
            final int n = _argsType.size();
            for (int i = 0; i < n; i++) {
                buffer.append((_argsType.get(i)).toSignature());
            }
        }
        return buffer
            .append(lastArgSig)
            .append(')')
            .append(_resultType.toSignature())
            .toString();
    }

    public com.sun.org.apache.bcel.internal.generic.Type toJCType() {
        return null;    // should never be called
    }

    public boolean identicalTo(Type other) {
        boolean result = false;
        if (other instanceof MethodType) {
            final MethodType temp = (MethodType) other;
            if (_resultType.identicalTo(temp._resultType)) {
                final int len = argsCount();
                result = len == temp.argsCount();
                for (int i = 0; i < len && result; i++) {
                    final Type arg1 = _argsType.get(i);
                    final Type arg2 = temp._argsType.get(i);
                    result = arg1.identicalTo(arg2);
                }
            }
        }
        return result;
    }

    public int distanceTo(Type other) {
        int result = Integer.MAX_VALUE;
        if (other instanceof MethodType) {
            final MethodType mtype = (MethodType) other;
            if (_argsType != null) {
                final int len = _argsType.size();
                if (len == mtype._argsType.size()) {
                    result = 0;
                    for (int i = 0; i < len; i++) {
                        Type arg1 = _argsType.get(i);
                        Type arg2 = mtype._argsType.get(i);
                        final int temp = arg1.distanceTo(arg2);
                        if (temp == Integer.MAX_VALUE) {
                            result = temp;  // return MAX_VALUE
                            break;
                        }
                        else {
                            result += arg1.distanceTo(arg2);
                        }
                    }
                }
            }
            else if (mtype._argsType == null) {
                result = 0;   // both methods have no args
            }
        }
        return result;
    }

    public Type resultType() {
        return _resultType;
    }

    public List<Type> argsType() {
        return _argsType;
    }

    public int argsCount() {
        return _argsType == null ? 0 : _argsType.size();
    }
}
