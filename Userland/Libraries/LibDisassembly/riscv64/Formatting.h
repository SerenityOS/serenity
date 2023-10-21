/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/String.h>
#include <LibDisassembly/riscv64/Encoding.h>

template<>
struct AK::Formatter<Disassembly::RISCV64::Register> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::Register reg)
    {
        return AK::Formatter<FormatString>::format(builder, "x{}"sv, static_cast<u8>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::FloatRegister> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::FloatRegister reg)
    {
        return AK::Formatter<FormatString>::format(builder, "f{}"sv, static_cast<u8>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RegisterABINames> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RegisterABINames reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::RegisterABINames::zero:
            formatted = "zero"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::ra:
            formatted = "ra"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::sp:
            formatted = "sp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::gp:
            formatted = "gp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::tp:
            formatted = "tp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t0:
            formatted = "t0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t1:
            formatted = "t1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t2:
            formatted = "t2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s0:
            formatted = "s0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s1:
            formatted = "s1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a0:
            formatted = "a0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a1:
            formatted = "a1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a2:
            formatted = "a2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a3:
            formatted = "a3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a4:
            formatted = "a4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a5:
            formatted = "a5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a6:
            formatted = "a6"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a7:
            formatted = "a7"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s2:
            formatted = "s2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s3:
            formatted = "s3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s4:
            formatted = "s4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s5:
            formatted = "s5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s6:
            formatted = "s6"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s7:
            formatted = "s7"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s8:
            formatted = "s8"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s9:
            formatted = "s9"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s10:
            formatted = "s10"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s11:
            formatted = "s11"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t3:
            formatted = "t3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t4:
            formatted = "t4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t5:
            formatted = "t5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t6:
            formatted = "t6"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RegisterABINamesWithFP> : AK::Formatter<Disassembly::RISCV64::RegisterABINames> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RegisterABINamesWithFP reg)
    {
        if (reg == Disassembly::RISCV64::RegisterABINamesWithFP::fp)
            return AK::Formatter<StringView>::format(builder, "fp"sv);
        return AK::Formatter<Disassembly::RISCV64::RegisterABINames>::format(builder, static_cast<Disassembly::RISCV64::RegisterABINames>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::FloatRegisterABINames> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::FloatRegisterABINames reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::FloatRegisterABINames::ft0:
            formatted = "ft0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft1:
            formatted = "ft1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft2:
            formatted = "ft2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft3:
            formatted = "ft3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft4:
            formatted = "ft4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft5:
            formatted = "ft5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft6:
            formatted = "ft6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft7:
            formatted = "ft7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs0:
            formatted = "fs0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs1:
            formatted = "fs1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa0:
            formatted = "fa0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa1:
            formatted = "fa1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa2:
            formatted = "fa2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa3:
            formatted = "fa3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa4:
            formatted = "fa4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa5:
            formatted = "fa5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa6:
            formatted = "fa6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa7:
            formatted = "fa7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs2:
            formatted = "fs2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs3:
            formatted = "fs3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs4:
            formatted = "fs4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs5:
            formatted = "fs5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs6:
            formatted = "fs6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs7:
            formatted = "fs7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs8:
            formatted = "fs8"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs9:
            formatted = "fs9"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs10:
            formatted = "fs10"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs11:
            formatted = "fs11"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft8:
            formatted = "ft8"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft9:
            formatted = "ft9"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft10:
            formatted = "ft10"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft11:
            formatted = "ft11"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RoundingMode> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RoundingMode reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::RoundingMode::RNE:
            formatted = "rne"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RTZ:
            formatted = "rtz"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RDN:
            formatted = "rdn"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RUP:
            formatted = "rup"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RMM:
            formatted = "rmm"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::Invalid1:
        case Disassembly::RISCV64::RoundingMode::Invalid2:
            formatted = "invalid"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::DYN:
            formatted = "dyn"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};
