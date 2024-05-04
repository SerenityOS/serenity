/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <Kernel/API/DeviceFileTypes.h>

// NOTE: PLEASE READ THIS NOTE CAREFULLY WHEN CHANGING THIS FILE!
// When adding a major number allocation, you must:
// 1. Add a new allocation based on its Type (either Block or Character)
// 2. Make sure the family_name is not already taken! Duplicates are not allowed.
//    Make sure the family_name string is informative and short.
//    Prepare a CamelCase name for the chosen family_name, and insert it in the appropriate
//    enum class - either CharacterDeviceFamily or BlockDeviceFamily.
// 3. Make sure you put the allocation in the correct array - either s_char_device_numbers
//    or s_block_device_numbers.
// 4. Make sure you put the allocation and CamelCase for family name in ascending order.
//    For example, if you allocate a number for new family of character devices, say 30, then you
//    must put it after allocated number 29 (if so exists, if not look to the biggest existing number below 30)
//    but not after the smallest existing number after 30 (which might be 31).
//    Please note that you must insert in while keeping the ascending order, otherwise
//    the code will not compile due to a failed static_assert().

namespace Kernel::MajorAllocation {

enum class CharacterDeviceFamily : unsigned {
    Generic = 0,
    DeviceControl,
    Serial,
    Console,
    Mouse,
    FUSE,
    AllMice,
    GPURender,
    VirtualConsole,
    Keyboard,
    Audio,
    MasterPTY,
    SlavePTY,
    GPU,
    VirtIOConsole,
    __END,
};

struct CharacterDeviceNumber {
    CharacterDeviceFamily family;
    MajorNumber const allocated_number;
    StringView family_name;
};

static constexpr Array<CharacterDeviceNumber, to_underlying(CharacterDeviceFamily::__END)> s_char_device_numbers = {
    CharacterDeviceNumber { CharacterDeviceFamily::Generic, 1, "generic"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::DeviceControl, 2, "devctl"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::Serial, 4, "serial"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::Console, 5, "console"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::Mouse, 10, "mouse"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::FUSE, 11, "fuse"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::AllMice, 12, "all-mice"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::GPURender, 28, "gpu-render"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::VirtualConsole, 35, "virtual-console"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::Keyboard, 85, "keyboard"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::Audio, 116, "audio"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::MasterPTY, 200, "master-pty"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::SlavePTY, 201, "slave-pty"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::GPU, 226, "gpu"sv },
    CharacterDeviceNumber { CharacterDeviceFamily::VirtIOConsole, 229, "virtio-console"sv },
};

static_assert(s_char_device_numbers.size() == to_underlying(CharacterDeviceFamily::__END));
constexpr bool s_char_dev_numbers_in_order()
{
    for (unsigned index = 0; index < s_char_device_numbers.size(); ++index) {
        if ((to_underlying(s_char_device_numbers[index].family) != index))
            return false;
    }

    for (size_t index = 0; index < s_char_device_numbers.size() - 1; ++index) {
        if (!(s_char_device_numbers[index].allocated_number.value() < s_char_device_numbers[index + 1].allocated_number.value()))
            return false;
    }
    return true;
}
static_assert(s_char_dev_numbers_in_order() == true);

enum class BlockDeviceFamily : unsigned {
    Storage = 0,
    Loop,
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    KCOV,
#endif
    StoragePartition,
    __END,
};

struct BlockDeviceNumber {
    BlockDeviceFamily family;
    MajorNumber const allocated_number;
    StringView family_name;
};

static constexpr Array<BlockDeviceNumber, to_underlying(BlockDeviceFamily::__END)> s_block_device_numbers = {
    BlockDeviceNumber { BlockDeviceFamily::Storage, 3, "storage"sv },
    BlockDeviceNumber { BlockDeviceFamily::Loop, 20, "loop"sv },
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    BlockDeviceNumber { BlockDeviceFamily::KCOV, 30, "kcov"sv },
#endif
    BlockDeviceNumber { BlockDeviceFamily::StoragePartition, 100, "storage-partition"sv },
};

static_assert(s_block_device_numbers.size() == to_underlying(BlockDeviceFamily::__END));
constexpr bool s_block_dev_numbers_in_order()
{
    for (unsigned index = 0; index < s_block_device_numbers.size(); ++index) {
        if ((to_underlying(s_block_device_numbers[index].family) != index))
            return false;
    }

    for (size_t index = 0; index < s_block_device_numbers.size() - 1; ++index) {
        if (!(s_block_device_numbers[index].allocated_number.value() < s_block_device_numbers[index + 1].allocated_number.value()))
            return false;
    }
    return true;
}
static_assert(s_block_dev_numbers_in_order() == true);

}
