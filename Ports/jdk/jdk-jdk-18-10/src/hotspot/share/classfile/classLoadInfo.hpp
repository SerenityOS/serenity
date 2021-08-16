/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADINFO_HPP
#define SHARE_CLASSFILE_CLASSLOADINFO_HPP

#include "runtime/handles.hpp"

class InstanceKlass;

template <typename T> class GrowableArray;

class ClassInstanceInfo : public StackObj {
 private:
  InstanceKlass* _dynamic_nest_host;
  Handle _class_data;

 public:
  ClassInstanceInfo() {
    _dynamic_nest_host = NULL;
    _class_data = Handle();
  }
  ClassInstanceInfo(InstanceKlass* dynamic_nest_host, Handle class_data) {
    _dynamic_nest_host = dynamic_nest_host;
    _class_data = class_data;
  }

  InstanceKlass* dynamic_nest_host() const { return _dynamic_nest_host; }
  Handle class_data() const { return _class_data; }
  friend class ClassLoadInfo;
};

class ClassLoadInfo : public StackObj {
 private:
  Handle                 _protection_domain;
  ClassInstanceInfo      _class_hidden_info;
  bool                   _is_hidden;
  bool                   _is_strong_hidden;
  bool                   _can_access_vm_annotations;

 public:
  ClassLoadInfo(Handle protection_domain) {
    _protection_domain = protection_domain;
    _class_hidden_info._dynamic_nest_host = NULL;
    _class_hidden_info._class_data = Handle();
    _is_hidden = false;
    _is_strong_hidden = false;
    _can_access_vm_annotations = false;
  }

  ClassLoadInfo(Handle protection_domain, InstanceKlass* dynamic_nest_host,
                Handle class_data, bool is_hidden, bool is_strong_hidden,
                bool can_access_vm_annotations) {
    _protection_domain = protection_domain;
    _class_hidden_info._dynamic_nest_host = dynamic_nest_host;
    _class_hidden_info._class_data = class_data;
    _is_hidden = is_hidden;
    _is_strong_hidden = is_strong_hidden;
    _can_access_vm_annotations = can_access_vm_annotations;
  }

  Handle protection_domain()             const { return _protection_domain; }
  const ClassInstanceInfo* class_hidden_info_ptr() const { return &_class_hidden_info; }
  bool is_hidden()                       const { return _is_hidden; }
  bool is_strong_hidden()                const { return _is_strong_hidden; }
  bool can_access_vm_annotations()       const { return _can_access_vm_annotations; }
};

#endif // SHARE_CLASSFILE_CLASSLOADINFO_HPP
