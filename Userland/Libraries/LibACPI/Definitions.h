/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace ACPI {

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

AK_ENUM_BITWISE_OPERATORS(FeatureFlags);

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#fixed-acpi-description-table-boot-ia-pc-boot-architecture-flags
enum class IA_PC_Flags : u16 {
    Legacy_Devices = 1 << 0,
    PS2_8042 = 1 << 1,
    VGA_Not_Present = 1 << 2,
    MSI_Not_Supported = 1 << 3,
    PCIe_ASPM_Controls = 1 << 4,
    CMOS_RTC_Not_Present = 1 << 5
};

AK_ENUM_BITWISE_OPERATORS(IA_PC_Flags);

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

enum class AddressSpace : u8 {
    SystemMemory = 0,
    SystemIO = 1,
    PCIConfigurationSpace = 2,
    EmbeddedController = 3,
    SMBus = 4,
    PCC = 0xA,
    FunctionalFixedHardware = 0x7F
};

enum class AccessSize : u8 {
    Undefined = 0,
    Byte = 1,
    Word = 2,
    DWord = 3,
    QWord = 4
};

enum class BitWidth : u8 {
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
    ::ACPI::GenericAddressStructure::AddressSpace address_space;
    ::ACPI::GenericAddressStructure::BitWidth bit_width;
    u8 bit_offset;
    ::ACPI::GenericAddressStructure::AccessSize access_size;
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
    FADTFlags::IA_PC_Flags ia_pc_boot_arch_flags;
    u8 reserved2;
    FADTFlags::FeatureFlags flags;
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
enum class MADTEntryType : u8 {
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
    MADTEntryType type;
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

namespace AML {

enum class Prefix : u8 {
    ExtOpPrefix = 0x5B,
    DualNamePrefix = 0x2E,
    MultiNamePrefix = 0x2F,
};

enum class Opcode : u16 {
    ZeroOp = 0x0000,
    OneOp = 0x0001,
    OnesOp = 0x00FF,

    BytePrefix = 0x000A,
    WordPrefix = 0x000B,
    DWordPrefix = 0x000C,
    QWordPrefix = 0x000E,
    StringPrefix = 0x000D,

    RevisionOp = 0x5B30,
    DebugOp = 0x5B31,

    AliasOp = 0x0006,
    NameOp = 0x0008,
    ScopeOp = 0x0010,
    BufferOp = 0x0011,
    PackageOp = 0x0012,
    VarPackageOp = 0x0013,
    MethodOp = 0x0014,
    ExternalOp = 0x0015,
    MutexOp = 0x5B01,
    EventOp = 0x5B02,
    TimerOp = 0x5B33,
    OpRegionOp = 0x5B80,
    FieldOp = 0x5B81,
    DeviceOp = 0x5B82,
    // NOTE: This opcode is missing in the latest spec but is still being used.
    ProcessorOp = 0x5B83,
    PowerResOp = 0x5B84,
    ThermalZoneOp = 0x5B85,
    IndexFieldOp = 0x5B86,
    BankFieldOp = 0x5B87,
    DataRegionOp = 0x5B88,

    CondRefOfOp = 0x5B12,
    CreateFieldOp = 0x5B13,
    LoadTableOp = 0x5B1F,
    LoadOp = 0x5B20,
    StallOp = 0x5B21,
    SleepOp = 0x5B22,
    AcquireOp = 0x5B23,
    SignalOp = 0x5B24,
    WaitOp = 0x5B25,
    ResetOp = 0x5B26,
    ReleaseOp = 0x5B27,
    FromBCDOp = 0x5B28,
    ToBCDOp = 0x5B29,
    FatalOp = 0x5B32,
    StoreOp = 0x70,
    RefOfOp = 0x71,

    AddOp = 0x72,
    ConcatOp = 0x73,
    SubtractOp = 0x74,
    IncrementOp = 0x75,
    DecrementOp = 0x76,
    MultiplyOp = 0x77,
    DivideOp = 0x78,
    ShiftLeftOp = 0x79,
    ShiftRightOp = 0x7A,
    AndOp = 0x7B,
    NandOp = 0x7C,
    OrOp = 0x7D,
    NorOp = 0x7E,
    XorOp = 0x7F,

    NotOp = 0x80,
    FindSetLeftBitOp = 0x81,
    FindSetRightBitOp = 0x82,

    DerefOfOp = 0x83,
    ConcatResOp = 0x84,
    ModOp = 0x85,
    NotifyOp = 0x86,
    SizeOfOp = 0x87,
    IndexOp = 0x88,
    MatchOp = 0x89,
    ObjectTypeOp = 0x8E,

    LAndOp = 0x90,
    LOrOp = 0x91,
    LNotOp = 0x92,
    LEqualOp = 0x93,
    LGreaterOp = 0x94,
    LLessOp = 0x95,

    ToBufferOp = 0x96,
    ToDecimalStringOp = 0x97,
    ToHexStringOp = 0x98,
    ToIntegerOp = 0x99,
    ToStringOp = 0x9C,

    CreateBitFieldOp = 0x8D,
    CreateByteFieldOp = 0x8C,
    CreateWordFieldOp = 0x8B,
    CreateDWordFieldOp = 0x8A,
    CreateQWordFieldOp = 0x8F,

    CopyObjectOp = 0x9D,
    MidOp = 0x9E,
    ContinueOp = 0x9F,
    IfOp = 0xA0,
    ElseOp = 0xA1,
    WhileOp = 0xA2,
    NoopOp = 0xA3,
    ReturnOp = 0xA4,
    BreakOp = 0xA5,
    BreakPointOp = 0xCC,

    Local0Op = 0x60,
    Local1Op = 0x61,
    Local2Op = 0x62,
    Local3Op = 0x63,
    Local4Op = 0x64,
    Local5Op = 0x65,
    Local6Op = 0x66,
    Local7Op = 0x67,
    Arg0Op = 0x68,
    Arg1Op = 0x69,
    Arg2Op = 0x6A,
    Arg3Op = 0x6B,
    Arg4Op = 0x6C,
    Arg5Op = 0x6D,
    Arg6Op = 0x6E,
};

}

}

template<>
class AK::Traits<::ACPI::Structures::SDTHeader> : public DefaultTraits<::ACPI::Structures::SDTHeader> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Formatter<::ACPI::AML::Opcode> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, ::ACPI::AML::Opcode opcode)
    {
        return AK::Formatter<FormatString>::format(builder, "{:04X}"sv, static_cast<u16>(opcode));
    }
};
