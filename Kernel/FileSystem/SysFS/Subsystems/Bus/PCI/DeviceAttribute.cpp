/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceAttribute.h>
#include <Kernel/Sections.h>

namespace Kernel {

StringView PCIDeviceAttributeSysFSComponent::name() const
{
    switch (m_offset) {
    case PCI::RegisterOffset::VENDOR_ID:
        return "vendor"sv;
    case PCI::RegisterOffset::DEVICE_ID:
        return "device_id"sv;
    case PCI::RegisterOffset::CLASS:
        return "class"sv;
    case PCI::RegisterOffset::SUBCLASS:
        return "subclass"sv;
    case PCI::RegisterOffset::REVISION_ID:
        return "revision"sv;
    case PCI::RegisterOffset::PROG_IF:
        return "progif"sv;
    case PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID:
        return "subsystem_vendor"sv;
    case PCI::RegisterOffset::SUBSYSTEM_ID:
        return "subsystem_id"sv;
    case PCI::RegisterOffset::BAR0:
        return "bar0"sv;
    case PCI::RegisterOffset::BAR1:
        return "bar1"sv;
    case PCI::RegisterOffset::BAR2:
        return "bar2"sv;
    case PCI::RegisterOffset::BAR3:
        return "bar3"sv;
    case PCI::RegisterOffset::BAR4:
        return "bar4"sv;
    case PCI::RegisterOffset::BAR5:
        return "bar5"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<PCIDeviceAttributeSysFSComponent> PCIDeviceAttributeSysFSComponent::create(PCIDeviceSysFSDirectory const& device, PCI::RegisterOffset offset, size_t field_bytes_width)
{
    return adopt_ref(*new (nothrow) PCIDeviceAttributeSysFSComponent(device, offset, field_bytes_width));
}

PCIDeviceAttributeSysFSComponent::PCIDeviceAttributeSysFSComponent(PCIDeviceSysFSDirectory const& device, PCI::RegisterOffset offset, size_t field_bytes_width)
    : SysFSComponent()
    , m_device(device)
    , m_offset(offset)
    , m_field_bytes_width(field_bytes_width)
{
}

ErrorOr<size_t> PCIDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> PCIDeviceAttributeSysFSComponent::try_to_generate_buffer() const
{
    OwnPtr<KString> value;
    SpinlockLocker locker(m_device->device_identifier().operation_lock());
    switch (m_field_bytes_width) {
    case 1:
        value = TRY(KString::formatted("{:#x}", PCI::read8_locked(m_device->device_identifier(), m_offset)));
        break;
    case 2:
        value = TRY(KString::formatted("{:#x}", PCI::read16_locked(m_device->device_identifier(), m_offset)));
        break;
    case 4:
        value = TRY(KString::formatted("{:#x}", PCI::read32_locked(m_device->device_identifier(), m_offset)));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return KBuffer::try_create_with_bytes("PCIDeviceAttributeSysFSComponent: Device address"sv, value->view().bytes());
}
}
