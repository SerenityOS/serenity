/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/MemoryDevice.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<MemoryDevice> MemoryDevice::must_create()
{
    return MUST(Device::try_create_device<MemoryDevice>());
}

UNMAP_AFTER_INIT MemoryDevice::MemoryDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Generic, 1)
{
}

UNMAP_AFTER_INIT MemoryDevice::~MemoryDevice() = default;

ErrorOr<size_t> MemoryDevice::read(OpenFileDescription&, u64 offset, UserOrKernelBuffer& buffer, size_t length)
{
    if (!MM.is_allowed_to_read_physical_memory_for_userspace(PhysicalAddress(offset), length)) {
        dbgln_if(MEMORY_DEVICE_DEBUG, "MemoryDevice: Trying to read physical memory at {} for range of {} bytes failed due to violation of access", PhysicalAddress(offset), length);
        return EINVAL;
    }
    auto mapping = TRY(Memory::map_typed<u8>(PhysicalAddress(offset), length));

    auto bytes = ReadonlyBytes { mapping.ptr(), length };
    TRY(buffer.write(bytes));
    return length;
}

ErrorOr<File::VMObjectAndMemoryType> MemoryDevice::vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const& range, u64& offset, bool)
{
    auto viewed_address = PhysicalAddress(offset);

    // Note: This check happens to guard against possible memory leak.
    // For example, if we try to mmap physical memory from 0x1000 to 0x2000 and you
    // can actually mmap only from 0x1001, then we would fail as usual.
    // However, in such case if we mmap from 0x1002, we are technically not violating
    // any rules, besides the fact that we mapped an entire page with two bytes which we
    // were not supposed to see. To prevent that, if we use mmap(2) syscall, we should
    // always consider the start page to be aligned on PAGE_SIZE, or to be more precise
    // is to be set to the page base of that start address.
    VERIFY(viewed_address == viewed_address.page_base());

    if (!MM.is_allowed_to_read_physical_memory_for_userspace(viewed_address, range.size())) {
        dbgln_if(MEMORY_DEVICE_DEBUG, "MemoryDevice: Trying to mmap physical memory at {} for range of {} bytes failed due to violation of access", viewed_address, range.size());
        return EINVAL;
    }

    offset = 0;
    return VMObjectAndMemoryType {
        .vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(viewed_address, range.size())),
        .memory_type = Memory::MemoryType::IO,
    };
}

}
