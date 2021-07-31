/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/EncodedTermOpcode.h>

namespace Kernel::ACPI {

EncodedTermOpcode::EncodedTermOpcode(Array<u8, 2> encoded_term_opcode)
{
    m_encoded_term_opcode[0] = encoded_term_opcode[0];
    m_encoded_term_opcode[1] = encoded_term_opcode[1];
}

EncodedTermOpcode::EncodedTermOpcode(u8 encoded_term_opcode)
{
    m_encoded_term_opcode[0] = encoded_term_opcode;
    m_encoded_term_opcode[1] = 0;
}

bool EncodedTermOpcode::has_extended_prefix() const
{
    return m_encoded_term_opcode[0] == 0x5B;
}

bool EncodedTermOpcode::has_math_prefix() const
{
    return m_encoded_term_opcode[0] == 0x92;
}

size_t EncodedTermOpcode::length() const
{
    if (has_extended_prefix())
        return 2;
    if (has_math_prefix() && m_encoded_term_opcode[1] >= 0x93 && m_encoded_term_opcode[1] <= 0x95) {
        return 2;
    }
    return 1;
}

Optional<EncodedTermOpcode::Opcode> EncodedTermOpcode::opcode() const
{
    if (has_math_prefix()) {
        switch (m_encoded_term_opcode[1]) {
        case 0x95:
            return Opcode::LGreaterEqual;
        case 0x94:
            return Opcode::LLessEqual;
        case 0x93:
            return Opcode::LNotEqual;
        }
    }
    if (!has_extended_prefix()) {
        switch (m_encoded_term_opcode[0]) {
        case 0x72:
            return Opcode::Add;
        case 0x7B:
            return Opcode::And;
        case 0x73:
            return Opcode::Concat;
        case 0x84:
            return Opcode::ConcatRes;
        case 0x9D:
            return Opcode::CopyObject;
        case 0x76:
            return Opcode::Decrement;
        case 0x83:
            return Opcode::DerefOf;
        case 0x78:
            return Opcode::Divide;
        case 0x81:
            return Opcode::FindSetLeftBit;
        case 0x82:
            return Opcode::FindSetRightBit;
        case 0x75:
            return Opcode::Increment;
        case 0x88:
            return Opcode::Index;
        case 0x90:
            return Opcode::LAnd;
        case 0x93:
            return Opcode::LEqual;
        case 0x94:
            return Opcode::LGreater;
        case 0x95:
            return Opcode::LLess;
        case 0x92:
            return Opcode::LNot;
        case 0x91:
            return Opcode::LOr;
        case 0x89:
            return Opcode::Match;
        case 0x9E:
            return Opcode::Mid;
        case 0x85:
            return Opcode::Mod;
        case 0x77:
            return Opcode::Multiply;
        case 0x7C:
            return Opcode::NAnd;
        case 0x7E:
            return Opcode::NOr;
        case 0x80:
            return Opcode::Not;
        case 0x8E:
            return Opcode::ObjectType;
        case 0x7D:
            return Opcode::Or;
        case 0x71:
            return Opcode::RefOf;
        case 0x79:
            return Opcode::ShiftLeft;
        case 0x7A:
            return Opcode::ShiftRight;
        case 0x87:
            return Opcode::SizeOf;
        case 0x70:
            return Opcode::Store;
        case 0x74:
            return Opcode::Subtract;
        case 0x96:
            return Opcode::ToBuffer;
        case 0x97:
            return Opcode::ToDecimalString;
        case 0x98:
            return Opcode::ToHexString;
        case 0x99:
            return Opcode::ToInteger;
        case 0x9C:
            return Opcode::ToString;
        case 0x7F:
            return Opcode::XOr;
        case 0x60:
            return Opcode::Local0;
        case 0x61:
            return Opcode::Local1;
        case 0x62:
            return Opcode::Local2;
        case 0x63:
            return Opcode::Local3;
        case 0x64:
            return Opcode::Local4;
        case 0x65:
            return Opcode::Local5;
        case 0x66:
            return Opcode::Local6;
        case 0x67:
            return Opcode::Local7;
        case 0x68:
            return Opcode::Arg0;
        case 0x69:
            return Opcode::Arg1;
        case 0x6A:
            return Opcode::Arg2;
        case 0x6B:
            return Opcode::Arg3;
        case 0x6C:
            return Opcode::Arg4;
        case 0x6D:
            return Opcode::Arg5;
        case 0x6E:
            return Opcode::Arg6;
        case 0x00:
            return Opcode::Zero;
        case 0x01:
            return Opcode::One;
        case 0xFF:
            return Opcode::Ones;
        case 0x11:
            return Opcode::Buffer;
        case 0x12:
            return Opcode::Package;
        case 0x13:
            return Opcode::VarPackage;
        case 0x0A:
            return Opcode::BytePrefix;
        case 0x0B:
            return Opcode::WordPrefix;
        case 0x0C:
            return Opcode::DWordPrefix;
        case 0x0E:
            return Opcode::QWordPrefix;
        case 0x0D:
            return Opcode::StringPrefix;
        case 0xA5:
            return Opcode::Break;
        case 0xCC:
            return Opcode::BreakPoint;
        case 0x9F:
            return Opcode::Continue;
        case 0xA1:
            return Opcode::Else;
        case 0xA0:
            return Opcode::IfElse;
        case 0xA3:
            return Opcode::NoOp;
        case 0x86:
            return Opcode::Notify;
        case 0xA4:
            return Opcode::Return;
        case 0xA2:
            return Opcode::While;
        default:
            // Unknown TermArg
            return {};
        }
    }
    switch (m_encoded_term_opcode[1]) {
    case 0x30:
        return Opcode::Revision;
    case 0x31:
        return Opcode::DebugOp;
    case 0x23:
        return Opcode::Acquire;
    case 0x12:
        return Opcode::CondRefOf;
    case 0x28:
        return Opcode::FromBCD;
    case 0x1F:
        return Opcode::LoadTable;
    case 0x33:
        return Opcode::Timer;
    case 0x29:
        return Opcode::ToBCD;
    case 0x25:
        return Opcode::Wait;
    case 0x32:
        return Opcode::Fatal;
    case 0x20:
        return Opcode::Load;
    case 0x27:
        return Opcode::Release;
    case 0x26:
        return Opcode::Reset;
    case 0x24:
        return Opcode::Signal;
    case 0x22:
        return Opcode::Sleep;
    case 0x21:
        return Opcode::Stall;
    };
    // Unknown TermArg
    return {};
}
}
