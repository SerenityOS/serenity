/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>

// https://uefi.org/specs/UEFI/2.10/10_Protocols_Device_Path_Protocol.html

namespace Kernel::EFI {

// EFI_DEVICE_PATH_PROTOCOL: https://uefi.org/specs/UEFI/2.10/10_Protocols_Device_Path_Protocol.html#efi-device-path-protocol
struct DevicePathProtocol {
    static constexpr GUID guid = { 0x09576e91, 0x6d3f, 0x11d2, { 0x8e, 0x39, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    u8 type;
    u8 sub_type;
    u8 length[2];
};
static_assert(AssertSize<DevicePathProtocol, 4>());

// EFI_DEVICE_PATH_FROM_TEXT_PROTOCOL: https://uefi.org/specs/UEFI/2.10/10_Protocols_Device_Path_Protocol.html#device-path-from-text-protocol
struct DevicePathFromTextProtocol {
    static constexpr GUID guid = { 0x5c99a21, 0xc70f, 0x4ad2, { 0x8a, 0x5f, 0x35, 0xdf, 0x33, 0x43, 0xf5, 0x1e } };

    using DevicePathFromTextNodeFn = EFIAPI DevicePathProtocol (*)(char16_t const* text_device_node);
    using DevicePathFromTextPathFn = EFIAPI DevicePathProtocol (*)(char16_t const* text_device_path);

    DevicePathFromTextNodeFn convert_text_to_device_node;
    DevicePathFromTextPathFn convert_text_to_device_path;
};
static_assert(AssertSize<DevicePathFromTextProtocol, 16>());

}
