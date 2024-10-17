/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <AK/kstdio.h>

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/Globals.h>

namespace Kernel {

void ucs2_dbgln(char16_t const* message)
{
    if (g_efi_system_table == nullptr || g_efi_system_table->con_out == nullptr)
        return;

    g_efi_system_table->con_out->output_string(g_efi_system_table->con_out, const_cast<char16_t*>(message));
    g_efi_system_table->con_out->output_string(g_efi_system_table->con_out, const_cast<char16_t*>(u"\r\n"));
}

}

extern "C" void dbgputstr(char const* characters, size_t length)
{
    if (Kernel::g_efi_system_table == nullptr || Kernel::g_efi_system_table->con_out == nullptr)
        return;

    Vector<char16_t, 256> utf16_string;
    StringView string_view { characters, length };

    // FIXME: Support non-ASCII messages
    for (auto c : string_view) {
        if (c == '\n') {
            if (utf16_string.try_append(u'\r').is_error())
                return;
        }

        if (utf16_string.try_append(c).is_error())
            return;
    }

    if (utf16_string.try_append(0).is_error())
        return;

    Kernel::g_efi_system_table->con_out->output_string(Kernel::g_efi_system_table->con_out, bit_cast<char16_t*>(utf16_string.data()));
}
