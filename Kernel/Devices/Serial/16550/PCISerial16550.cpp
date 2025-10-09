/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Driver.h>
#include <Kernel/Devices/Serial/16550/PCISerial16550.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Serial16550* s_the = nullptr;

Serial16550& PCISerial16550::the()
{
    VERIFY(s_the);
    return *s_the;
}

bool PCISerial16550::is_available()
{
    return s_the;
}

struct BoardDefinition {
    PCI::HardwareID device_id;
    StringView name;
    u32 port_count { 0 };
    u32 pci_bar { 0 };
    u32 first_offset { 0 };
    u32 port_size { 0 };
    Serial16550::Baud baud_rate { Serial16550::Baud::Baud38400 };
};

static constexpr BoardDefinition board_definitions[] = {
    { { PCI::VendorID::RedHat, 0x0002 }, "QEMU PCI 16550A"sv, 1, 0, 0, 8, Serial16550::Baud::Baud115200 },
    { { PCI::VendorID::RedHat, 0x0003 }, "QEMU PCI Dual-port 16550A"sv, 2, 0, 0, 8, Serial16550::Baud::Baud115200 },
    { { PCI::VendorID::RedHat, 0x0004 }, "QEMU PCI Quad-port 16550A"sv, 4, 0, 0, 8, Serial16550::Baud::Baud115200 },
    { { PCI::VendorID::WCH, 0x2273 }, "WCH CH351"sv, 2, 0, 0, 8, Serial16550::Baud::Baud115200 },
    { { PCI::VendorID::WCH, 0x3253 }, "WCH CH382 2S"sv, 2, 0, 0xC0, 8, Serial16550::Baud::Baud115200 }
};

static constexpr BoardDefinition generic_board_definition = {
    .device_id = { 0xffff, 0xffff },
    .name = "Generic 16550-compatible UART"sv,
    .port_count = 1,
    .pci_bar = 0,
    .first_offset = 0,
    .port_size = 8,
    .baud_rate = Serial16550::Baud::Baud115200,
};

size_t s_current_device_minor = 4;

PCI_DRIVER(Serial16550Driver);

ErrorOr<void> Serial16550Driver::probe(PCI::DeviceIdentifier const& pci_device_identifier) const
{
    if (pci_device_identifier.class_code() != PCI::ClassID::SimpleCommunication
        || pci_device_identifier.subclass_code() != PCI::SimpleCommunication::SubclassID::SerialController)
        return ENOTSUP;

    auto initialize_serial_device = [&pci_device_identifier](BoardDefinition const& board_definition) {
        auto registers_io_window = IOWindow::create_for_pci_device_bar(pci_device_identifier, static_cast<PCI::HeaderType0BaseRegister>(board_definition.pci_bar)).release_value_but_fixme_should_propagate_errors();
        auto first_offset_registers_io_window = registers_io_window->create_from_io_window_with_offset(board_definition.first_offset).release_value_but_fixme_should_propagate_errors();

        for (size_t i = 0; i < board_definition.port_count; i++) {
            auto port_registers_io_window = first_offset_registers_io_window->create_from_io_window_with_offset(board_definition.port_size * i).release_value_but_fixme_should_propagate_errors();
            auto serial_device = Device::try_create_device<Serial16550>(move(port_registers_io_window), s_current_device_minor++).release_value_but_fixme_should_propagate_errors();
            if (board_definition.baud_rate != Serial16550::Baud::Baud38400) // non-default baud
                serial_device->set_baud(board_definition.baud_rate);

            // If this is the first port of the first pci serial device, store it as the debug PCI serial port (TODO: Make this configurable somehow?)
            if (s_the == nullptr)
                s_the = &serial_device.leak_ref(); // NOTE: We intentionally leak the reference to serial_device here.
        }

        dmesgln("PCISerial16550: Found {} @ {}", board_definition.name, pci_device_identifier.address());
    };

    for (auto const& board_definition : board_definitions) {
        if (board_definition.device_id != pci_device_identifier.hardware_id())
            continue;

        initialize_serial_device(board_definition);
        return {};
    }

    // If we don't have a special board definition for this device and it's 16550-compatible, use a generic board definition.
    if (first_is_one_of(pci_device_identifier.prog_if(),
            PCI::SimpleCommunication::SerialControllerProgIf::CompatbileWith16550,
            PCI::SimpleCommunication::SerialControllerProgIf::CompatbileWith16650,
            PCI::SimpleCommunication::SerialControllerProgIf::CompatbileWith16750,
            PCI::SimpleCommunication::SerialControllerProgIf::CompatbileWith16850,
            PCI::SimpleCommunication::SerialControllerProgIf::CompatbileWith16950)) {
        initialize_serial_device(generic_board_definition);
        return {};
    }

    return ENOTSUP;
}

}
