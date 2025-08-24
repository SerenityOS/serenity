/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Devices/Serial/16550/PCISerial16550.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Serial16550* s_the = nullptr;

UNMAP_AFTER_INIT void PCISerial16550::detect()
{
    size_t current_device_minor = 4;
    MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        for (auto& board_definition : board_definitions) {
            if (board_definition.device_id != device_identifier.hardware_id())
                continue;

            auto registers_io_window = IOWindow::create_for_pci_device_bar(device_identifier, static_cast<PCI::HeaderType0BaseRegister>(board_definition.pci_bar)).release_value_but_fixme_should_propagate_errors();
            auto first_offset_registers_io_window = registers_io_window->create_from_io_window_with_offset(board_definition.first_offset).release_value_but_fixme_should_propagate_errors();

            for (size_t i = 0; i < board_definition.port_count; i++) {
                auto port_registers_io_window = first_offset_registers_io_window->create_from_io_window_with_offset(board_definition.port_size * i).release_value_but_fixme_should_propagate_errors();
                auto serial_device = Device::try_create_device<Serial16550>(move(port_registers_io_window), current_device_minor++).release_value_but_fixme_should_propagate_errors();
                if (board_definition.baud_rate != Serial16550::Baud::Baud38400) // non-default baud
                    serial_device->set_baud(board_definition.baud_rate);

                // If this is the first port of the first pci serial device, store it as the debug PCI serial port (TODO: Make this configurable somehow?)
                if (!is_available())
                    s_the = &serial_device.leak_ref(); // NOTE: We intentionally leak the reference to serial_device here.
            }

            dmesgln("PCISerial16550: Found {} @ {}", board_definition.name, device_identifier.address());
            return;
        }
    }));
}

Serial16550& PCISerial16550::the()
{
    VERIFY(s_the);
    return *s_the;
}

bool PCISerial16550::is_available()
{
    return s_the;
}

}
