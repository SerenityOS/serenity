/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Devices/Storage/ATA/PIIX4IDE/Controller.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PIIX4IDEController>> PIIX4IDEController::initialize(PCI::Device& device, bool force_pio)
{
    auto controller = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PIIX4IDEController(device)));
    device.enable_io_space();
    device.enable_memory_space();
    device.enable_bus_mastering();
    ArmedScopeGuard disable_interrupts_on_failure([&] {
        device.disable_pin_based_interrupts();
    });
    device.enable_pin_based_interrupts();
    TRY(controller->initialize_and_enumerate_channels(force_pio));
    disable_interrupts_on_failure.disarm();
    return controller;
}

PIIX4IDEController::PIIX4IDEController(PCI::Device& device)
    : m_pci_device(device)
{
}

bool PIIX4IDEController::is_pci_native_mode_enabled() const
{
    return (m_pci_device->device_id().prog_if().value() & 0x05) != 0;
}

bool PIIX4IDEController::is_pci_native_mode_enabled_on_primary_channel() const
{
    return (m_pci_device->device_id().prog_if().value() & 0x1) == 0x1;
}

bool PIIX4IDEController::is_pci_native_mode_enabled_on_secondary_channel() const
{
    return (m_pci_device->device_id().prog_if().value() & 0x4) == 0x4;
}

bool PIIX4IDEController::is_bus_master_capable() const
{
    return m_pci_device->device_id().prog_if().value() & (1 << 7);
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

ErrorOr<void> PIIX4IDEController::initialize_and_enumerate_channels(bool force_pio)
{
    dbgln("IDE controller @ {}: interrupt line was set to {}", m_pci_device->device_id().address(), m_pci_device->device_id().interrupt_line().value());
    dbgln("IDE controller @ {}: {}", m_pci_device->device_id().address(), detect_controller_type(m_pci_device->device_id().prog_if().value()));
    {
        auto bus_master_base = IOAddress(m_pci_device->resources()[4].address & (~1));
        dbgln("IDE controller @ {}: bus master base was set to {}", m_pci_device->device_id().address(), bus_master_base);
    }

    auto initialize_and_enumerate = [&force_pio](IDEChannel& channel) -> ErrorOr<void> {
        TRY(channel.allocate_resources_for_pci_ide_controller({}, force_pio));
        TRY(channel.detect_connected_devices());
        return {};
    };

    if (!is_bus_master_capable())
        force_pio = true;

    OwnPtr<IOWindow> primary_base_io_window;
    OwnPtr<IOWindow> primary_control_io_window;
    if (!is_pci_native_mode_enabled_on_primary_channel()) {
        primary_base_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x1F0), 8));
        primary_control_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x3F6), 4));
    } else {
        primary_base_io_window = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR0));
        auto pci_primary_control_io_window = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR1));
        // Note: the PCI IDE specification says we should access the IO address with an offset of 2
        // on native PCI IDE controllers.
        primary_control_io_window = TRY(pci_primary_control_io_window->create_from_io_window_with_offset(2, 4));
    }

    VERIFY(primary_base_io_window);
    VERIFY(primary_control_io_window);

    OwnPtr<IOWindow> secondary_base_io_window;
    OwnPtr<IOWindow> secondary_control_io_window;

    if (!is_pci_native_mode_enabled_on_primary_channel()) {
        secondary_base_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x170), 8));
        secondary_control_io_window = TRY(IOWindow::create_for_io_space(IOAddress(0x376), 4));
    } else {
        secondary_base_io_window = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR2));
        auto pci_secondary_control_io_window = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR3));
        // Note: the PCI IDE specification says we should access the IO address with an offset of 2
        // on native PCI IDE controllers.
        secondary_control_io_window = TRY(pci_secondary_control_io_window->create_from_io_window_with_offset(2, 4));
    }
    VERIFY(secondary_base_io_window);
    VERIFY(secondary_control_io_window);

    auto primary_bus_master_io = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR4, 16));
    auto secondary_bus_master_io = TRY(primary_bus_master_io->create_from_io_window_with_offset(8));

    // FIXME: On IOAPIC based system, this value might be completely wrong
    // On QEMU for example, it should be "u8 irq_line = 22;" to actually work.
    auto irq_line = m_pci_device->device_id().interrupt_line().value();

    if (is_pci_native_mode_enabled()) {
        VERIFY(irq_line != 0);
    }

    auto primary_channel_io_window_group = IDEChannel::IOWindowGroup { primary_base_io_window.release_nonnull(), primary_control_io_window.release_nonnull(), move(primary_bus_master_io) };
    auto secondary_channel_io_window_group = IDEChannel::IOWindowGroup { secondary_base_io_window.release_nonnull(), secondary_control_io_window.release_nonnull(), move(secondary_bus_master_io) };

    if (is_pci_native_mode_enabled_on_primary_channel()) {
        m_channels[0] = TRY(IDEChannel::create(*this, irq_line, move(primary_channel_io_window_group), IDEChannel::ChannelType::Primary));
    } else {
        m_channels[0] = TRY(IDEChannel::create(*this, move(primary_channel_io_window_group), IDEChannel::ChannelType::Primary));
    }
    TRY(initialize_and_enumerate(*m_channels[0]));
    m_channels[0]->enable_irq();

    if (is_pci_native_mode_enabled_on_secondary_channel()) {
        m_channels[1] = TRY(IDEChannel::create(*this, irq_line, move(secondary_channel_io_window_group), IDEChannel::ChannelType::Secondary));
    } else {
        m_channels[1] = TRY(IDEChannel::create(*this, move(secondary_channel_io_window_group), IDEChannel::ChannelType::Secondary));
    }
    TRY(initialize_and_enumerate(*m_channels[1]));
    m_channels[1]->enable_irq();
    return {};
}

}
