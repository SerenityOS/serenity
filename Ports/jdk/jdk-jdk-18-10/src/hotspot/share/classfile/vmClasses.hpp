/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_VMCLASSES_HPP
#define SHARE_CLASSFILE_VMCLASSES_HPP

#include "classfile/vmClassID.hpp"
#include "memory/allStatic.hpp"
#include "runtime/handles.hpp"
#include "utilities/exceptions.hpp"

class ClassLoaderData;
class InstanceKlass;
class MetaspaceClosure;

class vmClasses : AllStatic {
  friend class VMStructs;

  static vmClassID check_id(vmClassID id) {
    assert((int)id >= (int)vmClassID::FIRST && (int)id < (int)vmClassID::LIMIT, "oob");
    return id;
  }

  static int as_int(vmClassID id) {
    return static_cast<int>(check_id(id));
  }

  static vmClassID as_id(int i) {
    vmClassID id = static_cast<vmClassID>(i);
    return check_id(id);
  }

  static InstanceKlass* check_klass(InstanceKlass* k) {
    assert(k != NULL, "klass not loaded");
    return k;
  }

  static bool is_loaded(InstanceKlass* klass);
  static bool resolve(vmClassID id, TRAPS);
  static void resolve_until(vmClassID limit_id, vmClassID &start_id, TRAPS);
  static void resolve_through(vmClassID last_id, vmClassID &start_id, TRAPS) {
    int limit = as_int(last_id) + 1;
    resolve_until(as_id(limit), start_id, THREAD);
  }

  static void resolve_shared_class(InstanceKlass* klass, ClassLoaderData* loader_data, Handle domain, TRAPS) NOT_CDS_RETURN;

#ifdef ASSERT
  static bool contain(Klass* k);
  static bool contain(Symbol* class_name);
#endif

  static InstanceKlass* _klasses[];

  // table of box klasses (int_klass, etc.)
  static InstanceKlass* _box_klasses[];

  // VM_CLASS_AT should be used by vmClasses.cpp and vmStructs.cpp only.
  #define VM_CLASS_AT(name) _klasses[static_cast<int>(VM_CLASS_ID(name))]

public:
  #define _VM_CLASS_DECLARE(name, symbol) \
    static InstanceKlass* name()         { return check_klass(VM_CLASS_AT(name)); }      \
    static InstanceKlass** name##_addr() { return &VM_CLASS_AT(name); }                  \
    static bool name##_is_loaded()       { return is_loaded(VM_CLASS_AT(name)); }
  VM_CLASSES_DO(_VM_CLASS_DECLARE);
  #undef _VM_CLASS_DECLARE

  static InstanceKlass* klass_at(vmClassID id) {
    return _klasses[as_int(id)];
  }

  static InstanceKlass** klass_addr_at(vmClassID id) {
    return &_klasses[as_int(id)];
  }

  static void metaspace_pointers_do(MetaspaceClosure* it);
  static void resolve_all(TRAPS);

  static BasicType box_klass_type(Klass* k);  // inverse of box_klass

  static InstanceKlass* box_klass(BasicType t) {
    assert((uint)t < T_VOID+1, "range check");
    return check_klass(_box_klasses[t]);
  }

  static bool Object_klass_loaded()         { return is_loaded(VM_CLASS_AT(Object_klass));             }
  static bool Class_klass_loaded()          { return is_loaded(VM_CLASS_AT(Class_klass));              }
  static bool Cloneable_klass_loaded()      { return is_loaded(VM_CLASS_AT(Cloneable_klass));          }
  static bool Parameter_klass_loaded()      { return is_loaded(VM_CLASS_AT(reflect_Parameter_klass));  }
  static bool ClassLoader_klass_loaded()    { return is_loaded(VM_CLASS_AT(ClassLoader_klass));        }
};

#endif // SHARE_CLASSFILE_VMCLASSES_HPP
