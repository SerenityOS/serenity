/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class EncodedTermOpcode {
public:
    enum class Opcode {
        Local0 = 0,
        Local1,
        Local2,
        Local3,
        Local4,
        Local5,
        Local6,
        Local7,

        Arg0,
        Arg1,
        Arg2,
        Arg3,
        Arg4,
        Arg5,
        Arg6,

        Zero,
        One,
        Ones,
        Buffer,
        Package,
        VarPackage,

        // Expression opcodes that don't use the ExtOpPrefix
        Add,
        And,
        Concat,
        ConcatRes,
        CopyObject,
        Decrement,
        DerefOf,
        Divide,
        FindSetLeftBit,
        FindSetRightBit,
        Increment,
        Index,
        LAnd,
        LEqual,
        LGreater,
        LGreaterEqual,
        LLess,
        LLessEqual,
        LNot,
        LNotEqual,
        LOr,
        Match,
        Mid,
        Mod,
        Multiply,
        NAnd,
        NOr,
        Not,
        ObjectType,
        Or,
        RefOf,
        ShiftLeft,
        ShiftRight,
        SizeOf,
        Store,
        Subtract,
        ToBuffer,
        ToDecimalString,
        ToHexString,
        ToInteger,
        ToString,
        XOr,

        // Prefixes
        BytePrefix,
        WordPrefix,
        DWordPrefix,
        QWordPrefix,
        StringPrefix,

        // Extended Opcodes
        Revision,
        DebugOp,

        // Expression opcodes that use the ExtOpPrefix
        Acquire,
        CondRefOf,
        FromBCD,
        LoadTable,
        Timer,
        ToBCD,
        Wait,

        // Statement Opcodes that don't use the ExtOpPrefix
        Break,
        BreakPoint,
        Continue,
        Else,
        IfElse,
        NoOp,
        Notify,
        Return,
        While,

        // Statement Opcodes that use the ExtOpPrefix
        Fatal,
        Load,
        Release,
        Reset,
        Signal,
        Sleep,
        Stall,
    };

public:
    explicit EncodedTermOpcode(Array<u8, 2> encoded_term_opcode);
    explicit EncodedTermOpcode(u8 encoded_term_opcode);
    Optional<Opcode> opcode() const;

    bool has_extended_prefix() const;
    bool has_math_prefix() const;

    size_t length() const;

private:
    Array<u8, 2> m_encoded_term_opcode;
};

}
