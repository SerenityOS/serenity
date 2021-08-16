/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JVMCI_JVMCIOBJECT_HPP
#define SHARE_JVMCI_JVMCIOBJECT_HPP

#include "jni.h"
#include "utilities/debug.hpp"

class JVMCIArray;
class JVMCIPrimitiveArray;
class JVMCIObjectArray;

class JVMCIObject {

 private:
  jobject _object;
  bool _is_hotspot;

 public:
  JVMCIObject(): _object(NULL), _is_hotspot(false) {}
  JVMCIObject(jobject o, bool is_hotspot): _object(o), _is_hotspot(is_hotspot) { }

  static JVMCIObject create(jobject o, bool is_hotspot) { JVMCIObject r(o, is_hotspot); return r; }
  jobject as_jobject() { return _object; }
  jobject as_jweak()   { return (jweak) _object; }
  jstring as_jstring() { return (jstring) _object; }
  bool is_hotspot() { return _is_hotspot; }

  bool is_null() const { return _object == NULL; }
  bool is_non_null() const { return _object != NULL; }

  operator JVMCIArray();
  operator JVMCIPrimitiveArray();
  operator JVMCIObjectArray();
};

class JVMCIArray : public JVMCIObject {
 public:
  JVMCIArray() {}
  JVMCIArray(jobject o, bool is_hotspot): JVMCIObject(o, is_hotspot) {}
  jarray as_jobject() { return (jarray) JVMCIObject::as_jobject(); }
};

class JVMCIObjectArray : public JVMCIArray {
 public:
  JVMCIObjectArray() {}
  JVMCIObjectArray(void* v): JVMCIArray() { assert(v == NULL, "must be NULL"); }
  JVMCIObjectArray(jobject o, bool is_hotspot): JVMCIArray(o, is_hotspot) {}

  jobjectArray as_jobject() { return (jobjectArray) JVMCIArray::as_jobject(); }
};

class JVMCIPrimitiveArray : public JVMCIArray {
 public:
  JVMCIPrimitiveArray() {}
  JVMCIPrimitiveArray(void* v): JVMCIArray() { assert(v == NULL, "must be NULL"); }
  JVMCIPrimitiveArray(jobject o, bool is_hotspot): JVMCIArray(o, is_hotspot) {}

  jbooleanArray as_jbooleanArray() { return (jbooleanArray) as_jobject(); }
  jbyteArray    as_jbyteArray()    { return (jbyteArray) as_jobject();    }
  jcharArray    as_jcharArray()    { return (jcharArray) as_jobject();    }
  jshortArray   as_jshortArray()   { return (jshortArray) as_jobject();   }
  jintArray     as_jintArray()     { return (jintArray) as_jobject();     }
  jfloatArray   as_jfloatArray()   { return (jfloatArray) as_jobject();   }
  jlongArray    as_jlongArray()    { return (jlongArray) as_jobject();    }
  jdoubleArray  as_jdoubleArray()  { return (jdoubleArray) as_jobject();  }
};

inline JVMCIObject::operator JVMCIArray() { return JVMCIArray(_object, _is_hotspot); }
inline JVMCIObject::operator JVMCIPrimitiveArray() { return JVMCIPrimitiveArray(_object, _is_hotspot); }
inline JVMCIObject::operator JVMCIObjectArray() { return JVMCIObjectArray(_object, _is_hotspot); }

#endif // SHARE_JVMCI_JVMCIOBJECT_HPP
