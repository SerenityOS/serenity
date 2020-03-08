/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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
#include <LibBareMetal/Memory/PhysicalAddress.h>

namespace Kernel {

namespace ACPI {

    enum class FADTFeatureFlags : u32 {
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
    }

    namespace Structures {
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

        namespace MADTEntries {
            struct [[gnu::packed]] IOAPIC
            {
                MADTEntryHeader h;
                u8 ioapic_id;
                u8 reserved;
                u32 ioapic_address;
                u32 gsi_base;
            };

            struct [[gnu::packed]] InterruptSourceOverride
            {
                MADTEntryHeader h;
                u8 bus;
                u8 source;
                u32 global_system_interrupt;
                u16 flags;
            };
        }

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

    class StaticParser;
    class DynamicParser;
    class Parser;

    namespace StaticParsing {
        PhysicalAddress search_rsdp_in_ebda(u16 ebda_segment);
        PhysicalAddress search_rsdp_in_bios_area();
        PhysicalAddress search_rsdp();
        bool match_table_signature(PhysicalAddress table_header, const char*);
        PhysicalAddress search_table(PhysicalAddress rsdp, const char*);
        PhysicalAddress search_table_in_xsdt(PhysicalAddress xsdt, const char*);
        PhysicalAddress search_table_in_rsdt(PhysicalAddress rsdt, const char*);
        inline bool validate_table(Structures::SDTHeader&, size_t length);
    };
}
}
