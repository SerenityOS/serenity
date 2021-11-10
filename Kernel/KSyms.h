/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Kernel {

struct KernelSymbol {
    FlatPtr address;
    const char* name;
};

enum class PrintToScreen {
    No,
    Yes,
};

FlatPtr address_for_kernel_symbol(StringView name);
const KernelSymbol* symbolicate_kernel_address(FlatPtr);
void load_kernel_symbol_table();

extern bool g_kernel_symbols_available;
extern FlatPtr g_lowest_kernel_symbol_address;
extern FlatPtr g_highest_kernel_symbol_address;

void dump_backtrace(PrintToScreen print_to_screen = PrintToScreen::No);

}
