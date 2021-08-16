/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_BYTECODES_HPP
#define SHARE_INTERPRETER_BYTECODES_HPP

#include "memory/allocation.hpp"

// Bytecodes specifies all bytecodes used in the VM and
// provides utility functions to get bytecode attributes.

class Method;

// NOTE: replicated in SA in vm/agent/sun/jvm/hotspot/interpreter/Bytecodes.java
class Bytecodes: AllStatic {
 public:
  enum Code {
    _illegal              =  -1,

    // Java bytecodes
    _nop                  =   0, // 0x00
    _aconst_null          =   1, // 0x01
    _iconst_m1            =   2, // 0x02
    _iconst_0             =   3, // 0x03
    _iconst_1             =   4, // 0x04
    _iconst_2             =   5, // 0x05
    _iconst_3             =   6, // 0x06
    _iconst_4             =   7, // 0x07
    _iconst_5             =   8, // 0x08
    _lconst_0             =   9, // 0x09
    _lconst_1             =  10, // 0x0a
    _fconst_0             =  11, // 0x0b
    _fconst_1             =  12, // 0x0c
    _fconst_2             =  13, // 0x0d
    _dconst_0             =  14, // 0x0e
    _dconst_1             =  15, // 0x0f
    _bipush               =  16, // 0x10
    _sipush               =  17, // 0x11
    _ldc                  =  18, // 0x12
    _ldc_w                =  19, // 0x13
    _ldc2_w               =  20, // 0x14
    _iload                =  21, // 0x15
    _lload                =  22, // 0x16
    _fload                =  23, // 0x17
    _dload                =  24, // 0x18
    _aload                =  25, // 0x19
    _iload_0              =  26, // 0x1a
    _iload_1              =  27, // 0x1b
    _iload_2              =  28, // 0x1c
    _iload_3              =  29, // 0x1d
    _lload_0              =  30, // 0x1e
    _lload_1              =  31, // 0x1f
    _lload_2              =  32, // 0x20
    _lload_3              =  33, // 0x21
    _fload_0              =  34, // 0x22
    _fload_1              =  35, // 0x23
    _fload_2              =  36, // 0x24
    _fload_3              =  37, // 0x25
    _dload_0              =  38, // 0x26
    _dload_1              =  39, // 0x27
    _dload_2              =  40, // 0x28
    _dload_3              =  41, // 0x29
    _aload_0              =  42, // 0x2a
    _aload_1              =  43, // 0x2b
    _aload_2              =  44, // 0x2c
    _aload_3              =  45, // 0x2d
    _iaload               =  46, // 0x2e
    _laload               =  47, // 0x2f
    _faload               =  48, // 0x30
    _daload               =  49, // 0x31
    _aaload               =  50, // 0x32
    _baload               =  51, // 0x33
    _caload               =  52, // 0x34
    _saload               =  53, // 0x35
    _istore               =  54, // 0x36
    _lstore               =  55, // 0x37
    _fstore               =  56, // 0x38
    _dstore               =  57, // 0x39
    _astore               =  58, // 0x3a
    _istore_0             =  59, // 0x3b
    _istore_1             =  60, // 0x3c
    _istore_2             =  61, // 0x3d
    _istore_3             =  62, // 0x3e
    _lstore_0             =  63, // 0x3f
    _lstore_1             =  64, // 0x40
    _lstore_2             =  65, // 0x41
    _lstore_3             =  66, // 0x42
    _fstore_0             =  67, // 0x43
    _fstore_1             =  68, // 0x44
    _fstore_2             =  69, // 0x45
    _fstore_3             =  70, // 0x46
    _dstore_0             =  71, // 0x47
    _dstore_1             =  72, // 0x48
    _dstore_2             =  73, // 0x49
    _dstore_3             =  74, // 0x4a
    _astore_0             =  75, // 0x4b
    _astore_1             =  76, // 0x4c
    _astore_2             =  77, // 0x4d
    _astore_3             =  78, // 0x4e
    _iastore              =  79, // 0x4f
    _lastore              =  80, // 0x50
    _fastore              =  81, // 0x51
    _dastore              =  82, // 0x52
    _aastore              =  83, // 0x53
    _bastore              =  84, // 0x54
    _castore              =  85, // 0x55
    _sastore              =  86, // 0x56
    _pop                  =  87, // 0x57
    _pop2                 =  88, // 0x58
    _dup                  =  89, // 0x59
    _dup_x1               =  90, // 0x5a
    _dup_x2               =  91, // 0x5b
    _dup2                 =  92, // 0x5c
    _dup2_x1              =  93, // 0x5d
    _dup2_x2              =  94, // 0x5e
    _swap                 =  95, // 0x5f
    _iadd                 =  96, // 0x60
    _ladd                 =  97, // 0x61
    _fadd                 =  98, // 0x62
    _dadd                 =  99, // 0x63
    _isub                 = 100, // 0x64
    _lsub                 = 101, // 0x65
    _fsub                 = 102, // 0x66
    _dsub                 = 103, // 0x67
    _imul                 = 104, // 0x68
    _lmul                 = 105, // 0x69
    _fmul                 = 106, // 0x6a
    _dmul                 = 107, // 0x6b
    _idiv                 = 108, // 0x6c
    _ldiv                 = 109, // 0x6d
    _fdiv                 = 110, // 0x6e
    _ddiv                 = 111, // 0x6f
    _irem                 = 112, // 0x70
    _lrem                 = 113, // 0x71
    _frem                 = 114, // 0x72
    _drem                 = 115, // 0x73
    _ineg                 = 116, // 0x74
    _lneg                 = 117, // 0x75
    _fneg                 = 118, // 0x76
    _dneg                 = 119, // 0x77
    _ishl                 = 120, // 0x78
    _lshl                 = 121, // 0x79
    _ishr                 = 122, // 0x7a
    _lshr                 = 123, // 0x7b
    _iushr                = 124, // 0x7c
    _lushr                = 125, // 0x7d
    _iand                 = 126, // 0x7e
    _land                 = 127, // 0x7f
    _ior                  = 128, // 0x80
    _lor                  = 129, // 0x81
    _ixor                 = 130, // 0x82
    _lxor                 = 131, // 0x83
    _iinc                 = 132, // 0x84
    _i2l                  = 133, // 0x85
    _i2f                  = 134, // 0x86
    _i2d                  = 135, // 0x87
    _l2i                  = 136, // 0x88
    _l2f                  = 137, // 0x89
    _l2d                  = 138, // 0x8a
    _f2i                  = 139, // 0x8b
    _f2l                  = 140, // 0x8c
    _f2d                  = 141, // 0x8d
    _d2i                  = 142, // 0x8e
    _d2l                  = 143, // 0x8f
    _d2f                  = 144, // 0x90
    _i2b                  = 145, // 0x91
    _i2c                  = 146, // 0x92
    _i2s                  = 147, // 0x93
    _lcmp                 = 148, // 0x94
    _fcmpl                = 149, // 0x95
    _fcmpg                = 150, // 0x96
    _dcmpl                = 151, // 0x97
    _dcmpg                = 152, // 0x98
    _ifeq                 = 153, // 0x99
    _ifne                 = 154, // 0x9a
    _iflt                 = 155, // 0x9b
    _ifge                 = 156, // 0x9c
    _ifgt                 = 157, // 0x9d
    _ifle                 = 158, // 0x9e
    _if_icmpeq            = 159, // 0x9f
    _if_icmpne            = 160, // 0xa0
    _if_icmplt            = 161, // 0xa1
    _if_icmpge            = 162, // 0xa2
    _if_icmpgt            = 163, // 0xa3
    _if_icmple            = 164, // 0xa4
    _if_acmpeq            = 165, // 0xa5
    _if_acmpne            = 166, // 0xa6
    _goto                 = 167, // 0xa7
    _jsr                  = 168, // 0xa8
    _ret                  = 169, // 0xa9
    _tableswitch          = 170, // 0xaa
    _lookupswitch         = 171, // 0xab
    _ireturn              = 172, // 0xac
    _lreturn              = 173, // 0xad
    _freturn              = 174, // 0xae
    _dreturn              = 175, // 0xaf
    _areturn              = 176, // 0xb0
    _return               = 177, // 0xb1
    _getstatic            = 178, // 0xb2
    _putstatic            = 179, // 0xb3
    _getfield             = 180, // 0xb4
    _putfield             = 181, // 0xb5
    _invokevirtual        = 182, // 0xb6
    _invokespecial        = 183, // 0xb7
    _invokestatic         = 184, // 0xb8
    _invokeinterface      = 185, // 0xb9
    _invokedynamic        = 186, // 0xba
    _new                  = 187, // 0xbb
    _newarray             = 188, // 0xbc
    _anewarray            = 189, // 0xbd
    _arraylength          = 190, // 0xbe
    _athrow               = 191, // 0xbf
    _checkcast            = 192, // 0xc0
    _instanceof           = 193, // 0xc1
    _monitorenter         = 194, // 0xc2
    _monitorexit          = 195, // 0xc3
    _wide                 = 196, // 0xc4
    _multianewarray       = 197, // 0xc5
    _ifnull               = 198, // 0xc6
    _ifnonnull            = 199, // 0xc7
    _goto_w               = 200, // 0xc8
    _jsr_w                = 201, // 0xc9
    _breakpoint           = 202, // 0xca

    number_of_java_codes,

    // JVM bytecodes
    _fast_agetfield       = number_of_java_codes,
    _fast_bgetfield       ,
    _fast_cgetfield       ,
    _fast_dgetfield       ,
    _fast_fgetfield       ,
    _fast_igetfield       ,
    _fast_lgetfield       ,
    _fast_sgetfield       ,

    _fast_aputfield       ,
    _fast_bputfield       ,
    _fast_zputfield       ,
    _fast_cputfield       ,
    _fast_dputfield       ,
    _fast_fputfield       ,
    _fast_iputfield       ,
    _fast_lputfield       ,
    _fast_sputfield       ,

    _fast_aload_0         ,
    _fast_iaccess_0       ,
    _fast_aaccess_0       ,
    _fast_faccess_0       ,

    _fast_iload           ,
    _fast_iload2          ,
    _fast_icaload         ,

    _fast_invokevfinal    ,
    _fast_linearswitch    ,
    _fast_binaryswitch    ,

    // special handling of oop constants:
    _fast_aldc            ,
    _fast_aldc_w          ,

    _return_register_finalizer    ,

    // special handling of signature-polymorphic methods:
    _invokehandle         ,

    // These bytecodes are rewritten at CDS dump time, so that we can prevent them from being
    // rewritten at run time. This way, the ConstMethods can be placed in the CDS ReadOnly
    // section, and RewriteByteCodes/RewriteFrequentPairs can rewrite non-CDS bytecodes
    // at run time.
    //
    // Rewritten at CDS dump time to | Original bytecode
    // _invoke_virtual rewritten on sparc, will be disabled if UseSharedSpaces turned on.
    // ------------------------------+------------------
    _nofast_getfield      ,          //  <- _getfield
    _nofast_putfield      ,          //  <- _putfield
    _nofast_aload_0       ,          //  <- _aload_0
    _nofast_iload         ,          //  <- _iload

    _shouldnotreachhere   ,          // For debugging


    number_of_codes
  };

  // Flag bits derived from format strings, can_trap, can_rewrite, etc.:
  enum Flags {
    // semantic flags:
    _bc_can_trap      = 1<<0,     // bytecode execution can trap or block
    _bc_can_rewrite   = 1<<1,     // bytecode execution has an alternate form

    // format bits (determined only by the format string):
    _fmt_has_c        = 1<<2,     // constant, such as sipush "bcc"
    _fmt_has_j        = 1<<3,     // constant pool cache index, such as getfield "bjj"
    _fmt_has_k        = 1<<4,     // constant pool index, such as ldc "bk"
    _fmt_has_i        = 1<<5,     // local index, such as iload
    _fmt_has_o        = 1<<6,     // offset, such as ifeq
    _fmt_has_nbo      = 1<<7,     // contains native-order field(s)
    _fmt_has_u2       = 1<<8,     // contains double-byte field(s)
    _fmt_has_u4       = 1<<9,     // contains quad-byte field
    _fmt_not_variable = 1<<10,    // not of variable length (simple or wide)
    _fmt_not_simple   = 1<<11,    // either wide or variable length
    _all_fmt_bits     = (_fmt_not_simple*2 - _fmt_has_c),

    // Example derived format syndromes:
    _fmt_b      = _fmt_not_variable,
    _fmt_bc     = _fmt_b | _fmt_has_c,
    _fmt_bi     = _fmt_b | _fmt_has_i,
    _fmt_bkk    = _fmt_b | _fmt_has_k | _fmt_has_u2,
    _fmt_bJJ    = _fmt_b | _fmt_has_j | _fmt_has_u2 | _fmt_has_nbo,
    _fmt_bo2    = _fmt_b | _fmt_has_o | _fmt_has_u2,
    _fmt_bo4    = _fmt_b | _fmt_has_o | _fmt_has_u4
  };

 private:
  static bool        _is_initialized;
  static const char* _name          [number_of_codes];
  static BasicType   _result_type   [number_of_codes];
  static s_char      _depth         [number_of_codes];
  static u_char      _lengths       [number_of_codes];
  static Code        _java_code     [number_of_codes];
  static jchar       _flags         [(1<<BitsPerByte)*2]; // all second page for wide formats

  static void        def(Code code, const char* name, const char* format, const char* wide_format, BasicType result_type, int depth, bool can_trap);
  static void        def(Code code, const char* name, const char* format, const char* wide_format, BasicType result_type, int depth, bool can_trap, Code java_code);

  // Verify that bcp points into method
#ifdef ASSERT
  static bool        check_method(const Method* method, address bcp);
#endif
  static bool check_must_rewrite(Bytecodes::Code bc);

 public:
  // Conversion
  static void        check          (Code code)    { assert(is_defined(code),      "illegal code: %d", (int)code); }
  static void        wide_check     (Code code)    { assert(wide_is_defined(code), "illegal code: %d", (int)code); }
  static Code        cast           (int  code)    { return (Code)code; }


  // Fetch a bytecode, hiding breakpoints as necessary.  The method
  // argument is used for conversion of breakpoints into the original
  // bytecode.  The CI uses these methods but guarantees that
  // breakpoints are hidden so the method argument should be passed as
  // NULL since in that case the bcp and Method* are unrelated
  // memory.
  static Code       code_at(const Method* method, address bcp) {
    assert(method == NULL || check_method(method, bcp), "bcp must point into method");
    Code code = cast(*bcp);
    assert(code != _breakpoint || method != NULL, "need Method* to decode breakpoint");
    return (code != _breakpoint) ? code : non_breakpoint_code_at(method, bcp);
  }
  static Code       java_code_at(const Method* method, address bcp) {
    return java_code(code_at(method, bcp));
  }

  // Fetch a bytecode or a breakpoint:
  static Code       code_or_bp_at(address bcp)    { return (Code)cast(*bcp); }

  static Code       code_at(Method* method, int bci);

  // find a bytecode, behind a breakpoint if necessary:
  static Code       non_breakpoint_code_at(const Method* method, address bcp);

  // Bytecode attributes
  static bool        is_valid       (int  code)    { return 0 <= code && code < number_of_codes; }
  static bool        is_defined     (int  code)    { return is_valid(code) && flags(code, false) != 0; }
  static bool        wide_is_defined(int  code)    { return is_defined(code) && flags(code, true) != 0; }
  static const char* name           (Code code)    { check(code);      return _name          [code]; }
  static BasicType   result_type    (Code code)    { check(code);      return _result_type   [code]; }
  static int         depth          (Code code)    { check(code);      return _depth         [code]; }
  // Note: Length functions must return <=0 for invalid bytecodes.
  // Calling check(code) in length functions would throw an unwanted assert.
  static int         length_for     (Code code)    { return is_valid(code) ? _lengths[code] & 0xF : -1; }
  static int         wide_length_for(Code code)    { return is_valid(code) ? _lengths[code]  >> 4 : -1; }
  static bool        can_trap       (Code code)    { check(code);      return has_all_flags(code, _bc_can_trap, false); }
  static Code        java_code      (Code code)    { check(code);      return _java_code     [code]; }
  static bool        can_rewrite    (Code code)    { check(code);      return has_all_flags(code, _bc_can_rewrite, false); }
  static bool        must_rewrite(Bytecodes::Code code) { return can_rewrite(code) && check_must_rewrite(code); }
  static bool        native_byte_order(Code code)  { check(code);      return has_all_flags(code, _fmt_has_nbo, false); }
  static bool        uses_cp_cache  (Code code)    { check(code);      return has_all_flags(code, _fmt_has_j, false); }
  // if 'end' is provided, it indicates the end of the code buffer which
  // should not be read past when parsing.
  static int         special_length_at(Bytecodes::Code code, address bcp, address end = NULL);
  static int         raw_special_length_at(address bcp, address end = NULL);
  static int         length_for_code_at(Bytecodes::Code code, address bcp)  { int l = length_for(code); return l > 0 ? l : special_length_at(code, bcp); }
  static int         length_at      (Method* method, address bcp)  { return length_for_code_at(code_at(method, bcp), bcp); }
  static int         java_length_at (Method* method, address bcp)  { return length_for_code_at(java_code_at(method, bcp), bcp); }
  static bool        is_java_code   (Code code)    { return 0 <= code && code < number_of_java_codes; }

  static bool        is_store_into_local(Code code){ return (_istore <= code && code <= _astore_3); }
  static bool        is_const       (Code code)    { return (_aconst_null <= code && code <= _ldc2_w); }
  static bool        is_zero_const  (Code code)    { return (code == _aconst_null || code == _iconst_0
                                                           || code == _fconst_0 || code == _dconst_0); }
  static bool        is_return      (Code code)    { return (_ireturn <= code && code <= _return); }
  static bool        is_invoke      (Code code)    { return (_invokevirtual <= code && code <= _invokedynamic); }
  static bool        has_receiver   (Code code)    { assert(is_invoke(code), "");  return code == _invokevirtual ||
                                                                                          code == _invokespecial ||
                                                                                          code == _invokeinterface; }
  static bool        has_optional_appendix(Code code) { return code == _invokedynamic || code == _invokehandle; }

  static int         compute_flags  (const char* format, int more_flags = 0);  // compute the flags
  static int         flags          (int code, bool is_wide) {
    assert(code == (u_char)code, "must be a byte");
    return _flags[code + (is_wide ? (1<<BitsPerByte) : 0)];
  }
  static bool        has_all_flags  (Code code, int test_flags, bool is_wide) {
    return (flags(code, is_wide) & test_flags) == test_flags;
  }

  // Initialization
  static void        initialize     ();
};

#endif // SHARE_INTERPRETER_BYTECODES_HPP
