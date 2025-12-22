/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/aarch64/Interrupts/GICv3.h>

namespace Kernel {

static constexpr size_t PERIPHERAL_ID2_ARCHITECTURE_REVISION_OFFSET = 4;
static constexpr size_t PERIPHERAL_ID2_ARCHITECTURE_REVISION_MASK = (1 << 4) - 1;

enum class ArchitectureRevision : u8 {
    GICv1 = 0x1,
    GICv2 = 0x2,
    GICv3 = 0x3,
    GICv4 = 0x4,
};

// Note: All definitions assume that we are in non-secure state
//       for registers that behave differently depending on the security state.

// 12.8 The GIC Distributor register map
struct GICv3::DistributorRegisters {
    enum class Control : u32 {
        // If only a single Security state is supported: Enables group 0
        // If two Security states are supported
        //    - and AffinityRoutingEnable == 1: Enables non-secure group 1 interrupts
        //    - and AffinityRoutingEnable == 0: RES0
        EnableGroup1 = 1u << 0, // EnableGrp1/EnableGrp1

        // If only a single Security state is supported: Enables group 1
        // If two Security states are supported
        //    - and AffinityRoutingEnable == 1: RES0
        //    - and AffinityRoutingEnable == 0: Enables non-scure group 1 interrupts
        EnableGroup1A = 1u << 1, // EnableGrp0/EnableGrp1A

        // Setting this bit disables GICv2 backwards compatibility.
        // It may be permanently set to 1 if legacy mode isn't supported.
        AffinityRoutingEnable = 1u << 4, // ARE_NS

        // "Register Write Pending. Read only. Indicates whether a register write is in progress or not."
        //  0b0  No register write in progress. The effects of previous register writes to the affected
        //       register fields are visible to all logical components of the GIC architecture, including
        //       the CPU interfaces.
        //  0b1  Register write in progress. The effects of previous register writes to the affected register
        //       fields are not guaranteed to be visible to all logical components of the GIC architecture,
        //       including the CPU interfaces, as the effects of the changes are still being propagated.
        //  This field tracks writes to:
        //  • GICD_CTLR[2:0], the Group Enables, for transitions from 1 to 0 only.
        //  • GICD_CTLR[7:4], the ARE bits, E1NWF bit and DS bit.
        //  • GICD_ICENABLER<n>."
        RegisterWritePending = 1u << 31, // RWP
    };

    static constexpr size_t INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_OFFSET = 0;
    static constexpr u32 INTERRUPT_CONTROLLER_TYPE_IT_LINES_NUMBER_MASK = (1 << 5) - 1;

    static constexpr size_t INTERRUPT_ROUTING_AFF0_OFFSET = 0;
    static constexpr size_t INTERRUPT_ROUTING_AFF1_OFFSET = 8;
    static constexpr size_t INTERRUPT_ROUTING_AFF2_OFFSET = 16;
    static constexpr size_t INTERRUPT_ROUTING_AFF3_OFFSET = 32;

    Control control;                                          // GICD_CTLR
    u32 interrupt_controller_type;                            // GICD_TYPER
    u32 implementer_identification;                           // GICD_IIDR
    u32 interrupt_controller_type_2;                          // GICD_TYPER2
    u32 error_reporting_status;                               // GICD_STATUSR, optional
    u32 _[3];                                                 //
    u32 implementation_defined0[8];                           //
    u32 set_spi_non_secure;                                   // GICD_SETSPI_NSR
    u32 _;                                                    //
    u32 clear_spi_non_secure;                                 // GICD_CLRSPI_NSR
    u32 _;                                                    //
    u32 set_spi_secure;                                       // GICD_SETSPI_SR
    u32 _;                                                    //
    u32 clear_spi_secure;                                     // GICD_CLRSPI_SR
    u32 _[9];                                                 //
    u32 interrupt_group[32];                                  // GICD_IGROUPR<n>
    u32 interrupt_set_enable[32];                             // GICD_ISENABLER<n>
    u32 interrupt_clear_enable[32];                           // GICD_ICENABLER<n>
    u32 interrupt_set_pending[32];                            // GICD_ISPENDR<n>
    u32 interrupt_clear_pending[32];                          // GICD_ICPENDR<n>
    u32 interrupt_set_active[32];                             // GICD_ISACTIVER<n>
    u32 interrupt_clear_active[32];                           // GICD_ICACTIVER<n>
    u32 interrupt_priority[255];                              // GICD_IPRIORITYR<n>
    u32 _;                                                    //
    u32 interrupt_processor_targets[255];                     // GICD_ITARGETSR<n>, legacy
    u32 _;                                                    //
    u32 interrupt_configuration[64];                          // GICD_ICFGR<n>
    u32 interrupt_group_modifier[64];                         // GICD_IGRPMODR<n>
    u32 non_secure_access_control[64];                        // GICD_NSACR<n>
    u32 software_generated_interrupt;                         // GICD_SGIR, legacy
    u32 _[3];                                                 //
    u32 software_generated_interrupt_clear_pending[4];        // GICD_CPENDSGIR<n>
    u32 software_generated_interrupt_set_pending[4];          // GICD_SPENDSGIR<n>
    u32 _[20];                                                //
    u32 non_maskable_interrupt[32];                           // GICD_INMRIR<n>
    u32 interrupt_group_for_extended_spi_range[32];           // GICD_IGROUPR<n>E
    u32 _[96];                                                //
    u32 interrupt_set_enable_for_extended_spi_range[32];      // GICD_ISENABLER<n>E
    u32 _[96];                                                //
    u32 interrupt_clear_enable_for_extended_spi_range[32];    // GICD_ICENABLER<n>E
    u32 _[96];                                                //
    u32 interrupt_set_pending_for_extended_spi_range[32];     // GICD_ISPENDR<n>E
    u32 _[96];                                                //
    u32 interrupt_clear_pending_for_extended_spi_range[32];   // GICD_ICPENDR<n>E
    u32 _[96];                                                //
    u32 interrupt_set_active_for_extended_spi_range[32];      // GICD_ISACTIVER<n>E
    u32 _[96];                                                //
    u32 interrupt_clear_active_for_extended_spi_range[32];    // GICD_ICACTIVER<n>E
    u32 _[224];                                               //
    u32 interrupt_priority_for_extended_spi_range[256];       // GICD_IPRIORITYR<n>E
    u32 _[768];                                               //
    u32 interrupt_configuration_for_extended_spi_range[64];   // GICD_ICFGR<n>E
    u32 _[192];                                               //
    u32 interrupt_group_modifier_for_extended_spi_range[32];  // GICD_IGRPMODR<n>E
    u32 _[96];                                                //
    u32 non_secure_access_control_for_extended_spi_range[64]; // GICD_INSACR<n>E
    u32 _[256];                                               //
    u32 non_maskable_interrupt_for_extended_spi_range[32];    // GICD_INMRIR<n>E
    u32 _[2400];                                              //
    u32 interrupt_routing[1984];                              // GICD_IROUTER<n>
    u32 interrupt_routing_for_extended_spi_range[2048];       // GICD_IROUTER<n>E
    u32 _[2048];                                              //
    u32 implementation_defined1[4084];                        //
    u32 identification[12];                                   //
};
static_assert(AssertSize<GICv3::DistributorRegisters, 0x10000>());
static_assert(offsetof(GICv3::DistributorRegisters, error_reporting_status) == 0x10);
static_assert(offsetof(GICv3::DistributorRegisters, set_spi_non_secure) == 0x40);
static_assert(offsetof(GICv3::DistributorRegisters, clear_spi_secure) == 0x58);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_priority) == 0x400);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_processor_targets) == 0x800);
static_assert(offsetof(GICv3::DistributorRegisters, non_secure_access_control) == 0xe00);
static_assert(offsetof(GICv3::DistributorRegisters, software_generated_interrupt_set_pending) == 0xf20);
static_assert(offsetof(GICv3::DistributorRegisters, non_maskable_interrupt) == 0xf80);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_group_for_extended_spi_range) == 0x1000);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_set_enable_for_extended_spi_range) == 0x1200);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_priority_for_extended_spi_range) == 0x2000);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_configuration_for_extended_spi_range) == 0x3000);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_group_modifier_for_extended_spi_range) == 0x3400);
static_assert(offsetof(GICv3::DistributorRegisters, non_secure_access_control_for_extended_spi_range) == 0x3600);
static_assert(offsetof(GICv3::DistributorRegisters, non_maskable_interrupt_for_extended_spi_range) == 0x3b00);
static_assert(offsetof(GICv3::DistributorRegisters, interrupt_routing) == 0x6100);
static_assert(offsetof(GICv3::DistributorRegisters, identification) == 0xffd0);

AK_ENUM_BITWISE_OPERATORS(GICv3::DistributorRegisters::Control);

// Table 12-27 GIC physical LPI Redistributor register map
struct PhysicalLPIRedistributorRegisters {
    enum class Type : u64 {
        Last = 1u << 4,
    };

    static constexpr size_t TYPE_AFF0_OFFSET = 32;
    static constexpr size_t TYPE_AFF1_OFFSET = 40;
    static constexpr size_t TYPE_AFF2_OFFSET = 48;
    static constexpr size_t TYPE_AFF3_OFFSET = 56;

    enum class Wake : u32 {
        ProcessorSleep = 1u << 1,
        ChildrenAsleep = 1u << 2,
    };

    static constexpr size_t IDENTIFICATION_PERIPHERAL_ID2_REGISTER_INDEX = 6;

    u32 control;                        // GICR_CTLR
    u32 implementer_identification;     // GICR_IIDR
    Type type;                          // GICR_TYPER
    u32 error_reporting_status;         // GICR_STATUSR
    Wake wake;                          // GICR_WAKER
    u32 maximum_partid_and_pmg;         // GICR_MPAMIDR
    u32 set_partid_and_pmg;             // GICR_PARTIDR
    u32 _[8];                           //
    u64 set_lpi_pending;                // GICR_SETLPIR
    u64 clear_lpi_pending;              // GICR_CLRLPRIR
    u64 _[4];                           //
    u64 properties_base_address;        // GICR_PROPBASER
    u64 lpi_pending_table_base_address; // GICR_PENDBASER
    u32 _[8];                           //
    u64 invalidate_lpi;                 // GICR_INVLPIR
    u64 _;                              //
    u64 invalidate_all;                 // GICR_INVALLR
    u64 _;                              //
    u32 synchronize;                    // GICR_SYNCR
    u32 _[15];                          //
    u32 implementation_defined0[2];     //
    u32 _[2];                           //
    u32 implementation_defined1[2];     //
    u32 _[12218];                       //
    u32 implementation_defined2[4084];  //
    u32 identification[12];             //
};
static_assert(offsetof(PhysicalLPIRedistributorRegisters, error_reporting_status) == 0x10);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, set_partid_and_pmg) == 0x1c);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, set_lpi_pending) == 0x40);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, lpi_pending_table_base_address) == 0x78);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, invalidate_lpi) == 0xa0);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, invalidate_all) == 0xb0);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, synchronize) == 0xc0);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, implementation_defined2) == 0xc000);
static_assert(offsetof(PhysicalLPIRedistributorRegisters, identification) == 0xffd0);
static_assert(AssertSize<PhysicalLPIRedistributorRegisters, 64 * KiB>());

AK_ENUM_BITWISE_OPERATORS(PhysicalLPIRedistributorRegisters::Type);
AK_ENUM_BITWISE_OPERATORS(PhysicalLPIRedistributorRegisters::Wake);

// Table 12-29 GIC SGI and PPI Redistributor register map
struct SGIAndPPIRedistributorRegisters {
    u32 _[32];                         //
    u32 interrupt_group[3];            // GICR_IGROUPR0, GICR_IGROUPR<n>E
    u32 _[29];                         //
    u32 interrupt_set_enable[3];       // GICR_ISENABLER0, GICR_ISENABLER<n>E
    u32 _[29];                         //
    u32 interrupt_clear_enable[3];     // GICR_ICENABLER0, GICR_ICENABLER<n>E
    u32 _[29];                         //
    u32 interrupt_set_pending[3];      // GICR_ISPENDR0, GICR_ISPENDR<n>E
    u32 _[29];                         //
    u32 interrupt_clear_pending[3];    // GICR_ICPENDR0, GICR_ICPENDR<n>E
    u32 _[29];                         //
    u32 interrupt_set_active[3];       // GICR_ISACTIVER0, GICR_ISACTIVER<n>E
    u32 _[29];                         //
    u32 interrupt_clear_active[3];     // GICR_ICACTIVER0, GICR_ICACTIVER<n>E
    u32 _[29];                         //
    u32 interrupt_priority[24];        // GICR_IPRIORITYR<n>, GICR_IPRIORITYR<n>E
    u32 _[488];                        //
    u32 interrupt_configuration[6];    // GICR_ICFGR0, GICR_IFCFGR1, GICR_ICFGR<n>E
    u32 _[58];                         //
    u32 interrupt_group_modifier[3];   // GICR_IGRPMODR0, GICR_IGRPMODR<n>E
    u32 _[61];                         //
    u32 non_secure_access_control;     // GICR_NSACR
    u32 _[95];                         //
    u32 non_maskable_interrupt[3];     // GICR_INMIR0 GICR_INMIR<n>E
    u32 _[11293];                      //
    u32 implementation_defined0[4084]; //
    u32 _[12];                         //
};
static_assert(offsetof(SGIAndPPIRedistributorRegisters, interrupt_group) == 0x80);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, interrupt_set_pending) == 0x200);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, interrupt_priority) == 0x400);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, interrupt_configuration) == 0xc00);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, interrupt_group_modifier) == 0xd00);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, non_secure_access_control) == 0xe00);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, non_maskable_interrupt) == 0xf80);
static_assert(offsetof(SGIAndPPIRedistributorRegisters, implementation_defined0) == 0xc000);
static_assert(AssertSize<SGIAndPPIRedistributorRegisters, 64 * KiB>());

// 12.10 The Redistributor register map
struct GICv3::RedistributorRegisters {
    PhysicalLPIRedistributorRegisters physical_lpis_and_overall_behavior; // RD_base
    SGIAndPPIRedistributorRegisters sgis_and_ppis;                        // SGI_base
};
static_assert(offsetof(GICv3::RedistributorRegisters, sgis_and_ppis) == 1uz * 64 * KiB);
static_assert(AssertSize<GICv3::RedistributorRegisters, 2uz * 64 * KiB>());

}
