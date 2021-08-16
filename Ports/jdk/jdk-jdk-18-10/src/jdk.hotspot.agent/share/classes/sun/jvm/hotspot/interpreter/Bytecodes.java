/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.interpreter;

import java.util.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// Bytecodes specifies all bytecodes used in the VM and
// provides utility functions to get bytecode attributes.

public class Bytecodes {
  public static final int _illegal    =  -1;

  // Java bytecodes
  public static final int _nop                  =   0; // 0x00
  public static final int _aconst_null          =   1; // 0x01
  public static final int _iconst_m1            =   2; // 0x02
  public static final int _iconst_0             =   3; // 0x03
  public static final int _iconst_1             =   4; // 0x04
  public static final int _iconst_2             =   5; // 0x05
  public static final int _iconst_3             =   6; // 0x06
  public static final int _iconst_4             =   7; // 0x07
  public static final int _iconst_5             =   8; // 0x08
  public static final int _lconst_0             =   9; // 0x09
  public static final int _lconst_1             =  10; // 0x0a
  public static final int _fconst_0             =  11; // 0x0b
  public static final int _fconst_1             =  12; // 0x0c
  public static final int _fconst_2             =  13; // 0x0d
  public static final int _dconst_0             =  14; // 0x0e
  public static final int _dconst_1             =  15; // 0x0f
  public static final int _bipush               =  16; // 0x10
  public static final int _sipush               =  17; // 0x11
  public static final int _ldc                  =  18; // 0x12
  public static final int _ldc_w                =  19; // 0x13
  public static final int _ldc2_w               =  20; // 0x14
  public static final int _iload                =  21; // 0x15
  public static final int _lload                =  22; // 0x16
  public static final int _fload                =  23; // 0x17
  public static final int _dload                =  24; // 0x18
  public static final int _aload                =  25; // 0x19
  public static final int _iload_0              =  26; // 0x1a
  public static final int _iload_1              =  27; // 0x1b
  public static final int _iload_2              =  28; // 0x1c
  public static final int _iload_3              =  29; // 0x1d
  public static final int _lload_0              =  30; // 0x1e
  public static final int _lload_1              =  31; // 0x1f
  public static final int _lload_2              =  32; // 0x20
  public static final int _lload_3              =  33; // 0x21
  public static final int _fload_0              =  34; // 0x22
  public static final int _fload_1              =  35; // 0x23
  public static final int _fload_2              =  36; // 0x24
  public static final int _fload_3              =  37; // 0x25
  public static final int _dload_0              =  38; // 0x26
  public static final int _dload_1              =  39; // 0x27
  public static final int _dload_2              =  40; // 0x28
  public static final int _dload_3              =  41; // 0x29
  public static final int _aload_0              =  42; // 0x2a
  public static final int _aload_1              =  43; // 0x2b
  public static final int _aload_2              =  44; // 0x2c
  public static final int _aload_3              =  45; // 0x2d
  public static final int _iaload               =  46; // 0x2e
  public static final int _laload               =  47; // 0x2f
  public static final int _faload               =  48; // 0x30
  public static final int _daload               =  49; // 0x31
  public static final int _aaload               =  50; // 0x32
  public static final int _baload               =  51; // 0x33
  public static final int _caload               =  52; // 0x34
  public static final int _saload               =  53; // 0x35
  public static final int _istore               =  54; // 0x36
  public static final int _lstore               =  55; // 0x37
  public static final int _fstore               =  56; // 0x38
  public static final int _dstore               =  57; // 0x39
  public static final int _astore               =  58; // 0x3a
  public static final int _istore_0             =  59; // 0x3b
  public static final int _istore_1             =  60; // 0x3c
  public static final int _istore_2             =  61; // 0x3d
  public static final int _istore_3             =  62; // 0x3e
  public static final int _lstore_0             =  63; // 0x3f
  public static final int _lstore_1             =  64; // 0x40
  public static final int _lstore_2             =  65; // 0x41
  public static final int _lstore_3             =  66; // 0x42
  public static final int _fstore_0             =  67; // 0x43
  public static final int _fstore_1             =  68; // 0x44
  public static final int _fstore_2             =  69; // 0x45
  public static final int _fstore_3             =  70; // 0x46
  public static final int _dstore_0             =  71; // 0x47
  public static final int _dstore_1             =  72; // 0x48
  public static final int _dstore_2             =  73; // 0x49
  public static final int _dstore_3             =  74; // 0x4a
  public static final int _astore_0             =  75; // 0x4b
  public static final int _astore_1             =  76; // 0x4c
  public static final int _astore_2             =  77; // 0x4d
  public static final int _astore_3             =  78; // 0x4e
  public static final int _iastore              =  79; // 0x4f
  public static final int _lastore              =  80; // 0x50
  public static final int _fastore              =  81; // 0x51
  public static final int _dastore              =  82; // 0x52
  public static final int _aastore              =  83; // 0x53
  public static final int _bastore              =  84; // 0x54
  public static final int _castore              =  85; // 0x55
  public static final int _sastore              =  86; // 0x56
  public static final int _pop                  =  87; // 0x57
  public static final int _pop2                 =  88; // 0x58
  public static final int _dup                  =  89; // 0x59
  public static final int _dup_x1               =  90; // 0x5a
  public static final int _dup_x2               =  91; // 0x5b
  public static final int _dup2                 =  92; // 0x5c
  public static final int _dup2_x1              =  93; // 0x5d
  public static final int _dup2_x2              =  94; // 0x5e
  public static final int _swap                 =  95; // 0x5f
  public static final int _iadd                 =  96; // 0x60
  public static final int _ladd                 =  97; // 0x61
  public static final int _fadd                 =  98; // 0x62
  public static final int _dadd                 =  99; // 0x63
  public static final int _isub                 = 100; // 0x64
  public static final int _lsub                 = 101; // 0x65
  public static final int _fsub                 = 102; // 0x66
  public static final int _dsub                 = 103; // 0x67
  public static final int _imul                 = 104; // 0x68
  public static final int _lmul                 = 105; // 0x69
  public static final int _fmul                 = 106; // 0x6a
  public static final int _dmul                 = 107; // 0x6b
  public static final int _idiv                 = 108; // 0x6c
  public static final int _ldiv                 = 109; // 0x6d
  public static final int _fdiv                 = 110; // 0x6e
  public static final int _ddiv                 = 111; // 0x6f
  public static final int _irem                 = 112; // 0x70
  public static final int _lrem                 = 113; // 0x71
  public static final int _frem                 = 114; // 0x72
  public static final int _drem                 = 115; // 0x73
  public static final int _ineg                 = 116; // 0x74
  public static final int _lneg                 = 117; // 0x75
  public static final int _fneg                 = 118; // 0x76
  public static final int _dneg                 = 119; // 0x77
  public static final int _ishl                 = 120; // 0x78
  public static final int _lshl                 = 121; // 0x79
  public static final int _ishr                 = 122; // 0x7a
  public static final int _lshr                 = 123; // 0x7b
  public static final int _iushr                = 124; // 0x7c
  public static final int _lushr                = 125; // 0x7d
  public static final int _iand                 = 126; // 0x7e
  public static final int _land                 = 127; // 0x7f
  public static final int _ior                  = 128; // 0x80
  public static final int _lor                  = 129; // 0x81
  public static final int _ixor                 = 130; // 0x82
  public static final int _lxor                 = 131; // 0x83
  public static final int _iinc                 = 132; // 0x84
  public static final int _i2l                  = 133; // 0x85
  public static final int _i2f                  = 134; // 0x86
  public static final int _i2d                  = 135; // 0x87
  public static final int _l2i                  = 136; // 0x88
  public static final int _l2f                  = 137; // 0x89
  public static final int _l2d                  = 138; // 0x8a
  public static final int _f2i                  = 139; // 0x8b
  public static final int _f2l                  = 140; // 0x8c
  public static final int _f2d                  = 141; // 0x8d
  public static final int _d2i                  = 142; // 0x8e
  public static final int _d2l                  = 143; // 0x8f
  public static final int _d2f                  = 144; // 0x90
  public static final int _i2b                  = 145; // 0x91
  public static final int _i2c                  = 146; // 0x92
  public static final int _i2s                  = 147; // 0x93
  public static final int _lcmp                 = 148; // 0x94
  public static final int _fcmpl                = 149; // 0x95
  public static final int _fcmpg                = 150; // 0x96
  public static final int _dcmpl                = 151; // 0x97
  public static final int _dcmpg                = 152; // 0x98
  public static final int _ifeq                 = 153; // 0x99
  public static final int _ifne                 = 154; // 0x9a
  public static final int _iflt                 = 155; // 0x9b
  public static final int _ifge                 = 156; // 0x9c
  public static final int _ifgt                 = 157; // 0x9d
  public static final int _ifle                 = 158; // 0x9e
  public static final int _if_icmpeq            = 159; // 0x9f
  public static final int _if_icmpne            = 160; // 0xa0
  public static final int _if_icmplt            = 161; // 0xa1
  public static final int _if_icmpge            = 162; // 0xa2
  public static final int _if_icmpgt            = 163; // 0xa3
  public static final int _if_icmple            = 164; // 0xa4
  public static final int _if_acmpeq            = 165; // 0xa5
  public static final int _if_acmpne            = 166; // 0xa6
  public static final int _goto                 = 167; // 0xa7
  public static final int _jsr                  = 168; // 0xa8
  public static final int _ret                  = 169; // 0xa9
  public static final int _tableswitch          = 170; // 0xaa
  public static final int _lookupswitch         = 171; // 0xab
  public static final int _ireturn              = 172; // 0xac
  public static final int _lreturn              = 173; // 0xad
  public static final int _freturn              = 174; // 0xae
  public static final int _dreturn              = 175; // 0xaf
  public static final int _areturn              = 176; // 0xb0
  public static final int _return               = 177; // 0xb1
  public static final int _getstatic            = 178; // 0xb2
  public static final int _putstatic            = 179; // 0xb3
  public static final int _getfield             = 180; // 0xb4
  public static final int _putfield             = 181; // 0xb5
  public static final int _invokevirtual        = 182; // 0xb6
  public static final int _invokespecial        = 183; // 0xb7
  public static final int _invokestatic         = 184; // 0xb8
  public static final int _invokeinterface      = 185; // 0xb9
  public static final int _invokedynamic        = 186; // 0xba
  public static final int _new                  = 187; // 0xbb
  public static final int _newarray             = 188; // 0xbc
  public static final int _anewarray            = 189; // 0xbd
  public static final int _arraylength          = 190; // 0xbe
  public static final int _athrow               = 191; // 0xbf
  public static final int _checkcast            = 192; // 0xc0
  public static final int _instanceof           = 193; // 0xc1
  public static final int _monitorenter         = 194; // 0xc2
  public static final int _monitorexit          = 195; // 0xc3
  public static final int _wide                 = 196; // 0xc4
  public static final int _multianewarray       = 197; // 0xc5
  public static final int _ifnull               = 198; // 0xc6
  public static final int _ifnonnull            = 199; // 0xc7
  public static final int _goto_w               = 200; // 0xc8
  public static final int _jsr_w                = 201; // 0xc9
  public static final int _breakpoint           = 202; // 0xca

  public static final int number_of_java_codes  = 203;

  // JVM bytecodes
  public static final int _fast_agetfield       = number_of_java_codes;
  public static final int _fast_bgetfield       = 204;
  public static final int _fast_cgetfield       = 205;
  public static final int _fast_dgetfield       = 206;
  public static final int _fast_fgetfield       = 207;
  public static final int _fast_igetfield       = 208;
  public static final int _fast_lgetfield       = 209;
  public static final int _fast_sgetfield       = 210;
  public static final int _fast_aputfield       = 211;
  public static final int _fast_bputfield       = 212;
  public static final int _fast_zputfield       = 213;
  public static final int _fast_cputfield       = 214;
  public static final int _fast_dputfield       = 215;
  public static final int _fast_fputfield       = 216;
  public static final int _fast_iputfield       = 217;
  public static final int _fast_lputfield       = 218;
  public static final int _fast_sputfield       = 219;
  public static final int _fast_aload_0         = 220;
  public static final int _fast_iaccess_0       = 221;
  public static final int _fast_aaccess_0       = 222;
  public static final int _fast_faccess_0       = 223;
  public static final int _fast_iload           = 224;
  public static final int _fast_iload2          = 225;
  public static final int _fast_icaload         = 226;
  public static final int _fast_invokevfinal    = 227;
  public static final int _fast_linearswitch    = 228;
  public static final int _fast_binaryswitch    = 229;
  public static final int _fast_aldc            = 230;
  public static final int _fast_aldc_w          = 231;
  public static final int _return_register_finalizer = 232;
  public static final int _invokehandle         = 233;

  // Bytecodes rewritten at CDS dump time
  public static final int _nofast_getfield      = 234;
  public static final int _nofast_putfield      = 235;
  public static final int _nofast_aload_0       = 236;
  public static final int _nofast_iload         = 237;
  public static final int _shouldnotreachhere   = 238; // For debugging

  public static final int number_of_codes       = 239;

  // Flag bits derived from format strings, can_trap, can_rewrite, etc.:
  // semantic flags:
  static final int  _bc_can_trap      = 1<<0;     // bytecode execution can trap or block
  static final int  _bc_can_rewrite   = 1<<1;     // bytecode execution has an alternate form

  // format bits (determined only by the format string):
  static final int  _fmt_has_c        = 1<<2;     // constant, such as sipush "bcc"
  static final int  _fmt_has_j        = 1<<3;     // constant pool cache index, such as getfield "bjj"
  static final int  _fmt_has_k        = 1<<4;     // constant pool index, such as ldc "bk"
  static final int  _fmt_has_i        = 1<<5;     // local index, such as iload
  static final int  _fmt_has_o        = 1<<6;     // offset, such as ifeq
  static final int  _fmt_has_nbo      = 1<<7;     // contains native-order field(s)
  static final int  _fmt_has_u2       = 1<<8;     // contains double-byte field(s)
  static final int  _fmt_has_u4       = 1<<9;     // contains quad-byte field
  static final int  _fmt_not_variable = 1<<10;    // not of variable length (simple or wide)
  static final int  _fmt_not_simple   = 1<<11;    // either wide or variable length
  static final int  _all_fmt_bits     = (_fmt_not_simple*2 - _fmt_has_c);

  // Example derived format syndromes:
  static final int  _fmt_b      = _fmt_not_variable;
  static final int  _fmt_bc     = _fmt_b | _fmt_has_c;
  static final int  _fmt_bi     = _fmt_b | _fmt_has_i;
  static final int  _fmt_bkk    = _fmt_b | _fmt_has_k | _fmt_has_u2;
  static final int  _fmt_bJJ    = _fmt_b | _fmt_has_j | _fmt_has_u2 | _fmt_has_nbo;
  static final int  _fmt_bo2    = _fmt_b | _fmt_has_o | _fmt_has_u2;
  static final int  _fmt_bo4    = _fmt_b | _fmt_has_o | _fmt_has_u4;


  public static int specialLengthAt(Method method, int bci) {
    int code = codeAt(method, bci);
    switch (code) {
    case _wide:
      return wideLengthFor(method.getBytecodeOrBPAt(bci + 1));
    case _tableswitch:
      {
        int alignedBCI = Bits.roundTo(bci + 1, jintSize);
        int lo = method.getBytecodeIntArg(alignedBCI + 1*jintSize);
        int hi = method.getBytecodeIntArg(alignedBCI + 2*jintSize);
        return (alignedBCI - bci) + (3 + hi - lo + 1)*jintSize;
      }

    case _lookupswitch:      // fall through
    case _fast_binaryswitch: // fall through
    case _fast_linearswitch:
      {
        int alignedBCI = Bits.roundTo(bci + 1, jintSize);
        int npairs = method.getBytecodeIntArg(alignedBCI + jintSize);
        return (alignedBCI - bci) + (2 + 2*npairs)*jintSize;
      }

    }
    throw new RuntimeException("should not reach here");
  }

  // Conversion
  public static void check(int code) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isDefined(code), "illegal code " + code);
    }
  }
  public static void wideCheck(int code) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(wideIsDefined(code), "illegal code " + code);
    }
  }

  /** Fetches a bytecode, hiding breakpoints as necessary */
  public static int codeAt(Method method, int bci) {
    int res = codeOrBPAt(method, bci);
    if (res == _breakpoint) {
      res = method.getOrigBytecodeAt(bci);
    }
    return res;
  }

  /** Fetches a bytecode or a breakpoint */
  public static int codeOrBPAt(Method method, int bci) {
    return method.getBytecodeOrBPAt(bci);
  }

  public static boolean isActiveBreakpointAt(Method method, int bci) {
    return (codeOrBPAt(method, bci) == _breakpoint);
  }

  // find a bytecode, behind a breakpoint if necessary:
  // FIXME: not yet implementable
  //   static Code       non_breakpoint_code_at(address bcp, Method* method = null);

  // Bytecode attributes
  public static boolean   isDefined    (int code) { return 0 <= code && code < number_of_codes && flags(code, false) != 0; }
  public static boolean   wideIsDefined(int code) { return isDefined(code) && flags(code, true) != 0; }
  public static String    name         (int code) { check(code);      return _name          [code]; }
  public static String    format       (int code) { check(code);      return _format        [code]; }
  public static String    wideFormat   (int code) { wideCheck(code);  return _wide_format   [code]; }
  public static int       resultType   (int code) { check(code);      return _result_type   [code]; }
  public static int       depth        (int code) { check(code);      return _depth         [code]; }
  public static int       lengthFor    (int code) { check(code);      return _lengths       [code] & 0xF; }
  public static int       wideLengthFor(int code) { check(code);      return _lengths       [code] >> 4; }
  public static boolean   canTrap      (int code) { check(code);      return has_all_flags(code, _bc_can_trap, false); }
  public static int       javaCode     (int code) { check(code);      return _java_code     [code]; }
  public static boolean   canRewrite   (int code) { check(code);      return has_all_flags(code, _bc_can_rewrite, false); }
  public static boolean   native_byte_order(int code)  { check(code);      return has_all_flags(code, _fmt_has_nbo, false); }
  public static boolean   uses_cp_cache  (int code)    { check(code);      return has_all_flags(code, _fmt_has_j, false); }
  public static int       lengthAt     (Method method, int bci) { int l = lengthFor(codeAt(method, bci)); return l > 0 ? l : specialLengthAt(method, bci); }
  public static int       javaLengthAt (Method method, int bci) { int l = lengthFor(javaCode(codeAt(method, bci))); return l > 0 ? l : specialLengthAt(method, bci); }
  public static boolean   isJavaCode   (int code) { return 0 <= code && code < number_of_java_codes; }
  public static boolean   isFastCode   (int code) { return number_of_java_codes <= code && code < number_of_codes; }

  public static boolean   isAload      (int code) { return (code == _aload  || code == _aload_0  || code == _aload_1
                                                                            || code == _aload_2  || code == _aload_3); }
  public static boolean   isAstore     (int code) { return (code == _astore || code == _astore_0 || code == _astore_1
                                                                            || code == _astore_2 || code == _astore_3); }

  public static boolean   isZeroConst  (int code) { return (code == _aconst_null || code == _iconst_0
                                                                                 || code == _fconst_0 || code == _dconst_0); }

  static int         flags          (int code, boolean is_wide) {
    assert code == (code & 0xff) : "must be a byte";
    return _flags[code + (is_wide ? 256 : 0)];
  }
  static int         format_bits    (int code, boolean is_wide) { return flags(code, is_wide) & _all_fmt_bits; }
  static boolean     has_all_flags  (int code, int test_flags, boolean is_wide) {
    return (flags(code, is_wide) & test_flags) == test_flags;
  }

  static char compute_flags(String format) {
    return compute_flags(format, 0);
  }
  static char compute_flags(String format, int more_flags) {
    if (format == null)  return 0;  // not even more_flags
    int flags = more_flags;
    int fp = 0;
    if (format.length() == 0) {
      flags |= _fmt_not_simple; // but variable
    } else {
      switch (format.charAt(fp)) {
      case 'b':
        flags |= _fmt_not_variable;  // but simple
        ++fp;  // skip 'b'
        break;
      case 'w':
        flags |= _fmt_not_variable | _fmt_not_simple;
        ++fp;  // skip 'w'
      assert(format.charAt(fp) == 'b') : "wide format must start with 'wb'";
        ++fp;  // skip 'b'
        break;
      }
    }

    boolean has_nbo = false, has_jbo = false;
    int has_size = 0;
    while (fp < format.length()) {
      int this_flag = 0;
      char fc = format.charAt(fp++);
      switch (fc) {
      case '_': continue;         // ignore these

      case 'j': this_flag = _fmt_has_j; has_jbo = true; break;
      case 'k': this_flag = _fmt_has_k; has_jbo = true; break;
      case 'i': this_flag = _fmt_has_i; has_jbo = true; break;
      case 'c': this_flag = _fmt_has_c; has_jbo = true; break;
      case 'o': this_flag = _fmt_has_o; has_jbo = true; break;

        // uppercase versions mark native byte order (from Rewriter)
        // actually, only the 'J' case happens currently
      case 'J': this_flag = _fmt_has_j; has_nbo = true; break;
      case 'K': this_flag = _fmt_has_k; has_nbo = true; break;
      case 'I': this_flag = _fmt_has_i; has_nbo = true; break;
      case 'C': this_flag = _fmt_has_c; has_nbo = true; break;
      case 'O': this_flag = _fmt_has_o; has_nbo = true; break;
      default:  assert false : "bad char in format";
      }

      flags |= this_flag;

      assert !(has_jbo && has_nbo) : "mixed byte orders in format";
      if (has_nbo)
        flags |= _fmt_has_nbo;

      int this_size = 1;
      if (fp < format.length() && format.charAt(fp) == fc) {
        // advance beyond run of the same characters
        this_size = 2;
        while (fp  + 1 < format.length() && format.charAt(++fp) == fc)  this_size++;
        switch (this_size) {
        case 2: flags |= _fmt_has_u2; break;
        case 4: flags |= _fmt_has_u4; break;
        default: assert false : "bad rep count in format";
        }
      }
      assert has_size == 0 ||                     // no field yet
        this_size == has_size ||             // same size
        this_size < has_size && fp == format.length() : // last field can be short
             "mixed field sizes in format";
      has_size = this_size;
    }

    assert flags == (char)flags : "change _format_flags";
    return (char)flags;
  }


  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static String[]    _name;
  private static String[]    _format;
  private static String[]    _wide_format;
  private static int[]       _result_type;
  private static byte[]      _depth;
  private static byte[]      _lengths;
  private static int[]       _java_code;
  private static char[]      _flags;

  static {
    _name           = new String [number_of_codes];
    _format         = new String [number_of_codes];
    _wide_format    = new String [number_of_codes];
    _result_type    = new int    [number_of_codes]; // See BasicType.java
    _depth          = new byte   [number_of_codes];
    _lengths        = new byte   [number_of_codes];
    _java_code      = new int    [number_of_codes];
    _flags          = new char[256 * 2]; // all second page for wide formats

    // In case we want to fetch this information from the VM in the
    // future
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize();
        }
      });
  }

  private static final int jintSize    =   4;

  //  private static String[]    _name           = new String [number_of_codes];
  //  private static String[]    _format         = new String [number_of_codes];
  //  private static String[]    _wide_format    = new String [number_of_codes];
  //  private static int[]       _result_type    = new int    [number_of_codes]; // See BasicType.java
  //  private static byte[]      _depth          = new byte   [number_of_codes];
  //  private static byte[]      _length         = new byte   [number_of_codes];
  //  private static boolean[]   _can_trap       = new boolean[number_of_codes];
  //  private static int[]       _java_code      = new int    [number_of_codes];
  //  private static boolean[]   _can_rewrite    = new boolean[number_of_codes];

  // Initialization
  private static void initialize() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(number_of_codes <= 256, "too many bytecodes");
    }

    // Format strings interpretation:
    //
    // b: bytecode
    // c: signed constant, Java byte-ordering
    // i: unsigned index , Java byte-ordering
    // j: unsigned index , native byte-ordering
    // o: branch offset  , Java byte-ordering
    // _: unused/ignored
    // w: wide bytecode
    //
    // Note: Right now the format strings are used for 2 purposes:
    //       1. to specify the length of the bytecode
    //          (= number of characters in format string)
    //       2. to specify the bytecode attributes
    //
    //       The bytecode attributes are currently used only for bytecode tracing
    //       (see BytecodeTracer); thus if more specific format information is
    //       used, one would also have to adjust the bytecode tracer.
    //
    // Note: For bytecodes with variable length, the format string is the empty string.

    // Note 1: null for the format string means the bytecode doesn't exist
    //         in that form.
    //
    // Note 2: The result type is T_ILLEGAL for bytecodes where the top of stack
    //         type after execution is not only determined by the bytecode itself.

    //  Java bytecodes
    //  bytecode               bytecode name           format   wide f.   result tp                stk traps
    def(_nop                 , "nop"                 , "b"    , null    , BasicType.getTVoid()   ,  0, false);
    def(_aconst_null         , "aconst_null"         , "b"    , null    , BasicType.getTObject() ,  1, false);
    def(_iconst_m1           , "iconst_m1"           , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_0            , "iconst_0"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_1            , "iconst_1"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_2            , "iconst_2"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_3            , "iconst_3"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_4            , "iconst_4"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iconst_5            , "iconst_5"            , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_lconst_0            , "lconst_0"            , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_lconst_1            , "lconst_1"            , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_fconst_0            , "fconst_0"            , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_fconst_1            , "fconst_1"            , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_fconst_2            , "fconst_2"            , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_dconst_0            , "dconst_0"            , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_dconst_1            , "dconst_1"            , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_bipush              , "bipush"              , "bc"   , null    , BasicType.getTInt()    ,  1, false);
    def(_sipush              , "sipush"              , "bcc"  , null    , BasicType.getTInt()    ,  1, false);
    def(_ldc                 , "ldc"                 , "bk"   , null    , BasicType.getTIllegal(),  1, true );
    def(_ldc_w               , "ldc_w"               , "bkk"  , null    , BasicType.getTIllegal(),  1, true );
    def(_ldc2_w              , "ldc2_w"              , "bkk"  , null    , BasicType.getTIllegal(),  2, true );
    def(_iload               , "iload"               , "bi"   , "wbii"  , BasicType.getTInt()    ,  1, false);
    def(_lload               , "lload"               , "bi"   , "wbii"  , BasicType.getTLong()   ,  2, false);
    def(_fload               , "fload"               , "bi"   , "wbii"  , BasicType.getTFloat()  ,  1, false);
    def(_dload               , "dload"               , "bi"   , "wbii"  , BasicType.getTDouble() ,  2, false);
    def(_aload               , "aload"               , "bi"   , "wbii"  , BasicType.getTObject() ,  1, false);
    def(_iload_0             , "iload_0"             , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iload_1             , "iload_1"             , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iload_2             , "iload_2"             , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_iload_3             , "iload_3"             , "b"    , null    , BasicType.getTInt()    ,  1, false);
    def(_lload_0             , "lload_0"             , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_lload_1             , "lload_1"             , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_lload_2             , "lload_2"             , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_lload_3             , "lload_3"             , "b"    , null    , BasicType.getTLong()   ,  2, false);
    def(_fload_0             , "fload_0"             , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_fload_1             , "fload_1"             , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_fload_2             , "fload_2"             , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_fload_3             , "fload_3"             , "b"    , null    , BasicType.getTFloat()  ,  1, false);
    def(_dload_0             , "dload_0"             , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_dload_1             , "dload_1"             , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_dload_2             , "dload_2"             , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_dload_3             , "dload_3"             , "b"    , null    , BasicType.getTDouble() ,  2, false);
    def(_aload_0             , "aload_0"             , "b"    , null    , BasicType.getTObject() ,  1, true ); // rewriting in interpreter
    def(_aload_1             , "aload_1"             , "b"    , null    , BasicType.getTObject() ,  1, false);
    def(_aload_2             , "aload_2"             , "b"    , null    , BasicType.getTObject() ,  1, false);
    def(_aload_3             , "aload_3"             , "b"    , null    , BasicType.getTObject() ,  1, false);
    def(_iaload              , "iaload"              , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_laload              , "laload"              , "b"    , null    , BasicType.getTLong()   ,  0, true );
    def(_faload              , "faload"              , "b"    , null    , BasicType.getTFloat()  , -1, true );
    def(_daload              , "daload"              , "b"    , null    , BasicType.getTDouble() ,  0, true );
    def(_aaload              , "aaload"              , "b"    , null    , BasicType.getTObject() , -1, true );
    def(_baload              , "baload"              , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_caload              , "caload"              , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_saload              , "saload"              , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_istore              , "istore"              , "bi"   , "wbii"  , BasicType.getTVoid()   , -1, false);
    def(_lstore              , "lstore"              , "bi"   , "wbii"  , BasicType.getTVoid()   , -2, false);
    def(_fstore              , "fstore"              , "bi"   , "wbii"  , BasicType.getTVoid()   , -1, false);
    def(_dstore              , "dstore"              , "bi"   , "wbii"  , BasicType.getTVoid()   , -2, false);
    def(_astore              , "astore"              , "bi"   , "wbii"  , BasicType.getTVoid()   , -1, false);
    def(_istore_0            , "istore_0"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_istore_1            , "istore_1"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_istore_2            , "istore_2"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_istore_3            , "istore_3"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_lstore_0            , "lstore_0"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_lstore_1            , "lstore_1"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_lstore_2            , "lstore_2"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_lstore_3            , "lstore_3"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_fstore_0            , "fstore_0"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_fstore_1            , "fstore_1"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_fstore_2            , "fstore_2"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_fstore_3            , "fstore_3"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_dstore_0            , "dstore_0"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_dstore_1            , "dstore_1"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_dstore_2            , "dstore_2"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_dstore_3            , "dstore_3"            , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_astore_0            , "astore_0"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_astore_1            , "astore_1"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_astore_2            , "astore_2"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_astore_3            , "astore_3"            , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_iastore             , "iastore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_lastore             , "lastore"             , "b"    , null    , BasicType.getTVoid()   , -4, true );
    def(_fastore             , "fastore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_dastore             , "dastore"             , "b"    , null    , BasicType.getTVoid()   , -4, true );
    def(_aastore             , "aastore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_bastore             , "bastore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_castore             , "castore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_sastore             , "sastore"             , "b"    , null    , BasicType.getTVoid()   , -3, true );
    def(_pop                 , "pop"                 , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_pop2                , "pop2"                , "b"    , null    , BasicType.getTVoid()   , -2, false);
    def(_dup                 , "dup"                 , "b"    , null    , BasicType.getTVoid()   ,  1, false);
    def(_dup_x1              , "dup_x1"              , "b"    , null    , BasicType.getTVoid()   ,  1, false);
    def(_dup_x2              , "dup_x2"              , "b"    , null    , BasicType.getTVoid()   ,  1, false);
    def(_dup2                , "dup2"                , "b"    , null    , BasicType.getTVoid()   ,  2, false);
    def(_dup2_x1             , "dup2_x1"             , "b"    , null    , BasicType.getTVoid()   ,  2, false);
    def(_dup2_x2             , "dup2_x2"             , "b"    , null    , BasicType.getTVoid()   ,  2, false);
    def(_swap                , "swap"                , "b"    , null    , BasicType.getTVoid()   ,  0, false);
    def(_iadd                , "iadd"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_ladd                , "ladd"                , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_fadd                , "fadd"                , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_dadd                , "dadd"                , "b"    , null    , BasicType.getTDouble() , -2, false);
    def(_isub                , "isub"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lsub                , "lsub"                , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_fsub                , "fsub"                , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_dsub                , "dsub"                , "b"    , null    , BasicType.getTDouble() , -2, false);
    def(_imul                , "imul"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lmul                , "lmul"                , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_fmul                , "fmul"                , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_dmul                , "dmul"                , "b"    , null    , BasicType.getTDouble() , -2, false);
    def(_idiv                , "idiv"                , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_ldiv                , "ldiv"                , "b"    , null    , BasicType.getTLong()   , -2, true );
    def(_fdiv                , "fdiv"                , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_ddiv                , "ddiv"                , "b"    , null    , BasicType.getTDouble() , -2, false);
    def(_irem                , "irem"                , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_lrem                , "lrem"                , "b"    , null    , BasicType.getTLong()   , -2, true );
    def(_frem                , "frem"                , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_drem                , "drem"                , "b"    , null    , BasicType.getTDouble() , -2, false);
    def(_ineg                , "ineg"                , "b"    , null    , BasicType.getTInt()    ,  0, false);
    def(_lneg                , "lneg"                , "b"    , null    , BasicType.getTLong()   ,  0, false);
    def(_fneg                , "fneg"                , "b"    , null    , BasicType.getTFloat()  ,  0, false);
    def(_dneg                , "dneg"                , "b"    , null    , BasicType.getTDouble() ,  0, false);
    def(_ishl                , "ishl"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lshl                , "lshl"                , "b"    , null    , BasicType.getTLong()   , -1, false);
    def(_ishr                , "ishr"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lshr                , "lshr"                , "b"    , null    , BasicType.getTLong()   , -1, false);
    def(_iushr               , "iushr"               , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lushr               , "lushr"               , "b"    , null    , BasicType.getTLong()   , -1, false);
    def(_iand                , "iand"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_land                , "land"                , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_ior                 , "ior"                 , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lor                 , "lor"                 , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_ixor                , "ixor"                , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_lxor                , "lxor"                , "b"    , null    , BasicType.getTLong()   , -2, false);
    def(_iinc                , "iinc"                , "bic"  , "wbiicc", BasicType.getTVoid()   ,  0, false);
    def(_i2l                 , "i2l"                 , "b"    , null    , BasicType.getTLong()   ,  1, false);
    def(_i2f                 , "i2f"                 , "b"    , null    , BasicType.getTFloat()  ,  0, false);
    def(_i2d                 , "i2d"                 , "b"    , null    , BasicType.getTDouble() ,  1, false);
    def(_l2i                 , "l2i"                 , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_l2f                 , "l2f"                 , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_l2d                 , "l2d"                 , "b"    , null    , BasicType.getTDouble() ,  0, false);
    def(_f2i                 , "f2i"                 , "b"    , null    , BasicType.getTInt()    ,  0, false);
    def(_f2l                 , "f2l"                 , "b"    , null    , BasicType.getTLong()   ,  1, false);
    def(_f2d                 , "f2d"                 , "b"    , null    , BasicType.getTDouble() ,  1, false);
    def(_d2i                 , "d2i"                 , "b"    , null    , BasicType.getTInt()    , -1, false);
    def(_d2l                 , "d2l"                 , "b"    , null    , BasicType.getTLong()   ,  0, false);
    def(_d2f                 , "d2f"                 , "b"    , null    , BasicType.getTFloat()  , -1, false);
    def(_i2b                 , "i2b"                 , "b"    , null    , BasicType.getTByte()   ,  0, false);
    def(_i2c                 , "i2c"                 , "b"    , null    , BasicType.getTChar()   ,  0, false);
    def(_i2s                 , "i2s"                 , "b"    , null    , BasicType.getTShort()  ,  0, false);
    def(_lcmp                , "lcmp"                , "b"    , null    , BasicType.getTVoid()   , -3, false);
    def(_fcmpl               , "fcmpl"               , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_fcmpg               , "fcmpg"               , "b"    , null    , BasicType.getTVoid()   , -1, false);
    def(_dcmpl               , "dcmpl"               , "b"    , null    , BasicType.getTVoid()   , -3, false);
    def(_dcmpg               , "dcmpg"               , "b"    , null    , BasicType.getTVoid()   , -3, false);
    def(_ifeq                , "ifeq"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_ifne                , "ifne"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_iflt                , "iflt"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_ifge                , "ifge"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_ifgt                , "ifgt"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_ifle                , "ifle"                , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_if_icmpeq           , "if_icmpeq"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_icmpne           , "if_icmpne"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_icmplt           , "if_icmplt"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_icmpge           , "if_icmpge"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_icmpgt           , "if_icmpgt"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_icmple           , "if_icmple"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_acmpeq           , "if_acmpeq"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_if_acmpne           , "if_acmpne"           , "boo"  , null    , BasicType.getTVoid()   , -2, false);
    def(_goto                , "goto"                , "boo"  , null    , BasicType.getTVoid()   ,  0, false);
    def(_jsr                 , "jsr"                 , "boo"  , null    , BasicType.getTInt()    ,  0, false);
    def(_ret                 , "ret"                 , "bi"   , "wbii"  , BasicType.getTVoid()   ,  0, false);
    def(_tableswitch         , "tableswitch"         , ""     , null    , BasicType.getTVoid()   , -1, false); // may have backward branches
    def(_lookupswitch        , "lookupswitch"        , ""     , null    , BasicType.getTVoid()   , -1, false); // rewriting in interpreter
    def(_ireturn             , "ireturn"             , "b"    , null    , BasicType.getTInt()    , -1, true );
    def(_lreturn             , "lreturn"             , "b"    , null    , BasicType.getTLong()   , -2, true );
    def(_freturn             , "freturn"             , "b"    , null    , BasicType.getTFloat()  , -1, true );
    def(_dreturn             , "dreturn"             , "b"    , null    , BasicType.getTDouble() , -2, true );
    def(_areturn             , "areturn"             , "b"    , null    , BasicType.getTObject() , -1, true );
    def(_return              , "return"              , "b"    , null    , BasicType.getTVoid()   ,  0, true );
    def(_getstatic           , "getstatic"           , "bJJ"  , null    , BasicType.getTIllegal(),  1, true );
    def(_putstatic           , "putstatic"           , "bJJ"  , null    , BasicType.getTIllegal(), -1, true );
    def(_getfield            , "getfield"            , "bJJ"  , null    , BasicType.getTIllegal(),  0, true );
    def(_putfield            , "putfield"            , "bJJ"  , null    , BasicType.getTIllegal(), -2, true );
    def(_invokevirtual       , "invokevirtual"       , "bJJ"  , null    , BasicType.getTIllegal(), -1, true );
    def(_invokespecial       , "invokespecial"       , "bJJ"  , null    , BasicType.getTIllegal(), -1, true );
    def(_invokestatic        , "invokestatic"        , "bJJ"  , null    , BasicType.getTIllegal(),  0, true );
    def(_invokeinterface     , "invokeinterface"     , "bJJ__", null    , BasicType.getTIllegal(), -1, true );
    def(_invokedynamic       , "invokedynamic"       , "bJJJJ", null    , BasicType.getTIllegal(),  0, true );
    def(_new                 , "new"                 , "bkk"  , null    , BasicType.getTObject() ,  1, true );
    def(_newarray            , "newarray"            , "bc"   , null    , BasicType.getTObject() ,  0, true );
    def(_anewarray           , "anewarray"           , "bkk"  , null    , BasicType.getTObject() ,  0, true );
    def(_arraylength         , "arraylength"         , "b"    , null    , BasicType.getTVoid()   ,  0, true );
    def(_athrow              , "athrow"              , "b"    , null    , BasicType.getTVoid()   , -1, true );
    def(_checkcast           , "checkcast"           , "bkk"  , null    , BasicType.getTObject() ,  0, true );
    def(_instanceof          , "instanceof"          , "bkk"  , null    , BasicType.getTInt()    ,  0, true );
    def(_monitorenter        , "monitorenter"        , "b"    , null    , BasicType.getTVoid()   , -1, true );
    def(_monitorexit         , "monitorexit"         , "b"    , null    , BasicType.getTVoid()   , -1, true );
    def(_wide                , "wide"                , ""     , null    , BasicType.getTVoid()   ,  0, false);
    def(_multianewarray      , "multianewarray"      , "bkkc" , null    , BasicType.getTObject() ,  1, true );
    def(_ifnull              , "ifnull"              , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_ifnonnull           , "ifnonnull"           , "boo"  , null    , BasicType.getTVoid()   , -1, false);
    def(_goto_w              , "goto_w"              , "boooo", null    , BasicType.getTVoid()   ,  0, false);
    def(_jsr_w               , "jsr_w"               , "boooo", null    , BasicType.getTInt()    ,  0, false);
    def(_breakpoint          , "breakpoint"          , ""     , null    , BasicType.getTVoid()   ,  0, true );

    //  JVM bytecodes
    //  bytecode               bytecode name           format   wide f.   result tp               stk traps  std code

    def(_fast_agetfield      , "fast_agetfield"      , "bJJ"  , null    , BasicType.getTObject() ,  0, true , _getfield       );
    def(_fast_bgetfield      , "fast_bgetfield"      , "bJJ"  , null    , BasicType.getTInt()    ,  0, true , _getfield       );
    def(_fast_cgetfield      , "fast_cgetfield"      , "bJJ"  , null    , BasicType.getTChar()   ,  0, true , _getfield       );
    def(_fast_dgetfield      , "fast_dgetfield"      , "bJJ"  , null    , BasicType.getTDouble() ,  0, true , _getfield       );
    def(_fast_fgetfield      , "fast_fgetfield"      , "bJJ"  , null    , BasicType.getTFloat()  ,  0, true , _getfield       );
    def(_fast_igetfield      , "fast_igetfield"      , "bJJ"  , null    , BasicType.getTInt()    ,  0, true , _getfield       );
    def(_fast_lgetfield      , "fast_lgetfield"      , "bJJ"  , null    , BasicType.getTLong()   ,  0, true , _getfield       );
    def(_fast_sgetfield      , "fast_sgetfield"      , "bJJ"  , null    , BasicType.getTShort()  ,  0, true , _getfield       );

    def(_fast_aputfield      , "fast_aputfield"      , "bJJ"  , null    , BasicType.getTObject() ,  0, true , _putfield       );
    def(_fast_bputfield      , "fast_bputfield"      , "bJJ"  , null    , BasicType.getTInt()    ,  0, true , _putfield       );
    def(_fast_zputfield      , "fast_zputfield"      , "bJJ"  , null    , BasicType.getTInt()    ,  0, true , _putfield       );
    def(_fast_cputfield      , "fast_cputfield"      , "bJJ"  , null    , BasicType.getTChar()   ,  0, true , _putfield       );
    def(_fast_dputfield      , "fast_dputfield"      , "bJJ"  , null    , BasicType.getTDouble() ,  0, true , _putfield       );
    def(_fast_fputfield      , "fast_fputfield"      , "bJJ"  , null    , BasicType.getTFloat()  ,  0, true , _putfield       );
    def(_fast_iputfield      , "fast_iputfield"      , "bJJ"  , null    , BasicType.getTInt()    ,  0, true , _putfield       );
    def(_fast_lputfield      , "fast_lputfield"      , "bJJ"  , null    , BasicType.getTLong()   ,  0, true , _putfield       );
    def(_fast_sputfield      , "fast_sputfield"      , "bJJ"  , null    , BasicType.getTShort()  ,  0, true , _putfield       );

    def(_fast_aload_0        , "fast_aload_0"        , "b"    , null    , BasicType.getTObject() ,  1, true , _aload_0        );
    def(_fast_iaccess_0      , "fast_iaccess_0"      , "b_JJ" , null    , BasicType.getTInt()    ,  1, true , _aload_0        );
    def(_fast_aaccess_0      , "fast_aaccess_0"      , "b_JJ" , null    , BasicType.getTObject() ,  1, true , _aload_0        );
    def(_fast_faccess_0      , "fast_faccess_0"      , "b_JJ" , null    , BasicType.getTObject() ,  1, true , _aload_0        );

    def(_fast_iload          , "fast_iload"          , "bi"   , null    , BasicType.getTInt()    ,  1, false, _iload          );
    def(_fast_iload2         , "fast_iload2"         , "bi_i" , null    , BasicType.getTInt()    ,  2, false, _iload          );
    def(_fast_icaload        , "fast_icaload"        , "bi_"  , null    , BasicType.getTInt()    ,  0, false, _iload          );

    // Faster method invocation.
    def(_fast_invokevfinal   , "fast_invokevfinal"   , "bJJ"  , null    , BasicType.getTIllegal(), -1, true, _invokevirtual   );

    def(_fast_linearswitch   , "fast_linearswitch"   , ""     , null    , BasicType.getTVoid()   , -1, false, _lookupswitch   );
    def(_fast_binaryswitch   , "fast_binaryswitch"   , ""     , null    , BasicType.getTVoid()   , -1, false, _lookupswitch   );
    def(_fast_aldc           , "fast_aldc"           , "bj"   , null    , BasicType.getTObject(),   1, true,  _ldc            );
    def(_fast_aldc_w         , "fast_aldc_w"         , "bJJ"  , null    , BasicType.getTObject(),   1, true,  _ldc_w          );

    def(_return_register_finalizer, "return_register_finalizer", "b"    , null    , BasicType.getTVoid()   , 0, true, _return );

    // special handling of signature-polymorphic methods
    def(_invokehandle        , "invokehandle"        , "bJJ"  , null    , BasicType.getTIllegal(), -1, true, _invokevirtual   );

    // CDS specific. Bytecodes rewritten at CDS dump time
    def(_nofast_getfield     , "_nofast_getfield"    , "bJJ"  , null    , BasicType.getTIllegal() , 0, true,  _getfield );
    def(_nofast_putfield     , "_nofast_putfield"    , "bJJ"  , null    , BasicType.getTIllegal() ,-2, true,  _putfield );
    def(_nofast_aload_0      , "_nofast_aload_0"     , "b"    , null    , BasicType.getTObject()  , 1, true,  _aload_0  );
    def(_nofast_iload        , "_nofast_iload"       , "bi"   , null    , BasicType.getTInt()     , 1, false, _iload    );

    def(_shouldnotreachhere  , "_shouldnotreachhere" , "b"    , null    , BasicType.getTVoid()   ,  0, false);

    if (Assert.ASSERTS_ENABLED) {
      // compare can_trap information for each bytecode with the
      // can_trap information for the corresponding base bytecode
      // (if a rewritten bytecode can trap, so must the base bytecode)
      for (int i = 0; i < number_of_codes; i++) {
        if (isDefined(i)) {
          int j = javaCode(i);
          if (canTrap(i) && !canTrap(j)) {
            Assert.that(false, name(i) + " can trap => " + name(j) + " can trap, too");
          }
        }
      }
    }
  }

  private static void def(int code, String name, String format, String wide_format, int result_type, int depth, boolean can_trap) {
    def(code, name, format, wide_format, result_type, depth, can_trap, code);
  }

  private static void def(int code, String name, String format, String wide_format, int result_type, int depth, boolean can_trap, int java_code) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(wide_format == null || format != null, "short form must exist if there's a wide form");
    }
    int len  = (format      != null ? format.length()      : 0);
    int wlen = (wide_format != null ? wide_format.length() : 0);
    _name          [code] = name;
    _result_type   [code] = result_type;
    _depth         [code] = (byte) depth;
    _lengths       [code] = (byte)((wlen << 4) | (len & 0xF));
    _java_code     [code] = java_code;
    _format        [code] = format;
    _wide_format   [code] = wide_format;
    int bc_flags = 0;
    if (can_trap)           bc_flags |= _bc_can_trap;
    if (java_code != code)  bc_flags |= _bc_can_rewrite;
    _flags[code+0*256] = compute_flags(format,      bc_flags);
    _flags[code+1*256] = compute_flags(wide_format, bc_flags);
  }
}
