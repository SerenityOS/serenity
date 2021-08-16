/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _NSK_SHARE_JVMTI_INJECTOR_H_
#define _NSK_SHARE_JVMTI_INJECTOR_H_

/* Class File Format Constants
 */

typedef unsigned char   u1;
typedef unsigned short  u2;
typedef unsigned int    u4;

/* Constant table */
enum {
    CONSTANT_Utf8                = 1,
    CONSTANT_Integer             = 3,
    CONSTANT_Float               = 4,
    CONSTANT_Long                = 5,
    CONSTANT_Double              = 6,
    CONSTANT_Class               = 7,
    CONSTANT_String              = 8,
    CONSTANT_Fieldref            = 9,
    CONSTANT_Methodref           = 10,
    CONSTANT_InterfaceMethodref  = 11,
    CONSTANT_NameAndType         = 12,
    CONSTANT_MethodHandle        = 15,
    CONSTANT_MethodType          = 16,
    CONSTANT_InvokeDynamic       = 18
};

/* Access and modifier flags */
enum {
    ACC_PUBLIC                   = 0x00000001,
    ACC_PRIVATE                  = 0x00000002,
    ACC_PROTECTED                = 0x00000004,
    ACC_STATIC                   = 0x00000008,
    ACC_FINAL                    = 0x00000010,
    ACC_SYNCHRONIZED             = 0x00000020,
    ACC_VOLATILE                 = 0x00000040,
    ACC_TRANSIENT                = 0x00000080,
    ACC_NATIVE                   = 0x00000100,
    ACC_INTERFACE                = 0x00000200,
    ACC_ABSTRACT                 = 0x00000400,
    ACC_SUPER                    = 0x00000020
};

/* Opcodes */
enum {
    opc_nop                      = 0,
    opc_aconst_null              = 1,
    opc_iconst_m1                = 2,
    opc_iconst_0                 = 3,
    opc_iconst_1                 = 4,
    opc_iconst_2                 = 5,
    opc_iconst_3                 = 6,
    opc_iconst_4                 = 7,
    opc_iconst_5                 = 8,
    opc_lconst_0                 = 9,
    opc_lconst_1                 = 10,
    opc_fconst_0                 = 11,
    opc_fconst_1                 = 12,
    opc_fconst_2                 = 13,
    opc_dconst_0                 = 14,
    opc_dconst_1                 = 15,
    opc_bipush                   = 16,
    opc_sipush                   = 17,
    opc_ldc                      = 18,
    opc_ldc_w                    = 19,
    opc_ldc2_w                   = 20,
    opc_iload                    = 21,
    opc_lload                    = 22,
    opc_fload                    = 23,
    opc_dload                    = 24,
    opc_aload                    = 25,
    opc_iload_0                  = 26,
    opc_iload_1                  = 27,
    opc_iload_2                  = 28,
    opc_iload_3                  = 29,
    opc_lload_0                  = 30,
    opc_lload_1                  = 31,
    opc_lload_2                  = 32,
    opc_lload_3                  = 33,
    opc_fload_0                  = 34,
    opc_fload_1                  = 35,
    opc_fload_2                  = 36,
    opc_fload_3                  = 37,
    opc_dload_0                  = 38,
    opc_dload_1                  = 39,
    opc_dload_2                  = 40,
    opc_dload_3                  = 41,
    opc_aload_0                  = 42,
    opc_aload_1                  = 43,
    opc_aload_2                  = 44,
    opc_aload_3                  = 45,
    opc_iaload                   = 46,
    opc_laload                   = 47,
    opc_faload                   = 48,
    opc_daload                   = 49,
    opc_aaload                   = 50,
    opc_baload                   = 51,
    opc_caload                   = 52,
    opc_saload                   = 53,
    opc_istore                   = 54,
    opc_lstore                   = 55,
    opc_fstore                   = 56,
    opc_dstore                   = 57,
    opc_astore                   = 58,
    opc_istore_0                 = 59,
    opc_istore_1                 = 60,
    opc_istore_2                 = 61,
    opc_istore_3                 = 62,
    opc_lstore_0                 = 63,
    opc_lstore_1                 = 64,
    opc_lstore_2                 = 65,
    opc_lstore_3                 = 66,
    opc_fstore_0                 = 67,
    opc_fstore_1                 = 68,
    opc_fstore_2                 = 69,
    opc_fstore_3                 = 70,
    opc_dstore_0                 = 71,
    opc_dstore_1                 = 72,
    opc_dstore_2                 = 73,
    opc_dstore_3                 = 74,
    opc_astore_0                 = 75,
    opc_astore_1                 = 76,
    opc_astore_2                 = 77,
    opc_astore_3                 = 78,
    opc_iastore                  = 79,
    opc_lastore                  = 80,
    opc_fastore                  = 81,
    opc_dastore                  = 82,
    opc_aastore                  = 83,
    opc_bastore                  = 84,
    opc_castore                  = 85,
    opc_sastore                  = 86,
    opc_pop                      = 87,
    opc_pop2                     = 88,
    opc_dup                      = 89,
    opc_dup_x1                   = 90,
    opc_dup_x2                   = 91,
    opc_dup2                     = 92,
    opc_dup2_x1                  = 93,
    opc_dup2_x2                  = 94,
    opc_swap                     = 95,
    opc_iadd                     = 96,
    opc_ladd                     = 97,
    opc_fadd                     = 98,
    opc_dadd                     = 99,
    opc_isub                     = 100,
    opc_lsub                     = 101,
    opc_fsub                     = 102,
    opc_dsub                     = 103,
    opc_imul                     = 104,
    opc_lmul                     = 105,
    opc_fmul                     = 106,
    opc_dmul                     = 107,
    opc_idiv                     = 108,
    opc_ldiv                     = 109,
    opc_fdiv                     = 110,
    opc_ddiv                     = 111,
    opc_irem                     = 112,
    opc_lrem                     = 113,
    opc_frem                     = 114,
    opc_drem                     = 115,
    opc_ineg                     = 116,
    opc_lneg                     = 117,
    opc_fneg                     = 118,
    opc_dneg                     = 119,
    opc_ishl                     = 120,
    opc_lshl                     = 121,
    opc_ishr                     = 122,
    opc_lshr                     = 123,
    opc_iushr                    = 124,
    opc_lushr                    = 125,
    opc_iand                     = 126,
    opc_land                     = 127,
    opc_ior                      = 128,
    opc_lor                      = 129,
    opc_ixor                     = 130,
    opc_lxor                     = 131,
    opc_iinc                     = 132,
    opc_i2l                      = 133,
    opc_i2f                      = 134,
    opc_i2d                      = 135,
    opc_l2i                      = 136,
    opc_l2f                      = 137,
    opc_l2d                      = 138,
    opc_f2i                      = 139,
    opc_f2l                      = 140,
    opc_f2d                      = 141,
    opc_d2i                      = 142,
    opc_d2l                      = 143,
    opc_d2f                      = 144,
    opc_i2b                      = 145,
    opc_i2c                      = 146,
    opc_i2s                      = 147,
    opc_lcmp                     = 148,
    opc_fcmpl                    = 149,
    opc_fcmpg                    = 150,
    opc_dcmpl                    = 151,
    opc_dcmpg                    = 152,
    opc_ifeq                     = 153,
    opc_ifne                     = 154,
    opc_iflt                     = 155,
    opc_ifge                     = 156,
    opc_ifgt                     = 157,
    opc_ifle                     = 158,
    opc_if_icmpeq                = 159,
    opc_if_icmpne                = 160,
    opc_if_icmplt                = 161,
    opc_if_icmpge                = 162,
    opc_if_icmpgt                = 163,
    opc_if_icmple                = 164,
    opc_if_acmpeq                = 165,
    opc_if_acmpne                = 166,
    opc_goto                     = 167,
    opc_jsr                      = 168,
    opc_ret                      = 169,
    opc_tableswitch              = 170,
    opc_lookupswitch             = 171,
    opc_ireturn                  = 172,
    opc_lreturn                  = 173,
    opc_freturn                  = 174,
    opc_dreturn                  = 175,
    opc_areturn                  = 176,
    opc_return                   = 177,
    opc_getstatic                = 178,
    opc_putstatic                = 179,
    opc_getfield                 = 180,
    opc_putfield                 = 181,
    opc_invokevirtual            = 182,
    opc_invokespecial            = 183,
    opc_invokestatic             = 184,
    opc_invokeinterface          = 185,
    opc_invokedynamic            = 186,
    opc_new                      = 187,
    opc_newarray                 = 188,
    opc_anewarray                = 189,
    opc_arraylength              = 190,
    opc_athrow                   = 191,
    opc_checkcast                = 192,
    opc_instanceof               = 193,
    opc_monitorenter             = 194,
    opc_monitorexit              = 195,
    opc_wide                     = 196,
    opc_multianewarray           = 197,
    opc_ifnull                   = 198,
    opc_ifnonnull                = 199,
    opc_goto_w                   = 200,
    opc_jsr_w                    = 201,
    opc_breakpoint               = 202
};

enum {
    BCI_MODE_EMCP   = 0,
    BCI_MODE_CALL   = 1,
    BCI_MODE_ALLOC  = 2
};

extern "C" {

/**
 * Class file transformer. Transforms a classfile image from old_bytes
 * to a new classfile image new_bytes according to value of bci_mode.
 * The new classfile image is allocated with malloc(), and should be
 * freed by the caller. The possible bci_mode values:
 *
 *  BCI_MODE_EMCP
 *      dummy, without injection any bytecodes
 *
 *  BCI_MODE_CALL
 *      inject invokestatic call to ProfileCollector.callTracker()
 *      at the beginning of all methods
 *
 *    BCI_MODE_ALLOC
 *      inject invokestatic call to ProfileCollector.allocTracker()
 *      immediately following new/newarray opcodes.
 *
 */

int Inject(const u1* old_bytes, const jint old_length,
    u1** new_bytes, jint* new_length, int bci_mode);

}

#endif /* _NSK_SHARE_JVMTI_INJECTOR_H_ */
