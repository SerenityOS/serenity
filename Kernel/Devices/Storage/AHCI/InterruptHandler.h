/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/AHCI/Controller.h>
#include <Kernel/Devices/Storage/AHCI/Port.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class AHCIController;
class AHCIPort;
class AHCIInterruptHandler final : public PCI::IRQHandler {
    friend class AHCIController;

public:
    static ErrorOr<NonnullOwnPtr<AHCIInterruptHandler>> create(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);
    virtual ~AHCIInterruptHandler() override;

    virtual StringView purpose() const override { return "SATA IRQ Handler"sv; }

    bool is_responsible_for_port_index(u32 port_index) const { return m_taken_ports.is_set_at(port_index); }

private:
    AHCIInterruptHandler(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);

    void allocate_resources_and_initialize_ports();

    //^ IRQHandler
    virtual bool handle_irq() override;

    enum class Direction : u8 {
        Read,
        Write,
    };

    AHCI::MaskedBitField create_pending_ports_interrupts_bitfield() const;

    // Data members
    NonnullLockRefPtr<AHCIController> m_parent_controller;
    AHCI::MaskedBitField m_taken_ports;
    AHCI::MaskedBitField m_pending_ports_interrupts;
};
}
