/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::RPi {

class RP1 {
public:
    static ErrorOr<void> try_to_initialize_xhci_controllers(PCI::DeviceIdentifier const&);
};

}
