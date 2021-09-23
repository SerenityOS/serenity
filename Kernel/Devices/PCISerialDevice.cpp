/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SerialDevice* s_the = nullptr;

UNMAP_AFTER_INIT void PCISerialDevice::detect()
{
    size_t current_device_minor = 68;
    PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
        for (auto& board_definition : board_definitions) {
            if (board_definition.device_id != device_identifier.hardware_id())
                continue;

            auto bar_base = PCI::get_BAR(device_identifier.address(), board_definition.pci_bar) & ~1;
            auto port_base = IOAddress(bar_base + board_definition.first_offset);
            for (size_t i = 0; i < board_definition.port_count; i++) {
                auto serial_device = new SerialDevice(port_base.offset(board_definition.port_size * i), current_device_minor++);
                if (board_definition.baud_rate != SerialDevice::Baud::Baud38400) // non-default baud
                    serial_device->set_baud(board_definition.baud_rate);

                // If this is the first port of the first pci serial device, store it as the debug PCI serial port (TODO: Make this configurable somehow?)
                if (!is_available())
                    s_the = serial_device;
                // NOTE: We intentionally leak the reference to serial_device here, as it is eternal
            }

            dmesgln("PCISerialDevice: Found {} @ {}", board_definition.name, device_identifier.address());
            return;
        }
    });
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
