/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_ACCESSFLAGS_HPP
#define SHARE_UTILITIES_ACCESSFLAGS_HPP

#include "jvm_constants.h"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// AccessFlags is an abstraction over Java access flags.

class outputStream;

enum {
  // See jvm.h for shared JVM_ACC_XXX access flags

  // HotSpot-specific access flags

  // flags actually put in .class file
  JVM_ACC_WRITTEN_FLAGS           = 0x00007FFF,

  // Method* flags
  JVM_ACC_MONITOR_MATCH           = 0x10000000,     // True if we know that monitorenter/monitorexit bytecodes match
  JVM_ACC_HAS_MONITOR_BYTECODES   = 0x20000000,     // Method contains monitorenter/monitorexit bytecodes
  JVM_ACC_HAS_LOOPS               = 0x40000000,     // Method has loops
  JVM_ACC_LOOPS_FLAG_INIT         = (int)0x80000000,// The loop flag has been initialized
  JVM_ACC_QUEUED                  = 0x01000000,     // Queued for compilation
  JVM_ACC_NOT_C2_COMPILABLE       = 0x02000000,
  JVM_ACC_NOT_C1_COMPILABLE       = 0x04000000,
  JVM_ACC_NOT_C2_OSR_COMPILABLE   = 0x08000000,
  JVM_ACC_HAS_LINE_NUMBER_TABLE   = 0x00100000,
  JVM_ACC_HAS_CHECKED_EXCEPTIONS  = 0x00400000,
  JVM_ACC_HAS_JSRS                = 0x00800000,
  JVM_ACC_IS_OLD                  = 0x00010000,     // RedefineClasses() has replaced this method
  JVM_ACC_IS_OBSOLETE             = 0x00020000,     // RedefineClasses() has made method obsolete
  JVM_ACC_IS_PREFIXED_NATIVE      = 0x00040000,     // JVMTI has prefixed this native method
  JVM_ACC_ON_STACK                = 0x00080000,     // RedefineClasses() was used on the stack
  JVM_ACC_IS_DELETED              = 0x00008000,     // RedefineClasses() has deleted this method

  // Klass* flags
  JVM_ACC_HAS_MIRANDA_METHODS     = 0x10000000,     // True if this class has miranda methods in it's vtable
  JVM_ACC_HAS_VANILLA_CONSTRUCTOR = 0x20000000,     // True if klass has a vanilla default constructor
  JVM_ACC_HAS_FINALIZER           = 0x40000000,     // True if klass has a non-empty finalize() method
  JVM_ACC_IS_CLONEABLE_FAST       = (int)0x80000000,// True if klass implements the Cloneable interface and can be optimized in generated code
  JVM_ACC_HAS_FINAL_METHOD        = 0x01000000,     // True if klass has final method
  JVM_ACC_IS_SHARED_CLASS         = 0x02000000,     // True if klass is shared
  JVM_ACC_IS_HIDDEN_CLASS         = 0x04000000,     // True if klass is hidden
  JVM_ACC_IS_VALUE_BASED_CLASS    = 0x08000000,     // True if klass is marked as a ValueBased class

  // Klass* and Method* flags
  JVM_ACC_HAS_LOCAL_VARIABLE_TABLE= 0x00200000,

  JVM_ACC_PROMOTED_FLAGS          = 0x00200000,     // flags promoted from methods to the holding klass

  // field flags
  // Note: these flags must be defined in the low order 16 bits because
  // InstanceKlass only stores a ushort worth of information from the
  // AccessFlags value.
  // These bits must not conflict with any other field-related access flags
  // (e.g., ACC_ENUM).
  // Note that the class-related ACC_ANNOTATION bit conflicts with these flags.
  JVM_ACC_FIELD_ACCESS_WATCHED            = 0x00002000, // field access is watched by JVMTI
  JVM_ACC_FIELD_MODIFICATION_WATCHED      = 0x00008000, // field modification is watched by JVMTI
  JVM_ACC_FIELD_INTERNAL                  = 0x00000400, // internal field, same as JVM_ACC_ABSTRACT
  JVM_ACC_FIELD_STABLE                    = 0x00000020, // @Stable field, same as JVM_ACC_SYNCHRONIZED and JVM_ACC_SUPER
  JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE  = 0x00000100, // (static) final field updated outside (class) initializer, same as JVM_ACC_NATIVE
  JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE     = 0x00000800, // field has generic signature

  JVM_ACC_FIELD_INTERNAL_FLAGS       = JVM_ACC_FIELD_ACCESS_WATCHED |
                                       JVM_ACC_FIELD_MODIFICATION_WATCHED |
                                       JVM_ACC_FIELD_INTERNAL |
                                       JVM_ACC_FIELD_STABLE |
                                       JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE,

                                                    // flags accepted by set_field_flags()
  JVM_ACC_FIELD_FLAGS                = JVM_RECOGNIZED_FIELD_MODIFIERS | JVM_ACC_FIELD_INTERNAL_FLAGS

};


class AccessFlags {
  friend class VMStructs;
 private:
  jint _flags;

 public:
  AccessFlags() : _flags(0) {}
  explicit AccessFlags(jint flags) : _flags(flags) {}

  // Java access flags
  bool is_public      () const         { return (_flags & JVM_ACC_PUBLIC      ) != 0; }
  bool is_private     () const         { return (_flags & JVM_ACC_PRIVATE     ) != 0; }
  bool is_protected   () const         { return (_flags & JVM_ACC_PROTECTED   ) != 0; }
  bool is_static      () const         { return (_flags & JVM_ACC_STATIC      ) != 0; }
  bool is_final       () const         { return (_flags & JVM_ACC_FINAL       ) != 0; }
  bool is_synchronized() const         { return (_flags & JVM_ACC_SYNCHRONIZED) != 0; }
  bool is_super       () const         { return (_flags & JVM_ACC_SUPER       ) != 0; }
  bool is_volatile    () const         { return (_flags & JVM_ACC_VOLATILE    ) != 0; }
  bool is_transient   () const         { return (_flags & JVM_ACC_TRANSIENT   ) != 0; }
  bool is_native      () const         { return (_flags & JVM_ACC_NATIVE      ) != 0; }
  bool is_interface   () const         { return (_flags & JVM_ACC_INTERFACE   ) != 0; }
  bool is_abstract    () const         { return (_flags & JVM_ACC_ABSTRACT    ) != 0; }

  // Attribute flags
  bool is_synthetic   () const         { return (_flags & JVM_ACC_SYNTHETIC   ) != 0; }

  // Method* flags
  bool is_monitor_matching     () const { return (_flags & JVM_ACC_MONITOR_MATCH          ) != 0; }
  bool has_monitor_bytecodes   () const { return (_flags & JVM_ACC_HAS_MONITOR_BYTECODES  ) != 0; }
  bool has_loops               () const { return (_flags & JVM_ACC_HAS_LOOPS              ) != 0; }
  bool loops_flag_init         () const { return (_flags & JVM_ACC_LOOPS_FLAG_INIT        ) != 0; }
  bool queued_for_compilation  () const { return (_flags & JVM_ACC_QUEUED                 ) != 0; }
  bool is_not_c1_compilable    () const { return (_flags & JVM_ACC_NOT_C1_COMPILABLE      ) != 0; }
  bool is_not_c2_compilable    () const { return (_flags & JVM_ACC_NOT_C2_COMPILABLE      ) != 0; }
  bool is_not_c2_osr_compilable() const { return (_flags & JVM_ACC_NOT_C2_OSR_COMPILABLE  ) != 0; }
  bool has_linenumber_table    () const { return (_flags & JVM_ACC_HAS_LINE_NUMBER_TABLE  ) != 0; }
  bool has_checked_exceptions  () const { return (_flags & JVM_ACC_HAS_CHECKED_EXCEPTIONS ) != 0; }
  bool has_jsrs                () const { return (_flags & JVM_ACC_HAS_JSRS               ) != 0; }
  bool is_old                  () const { return (_flags & JVM_ACC_IS_OLD                 ) != 0; }
  bool is_obsolete             () const { return (_flags & JVM_ACC_IS_OBSOLETE            ) != 0; }
  bool is_deleted              () const { return (_flags & JVM_ACC_IS_DELETED             ) != 0; }
  bool is_prefixed_native      () const { return (_flags & JVM_ACC_IS_PREFIXED_NATIVE     ) != 0; }

  // Klass* flags
  bool has_miranda_methods     () const { return (_flags & JVM_ACC_HAS_MIRANDA_METHODS    ) != 0; }
  bool has_vanilla_constructor () const { return (_flags & JVM_ACC_HAS_VANILLA_CONSTRUCTOR) != 0; }
  bool has_finalizer           () const { return (_flags & JVM_ACC_HAS_FINALIZER          ) != 0; }
  bool has_final_method        () const { return (_flags & JVM_ACC_HAS_FINAL_METHOD       ) != 0; }
  bool is_cloneable_fast       () const { return (_flags & JVM_ACC_IS_CLONEABLE_FAST      ) != 0; }
  bool is_shared_class         () const { return (_flags & JVM_ACC_IS_SHARED_CLASS        ) != 0; }
  bool is_hidden_class         () const { return (_flags & JVM_ACC_IS_HIDDEN_CLASS        ) != 0; }
  bool is_value_based_class    () const { return (_flags & JVM_ACC_IS_VALUE_BASED_CLASS   ) != 0; }

  // Klass* and Method* flags
  bool has_localvariable_table () const { return (_flags & JVM_ACC_HAS_LOCAL_VARIABLE_TABLE) != 0; }
  void set_has_localvariable_table()    { atomic_set_bits(JVM_ACC_HAS_LOCAL_VARIABLE_TABLE); }
  void clear_has_localvariable_table()  { atomic_clear_bits(JVM_ACC_HAS_LOCAL_VARIABLE_TABLE); }

  // field flags
  bool is_field_access_watched() const  { return (_flags & JVM_ACC_FIELD_ACCESS_WATCHED) != 0; }
  bool is_field_modification_watched() const
                                        { return (_flags & JVM_ACC_FIELD_MODIFICATION_WATCHED) != 0; }
  bool has_field_initialized_final_update() const
                                        { return (_flags & JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE) != 0; }
  bool on_stack() const                 { return (_flags & JVM_ACC_ON_STACK) != 0; }
  bool is_internal() const              { return (_flags & JVM_ACC_FIELD_INTERNAL) != 0; }
  bool is_stable() const                { return (_flags & JVM_ACC_FIELD_STABLE) != 0; }
  bool field_has_generic_signature() const
                                        { return (_flags & JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0; }

  // get .class file flags
  jint get_flags               () const { return (_flags & JVM_ACC_WRITTEN_FLAGS); }

  // Initialization
  void add_promoted_flags(jint flags)   { _flags |= (flags & JVM_ACC_PROMOTED_FLAGS); }
  void set_field_flags(jint flags)      {
    assert((flags & JVM_ACC_FIELD_FLAGS) == flags, "only recognized flags");
    _flags = (flags & JVM_ACC_FIELD_FLAGS);
  }
  void set_flags(jint flags)            { _flags = (flags & JVM_ACC_WRITTEN_FLAGS); }

  void set_queued_for_compilation()    { atomic_set_bits(JVM_ACC_QUEUED); }
  void clear_queued_for_compilation()  { atomic_clear_bits(JVM_ACC_QUEUED); }

  // Atomic update of flags
  void atomic_set_bits(jint bits);
  void atomic_clear_bits(jint bits);

 private:
  friend class Method;
  friend class Klass;
  friend class ClassFileParser;
  // the functions below should only be called on the _access_flags inst var directly,
  // otherwise they are just changing a copy of the flags

  // attribute flags
  void set_is_synthetic()              { atomic_set_bits(JVM_ACC_SYNTHETIC);               }

  // Method* flags
  void set_monitor_matching()          { atomic_set_bits(JVM_ACC_MONITOR_MATCH);           }
  void set_has_monitor_bytecodes()     { atomic_set_bits(JVM_ACC_HAS_MONITOR_BYTECODES);   }
  void set_has_loops()                 { atomic_set_bits(JVM_ACC_HAS_LOOPS);               }
  void set_loops_flag_init()           { atomic_set_bits(JVM_ACC_LOOPS_FLAG_INIT);         }
  void set_not_c1_compilable()         { atomic_set_bits(JVM_ACC_NOT_C1_COMPILABLE);       }
  void set_not_c2_compilable()         { atomic_set_bits(JVM_ACC_NOT_C2_COMPILABLE);       }
  void set_not_c2_osr_compilable()     { atomic_set_bits(JVM_ACC_NOT_C2_OSR_COMPILABLE);   }
  void set_has_linenumber_table()      { atomic_set_bits(JVM_ACC_HAS_LINE_NUMBER_TABLE);   }
  void set_has_checked_exceptions()    { atomic_set_bits(JVM_ACC_HAS_CHECKED_EXCEPTIONS);  }
  void set_has_jsrs()                  { atomic_set_bits(JVM_ACC_HAS_JSRS);                }
  void set_is_old()                    { atomic_set_bits(JVM_ACC_IS_OLD);                  }
  void set_is_obsolete()               { atomic_set_bits(JVM_ACC_IS_OBSOLETE);             }
  void set_is_deleted()                { atomic_set_bits(JVM_ACC_IS_DELETED);              }
  void set_is_prefixed_native()        { atomic_set_bits(JVM_ACC_IS_PREFIXED_NATIVE);      }

  void clear_not_c1_compilable()       { atomic_clear_bits(JVM_ACC_NOT_C1_COMPILABLE);       }
  void clear_not_c2_compilable()       { atomic_clear_bits(JVM_ACC_NOT_C2_COMPILABLE);       }
  void clear_not_c2_osr_compilable()   { atomic_clear_bits(JVM_ACC_NOT_C2_OSR_COMPILABLE);   }
  // Klass* flags
  void set_has_vanilla_constructor()   { atomic_set_bits(JVM_ACC_HAS_VANILLA_CONSTRUCTOR); }
  void set_has_finalizer()             { atomic_set_bits(JVM_ACC_HAS_FINALIZER);           }
  void set_has_final_method()          { atomic_set_bits(JVM_ACC_HAS_FINAL_METHOD);        }
  void set_is_cloneable_fast()         { atomic_set_bits(JVM_ACC_IS_CLONEABLE_FAST);       }
  void set_has_miranda_methods()       { atomic_set_bits(JVM_ACC_HAS_MIRANDA_METHODS);     }
  void set_is_shared_class()           { atomic_set_bits(JVM_ACC_IS_SHARED_CLASS);         }
  void set_is_hidden_class()           { atomic_set_bits(JVM_ACC_IS_HIDDEN_CLASS);         }
  void set_is_value_based_class()      { atomic_set_bits(JVM_ACC_IS_VALUE_BASED_CLASS);    }

 public:
  // field flags
  void set_is_field_access_watched(const bool value)
                                       {
                                         if (value) {
                                           atomic_set_bits(JVM_ACC_FIELD_ACCESS_WATCHED);
                                         } else {
                                           atomic_clear_bits(JVM_ACC_FIELD_ACCESS_WATCHED);
                                         }
                                       }
  void set_is_field_modification_watched(const bool value)
                                       {
                                         if (value) {
                                           atomic_set_bits(JVM_ACC_FIELD_MODIFICATION_WATCHED);
                                         } else {
                                           atomic_clear_bits(JVM_ACC_FIELD_MODIFICATION_WATCHED);
                                         }
                                       }

  void set_has_field_initialized_final_update(const bool value) {
    if (value) {
      atomic_set_bits(JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE);
    } else {
      atomic_clear_bits(JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE);
    }
  }

  void set_field_has_generic_signature()
                                       {
                                         atomic_set_bits(JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE);
                                       }

  void set_on_stack(const bool value)
                                       {
                                         if (value) {
                                           atomic_set_bits(JVM_ACC_ON_STACK);
                                         } else {
                                           atomic_clear_bits(JVM_ACC_ON_STACK);
                                         }
                                       }
  // Conversion
  jshort as_short() const              { return (jshort)_flags; }
  jint   as_int() const                { return _flags; }

  inline friend AccessFlags accessFlags_from(jint flags);

  // Printing/debugging
#if INCLUDE_JVMTI
  void print_on(outputStream* st) const;
#else
  void print_on(outputStream* st) const PRODUCT_RETURN;
#endif
};

inline AccessFlags accessFlags_from(jint flags) {
  AccessFlags af;
  af._flags = flags;
  return af;
}

#endif // SHARE_UTILITIES_ACCESSFLAGS_HPP
