/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/PCISerialDevice.h>

namespace Kernel {

static SerialDevice* s_the = nullptr;

void PCISerialDevice::detect()
{
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;

        // HACK: There's currently no way to break out of PCI::enumerate, so we just early return if we already initialized the pci serial device
        if (is_available())
            return;

        for (auto& board_definition : board_definitions) {
            if (board_definition.device_id != id)
                continue;

            auto bar_base = PCI::get_BAR(address, board_definition.pci_bar) & ~1;
            // FIXME: We should support more than 1 PCI serial port (per card/multiple devices)
            s_the = new SerialDevice(IOAddress(bar_base + board_definition.first_offset), 64);
            if (board_definition.baud_rate != SerialDevice::Baud::Baud38400) // non-default baud
                s_the->set_baud(board_definition.baud_rate);

            dmesgln("PCISerialDevice: Found {} @ {}", board_definition.name, address);
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
