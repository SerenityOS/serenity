/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */
package nsk.jvmti.unit.FollowReferences;

import java.lang.ref.*;
import java.lang.reflect.*;

// 2 superinterfaces
// (Henri Cartier-Bresson, Alex Webb and Bruno Barbey are famous photographers)
interface Cartier {
    public static final int c21 = 21;
    static final int c22 = 22;
}

interface Bresson {
    public static final int c21 = 21;
    static final int c22 = 22;
}

class HenriCartierBresson implements Cartier, Bresson {
    private   final int c31 = 31;
    protected final int c32 = 32;
    public    final int c33 = 33;
}

class AlexWebb extends HenriCartierBresson {
    public static final int aw = 50;
}

public class FollowRefObjects {

    static final int ARRAY_SIZE = 3;

    // This array has to be up-to-date with the jvmtiHeapReferenceKind enum
    // TODO: auto-generate
    static final int JVMTI_HEAP_REFERENCE_CLASS             = 1;
    static final int JVMTI_HEAP_REFERENCE_FIELD             = 2;
    static final int JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT     = 3;
    static final int JVMTI_HEAP_REFERENCE_CLASS_LOADER      = 4;
    static final int JVMTI_HEAP_REFERENCE_SIGNERS           = 5;
    static final int JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN = 6;
    static final int JVMTI_HEAP_REFERENCE_INTERFACE         = 7;
    static final int JVMTI_HEAP_REFERENCE_STATIC_FIELD      = 8;
    static final int JVMTI_HEAP_REFERENCE_CONSTANT_POOL     = 9;
    static final int JVMTI_HEAP_REFERENCE_SUPERCLASS        = 10;
    static final int JVMTI_HEAP_REFERENCE_JNI_GLOBAL        = 21;
    static final int JVMTI_HEAP_REFERENCE_SYSTEM_CLASS      = 22;
    static final int JVMTI_HEAP_REFERENCE_MONITOR           = 23;
    static final int JVMTI_HEAP_REFERENCE_STACK_LOCAL       = 24;
    static final int JVMTI_HEAP_REFERENCE_JNI_LOCAL         = 25;
    static final int JVMTI_HEAP_REFERENCE_THREAD            = 26;
    static final int JVMTI_HEAP_REFERENCE_OTHER             = 27;

    public boolean[] _boolArr;
    public byte[]    _byteArr;
    public short[]   _shortArr;
    public int[]     _intArr;
    public long[]    _longArr;
    public float[]   _floatArr;
    public double[]  _doubleArr;

    public String[]   _strArr;
    public Object[]   _objArr;
    public Object[][] _objArrArr;

    // This class obtained through the reflection API
    class FRHandler implements InvocationHandler {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
    }

    public SoftReference<Object>    _softRef;
    public Object           _softReferree;

    public WeakReference<Object>     _weakRef;
    public Object           _weakReferree;

    public PhantomReference<Object>  _phantomRef;
    public Object           _phantomReferree;

    public FollowRefObjects _selfRef1, _selfRef2, _selfRef3;

    public Thread           _thread;

    public ClassLoader      _classLoader;
    public Object           _loadedObject;

    // Circular list
    class NextRef {
        Object _next;
    }

    public NextRef[]        _nextRef;

    class BrunoBarbey extends HenriCartierBresson {
        public static final int bb = 60;
    }

    public AlexWebb    _aw;
    public BrunoBarbey _bb;

    public Class            _reflectClass;
    public Cartier         _cartierInAMirror;

    /** Create references */
    public void createObjects() {

        resetCurTag();
        tag(this, "this");

        // A plenty of array kinds
        _boolArr = new boolean[ARRAY_SIZE];    tag(_boolArr, "_boolArr");
        _byteArr = new byte[ARRAY_SIZE];       tag(_byteArr, "_byteArr");
        _shortArr = new short[ARRAY_SIZE];     tag(_shortArr, "_shortArr");
        _intArr = new int[ARRAY_SIZE];         tag(_intArr, "_intArr");
        _longArr = new long[ARRAY_SIZE];       tag(_longArr, "_longArr");
        _floatArr = new float[ARRAY_SIZE];     tag(_floatArr, "_floatArr");
        _doubleArr = new double[ARRAY_SIZE];   tag(_doubleArr, "_doubleArr");

        _objArrArr = new Object[ARRAY_SIZE][ARRAY_SIZE]; tag(_objArrArr, "_objArrArr");

        // Multiple references to the same object
        _objArr = new Object[ARRAY_SIZE];      tag(_objArr, "_objArr");
        _objArr[0] = new Object();             tag(_objArr[0], "_objArr[0]");
        _objArr[1] = _objArr[2] = _objArr[0];

        // Multiple references to myself
        _selfRef1 = _selfRef2 = _selfRef3 = this;

        // Circular linked list
        _nextRef = new NextRef[2];             tag(_nextRef, "_nextRef");
        _nextRef[0] = new NextRef();           tag(_nextRef[0], "_nextRef[0]");
        _nextRef[1] = new NextRef();           tag(_nextRef[1], "_nextRef[1]");
        _nextRef[0]._next = _nextRef[1];
        _nextRef[1]._next = _nextRef[0];

        // Strings
        _strArr = new String[ARRAY_SIZE];      tag(_strArr, "_strArr");
        _strArr[0] = "CTAKAH OT CAXAPA HE TPECHET"; tag(_strArr[0], "_strArr[0]"); // ASCII characters
        _strArr[1] = "?????? ?? ??????? ???????!";  tag(_strArr[1], "_strArr[1]"); // Russian characters
        _strArr[2] = getClass().getCanonicalName(); tag(_strArr[2], "_strArr[2]"); // Copy some existing string

        // A proxy class created by reflection API
        _reflectClass = java.lang.reflect.Proxy.getProxyClass(
                getClass().getClassLoader(),
                new Class[] { Cartier.class});
        tag(_reflectClass, "_reflectClass");

        _cartierInAMirror = (Cartier) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { Cartier.class },
                new FRHandler());
        tag(_cartierInAMirror, "_cartierInAMirror");

        // Soft, Weak, Phantom References
        _softRef = new SoftReference<Object>(_softReferree = new Object());
         tag(_softRef, "_softRef");          tag(_softReferree, "_softReferree");

        _weakRef = new WeakReference<Object>(_weakReferree = new Object());
        tag(_weakRef, "_weakRef");          tag(_weakReferree, "_weakReferree");

        _phantomRef = new PhantomReference<Object>(_phantomReferree = new Object(), null);
        tag(_phantomRef, "_phantomRef");    tag(_phantomReferree, "_phantomReferree");

        _thread = new Thread(); tag(_thread, "_thread");

        _classLoader = new java.net.URLClassLoader(new java.net.URL[] {});
        tag(_classLoader, "_classLoader");

        try {
            _loadedObject = _classLoader.loadClass("java/lang/SecurityManager");
            tag(_loadedObject, "_loadedObject");
        } catch ( ClassNotFoundException e ) {}

        _aw = new AlexWebb();      tag(_aw, "_aw");
        _bb = new BrunoBarbey();   tag(_bb, "_bb");

        // TODO:
        // - Security Manager,
        // - Signers
        //
        // Kinds of references left to the agent:
        // - stack references,
        // - monitor references,
        // - JNI local, weak global and global references
    }

    /** Set all reference fields to null */

    public void dropObjects() {

        _boolArr = null;
        _byteArr = null;
        _shortArr = null;
        _intArr = null;
        _longArr = null;
        _floatArr = null;
        _doubleArr = null;

        _objArrArr = null;

        // Multiple references to the same object
        _objArr = null;

        // Multiple references to myself
        _selfRef1 = _selfRef2 = _selfRef3 = null;

        // Circular linked list
        _nextRef = null;

        // Strings
        _strArr = null;

        // A proxy class created by reflection API
        _reflectClass = null;
        _cartierInAMirror = null;

        // Soft, Weak, Phantom References
        _softRef = null;
        _softReferree = null;
        _weakRef = null;
        _weakReferree = null;
        _phantomRef = null;
        _phantomReferree = null;

        _thread = null;

        _classLoader = null;
        _loadedObject = null;

        _aw = null;
        _bb = null;
    }

    private long _curTag;

    private void resetCurTag() {
        _curTag = 1; // 0 == no tag, so start from 1
        resetTags();
    }

    private void tag(Object o)
    {
        setTag(o, _curTag++, "(" + ((o == null) ? "null" : o.getClass().getCanonicalName()) + ")");
    }

    private void tag(Object o, String fieldName)
    {
        setTag(o, _curTag++, fieldName + "(" + ((o == null) ? "null" : o.getClass().getCanonicalName()) + ")");
    }

    // A shortcut
    private void ref(Object from, Object to, int jvmtiRefKind, int count)
    {
        addRefToVerify(from, to, jvmtiRefKind, count);
    }

    public static native void resetTags();
    public static native boolean setTag(Object o, long tag, String sInfo);
    public static native long getTag(Object o);

    public static native void resetRefsToVerify();
    public static native boolean addRefToVerify(Object from, Object to, int jvmtiRefKind, int count);
}
