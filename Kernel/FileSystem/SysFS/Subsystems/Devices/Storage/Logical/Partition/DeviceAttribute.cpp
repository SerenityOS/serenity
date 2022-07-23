/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceAttribute.h>
#include <Kernel/Sections.h>

namespace Kernel {

StringView PartitionDeviceAttributeSysFSComponent::name() const
{
    switch (m_type) {
    case Type::StartLBA:
        return "start_lba"sv;
    case Type::EndLBA:
        return "end_lba"sv;
    case Type::UUID:
        return "uuid"sv;
    case Type::PartitionType:
        return "partition_type"sv;
    case Type::Attributes:
        return "attributes"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<PartitionDeviceAttributeSysFSComponent> PartitionDeviceAttributeSysFSComponent::must_create(PartitionDeviceSysFSDirectory const& device_directory, Type type)
{
    return adopt_ref(*new (nothrow) PartitionDeviceAttributeSysFSComponent(device_directory, type));
}

PartitionDeviceAttributeSysFSComponent::PartitionDeviceAttributeSysFSComponent(PartitionDeviceSysFSDirectory const& device_directory, Type type)
    : SysFSComponent()
    , m_device(device_directory.device({}))
    , m_type(type)
{
}

ErrorOr<size_t> PartitionDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> PartitionDeviceAttributeSysFSComponent::try_to_generate_buffer() const
{
    OwnPtr<KString> value;
    switch (m_type) {
    case Type::StartLBA:
        value = TRY(KString::formatted("{}", m_device->metadata().start_block()));
        break;
    case Type::EndLBA:
        value = TRY(KString::formatted("{}", m_device->metadata().end_block()));
        break;
    case Type::UUID:
        VERIFY(!m_device->metadata().unique_guid().is_zero());
        value = TRY(m_device->metadata().unique_guid().to_string());
        break;
    case Type::PartitionType: {
        auto& type = m_device->metadata().type();
        if (type.is_uuid()) {
            value = TRY(type.to_uuid().to_string());
        } else {
            value = TRY(KString::formatted("{:#x}", type.to_byte_indicator()));
        }
        break;
    }
    case Type::Attributes: {
        u64 attributes = 0;
        if (m_device->metadata().special_attributes().has_value())
            attributes = m_device->metadata().special_attributes().value();
        value = TRY(KString::formatted("{:#x}", attributes));
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return KBuffer::try_create_with_bytes("SysFS PartitionDeviceAttributeSysFSComponent buffer"sv, value->view().bytes());
}
}
