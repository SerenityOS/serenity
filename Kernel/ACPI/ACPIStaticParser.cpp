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

#include <Kernel/ACPI/ACPIStaticParser.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>
#include <LibBareMetal/StdLib.h>

//#define ACPI_DEBUG

namespace Kernel {

void ACPIStaticParser::initialize(ACPI_RAW::RSDPDescriptor20& rsdp)
{
    if (!ACPIParser::is_initialized()) {
        new ACPIStaticParser(rsdp);
    }
}
void ACPIStaticParser::initialize_without_rsdp()
{
    if (!ACPIParser::is_initialized()) {
        new ACPIStaticParser();
    }
}

bool ACPIStaticParser::is_initialized()
{
    return ACPIParser::is_initialized();
}

void ACPIStaticParser::locate_static_data()
{
    locate_main_system_description_table();
    initialize_main_system_description_table();
    init_fadt();
    locate_all_aml_tables();
}

ACPI_RAW::SDTHeader* ACPIStaticParser::find_table(const char* sig)
{
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Calling Find Table method!\n");
#endif
    for (auto* physical_sdt_ptr : m_main_sdt->get_sdt_pointers()) {
        auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(physical_sdt_ptr)), (PAGE_SIZE * 2), "ACPI Static Parser Tables Finding", Region::Access::Read);
        auto* sdt = (const ACPI_RAW::SDTHeader*)region->vaddr().offset(offset_in_page(physical_sdt_ptr)).as_ptr();
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: Examining Table @ P 0x%x\n", physical_sdt_ptr);
#endif
        if (!strncmp(sdt->sig, sig, 4)) {
#ifdef ACPI_DEBUG
            dbgprintf("ACPI: Found Table @ P 0x%x\n", physical_sdt_ptr);
#endif
            return physical_sdt_ptr;
        }
    }
    return nullptr;
}

void ACPIStaticParser::init_fadt()
{
    kprintf("ACPI: Initializing Fixed ACPI data\n");
    kprintf("ACPI: Searching for the Fixed ACPI Data Table\n");
    ASSERT(find_table("FACP") != nullptr);
    auto* fadt_ptr = find_table("FACP");

    auto checkup_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((fadt_ptr))), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Checking FADT Length to choose the correct mapping size\n");
#endif

    auto* sdt = (const ACPI_RAW::SDTHeader*)checkup_region->vaddr().offset(offset_in_page((fadt_ptr))).as_ptr();
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: FADT @ V 0x%x, P 0x%x\n", sdt, fadt_ptr);
#endif
    u32 length = sdt->length;
    kprintf("ACPI: Fixed ACPI data, Revision %u\n", sdt->revision);

    auto fadt_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((fadt_ptr))), PAGE_ROUND_UP(length) + PAGE_SIZE, "ACPI Static Parser", Region::Access::Read);
    m_fadt = make<ACPI::FixedACPIData>(*(ACPI_RAW::FADT*)fadt_region->vaddr().offset(offset_in_page((fadt_ptr))).as_ptr());
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Finished to initialize Fixed ACPI data\n");
#endif
}

void ACPIStaticParser::do_acpi_reboot()
{
    // FIXME: Determine if we need to do MMIO/PCI/IO access to reboot, according to ACPI spec 6.2, Section 4.8.3.6
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Rebooting, Probing FADT (P @ 0x%x)\n", m_fadt.ptr());
#endif
    if (m_fadt->m_revision >= 2) {
        kprintf("ACPI: Reboot, Sending value 0%x to Port 0x%x\n", m_fadt->m_reset_value, m_fadt->m_reset_reg.address);
        IO::out8(m_fadt->m_reset_reg.address, m_fadt->m_reset_value);
    } else {
        kprintf("ACPI: Reboot, Not supported!\n");
    }

    ASSERT_NOT_REACHED(); /// If rebooting didn't work, halt.
}

void ACPIStaticParser::do_acpi_shutdown()
{
    kprintf("ACPI: Shutdown is not supported with the current configuration, Abort!\n");
    ASSERT_NOT_REACHED();
}

inline bool validate_acpi_table(ACPI_RAW::SDTHeader& v_header, size_t length)
{
    u8 checksum = 0;
    auto* sdt = (u8*)&v_header;
    for (size_t i = 0; i < length; i++)
        checksum += sdt[i];
    if (checksum == 0)
        return true;
    return false;
}

size_t ACPIStaticParser::get_table_size(ACPI_RAW::SDTHeader& p_header)
{
    InterruptDisabler disabler;
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Checking SDT Length\n");
#endif
    auto region = MM.allocate_kernel_region(PhysicalAddress((uintptr_t)&p_header).page_base(), (PAGE_SIZE * 2), "ACPI get_table_size()", Region::Access::Read);
    auto* sdt = (volatile ACPI_RAW::SDTHeader*)region->vaddr().offset(offset_in_page(&p_header)).as_ptr();
    return sdt->length;
}

u8 ACPIStaticParser::get_table_revision(ACPI_RAW::SDTHeader& p_header)
{
    InterruptDisabler disabler;
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Checking SDT Revision\n");
#endif
    auto region = MM.allocate_kernel_region(PhysicalAddress((uintptr_t)&p_header).page_base(), (PAGE_SIZE * 2), "ACPI get_table_revision()", Region::Access::Read);
    auto* sdt = (volatile ACPI_RAW::SDTHeader*)region->vaddr().offset(offset_in_page(&p_header)).as_ptr();
    return sdt->revision;
}

void ACPIStaticParser::initialize_main_system_description_table()
{
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Checking Main SDT Length to choose the correct mapping size\n");
#endif
    ASSERT(m_main_system_description_table != nullptr);
    u32 length;
    u8 revision;
    if (m_xsdt_supported) {
        length = get_table_size(*m_main_system_description_table);
        revision = get_table_revision(*m_main_system_description_table);
    } else {
        length = get_table_size(*m_main_system_description_table);
        revision = get_table_revision(*m_main_system_description_table);
    }

    auto main_sdt_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(m_main_system_description_table)), PAGE_ROUND_UP(length) + PAGE_SIZE, "ACPI Static Parser Initialization", Region::Access::Read, false, true);
    auto* sdt = (volatile ACPI_RAW::SDTHeader*)main_sdt_region->vaddr().offset(offset_in_page(m_main_system_description_table)).as_ptr();
    kprintf("ACPI: Main Description Table valid? 0x%x\n", validate_acpi_table(const_cast<ACPI_RAW::SDTHeader&>(*sdt), length));

    Vector<ACPI_RAW::SDTHeader*> sdt_pointers;
    if (m_xsdt_supported) {
        volatile auto* xsdt = (volatile ACPI_RAW::XSDT*)sdt;
        kprintf("ACPI: Using XSDT, Enumerating tables @ P 0x%x\n", m_main_system_description_table);
        kprintf("ACPI: XSDT Revision %d, Total length - %u\n", revision, length);
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: XSDT pointer @ V 0x%x\n", xsdt);
#endif
        for (u32 i = 0; i < ((length - sizeof(ACPI_RAW::SDTHeader)) / sizeof(u64)); i++) {
#ifdef ACPI_DEBUG
            dbgprintf("ACPI: Found new table [%u], @ V0x%x - P0x%x\n", i, &xsdt->table_ptrs[i], xsdt->table_ptrs[i]);
#endif
            sdt_pointers.append((ACPI_RAW::SDTHeader*)xsdt->table_ptrs[i]);
        }
    } else {
        volatile auto* rsdt = (volatile ACPI_RAW::RSDT*)sdt;
        kprintf("ACPI: Using RSDT, Enumerating tables @ P 0x%x\n", m_main_system_description_table);
        kprintf("ACPI: RSDT Revision %d, Total length - %u\n", revision, length);
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: RSDT pointer @ V 0x%x\n", rsdt);
#endif
        for (u32 i = 0; i < ((length - sizeof(ACPI_RAW::SDTHeader)) / sizeof(u32)); i++) {
#ifdef ACPI_DEBUG
            dbgprintf("ACPI: Found new table [%u], @ V0x%x - P0x%x\n", i, &rsdt->table_ptrs[i], rsdt->table_ptrs[i]);
#endif
            sdt_pointers.append((ACPI_RAW::SDTHeader*)rsdt->table_ptrs[i]);
        }
    }
    m_main_sdt = OwnPtr<ACPI::MainSystemDescriptionTable>(new ACPI::MainSystemDescriptionTable(move(sdt_pointers)));
}

void ACPIStaticParser::locate_main_system_description_table()
{
    auto rsdp_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)m_rsdp)), (PAGE_SIZE * 2), "ACPI Static Parser Initialization", Region::Access::Read, false, true);
    volatile auto* rsdp = (ACPI_RAW::RSDPDescriptor20*)rsdp_region->vaddr().offset(offset_in_page((u32)m_rsdp)).as_ptr();
    if (rsdp->base.revision == 0) {
        m_xsdt_supported = false;
    } else if (rsdp->base.revision >= 2) {
        if (rsdp->xsdt_ptr != (u64) nullptr) {
            m_xsdt_supported = true;
        } else {
            m_xsdt_supported = false;
        }
    }
    if (!m_xsdt_supported) {
        m_main_system_description_table = (ACPI_RAW::SDTHeader*)rsdp->base.rsdt_ptr;
    } else {
        m_main_system_description_table = (ACPI_RAW::SDTHeader*)rsdp->xsdt_ptr;
    }
}

void ACPIStaticParser::locate_all_aml_tables()
{
    // Note: According to the ACPI spec, DSDT pointer may be found in the FADT table.
    // All other continuation of the DSDT can be found as pointers in the RSDT/XSDT.

    kprintf("ACPI: Searching for AML Tables\n");
    m_aml_tables_ptrs.append(m_fadt->get_dsdt());
    for (auto* sdt_ptr : m_main_sdt->get_sdt_pointers()) {
        auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(sdt_ptr)), (PAGE_SIZE * 2), "ACPI Static Parser AML Tables Finding", Region::Access::Read);
        auto* sdt = (ACPI_RAW::SDTHeader*)region->vaddr().offset(offset_in_page(sdt_ptr)).as_ptr();
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: Examining Table @ P 0x%x\n", sdt_ptr);
#endif
        if (!strncmp(sdt->sig, "SSDT", 4)) {
            kprintf("ACPI: Found AML Table @ P 0x%x, registering\n", sdt_ptr);
            m_aml_tables_ptrs.append(sdt);
        }
    }
}

ACPIStaticParser::ACPIStaticParser()
    : ACPIParser(true)
    , m_rsdp(nullptr)
    , m_main_sdt(nullptr)
    , m_fadt(nullptr)
{
    m_rsdp = search_rsdp();
    if (m_rsdp != nullptr) {
        kprintf("ACPI: Using RSDP @ P 0x%x\n", m_rsdp);
        m_operable = true;
        locate_static_data();
    } else {
        m_operable = false;
        kprintf("ACPI: Disabled, due to RSDP being absent\n");
    }
}

ACPI_RAW::RSDPDescriptor20* ACPIStaticParser::search_rsdp_in_ebda(u16 ebda_segment)
{
    auto rsdp_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)(ebda_segment << 4))), PAGE_ROUND_UP(1024), "ACPI Static Parser RSDP Finding #1", Region::Access::Read, false, true);
    char* p_rsdp_str = (char*)(PhysicalAddress(ebda_segment << 4).as_ptr());
    for (char* rsdp_str = (char*)rsdp_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).as_ptr(); rsdp_str < (char*)(rsdp_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).get() + 1024); rsdp_str += 16) {
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: Looking for RSDP in EBDA @ V0x%x, P0x%x\n", rsdp_str, p_rsdp_str);
#endif
        if (!strncmp("RSD PTR ", rsdp_str, strlen("RSD PTR ")))
            return (ACPI_RAW::RSDPDescriptor20*)p_rsdp_str;
        p_rsdp_str += 16;
    }
    return nullptr;
}

ACPI_RAW::RSDPDescriptor20* ACPIStaticParser::search_rsdp_in_bios_area()
{
    auto rsdp_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)0xE0000)), PAGE_ROUND_UP(0xFFFFF - 0xE0000), "ACPI Static Parser RSDP Finding #2", Region::Access::Read, false, true);
    char* p_rsdp_str = (char*)(PhysicalAddress(0xE0000).as_ptr());
    for (char* rsdp_str = (char*)rsdp_region->vaddr().offset(offset_in_page((u32)(0xE0000))).as_ptr(); rsdp_str < (char*)(rsdp_region->vaddr().offset(offset_in_page((u32)(0xE0000))).get() + (0xFFFFF - 0xE0000)); rsdp_str += 16) {
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: Looking for RSDP in EBDA @ V0x%x, P0x%x\n", rsdp_str, p_rsdp_str);
#endif
        if (!strncmp("RSD PTR ", rsdp_str, strlen("RSD PTR ")))
            return (ACPI_RAW::RSDPDescriptor20*)p_rsdp_str;
        p_rsdp_str += 16;
    }
    return nullptr;
}

ACPI_RAW::RSDPDescriptor20* ACPIStaticParser::search_rsdp()
{
    ACPI_RAW::RSDPDescriptor20* rsdp = nullptr;
    auto region = MM.allocate_kernel_region(PhysicalAddress(0), PAGE_SIZE, "ACPI Static Parser RSDP Finding", Region::Access::Read);
    u16 ebda_seg = (u16) * ((uint16_t*)((region->vaddr().get() & PAGE_MASK) + 0x40e));
    kprintf("ACPI: Probing EBDA, Segment 0x%x\n", ebda_seg);

    rsdp = search_rsdp_in_ebda(ebda_seg);
    if (rsdp != nullptr)
        return rsdp;
    return search_rsdp_in_bios_area();
}

ACPIStaticParser::ACPIStaticParser(ACPI_RAW::RSDPDescriptor20& rsdp)
    : ACPIParser(true)
    , m_rsdp(&rsdp)
    , m_main_sdt(nullptr)
    , m_fadt(nullptr)
{
    kprintf("ACPI: Using RSDP @ Px%x\n", &rsdp);
    m_operable = true;
    locate_static_data();
}

ACPI::MainSystemDescriptionTable::MainSystemDescriptionTable(Vector<ACPI_RAW::SDTHeader*>&& sdt_pointers)
{
    for (auto* sdt_ptr : sdt_pointers) {
#ifdef ACPI_DEBUG
        dbgprintf("ACPI: Register new table in Main SDT, @ P 0x%x\n", sdt_ptr);
#endif
        m_sdt_pointers.append(sdt_ptr);
    }
}
Vector<ACPI_RAW::SDTHeader*>& ACPI::MainSystemDescriptionTable::get_sdt_pointers()
{
    return m_sdt_pointers;
}

ACPI::FixedACPIData::FixedACPIData(ACPI_RAW::FADT& fadt)
{
    m_dsdt_ptr = fadt.dsdt_ptr;
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: DSDT pointer @ P 0x%x\n", m_dsdt_ptr);
#endif
    m_revision = fadt.h.revision;
    m_x_dsdt_ptr = fadt.x_dsdt;
    m_preferred_pm_profile = fadt.preferred_pm_profile;
    m_sci_int = fadt.sci_int;
    m_smi_cmd = fadt.smi_cmd;
    m_acpi_enable_value = fadt.acpi_enable_value;
    m_acpi_disable_value = fadt.acpi_disable_value;
    m_s4bios_req = fadt.s4bios_req;
    m_pstate_cnt = fadt.pstate_cnt;

    m_PM1a_EVT_BLK = fadt.PM1a_EVT_BLK;
    m_PM1b_EVT_BLK = fadt.PM1b_EVT_BLK;
    m_PM1a_CNT_BLK = fadt.PM1a_CNT_BLK;
    m_PM1b_CNT_BLK = fadt.PM1b_CNT_BLK;
    m_PM2_CNT_BLK = fadt.PM2_CNT_BLK;
    m_PM_TMR_BLK = fadt.PM_TMR_BLK;
    m_GPE0_BLK = fadt.GPE0_BLK;
    m_GPE1_BLK = fadt.GPE1_BLK;
    m_PM1_EVT_LEN = fadt.PM1_EVT_LEN;
    m_PM1_CNT_LEN = fadt.PM1_CNT_LEN;
    m_PM2_CNT_LEN = fadt.PM2_CNT_LEN;
    m_PM_TMR_LEN = fadt.PM_TMR_LEN;
    m_GPE0_BLK_LEN = fadt.GPE0_BLK_LEN;
    m_GPE1_BLK_LEN = fadt.GPE1_BLK_LEN;
    m_GPE1_BASE = fadt.GPE1_BASE;
    m_cst_cnt = fadt.cst_cnt;
    m_P_LVL2_LAT = fadt.P_LVL2_LAT;
    m_P_LVL3_LAT = fadt.P_LVL3_LAT;
    m_flush_size = fadt.flush_size;

    m_flush_stride = fadt.flush_stride;
    m_duty_offset = fadt.duty_offset;
    m_duty_width = fadt.duty_width;
    m_day_alrm = fadt.day_alrm;
    m_mon_alrm = fadt.mon_alrm;
    m_century = fadt.century;

    m_ia_pc_boot_arch_flags = fadt.ia_pc_boot_arch_flags;
    m_flags = fadt.flags;

    m_reset_reg = fadt.reset_reg;
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Reset Register @ IO 0x%x\n", m_reset_reg.address);
    dbgprintf("ACPI: Reset Register Address space %x\n", fadt.reset_reg.address_space);
#endif
    m_reset_value = fadt.reset_value;
#ifdef ACPI_DEBUG
    dbgprintf("ACPI: Reset Register value @ P 0x%x\n", m_reset_value);
#endif
    m_x_pm1a_evt_blk = fadt.x_pm1a_evt_blk;
    m_x_pm1b_evt_blk = fadt.x_pm1b_evt_blk;
    m_x_pm1a_cnt_blk = fadt.x_pm1a_cnt_blk;
    m_x_pm1b_cnt_blk = fadt.x_pm1b_cnt_blk;
    m_x_pm2_cnt_blk = fadt.x_pm2_cnt_blk;
    m_x_pm_tmr_blk = fadt.x_pm_tmr_blk;
    m_x_gpe0_blk = fadt.x_gpe0_blk;
    m_x_gpe1_blk = fadt.x_gpe1_blk;
    m_sleep_control = fadt.sleep_control;
    m_sleep_status = fadt.sleep_status;

    m_hypervisor_vendor_identity = fadt.hypervisor_vendor_identity;
}

ACPI_RAW::SDTHeader* ACPI::FixedACPIData::get_dsdt()
{
    if (m_x_dsdt_ptr != (uintptr_t) nullptr)
        return (ACPI_RAW::SDTHeader*)m_x_dsdt_ptr;
    else {
        ASSERT((ACPI_RAW::SDTHeader*)m_dsdt_ptr != nullptr);
        return (ACPI_RAW::SDTHeader*)m_dsdt_ptr;
    }
}

}
