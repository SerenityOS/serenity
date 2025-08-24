/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Serial/NS16550/NS16550.h>

namespace Kernel {

class PCINS16550 {
public:
    static NS16550& the();
    static bool is_available();
};

}
