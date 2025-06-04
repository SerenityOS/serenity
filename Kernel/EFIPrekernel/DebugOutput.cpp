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
    
    for (size_t i = 0; i < string_view.length();) {
        char32_t code_point = 0;
        size_t bytes_consumed = 0;

        unsigned char c = string_view[i];
        if (c < 0x80) {
            code_point = c;
            bytes_consumed = 1;
        } else if ((c & 0xE0) == 0xC0) {
            if (i + 1 >= string_view.length())
                break;
            code_point = ((c & 0x1F) << 6) | (string_view[i + 1] & 0x3F);
            bytes_consumed = 2;
        } else if ((c & 0xF0) == 0xE0) {
            if (i + 2 >= string_view.length())
                break;
            code_point = ((c & 0x0F) << 12) | ((string_view[i + 1] & 0x3F) << 6) | (string_view[i + 2] & 0x3F);
            bytes_consumed = 3;
        } else if ((c & 0xF8) == 0xF0) {
            if (i + 3 >= string_view.length())
                break;
            code_point = ((c & 0x07) << 18) | ((string_view[i + 1] & 0x3F) << 12) | ((string_view[i + 2] & 0x3F) << 6) | (string_view[i + 3] & 0x3F);
            bytes_consumed = 4;
        } else {
            i++;
            continue;
        }

        if (code_point == '\n') {
            if (utf16_string.try_append(u'\r').is_error())
                return;
        }

        if (code_point <= 0xFFFF) {
            if (utf16_string.try_append(static_cast<char16_t>(code_point)).is_error())
                return;
        } else if (code_point <= 0x10FFFF) {
            code_point -= 0x10000;
            char16_t high_surrogate = 0xD800 | ((code_point >> 10) & 0x3FF);
            char16_t low_surrogate = 0xDC00 | (code_point & 0x3FF);
            if (utf16_string.try_append(high_surrogate).is_error() || utf16_string.try_append(low_surrogate).is_error())
                return;
        } else {
            i += bytes_consumed;
            continue;
        }

        i += bytes_consumed;
    }

    if (utf16_string.try_append(0).is_error())
        return;

    Kernel::g_efi_system_table->con_out->output_string(Kernel::g_efi_system_table->con_out, bit_cast<char16_t*>(utf16_string.data()));
}
