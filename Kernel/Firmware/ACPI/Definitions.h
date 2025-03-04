/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::ACPI {

namespace FADTFlags {

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#fixed-acpi-description-table-fixed-feature-flags
enum class FeatureFlags : u32 {
    WBINVD = 1 << 0,
    WBINVD_FLUSH = 1 << 1,
    PROC_C1 = 1 << 2,
    P_LVL2_UP = 1 << 3,
    PWR_BUTTON = 1 << 4,
    SLP_BUTTON = 1 << 5,
    FIX_RTC = 1 << 6,
    RTC_s4 = 1 << 7,
    TMR_VAL_EXT = 1 << 8,
    DCK_CAP = 1 << 9,
    RESET_REG_SUPPORTED = 1 << 10,
    SEALED_CASE = 1 << 11,
    HEADLESS = 1 << 12,
    CPU_SW_SLP = 1 << 13,
    PCI_EXP_WAK = 1 << 14,
    USE_PLATFORM_CLOCK = 1 << 15,
    S4_RTC_STS_VALID = 1 << 16,
    REMOTE_POWER_ON_CAPABLE = 1 << 17,
    FORCE_APIC_CLUSTER_MODEL = 1 << 18,
    FORCE_APIC_PHYSICAL_DESTINATION_MODE = 1 << 19,
    HW_REDUCED_ACPI = 1 << 20,
    LOW_POWER_S0_IDLE_CAPABLE = 1 << 21
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#fixed-acpi-description-table-boot-ia-pc-boot-architecture-flags
enum class IA_PC_Flags : u8 {
    Legacy_Devices = 1 << 0,
    PS2_8042 = 1 << 1,
    VGA_Not_Present = 1 << 2,
    MSI_Not_Supported = 1 << 3,
    PCIe_ASPM_Controls = 1 << 4,
    CMOS_RTC_Not_Present = 1 << 5
};

struct [[gnu::packed]] HardwareFeatures {
    bool wbinvd : 1;
    bool wbinvd_flush : 1;
    bool processor_c1 : 1;
    bool multiprocessor_c2 : 1;
    bool power_button : 1;
    bool sleep_button : 1;
    bool fix_rtc : 1;
    bool rtc_s4 : 1;
    bool timer_value_extension : 1;
    bool docking_capability : 1;
    bool reset_register_supported : 1;
    bool sealed_case : 1;
    bool headless : 1;
    bool cpu_software_sleep : 1;
    bool pci_express_wake : 1;
    bool use_platform_clock : 1;
    bool s4_rtc_status_valid : 1;
    bool remote_power_on_capable : 1;
    bool force_apic_cluster_model : 1;
    bool force_apic_physical_destination_mode : 1;
    bool hardware_reduced_acpi : 1;
    bool low_power_s0_idle_capable : 1;
};
struct [[gnu::packed]] x86_Specific_Flags {
    bool legacy_devices : 1;
    bool keyboard_8042 : 1;
    bool vga_not_present : 1;
    bool msi_not_supported : 1;
    bool cmos_rtc_not_present : 1;
};
};

namespace GenericAddressStructure {
enum class AddressSpace {
    SystemMemory = 0,
    SystemIO = 1,
    PCIConfigurationSpace = 2,
    EmbeddedController = 3,
    SMBus = 4,
    PCC = 0xA,
    FunctionalFixedHardware = 0x7F
};
enum class AccessSize {
    Undefined = 0,
    Byte = 1,
    Word = 2,
    DWord = 3,
    QWord = 4
};
enum class BitWidth {
    Undefined = 0,
    Byte = 8,
    Word = 16,
    DWord = 32,
    QWord = 64
};
}

namespace Structures {

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#root-system-description-pointer-rsdp-structure
struct [[gnu::packed]] RSDPDescriptor {
    char sig[8];
    u8 checksum;
    char oem_id[6];
    u8 revision;
    u32 rsdt_ptr;
};

struct [[gnu::packed]] RSDPDescriptor20 {
    RSDPDescriptor base;
    u32 length;
    u64 xsdt_ptr;
    u8 ext_checksum;
    u8 reserved[3];
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#system-description-table-header
struct [[gnu::packed]] SDTHeader {
    char sig[4];
    u32 length;
    u8 revision;
    u8 checksum;
    char oem_id[6];
    char oem_table_id[8];
    u32 oem_revision;
    u32 creator_id;
    u32 creator_revision;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#root-system-description-table-rsdt
struct [[gnu::packed]] RSDT {
    SDTHeader h;
    u32 table_ptrs[];
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#extended-system-description-table-xsdt
struct [[gnu::packed]] XSDT {
    SDTHeader h;
    u64 table_ptrs[];
};

struct [[gnu::packed]] GenericAddressStructure {
    u8 address_space;
    u8 bit_width;
    u8 bit_offset;
    u8 access_size;
    u64 address;
};

struct [[gnu::packed]] HPET {
    SDTHeader h;
    u8 hardware_revision_id;
    u8 attributes;
    u16 pci_vendor_id;
    GenericAddressStructure event_timer_block;
    u8 hpet_number;
    u16 mininum_clock_tick;
    u8 page_protection;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#fixed-acpi-description-table-fadt
struct [[gnu::packed]] FADT {
    SDTHeader h;
    u32 firmware_ctrl;
    u32 dsdt_ptr;
    u8 reserved;
    u8 preferred_pm_profile;
    u16 sci_int;
    u32 smi_cmd;
    u8 acpi_enable_value;
    u8 acpi_disable_value;
    u8 s4bios_req;
    u8 pstate_cnt;
    u32 PM1a_EVT_BLK;
    u32 PM1b_EVT_BLK;
    u32 PM1a_CNT_BLK;
    u32 PM1b_CNT_BLK;
    u32 PM2_CNT_BLK;
    u32 PM_TMR_BLK;
    u32 GPE0_BLK;
    u32 GPE1_BLK;
    u8 PM1_EVT_LEN;
    u8 PM1_CNT_LEN;
    u8 PM2_CNT_LEN;
    u8 PM_TMR_LEN;
    u8 GPE0_BLK_LEN;
    u8 GPE1_BLK_LEN;
    u8 GPE1_BASE;
    u8 cst_cnt;
    u16 P_LVL2_LAT;
    u16 P_LVL3_LAT;
    u16 flush_size;
    u16 flush_stride;
    u8 duty_offset;
    u8 duty_width;
    u8 day_alrm;
    u8 mon_alrm;
    u8 century;
    u16 ia_pc_boot_arch_flags;
    u8 reserved2;
    u32 flags;
    GenericAddressStructure reset_reg;
    u8 reset_value;
    u16 arm_boot_arch;
    u8 fadt_minor_version;
    u64 x_firmware_ctrl;
    u64 x_dsdt;
    GenericAddressStructure x_pm1a_evt_blk;
    GenericAddressStructure x_pm1b_evt_blk;
    GenericAddressStructure x_pm1a_cnt_blk;
    GenericAddressStructure x_pm1b_cnt_blk;
    GenericAddressStructure x_pm2_cnt_blk;
    GenericAddressStructure x_pm_tmr_blk;
    GenericAddressStructure x_gpe0_blk;
    GenericAddressStructure x_gpe1_blk;
    GenericAddressStructure sleep_control;
    GenericAddressStructure sleep_status;
    u64 hypervisor_vendor_identity;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#interrupt-controller-structure-types
enum class MADTEntryType {
    LocalAPIC = 0x0,
    IOAPIC = 0x1,
    InterruptSourceOverride = 0x2,
    NMI_Source = 0x3,
    LocalAPIC_NMI = 0x4,
    LocalAPIC_Address_Override = 0x5,
    IO_SAPIC = 0x6,
    Local_SAPIC = 0x7,
    Platform_interrupt_Sources = 0x8,
    Local_x2APIC = 0x9,
    Local_x2APIC_NMI = 0xA,
    GIC_CPU = 0xB,
    GIC_Distributor = 0xC,
    GIC_MSI = 0xD,
    GIC_Redistrbutor = 0xE,
    GIC_Interrupt_Translation = 0xF
};

struct [[gnu::packed]] MADTEntryHeader {
    u8 type;
    u8 length;
};

namespace MADTEntries {

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#i-o-apic-structure
struct [[gnu::packed]] IOAPIC {
    MADTEntryHeader h;
    u8 ioapic_id;
    u8 reserved;
    u32 ioapic_address;
    u32 gsi_base;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#processor-local-apic-structure
struct [[gnu::packed]] ProcessorLocalAPIC {
    MADTEntryHeader h;
    u8 acpi_processor_id;
    u8 apic_id;
    u32 flags;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#processor-local-x2apic-structure
struct [[gnu::packed]] ProcessorLocalX2APIC {
    MADTEntryHeader h;
    u16 reserved;
    u32 apic_id;
    u32 flags;
    u32 acpi_processor_id;
};

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#interrupt-source-override-structure
struct [[gnu::packed]] InterruptSourceOverride {
    MADTEntryHeader h;
    u8 bus;
    u8 source;
    u32 global_system_interrupt;
    u16 flags;
};
}

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#multiple-apic-description-table-madt-format
struct [[gnu::packed]] MADT {
    SDTHeader h;
    u32 lapic_address;
    u32 flags;
    MADTEntryHeader entries[];
};

struct [[gnu::packed]] AMLTable {
    SDTHeader h;
    char aml_code[];
};

struct [[gnu::packed]] PCI_MMIO_Descriptor {
    u64 base_addr;
    u16 seg_group_number;
    u8 start_pci_bus;
    u8 end_pci_bus;
    u32 reserved;
};

struct [[gnu::packed]] MCFG {
    SDTHeader header;
    u64 reserved;
    PCI_MMIO_Descriptor descriptors[];
};

struct [[gnu::packed]] DSDT {
    SDTHeader h;
    unsigned char definition_block[];
};
}

}
