/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "Injector.h"

/* ========================================================================== */

/* Opcode Lengths */
static const u1 opcLengths[] = {
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3, /*   0- 19 */
    3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, /*  20- 39 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, /*  40- 59 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, /*  60- 79 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, /*  80- 99 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, /* 100-119 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, /* 120-139 */
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, /* 140-159 */
    3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0, 0, 1, 1, 1, 1, 1, 1, 3, 3, /* 160-179 */
    3, 3, 3, 3, 3, 5, 5, 3, 2, 3, 1, 1, 3, 3, 1, 1, 0, 4, 3, 3, /* 180-199 */
    5, 5, 1                                                     /* 200-    */
};

static const int GROWTH_FACTOR = 2;
static const char* codeAttributeName = "Code";
static const char* lineNumberAttributeName = "LineNumberTable";
static const char* localVarAttributeName = "LocalVariableTable";
static const char* localVarTypeAttributeName = "LocalVariableTypeTable";
static const char* stackMapAttributeName= "StackMapTable";

static u2 codeAttributeIndex;
static u2 lineNumberAttributeIndex;
static u2 localVarAttributeIndex;
static u2 localVarTypeAttributeIndex;
static u2 stackMapAttributeIndex;

static const u1 SAME_BEGIN = 0;
static const u1 SAME_END = 63;

static const u1 SAME_LOCALS_1_STACK_ITEM_BEGIN = 64;
static const u1 SAME_LOCALS_1_STACK_ITEM_END = 127;

//Tags in the range [128-246] are reserved for future use.

static const u1 SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;

static const u1 CHOP_BEGIN = 248;
static const u1 CHOP_END = 250;

static const u1 SAME_FRAME_EXTENDED = 251;

static const u1 APPEND_BEGIN = 252;
static const u1 APPEND_END = 254;

static const u1 FULL_FRAME = 255;

static const u1 ITEM_Object = 7;
static const u1 ITEM_Uninitialized = 8;

static u2 stackFrameOffset = 0;

static int mode;
static const u1* orig;
static u1* gen;

static u1* inputPos;
static const u1* endPos;
static u1* genPos;
static u1* markPos;

static char** constantPool;
static u2 constantPoolSize;
static u2 constantPoolCount;

static u1 callBytes[] = { opc_invokestatic, 0, 0 };
static u1 allocBytes[] = { opc_invokestatic, 0, 0 };
static u1 zeroBytes[] = { 0, 0, 0 };

static u4 codeLength;
static u4* map;
static jbyte* widening;

/* ========================================================================== */

extern "C" {

    static u1 get_u1() {
        return *inputPos++;
    }

    static u2 get_u2() {
        u1* p = inputPos;
        inputPos += 2;
        return (u2)p[1] | ((u2)p[0]<<8);
    }

    static u4 get_u4() {
        u1* p = inputPos;
        inputPos += 4;
        return (u4)p[3] | ((u4)p[2]<<8) | ((u4)p[1]<<16) | ((u4)p[0]<<24);
    }

    static void put_u1(u1 v) {
        *genPos++ = v;
    }

    static void put_u2(u2 v) {
        *genPos++ = (u1)(v>>8);
        *genPos++ = (u1)(v & 0xFF);
    }

    static void put_u4(u4 v) {
        *genPos++ = (u1)(v>>24);
        *genPos++ = (u1)(v>>16);
        *genPos++ = (u1)(v>>8);
        *genPos++ = (u1)(v & 0xFF);
    }

    static void set_u4(u1* pos, u4 v) {
        *pos++ = (u1)(v>>24);
        *pos++ = (u1)(v>>16);
        *pos++ = (u1)(v>>8);
        *pos++ = (u1)(v & 0xFF);
    }

    static u1 copy_u1() {
        u1 v = get_u1();
        put_u1(v);
        return v;
    }

    static u2 copy_u2() {
        u2 v = get_u2();
        put_u2(v);
        return v;
    }

    static u4 copy_u4() {
        u4 v = get_u4();
        put_u4(v);
        return v;
    }

    static void copy(int count) {
        memcpy(genPos, inputPos, count);
        inputPos += count;
        genPos += count;
    }

    static void skip(int count) {
        inputPos += count;
    }

    static void get(u1* bytes, int count) {
        memcpy(bytes, inputPos, count);
        inputPos += count;
    }

    static void put(u1* bytes, int count) {
        memcpy(genPos, bytes, count);
        genPos += count;
    }

    static void markLocalPositionStart() {
        markPos = inputPos;
    }

    static u4 localPosition() {
        return (u4) (inputPos - markPos);
    }

    void recallPosition() {
        inputPos = markPos;
    }

    static u4 generatedPosition() {
        return (u4) (genPos - gen);
    }

    static void randomAccessWriteU2(int pos, u2 v) {
        gen[pos]   = (u1)(v>>8);
        gen[pos+1] = (u1)(v & 0xFF);
    }

    static void randomAccessWriteU4(int pos, u4 v) {
        gen[pos]   = (u1)(v>>24);
        gen[pos+1] = (u1)(v>>16);
        gen[pos+2] = (u1)(v>>8);
        gen[pos+3] = (u1)(v & 0xFF);
    }

    static int copyConstantPool(u2 constantPoolCount) {
        u2 i;
        u2 len;
        char* utf8;

        constantPoolSize = constantPoolCount;

        NSK_DISPLAY1("copying ConstantPool: %d\n", constantPoolSize);
        constantPool = (char**) malloc(constantPoolSize * sizeof(char*));
        if (!NSK_VERIFY(constantPool != NULL)) {
            NSK_COMPLAIN0("out of memory\n");
            return NSK_FALSE;
        }

        memset(constantPool, 0, constantPoolSize * sizeof(char*));

        codeAttributeIndex = 0;
        lineNumberAttributeIndex = 0;
        localVarAttributeIndex = 0;
        localVarTypeAttributeIndex = 0;
        stackMapAttributeIndex = 0;

        for (i = 1; i < constantPoolSize; i++) {
            u1 tag = copy_u1();
            switch (tag) {
            case CONSTANT_Class:
            case CONSTANT_String:
            case CONSTANT_MethodType:
                copy(2);
                break;
            case CONSTANT_MethodHandle:
                copy(3);
                break;
            case CONSTANT_Fieldref:
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Integer:
            case CONSTANT_Float:
            case CONSTANT_NameAndType:
            case CONSTANT_InvokeDynamic:
                copy(4);
                break;
            case CONSTANT_Long:
            case CONSTANT_Double:
                copy(8);
                i++; /* takes two CP entries */
                break;
            case CONSTANT_Utf8:
                len = copy_u2();
                utf8 = (char*) malloc(len + 1);
                if (!NSK_VERIFY(utf8 != NULL)) {
                    NSK_COMPLAIN0("out of memory\n");
                    return NSK_FALSE;
                }
                get((u1*) utf8, len);
                utf8[len] = 0;
                constantPool[i] = utf8;
                if (strcmp(utf8, codeAttributeName) == 0) {
                    codeAttributeIndex = i;
                } else if (strcmp(utf8, lineNumberAttributeName) == 0) {
                    lineNumberAttributeIndex = i;
                } else if (strcmp(utf8, localVarAttributeName) == 0) {
                    localVarAttributeIndex = i;
                } else if (strcmp(utf8, localVarTypeAttributeName) == 0) {
                    localVarTypeAttributeIndex = i;
                } else if (strcmp(utf8, stackMapAttributeName) == 0) {
                    stackMapAttributeIndex = i;
                }
                put((u1*) utf8, len);
                break;
            default:
                NSK_COMPLAIN2("%d unexpected constant pool tag: %d\n", i, tag);
                return NSK_FALSE;
            }
        }

        return NSK_TRUE;
    }

    static void freeConstantPool() {
        u2 i;

        for (i = 1; i < constantPoolSize; i++) {
            if (constantPool[i] != NULL) {
                free(constantPool[i]);
            }
        }

        free(constantPool);
    }

    /* ========================================================================== */

    /* Copy the exception table for this method code */
    static void copyExceptionTable() {
        u2 tableLength;
        u2 i;

        tableLength = copy_u2();
        NSK_DISPLAY1("ExceptionTable length: %d\n", tableLength);
        for (i = tableLength; i > 0; i--) {
            put_u2((u2) map[get_u2()]); /* start_pc */
            put_u2((u2) map[get_u2()]); /* end_pc */
            put_u2((u2) map[get_u2()]); /* handler_pc */
            copy(2);                    /* catch_type */
        }
    }

    /* Copy the line number table for this method code */
    static void copyLineNumberAttr() {
        u2 tableLength;
        u2 i;

        copy(4); /* attr len */

        tableLength = copy_u2();

        NSK_DISPLAY1("LineNumberTable length: %d\n", tableLength);
        for (i = tableLength; i > 0; i--) {
            put_u2((u2) map[get_u2()]); /* start_pc */
            copy(2);                    /* line_number */
        }
    }

    /* Copy the local variable table for this method code */
    static void copyLocalVarAttr() {
        u2 tableLength;
        u2 startPC;
        u2 i;

        copy(4); /* attr len */

        tableLength = copy_u2();

        NSK_DISPLAY1("LocalVariableTable length: %d\n", tableLength);
        for (i = tableLength; i > 0; i--) {
            startPC = get_u2();
            put_u2((u2) map[startPC]);  /* start_pc */
            put_u2((u2) (map[startPC + get_u2()] - map[startPC])); /* length */
            copy(6); /* name_index, descriptor_index, index */
        }
    }

    /* Copy the local variable type table for this method code */
    static void copyLocalVarTypeAttr() {
        u2 tableLength;
        u2 startPC;
        u2 i;

        copy(4); /* attr len */

        tableLength = copy_u2();

        NSK_DISPLAY1("LocalVariableTypeTable length: %d\n", tableLength);
        for (i = tableLength; i > 0; i--) {
            startPC = get_u2();
            put_u2((u2) map[startPC]);  /* start_pc */
            put_u2((u2) (map[startPC + get_u2()] - map[startPC])); /* length */
            copy(6); /* name_index, signature_index, index */
        }
    }

    static u2 calculateOffsetDelta(u2 frameNumber, u2 frameOffsetDelta) {
        u2 oldOffset;
        u2 newOffset;
        if (frameNumber == 0) {
            stackFrameOffset = frameOffsetDelta;
            return (u2) map[stackFrameOffset];
        } else {
            oldOffset = (u2) map[stackFrameOffset];
            stackFrameOffset = stackFrameOffset + frameOffsetDelta + 1;
            newOffset = (u2) map[stackFrameOffset - 1];
            return newOffset - oldOffset;
        }
    }

    static void copyVerificationTypeInfo(u2 count) {
        u2 i;
        u2 offset;
        u1 tag;
        for (i=0; i<count; i++) {
            tag = get_u1();
            put_u1(tag);
            if (tag == ITEM_Object) {
                copy_u2();
            } else if (tag == ITEM_Uninitialized) {
                copy_u2();
                offset = get_u2();
                put_u2((u2)map[offset]);
            }
        }
    }

    static void copyStackMapAttr() {
        u2 number_of_entries;
        u2 i;
        u4 len;
        unsigned int frame_type;
        u2 frameOffsetDelta;
        u2 number_of_stack_items;
        u2 number_of_locals;
        u1* lenPtr = genPos;

        len=copy_u4(); /* attr len */

        number_of_entries = copy_u2();



        for (i=0; i<number_of_entries; i++) {
            frame_type = get_u1();

            if (frame_type <= SAME_END) {
                // same_frame {
                //        u1 frame_type = SAME; /* 0-63 */
                // }

                put_u1(SAME_FRAME_EXTENDED);
                put_u2(calculateOffsetDelta(i, (u2) frame_type));

            } else if ((frame_type >= SAME_LOCALS_1_STACK_ITEM_BEGIN) && (frame_type <= SAME_LOCALS_1_STACK_ITEM_END)) {
                // same_locals_1_stack_item_frame {
                //         u1 frame_type = SAME_LOCALS_1_STACK_ITEM;/* 64-127 */
                //         verification_type_info stack[1];
                // }

                put_u1(SAME_LOCALS_1_STACK_ITEM_EXTENDED);
                put_u2(calculateOffsetDelta(i, (u2) (frame_type-64)));
                copyVerificationTypeInfo(1);

                // Tags in the range [128-246] are reserved for future use.
            } else if (frame_type == SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
                // same_locals_1_stack_item_frame_extended {
                //     u1 frame_type = SAME_LOCALS_1_STACK_ITEM_EXTENDED; /* 247 */
                //     u2 offset_delta;
                //     verification_type_info stack[1];
                // }

                put_u1(SAME_LOCALS_1_STACK_ITEM_EXTENDED);
                frameOffsetDelta = get_u2();
                put_u2(calculateOffsetDelta(i, frameOffsetDelta));
                copyVerificationTypeInfo(1);

            } else if ((frame_type >= CHOP_BEGIN) && (frame_type <= CHOP_END)) {
                // chop_frame {
                //         u1 frame_type = CHOP; /* 248-250 */
                //         u2 offset_delta;
                // }
                put_u1((u1)frame_type);
                frameOffsetDelta = get_u2();
                put_u2(calculateOffsetDelta(i, frameOffsetDelta));

            } else if (frame_type == SAME_FRAME_EXTENDED) {
                // same_frame_extended {
                //     u1 frame_type = SAME_FRAME_EXTENDED; /* 251 */
                //     u2 offset_delta;
                // }

                put_u1(SAME_FRAME_EXTENDED);
                frameOffsetDelta = get_u2();
                put_u2(calculateOffsetDelta(i, frameOffsetDelta));

            } else if ((frame_type >= APPEND_BEGIN) && (frame_type <= APPEND_END)) {
                // append_frame {
                //     u1 frame_type = APPEND; /* 252-254 */
                //     u2 offset_delta;
                //     verification_type_info locals[frame_type - 251];
                // }

                put_u1((u1)frame_type);
                frameOffsetDelta = get_u2();
                put_u2(calculateOffsetDelta(i, frameOffsetDelta));
                copyVerificationTypeInfo((u1)(frame_type - 251));

            } else if (frame_type == FULL_FRAME) {
                // sfull_frame {
                //    u1 frame_type = FULL_FRAME; /* 255 */
                //    u2 offset_delta;
                //    u2 number_of_locals;
                //    verification_type_info locals[number_of_locals];
                //    u2 number_of_stack_items;
                //    verification_type_info stack[number_of_stack_items];
                // }

                put_u1(FULL_FRAME);
                frameOffsetDelta = get_u2();
                put_u2(calculateOffsetDelta(i, frameOffsetDelta));
                number_of_locals = copy_u2();
                copyVerificationTypeInfo(number_of_locals);
                number_of_stack_items = copy_u2();
                copyVerificationTypeInfo(number_of_stack_items);

            }

        }
        set_u4(lenPtr,(u4)((genPos-lenPtr) - 4));

    }

    /* ========================================================================== */

    static void injectBytes(u4 at, u4 len) {
        u4 i;

        NSK_DISPLAY2("Injecting %d bytes at %d\n", len, at);
        for (i = at; i <= codeLength; i++) {
            map[i] += len;
        }
    }

    static void widen(u4 at, jbyte len) {
        u4 i;
        jbyte delta = len - widening[at];

        NSK_DISPLAY2("Widening to %d bytes at %d\n", len, at);
        /* mark at beginning of instruction */
        widening[at] = len;
        /* inject at end of instruction */
        for (i = localPosition(); i <= codeLength; i++) {
            map[i] += delta;
        }
    }

    /* ========================================================================== */

    /**
     * Walk one instruction writing the transformed instruction.
     */
    static void writeInstruction() {
        u4 pos = localPosition();
        u4 newPos = map[pos];
        u1 opcode = get_u1();

        switch (opcode) {

        case opc_wide:
            put_u1(opcode);
            copy(copy_u1() == opc_iinc ? 4 : 2);
            break;

        case opc_new:
        case opc_newarray:
        case opc_anewarray:
        case opc_multianewarray:
            put_u1(opcode);
            copy(opcLengths[opcode] - 1);
            if (mode == BCI_MODE_ALLOC) {
                put(allocBytes, 3);
            }
            break;

        case opc_jsr_w:
        case opc_goto_w:
            put_u1(opcode);
            put_u4(map[pos + get_u4()] - newPos);
            break;

        case opc_jsr:
        case opc_goto:
        case opc_ifeq:
        case opc_ifge:
        case opc_ifgt:
        case opc_ifle:
        case opc_iflt:
        case opc_ifne:
        case opc_if_icmpeq:
        case opc_if_icmpne:
        case opc_if_icmpge:
        case opc_if_icmpgt:
        case opc_if_icmple:
        case opc_if_icmplt:
        case opc_if_acmpeq:
        case opc_if_acmpne:
        case opc_ifnull:
        case opc_ifnonnull: {
            u1 newOpcode = opcode;
            jbyte widened = widening[pos];
            if (widened == 0) { /* not widened */
                put_u1(opcode);
                put_u2((u2) (map[pos + (jshort) get_u2()] - newPos));
            } else if (widened == 2) { /* wide form */
                if (opcode == opc_jsr) {
                    newOpcode = opc_jsr_w;
                } else if (opcode == opc_jsr) {
                    newOpcode = opc_goto_w;
                } else {
                    NSK_COMPLAIN1("unexpected opcode: %d\n", opcode);
                }
                put_u1(newOpcode);
                put_u4(map[pos + (jshort) get_u2()] - newPos);
            } else if (widened == 5) { /* insert goto_w */
                switch (opcode) {
                case opc_ifeq:
                    newOpcode = opc_ifne;
                    break;
                case opc_ifge:
                    newOpcode = opc_iflt;
                    break;
                case opc_ifgt:
                    newOpcode = opc_ifle;
                    break;
                case opc_ifle:
                    newOpcode = opc_ifgt;
                    break;
                case opc_iflt:
                    newOpcode = opc_ifge;
                    break;
                case opc_ifne:
                    newOpcode = opc_ifeq;
                    break;
                case opc_if_icmpeq:
                    newOpcode = opc_if_icmpne;
                    break;
                case opc_if_icmpne:
                    newOpcode = opc_if_icmpeq;
                    break;
                case opc_if_icmpge:
                    newOpcode = opc_if_icmplt;
                    break;
                case opc_if_icmpgt:
                    newOpcode = opc_if_icmple;
                    break;
                case opc_if_icmple:
                    newOpcode = opc_if_icmpgt;
                    break;
                case opc_if_icmplt:
                    newOpcode = opc_if_icmpge;
                    break;
                case opc_if_acmpeq:
                    newOpcode = opc_if_acmpne;
                    break;
                case opc_if_acmpne:
                    newOpcode = opc_if_acmpeq;
                    break;
                case opc_ifnull:
                    newOpcode = opc_ifnonnull;
                    break;
                case opc_ifnonnull:
                    newOpcode = opc_ifnull;
                    break;
                default:
                    NSK_COMPLAIN1("unexpected opcode: %d\n", opcode);
                    break;
                }
                put_u1(newOpcode);  /* write inverse branch */
                put_u1(3 + 5);      /* beyond if and goto_w */
                put_u1(opc_goto_w); /* add a goto_w */
                put_u4(map[pos + (jshort) get_u2()] - newPos);
            } else {
                NSK_COMPLAIN2("unexpected widening: %d, pos=0x%x\n",
                              widened, pos);
            }
            break;
        }

        case opc_tableswitch: {
            u4 i, low, high;

            put_u1(opcode);

            /* skip old padding */
            skip(((pos+4) & (~3)) - (pos+1));

            /* write new padding */
            put(zeroBytes, ((newPos+4) & (~3)) - (newPos+1));
            put_u4(map[pos + get_u4()] - newPos);

            low = copy_u4();
            high = copy_u4();
            for (i = low; i <= high; i++) {
                put_u4(map[pos + get_u4()] - newPos);
            }

            break;
        }

        case opc_lookupswitch: {
            u4 i, npairs;

            put_u1(opcode);

            /* skip old padding */
            skip(((pos+4) & (~3)) - (pos+1));

            /* write new padding */
            put(zeroBytes, ((newPos+4) & (~3)) - (newPos+1));
            put_u4(map[pos + get_u4()] - newPos);

            npairs = copy_u4();
            for (i = npairs; i > 0; i--) {
                copy_u4();
                put_u4(map[pos + get_u4()] - newPos);
            }

            break;
        }

        default:
            put_u1(opcode);
            copy(opcLengths[opcode] - 1);
            break;
        }

    }

    /* ========================================================================== */

    /**
     * Walk one instruction adjusting for insertions
     */
    static int adjustInstruction() {
        u4 pos = localPosition();
        u4 newPos = map[pos];
        u1 opcode = get_u1();

        switch (opcode) {

        case opc_wide:
            skip(get_u1() == opc_iinc ? 4 : 2);
            break;

        case opc_jsr:
        case opc_goto:
        case opc_ifeq:
        case opc_ifge:
        case opc_ifgt:
        case opc_ifle:
        case opc_iflt:
        case opc_ifne:
        case opc_if_icmpeq:
        case opc_if_icmpne:
        case opc_if_icmpge:
        case opc_if_icmpgt:
        case opc_if_icmple:
        case opc_if_icmplt:
        case opc_if_acmpeq:
        case opc_if_acmpne:
        case opc_ifnull:
        case opc_ifnonnull: {
            jbyte widened = widening[pos];
            if (widened == 0) { /* not yet widened */
                jint delta = (jshort) get_u2();
                u4 target = pos + delta;
                u4 newTarget = map[target];
                jint newDelta = newTarget - newPos;
                if ((newDelta < -32768) || (newDelta > 32767)) {
                    if ((opcode == opc_jsr) || (opcode == opc_goto)) {
                        widen(pos, 2); /* will convert to wide */
                    } else {
                        widen(pos, 5); /* will inject goto_w */
                    }
                    return NSK_FALSE;  /* cause restart */
                }
            }
            break;
        }

        case opc_tableswitch: {
            jbyte widened = widening[pos];
            u4 low;
            jbyte deltaPadding;

            /* skip old padding and default */
            skip(((pos+4) & (~3)) - (pos+1) + 4);
            low = get_u4();
            skip((get_u4() - low + 1) * 4);

            deltaPadding = ((newPos+4) & (~3)) - newPos - ((pos+4) & (~3)) + pos;
            if (widened != deltaPadding) {
                widen(pos, deltaPadding);
                return NSK_FALSE;  /* cause restart */
            }
            break;
        }

        case opc_lookupswitch: {
            jbyte widened = widening[pos];
            jbyte deltaPadding;

            /* skip old padding and default */
            skip(((pos+4) & (~3)) - (pos+1) + 4);
            skip(get_u4() * 8);

            deltaPadding = ((newPos+4) & (~3)) - newPos - ((pos+4) & (~3)) + pos;
            if (widened != deltaPadding) {
                widen(pos, deltaPadding);
                return NSK_FALSE;  /* cause restart */
            }
            break;
        }

        default:
            skip(opcLengths[opcode] - 1);
            break;
        }

        return NSK_TRUE;
    }

    /* ========================================================================== */

    /**
     * Walk one instruction inserting instrumentation at specified instructions
     */
    static void insertAtInstruction() {
        u4 pos = localPosition();
        u1 opcode = get_u1();

        switch (opcode) {

        case opc_wide:
            /* no support for instrumenting wide instructions */
            skip(get_u1() == opc_iinc ? 4 : 2);
            break;

        case opc_new:
        case opc_newarray:
        case opc_anewarray:
        case opc_multianewarray:
            skip(opcLengths[opcode] - 1);
            injectBytes(localPosition(), 3);
            break;

        case opc_tableswitch:
            /* skip 4-byte boundry padding and default */
            skip(((pos+4) & (~3)) - (pos+1) + 4);
            {
                u4 low = get_u4();
                skip((get_u4() - low + 1) * 4);
            }
            break;

        case opc_lookupswitch:
            /* skip 4-byte boundry padding and default */
            skip(((pos+4) & (~3)) - (pos+1) + 4);
            skip(get_u4() * 8);
            break;

        default:
            skip(opcLengths[opcode] - 1);
            break;

        }
    }

    static void adjustOffsets() {
        recallPosition();

        if (mode == BCI_MODE_CALL) {
            /* instrument calls - method entry */
            injectBytes(0, 3);
        }

        if (mode == BCI_MODE_ALLOC) {
            /* instrument allocations */
            while (localPosition() < codeLength) {
                insertAtInstruction();
            }
            recallPosition();
        }

        NSK_DISPLAY0("Searching for adjustments...\n");
        while (localPosition() < codeLength) {
            if (!adjustInstruction()) {
                recallPosition();
                NSK_DISPLAY0("Restarting adjustments after change...\n");
            }
        }

        NSK_DISPLAY0("Writing new code...\n");
        recallPosition();

        if (mode == BCI_MODE_CALL) {
            put(callBytes, 3);
        }

        while (localPosition() < codeLength) {
            writeInstruction();
        }
    }

    /* ========================================================================== */

    static void copyAttr() {
        u4 len;

        copy(2);
        len = copy_u4();
        NSK_DISPLAY1("attr len: %d\n", len);
        copy(len);
    }

    static void copyAttrs(u2 attrCount) {
        u2 i;

        for (i = attrCount; i > 0; i--) {
            copyAttr();
        }
    }

    static void copyFields() {
        u2 count;
        u2 attrCount;
        u2 i;

        count = copy_u2();
        NSK_DISPLAY1("fields count: %d\n", count);
        for (i = count; i > 0; i--) {
            /* access, name, descriptor */
            copy(2 + 2 + 2);
            attrCount = copy_u2();
            NSK_DISPLAY1("field attrCount: %d\n", attrCount);
            copyAttrs(attrCount);
        }
    }

    static void copyAttrForCode() {
        u2 nameIndex = copy_u2();

        /* check for Code attr */
        if (nameIndex == lineNumberAttributeIndex) {
            copyLineNumberAttr();
        } else if (nameIndex == localVarAttributeIndex) {
            copyLocalVarAttr();
        } else if (nameIndex == localVarTypeAttributeIndex) {
            copyLocalVarTypeAttr();
        } else if (nameIndex == stackMapAttributeIndex) {
            copyStackMapAttr();
        } else {
            u4 len = copy_u4();
            NSK_DISPLAY1("code attr len: %d\n", len);
            copy(len);
        }
    }

    static void copyCodeAttr(char* name) {
        u4 attrLengthPos;
        u4 attrLength;
        u4 newAttrLength;
        u4 codeLengthPos;
        u4 newCodeLength;
        u2 attrCount;
        u4 i;

        attrLengthPos = generatedPosition();
        attrLength = copy_u4();

        NSK_DISPLAY2("Code attr found: %s, pos=0x%x\n", name,
                     inputPos - orig - 6);

        /* max_stack, max_locals */
        copy(2 + 2);

        codeLengthPos = generatedPosition();
        codeLength = copy_u4();

        if (codeLength == 0) {
            NSK_COMPLAIN0("code_length must be greater than zero\n");
            return;
        }

        if (mode == BCI_MODE_EMCP) {
            /* copy remainder minus already copied */
            copy(attrLength - 8);
            return;
        }

        markLocalPositionStart();

        map = (u4*) malloc((codeLength + 1) * sizeof(u4));
        for (i = 0; i <= codeLength; i++) {
            map[i] = i;
        }

        widening = (jbyte*) malloc(codeLength + 1);
        memset(widening, 0, codeLength + 1);

        adjustOffsets();

        /* fix up code length */
        newCodeLength = generatedPosition() - (codeLengthPos + 4);
        randomAccessWriteU4(codeLengthPos, newCodeLength);
        NSK_DISPLAY2("code length old: %d, new: %d\n",
                     codeLength, newCodeLength);

        copyExceptionTable();

        attrCount = copy_u2();
        for (i = attrCount; i > 0; i--) {
            copyAttrForCode();
        }

        free(map);
        free(widening);

        /* fix up attr length */
        newAttrLength = generatedPosition() - (attrLengthPos + 4);
        randomAccessWriteU4(attrLengthPos, newAttrLength);
        NSK_DISPLAY2("attr length old: %d, new: %d\n",
                     attrLength, newAttrLength);
    }

    static void copyAttrForMethod(char* name) {
        u2 nameIndex;

        nameIndex = copy_u2();
        if (nameIndex == codeAttributeIndex) {
            copyCodeAttr(name);
        } else {
            u4 len = copy_u4();

            NSK_DISPLAY1("method attr len: %d\n", len);
            copy(len);
        }
    }

    static void copyMethod() {
        u2 accessFlags;
        u2 methodNameIdx;
        char* name;
        u2 attrCount;
        u2 i;

        accessFlags = copy_u2();
        methodNameIdx = copy_u2();
        name = constantPool[methodNameIdx];

        /* descriptor */
        copy(2);

        attrCount = copy_u2();
        NSK_DISPLAY1("method attrCount: %d\n", attrCount);
        for (i = attrCount; i > 0; i--) {
            copyAttrForMethod(name);
        }
    }

    static void copyMethods() {
        u2 count;
        u2 i;

        count = copy_u2();
        NSK_DISPLAY1("methods count: %d\n", count);
        for (i = count; i > 0; i--) {
            copyMethod();
        }
    }

    static u2 writeCPEntryUtf8(const char* str) {
        u2 i;
        u2 len = (u2) strlen(str);
        put_u1(CONSTANT_Utf8);
        put_u2(len);
        for (i = 0; i < len; i++) {
            put_u1(str[i]);
        }
        return constantPoolCount++;
    }

    static u2 writeCPEntryClass(u2 classNameIndex) {
        put_u1(CONSTANT_Class);
        put_u2(classNameIndex);
        return constantPoolCount++;
    }

    static u2 writeCPEntryNameAndType(u2 nameIndex, u2 descrIndex) {
        put_u1(CONSTANT_NameAndType);
        put_u2(nameIndex);
        put_u2(descrIndex);
        return constantPoolCount++;
    }

    static u2 writeCPEntryMethodRef(u2 classIndex, u2 nameAndTypeIndex) {
        put_u1(CONSTANT_Methodref);
        put_u2(classIndex);
        put_u2(nameAndTypeIndex);
        return constantPoolCount++;
    }

    static u2 writeCPEntryFieldRef(u2 classIndex, u2 nameAndTypeIndex) {
        put_u1(CONSTANT_Fieldref);
        put_u2(classIndex);
        put_u2(nameAndTypeIndex);
        return constantPoolCount++;
    }

    static u2 addFieldToConstantPool(u2 classIndex, char* fieldName, char* descr) {
        u2 fieldNameIndex = writeCPEntryUtf8(fieldName);
        u2 descrIndex = writeCPEntryUtf8(descr);
        u2 nameAndTypeIndex = writeCPEntryNameAndType(fieldNameIndex, descrIndex);
        u2 fieldIndex = writeCPEntryFieldRef(classIndex, nameAndTypeIndex);
        return fieldIndex;
    }

    static u2 addMethodToConstantPool(u2 classIndex, const char* methodName, const char* descr) {
        u2 methodNameIndex = writeCPEntryUtf8(methodName);
        u2 descrIndex = writeCPEntryUtf8(descr);
        u2 nameAndTypeIndex = writeCPEntryNameAndType(methodNameIndex, descrIndex);
        u2 methodIndex = writeCPEntryMethodRef(classIndex, nameAndTypeIndex);
        return methodIndex;
    }

    static u2 addClassToConstantPool(const char* className) {
        u2 classNameIndex = writeCPEntryUtf8(className);
        u2 classIndex = writeCPEntryClass(classNameIndex);
        return classIndex;
    }

    /* ========================================================================== */

    int Inject(const u1* old_bytes, const jint old_length,
               u1** new_bytes, jint* new_length, int bci_mode) {
        u4 constantPoolCountPos;
        u2 profiler;
        u2 interfaceCount;
        u2 attrCount;

        //printf("inject\n");
        NSK_DISPLAY3("Injecting bytecodes: mode=%d, bytes=0x%p, len=%d\n",
                     bci_mode, old_bytes, old_length);

        mode = bci_mode;
        orig = old_bytes;
        inputPos = (u1*) orig;
        endPos = orig + old_length;
        gen = (u1*) malloc(old_length * GROWTH_FACTOR);
        if (!NSK_VERIFY(gen != NULL)) {
            NSK_COMPLAIN0("out of memory\n");
            return NSK_FALSE;
        }

        genPos = gen;

        /* magic + minor/major version */
        copy(4 + 2 + 2);

        constantPoolCountPos = generatedPosition();
        constantPoolCount = copy_u2();

        /* copy old constant pool */
        if (!copyConstantPool(constantPoolCount)) {
            return NSK_FALSE;
        }
        NSK_DISPLAY1("ConstantPool expanded from: %d\n", constantPoolCount);

        profiler = addClassToConstantPool("nsk/share/jvmti/ProfileCollector");

        if (mode == BCI_MODE_ALLOC) {
            u2 allocTracker =
                addMethodToConstantPool(profiler, "allocTracker", "()V");
            allocBytes[1] = (u1) (allocTracker >> 8);
            allocBytes[2] = (u1) (allocTracker & 0xFF);
        }

        if (mode == BCI_MODE_CALL) {
            u2 callTracker =
                addMethodToConstantPool(profiler, "callTracker", "()V");
            callBytes[1] = (u1) (callTracker >> 8);
            callBytes[2] = (u1) (callTracker & 0xFF);
        }

        /* access, this, super */
        copy(2 + 2 + 2);

        interfaceCount = copy_u2();
        NSK_DISPLAY1("interfaceCount: %d\n", interfaceCount);
        copy(interfaceCount * 2);

        copyFields();
        copyMethods();

        attrCount = copy_u2();
        NSK_DISPLAY1("class attrCount: %d\n", attrCount);
        copyAttrs(attrCount);

        randomAccessWriteU2(constantPoolCountPos, constantPoolCount);
        NSK_DISPLAY1("New constant pool size: %d\n", constantPoolCount);

        *new_length = (jint) (genPos - gen);
        *new_bytes = (u1*) realloc(gen, *new_length);

        freeConstantPool();

        return NSK_TRUE;
    }

    /* ========================================================================== */

}
