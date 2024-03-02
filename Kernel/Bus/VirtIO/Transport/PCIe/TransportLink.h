/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/VirtIO/Transport/Entity.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::VirtIO {

class PCIeTransportLink final
    : public TransportEntity {
public:
    static ErrorOr<NonnullOwnPtr<TransportEntity>> create(PCI::Device const&);
    virtual StringView determine_device_class_name() const override;

private:
    explicit PCIeTransportLink(PCI::Device const&);

    // ^TransportEntity
    virtual ErrorOr<void> locate_configurations_and_resources(Badge<VirtIO::Device>, VirtIO::Device&) override;
    virtual void disable_interrupts(Badge<VirtIO::Device>) override;
    virtual void enable_interrupts(Badge<VirtIO::Device>) override;

    ErrorOr<void> create_interrupt_handler(VirtIO::Device&);

    // FIXME: There could be multiple IRQ (MSI-X) handlers for a VirtIO device.
    // Find a way to use all of them.
    OwnPtr<PCI::IRQHandler> m_irq_handler;
    NonnullRefPtr<PCI::Device> const m_pci_device;
};
};
