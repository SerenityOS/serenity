/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/IRQController.h>
#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Firmware/DeviceTree/InterruptController.h>
#include <Kernel/Memory/TypedMapping.h>

// GIC v3/v4 Architecture Specification (rev H.b): ARM IHI 0069, https://developer.arm.com/documentation/ihi0069/hb/
// Learn the architecture - Generic Interrupt Controller v3 and v4, Overview (version 3.2): https://developer.arm.com/documentation/198123/0302/
// Learn the architecture - Generic Interrupt Controller v3 and v4, LPIs (version 1.0): https://developer.arm.com/documentation/102923/0100/

namespace Kernel {

class GICv3 final
    : public IRQController
    , public DeviceTree::InterruptController {
public:
    static ErrorOr<NonnullLockRefPtr<GICv3>> try_to_initialize(DeviceTree::Device::Resource distributor_registers_resource, Span<DeviceTree::Device::Resource const> redistributor_region_resources, Optional<size_t> redistributor_stride);

    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;

    virtual void eoi(GenericInterruptHandler const&) override;

    virtual Optional<size_t> pending_interrupt() const override;

    virtual StringView model() const override { return "GICv3"sv; }

    // ^DeviceTree::InterruptController
    virtual ErrorOr<size_t> translate_interrupt_specifier_to_interrupt_number(ReadonlyBytes) const override;

    struct DistributorRegisters;
    struct RedistributorRegisters;

private:
    GICv3(Memory::TypedMapping<DistributorRegisters volatile>, Vector<Memory::TypedMapping<RedistributorRegisters volatile>>, size_t boot_cpu_redistributor_index);

    ErrorOr<void> initialize();

    Memory::TypedMapping<DistributorRegisters volatile> m_distributor_registers;
    Vector<Memory::TypedMapping<RedistributorRegisters volatile>> m_redistributor_registers;

    size_t m_boot_cpu_redistributor_index;
};

}
