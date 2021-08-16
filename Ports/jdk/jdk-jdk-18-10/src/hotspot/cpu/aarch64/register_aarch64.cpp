/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#include "precompiled.hpp"
#include "register_aarch64.hpp"

const int ConcreteRegisterImpl::max_gpr = RegisterImpl::number_of_registers *
                                          RegisterImpl::max_slots_per_register;

const int ConcreteRegisterImpl::max_fpr
  = ConcreteRegisterImpl::max_gpr +
    FloatRegisterImpl::number_of_registers * FloatRegisterImpl::max_slots_per_register;

const int ConcreteRegisterImpl::max_pr
  = ConcreteRegisterImpl::max_fpr + PRegisterImpl::number_of_registers;

const char* RegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "c_rarg0", "c_rarg1", "c_rarg2", "c_rarg3", "c_rarg4", "c_rarg5", "c_rarg6", "c_rarg7",
    "rscratch1", "rscratch2",
    "r10", "r11", "r12", "r13", "r14", "r15", "r16",
    "r17", "r18_tls", "r19",
    "resp", "rdispatch", "rbcp", "r23", "rlocals", "rmonitors", "rcpool", "rheapbase",
    "rthread", "rfp", "lr", "sp"
  };
  return is_valid() ? names[encoding()] : "noreg";
}

const char* FloatRegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7",
    "v8", "v9", "v10", "v11", "v12", "v13", "v14", "v15",
    "v16", "v17", "v18", "v19", "v20", "v21", "v22", "v23",
    "v24", "v25", "v26", "v27", "v28", "v29", "v30", "v31"
  };
  return is_valid() ? names[encoding()] : "noreg";
}

const char* PRegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7",
    "p8", "p9", "p10", "p11", "p12", "p13", "p14", "p15"
  };
  return is_valid() ? names[encoding()] : "noreg";
}
