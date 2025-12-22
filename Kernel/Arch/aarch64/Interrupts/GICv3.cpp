/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/Interrupts/GICv3.h>
#include <Kernel/Arch/aarch64/Interrupts/GICv3Registers.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

// This driver supports both GICv3 and GICv4.
// GICv4 is an extension of GICv3, so we can support both versions in one driver.

namespace Kernel {

// 2.2 INTIDs
static constexpr size_t PRIVATE_PERIPHERAL_INTERRUPT_RANGE_START = 16; // PPI
static constexpr size_t PRIVATE_PERIPHERAL_INTERRUPT_RANGE_END = 32;

static constexpr size_t SHARED_PERIPHERAL_INTERRUPT_RANGE_START = 32; // SPI
static constexpr size_t SHARED_PERIPHERAL_INTERRUPT_RANGE_END = 1020;

static constexpr size_t EXTENDED_PRIVATE_PERIPHERAL_INTERRUPT_RANGE_START = 1056; // Extended PPI
static constexpr size_t EXTENDED_PRIVATE_PERIPHERAL_INTERRUPT_RANGE_END = 1120;

static constexpr size_t EXTENDED_SHARED_PERIPHERAL_INTERRUPT_RANGE_START = 4096; // Extended SPI
static constexpr size_t EXTENDED_SHARED_PERIPHERAL_INTERRUPT_RANGE_END = 5120;

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<GICv3>> GICv3::try_to_initialize(DeviceTree::Device::Resource distributor_registers_resource, Span<DeviceTree::Device::Resource const> redistributor_region_resources, Optional<size_t> redistributor_stride)
{
    if (distributor_registers_resource.size < sizeof(DistributorRegisters))
        return EINVAL;

    Vector<Memory::TypedMapping<RedistributorRegisters volatile>> redistributor_registers;

    auto boot_cpu_mpidr = Aarch64::MPIDR_EL1::read();
    Optional<size_t> boot_cpu_redistributor_index;

    // Detect the redistributors. Each redistributor region can have multiple redistributors.
    for (auto redistributor_region_resource : redistributor_region_resources) {
        PhysicalAddress current_address = redistributor_region_resource.paddr;

        for (;;) {
            if (current_address.get() - redistributor_region_resource.paddr.get() >= redistributor_region_resource.size)
                break;

            auto registers = TRY(Memory::map_typed_writable<RedistributorRegisters volatile>(current_address));

            auto peripheral_id2 = registers->physical_lpis_and_overall_behavior.identification[PhysicalLPIRedistributorRegisters::IDENTIFICATION_PERIPHERAL_ID2_REGISTER_INDEX];
            auto architecture_revision = static_cast<ArchitectureRevision>((peripheral_id2 >> PERIPHERAL_ID2_ARCHITECTURE_REVISION_OFFSET) & PERIPHERAL_ID2_ARCHITECTURE_REVISION_MASK);

            if (architecture_revision != ArchitectureRevision::GICv3 && architecture_revision != ArchitectureRevision::GICv4) {
                dmesgln("GICv3: Unknown redistributor architecture revision: {:#x}", to_underlying(architecture_revision));
                return ENOTSUP;
            }

            auto type = registers->physical_lpis_and_overall_behavior.type;

            TRY(redistributor_registers.try_append(move(registers)));

            // FIXME: Remove m_boot_cpu_redistributor_index once we support SMP on AArch64
            //        and configure every redistributor for each processor.
            u64 type_value = to_underlying(type);
            if ((((type_value >> PhysicalLPIRedistributorRegisters::TYPE_AFF0_OFFSET) & 0xff) == boot_cpu_mpidr.Aff0)
                && (((type_value >> PhysicalLPIRedistributorRegisters::TYPE_AFF1_OFFSET) & 0xff) == boot_cpu_mpidr.Aff1)
                && (((type_value >> PhysicalLPIRedistributorRegisters::TYPE_AFF2_OFFSET) & 0xff) == boot_cpu_mpidr.Aff2)
                && (((type_value >> PhysicalLPIRedistributorRegisters::TYPE_AFF3_OFFSET) & 0xff) == boot_cpu_mpidr.Aff3))
                boot_cpu_redistributor_index = redistributor_registers.size() - 1;

            // "Last, bit [4]
            //  Indicates whether this Redistributor is the highest-numbered Redistributor in a series of contiguous Redistributor pages."
            if (has_flag(type, PhysicalLPIRedistributorRegisters::Type::Last))
                break;

            if (redistributor_stride.has_value()) {
                current_address = current_address.offset(redistributor_stride.value());
            } else {
                // GICv4 has two additional 64 KiB register frames, see 12.10 Redistributor register map.
                if (architecture_revision == ArchitectureRevision::GICv4)
                    current_address = current_address.offset(4uz * 64 * KiB);
                else
                    current_address = current_address.offset(2uz * 64 * KiB);
            }
        }
    }

    if (!boot_cpu_redistributor_index.has_value())
        return ENOENT;

    auto distributor_registers = TRY(Memory::map_typed_writable<DistributorRegisters volatile>(distributor_registers_resource.paddr));

    auto gic = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) GICv3(move(distributor_registers), move(redistributor_registers), boot_cpu_redistributor_index.value())));
    TRY(gic->initialize());

    return gic;
}

void GICv3::enable(GenericInterruptHandler const& handler)
{
    // FIXME: Set the trigger mode in DistributorRegisters::interrupt_configuration (GICD_ICFGRn) to level-triggered or edge-triggered.

    auto interrupt_number = handler.interrupt_number();

    if (interrupt_number < SHARED_PERIPHERAL_INTERRUPT_RANGE_START)
        m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_set_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
    else
        m_distributor_registers->interrupt_set_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
}

void GICv3::disable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();

    if (interrupt_number < SHARED_PERIPHERAL_INTERRUPT_RANGE_START)
        m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_clear_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
    else
        m_distributor_registers->interrupt_clear_enable[interrupt_number / 32] = 1 << (interrupt_number % 32);
}

void GICv3::eoi(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    Aarch64::ICC_EOIR1_EL1::write({ .INTID = interrupt_number });
}

Optional<size_t> GICv3::pending_interrupt() const
{
    auto interrupt_number = Aarch64::ICC_IAR1_EL1::read().INTID;

    // 4.1.1 Physical CPU interface
    // "The effects of reading ICC_IAR0_EL1, ICC_IAR1_EL1, and ICC_NMIAR1_EL1 on the state of a returned INTID
    //  are not guaranteed to be visible until after the execution of a DSB."
    Aarch64::Asm::data_synchronization_barrier<Aarch64::Asm::BarrierLimitation::SY>();

    // 1023 means no pending interrupt.
    if (interrupt_number == 1023)
        return {};

    return interrupt_number;
}

ErrorOr<size_t> GICv3::translate_interrupt_specifier_to_interrupt_number(ReadonlyBytes interrupt_specifier) const
{
    // https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/arm,gic-v3.yaml

    if (interrupt_specifier.size() < 3 * sizeof(u32))
        return EINVAL;

    FixedMemoryStream stream { interrupt_specifier };

    enum class InterruptType : u32 {
        SPI = 0,
        PPI = 1,
        ExtendedSPI = 2,
        ExtendedPPI = 3,
    };

    auto interrupt_type = MUST(stream.read_value<BigEndian<InterruptType>>());
    auto interrupt_number = MUST(stream.read_value<BigEndian<u32>>());
    auto flags = MUST(stream.read_value<BigEndian<u32>>());

    (void)flags; // FIXME: Use this to configure the trigger mode properly.

    switch (interrupt_type) {
    case InterruptType::SPI:
        if (interrupt_number + SHARED_PERIPHERAL_INTERRUPT_RANGE_START >= SHARED_PERIPHERAL_INTERRUPT_RANGE_END)
            return ERANGE;

        return interrupt_number + SHARED_PERIPHERAL_INTERRUPT_RANGE_START;

    case InterruptType::PPI:
        if (interrupt_number + PRIVATE_PERIPHERAL_INTERRUPT_RANGE_START >= PRIVATE_PERIPHERAL_INTERRUPT_RANGE_END)
            return ERANGE;

        return interrupt_number + PRIVATE_PERIPHERAL_INTERRUPT_RANGE_START;

    case InterruptType::ExtendedSPI:
        if (interrupt_number + EXTENDED_SHARED_PERIPHERAL_INTERRUPT_RANGE_START >= EXTENDED_SHARED_PERIPHERAL_INTERRUPT_RANGE_END)
            return ERANGE;

        dbgln("FIXME: Support interrupts in the GICv3 extended SPI range");
        return ENOTIMPL;

    case InterruptType::ExtendedPPI:
        // Extended PPIs: 1056-1119
        // Note: The devicetree binding says that Extended PPIs are in the range [0-127],
        //       but the GIC v3/v4 spec only defines 64 interrupts in the extended PPI range (1119 - 1056 = 63).
        //       We only allow 64 extended PPIs, since the register interface only makes 64 of them available.
        if (interrupt_number + EXTENDED_PRIVATE_PERIPHERAL_INTERRUPT_RANGE_START >= EXTENDED_PRIVATE_PERIPHERAL_INTERRUPT_RANGE_END)
            return ERANGE;

        dbgln("FIXME: Support interrupts in the GICv3 extended PPI range");
        return ENOTIMPL;

    default:
        return EINVAL;
    }
}

UNMAP_AFTER_INIT GICv3::GICv3(Memory::TypedMapping<DistributorRegisters volatile> distributor_registers, Vector<Memory::TypedMapping<RedistributorRegisters volatile>> cpu_interface_registers, size_t boot_cpu_redistributor_index)
    : m_distributor_registers(move(distributor_registers))
    , m_redistributor_registers(move(cpu_interface_registers))
    , m_boot_cpu_redistributor_index(boot_cpu_redistributor_index)
{
}

UNMAP_AFTER_INIT ErrorOr<void> GICv3::initialize()
{
    // https://developer.arm.com/documentation/198123/0302/Configuring-the-Arm-GIC

    // "# Global settings
    //  The Distributor control register (GICD_CTLR) must be configured to enable the interrupt groups
    //  and to set the routing mode as follows:
    //  • Enable Affinity routing (ARE bits): The ARE bits in GICD_CTLR control whether the GIC is operating
    //    in GICv3 mode or legacy mode. Legacy mode provides backwards compatibility with GICv2.
    //    This guide assumes that the ARE bits are set to 1, so that GICv3 mode is being used."
    // We first need to disable both group enable bits, since setting the ARE bit from 0 to 1
    // is UNPREDICTABLE unless both group enable bits are cleared.
    m_distributor_registers->control &= ~(DistributorRegisters::Control::EnableGroup1 | DistributorRegisters::Control::EnableGroup1A);
    m_distributor_registers->control |= DistributorRegisters::Control::AffinityRoutingEnable;

    // "• Enables: GICD_CTLR contains separate enable bits for Group 0, Secure Group 1 and Non-secure Group 1:
    //    ◦ EnableGrp1S enables distribution of Secure Group 1 interrupts.
    //    ◦ EnableGrp1NS enables distribution of Non-secure Group 1 interrupts.
    //    ◦ EnableGrp0 enables distribution of Group 0 interrupts."

    // We only use non-secure group 1 interrupts in this driver. Other groups are simply not usable in non-secure state.
    // Let's keep the interrupt group disabled until all distributor registers are initialized.

    // 12.9.38 GICD_TYPER, Interrupt Controller Type Register:
    // "If the value of this field is N, the maximum SPI INTID is 32(N+1) minus 1."
    // "The ITLinesNumber field only indicates the maximum number of SPIs that the GIC implementation might support.
    //  This value determines the number of instances of the following interrupt registers:
    //    GICD_IGROUPR<n>, GICD_ISENABLER<n>, GICD_ICENABLER<n>, GICD_ISPENDR<n>, GICD_ICPENDR<n>, GICD_ISACTIVER<n>,
    //    GICD_ICACTIVER<n>, GICD_IPRIORITYR<n>, GICD_ITARGETSR<n>, GICD_ICFGR<n>, GICD_IROUTER<n>, GICD_IGRPMODR<n>
    //  The GIC architecture does not require a GIC implementation to support a continuous range of SPI interrupt IDs.
    //  Software must check which SPI INTIDs are supported, up to the maximum value indicated by GICD_TYPER.ITLinesNumber."
    auto it_lines_number = (m_distributor_registers->interrupt_controller_type >> DistributorRegisters::INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_OFFSET) & DistributorRegisters::INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_MASK;
    auto max_spi_range_end = 32 * (it_lines_number + 1);

    // "# SPI, PPI, and SGI configuration
    //  [...]
    //  For each INTID, software must configure the following:
    //  • Priority: GICD_IPRIORITYn, GICR_IPRIORITYn.
    //    Each INTID has an associated priority, represented as an 8-bit unsigned value.
    //    0x00 is the highest possible priority, and 0xFF is the lowest possible priority.
    //    Running priority and preemption describes how the priority value in GICD_IPRIORITYn
    //    and GICR_IPRIORITYn masks low priority interrupts, and how it controls preemption.
    //    An interrupt controller is not required to implement all 8 priority bits. [...]"
    for (size_t i = SHARED_PERIPHERAL_INTERRUPT_RANGE_START / 4; i < max_spi_range_end / 4; i++)
        m_distributor_registers->interrupt_priority[i] = 0x00'00'00'00; // highest priority

    for (size_t i = 0; i < PRIVATE_PERIPHERAL_INTERRUPT_RANGE_END / 8; i++)
        m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_priority[i] = 0x00'00'00'00; // highest priority

    // "• Group: GICD_IGROUPn, GICD_IGRPMODn, GICR_IGROUPn, GICR_IGRPMODn
    //    As described in Security model, an interrupt can be configured to belong to one of the three interrupt groups.
    //    These interrupt groups are Group 0, Secure Group 1 and Non-secure Group 1."
    // Configure all interrupts to non-secure group 1 by setting every group modifier bit to 0 and group status bit to 1.
    for (size_t i = SHARED_PERIPHERAL_INTERRUPT_RANGE_START / 32; i < max_spi_range_end / 32; i++) {
        m_distributor_registers->interrupt_group[i] = 0xffff'ffff;
        m_distributor_registers->interrupt_group_modifier[i] = 0x0000'0000;
    }

    m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_group[0] = 0xffff'ffff;
    m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_group_modifier[0] = 0x0000'0000;

    // "• Edge-triggered or level-sensitive: GICD_ICFGRn, GICR_ICFGRn
    //    For PPIs and SPI, the software must specify whether the interrupt is edge-triggered or level-sensitive.
    //    SGIs are always treated as edge-triggered, and therefore GICR_ICFGR0 behaves as Read-As-One, Writes Ignored (RAO/WI)
    //    for these interrupts."
    // FIXME: Configure the trigger mode in the enable() function.

    // "• Enable: GICD_ISENABLERn, GICD_ICENABLER, GICR_ISENABLERn, GICR_ICENABLERn
    //    Each INTID has an enable bit. Set-enable registers and Clear-enable registers remove the requirement
    //    to perform read-modify-write routines.
    //    Arm recommends that the settings outlined in this section are configured before enabling the INTID."
    // Disable all interrupts by default.
    for (size_t i = SHARED_PERIPHERAL_INTERRUPT_RANGE_START / 32; i < max_spi_range_end / 32; i++)
        m_distributor_registers->interrupt_clear_enable[i] = 0xffff'ffff;

    m_redistributor_registers[m_boot_cpu_redistributor_index]->sgis_and_ppis.interrupt_clear_enable[0] = 0xffff'ffff;

    // "• Non-maskable: Interrupts configured as non-maskable are treated as higher priority than all other interrupts
    //    belonging to the same Group. That is, a non-maskable Non-secure Group 1 interrupt is treated as higher priority
    //    than all other Non-secure Group 1 interrupts.
    //    The non-maskable property is added in GICv3.3 and requires matching support in the PE.
    //    Only Secure Group 1 and Non-secure Group 1 interrupts can be marked as non-maskable."
    // TODO: Implement NMI support. NMIs should be disabled by default, so we don't need to do anything here.

    // Configure all interrupts to target the current processor.
    // FIXME: Once we support SMP on AArch64, we should distribute interrupts across all available processors.
    //        Or if 1 of N distribution is supported by the GIC, enable that feature for each SPI in GICD_IROUTER<n>.
    auto mpidr = Aarch64::MPIDR_EL1::read();
    u64 route = ((static_cast<u64>(mpidr.Aff0) << DistributorRegisters::INTERRUPT_ROUTING_AFF0_OFFSET)
        | (static_cast<u64>(mpidr.Aff1) << DistributorRegisters::INTERRUPT_ROUTING_AFF1_OFFSET)
        | (static_cast<u64>(mpidr.Aff2) << DistributorRegisters::INTERRUPT_ROUTING_AFF2_OFFSET)
        | (static_cast<u64>(mpidr.Aff3) << DistributorRegisters::INTERRUPT_ROUTING_AFF3_OFFSET));

    for (size_t i = SHARED_PERIPHERAL_INTERRUPT_RANGE_START; i < max_spi_range_end; i++)
        m_distributor_registers->interrupt_routing[i] = route;

    // Enable interrupts in non-secure group 1.
    m_distributor_registers->control |= DistributorRegisters::Control::EnableGroup1A;

    // "# Redistributor configuration
    //  [...]
    //  The Redistributor contains a register called GICR_WAKER which is used to record whether the connected PE
    //  is online or offline. Interrupts are only forwarded to a PE that the GIC believes is online.
    //  At reset, all PEs are treated as being offline.
    //
    //  To mark the connected PE as being online, software must:
    //  • Clear GICR_WAKER.ProcessorSleep to 0."
    m_redistributor_registers[m_boot_cpu_redistributor_index]->physical_lpis_and_overall_behavior.wake &= ~PhysicalLPIRedistributorRegisters::Wake::ProcessorSleep;

    // "• Poll GICR_WAKER.ChildrenAsleep until it reads 0."
    while (has_flag(m_redistributor_registers[m_boot_cpu_redistributor_index]->physical_lpis_and_overall_behavior.wake, PhysicalLPIRedistributorRegisters::Wake::ChildrenAsleep))
        Processor::pause();

    // FIXME: We need to configure the redistributor and CPU interface for each processor (PE) once we support SMP on AArch64.

    // "It is important that software performs these steps before configuring the CPU interface,
    //  otherwise behavior can be UNPREDICTABLE."

    // "# CPU interface configuration
    //  The CPU interface is responsible for delivering interrupt exceptions to the PE to which it is connected.
    //  To enable the CPU interface, software must configure the following:
    //
    //  • Enable System register access: The CPU interfaces (ICC_*_ELn) section describes the CPU interface registers,
    //    and how they are accessed as System registers in GICv3. Software must enable access to the CPU interface
    //    registers, by setting the SRE bit in the ICC_SRE_ELn registers."
    Aarch64::ICC_SRE_EL1::write({
        .SRE = 1,
        .DFB = 0,
        .DIB = 0,
    });

    // Ensure that the enabling of the system register access is visible before accessing any CPU interface register.
    Aarch64::Asm::instruction_synchronization_barrier();

    // "• Set Priority Mask and Binary Point registers: The CPU interface contains the Priority Mask register
    //    (ICC_PMR_EL1) and the Binary Point registers (ICC_BPRn_EL1). The Priority Mask sets the minimum priority
    //    that an interrupt must have in order to be forwarded to the PE.
    //    The Binary Point register is used for priority grouping and preemption.
    //    This is described in more detail in End of interrupt."

    // Set the interrupt priority threshold to the max value, so accept any interrupt with a priority below 0xff.
    Aarch64::ICC_PMR_EL1::write({ .Priority = 0xff });
    Aarch64::ICC_BPR1_EL1::write({ .BinaryPoint = 0 });

    // "• Set EOI mode: The EOImode bits in ICC_CTLR_EL1 and ICC_CTLR_EL3 in the CPU interface control how the
    //    completion of an interrupt is handled. This is described in more detail in End of interrupt."
    Aarch64::ICC_CTLR_EL1::write({
        .CBPR = 0,
        .EOImode = 0,
        .PMHE = 0,
        .PRIbits = 0,
        .IDbits = 0,
        .SEIS = 0,
        .A3V = 0,
        .RSS = 0,
        .ExtRange = 0,
    });

    // "• Enable signaling of each interrupt group: The signaling of each interrupt group must be enabled before
    //    interrupts of that group will be forwarded by the CPU interface to the PE. To enable signaling,
    //    software must write to the ICC_IGRPEN1_EL1 register for Group 1 interrupts and ICC_IGRPEN0_EL1 registers
    //    for Group 0 interrupts. ICC_IGRPEN1_EL1 is banked by Security state. This means that ICC_GRPEN1_EL1 controls
    //    Group 1 for the current Security state. At EL3, software can access both Group 1 enables using ICC_IGRPEN1_EL3."
    Aarch64::ICC_IGRPEN1_EL1::write({ .Enable = 1 });

    // Ensure that all cpu interface register changes are visible.
    Aarch64::Asm::instruction_synchronization_barrier();

    return {};
}

static constinit Array const compatibles_array = {
    "arm,gic-v3"sv, // Used by both GICv3 and GICv4.
};

INTERRUPT_CONTROLLER_DEVICETREE_DRIVER(GICv3Driver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/interrupt-controller/arm,gic-v3.yaml
ErrorOr<void> GICv3Driver::probe(DeviceTree::Device const& device, StringView) const
{
    auto distributor_registers_resource = TRY(device.get_resource(0));

    // This is not the number of redistributors itself, but the number of contiguos regions containing the redistributor registers.
    auto number_of_redistributor_regions = device.node().get_u32_property("#redistributor-regions"sv).value_or(1);

    Vector<DeviceTree::Device::Resource> redistributor_region_resources;
    TRY(redistributor_region_resources.try_resize(number_of_redistributor_regions));

    for (size_t i = 0; i < number_of_redistributor_regions; i++)
        redistributor_region_resources[i] = TRY(device.get_resource(i + 1));

    auto redistributor_stride = device.node().get_u64_property("redistributor-stride"sv);

    auto gic = TRY(GICv3::try_to_initialize(distributor_registers_resource, redistributor_region_resources, redistributor_stride));

    MUST(DeviceTree::Management::register_interrupt_controller(device, *gic));
    MUST(InterruptManagement::register_interrupt_controller(move(gic)));

    return {};
}

}
