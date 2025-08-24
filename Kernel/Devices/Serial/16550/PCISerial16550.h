/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Serial/16550/Serial16550.h>

namespace Kernel {

class PCISerial16550 {
public:
    static Serial16550& the();
    static bool is_available();
};

}
