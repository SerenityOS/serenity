/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatting.h"

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

}
