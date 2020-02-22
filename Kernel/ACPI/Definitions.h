/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Kernel {

namespace ACPI_RAW {

    struct [[gnu::packed]] RSDPDescriptor
    {
        char sig[8];
        u8 checksum;
        char oem_id[6];
        u8 revision;
        u32 rsdt_ptr;
    };

    struct [[gnu::packed]] RSDPDescriptor20
    {
        RSDPDescriptor base;
        u32 length;
        u64 xsdt_ptr;
        u8 ext_checksum;
        u8 reserved[3];
    };

    struct [[gnu::packed]] SDTHeader
    {
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

    struct [[gnu::packed]] RSDT
    {
        SDTHeader h;
        u32 table_ptrs[];
    };

    struct [[gnu::packed]] XSDT
    {
        SDTHeader h;
        u64 table_ptrs[];
    };

    struct [[gnu::packed]] GenericAddressStructure
    {
        u8 address_space;
        u8 bit_width;
        u8 bit_offset;
        u8 access_size;
        u64 address;
    };

    struct [[gnu::packed]] TimerStructure
    {
        u64 configuration_capability;
        u64 comparator_value;
        u64 fsb_interrupt_route;
    };

    struct [[gnu::packed]] HPET
    {
        SDTHeader h;
        u64 capabilities;
        u64 reserved;
        u64 configuration;
        u64 reserved2;
        u64 interrupt_status;
        u64 reserved3;
        u64 main_counter_value;
        u64 reserved4;
        TimerStructure timer0;
        u64 reserved5;
        TimerStructure timer1;
        u64 reserved6;
        TimerStructure timer2;
        u64 reserved7;
    };

    struct [[gnu::packed]] FADT
    {
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

    struct [[gnu::packed]] MADTEntryHeader
    {
        u8 type;
        u8 length;
    };

    struct [[gnu::packed]] MADT_IOAPIC
    {
        MADTEntryHeader h;
        u8 ioapic_id;
        u8 reserved;
        u32 ioapic_address;
        u32 gsi_base;
    };

    struct [[gnu::packed]] MADT_InterruptSourceOverride
    {
        MADTEntryHeader h;
        u8 bus;
        u8 source;
        u32 global_system_interrupt;
        u16 flags;
    };

    struct [[gnu::packed]] MADT
    {
        SDTHeader h;
        u32 lapic_address;
        u32 flags;
        MADTEntryHeader entries[];
    };

    struct [[gnu::packed]] AMLTable
    {
        SDTHeader h;
        char aml_code[];
    };

    struct [[gnu::packed]] PCI_MMIO_Descriptor
    {
        u64 base_addr;
        u16 seg_group_number;
        u8 start_pci_bus;
        u8 end_pci_bus;
        u32 reserved;
    };

    struct [[gnu::packed]] MCFG
    {
        SDTHeader header;
        u64 reserved;
        PCI_MMIO_Descriptor descriptors[];
    };
}

class ACPIStaticParser;

namespace ACPI {

    class SDT : public RefCounted<SDT> {
    };

    struct GenericAddressStructure {
        u8 address_space;
        u8 bit_width;
        u8 bit_offset;
        u8 access_size;
        u64 address;
        GenericAddressStructure& operator=(const GenericAddressStructure& other)
        {
            this->address_space = other.address_space;
            this->bit_width = other.bit_width;
            this->bit_offset = other.bit_offset;
            this->access_size = other.access_size;
            this->address = (uintptr_t)other.address;
            return *this;
        }
        GenericAddressStructure& operator=(const ACPI_RAW::GenericAddressStructure& other)
        {
            this->address_space = other.address_space;
            this->bit_width = other.bit_width;
            this->bit_offset = other.bit_offset;
            this->access_size = other.access_size;
            this->address = (uintptr_t)other.address;
            return *this;
        }
    };

    class FixedACPIData;
}

class ACPI::FixedACPIData : public ACPI::SDT {
    friend ACPIStaticParser;

public:
    explicit FixedACPIData(ACPI_RAW::FADT&);
    ACPI_RAW::SDTHeader* get_dsdt();

private:
    u8 m_revision;
    u32 m_dsdt_ptr;
    u64 m_x_dsdt_ptr;
    u8 m_preferred_pm_profile;
    u16 m_sci_int;
    u32 m_smi_cmd;
    u8 m_acpi_enable_value;
    u8 m_acpi_disable_value;
    u8 m_s4bios_req;
    u8 m_pstate_cnt;
    u32 m_PM1a_EVT_BLK;
    u32 m_PM1b_EVT_BLK;
    u32 m_PM1a_CNT_BLK;
    u32 m_PM1b_CNT_BLK;
    u32 m_PM2_CNT_BLK;
    u32 m_PM_TMR_BLK;
    u32 m_GPE0_BLK;
    u32 m_GPE1_BLK;
    u8 m_PM1_EVT_LEN;
    u8 m_PM1_CNT_LEN;
    u8 m_PM2_CNT_LEN;
    u8 m_PM_TMR_LEN;
    u8 m_GPE0_BLK_LEN;
    u8 m_GPE1_BLK_LEN;
    u8 m_GPE1_BASE;
    u8 m_cst_cnt;
    u16 m_P_LVL2_LAT;
    u16 m_P_LVL3_LAT;
    u16 m_flush_size;
    u16 m_flush_stride;
    u8 m_duty_offset;
    u8 m_duty_width;
    u8 m_day_alrm;
    u8 m_mon_alrm;
    u8 m_century;
    u16 m_ia_pc_boot_arch_flags;
    u32 m_flags;
    ACPI::GenericAddressStructure m_reset_reg;
    u8 m_reset_value;
    ACPI::GenericAddressStructure m_x_pm1a_evt_blk;
    ACPI::GenericAddressStructure m_x_pm1b_evt_blk;
    ACPI::GenericAddressStructure m_x_pm1a_cnt_blk;
    ACPI::GenericAddressStructure m_x_pm1b_cnt_blk;
    ACPI::GenericAddressStructure m_x_pm2_cnt_blk;
    ACPI::GenericAddressStructure m_x_pm_tmr_blk;
    ACPI::GenericAddressStructure m_x_gpe0_blk;
    ACPI::GenericAddressStructure m_x_gpe1_blk;
    ACPI::GenericAddressStructure m_sleep_control;
    ACPI::GenericAddressStructure m_sleep_status;
    u64 m_hypervisor_vendor_identity;
};

namespace ACPI {

    class MainSystemDescriptionTable : public SDT {
    public:
        explicit MainSystemDescriptionTable(Vector<ACPI_RAW::SDTHeader*>&& sdt_pointers);
        Vector<ACPI_RAW::SDTHeader*>& get_sdt_pointers();

    private:
        Vector<ACPI_RAW::SDTHeader*> m_sdt_pointers;
    };

    class MCFG : public SDT {
    public:
        MCFG(ACPI_RAW::MCFG&);
    };

    class FACS : public SDT {

    public:
    private:
        u32 hardware_sig;
        u32 waking_vector;
        u32 global_lock;
        u32 flags;
        u64 x_waking_vector;
        u32 ospm_flags;
    };

    class MADT : public SDT {
    };
}

}
