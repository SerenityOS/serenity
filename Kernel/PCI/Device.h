/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/PCI/Definitions.h>

namespace Kernel {
class PCI::Device : public IRQHandler {
public:
    Address pci_address() const { return m_pci_address; };

protected:
    Device(Address pci_address);
    Device(Address pci_address, u8 interrupt_vector);
    ~Device();

private:
    Address m_pci_address;
};
}
