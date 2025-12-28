/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/IRQController.h>
#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Firmware/DeviceTree/InterruptController.h>
#include <Kernel/Memory/TypedMapping.h>

// GICv2 Architecture Specification: https://documentation-service.arm.com/static/5f8ff21df86e16515cdbfafe
// GIC-400 Technical Reference Manual: https://documentation-service.arm.com/static/5e8f15e27100066a414f7424

namespace Kernel {

class GICv2 final
    : public IRQController
    , public DeviceTree::InterruptController {
public:
    static ErrorOr<NonnullLockRefPtr<GICv2>> try_to_initialize(DeviceTree::Device::Resource distributor_registers_resource, DeviceTree::Device::Resource cpu_interface_registers_registers_resource);

    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;

    virtual void eoi(GenericInterruptHandler const&) override;

    virtual Optional<size_t> pending_interrupt() const override;

    virtual StringView model() const override { return "GICv2"sv; }

    // ^DeviceTree::InterruptController
    virtual ErrorOr<size_t> translate_interrupt_specifier_to_interrupt_number(ReadonlyBytes) const override;

    struct DistributorRegisters;
    struct CPUInterfaceRegisters;

private:
    GICv2(Memory::TypedMapping<DistributorRegisters volatile>, Memory::TypedMapping<CPUInterfaceRegisters volatile>);

    ErrorOr<void> initialize();

    Memory::TypedMapping<DistributorRegisters volatile> m_distributor_registers;
    Memory::TypedMapping<CPUInterfaceRegisters volatile> m_cpu_interface_registers;
};

}
