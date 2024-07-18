/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/EFIPrekernel/GOP.h>
#include <Kernel/EFIPrekernel/Globals.h>

namespace Kernel {

void init_gop_and_populate_framebuffer_boot_info(BootInfo& boot_info)
{
    auto gop_guid = EFI::GraphicsOutputProtocol::guid;
    EFI::GraphicsOutputProtocol* gop;
    if (g_efi_system_table->boot_services->locate_protocol(&gop_guid, nullptr, reinterpret_cast<void**>(&gop)) != EFI::Status::Success) {
        dbgln("GOP not available");
        return;
    }

    // Choose the mode with the highest pixel count.
    EFI::GraphicsOutputModeInformation chosen_mode_info {};
    ssize_t chosen_mode_number = -1;

    // NOTE: MaxMode is the number of supported modes, not the highest mode number.
    for (u32 mode_number = 0; mode_number < gop->mode->max_mode; mode_number++) {
        FlatPtr size_of_mode_info;
        EFI::GraphicsOutputModeInformation* mode_info;

        if (auto status = gop->query_mode(gop, mode_number, &size_of_mode_info, &mode_info); status != EFI::Status::Success) {
            dbgln("Failed to query GOP mode {}: {}", mode_number, status);
            continue;
        }

        if (mode_info->pixel_format == EFI::GraphicsPixelFormat::BlueGreenRedReserved8BitPerColor
            && mode_info->vertical_resolution * mode_info->horizontal_resolution > chosen_mode_info.vertical_resolution * chosen_mode_info.horizontal_resolution) {
            chosen_mode_info = *mode_info;
            chosen_mode_number = mode_number;
        }
    }

    if (chosen_mode_number == -1) {
        dbgln("No usable GOP mode found");
        return;
    }

    if (auto status = gop->set_mode(gop, chosen_mode_number); status != EFI::Status::Success) {
        dbgln("Failed to set GOP mode {}: {}", chosen_mode_number, status);
        return;
    }

    dbgln("Chosen GOP mode: {}x{} (mode {})", chosen_mode_info.horizontal_resolution, chosen_mode_info.vertical_resolution, chosen_mode_number);

    boot_info.boot_framebuffer = {
        .paddr = PhysicalAddress { gop->mode->frame_buffer_base },
        .pitch = gop->mode->info->pixels_per_scan_line * sizeof(u32),
        .width = gop->mode->info->horizontal_resolution,
        .height = gop->mode->info->vertical_resolution,
        .bpp = 32,
        .type = BootFramebufferType::BGRx8888,
    };
}

}
