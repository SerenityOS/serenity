/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

public interface RuntimeConstants {

    /* Signature Characters */
    char   SIGC_VOID                  = 'V';
    String SIG_VOID                   = "V";
    char   SIGC_BOOLEAN               = 'Z';
    String SIG_BOOLEAN                = "Z";
    char   SIGC_BYTE                  = 'B';
    String SIG_BYTE                   = "B";
    char   SIGC_CHAR                  = 'C';
    String SIG_CHAR                   = "C";
    char   SIGC_SHORT                 = 'S';
    String SIG_SHORT                  = "S";
    char   SIGC_INT                   = 'I';
    String SIG_INT                    = "I";
    char   SIGC_LONG                  = 'J';
    String SIG_LONG                   = "J";
    char   SIGC_FLOAT                 = 'F';
    String SIG_FLOAT                  = "F";
    char   SIGC_DOUBLE                = 'D';
    String SIG_DOUBLE                 = "D";
    char   SIGC_ARRAY                 = '[';
    String SIG_ARRAY                  = "[";
    char   SIGC_CLASS                 = 'L';
    String SIG_CLASS                  = "L";
    char   SIGC_METHOD                = '(';
    String SIG_METHOD                 = "(";
    char   SIGC_ENDCLASS              = ';';
    String SIG_ENDCLASS               = ";";
    char   SIGC_ENDMETHOD             = ')';
    String SIG_ENDMETHOD              = ")";
    char   SIGC_PACKAGE               = '/';
    String SIG_PACKAGE                = "/";

    /* Class File Constants */
    int JAVA_MAGIC                   = 0xcafebabe;
    int JAVA_MIN_SUPPORTED_VERSION   = 45;
    int JAVA_MAX_SUPPORTED_VERSION   = 48;
    int JAVA_MAX_SUPPORTED_MINOR_VERSION = 0;

    /* Generate class file version for 1.1  by default */
    int JAVA_DEFAULT_VERSION         = 45;
    int JAVA_DEFAULT_MINOR_VERSION   = 3;

    /* Constant table */
    int CONSTANT_UTF8                = 1;
    int CONSTANT_UNICODE             = 2;
    int CONSTANT_INTEGER             = 3;
    int CONSTANT_FLOAT               = 4;
    int CONSTANT_LONG                = 5;
    int CONSTANT_DOUBLE              = 6;
    int CONSTANT_CLASS               = 7;
    int CONSTANT_STRING              = 8;
    int CONSTANT_FIELD               = 9;
    int CONSTANT_METHOD              = 10;
    int CONSTANT_INTERFACEMETHOD     = 11;
    int CONSTANT_NAMEANDTYPE         = 12;

    /* Access and modifier flags */
    int ACC_PUBLIC                   = 0x00000001;
    int ACC_PRIVATE                  = 0x00000002;
    int ACC_PROTECTED                = 0x00000004;
    int ACC_STATIC                   = 0x00000008;
    int ACC_FINAL                    = 0x00000010;
    int ACC_SYNCHRONIZED             = 0x00000020;
    int ACC_VOLATILE                 = 0x00000040;
    int ACC_TRANSIENT                = 0x00000080;
    int ACC_NATIVE                   = 0x00000100;
    int ACC_INTERFACE                = 0x00000200;
    int ACC_ABSTRACT                 = 0x00000400;
    int ACC_SUPER                    = 0x00000020;
    int ACC_STRICT           = 0x00000800;

    /* Type codes */
    int T_CLASS                      = 0x00000002;
    int T_BOOLEAN                    = 0x00000004;
    int T_CHAR                       = 0x00000005;
    int T_FLOAT                      = 0x00000006;
    int T_DOUBLE                     = 0x00000007;
    int T_BYTE                       = 0x00000008;
    int T_SHORT                      = 0x00000009;
    int T_INT                        = 0x0000000a;
    int T_LONG                       = 0x0000000b;

    /* Opcodes */
    int opc_try                      = -3;
    int opc_dead                     = -2;
    int opc_label                    = -1;
    int opc_nop                      = 0;
    int opc_aconst_null              = 1;
    int opc_iconst_m1                = 2;
    int opc_iconst_0                 = 3;
    int opc_iconst_1                 = 4;
    int opc_iconst_2                 = 5;
    int opc_iconst_3                 = 6;
    int opc_iconst_4                 = 7;
    int opc_iconst_5                 = 8;
    int opc_lconst_0                 = 9;
    int opc_lconst_1                 = 10;
    int opc_fconst_0                 = 11;
    int opc_fconst_1                 = 12;
    int opc_fconst_2                 = 13;
    int opc_dconst_0                 = 14;
    int opc_dconst_1                 = 15;
    int opc_bipush                   = 16;
    int opc_sipush                   = 17;
    int opc_ldc                      = 18;
    int opc_ldc_w                    = 19;
    int opc_ldc2_w                   = 20;
    int opc_iload                    = 21;
    int opc_lload                    = 22;
    int opc_fload                    = 23;
    int opc_dload                    = 24;
    int opc_aload                    = 25;
    int opc_iload_0                  = 26;
    int opc_iload_1                  = 27;
    int opc_iload_2                  = 28;
    int opc_iload_3                  = 29;
    int opc_lload_0                  = 30;
    int opc_lload_1                  = 31;
    int opc_lload_2                  = 32;
    int opc_lload_3                  = 33;
    int opc_fload_0                  = 34;
    int opc_fload_1                  = 35;
    int opc_fload_2                  = 36;
    int opc_fload_3                  = 37;
    int opc_dload_0                  = 38;
    int opc_dload_1                  = 39;
    int opc_dload_2                  = 40;
    int opc_dload_3                  = 41;
    int opc_aload_0                  = 42;
    int opc_aload_1                  = 43;
    int opc_aload_2                  = 44;
    int opc_aload_3                  = 45;
    int opc_iaload                   = 46;
    int opc_laload                   = 47;
    int opc_faload                   = 48;
    int opc_daload                   = 49;
    int opc_aaload                   = 50;
    int opc_baload                   = 51;
    int opc_caload                   = 52;
    int opc_saload                   = 53;
    int opc_istore                   = 54;
    int opc_lstore                   = 55;
    int opc_fstore                   = 56;
    int opc_dstore                   = 57;
    int opc_astore                   = 58;
    int opc_istore_0                 = 59;
    int opc_istore_1                 = 60;
    int opc_istore_2                 = 61;
    int opc_istore_3                 = 62;
    int opc_lstore_0                 = 63;
    int opc_lstore_1                 = 64;
    int opc_lstore_2                 = 65;
    int opc_lstore_3                 = 66;
    int opc_fstore_0                 = 67;
    int opc_fstore_1                 = 68;
    int opc_fstore_2                 = 69;
    int opc_fstore_3                 = 70;
    int opc_dstore_0                 = 71;
    int opc_dstore_1                 = 72;
    int opc_dstore_2                 = 73;
    int opc_dstore_3                 = 74;
    int opc_astore_0                 = 75;
    int opc_astore_1                 = 76;
    int opc_astore_2                 = 77;
    int opc_astore_3                 = 78;
    int opc_iastore                  = 79;
    int opc_lastore                  = 80;
    int opc_fastore                  = 81;
    int opc_dastore                  = 82;
    int opc_aastore                  = 83;
    int opc_bastore                  = 84;
    int opc_castore                  = 85;
    int opc_sastore                  = 86;
    int opc_pop                      = 87;
    int opc_pop2                     = 88;
    int opc_dup                      = 89;
    int opc_dup_x1                   = 90;
    int opc_dup_x2                   = 91;
    int opc_dup2                     = 92;
    int opc_dup2_x1                  = 93;
    int opc_dup2_x2                  = 94;
    int opc_swap                     = 95;
    int opc_iadd                     = 96;
    int opc_ladd                     = 97;
    int opc_fadd                     = 98;
    int opc_dadd                     = 99;
    int opc_isub                     = 100;
    int opc_lsub                     = 101;
    int opc_fsub                     = 102;
    int opc_dsub                     = 103;
    int opc_imul                     = 104;
    int opc_lmul                     = 105;
    int opc_fmul                     = 106;
    int opc_dmul                     = 107;
    int opc_idiv                     = 108;
    int opc_ldiv                     = 109;
    int opc_fdiv                     = 110;
    int opc_ddiv                     = 111;
    int opc_irem                     = 112;
    int opc_lrem                     = 113;
    int opc_frem                     = 114;
    int opc_drem                     = 115;
    int opc_ineg                     = 116;
    int opc_lneg                     = 117;
    int opc_fneg                     = 118;
    int opc_dneg                     = 119;
    int opc_ishl                     = 120;
    int opc_lshl                     = 121;
    int opc_ishr                     = 122;
    int opc_lshr                     = 123;
    int opc_iushr                    = 124;
    int opc_lushr                    = 125;
    int opc_iand                     = 126;
    int opc_land                     = 127;
    int opc_ior                      = 128;
    int opc_lor                      = 129;
    int opc_ixor                     = 130;
    int opc_lxor                     = 131;
    int opc_iinc                     = 132;
    int opc_i2l                      = 133;
    int opc_i2f                      = 134;
    int opc_i2d                      = 135;
    int opc_l2i                      = 136;
    int opc_l2f                      = 137;
    int opc_l2d                      = 138;
    int opc_f2i                      = 139;
    int opc_f2l                      = 140;
    int opc_f2d                      = 141;
    int opc_d2i                      = 142;
    int opc_d2l                      = 143;
    int opc_d2f                      = 144;
    int opc_i2b                      = 145;
    int opc_i2c                      = 146;
    int opc_i2s                      = 147;
    int opc_lcmp                     = 148;
    int opc_fcmpl                    = 149;
    int opc_fcmpg                    = 150;
    int opc_dcmpl                    = 151;
    int opc_dcmpg                    = 152;
    int opc_ifeq                     = 153;
    int opc_ifne                     = 154;
    int opc_iflt                     = 155;
    int opc_ifge                     = 156;
    int opc_ifgt                     = 157;
    int opc_ifle                     = 158;
    int opc_if_icmpeq                = 159;
    int opc_if_icmpne                = 160;
    int opc_if_icmplt                = 161;
    int opc_if_icmpge                = 162;
    int opc_if_icmpgt                = 163;
    int opc_if_icmple                = 164;
    int opc_if_acmpeq                = 165;
    int opc_if_acmpne                = 166;
    int opc_goto                     = 167;
    int opc_jsr                      = 168;
    int opc_ret                      = 169;
    int opc_tableswitch              = 170;
    int opc_lookupswitch             = 171;
    int opc_ireturn                  = 172;
    int opc_lreturn                  = 173;
    int opc_freturn                  = 174;
    int opc_dreturn                  = 175;
    int opc_areturn                  = 176;
    int opc_return                   = 177;
    int opc_getstatic                = 178;
    int opc_putstatic                = 179;
    int opc_getfield                 = 180;
    int opc_putfield                 = 181;
    int opc_invokevirtual            = 182;
    int opc_invokespecial            = 183;
    int opc_invokestatic             = 184;
    int opc_invokeinterface          = 185;
    int opc_xxxunusedxxx             = 186;
    int opc_new                      = 187;
    int opc_newarray                 = 188;
    int opc_anewarray                = 189;
    int opc_arraylength              = 190;
    int opc_athrow                   = 191;
    int opc_checkcast                = 192;
    int opc_instanceof               = 193;
    int opc_monitorenter             = 194;
    int opc_monitorexit              = 195;
    int opc_wide                     = 196;
    int opc_multianewarray           = 197;
    int opc_ifnull                   = 198;
    int opc_ifnonnull                = 199;
    int opc_goto_w                   = 200;
    int opc_jsr_w                    = 201;
    int opc_breakpoint               = 202;

    /* Opcode Names */
    String opcNames[] = {
    "nop",
    "aconst_null",
    "iconst_m1",
    "iconst_0",
    "iconst_1",
    "iconst_2",
    "iconst_3",
    "iconst_4",
    "iconst_5",
    "lconst_0",
    "lconst_1",
    "fconst_0",
    "fconst_1",
    "fconst_2",
    "dconst_0",
    "dconst_1",
    "bipush",
    "sipush",
    "ldc",
    "ldc_w",
    "ldc2_w",
    "iload",
    "lload",
    "fload",
    "dload",
    "aload",
    "iload_0",
    "iload_1",
    "iload_2",
    "iload_3",
    "lload_0",
    "lload_1",
    "lload_2",
    "lload_3",
    "fload_0",
    "fload_1",
    "fload_2",
    "fload_3",
    "dload_0",
    "dload_1",
    "dload_2",
    "dload_3",
    "aload_0",
    "aload_1",
    "aload_2",
    "aload_3",
    "iaload",
    "laload",
    "faload",
    "daload",
    "aaload",
    "baload",
    "caload",
    "saload",
    "istore",
    "lstore",
    "fstore",
    "dstore",
    "astore",
    "istore_0",
    "istore_1",
    "istore_2",
    "istore_3",
    "lstore_0",
    "lstore_1",
    "lstore_2",
    "lstore_3",
    "fstore_0",
    "fstore_1",
    "fstore_2",
    "fstore_3",
    "dstore_0",
    "dstore_1",
    "dstore_2",
    "dstore_3",
    "astore_0",
    "astore_1",
    "astore_2",
    "astore_3",
    "iastore",
    "lastore",
    "fastore",
    "dastore",
    "aastore",
    "bastore",
    "castore",
    "sastore",
    "pop",
    "pop2",
    "dup",
    "dup_x1",
    "dup_x2",
    "dup2",
    "dup2_x1",
    "dup2_x2",
    "swap",
    "iadd",
    "ladd",
    "fadd",
    "dadd",
    "isub",
    "lsub",
    "fsub",
    "dsub",
    "imul",
    "lmul",
    "fmul",
    "dmul",
    "idiv",
    "ldiv",
    "fdiv",
    "ddiv",
    "irem",
    "lrem",
    "frem",
    "drem",
    "ineg",
    "lneg",
    "fneg",
    "dneg",
    "ishl",
    "lshl",
    "ishr",
    "lshr",
    "iushr",
    "lushr",
    "iand",
    "land",
    "ior",
    "lor",
    "ixor",
    "lxor",
    "iinc",
    "i2l",
    "i2f",
    "i2d",
    "l2i",
    "l2f",
    "l2d",
    "f2i",
    "f2l",
    "f2d",
    "d2i",
    "d2l",
    "d2f",
    "i2b",
    "i2c",
    "i2s",
    "lcmp",
    "fcmpl",
    "fcmpg",
    "dcmpl",
    "dcmpg",
    "ifeq",
    "ifne",
    "iflt",
    "ifge",
    "ifgt",
    "ifle",
    "if_icmpeq",
    "if_icmpne",
    "if_icmplt",
    "if_icmpge",
    "if_icmpgt",
    "if_icmple",
    "if_acmpeq",
    "if_acmpne",
    "goto",
    "jsr",
    "ret",
    "tableswitch",
    "lookupswitch",
    "ireturn",
    "lreturn",
    "freturn",
    "dreturn",
    "areturn",
    "return",
    "getstatic",
    "putstatic",
    "getfield",
    "putfield",
    "invokevirtual",
    "invokespecial",
    "invokestatic",
    "invokeinterface",
    "xxxunusedxxx",
    "new",
    "newarray",
    "anewarray",
    "arraylength",
    "athrow",
    "checkcast",
    "instanceof",
    "monitorenter",
    "monitorexit",
    "wide",
    "multianewarray",
    "ifnull",
    "ifnonnull",
    "goto_w",
    "jsr_w",
    "breakpoint"
    };

    /* Opcode Lengths */
    int opcLengths[] = {
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    2,
    3,
    2,
    3,
    3,
    2,
    2,
    2,
    2,
    2,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    2,
    2,
    2,
    2,
    2,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    3,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    2,
    99,
    99,
    1,
    1,
    1,
    1,
    1,
    1,
    3,
    3,
    3,
    3,
    3,
    3,
    3,
    5,
    0,
    3,
    2,
    3,
    1,
    1,
    3,
    3,
    1,
    1,
    0,
    4,
    3,
    3,
    5,
    5,
    1
    };

}
