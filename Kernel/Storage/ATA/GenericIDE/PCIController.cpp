/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Storage/ATA/GenericIDE/PCIController.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<PCIIDEController> PCIIDEController::initialize(PCI::DeviceIdentifier const& device_identifier, bool force_pio)
{
    return adopt_lock_ref(*new PCIIDEController(device_identifier, force_pio));
}

UNMAP_AFTER_INIT PCIIDEController::PCIIDEController(PCI::DeviceIdentifier const& device_identifier, bool force_pio)
    : PCI::Device(device_identifier.address())
    , m_prog_if(device_identifier.prog_if())
    , m_interrupt_line(device_identifier.interrupt_line())
{
    PCI::enable_io_space(device_identifier.address());
    PCI::enable_memory_space(device_identifier.address());
    PCI::enable_bus_mastering(device_identifier.address());
    enable_pin_based_interrupts();
    initialize(force_pio);
}

bool PCIIDEController::is_pci_native_mode_enabled() const
{
    return (m_prog_if.value() & 0x05) != 0;
}

bool PCIIDEController::is_pci_native_mode_enabled_on_primary_channel() const
{
    return (m_prog_if.value() & 0x1) == 0x1;
}

bool PCIIDEController::is_pci_native_mode_enabled_on_secondary_channel() const
{
    return (m_prog_if.value() & 0x4) == 0x4;
}

bool PCIIDEController::is_bus_master_capable() const
{
    return m_prog_if.value() & (1 << 7);
}

static char const* detect_controller_type(u8 programming_value)
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

UNMAP_AFTER_INIT void PCIIDEController::initialize(bool force_pio)
{
    auto bus_master_base = IOAddress(PCI::get_BAR4(pci_address()) & (~1));
    dbgln("IDE controller @ {}: bus master base was set to {}", pci_address(), bus_master_base);
    dbgln("IDE controller @ {}: interrupt line was set to {}", pci_address(), m_interrupt_line.value());
    dbgln("IDE controller @ {}: {}", pci_address(), detect_controller_type(m_prog_if.value()));
    dbgln("IDE controller @ {}: primary channel DMA capable? {}", pci_address(), ((bus_master_base.offset(2).in<u8>() >> 5) & 0b11));
    dbgln("IDE controller @ {}: secondary channel DMA capable? {}", pci_address(), ((bus_master_base.offset(2 + 8).in<u8>() >> 5) & 0b11));

    auto initialize_and_enumerate = [&force_pio](IDEChannel& channel) -> void {
        {
            auto result = channel.allocate_resources_for_pci_ide_controller({}, force_pio);
            // FIXME: Propagate errors properly
            VERIFY(!result.is_error());
        }
        {
            auto result = channel.detect_connected_devices();
            // FIXME: Propagate errors properly
            VERIFY(!result.is_error());
        }
    };

    if (!is_bus_master_capable())
        force_pio = true;

    auto bar0 = PCI::get_BAR0(pci_address());
    auto bar1 = PCI::get_BAR1(pci_address());
    auto bar2 = PCI::get_BAR2(pci_address());
    auto bar3 = PCI::get_BAR3(pci_address());

    auto primary_base_io = (bar0 == 0x1 || bar0 == 0) ? IOAddress(0x1F0) : IOAddress(bar0 & (~1));
    // Note: the PCI IDE specification says we should access the IO address with an offset of 2
    // on native PCI IDE controllers.
    auto primary_control_io = (bar1 == 0x1 || bar1 == 0) ? IOAddress(0x3F6) : IOAddress((bar1 & (~1)) | 2);

    auto secondary_base_io = (bar2 == 0x1 || bar2 == 0) ? IOAddress(0x170) : IOAddress(bar2 & (~1));
    // Note: the PCI IDE specification says we should access the IO address with an offset of 2
    // on native PCI IDE controllers.
    auto secondary_control_io = (bar3 == 0x1 || bar3 == 0) ? IOAddress(0x376) : IOAddress((bar3 & (~1)) | 2);

    // FIXME: On IOAPIC based system, this value might be completely wrong
    // On QEMU for example, it should be "u8 irq_line = 22;" to actually work.
    auto irq_line = m_interrupt_line.value();

    if (is_pci_native_mode_enabled()) {
        VERIFY(irq_line != 0);
    }

    if (is_pci_native_mode_enabled_on_primary_channel()) {
        m_channels.append(IDEChannel::create(*this, irq_line, { primary_base_io, primary_control_io, bus_master_base }, IDEChannel::ChannelType::Primary));
    } else {
        m_channels.append(IDEChannel::create(*this, { primary_base_io, primary_control_io, bus_master_base }, IDEChannel::ChannelType::Primary));
    }
    initialize_and_enumerate(m_channels[0]);
    m_channels[0].enable_irq();

    if (is_pci_native_mode_enabled_on_secondary_channel()) {
        m_channels.append(IDEChannel::create(*this, irq_line, { secondary_base_io, secondary_control_io, bus_master_base.offset(8) }, IDEChannel::ChannelType::Secondary));
    } else {
        m_channels.append(IDEChannel::create(*this, { secondary_base_io, secondary_control_io, bus_master_base.offset(8) }, IDEChannel::ChannelType::Secondary));
    }
    initialize_and_enumerate(m_channels[1]);
    m_channels[1].enable_irq();
}

}
