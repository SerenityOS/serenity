/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/SerialDevice.h>

namespace Kernel {

class PCISerialDevice {
    AK_MAKE_ETERNAL
public:
    static void detect();
    static SerialDevice& the();
    static bool is_available();

private:
    struct BoardDefinition {
        PCI::HardwareID device_id;
        StringView name;
        u32 port_count { 0 };
        u32 pci_bar { 0 };
        u32 first_offset { 0 };
        u32 port_size { 0 };
        SerialDevice::Baud baud_rate { SerialDevice::Baud::Baud38400 };
    };

    static constexpr BoardDefinition board_definitions[4] = {
        { { PCI::VendorID::WCH, 0x3253 }, "WCH CH382 2S", 2, 0, 0xC0, 8, SerialDevice::Baud::Baud115200 },
        { { PCI::VendorID::RedHat, 0x0002 }, "QEMU PCI 16550A", 1, 0, 0, 8, SerialDevice::Baud::Baud115200 },
        { { PCI::VendorID::RedHat, 0x0003 }, "QEMU PCI Dual-port 16550A", 2, 0, 0, 8, SerialDevice::Baud::Baud115200 },
        { { PCI::VendorID::RedHat, 0x0004 }, "QEMU PCI Quad-port 16550A", 4, 0, 0, 8, SerialDevice::Baud::Baud115200 }
    };
};

}
