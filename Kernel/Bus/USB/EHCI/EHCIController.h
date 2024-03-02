/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/EHCI/Registers.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::USB::EHCI {

class EHCIController : public USBController {
    KERNEL_MAKE_DRIVER_LISTABLE(EHCIController)
public:
    static ErrorOr<NonnullRefPtr<EHCIController>> try_to_initialize(PCI::Device& pci_device);
    virtual ~EHCIController() override = default;

    // ^USBController
    virtual ErrorOr<void> initialize() override;

    virtual ErrorOr<void> reset() override { return ENOTSUP; }
    virtual ErrorOr<void> stop() override { return ENOTSUP; }
    virtual ErrorOr<void> start() override { return ENOTSUP; }

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer>) override {};
    virtual ErrorOr<size_t> submit_control_transfer(Transfer&) override { return ENOTSUP; }
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer&) override { return ENOTSUP; }
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer>, u16) override { return ENOTSUP; }

private:
    EHCIController(PCI::Device&, NonnullOwnPtr<Memory::Region> register_region, VirtualAddress register_base_address);

    NonnullRefPtr<PCI::Device> const m_pci_device;
    NonnullOwnPtr<Memory::Region> m_register_region;
    CapabilityRegisters const* m_cap_regs;
    OperationalRegisters volatile* m_op_regs;
};
}
