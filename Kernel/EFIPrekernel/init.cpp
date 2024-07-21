/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/Runtime.h>

// FIXME: Initialize the __stack_chk_guard with a random value via the EFI_RNG_PROTOCOL or other arch-specific methods.
uintptr_t __stack_chk_guard __attribute__((used));

namespace Kernel {

extern "C" [[noreturn]] void __stack_chk_fail();
extern "C" [[noreturn]] void __stack_chk_fail()
{
    PANIC("Stack protector failure, stack smashing detected!");
}

EFI::Handle g_efi_image_handle = 0;
EFI::SystemTable* g_efi_system_table = nullptr;

static_assert(EFI::EFI_PAGE_SIZE == PAGE_SIZE, "The EFIPrekernel assumes that EFI_PAGE_SIZE == PAGE_SIZE");

extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table);
extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table)
{
    // We use some EFI 1.10 functions from the System Table, so reject older versions.
    static constexpr u32 efi_version_1_10 = (1 << 16) | 10;
    if (system_table->hdr.signature != EFI::SystemTable::signature || system_table->hdr.revision < efi_version_1_10)
        return EFI::Status::Unsupported;

    g_efi_image_handle = image_handle;
    g_efi_system_table = system_table;

    system_table->con_out->set_attribute(system_table->con_out,
        EFI::TextAttribute {
            .foreground_color = EFI::TextAttribute::ForegroundColor::White,
            .background_color = EFI::TextAttribute::BackgroundColor::Black,
        });

    // Clear the screen. This also removes the manufacturer logo, if present.
    system_table->con_out->clear_screen(system_table->con_out);

    ucs2_dbgln(u"SerenityOS EFI Prekernel");

    TODO();
}

}

void __assertion_failed(char const* msg, char const* file, unsigned int line, char const* func)
{
    dbgln("ASSERTION FAILED: {}", msg);
    dbgln("{}:{} in {}", file, line, func);
    Kernel::halt();
}
