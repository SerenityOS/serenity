/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatting.h"
#include "Instruction.h"

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
    return MUST(String::formatted("l{:9} {}, {:#03x}({})", m_width, format_register(destination_register(), display_style), immediate(), format_register(source_register(), display_style)));
}

String MemoryLoad::mnemonic() const
{
    return MUST(String::formatted("l{}", m_width));
}

String MemoryStore::to_string(DisplayStyle display_style, u32, Optional<SymbolProvider const&>) const
{
    return MUST(String::formatted("s{:9} {}, {:#03x}({})", m_width, format_register(source_register_2(), display_style), immediate(), format_register(source_register_1(), display_style)));
}

String MemoryStore::mnemonic() const
{
    return MUST(String::formatted("s{}", m_width));
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

}
