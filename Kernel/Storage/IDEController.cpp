/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATADiskDevice.h>
#include <Kernel/Storage/BMIDEChannel.h>
#include <Kernel/Storage/IDEController.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<IDEController> IDEController::initialize(PCI::Address address, bool force_pio)
{
    return adopt_ref(*new IDEController(address, force_pio));
}

bool IDEController::reset()
{
    TODO();
}

bool IDEController::shutdown()
{
    TODO();
}

size_t IDEController::devices_count() const
{
    size_t count = 0;
    for (u32 index = 0; index < 4; index++) {
        if (!device(index).is_null())
            count++;
    }
    return count;
}

void IDEController::start_request(const ATADevice& device, AsyncBlockDeviceRequest& request)
{
    auto& address = device.ata_address();
    VERIFY(address.subport < 2);
    switch (address.port) {
    case 0:
        m_channels[0].start_request(request, address.subport == 0 ? false : true, device.ata_capabilites());
        return;
    case 1:
        m_channels[1].start_request(request, address.subport == 0 ? false : true, device.ata_capabilites());
        return;
    }
    VERIFY_NOT_REACHED();
}

void IDEController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT IDEController::IDEController(PCI::Address address, bool force_pio)
    : ATAController()
    , PCI::Device(address)
{
    PCI::enable_io_space(address);
    PCI::enable_memory_space(address);
    initialize(force_pio);
}

UNMAP_AFTER_INIT IDEController::~IDEController()
{
}

bool IDEController::is_pci_native_mode_enabled() const
{
    return (PCI::get_programming_interface(pci_address()) & 0x05) != 0;
}

bool IDEController::is_pci_native_mode_enabled_on_primary_channel() const
{
    return (PCI::get_programming_interface(pci_address()) & 0x1) == 0x1;
}

bool IDEController::is_pci_native_mode_enabled_on_secondary_channel() const
{
    return (PCI::get_programming_interface(pci_address()) & 0x4) == 0x4;
}

bool IDEController::is_bus_master_capable() const
{
    return PCI::get_programming_interface(pci_address()) & (1 << 7);
}

static const char* detect_controller_type(u8 programming_value)
{
    switch (programming_value) {
    case 0x00:
        return "ISA Compatibility mode-only controller";
    case 0x05:
        return "PCI native mode-only controller";
    case 0x0A:
        return "ISA Compatibility mode controller, supports both channels switched to PCI native mode";
    case 0x0F:
        return "PCI native mode controller, supports both channels switched to ISA compatibility mode";
    case 0x80:
        return "ISA Compatibility mode-only controller, supports bus mastering";
    case 0x85:
        return "PCI native mode-only controller, supports bus mastering";
    case 0x8A:
        return "ISA Compatibility mode controller, supports both channels switched to PCI native mode, supports bus mastering";
    case 0x8F:
        return "PCI native mode controller, supports both channels switched to ISA compatibility mode, supports bus mastering";
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void IDEController::initialize(bool force_pio)
{
    auto bus_master_base = IOAddress(PCI::get_BAR4(pci_address()) & (~1));
    dbgln("IDE controller @ {}: bus master base was set to {}", pci_address(), bus_master_base);
    dbgln("IDE controller @ {}: interrupt line was set to {}", pci_address(), PCI::get_interrupt_line(pci_address()));
    dbgln("IDE controller @ {}: {}", pci_address(), detect_controller_type(PCI::get_programming_interface(pci_address())));
    dbgln("IDE controller @ {}: primary channel DMA capable? {}", pci_address(), ((bus_master_base.offset(2).in<u8>() >> 5) & 0b11));
    dbgln("IDE controller @ {}: secondary channel DMA capable? {}", pci_address(), ((bus_master_base.offset(2 + 8).in<u8>() >> 5) & 0b11));

    if (!is_bus_master_capable())
        force_pio = true;

    auto bar0 = PCI::get_BAR0(pci_address());
    auto primary_base_io = (bar0 == 0x1 || bar0 == 0) ? IOAddress(0x1F0) : IOAddress(bar0 & (~1));
    auto bar1 = PCI::get_BAR1(pci_address());
    auto primary_control_io = (bar1 == 0x1 || bar1 == 0) ? IOAddress(0x3F6) : IOAddress(bar1 & (~1));
    auto bar2 = PCI::get_BAR2(pci_address());
    auto secondary_base_io = (bar2 == 0x1 || bar2 == 0) ? IOAddress(0x170) : IOAddress(bar2 & (~1));
    auto bar3 = PCI::get_BAR3(pci_address());
    auto secondary_control_io = (bar3 == 0x1 || bar3 == 0) ? IOAddress(0x376) : IOAddress(bar3 & (~1));

    auto irq_line = PCI::get_interrupt_line(pci_address());
    if (is_pci_native_mode_enabled()) {
        VERIFY(irq_line != 0);
    }

    if (is_pci_native_mode_enabled_on_primary_channel()) {
        if (force_pio)
            m_channels.append(IDEChannel::create(*this, irq_line, { primary_base_io, primary_control_io }, IDEChannel::ChannelType::Primary));
        else
            m_channels.append(BMIDEChannel::create(*this, irq_line, { primary_base_io, primary_control_io, bus_master_base }, IDEChannel::ChannelType::Primary));
    } else {
        if (force_pio)
            m_channels.append(IDEChannel::create(*this, { primary_base_io, primary_control_io }, IDEChannel::ChannelType::Primary));
        else
            m_channels.append(BMIDEChannel::create(*this, { primary_base_io, primary_control_io, bus_master_base }, IDEChannel::ChannelType::Primary));
    }

    m_channels[0].enable_irq();

    if (is_pci_native_mode_enabled_on_secondary_channel()) {
        if (force_pio)
            m_channels.append(IDEChannel::create(*this, irq_line, { secondary_base_io, secondary_control_io }, IDEChannel::ChannelType::Secondary));
        else
            m_channels.append(BMIDEChannel::create(*this, irq_line, { secondary_base_io, secondary_control_io, bus_master_base.offset(8) }, IDEChannel::ChannelType::Secondary));
    } else {
        if (force_pio)
            m_channels.append(IDEChannel::create(*this, { secondary_base_io, secondary_control_io }, IDEChannel::ChannelType::Secondary));
        else
            m_channels.append(BMIDEChannel::create(*this, { secondary_base_io, secondary_control_io, bus_master_base.offset(8) }, IDEChannel::ChannelType::Secondary));
    }

    m_channels[1].enable_irq();
}

RefPtr<StorageDevice> IDEController::device_by_channel_and_position(u32 index) const
{
    switch (index) {
    case 0:
        return m_channels[0].master_device();
    case 1:
        return m_channels[0].slave_device();
    case 2:
        return m_channels[1].master_device();
    case 3:
        return m_channels[1].slave_device();
    }
    VERIFY_NOT_REACHED();
}

RefPtr<StorageDevice> IDEController::device(u32 index) const
{
    NonnullRefPtrVector<StorageDevice> connected_devices;
    for (size_t index = 0; index < 4; index++) {
        auto checked_device = device_by_channel_and_position(index);
        if (checked_device.is_null())
            continue;
        connected_devices.append(checked_device.release_nonnull());
    }
    if (index >= connected_devices.size())
        return nullptr;
    return connected_devices[index];
}
}
