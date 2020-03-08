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

#include <Kernel/ACPI/ACPIStaticParser.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>
#include <LibBareMetal/StdLib.h>

//#define ACPI_DEBUG

namespace Kernel {
namespace ACPI {

    void StaticParser::initialize(PhysicalAddress rsdp)
    {
        if (!Parser::is_initialized()) {
            new StaticParser(rsdp);
        }
    }
    void StaticParser::initialize_without_rsdp()
    {
        if (!Parser::is_initialized()) {
            new StaticParser();
        }
    }

    bool StaticParser::is_initialized()
    {
        return Parser::is_initialized();
    }

    void StaticParser::locate_static_data()
    {
        locate_main_system_description_table();
        initialize_main_system_description_table();
        init_fadt();
        init_facs();
    }

    PhysicalAddress StaticParser::find_table(const char* sig)
    {
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Calling Find Table method!";
#endif
        for (auto p_sdt : m_sdt_pointers) {
            auto region = MM.allocate_kernel_region(p_sdt.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser Tables Finding", Region::Access::Read);
            auto* sdt = (const Structures::SDTHeader*)region->vaddr().offset(p_sdt.offset_in_page()).as_ptr();
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Examining Table @ P " << physical_sdt_ptr;
#endif
            if (!strncmp(sdt->sig, sig, 4)) {
#ifdef ACPI_DEBUG
                dbg() << "ACPI: Found Table @ P " << physical_sdt_ptr;
#endif
                return p_sdt;
            }
        }
        return {};
    }

    void StaticParser::init_facs()
    {
        m_facs = find_table("FACS");
    }

    void StaticParser::init_fadt()
    {
        klog() << "ACPI: Initializing Fixed ACPI data";
        klog() << "ACPI: Searching for the Fixed ACPI Data Table";

        m_fadt = find_table("FACP");
        ASSERT(!m_fadt.is_null());

        auto checkup_region = MM.allocate_kernel_region(m_fadt.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
        auto* sdt = (const Structures::SDTHeader*)checkup_region->vaddr().offset(m_fadt.offset_in_page()).as_ptr();
#ifdef ACPI_DEBUG
        dbg() << "ACPI: FADT @ V " << sdt << ", P " << (void*)fadt.as_ptr();
#endif
        klog() << "ACPI: Fixed ACPI data, Revision " << sdt->revision << ", Length " << sdt->length << " bytes";
    }

    bool StaticParser::can_reboot()
    {
        auto region = MM.allocate_kernel_region(m_fadt.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
        auto* fadt = (const Structures::FADT*)region->vaddr().offset(m_fadt.offset_in_page()).as_ptr();
        if (fadt->h.revision < 2)
            return false;
        return (fadt->flags & (u32)FADTFeatureFlags::RESET_REG_SUPPORTED) != 0;
    }

    void StaticParser::access_generic_address(const Structures::GenericAddressStructure& structure, u32 value)
    {
        switch (structure.address_space) {
        case (u8)GenericAddressStructure::AddressSpace::SystemIO: {
            dbg() << "ACPI: Sending value 0x" << String::format("%x", value) << " to " << IOAddress(structure.address);
            switch (structure.access_size) {
            case (u8)GenericAddressStructure::AccessSize::Byte: {
                IO::out8(structure.address, value);
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::Word: {
                IO::out16(structure.address, value);
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::DWord: {
                IO::out32(structure.address, value);
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::QWord: {
                dbg() << "Trying to send QWord to IO port";
                ASSERT_NOT_REACHED();
                break;
            }
            default:
                // FIXME: Determine if for reset register we can actually determine the right IO operation.
                dbg() << "ACPI Warning: Unknown access size " << structure.access_size;
                IO::out8(structure.address, value);
                break;
            }
            return;
        }
        case (u8)GenericAddressStructure::AddressSpace::SystemMemory: {
            auto p_reg = PhysicalAddress(structure.address);
            auto p_region = MM.allocate_kernel_region(p_reg.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
            dbg() << "ACPI: Sending value 0x" << String::format("%x", value) << " to " << p_reg;
            switch (structure.access_size) {
            case (u8)GenericAddressStructure::AccessSize::Byte: {
                auto* reg = (volatile u8*)p_region->vaddr().offset(p_reg.offset_in_page()).as_ptr();
                (*reg) = value;
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::Word: {
                auto* reg = (volatile u16*)p_region->vaddr().offset(p_reg.offset_in_page()).as_ptr();
                (*reg) = value;
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::DWord: {
                auto* reg = (volatile u32*)p_region->vaddr().offset(p_reg.offset_in_page()).as_ptr();
                (*reg) = value;
                break;
            }
            case (u8)GenericAddressStructure::AccessSize::QWord: {
                auto* reg = (volatile u64*)p_region->vaddr().offset(p_reg.offset_in_page()).as_ptr();
                (*reg) = value;
                break;
            }
            default:
                ASSERT_NOT_REACHED();
            }
            return;
        }
        case (u8)GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
            // According to the ACPI specification 6.2, page 168, PCI addresses must be confined to devices on Segment group 0, bus 0.
            auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
            dbg() << "ACPI: Sending value 0x" << String::format("%x", value) << " to " << pci_address;
            u32 offset_in_pci_address = structure.address & 0xFFFF;
            if (structure.access_size == (u8)GenericAddressStructure::AccessSize::QWord) {
                dbg() << "Trying to send QWord to PCI configuration space";
                ASSERT_NOT_REACHED();
            }
            ASSERT(structure.access_size != (u8)GenericAddressStructure::AccessSize::Undefined);
            PCI::raw_access(pci_address, offset_in_pci_address, (1 << (structure.access_size - 1)), value);
            return;
        }
        default:
            ASSERT_NOT_REACHED();
        }
        ASSERT_NOT_REACHED();
    }

    bool StaticParser::validate_reset_register()
    {
        // According to the ACPI spec 6.2, page 152, The reset register can only be located in I/O bus, PCI bus or memory-mapped.
        auto region = MM.allocate_kernel_region(m_fadt.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
        auto* fadt = (const Structures::FADT*)region->vaddr().offset(m_fadt.offset_in_page()).as_ptr();
        return (fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::PCIConfigurationSpace || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemMemory || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemIO);
    }

    void StaticParser::try_acpi_reboot()
    {
        InterruptDisabler disabler;
        if (!can_reboot()) {
            klog() << "ACPI: Reboot, Not supported!";
            return;
        }
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Rebooting, Probing FADT (" << m_fadt << ")";
#endif

        auto region = MM.allocate_kernel_region(m_fadt.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser", Region::Access::Read);
        auto* fadt = (const Structures::FADT*)region->vaddr().offset(m_fadt.offset_in_page()).as_ptr();
        ASSERT(validate_reset_register());
        access_generic_address(fadt->reset_reg, fadt->reset_value);
        for (;;)
            ;
    }

    void StaticParser::try_acpi_shutdown()
    {
        klog() << "ACPI: Shutdown is not supported with the current configuration, Abort!";
    }

    size_t StaticParser::get_table_size(PhysicalAddress table_header)
    {
        InterruptDisabler disabler;
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Checking SDT Length";
#endif
        auto region = MM.allocate_kernel_region(table_header.page_base(), (PAGE_SIZE * 2), "ACPI get_table_size()", Region::Access::Read);
        auto* sdt = (volatile Structures::SDTHeader*)region->vaddr().offset(table_header.offset_in_page()).as_ptr();
        return sdt->length;
    }

    u8 StaticParser::get_table_revision(PhysicalAddress table_header)
    {
        InterruptDisabler disabler;
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Checking SDT Revision";
#endif
        auto region = MM.allocate_kernel_region(table_header.page_base(), (PAGE_SIZE * 2), "ACPI get_table_revision()", Region::Access::Read);
        auto* sdt = (volatile Structures::SDTHeader*)region->vaddr().offset(table_header.offset_in_page()).as_ptr();
        return sdt->revision;
    }

    void StaticParser::initialize_main_system_description_table()
    {
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Checking Main SDT Length to choose the correct mapping size";
#endif
        ASSERT(!m_main_system_description_table.is_null());
        auto length = get_table_size(m_main_system_description_table);
        auto revision = get_table_revision(m_main_system_description_table);

        auto main_sdt_region = MM.allocate_kernel_region(m_main_system_description_table.page_base(), PAGE_ROUND_UP(length) + PAGE_SIZE, "ACPI Static Parser Initialization", Region::Access::Read, false, true);
        auto* sdt = (volatile Structures::SDTHeader*)main_sdt_region->vaddr().offset(m_main_system_description_table.offset_in_page()).as_ptr();
        klog() << "ACPI: Main Description Table valid? " << StaticParsing::validate_table(const_cast<Structures::SDTHeader&>(*sdt), length);

        if (m_xsdt_supported) {
            volatile auto* xsdt = (volatile Structures::XSDT*)sdt;
            klog() << "ACPI: Using XSDT, Enumerating tables @ " << m_main_system_description_table;
            klog() << "ACPI: XSDT Revision " << revision << ", Total length - " << length;
#ifdef ACPI_DEBUG
            dbg() << "ACPI: XSDT pointer @ V " << xsdt;
#endif
            for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u64)); i++) {
#ifdef ACPI_DEBUG
                dbg() << "ACPI: Found new table [" << i << "], @ V 0x" << String::format("%x", &xsdt->table_ptrs[i]) << " - P 0x" << String::format("%x", xsdt->table_ptrs[i]);
#endif
                m_sdt_pointers.append(PhysicalAddress(xsdt->table_ptrs[i]));
            }
        } else {
            volatile auto* rsdt = (volatile Structures::RSDT*)sdt;
            klog() << "ACPI: Using RSDT, Enumerating tables @ " << m_main_system_description_table;
            klog() << "ACPI: RSDT Revision " << revision << ", Total length - " << length;
#ifdef ACPI_DEBUG
            dbg() << "ACPI: RSDT pointer @ V " << rsdt;
#endif
            for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
#ifdef ACPI_DEBUG
                dbg() << "ACPI: Found new table [" << i << "], @ V 0x" << String::format("%x", &rsdt->table_ptrs[i]) << " - P 0x" << String::format("%x", rsdt->table_ptrs[i]);
#endif
                m_sdt_pointers.append(PhysicalAddress(rsdt->table_ptrs[i]));
            }
        }
    }

    void StaticParser::locate_main_system_description_table()
    {
        auto rsdp_region = MM.allocate_kernel_region(m_rsdp.page_base(), (PAGE_SIZE * 2), "ACPI Static Parser Initialization", Region::Access::Read, false, true);
        volatile auto* rsdp = (Structures::RSDPDescriptor20*)rsdp_region->vaddr().offset(m_rsdp.offset_in_page()).as_ptr();
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
            m_main_system_description_table = PhysicalAddress(rsdp->base.rsdt_ptr);
        } else {
            m_main_system_description_table = PhysicalAddress(rsdp->xsdt_ptr);
        }
    }

    StaticParser::StaticParser()
        : Parser(true)
        , m_rsdp(StaticParsing::search_rsdp())
    {
        if (!m_rsdp.is_null()) {
            klog() << "ACPI: Using RSDP @ " << m_rsdp;
            m_operable = true;
            locate_static_data();
        } else {
            m_operable = false;
            klog() << "ACPI: Disabled, due to RSDP being absent";
        }
    }

    StaticParser::StaticParser(PhysicalAddress rsdp)
        : Parser(true)
        , m_rsdp(rsdp)
    {
        klog() << "ACPI: Using RSDP @ " << rsdp;
        m_operable = true;
        locate_static_data();
    }

    PhysicalAddress StaticParsing::search_rsdp_in_ebda(u16 ebda_segment)
    {
        auto rsdp_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of((u32)(ebda_segment << 4))), PAGE_ROUND_UP(1024), "ACPI Static Parser RSDP Finding #1", Region::Access::Read, false, true);
        char* p_rsdp_str = (char*)(PhysicalAddress(ebda_segment << 4).as_ptr());
        for (char* rsdp_str = (char*)rsdp_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).as_ptr(); rsdp_str < (char*)(rsdp_region->vaddr().offset(offset_in_page((u32)(ebda_segment << 4))).get() + 1024); rsdp_str += 16) {
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Looking for RSDP in EBDA @ V " << (void*)rsdp_str << ", P " << (void*)p_rsdp_str;
#endif
            if (!strncmp("RSD PTR ", rsdp_str, strlen("RSD PTR ")))
                return PhysicalAddress((FlatPtr)p_rsdp_str);
            p_rsdp_str += 16;
        }
        return {};
    }

    PhysicalAddress StaticParsing::search_rsdp_in_bios_area()
    {
        auto rsdp_region = MM.allocate_kernel_region(PhysicalAddress(0xE0000), PAGE_ROUND_UP(0xFFFFF - 0xE0000), "ACPI Static Parser RSDP Finding #2", Region::Access::Read, false, true);
        char* p_rsdp_str = (char*)(PhysicalAddress(0xE0000).as_ptr());
        for (char* rsdp_str = (char*)rsdp_region->vaddr().offset(offset_in_page((u32)(0xE0000))).as_ptr(); rsdp_str < (char*)(rsdp_region->vaddr().offset(offset_in_page((u32)(0xE0000))).get() + (0xFFFFF - 0xE0000)); rsdp_str += 16) {
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Looking for RSDP in BIOS ROM area @ V " << (void*)rsdp_str << ", P " << (void*)p_rsdp_str;
#endif
            if (!strncmp("RSD PTR ", rsdp_str, strlen("RSD PTR ")))
                return PhysicalAddress((FlatPtr)p_rsdp_str);
            p_rsdp_str += 16;
        }
        return {};
    }

    inline bool StaticParsing::validate_table(Structures::SDTHeader& v_header, size_t length)
    {
        u8 checksum = 0;
        auto* sdt = (u8*)&v_header;
        for (size_t i = 0; i < length; i++)
            checksum += sdt[i];
        if (checksum == 0)
            return true;
        return false;
    }

    PhysicalAddress StaticParsing::search_rsdp()
    {
        PhysicalAddress rsdp;
        auto region = MM.allocate_kernel_region(PhysicalAddress(0), PAGE_SIZE, "ACPI RSDP Searching", Region::Access::Read);
        u16 ebda_seg = (u16) * ((uint16_t*)((region->vaddr().get() & PAGE_MASK) + 0x40e));
        klog() << "ACPI: Probing EBDA, Segment 0x" << String::format("%x", ebda_seg);

        rsdp = search_rsdp_in_ebda(ebda_seg);
        if (!rsdp.is_null())
            return rsdp;
        return search_rsdp_in_bios_area();
    }

    PhysicalAddress StaticParsing::search_table(PhysicalAddress rsdp, const char* signature)
    {
        // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
        // FIXME: Don't blindly use PAGE_SIZE here, but probe the actual length.

        ASSERT(strlen(signature) == 4);
        auto rsdp_region = MM.allocate_kernel_region(rsdp.page_base(), (PAGE_SIZE * 2), "ACPI Static Parsing search_table()", Region::Access::Read, false, true);
        volatile auto* rsdp_ptr = (Structures::RSDPDescriptor20*)rsdp_region->vaddr().offset(rsdp.offset_in_page()).as_ptr();
        if (rsdp_ptr->base.revision == 0) {
            return search_table_in_rsdt(PhysicalAddress(rsdp_ptr->base.rsdt_ptr), signature);
        }
        if (rsdp_ptr->base.revision >= 2) {
            if (rsdp_ptr->xsdt_ptr != (u64) nullptr)
                return search_table_in_xsdt(PhysicalAddress(rsdp_ptr->xsdt_ptr), signature);
            return search_table_in_rsdt(PhysicalAddress(rsdp_ptr->base.rsdt_ptr), signature);
        }
        ASSERT_NOT_REACHED();
    }

    PhysicalAddress StaticParsing::search_table_in_xsdt(PhysicalAddress xsdt, const char* signature)
    {
        // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
        // FIXME: Don't blindly use PAGE_SIZE here, but probe the actual length.

        ASSERT(strlen(signature) == 4);
        auto main_sdt_region = MM.allocate_kernel_region(xsdt.page_base(), PAGE_SIZE, "ACPI Static Parsing search_table_in_xsdt()", Region::Access::Read, false, true);
        auto* xsdt_ptr = (volatile Structures::XSDT*)main_sdt_region->vaddr().offset(xsdt.offset_in_page()).as_ptr();
        for (u32 i = 0; i < ((xsdt_ptr->h.length - sizeof(Structures::SDTHeader)) / sizeof(u64)); i++) {
            if (match_table_signature(PhysicalAddress((FlatPtr)xsdt_ptr->table_ptrs[i]), signature))
                return PhysicalAddress((FlatPtr)xsdt_ptr->table_ptrs[i]);
        }
        return {};
    }

    bool StaticParsing::match_table_signature(PhysicalAddress table_header, const char* signature)
    {
        // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
        // FIXME: Don't blindly use PAGE_SIZE here, but probe the actual length.

        ASSERT(strlen(signature) == 4);
        auto main_sdt_region = MM.allocate_kernel_region(table_header.page_base(), PAGE_SIZE, "ACPI Static Parsing match_table_signature()", Region::Access::Read, false, true);
        auto* table_ptr = (volatile Structures::RSDT*)main_sdt_region->vaddr().offset(table_header.offset_in_page()).as_ptr();
        return !strncmp(const_cast<const char*>(table_ptr->h.sig), signature, 4);
    }

    PhysicalAddress StaticParsing::search_table_in_rsdt(PhysicalAddress rsdt, const char* signature)
    {
        // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
        // FIXME: Don't blindly use PAGE_SIZE here, but probe the actual length.
        ASSERT(strlen(signature) == 4);

        auto main_sdt_region = MM.allocate_kernel_region(rsdt.page_base(), PAGE_SIZE, "ACPI Static Parsing search_table_in_rsdt()", Region::Access::Read, false, true);
        auto* rsdt_ptr = (volatile Structures::RSDT*)main_sdt_region->vaddr().offset(rsdt.offset_in_page()).as_ptr();

        for (u32 i = 0; i < ((rsdt_ptr->h.length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
            if (match_table_signature(PhysicalAddress((FlatPtr)rsdt_ptr->table_ptrs[i]), signature))
                return PhysicalAddress((FlatPtr)rsdt_ptr->table_ptrs[i]);
        }
        return {};
    }
}
}
