/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatting.h"
#include "Instruction.h"
#include "Priviledged.h"

namespace Disassembly::RISCV64 {

template<typename RegisterType>
static String format_register(RegisterType reg, DisplayStyle display_style)
{
    switch (display_style.register_names) {
    case DisplayStyle::RegisterNames::Hardware:
        return MUST(String::formatted("{}", reg));
    case DisplayStyle::RegisterNames::ABI:
        return MUST(String::formatted("{}", static_cast<RegisterNameTraits<RegisterType>::ABIType>(reg.value())));
    case DisplayStyle::RegisterNames::ABIWithFramePointer:
        return MUST(String::formatted("{}", static_cast<RegisterNameTraits<RegisterType>::ABIWithFPType>(reg.value())));
    }
    VERIFY_NOT_REACHED();
}

static String format_relative_address(DisplayStyle display_style, Optional<SymbolProvider const&> symbol_provider, u32 origin, i32 offset)
{
    String formatted_target;
    if (display_style.relative_address_style == DisplayStyle::RelativeAddressStyle::Symbol) {
        auto target_address = origin + offset;
        if (symbol_provider.has_value()) {
            u32 offset;
            auto symbol = symbol_provider->symbolicate(target_address, &offset);
            if (symbol.is_empty())
                formatted_target = MUST(String::formatted("{:#x}", target_address));
            else if (offset == 0)
                formatted_target = MUST(String::formatted("{:#x} <{}>", target_address, symbol));
            else
                formatted_target = MUST(String::formatted("{:#x} <{}{:+#x}>", target_address, symbol, offset));
        } else {
            formatted_target = MUST(String::formatted("{:#x}", target_address));
        }
    } else {
        formatted_target = MUST(String::formatted("{:+#06x}", offset));
    }
    return formatted_target;
}

String InstructionWithoutArguments::to_string(DisplayStyle, u32, Optional<SymbolProvider const&>) const
{
    return mnemonic();
}

String UnknownInstruction::to_string(DisplayStyle, u32, Optional<SymbolProvider const&>) const
{
    // FIXME: Print the full instruction’s bytes in some useful format.
    return mnemonic();
}

String UnknownInstruction::mnemonic() const
{
    return ".insn"_string;
}

String LoadUpperImmediate::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {:d}", mnemonic(), format_register(destination_register(), display_style), immediate()));
}

String LoadUpperImmediate::mnemonic() const
{
    return "lui"_string;
}

String JumpAndLink::to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const
{
    auto formatted_target = format_relative_address(display_style, symbol_provider, origin, immediate());

    if (display_style.use_pseudoinstructions == DisplayStyle::UsePseudoinstructions::Yes && destination_register() == RegisterABINames::zero)
        return MUST(String::formatted("j          {}", formatted_target));

    return MUST(String::formatted("{:10} {}, {}", mnemonic(), format_register(destination_register(), display_style), formatted_target));
}

String JumpAndLink::mnemonic() const
{
    return "jal"_string;
}

String JumpAndLinkRegister::to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const
{
    return MUST(String::formatted("{:10} {}, {}, {}", mnemonic(), format_register(destination_register(), display_style), format_register(source_register(), display_style), format_relative_address(display_style, symbol_provider, origin, immediate())));
}

String JumpAndLinkRegister::mnemonic() const
{
    return "jalr"_string;
}

String Branch::to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const
{
    return MUST(String::formatted("{:10} {}, {}, {}", m_condition, format_register(source_register_1(), display_style), format_register(source_register_2(), display_style), format_relative_address(display_style, symbol_provider, origin, immediate())));
}

String Branch::mnemonic() const
{
    return MUST(String::formatted("{}", m_condition));
}

String AddUpperImmediateToProgramCounter::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}", mnemonic(), format_register(destination_register(), display_style), immediate()));
}

String AddUpperImmediateToProgramCounter::mnemonic() const
{
    return "auipc"_string;
}

String ArithmeticInstruction::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, {}", m_operation, format_register(destination_register(), display_style), format_register(source_register_1(), display_style), format_register(source_register_2(), display_style)));
}

String ArithmeticInstruction::mnemonic() const
{
    return MUST(String::formatted("{}", m_operation));
}

String FloatComputationInstruction::format_rounding_mode(DisplayStyle display_style) const
{
    if (display_style.use_pseudoinstructions == DisplayStyle::UsePseudoinstructions::Yes && rounding_mode() == RoundingMode::DYN)
        return {};
    return MUST(String::formatted(", {}", rounding_mode()));
}

String FloatArithmeticInstruction::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, {}{}", mnemonic(), format_register(destination_register(), display_style), format_register(source_register_1(), display_style), format_register(source_register_2(), display_style), format_rounding_mode(display_style)));
}

String FloatArithmeticInstruction::mnemonic() const
{
    return MUST(String::formatted("{}.{}", m_operation, width()));
}

String FloatSquareRoot::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}{}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style), format_rounding_mode(display_style)));
}

String FloatSquareRoot::mnemonic() const
{
    return MUST(String::formatted("fsqrt.{}", width()));
}

String FloatFusedMultiplyAdd::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, {}, {}{}", mnemonic(), format_register(destination_register(), display_style), format_register(source_register_1(), display_style), format_register(source_register_2(), display_style), format_register(source_register_3(), display_style), format_rounding_mode(display_style)));
}

String FloatFusedMultiplyAdd::mnemonic() const
{
    return MUST(String::formatted("{}.{}", m_operation, width()));
}

String FloatCompare::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, {}", mnemonic(), format_register(m_rd, display_style), format_register(source_register_1(), display_style), format_register(source_register_2(), display_style)));
}

String FloatCompare::mnemonic() const
{
    auto base_name = ""sv;
    switch (m_operation) {
    case Operation::Equals:
        base_name = "feq"sv;
        break;
    case Operation::LessThan:
        base_name = "flt"sv;
        break;
    case Operation::LessThanEquals:
        base_name = "fle"sv;
        break;
    }
    return MUST(String::formatted("{}.{}", base_name, width()));
}

String ConvertFloatAndInteger::integer_width_suffix() const
{
    return MUST(MUST(String::formatted("{}", integer_width())).replace("d"sv, "l"sv, ReplaceMode::FirstOnly));
}

String ConvertFloatToInteger::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}{}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style), format_rounding_mode(display_style)));
}

String ConvertFloatToInteger::mnemonic() const
{
    return MUST(String::formatted("fcvt.{}.{}", integer_width_suffix(), width()));
}

String ConvertIntegerToFloat::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}{}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style), format_rounding_mode(display_style)));
}

String ConvertIntegerToFloat::mnemonic() const
{
    return MUST(String::formatted("fcvt.{}.{}", width(), integer_width_suffix()));
}

String MoveFloatToInteger::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style)));
}

String MoveFloatToInteger::mnemonic() const
{
    return MUST(String::formatted("fmv.x.{}", memory_width()));
}

String MoveIntegerToFloat::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style)));
}

String MoveIntegerToFloat::mnemonic() const
{
    return MUST(String::formatted("fmv.{}.x", memory_width()));
}

String ConvertFloat::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}{}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style), format_rounding_mode(display_style)));
}

String ConvertFloat::mnemonic() const
{
    return m_operation == ConvertFloat::Operation::DoubleToSingle ? "fcvt.s.d"_string : "fcvt.d.s"_string;
}

String FloatClassify::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}", mnemonic(), format_register(m_rd, display_style), format_register(m_rs, display_style)));
}

String FloatClassify::mnemonic() const
{
    return MUST(String::formatted("fclass.{}", width()));
}

String ArithmeticImmediateInstruction::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, {:d}", m_operation, format_register(destination_register(), display_style), format_register(source_register(), display_style), immediate()));
}

String ArithmeticImmediateInstruction::mnemonic() const
{
    return MUST(String::formatted("{}", m_operation));
}

String MemoryLoad::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {:#03x}({})", mnemonic(), format_register(destination_register(), display_style), immediate(), format_register(source_register(), display_style)));
}

String MemoryLoad::mnemonic() const
{
    return MUST(String::formatted("l{}", m_width));
}

String FloatMemoryLoad::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {:#03x}({})", mnemonic(), format_register(destination_register(), display_style), immediate(), format_register(base(), display_style)));
}

String FloatMemoryLoad::mnemonic() const
{
    return MUST(String::formatted("fl{}", memory_width()));
}

String MemoryStore::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("s{:9} {}, {:#03x}({})", m_width, format_register(source_register_2(), display_style), immediate(), format_register(source_register_1(), display_style)));
}

String MemoryStore::mnemonic() const
{
    return MUST(String::formatted("s{}", m_width));
}

String FloatMemoryStore::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:8} {}, {:#03x}({})", mnemonic(), format_register(source_register(), display_style), immediate(), format_register(base(), display_style)));
}

String FloatMemoryStore::mnemonic() const
{
    return MUST(String::formatted("fs{}", memory_width()));
}

String EnvironmentBreak::mnemonic() const
{
    return "ebreak"_string;
}

String EnvironmentCall::mnemonic() const
{
    return "ecall"_string;
}

String InstructionFetchFence::mnemonic() const
{
    return "fence.i"_string;
}

String MachineModeTrapReturn::mnemonic() const
{
    return "mret"_string;
}

String SupervisorModeTrapReturn::mnemonic() const
{
    return "sret"_string;
}

String WaitForInterrupt::mnemonic() const
{
    return "wfi"_string;
}

String CSRImmediateInstruction::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {:#x}, {}", mnemonic(), format_register(destination_register(), display_style), csr(), immediate()));
}

String CSRImmediateInstruction::mnemonic() const
{
    return MUST(String::formatted("{}i", operation()));
}

String CSRRegisterInstruction::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {:#x}, {}", mnemonic(), format_register(destination_register(), display_style), csr(), format_register(source_register(), display_style)));
}

String CSRRegisterInstruction::mnemonic() const
{
    return MUST(String::formatted("{}", operation()));
}

String Fence::to_string(DisplayStyle, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}", mnemonic(), m_predecessor, m_successor));
}

String Fence::mnemonic() const
{
    switch (m_mode) {
    case Fence::Mode::Normal:
        return "fence"_string;
    case Fence::Mode::NoStoreToLoadOrdering:
        return "fence.tso"_string;
    }
    VERIFY_NOT_REACHED();
}

String AtomicMemoryOperation::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("{:10} {}, {}, ({})", mnemonic(), format_register(destination_register(), display_style), format_register(source_register_2(), display_style), format_register(source_register_1(), display_style)));
}

String AtomicMemoryOperation::mnemonic() const
{
    return MUST(String::formatted("amo{}.{}{}", m_operation, MemoryAccessMode { width(), Signedness::Signed },
        is_acquire_release() ? ".aqrl"sv
            : is_acquire()   ? ".aq"sv
            : is_release()   ? ".rl"sv
                             : ""sv));
}

String LoadReservedStoreConditional::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    if (m_operation == Operation::LoadReserved)
        return MUST(String::formatted("{:10} {}, ({})", mnemonic(), format_register(destination_register(), display_style), format_register(source_register_1(), display_style)));
    // Operation::StoreConditional
    return MUST(String::formatted("{:10} {}, {}, ({})", mnemonic(), format_register(destination_register(), display_style), format_register(source_register_2(), display_style), format_register(source_register_1(), display_style)));
}

String LoadReservedStoreConditional::mnemonic() const
{
    auto operation = m_operation == Operation::LoadReserved ? "lr"sv : "sc"sv;
    return MUST(String::formatted("{}.{}{}", operation, MemoryAccessMode { width(), Signedness::Signed },
        is_acquire_release() ? ".aqrl"sv
            : is_acquire()   ? ".aq"sv
            : is_release()   ? ".rl"sv
                             : ""sv));
}

}
