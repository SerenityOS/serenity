/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SerialDevice* s_the = nullptr;

UNMAP_AFTER_INIT void PCISerialDevice::detect()
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
                auto serial_device = new SerialDevice(move(port_registers_io_window), current_device_minor++);
                if (board_definition.baud_rate != SerialDevice::Baud::Baud38400) // non-default baud
                    serial_device->set_baud(board_definition.baud_rate);

                // If this is the first port of the first pci serial device, store it as the debug PCI serial port (TODO: Make this configurable somehow?)
                if (!is_available())
                    s_the = serial_device;
                // NOTE: We intentionally leak the reference to serial_device here.
            }

            dmesgln("PCISerialDevice: Found {} @ {}", board_definition.name, device_identifier.address());
            return;
        }
    }));
}

SerialDevice& PCISerialDevice::the()
{
    VERIFY(s_the);
    return *s_the;
}

bool PCISerialDevice::is_available()
{
    return s_the;
}

}
