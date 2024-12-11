/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/Interrupts/GIC.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {

// 4.1.2 Distributor register map
struct GIC::DistributorRegisters {
    enum class ControlBits : u32 {
        Enable = 1 << 0,
    };

    static constexpr size_t INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_OFFSET = 0;
    static constexpr u32 INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_MASK = (1 << 5) - 1;

    u32 control;                    // GICD_CTLR
    u32 interrupt_controller_type;  // GICR_TYPER
    u32 implementer_identification; // GICD_IIDR
    u32 reserved0[5];
    u32 implementation_defined0[8];
    u32 reserved1[16];
    u32 interrupt_group[32];         // GICD_IGROUPn
    u32 interrupt_set_enable[32];    // GICD_ISENABLERn
    u32 interrupt_clear_enable[32];  // GICD_ICENABLERn
    u32 interrupt_set_pending[32];   // GICD_ISPENDRn
    u32 interrupt_clear_pending[32]; // GICD_ICPENDRn
    u32 set_active[32];              // GICD_ISACTIVERn
    u32 clear_active[32];            // GICD_ICACTIVERn
    u32 interrupt_priority[255];     // GICD_IPRIORITYRn
    u32 reserved2;
    u32 interrupt_processor_targets[255]; // GICD_ITARGETSRn
    u32 reserved3;
    u32 interrupt_configuration[64]; // GICD_ICFGRn
    u32 reserved4[64];
    u32 non_secure_access_control[64]; // GICD_NSACRn
    u32 software_generated_interrupt;  // GICD_SGIR
    u32 reserved5[3];
    u32 software_generated_interrupt_clear_pending[4]; // GICD_CPENDSGIRn
    u32 software_generated_interrupt_set_pending[4];   // GICD_SPENDSGIRn
    u32 reserved6[40];
    u32 implementation_defined1[12];
};
static_assert(AssertSize<GIC::DistributorRegisters, 0x1000>());

AK_ENUM_BITWISE_OPERATORS(GIC::DistributorRegisters::ControlBits);

// 4.1.3 CPU interface register map
struct GIC::CPUInterfaceRegisters {
    enum class ControlBits : u32 {
        Enable = 1 << 0,
    };

    static constexpr size_t IDENTIFICATION_ARCHITECTURE_VERSION_OFFSET = 16;
    static constexpr u32 IDENTIFICATION_ARCHITECTURE_VERSION_MASK = (1 << 4) - 1;

    u32 control;                                    // GICC_CTLR
    u32 interrupt_priority_mask;                    // GICC_PMR, only the 8 bottom bits are valid
    u32 binary_point;                               // GICC_BPR
    u32 interrupt_acknowledge;                      // GICC_IAR
    u32 end_of_interrupt;                           // GICC_EOIR
    u32 running_priority;                           // GICC_RPR, only the 8 bottom bits are valid
    u32 highest_priority_pending_interrupt;         // GICC_HPPIR
    u32 aliased_binary_point;                       // GICC_ABPR
    u32 aliased_interrupt_acknowledge;              // GICC_AIAR
    u32 aliased_end_of_interrupt;                   // GICC_AEOIR
    u32 aliased_highest_priority_pending_interrupt; // GICC_AHPPIR
    u32 reserved0[5];
    u32 implementation_defined0[36];
    u32 active_priorities[4];            // GICC_APRn
    u32 non_secure_active_priorities[4]; // GICC_NSAPRn
    u32 reserved1[3];
    u32 identification; // GICC_IIDR
    u32 reserved2[960];
    u32 deactivate_interrupt; // GICC_DIR
};
static_assert(AssertSize<GIC::CPUInterfaceRegisters, 0x1004>());

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<GIC>> GIC::try_to_initialize(DeviceTree::Device::Resource distributor_registers_resource, DeviceTree::Device::Resource cpu_interface_registers_resource)
{
    if (distributor_registers_resource.size < sizeof(DistributorRegisters))
        return EINVAL;

    if (cpu_interface_registers_resource.size < sizeof(CPUInterfaceRegisters))
        return EINVAL;

    auto distributor_registers = TRY(Memory::map_typed_writable<DistributorRegisters volatile>(distributor_registers_resource.paddr));
    auto cpu_interface_registers = TRY(Memory::map_typed_writable<CPUInterfaceRegisters volatile>(cpu_interface_registers_resource.paddr));

    auto gic = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) GIC(move(distributor_registers), move(cpu_interface_registers))));
    TRY(gic->initialize());

    return gic;
}

void GIC::enable(GenericInterruptHandler const& handler)
{
    // FIXME: Set the trigger mode in DistributorRegisters::interrupt_configuration (GICD_ICFGRn) to level-triggered or edge-triggered.

    auto interrupt_number = handler.interrupt_number();
    m_distributor_registers->interrupt_set_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
}

void GIC::disable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    m_distributor_registers->interrupt_clear_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
}

void GIC::eoi(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    m_cpu_interface_registers->end_of_interrupt = interrupt_number;
}

Optional<size_t> GIC::pending_interrupt() const
{
    auto interrupt_number = m_cpu_interface_registers->interrupt_acknowledge;

    // 1023 means no pending interrupt.
    if (interrupt_number == 1023)
        return {};

    return interrupt_number;
}

UNMAP_AFTER_INIT GIC::GIC(Memory::TypedMapping<DistributorRegisters volatile> distributor_registers, Memory::TypedMapping<CPUInterfaceRegisters volatile> cpu_interface_registers)
    : m_distributor_registers(move(distributor_registers))
    , m_cpu_interface_registers(move(cpu_interface_registers))
{
}

UNMAP_AFTER_INIT ErrorOr<void> GIC::initialize()
{
    auto gic_architecture_version = (m_cpu_interface_registers->identification >> CPUInterfaceRegisters::IDENTIFICATION_ARCHITECTURE_VERSION_OFFSET) & CPUInterfaceRegisters::IDENTIFICATION_ARCHITECTURE_VERSION_MASK;
    if (gic_architecture_version != 2)
        return ENOTSUP; // We currently only support GICv2.

    // Disable forwarding of interrupts to the CPU interfaces during initialization.
    m_distributor_registers->control &= ~to_underlying(DistributorRegisters::ControlBits::Enable);

    auto it_lines_number = (m_distributor_registers->interrupt_controller_type >> DistributorRegisters::INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_OFFSET) & DistributorRegisters::INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_MASK;

    // 4.3.2 Interrupt Controller Type Register, GICD_TYPER:
    // "If ITLinesNumber=N, the maximum number of interrupts is 32(N+1)."
    // "The ITLinesNumber field only indicates the maximum number of SPIs that the GIC might support. This value determines the number of implemented interrupt registers [...]"
    u32 const max_number_of_interrupts = 32 * (it_lines_number + 1);

    // Disable all interrupts and mark them as non-pending.
    for (size_t i = 0; i < max_number_of_interrupts / 32; i++) {
        m_distributor_registers->interrupt_clear_enable[i] = 0xffff'ffff;
        m_distributor_registers->interrupt_clear_pending[i] = 0xffff'ffff;
        m_distributor_registers->clear_active[i] = 0xffff'ffff;
    }

    // Initialize the priority of all interrupts to 0 (the highest priority) and configure them to target all processors.
    for (size_t i = 0; i < max_number_of_interrupts / 4; i++) {
        m_distributor_registers->interrupt_priority[i] = 0;
        m_distributor_registers->interrupt_processor_targets[i] = static_cast<u32>(explode_byte(0xff));
    }

    // FIXME: We need to configure the CPU interface for each processor once we support SMP.

    // Set the interrupt priority threshold to the max value, so accept any interrupt with a priority below 0xff.
    m_cpu_interface_registers->interrupt_priority_mask = 0xff;

    // Enable the distributor and CPU interface.
    m_cpu_interface_registers->control |= to_underlying(CPUInterfaceRegisters::ControlBits::Enable);
    m_distributor_registers->control |= to_underlying(DistributorRegisters::ControlBits::Enable);

    return {};
}

static constinit Array const compatibles_array = {
    "arm,gic-400"sv,
    "arm,cortex-a15-gic"sv,
};

DEVICETREE_DRIVER(GICDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/arm,gic.yaml
ErrorOr<void> GICDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto distributor_registers_resource = TRY(device.get_resource(0));
    auto cpu_interface_registers_resource = TRY(device.get_resource(1));

    DeviceTree::DeviceRecipe<NonnullLockRefPtr<IRQController>> recipe {
        name(),
        device.node_name(),
        [distributor_registers_resource, cpu_interface_registers_resource] {
            return GIC::try_to_initialize(distributor_registers_resource, cpu_interface_registers_resource);
        },
    };

    InterruptManagement::add_recipe(move(recipe));

    return {};
}

}
