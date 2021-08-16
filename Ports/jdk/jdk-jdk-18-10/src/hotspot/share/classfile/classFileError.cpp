/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classFileParser.hpp"
#include "classfile/stackMapTable.hpp"
#include "classfile/verifier.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/resourceArea.hpp"

// Keep these in a separate file to prevent inlining

PRAGMA_DIAG_PUSH
PRAGMA_FORMAT_NONLITERAL_IGNORED

void ClassFileParser::classfile_parse_error(const char* msg, TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_ClassFormatError(),
                     msg, _class_name->as_C_string());
}

void ClassFileParser::classfile_parse_error(const char* msg,
                                            int index,
                                            TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_ClassFormatError(),
                     msg, index, _class_name->as_C_string());
}

void ClassFileParser::classfile_parse_error(const char* msg,
                                            const char* name,
                                            TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_ClassFormatError(),
                     msg, name, _class_name->as_C_string());
}

void ClassFileParser::classfile_parse_error(const char* msg,
                                            int index,
                                            const char* name,
                                            TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_ClassFormatError(),
                     msg, index, name, _class_name->as_C_string());
}

void ClassFileParser::classfile_parse_error(const char* msg,
                                            const char* name,
                                            const char* signature,
                                            TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_ClassFormatError(),
                     msg, name, signature, _class_name->as_C_string());
}

void ClassFileParser::classfile_icce_error(const char* msg,
                                           const Klass* k,
                                           TRAPS) const {
  assert(_class_name != NULL, "invariant");
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_IncompatibleClassChangeError(),
                     msg, _class_name->as_klass_external_name(), k->external_name());
}

void ClassFileParser::classfile_ucve_error(const char* msg,
                                           const Symbol* class_name,
                                           u2 major,
                                           u2 minor,
                                           TRAPS) const {
  ResourceMark rm(THREAD);
  Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_UnsupportedClassVersionError(),
                     msg, class_name->as_C_string(), major, minor);
}

PRAGMA_DIAG_POP

void StackMapStream::stackmap_format_error(const char* msg, TRAPS) {
  ResourceMark rm(THREAD);
  Exceptions::fthrow(
    THREAD_AND_LOCATION,
    vmSymbols::java_lang_ClassFormatError(),
    "StackMapTable format error: %s", msg
  );
}
