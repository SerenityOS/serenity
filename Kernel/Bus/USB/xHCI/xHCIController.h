/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/xHCI/Registers.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::USB::xHCI {

class xHCIController : public USBController
    , public PCI::Device {
public:
    static constexpr auto BaseRegister = PCI::HeaderType0BaseRegister::BAR0;

    static ErrorOr<NonnullLockRefPtr<xHCIController>> try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier);
    virtual ~xHCIController() override = default;

    // ^PCI::Device
    virtual StringView device_name() const override { return "xHCI"sv; }

    // ^USBController
    virtual ErrorOr<void> initialize() override;

    virtual ErrorOr<void> reset() override { return ENOTSUP; }
    virtual ErrorOr<void> stop() override { return ENOTSUP; }
    virtual ErrorOr<void> start() override { return ENOTSUP; }

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer>) override {};
    virtual ErrorOr<size_t> submit_control_transfer(Transfer&) override { return ENOTSUP; }
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer&) override { return ENOTSUP; }
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer>, u16) override { return ENOTSUP; }

    virtual ErrorOr<void> set_device_configuration(USB::Device&, USBConfiguration const&) override { return ENOTSUP; }

private:
    xHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<Memory::Region> register_region);

    NonnullOwnPtr<Memory::Region> m_register_region;
    // FIXME: As stated in Registers.h, QEMU enforces 32 bit reads on the capability registers, which the spec does not seem to do
    //        So to enforce full sized reads, we also need to make it volatile
    CapabilityRegisters const volatile* m_cap_regs;
    OperationalRegisters volatile* m_op_regs;
    RuntimeRegisters volatile* m_runtime_regs;
    DoorbellRegister volatile* m_doorbell_regs;
};

}
