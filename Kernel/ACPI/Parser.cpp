/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StringView.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {
namespace ACPI {

static Parser* s_acpi_parser;

Parser* Parser::the()
{
    return s_acpi_parser;
}

void Parser::set_the(Parser& parser)
{
    ASSERT(!s_acpi_parser);
    s_acpi_parser = &parser;
}

static bool match_table_signature(PhysicalAddress table_header, const StringView& signature);
static PhysicalAddress search_table_in_xsdt(PhysicalAddress xsdt, const StringView& signature);
static PhysicalAddress search_table_in_rsdt(PhysicalAddress rsdt, const StringView& signature);
static bool validate_table(const Structures::SDTHeader&, size_t length);

void Parser::locate_static_data()
{
    locate_main_system_description_table();
    initialize_main_system_description_table();
    init_fadt();
    init_facs();
}

PhysicalAddress Parser::find_table(const StringView& signature)
{
#ifdef ACPI_DEBUG
    dbg() << "ACPI: Calling Find Table method!";
#endif
    for (auto p_sdt : m_sdt_pointers) {
        auto sdt = map_typed<Structures::SDTHeader>(p_sdt);
#ifdef ACPI_DEBUG
        dbg() << "ACPI: Examining Table @ P " << p_sdt;
#endif
        if (!strncmp(sdt->sig, signature.characters_without_null_termination(), 4)) {
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Found Table @ P " << p_sdt;
#endif
            return p_sdt;
        }
    }
    return {};
}

void Parser::init_facs()
{
    m_facs = find_table("FACS");
}

void Parser::init_fadt()
{
    klog() << "ACPI: Initializing Fixed ACPI data";
    klog() << "ACPI: Searching for the Fixed ACPI Data Table";

    m_fadt = find_table("FACP");
    ASSERT(!m_fadt.is_null());

    auto sdt = map_typed<Structures::FADT>(m_fadt);

#ifdef ACPI_DEBUG
    dbg() << "ACPI: FADT @ V " << sdt << ", P " << (void*)m_fadt.as_ptr();
#endif
    klog() << "ACPI: Fixed ACPI data, Revision " << sdt->h.revision << ", Length " << sdt->h.length << " bytes";
    klog() << "ACPI: DSDT " << PhysicalAddress(sdt->dsdt_ptr);
    m_x86_specific_flags.cmos_rtc_not_present = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::CMOS_RTC_Not_Present);
    m_x86_specific_flags.keyboard_8042 = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::PS2_8042);
    m_x86_specific_flags.legacy_devices = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::Legacy_Devices);
    m_x86_specific_flags.msi_not_supported = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::MSI_Not_Supported);
    m_x86_specific_flags.vga_not_present = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::VGA_Not_Present);

    m_hardware_flags.cpu_software_sleep = (sdt->flags & (u32)FADTFlags::FeatureFlags::CPU_SW_SLP);
    m_hardware_flags.docking_capability = (sdt->flags & (u32)FADTFlags::FeatureFlags::DCK_CAP);
    m_hardware_flags.fix_rtc = (sdt->flags & (u32)FADTFlags::FeatureFlags::FIX_RTC);
    m_hardware_flags.force_apic_cluster_model = (sdt->flags & (u32)FADTFlags::FeatureFlags::FORCE_APIC_CLUSTER_MODEL);
    m_hardware_flags.force_apic_physical_destination_mode = (sdt->flags & (u32)FADTFlags::FeatureFlags::FORCE_APIC_PHYSICAL_DESTINATION_MODE);
    m_hardware_flags.hardware_reduced_acpi = (sdt->flags & (u32)FADTFlags::FeatureFlags::HW_REDUCED_ACPI);
    m_hardware_flags.headless = (sdt->flags & (u32)FADTFlags::FeatureFlags::HEADLESS);
    m_hardware_flags.low_power_s0_idle_capable = (sdt->flags & (u32)FADTFlags::FeatureFlags::LOW_POWER_S0_IDLE_CAPABLE);
    m_hardware_flags.multiprocessor_c2 = (sdt->flags & (u32)FADTFlags::FeatureFlags::P_LVL2_UP);
    m_hardware_flags.pci_express_wake = (sdt->flags & (u32)FADTFlags::FeatureFlags::PCI_EXP_WAK);
    m_hardware_flags.power_button = (sdt->flags & (u32)FADTFlags::FeatureFlags::PWR_BUTTON);
    m_hardware_flags.processor_c1 = (sdt->flags & (u32)FADTFlags::FeatureFlags::PROC_C1);
    m_hardware_flags.remote_power_on_capable = (sdt->flags & (u32)FADTFlags::FeatureFlags::REMOTE_POWER_ON_CAPABLE);
    m_hardware_flags.reset_register_supported = (sdt->flags & (u32)FADTFlags::FeatureFlags::RESET_REG_SUPPORTED);
    m_hardware_flags.rtc_s4 = (sdt->flags & (u32)FADTFlags::FeatureFlags::RTC_s4);
    m_hardware_flags.s4_rtc_status_valid = (sdt->flags & (u32)FADTFlags::FeatureFlags::S4_RTC_STS_VALID);
    m_hardware_flags.sealed_case = (sdt->flags & (u32)FADTFlags::FeatureFlags::SEALED_CASE);
    m_hardware_flags.sleep_button = (sdt->flags & (u32)FADTFlags::FeatureFlags::SLP_BUTTON);
    m_hardware_flags.timer_value_extension = (sdt->flags & (u32)FADTFlags::FeatureFlags::TMR_VAL_EXT);
    m_hardware_flags.use_platform_clock = (sdt->flags & (u32)FADTFlags::FeatureFlags::USE_PLATFORM_CLOCK);
    m_hardware_flags.wbinvd = (sdt->flags & (u32)FADTFlags::FeatureFlags::WBINVD);
    m_hardware_flags.wbinvd_flush = (sdt->flags & (u32)FADTFlags::FeatureFlags::WBINVD_FLUSH);
}

bool Parser::can_reboot()
{
    auto fadt = map_typed<Structures::FADT>(m_fadt);
    if (fadt->h.revision < 2)
        return false;
    return m_hardware_flags.reset_register_supported;
}

void Parser::access_generic_address(const Structures::GenericAddressStructure& structure, u32 value)
{
    switch ((GenericAddressStructure::AddressSpace)structure.address_space) {
    case GenericAddressStructure::AddressSpace::SystemIO: {
        IOAddress address(structure.address);
        dbg() << "ACPI: Sending value 0x" << String::format("%x", value) << " to " << address;
        switch (structure.access_size) {
        case (u8)GenericAddressStructure::AccessSize::QWord: {
            dbg() << "Trying to send QWord to IO port";
            ASSERT_NOT_REACHED();
            break;
        }
        case (u8)GenericAddressStructure::AccessSize::Undefined: {
            dbg() << "ACPI Warning: Unknown access size " << structure.access_size;
            ASSERT(structure.bit_width != (u8)GenericAddressStructure::BitWidth::QWord);
            ASSERT(structure.bit_width != (u8)GenericAddressStructure::BitWidth::Undefined);
            dbg() << "ACPI: Bit Width - " << structure.bit_width << " bits";
            address.out(value, structure.bit_width);
            break;
        }
        default:
            address.out(value, (8 << (structure.access_size - 1)));
            break;
        }
        return;
    }
    case GenericAddressStructure::AddressSpace::SystemMemory: {
        dbg() << "ACPI: Sending value 0x" << String::format("%x", value) << " to " << PhysicalAddress(structure.address);
        switch ((GenericAddressStructure::AccessSize)structure.access_size) {
        case GenericAddressStructure::AccessSize::Byte:
            *map_typed<u8>(PhysicalAddress(structure.address)) = value;
            break;
        case GenericAddressStructure::AccessSize::Word:
            *map_typed<u16>(PhysicalAddress(structure.address)) = value;
            break;
        case GenericAddressStructure::AccessSize::DWord:
            *map_typed<u32>(PhysicalAddress(structure.address)) = value;
            break;
        case GenericAddressStructure::AccessSize::QWord: {
            *map_typed<u64>(PhysicalAddress(structure.address)) = value;
            break;
        }
        default:
            ASSERT_NOT_REACHED();
        }
        return;
    }
    case GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
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

bool Parser::validate_reset_register()
{
    // According to the ACPI spec 6.2, page 152, The reset register can only be located in I/O bus, PCI bus or memory-mapped.
    auto fadt = map_typed<Structures::FADT>(m_fadt);
    return (fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::PCIConfigurationSpace || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemMemory || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemIO);
}

void Parser::try_acpi_reboot()
{
    InterruptDisabler disabler;
    if (!can_reboot()) {
        klog() << "ACPI: Reboot, Not supported!";
        return;
    }
#ifdef ACPI_DEBUG
    dbg() << "ACPI: Rebooting, Probing FADT (" << m_fadt << ")";
#endif

    auto fadt = map_typed<Structures::FADT>(m_fadt);
    ASSERT(validate_reset_register());
    access_generic_address(fadt->reset_reg, fadt->reset_value);
    Processor::halt();
}

void Parser::try_acpi_shutdown()
{
    klog() << "ACPI: Shutdown is not supported with the current configuration, Abort!";
}

size_t Parser::get_table_size(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
#ifdef ACPI_DEBUG
    dbg() << "ACPI: Checking SDT Length";
#endif
    return map_typed<Structures::SDTHeader>(table_header)->length;
}

u8 Parser::get_table_revision(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
#ifdef ACPI_DEBUG
    dbg() << "ACPI: Checking SDT Revision";
#endif
    return map_typed<Structures::SDTHeader>(table_header)->revision;
}

void Parser::initialize_main_system_description_table()
{
#ifdef ACPI_DEBUG
    dbg() << "ACPI: Checking Main SDT Length to choose the correct mapping size";
#endif
    ASSERT(!m_main_system_description_table.is_null());
    auto length = get_table_size(m_main_system_description_table);
    auto revision = get_table_revision(m_main_system_description_table);

    auto sdt = map_typed<Structures::SDTHeader>(m_main_system_description_table, length);

    klog() << "ACPI: Main Description Table valid? " << validate_table(*sdt, length);

    if (m_xsdt_supported) {
        auto& xsdt = (const Structures::XSDT&)*sdt;
        klog() << "ACPI: Using XSDT, Enumerating tables @ " << m_main_system_description_table;
        klog() << "ACPI: XSDT Revision " << revision << ", Total length - " << length;
#ifdef ACPI_DEBUG
        dbg() << "ACPI: XSDT pointer @ V " << xsdt;
#endif
        for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u64)); i++) {
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Found new table [" << i << "], @ V 0x" << String::format("%x", &xsdt.table_ptrs[i]) << " - P 0x" << String::format("%x", xsdt.table_ptrs[i]);
#endif
            m_sdt_pointers.append(PhysicalAddress(xsdt.table_ptrs[i]));
        }
    } else {
        auto& rsdt = (const Structures::RSDT&)*sdt;
        klog() << "ACPI: Using RSDT, Enumerating tables @ " << m_main_system_description_table;
        klog() << "ACPI: RSDT Revision " << revision << ", Total length - " << length;
#ifdef ACPI_DEBUG
        dbg() << "ACPI: RSDT pointer @ V " << rsdt;
#endif
        for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
#ifdef ACPI_DEBUG
            dbg() << "ACPI: Found new table [" << i << "], @ V 0x" << String::format("%x", &rsdt.table_ptrs[i]) << " - P 0x" << String::format("%x", rsdt.table_ptrs[i]);
#endif
            m_sdt_pointers.append(PhysicalAddress(rsdt.table_ptrs[i]));
        }
    }
}

void Parser::locate_main_system_description_table()
{
    auto rsdp = map_typed<Structures::RSDPDescriptor20>(m_rsdp);
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

Parser::Parser(PhysicalAddress rsdp)
    : m_rsdp(rsdp)
{
    klog() << "ACPI: Using RSDP @ " << rsdp;
    locate_static_data();
}

static bool validate_table(const Structures::SDTHeader& v_header, size_t length)
{
    u8 checksum = 0;
    auto* sdt = (const u8*)&v_header;
    for (size_t i = 0; i < length; i++)
        checksum += sdt[i];
    if (checksum == 0)
        return true;
    return false;
}

Optional<PhysicalAddress> StaticParsing::find_rsdp()
{
    StringView signature("RSD PTR ");
    auto rsdp = map_ebda().find_chunk_starting_with(signature, 16);
    if (rsdp.has_value())
        return rsdp;
    return map_bios().find_chunk_starting_with(signature, 16);
}

PhysicalAddress StaticParsing::find_table(PhysicalAddress rsdp_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    ASSERT(signature.length() == 4);

    auto rsdp = map_typed<Structures::RSDPDescriptor20>(rsdp_address);

    if (rsdp->base.revision == 0)
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);

    if (rsdp->base.revision >= 2) {
        if (rsdp->xsdt_ptr)
            return search_table_in_xsdt(PhysicalAddress(rsdp->xsdt_ptr), signature);
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);
    }
    ASSERT_NOT_REACHED();
}

static PhysicalAddress search_table_in_xsdt(PhysicalAddress xsdt_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    ASSERT(signature.length() == 4);

    auto xsdt = map_typed<Structures::XSDT>(xsdt_address);

    for (size_t i = 0; i < ((xsdt->h.length - sizeof(Structures::SDTHeader)) / sizeof(u64)); ++i) {
        if (match_table_signature(PhysicalAddress((FlatPtr)xsdt->table_ptrs[i]), signature))
            return PhysicalAddress((FlatPtr)xsdt->table_ptrs[i]);
    }
    return {};
}

static bool match_table_signature(PhysicalAddress table_header, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    ASSERT(signature.length() == 4);

    auto table = map_typed<Structures::RSDT>(table_header);
    return !strncmp(table->h.sig, signature.characters_without_null_termination(), 4);
}

static PhysicalAddress search_table_in_rsdt(PhysicalAddress rsdt_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    ASSERT(signature.length() == 4);

    auto rsdt = map_typed<Structures::RSDT>(rsdt_address);

    for (u32 i = 0; i < ((rsdt->h.length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
        if (match_table_signature(PhysicalAddress((FlatPtr)rsdt->table_ptrs[i]), signature))
            return PhysicalAddress((FlatPtr)rsdt->table_ptrs[i]);
    }
    return {};
}

void Parser::enable_aml_interpretation()
{
    ASSERT_NOT_REACHED();
}

void Parser::enable_aml_interpretation(File&)
{
    ASSERT_NOT_REACHED();
}

void Parser::enable_aml_interpretation(u8*, u32)
{
    ASSERT_NOT_REACHED();
}

void Parser::disable_aml_interpretation()
{
    ASSERT_NOT_REACHED();
}

}
}
