/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#ifndef SHARE_RUNTIME_SIGNATURE_HPP
#define SHARE_RUNTIME_SIGNATURE_HPP

#include "memory/allocation.hpp"
#include "oops/method.hpp"


// Static routines and parsing loops for processing field and method
// descriptors.  In the HotSpot sources we call them "signatures".
//
// A SignatureStream iterates over a Java descriptor (or parts of it).
// The syntax is documented in the Java Virtual Machine Specification,
// section 4.3.
//
// The syntax may be summarized as follows:
//
//     MethodType: '(' {FieldType}* ')' (FieldType | 'V')
//     FieldType: PrimitiveType | ObjectType | ArrayType
//     PrimitiveType: 'B' | 'C' | 'D' | 'F' | 'I' | 'J' | 'S' | 'Z'
//     ObjectType: 'L' ClassName ';' | ArrayType
//     ArrayType: '[' FieldType
//     ClassName: {UnqualifiedName '/'}* UnqualifiedName
//     UnqualifiedName: NameChar {NameChar}*
//     NameChar: ANY_CHAR_EXCEPT('/' | '.' | ';' | '[')
//
// All of the concrete characters in the above grammar are given
// standard manifest constant names of the form JVM_SIGNATURE_x.
// Executable code uses these constant names in preference to raw
// character constants.  Comments and assertion code sometimes use
// the raw character constants for brevity.
//
// The primitive field types (like 'I') correspond 1-1 with type codes
// (like T_INT) which form part of the specification of the 'newarray'
// instruction (JVMS 6.5, section on newarray).  These type codes are
// widely used in the HotSpot code.  They are joined by ad hoc codes
// like T_OBJECT and T_ARRAY (defined in HotSpot but not in the JVMS)
// so that each "basic type" of field descriptor (or void return type)
// has a corresponding T_x code.  Thus, while T_x codes play a very
// minor role in the JVMS, they play a major role in the HotSpot
// sources.  There are fewer than 16 such "basic types", so they fit
// nicely into bitfields.
//
// The syntax of ClassName overlaps slightly with the descriptor
// syntaxes.  The strings "I" and "(I)V" are both class names
// *and* descriptors.  If a class name contains any character other
// than "BCDFIJSZ()V" it cannot be confused with a descriptor.
// Class names inside of descriptors are always contained in an
// "envelope" syntax which starts with 'L' and ends with ';'.
//
// As a confounding factor, array types report their type name strings
// in descriptor format.  These name strings are easy to recognize,
// since they begin with '['.  For this reason some API points on
// HotSpot look for array descriptors as well as proper class names.
//
// For historical reasons some API points that accept class names and
// array names also look for class names wrapped inside an envelope
// (like "LFoo;") and unwrap them on the fly (to a name like "Foo").

class Signature : AllStatic {
 private:
  static bool is_valid_array_signature(const Symbol* sig);

 public:

  // Returns the basic type of a field signature (or T_VOID for "V").
  // Assumes the signature is a valid field descriptor.
  // Do not apply this function to class names or method signatures.
  static BasicType basic_type(const Symbol* signature) {
    return basic_type(signature->char_at(0));
  }

  // Returns T_ILLEGAL for an illegal signature char.
  static BasicType basic_type(int ch);

  // Assuming it is either a class name or signature,
  // determine if it in fact cannot be a class name.
  // This means it either starts with '[' or ends with ';'
  static bool not_class_name(const Symbol* signature) {
    return (signature->starts_with(JVM_SIGNATURE_ARRAY) ||
            signature->ends_with(JVM_SIGNATURE_ENDCLASS));
  }

  // Assuming it is either a class name or signature,
  // determine if it in fact is an array descriptor.
  static bool is_array(const Symbol* signature) {
    return (signature->utf8_length() > 1 &&
            signature->char_at(0) == JVM_SIGNATURE_ARRAY &&
            is_valid_array_signature(signature));
  }

  // Assuming it is either a class name or signature,
  // determine if it contains a class name plus ';'.
  static bool has_envelope(const Symbol* signature) {
    return ((signature->utf8_length() > 0) &&
            signature->ends_with(JVM_SIGNATURE_ENDCLASS) &&
            has_envelope(signature->char_at(0)));
  }

  // Determine if this signature char introduces an
  // envelope, which is a class name plus ';'.
  static bool has_envelope(char signature_char) {
    return (signature_char == JVM_SIGNATURE_CLASS);
  }

  // Assuming has_envelope is true, return the symbol
  // inside the envelope, by stripping 'L' and ';'.
  // Caller is responsible for decrementing the newly created
  // Symbol's refcount, use TempNewSymbol.
  static Symbol* strip_envelope(const Symbol* signature);

  // Assuming it's either a field or method descriptor, determine
  // whether it is in fact a method descriptor:
  static bool is_method(const Symbol* signature) {
    return signature->starts_with(JVM_SIGNATURE_FUNC);
  }

  // Assuming it's a method signature, determine if it must
  // return void.
  static bool is_void_method(const Symbol* signature) {
    assert(is_method(signature), "signature is not for a method");
    return signature->ends_with(JVM_SIGNATURE_VOID);
  }
};

// A SignatureIterator uses a SignatureStream to produce BasicType
// results, discarding class names.  This means it can be accelerated
// using a fingerprint mechanism, in many cases, without loss of type
// information.  The FingerPrinter class computes and caches this
// reduced information for faster iteration.

class SignatureIterator: public ResourceObj {
 public:
  typedef uint64_t fingerprint_t;

 protected:
  Symbol*      _signature;             // the signature to iterate over
  BasicType    _return_type;
  fingerprint_t _fingerprint;

 public:
  // Definitions used in generating and iterating the
  // bit field form of the signature generated by the
  // Fingerprinter.
  enum {
    fp_static_feature_size    = 1,
    fp_is_static_bit          = 1,

    fp_result_feature_size    = 4,
    fp_result_feature_mask    = right_n_bits(fp_result_feature_size),
    fp_parameter_feature_size = 4,
    fp_parameter_feature_mask = right_n_bits(fp_parameter_feature_size),

    fp_parameters_done        = 0,  // marker for end of parameters (must be zero)

    // Parameters take up full wordsize, minus the result and static bit fields.
    // Since fp_parameters_done is zero, termination field arises from shifting
    // in zero bits, and therefore occupies no extra space.
    // The sentinel value is all-zero-bits, which is impossible for a true
    // fingerprint, since at least the result field will be non-zero.
    fp_max_size_of_parameters = ((BitsPerLong
                                  - (fp_result_feature_size + fp_static_feature_size))
                                 / fp_parameter_feature_size)
  };

  static bool fp_is_valid_type(BasicType type, bool for_return_type = false);

  // Sentinel values are zero and not-zero (-1).
  // No need to protect the sign bit, since every valid return type is non-zero
  // (even T_VOID), and there are no valid parameter fields which are 0xF (T_VOID).
  static fingerprint_t zero_fingerprint() { return (fingerprint_t)0; }
  static fingerprint_t overflow_fingerprint() { return ~(fingerprint_t)0; }
  static bool fp_is_valid(fingerprint_t fingerprint) {
    return (fingerprint != zero_fingerprint()) && (fingerprint != overflow_fingerprint());
  }

  // Constructors
  SignatureIterator(Symbol* signature, fingerprint_t fingerprint = zero_fingerprint()) {
    _signature   = signature;
    _return_type = T_ILLEGAL;  // sentinel value for uninitialized
    _fingerprint = zero_fingerprint();
    if (fingerprint != _fingerprint) {
      set_fingerprint(fingerprint);
    }
  }

  // If the fingerprint is present, we can use an accelerated loop.
  void set_fingerprint(fingerprint_t fingerprint);

  // Returns the set fingerprint, or zero_fingerprint()
  // if none has been set already.
  fingerprint_t fingerprint() const { return _fingerprint; }

  // Iteration
  // Hey look:  There are no virtual methods in this class.
  // So how is it customized?  By calling do_parameters_on
  // an object which answers to "do_type(BasicType)".
  // By convention, this object is in the subclass
  // itself, so the call is "do_parameters_on(this)".
  // The effect of this is to inline the parsing loop
  // everywhere "do_parameters_on" is called.
  // If there is a valid fingerprint in the object,
  // an improved loop is called which just unpacks the
  // bitfields from the fingerprint.  Otherwise, the
  // symbol is parsed.
  template<typename T> inline void do_parameters_on(T* callback); // iterates over parameters only
  BasicType return_type();  // computes the value on the fly if necessary

  static bool fp_is_static(fingerprint_t fingerprint) {
    assert(fp_is_valid(fingerprint), "invalid fingerprint");
    return fingerprint & fp_is_static_bit;
  }
  static BasicType fp_return_type(fingerprint_t fingerprint) {
    assert(fp_is_valid(fingerprint), "invalid fingerprint");
    return (BasicType) ((fingerprint >> fp_static_feature_size) & fp_result_feature_mask);
  }
  static fingerprint_t fp_start_parameters(fingerprint_t fingerprint) {
    assert(fp_is_valid(fingerprint), "invalid fingerprint");
    return fingerprint >> (fp_static_feature_size + fp_result_feature_size);
  }
  static BasicType fp_next_parameter(fingerprint_t& mask) {
    int result = (mask & fp_parameter_feature_mask);
    mask >>= fp_parameter_feature_size;
    return (BasicType) result;
  }
};


// Specialized SignatureIterators: Used to compute signature specific values.

class SignatureTypeNames : public SignatureIterator {
 protected:
  virtual void type_name(const char* name)   = 0;

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    switch (type) {
    case T_BOOLEAN: type_name("jboolean"); break;
    case T_CHAR:    type_name("jchar"   ); break;
    case T_FLOAT:   type_name("jfloat"  ); break;
    case T_DOUBLE:  type_name("jdouble" ); break;
    case T_BYTE:    type_name("jbyte"   ); break;
    case T_SHORT:   type_name("jshort"  ); break;
    case T_INT:     type_name("jint"    ); break;
    case T_LONG:    type_name("jlong"   ); break;
    case T_VOID:    type_name("void"    ); break;
    case T_ARRAY:
    case T_OBJECT:  type_name("jobject" ); break;
    default: ShouldNotReachHere();
    }
  }

 public:
  SignatureTypeNames(Symbol* signature) : SignatureIterator(signature) {}
};


// Specialized SignatureIterator: Used to compute the argument size.

class ArgumentSizeComputer: public SignatureIterator {
 private:
  int _size;
  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) { _size += parameter_type_word_count(type); }
 public:
  ArgumentSizeComputer(Symbol* signature);
  int size() { return _size; }
};


class ArgumentCount: public SignatureIterator {
 private:
  int _size;
  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) { _size++; }
 public:
  ArgumentCount(Symbol* signature);
  int size() { return _size; }
};


class ReferenceArgumentCount: public SignatureIterator {
 private:
  int _refs;
  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) { if (is_reference_type(type)) _refs++; }
 public:
  ReferenceArgumentCount(Symbol* signature);
  int count() { return _refs; }
};


// Specialized SignatureIterator: Used to compute the result type.

class ResultTypeFinder: public SignatureIterator {
 public:
  BasicType type() { return return_type(); }
  ResultTypeFinder(Symbol* signature) : SignatureIterator(signature) { }
};


// Fingerprinter computes a unique ID for a given method. The ID
// is a bitvector characterizing the methods signature (incl. the receiver).
class Fingerprinter: public SignatureIterator {
 private:
  fingerprint_t _accumulator;
  int _param_size;
  int _shift_count;
  const Method* _method;

  void initialize_accumulator() {
    _accumulator = 0;
    _shift_count = fp_result_feature_size + fp_static_feature_size;
    _param_size = 0;
  }

  // Out-of-line method does it all in constructor:
  void compute_fingerprint_and_return_type(bool static_flag = false);

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    assert(fp_is_valid_type(type), "bad parameter type");
    _accumulator |= ((fingerprint_t)type << _shift_count);
    _shift_count += fp_parameter_feature_size;
    _param_size += (is_double_word_type(type) ? 2 : 1);
  }

 public:
  int size_of_parameters() const { return _param_size; }
  // fingerprint() and return_type() are in super class

  Fingerprinter(const methodHandle& method)
    : SignatureIterator(method->signature()),
      _method(method()) {
    compute_fingerprint_and_return_type();
  }
  Fingerprinter(Symbol* signature, bool is_static)
    : SignatureIterator(signature),
      _method(NULL) {
    compute_fingerprint_and_return_type(is_static);
  }
};


// Specialized SignatureIterator: Used for native call purposes

class NativeSignatureIterator: public SignatureIterator {
 private:
  methodHandle _method;
// We need separate JNI and Java offset values because in 64 bit mode,
// the argument offsets are not in sync with the Java stack.
// For example a long takes up 1 "C" stack entry but 2 Java stack entries.
  int          _offset;                // The java stack offset
  int          _prepended;             // number of prepended JNI parameters (1 JNIEnv, plus 1 mirror if static)
  int          _jni_offset;            // the current parameter offset, starting with 0

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    switch (type) {
    case T_BYTE:
    case T_BOOLEAN:
      pass_byte();  _jni_offset++; _offset++;
      break;
    case T_CHAR:
    case T_SHORT:
      pass_short();  _jni_offset++; _offset++;
      break;
    case T_INT:
      pass_int();    _jni_offset++; _offset++;
      break;
    case T_FLOAT:
      pass_float();  _jni_offset++; _offset++;
      break;
    case T_DOUBLE: {
      int jni_offset = LP64_ONLY(1) NOT_LP64(2);
      pass_double(); _jni_offset += jni_offset; _offset += 2;
      break;
    }
    case T_LONG: {
      int jni_offset = LP64_ONLY(1) NOT_LP64(2);
      pass_long();   _jni_offset += jni_offset; _offset += 2;
      break;
    }
    case T_ARRAY:
    case T_OBJECT:
      pass_object(); _jni_offset++; _offset++;
      break;
    default:
      ShouldNotReachHere();
    }
  }

 public:
  methodHandle method() const          { return _method; }
  int          offset() const          { return _offset; }
  int      jni_offset() const          { return _jni_offset + _prepended; }
  bool      is_static() const          { return method()->is_static(); }
  virtual void pass_int()              = 0;
  virtual void pass_long()             = 0;
  virtual void pass_object()           = 0;  // objects, arrays, inlines
  virtual void pass_float()            = 0;
  virtual void pass_byte()             { pass_int(); };
  virtual void pass_short()            { pass_int(); };
#ifdef _LP64
  virtual void pass_double()           = 0;
#else
  virtual void pass_double()           { pass_long(); }  // may be same as long
#endif

  NativeSignatureIterator(const methodHandle& method) : SignatureIterator(method->signature()) {
    _method = method;
    _offset = 0;
    _jni_offset = 0;

    const int JNIEnv_words = 1;
    const int mirror_words = 1;
    _prepended = !is_static() ? JNIEnv_words : JNIEnv_words + mirror_words;
  }

  void iterate() { iterate(Fingerprinter(method()).fingerprint()); }

  // iterate() calls the 3 virtual methods according to the following invocation syntax:
  //
  // {pass_int | pass_long | pass_object}
  //
  // Arguments are handled from left to right (receiver first, if any).
  // The offset() values refer to the Java stack offsets but are 0 based and increasing.
  // The java_offset() values count down to 0, and refer to the Java TOS.
  // The jni_offset() values increase from 1 or 2, and refer to C arguments.
  // The method's return type is ignored.

  void iterate(fingerprint_t fingerprint) {
    set_fingerprint(fingerprint);
    if (!is_static()) {
      // handle receiver (not handled by iterate because not in signature)
      pass_object(); _jni_offset++; _offset++;
    }
    do_parameters_on(this);
  }
};


// This is the core parsing logic for iterating over signatures.
// All of the previous classes use this for doing their work.

class SignatureStream : public StackObj {
 private:
  const Symbol* _signature;
  int          _begin;
  int          _end;
  int          _limit;
  int          _array_prefix;  // count of '[' before the array element descr
  BasicType    _type;
  int          _state;
  Symbol*      _previous_name;    // cache the previously looked up symbol to avoid lookups
  GrowableArray<Symbol*>* _names; // symbols created while parsing that need to be dereferenced

  Symbol* find_symbol();

  enum { _s_field = 0, _s_method = 1, _s_method_return = 3 };
  void set_done() {
    _state |= -2;   // preserve s_method bit
    assert(is_done(), "Unable to set state to done");
  }
  int scan_type(BasicType bt);

 public:
  bool at_return_type() const                    { return _state == (int)_s_method_return; }
  bool is_done() const                           { return _state < 0; }
  void next();

  SignatureStream(const Symbol* signature, bool is_method = true);
  ~SignatureStream();

  bool is_reference() const { return is_reference_type(_type); }
  bool is_array() const     { return _type == T_ARRAY; }
  bool is_primitive() const { return is_java_primitive(_type); }
  BasicType type() const    { return _type; }

  const u1* raw_bytes() const  { return _signature->bytes() + _begin; }
  int       raw_length() const { return _end - _begin; }
  int raw_symbol_begin() const { return _begin + (has_envelope() ? 1 : 0); }
  int raw_symbol_end() const   { return _end  -  (has_envelope() ? 1 : 0); }
  char raw_char_at(int i) const {
    assert(i < _limit, "index for raw_char_at is over the limit");
    return _signature->char_at(i);
  }

  // True if there is an embedded class name in this type,
  // followed by ';'.
  bool has_envelope() const {
    if (!Signature::has_envelope(_signature->char_at(_begin)))
      return false;
    // this should always be true, but let's test it:
    assert(_signature->char_at(_end-1) == JVM_SIGNATURE_ENDCLASS, "signature envelope has no semi-colon at end");
    return true;
  }

  // return the symbol for chars in symbol_begin()..symbol_end()
  Symbol* as_symbol() {
    return find_symbol();
  }

  // in case you want only the return type:
  void skip_to_return_type();

  // number of '[' in array prefix
  int array_prefix_length() {
    return _type == T_ARRAY ? _array_prefix : 0;
  }

  // In case you want only the array base type,
  // reset the stream after skipping some brackets '['.
  // (The argument is clipped to array_prefix_length(),
  // and if it ends up as zero this call is a nop.
  // The default is value skips all brackets '['.)
 private:
  int skip_whole_array_prefix();
 public:
  int skip_array_prefix(int max_skip_length) {
    if (_type != T_ARRAY) {
      return 0;
    }
     if (_array_prefix > max_skip_length) {
      // strip some but not all levels of T_ARRAY
      _array_prefix -= max_skip_length;
      _begin += max_skip_length;
      return max_skip_length;
    }
    return skip_whole_array_prefix();
  }
  int skip_array_prefix() {
    if (_type != T_ARRAY) {
      return 0;
    }
    return skip_whole_array_prefix();
  }

  // free-standing lookups (bring your own CL/PD pair)
  enum FailureMode { ReturnNull, NCDFError, CachedOrNull };
  Klass* as_klass(Handle class_loader, Handle protection_domain, FailureMode failure_mode, TRAPS);
  oop as_java_mirror(Handle class_loader, Handle protection_domain, FailureMode failure_mode, TRAPS);
};

// Specialized SignatureStream: used for invoking SystemDictionary to either find
//                              or resolve the underlying type when iterating over a
//                              Java descriptor (or parts of it).
class ResolvingSignatureStream : public SignatureStream {
  Klass*       _load_origin;
  bool         _handles_cached;
  Handle       _class_loader;       // cached when needed
  Handle       _protection_domain;  // cached when needed

  void initialize_load_origin(Klass* load_origin) {
    _load_origin = load_origin;
    _handles_cached = (load_origin == NULL);
  }
  void need_handles() {
    if (!_handles_cached) {
      cache_handles();
      _handles_cached = true;
    }
  }
  void cache_handles();

 public:
  ResolvingSignatureStream(Symbol* signature, Klass* load_origin, bool is_method = true);
  ResolvingSignatureStream(Symbol* signature, Handle class_loader, Handle protection_domain, bool is_method = true);
  ResolvingSignatureStream(const Method* method);
  ResolvingSignatureStream(fieldDescriptor& field);

  Klass* load_origin()       { return _load_origin; }
  Handle class_loader()      { need_handles(); return _class_loader; }
  Handle protection_domain() { need_handles(); return _protection_domain; }

  Klass* as_klass_if_loaded(TRAPS);
  Klass* as_klass(FailureMode failure_mode, TRAPS) {
    need_handles();
    return SignatureStream::as_klass(_class_loader, _protection_domain,
                                     failure_mode, THREAD);
  }
  oop as_java_mirror(FailureMode failure_mode, TRAPS) {
    if (is_reference()) {
      need_handles();
    }
    return SignatureStream::as_java_mirror(_class_loader, _protection_domain,
                                           failure_mode, THREAD);
  }
};

// Here is how all the SignatureIterator classes invoke the
// SignatureStream engine to do their parsing.
template<typename T> inline
void SignatureIterator::do_parameters_on(T* callback) {
  fingerprint_t unaccumulator = _fingerprint;

  // Check for too many arguments, or missing fingerprint:
  if (!fp_is_valid(unaccumulator)) {
    SignatureStream ss(_signature);
    for (; !ss.at_return_type(); ss.next()) {
      callback->do_type(ss.type());
    }
    // while we are here, capture the return type
    _return_type = ss.type();
  } else {
    // Optimized version of do_parameters when fingerprint is known
    assert(_return_type != T_ILLEGAL, "return type already captured from fp");
    unaccumulator = fp_start_parameters(unaccumulator);
    for (BasicType type; (type = fp_next_parameter(unaccumulator)) != (BasicType)fp_parameters_done; ) {
      assert(fp_is_valid_type(type), "garbled fingerprint");
      callback->do_type(type);
    }
  }
}

 #ifdef ASSERT
 class SignatureVerifier : public StackObj {
  public:
    static bool is_valid_method_signature(Symbol* sig);
    static bool is_valid_type_signature(Symbol* sig);
  private:
    static ssize_t is_valid_type(const char*, ssize_t);
};
#endif
#endif // SHARE_RUNTIME_SIGNATURE_HPP
