/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceAttribute.h>
#include <Kernel/Sections.h>

namespace Kernel {

StringView StorageDeviceAttributeSysFSComponent::name() const
{
    switch (m_type) {
    case Type::EndLBA:
        return "last_lba"sv;
    case Type::SectorSize:
        return "sector_size"sv;
    case Type::CommandSet:
        return "command_set"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<StorageDeviceAttributeSysFSComponent> StorageDeviceAttributeSysFSComponent::must_create(StorageDeviceSysFSDirectory const& device_directory, Type type)
{
    return adopt_ref(*new (nothrow) StorageDeviceAttributeSysFSComponent(device_directory, type));
}

StorageDeviceAttributeSysFSComponent::StorageDeviceAttributeSysFSComponent(StorageDeviceSysFSDirectory const& device_directory, Type type)
    : SysFSComponent()
    , m_device(device_directory.device({}))
    , m_type(type)
{
}

ErrorOr<size_t> StorageDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> StorageDeviceAttributeSysFSComponent::try_to_generate_buffer() const
{
    OwnPtr<KString> value;
    switch (m_type) {
    case Type::EndLBA:
        value = TRY(KString::formatted("{}", m_device->max_addressable_block()));
        break;
    case Type::SectorSize:
        value = TRY(KString::formatted("{}", m_device->block_size()));
        break;
    case Type::CommandSet:
        value = TRY(KString::formatted("{}", m_device->command_set_to_string_view()));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return KBuffer::try_create_with_bytes("SysFS StorageDeviceAttributeComponent buffer"sv, value->view().bytes());
}
}
