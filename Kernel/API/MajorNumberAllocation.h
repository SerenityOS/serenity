/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Platform.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/API/DeviceFileTypes.h>

// NOTE: Please refer to Documentation/Kernel/DevelopmentGuidelines.md
// to observe how to add new allocations.

namespace Kernel::MajorAllocation {

enum class CharacterDeviceFamily : unsigned {
    Generic = 1,
    DeviceControl = 2,
    Serial = 4,
    Console = 5,
    Mouse = 10,
    FUSE = 11,
    GPURender = 28,
    VirtualConsole = 35,
    Keyboard = 85,
    Audio = 116,
    MasterPTY = 200,
    SlavePTY = 201,
    GPU = 226,
    VirtIOConsole = 229,
};

static constexpr CharacterDeviceFamily s_character_device_numbers[] = {
    CharacterDeviceFamily::Generic,
    CharacterDeviceFamily::DeviceControl,
    CharacterDeviceFamily::Serial,
    CharacterDeviceFamily::Console,
    CharacterDeviceFamily::Mouse,
    CharacterDeviceFamily::FUSE,
    CharacterDeviceFamily::GPURender,
    CharacterDeviceFamily::VirtualConsole,
    CharacterDeviceFamily::Keyboard,
    CharacterDeviceFamily::Audio,
    CharacterDeviceFamily::MasterPTY,
    CharacterDeviceFamily::SlavePTY,
    CharacterDeviceFamily::GPU,
    CharacterDeviceFamily::VirtIOConsole,
};

constexpr bool assert_character_device_numbers_are_in_order()
{
    unsigned major = 0;
    for (auto& allocation : s_character_device_numbers) {
        if (to_underlying(allocation) < major)
            return false;
        major = to_underlying(allocation);
    }
    return true;
}
static_assert(assert_character_device_numbers_are_in_order());

ALWAYS_INLINE MajorNumber character_device_family_to_major_number(CharacterDeviceFamily family)
{
    return static_cast<unsigned>(family);
}

ALWAYS_INLINE StringView character_device_family_to_string_view(CharacterDeviceFamily family)
{
    switch (family) {
    case CharacterDeviceFamily::Generic:
        return "generic"sv;
    case CharacterDeviceFamily::DeviceControl:
        return "devctl"sv;
    case CharacterDeviceFamily::Serial:
        return "serial"sv;
    case CharacterDeviceFamily::Console:
        return "console"sv;
    case CharacterDeviceFamily::Mouse:
        return "mouse"sv;
    case CharacterDeviceFamily::FUSE:
        return "fuse"sv;
    case CharacterDeviceFamily::GPURender:
        return "gpu-render"sv;
    case CharacterDeviceFamily::VirtualConsole:
        return "virtual-console"sv;
    case CharacterDeviceFamily::Keyboard:
        return "keyboard"sv;
    case CharacterDeviceFamily::Audio:
        return "audio"sv;
    case CharacterDeviceFamily::MasterPTY:
        return "master-pty"sv;
    case CharacterDeviceFamily::SlavePTY:
        return "slave-pty"sv;
    case CharacterDeviceFamily::GPU:
        return "gpu"sv;
    case CharacterDeviceFamily::VirtIOConsole:
        return "virtio-console"sv;
    }

    VERIFY_NOT_REACHED();
}

enum class BlockDeviceFamily : unsigned {
    Storage = 3,
    Loop = 20,
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    KCOV = 30,
#endif
    StoragePartition = 100,
};

static constexpr BlockDeviceFamily s_block_device_numbers[] = {
    BlockDeviceFamily::Storage,
    BlockDeviceFamily::Loop,
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    BlockDeviceFamily::KCOV,
#endif
    BlockDeviceFamily::StoragePartition,
};

constexpr bool assert_block_device_numbers_are_in_order()
{
    unsigned major = 0;
    for (auto& allocation : s_block_device_numbers) {
        if (to_underlying(allocation) < major)
            return false;
        major = to_underlying(allocation);
    }
    return true;
}
static_assert(assert_block_device_numbers_are_in_order());

ALWAYS_INLINE MajorNumber block_device_family_to_major_number(BlockDeviceFamily family)
{
    return static_cast<unsigned>(family);
}

inline StringView block_device_family_to_string_view(BlockDeviceFamily family)
{
    switch (family) {
    case BlockDeviceFamily::Storage:
        return "storage"sv;
    case BlockDeviceFamily::Loop:
        return "loop"sv;
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    case BlockDeviceFamily::KCOV:
        return "kcov"sv;
#endif
    case BlockDeviceFamily::StoragePartition:
        return "storage-partition"sv;
    }

    VERIFY_NOT_REACHED();
}

}
