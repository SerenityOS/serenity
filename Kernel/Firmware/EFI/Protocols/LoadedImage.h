/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/Protocols/DevicePath.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

// https://uefi.org/specs/UEFI/2.10/09_Protocols_EFI_Loaded_Image.html

namespace Kernel::EFI {

// EFI_LOADED_IMAGE_PROTOCOL: https://uefi.org/specs/UEFI/2.10/09_Protocols_EFI_Loaded_Image.html#efi-loaded-image-protocol
struct LoadedImageProtocol {
    static constexpr GUID guid = { 0x5b1b31a1, 0x9562, 0x11d2, { 0x8e, 0x3f, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    using ImageUnloadFn = EFIAPI Status (*)(Handle* image_handle);

    u32 revision;
    Handle parent_handle;
    SystemTable* system_table;

    // Source location of the image
    Handle device_handle;
    DevicePathProtocol* file_path;
    void* reserved;

    // Image’s load options
    u32 load_options_size;
    void* load_options;

    // Location where image was loaded
    void* image_base;
    u64 image_size;
    MemoryType image_code_type;
    MemoryType image_data_type;
    ImageUnloadFn unload;
};
static_assert(AssertSize<LoadedImageProtocol, 96>());

}
