/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_VMSTRUCTS_HPP
#define SHARE_RUNTIME_VMSTRUCTS_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif

// This table encapsulates the debugging information required by the
// serviceability agent in order to run. Specifically, we need to
// understand the layout of certain C data structures (offsets, in
// bytes, of their fields.)
//
// There are alternatives for the design of this mechanism, including
// parsing platform-specific debugging symbols from a debug build into
// a program database. While this current mechanism can be considered
// to be a workaround for the inability to debug arbitrary C and C++
// programs at the present time, it does have certain advantages.
// First, it is platform-independent, which will vastly simplify the
// initial bringup of the system both now and on future platforms.
// Second, it is embedded within the VM, as opposed to being in a
// separate program database; experience has shown that whenever
// portions of a system are decoupled, version skew is problematic.
// Third, generating a program database, for example for a product
// build, would probably require two builds to be done: the desired
// product build as well as an intermediary build with the PRODUCT
// flag turned on but also compiled with -g, leading to a doubling of
// the time required to get a serviceability agent-debuggable product
// build. Fourth, and very significantly, this table probably
// preserves more information about field types than stabs do; for
// example, it preserves the fact that a field is a "jlong" rather
// than transforming the type according to the typedef in jni_md.h,
// which allows the Java-side code to identify "Java-sized" fields in
// C++ data structures. If the symbol parsing mechanism was redone
// using stabs, it might still be necessary to have a table somewhere
// containing this information.
//
// Do not change the sizes or signedness of the integer values in
// these data structures; they are fixed over in the serviceability
// agent's Java code (for bootstrapping).

typedef struct {
  const char* typeName;            // The type name containing the given field (example: "Klass")
  const char* fieldName;           // The field name within the type           (example: "_name")
  const char* typeString;          // Quoted name of the type of this field (example: "Symbol*";
                                   // parsed in Java to ensure type correctness
  int32_t  isStatic;               // Indicates whether following field is an offset or an address
  uint64_t offset;                 // Offset of field within structure; only used for nonstatic fields
  void* address;                   // Address of field; only used for static fields
                                   // ("offset" can not be reused because of apparent solstudio compiler bug
                                   // in generation of initializer data)
} VMStructEntry;

typedef struct {
  const char* typeName;            // Type name (example: "Method")
  const char* superclassName;      // Superclass name, or null if none (example: "oopDesc")
  int32_t isOopType;               // Does this type represent an oop typedef? (i.e., "Method*" or
                                   // "Klass*", but NOT "Method")
  int32_t isIntegerType;           // Does this type represent an integer type (of arbitrary size)?
  int32_t isUnsigned;              // If so, is it unsigned?
  uint64_t size;                   // Size, in bytes, of the type
} VMTypeEntry;

typedef struct {
  const char* name;                // Name of constant (example: "_thread_in_native")
  int32_t value;                   // Value of constant
} VMIntConstantEntry;

typedef struct {
  const char* name;                // Name of constant (example: "_thread_in_native")
  uint64_t value;                  // Value of constant
} VMLongConstantEntry;

typedef struct {
  const char* name;                // Name of address (example: "SharedRuntime::register_finalizer")
  void* value;                     // Value of address
} VMAddressEntry;

// This class is a friend of most classes, to be able to access
// private fields
class VMStructs {
public:
  // The last entry is identified over in the serviceability agent by
  // the fact that it has a NULL fieldName
  static VMStructEntry localHotSpotVMStructs[];
  // The function to get localHotSpotVMStructs length
  static size_t localHotSpotVMStructsLength() NOT_VM_STRUCTS_RETURN_(0);

  // The last entry is identified over in the serviceability agent by
  // the fact that it has a NULL typeName
  static VMTypeEntry   localHotSpotVMTypes[];
  // The function to get localHotSpotVMTypes length
  static size_t localHotSpotVMTypesLength() NOT_VM_STRUCTS_RETURN_(0);

  // Table of integer constants required by the serviceability agent.
  // The last entry is identified over in the serviceability agent by
  // the fact that it has a NULL typeName
  static VMIntConstantEntry localHotSpotVMIntConstants[];
  // The function to get localHotSpotVMIntConstants length
  static size_t localHotSpotVMIntConstantsLength() NOT_VM_STRUCTS_RETURN_(0);

  // Table of long constants required by the serviceability agent.
  // The last entry is identified over in the serviceability agent by
  // the fact that it has a NULL typeName
  static VMLongConstantEntry localHotSpotVMLongConstants[];
  // The function to get localHotSpotVMIntConstants length
  static size_t localHotSpotVMLongConstantsLength() NOT_VM_STRUCTS_RETURN_(0);

  /**
   * Table of addresses.
   */
  static VMAddressEntry localHotSpotVMAddresses[];

#ifdef ASSERT
  // This is used to run any checking code necessary for validation of
  // the data structure (debug build only)
  static void init() NOT_VM_STRUCTS_RETURN;

private:
  // Look up a type in localHotSpotVMTypes using strcmp() (debug build only).
  // Returns 1 if found, 0 if not.
  static int findType(const char* typeName) NOT_VM_STRUCTS_RETURN_(0);
#endif // ASSERT
};

// This utility macro quotes the passed string
#define QUOTE(x) #x

//--------------------------------------------------------------------------------
// VMStructEntry macros
//

// This macro generates a VMStructEntry line for a nonstatic field
#define GENERATE_NONSTATIC_VM_STRUCT_ENTRY(typeName, fieldName, type)              \
 { QUOTE(typeName), QUOTE(fieldName), QUOTE(type), 0, offset_of(typeName, fieldName), NULL },

// This macro generates a VMStructEntry line for a static field
#define GENERATE_STATIC_VM_STRUCT_ENTRY(typeName, fieldName, type)                 \
 { QUOTE(typeName), QUOTE(fieldName), QUOTE(type), 1, 0, &typeName::fieldName },

// This macro generates a VMStructEntry line for a static pointer volatile field,
// e.g.: "static ObjectMonitor * volatile g_block_list;"
#define GENERATE_STATIC_PTR_VOLATILE_VM_STRUCT_ENTRY(typeName, fieldName, type)    \
 { QUOTE(typeName), QUOTE(fieldName), QUOTE(type), 1, 0, (void *)&typeName::fieldName },

// This macro generates a VMStructEntry line for an unchecked
// nonstatic field, in which the size of the type is also specified.
// The type string is given as NULL, indicating an "opaque" type.
#define GENERATE_UNCHECKED_NONSTATIC_VM_STRUCT_ENTRY(typeName, fieldName, size)    \
  { QUOTE(typeName), QUOTE(fieldName), NULL, 0, offset_of(typeName, fieldName), NULL },

// This macro generates a VMStructEntry line for an unchecked
// static field, in which the size of the type is also specified.
// The type string is given as NULL, indicating an "opaque" type.
#define GENERATE_UNCHECKED_STATIC_VM_STRUCT_ENTRY(typeName, fieldName, size)       \
 { QUOTE(typeName), QUOTE(fieldName), NULL, 1, 0, (void*) &typeName::fieldName },

// This macro generates the sentinel value indicating the end of the list
#define GENERATE_VM_STRUCT_LAST_ENTRY() \
 { NULL, NULL, NULL, 0, 0, NULL }


#ifdef ASSERT

// This macro checks the type of a VMStructEntry by comparing pointer types
#define CHECK_NONSTATIC_VM_STRUCT_ENTRY(typeName, fieldName, type)                 \
 {typeName *dummyObj = NULL; type* dummy = &dummyObj->fieldName;                   \
  assert(offset_of(typeName, fieldName) < sizeof(typeName), "Illegal nonstatic struct entry, field offset too large"); }

// This macro checks the type of a volatile VMStructEntry by comparing pointer types
#define CHECK_VOLATILE_NONSTATIC_VM_STRUCT_ENTRY(typeName, fieldName, type)        \
 {typedef type dummyvtype; typeName *dummyObj = NULL; volatile dummyvtype* dummy = &dummyObj->fieldName; }

// This macro checks the type of a static VMStructEntry by comparing pointer types
#define CHECK_STATIC_VM_STRUCT_ENTRY(typeName, fieldName, type)                    \
 {type* dummy = &typeName::fieldName; }

// This macro checks the type of a static pointer volatile VMStructEntry by comparing pointer types,
// e.g.: "static ObjectMonitor * volatile g_block_list;"
#define CHECK_STATIC_PTR_VOLATILE_VM_STRUCT_ENTRY(typeName, fieldName, type)       \
 {type volatile * dummy = &typeName::fieldName; }

// This macro ensures the type of a field and its containing type are
// present in the type table. The assertion string is shorter than
// preferable because (incredibly) of a bug in Solstice NFS client
// which seems to prevent very long lines from compiling. This assertion
// means that an entry in VMStructs::localHotSpotVMStructs[] was not
// found in VMStructs::localHotSpotVMTypes[].
#define ENSURE_FIELD_TYPE_PRESENT(typeName, fieldName, type)                       \
 { assert(findType(QUOTE(typeName)) != 0, "type \"" QUOTE(typeName) "\" not found in type table"); \
   assert(findType(QUOTE(type)) != 0, "type \"" QUOTE(type) "\" not found in type table"); }

// This is a no-op macro for unchecked fields
#define CHECK_NO_OP(a, b, c)

#endif // ASSERT

//--------------------------------------------------------------------------------
// VMTypeEntry macros
//

#define GENERATE_VM_TYPE_ENTRY(type, superclass) \
 { QUOTE(type), QUOTE(superclass), 0, 0, 0, sizeof(type) },

#define GENERATE_TOPLEVEL_VM_TYPE_ENTRY(type) \
 { QUOTE(type), NULL,              0, 0, 0, sizeof(type) },

#define GENERATE_OOP_VM_TYPE_ENTRY(type) \
 { QUOTE(type), NULL,              1, 0, 0, sizeof(type) },

#define GENERATE_INTEGER_VM_TYPE_ENTRY(type) \
 { QUOTE(type), NULL,              0, 1, 0, sizeof(type) },

#define GENERATE_UNSIGNED_INTEGER_VM_TYPE_ENTRY(type) \
 { QUOTE(type), NULL,              0, 1, 1, sizeof(type) },

#define GENERATE_VM_TYPE_LAST_ENTRY() \
 { NULL, NULL, 0, 0, 0, 0 }

#define CHECK_VM_TYPE_ENTRY(type, superclass) \
 { type* dummyObj = NULL; superclass* dummySuperObj = dummyObj; }

#define CHECK_VM_TYPE_NO_OP(a)
#define CHECK_SINGLE_ARG_VM_TYPE_NO_OP(a)


//--------------------------------------------------------------------------------
// VMIntConstantEntry macros
//

#define GENERATE_VM_INT_CONSTANT_ENTRY(name) \
 { QUOTE(name), (int32_t) name },

#define GENERATE_VM_INT_CONSTANT_WITH_VALUE_ENTRY(name, value) \
 { (name), (int32_t)(value) },

#define GENERATE_PREPROCESSOR_VM_INT_CONSTANT_ENTRY(name, value) \
 { name, (int32_t) value },

// This macro generates the sentinel value indicating the end of the list
#define GENERATE_VM_INT_CONSTANT_LAST_ENTRY() \
 { NULL, 0 }


//--------------------------------------------------------------------------------
// VMLongConstantEntry macros
//

#define GENERATE_VM_LONG_CONSTANT_ENTRY(name) \
  { QUOTE(name), name },

#define GENERATE_PREPROCESSOR_VM_LONG_CONSTANT_ENTRY(name, value) \
  { name, value },

// This macro generates the sentinel value indicating the end of the list
#define GENERATE_VM_LONG_CONSTANT_LAST_ENTRY() \
 { NULL, 0 }


//--------------------------------------------------------------------------------
// VMAddressEntry macros
//

#define GENERATE_VM_ADDRESS_ENTRY(name) \
  { QUOTE(name), (void*) (name) },

#define GENERATE_PREPROCESSOR_VM_ADDRESS_ENTRY(name, value) \
  { name, (void*) (value) },

#define GENERATE_VM_FUNCTION_ENTRY(name) \
  { QUOTE(name), CAST_FROM_FN_PTR(void*, &(name)) },

// This macro generates the sentinel value indicating the end of the list
#define GENERATE_VM_ADDRESS_LAST_ENTRY() \
 { NULL, NULL }

#endif // SHARE_RUNTIME_VMSTRUCTS_HPP
