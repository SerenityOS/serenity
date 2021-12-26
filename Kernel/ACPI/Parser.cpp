/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StringView.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/IO.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel::ACPI {

static Parser* s_acpi_parser;

Parser* Parser::the()
{
    return s_acpi_parser;
}

UNMAP_AFTER_INIT NonnullRefPtr<ACPISysFSComponent> ACPISysFSComponent::create(String name, PhysicalAddress paddr, size_t table_size)
{
    return adopt_ref(*new (nothrow) ACPISysFSComponent(name, paddr, table_size));
}

KResultOr<size_t> ACPISysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    auto blob = try_to_generate_buffer();
    if (!blob)
        return KResult(EFAULT);

    if ((size_t)offset >= blob->size())
        return KSuccess;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(blob->data() + offset, nread))
        return KResult(EFAULT);
    return nread;
}

OwnPtr<KBuffer> ACPISysFSComponent::try_to_generate_buffer() const
{
    auto acpi_blob = map_typed<u8>((m_paddr), m_length);
    return KBuffer::try_create_with_bytes(Span<u8> { acpi_blob.ptr(), m_length });
}

UNMAP_AFTER_INIT ACPISysFSComponent::ACPISysFSComponent(String name, PhysicalAddress paddr, size_t table_size)
    : SysFSComponent(name)
    , m_paddr(paddr)
    , m_length(table_size)
{
}

UNMAP_AFTER_INIT void ACPISysFSDirectory::initialize()
{
    auto acpi_folder = adopt_ref(*new (nothrow) ACPISysFSDirectory());
    SysFSComponentRegistry::the().register_new_component(acpi_folder);
}

UNMAP_AFTER_INIT ACPISysFSDirectory::ACPISysFSDirectory()
    : SysFSDirectory("acpi", SysFSComponentRegistry::the().root_folder())
{
    NonnullRefPtrVector<SysFSComponent> components;
    size_t ssdt_count = 0;
    ACPI::Parser::the()->enumerate_static_tables([&](const StringView& signature, PhysicalAddress p_table, size_t length) {
        if (signature == "SSDT") {
            components.append(ACPISysFSComponent::create(String::formatted("{:4s}{}", signature.characters_without_null_termination(), ssdt_count), p_table, length));
            ssdt_count++;
            return;
        }
        components.append(ACPISysFSComponent::create(signature, p_table, length));
    });
    m_components = components;

    auto rsdp = map_typed<Structures::RSDPDescriptor20>(ACPI::Parser::the()->rsdp());
    m_components.append(ACPISysFSComponent::create("RSDP", ACPI::Parser::the()->rsdp(), rsdp->base.revision == 0 ? sizeof(Structures::RSDPDescriptor) : rsdp->length));

    auto main_system_description_table = map_typed<Structures::SDTHeader>(ACPI::Parser::the()->main_system_description_table());
    if (ACPI::Parser::the()->is_xsdt_supported()) {
        m_components.append(ACPISysFSComponent::create("XSDT", ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
    } else {
        m_components.append(ACPISysFSComponent::create("RSDT", ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
    }
}

void Parser::enumerate_static_tables(Function<void(const StringView&, PhysicalAddress, size_t)> callback)
{
    for (auto& p_table : m_sdt_pointers) {
        auto table = map_typed<Structures::SDTHeader>(p_table);
        callback({ table->sig, 4 }, p_table, table->length);
    }
}

void Parser::set_the(Parser& parser)
{
    VERIFY(!s_acpi_parser);
    s_acpi_parser = &parser;
}

static bool match_table_signature(PhysicalAddress table_header, const StringView& signature);
static PhysicalAddress search_table_in_xsdt(PhysicalAddress xsdt, const StringView& signature);
static PhysicalAddress search_table_in_rsdt(PhysicalAddress rsdt, const StringView& signature);
static bool validate_table(const Structures::SDTHeader&, size_t length);

UNMAP_AFTER_INIT void Parser::locate_static_data()
{
    locate_main_system_description_table();
    initialize_main_system_description_table();
    init_fadt();
    init_facs();
}

UNMAP_AFTER_INIT PhysicalAddress Parser::find_table(const StringView& signature)
{
    dbgln_if(ACPI_DEBUG, "ACPI: Calling Find Table method!");
    for (auto p_sdt : m_sdt_pointers) {
        auto sdt = map_typed<Structures::SDTHeader>(p_sdt);
        dbgln_if(ACPI_DEBUG, "ACPI: Examining Table @ {}", p_sdt);
        if (!strncmp(sdt->sig, signature.characters_without_null_termination(), 4)) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found Table @ {}", p_sdt);
            return p_sdt;
        }
    }
    return {};
}

UNMAP_AFTER_INIT void Parser::init_facs()
{
    m_facs = find_table("FACS");
}

UNMAP_AFTER_INIT void Parser::init_fadt()
{
    dmesgln("ACPI: Initializing Fixed ACPI data");
    dmesgln("ACPI: Searching for the Fixed ACPI Data Table");

    m_fadt = find_table("FACP");
    VERIFY(!m_fadt.is_null());

    auto sdt = map_typed<const volatile Structures::FADT>(m_fadt);

    dbgln_if(ACPI_DEBUG, "ACPI: FADT @ V{}, {}", &sdt, m_fadt);

    auto* header = &sdt.ptr()->h;
    dmesgln("ACPI: Fixed ACPI data, Revision {}, length: {} bytes", (size_t)header->revision, (size_t)header->length);
    dmesgln("ACPI: DSDT {}", PhysicalAddress(sdt->dsdt_ptr));
    m_x86_specific_flags.cmos_rtc_not_present = (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::CMOS_RTC_Not_Present);

    // FIXME: QEMU doesn't report that we have an i8042 controller in these flags, even if it should (when FADT revision is 3),
    // Later on, we need to make sure that we enumerate the ACPI namespace (AML encoded), instead of just using this value.
    m_x86_specific_flags.keyboard_8042 = (sdt->h.revision <= 3) || (sdt->ia_pc_boot_arch_flags & (u8)FADTFlags::IA_PC_Flags::PS2_8042);

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
        dbgln("ACPI: Sending value {:x} to {}", value, address);
        switch (structure.access_size) {
        case (u8)GenericAddressStructure::AccessSize::QWord: {
            dbgln("Trying to send QWord to IO port");
            VERIFY_NOT_REACHED();
            break;
        }
        case (u8)GenericAddressStructure::AccessSize::Undefined: {
            dbgln("ACPI Warning: Unknown access size {}", structure.access_size);
            VERIFY(structure.bit_width != (u8)GenericAddressStructure::BitWidth::QWord);
            VERIFY(structure.bit_width != (u8)GenericAddressStructure::BitWidth::Undefined);
            dbgln("ACPI: Bit Width - {} bits", structure.bit_width);
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
        dbgln("ACPI: Sending value {:x} to {}", value, PhysicalAddress(structure.address));
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
            VERIFY_NOT_REACHED();
        }
        return;
    }
    case GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
        // According to https://uefi.org/specs/ACPI/6.4/05_ACPI_Software_Programming_Model/ACPI_Software_Programming_Model.html#address-space-format,
        // PCI addresses must be confined to devices on Segment group 0, bus 0.
        auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
        dbgln("ACPI: Sending value {:x} to {}", value, pci_address);
        u32 offset_in_pci_address = structure.address & 0xFFFF;
        if (structure.access_size == (u8)GenericAddressStructure::AccessSize::QWord) {
            dbgln("Trying to send QWord to PCI configuration space");
            VERIFY_NOT_REACHED();
        }
        VERIFY(structure.access_size != (u8)GenericAddressStructure::AccessSize::Undefined);
        PCI::raw_access(pci_address, offset_in_pci_address, (1 << (structure.access_size - 1)), value);
        return;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

bool Parser::validate_reset_register()
{
    // According to https://uefi.org/specs/ACPI/6.4/04_ACPI_Hardware_Specification/ACPI_Hardware_Specification.html#reset-register,
    // the reset register can only be located in I/O bus, PCI bus or memory-mapped.
    auto fadt = map_typed<Structures::FADT>(m_fadt);
    return (fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::PCIConfigurationSpace || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemMemory || fadt->reset_reg.address_space == (u8)GenericAddressStructure::AddressSpace::SystemIO);
}

void Parser::try_acpi_reboot()
{
    InterruptDisabler disabler;
    if (!can_reboot()) {
        dmesgln("ACPI: Reboot not supported!");
        return;
    }
    dbgln_if(ACPI_DEBUG, "ACPI: Rebooting, probing FADT ({})", m_fadt);

    auto fadt = map_typed<Structures::FADT>(m_fadt);
    VERIFY(validate_reset_register());
    access_generic_address(fadt->reset_reg, fadt->reset_value);
    Processor::halt();
}

void Parser::try_acpi_shutdown()
{
    dmesgln("ACPI: Shutdown is not supported with the current configuration, aborting!");
}

size_t Parser::get_table_size(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
    dbgln_if(ACPI_DEBUG, "ACPI: Checking SDT Length");
    return map_typed<Structures::SDTHeader>(table_header)->length;
}

u8 Parser::get_table_revision(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
    dbgln_if(ACPI_DEBUG, "ACPI: Checking SDT Revision");
    return map_typed<Structures::SDTHeader>(table_header)->revision;
}

UNMAP_AFTER_INIT void Parser::initialize_main_system_description_table()
{
    dbgln_if(ACPI_DEBUG, "ACPI: Checking Main SDT Length to choose the correct mapping size");
    VERIFY(!m_main_system_description_table.is_null());
    auto length = get_table_size(m_main_system_description_table);
    auto revision = get_table_revision(m_main_system_description_table);

    auto sdt = map_typed<Structures::SDTHeader>(m_main_system_description_table, length);

    dmesgln("ACPI: Main Description Table valid? {}", validate_table(*sdt, length));

    if (m_xsdt_supported) {
        auto& xsdt = (const Structures::XSDT&)*sdt;
        dmesgln("ACPI: Using XSDT, enumerating tables @ {}", m_main_system_description_table);
        dmesgln("ACPI: XSDT revision {}, total length: {}", revision, length);
        dbgln_if(ACPI_DEBUG, "ACPI: XSDT pointer @ {}", VirtualAddress { &xsdt });
        for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u64)); i++) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found new table [{0}], @ V{1:p} - P{1:p}", i, &xsdt.table_ptrs[i]);
            m_sdt_pointers.append(PhysicalAddress(xsdt.table_ptrs[i]));
        }
    } else {
        auto& rsdt = (const Structures::RSDT&)*sdt;
        dmesgln("ACPI: Using RSDT, enumerating tables @ {}", m_main_system_description_table);
        dmesgln("ACPI: RSDT revision {}, total length: {}", revision, length);
        dbgln_if(ACPI_DEBUG, "ACPI: RSDT pointer @ V{}", &rsdt);
        for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found new table [{0}], @ V{1:p} - P{1:p}", i, &rsdt.table_ptrs[i]);
            m_sdt_pointers.append(PhysicalAddress(rsdt.table_ptrs[i]));
        }
    }
}

UNMAP_AFTER_INIT void Parser::locate_main_system_description_table()
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

UNMAP_AFTER_INIT Parser::Parser(PhysicalAddress rsdp)
    : m_rsdp(rsdp)
{
    dmesgln("ACPI: Using RSDP @ {}", rsdp);
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

// https://uefi.org/specs/ACPI/6.4/05_ACPI_Software_Programming_Model/ACPI_Software_Programming_Model.html#finding-the-rsdp-on-ia-pc-systems
UNMAP_AFTER_INIT Optional<PhysicalAddress> StaticParsing::find_rsdp()
{
    StringView signature("RSD PTR ");
    auto rsdp = map_ebda().find_chunk_starting_with(signature, 16);
    if (rsdp.has_value())
        return rsdp;
    return map_bios().find_chunk_starting_with(signature, 16);
}

UNMAP_AFTER_INIT PhysicalAddress StaticParsing::find_table(PhysicalAddress rsdp_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto rsdp = map_typed<Structures::RSDPDescriptor20>(rsdp_address);

    if (rsdp->base.revision == 0)
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);

    if (rsdp->base.revision >= 2) {
        if (rsdp->xsdt_ptr)
            return search_table_in_xsdt(PhysicalAddress(rsdp->xsdt_ptr), signature);
        return search_table_in_rsdt(PhysicalAddress(rsdp->base.rsdt_ptr), signature);
    }
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT static PhysicalAddress search_table_in_xsdt(PhysicalAddress xsdt_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

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
    VERIFY(signature.length() == 4);

    auto table = map_typed<Structures::RSDT>(table_header);
    return !strncmp(table->h.sig, signature.characters_without_null_termination(), 4);
}

UNMAP_AFTER_INIT static PhysicalAddress search_table_in_rsdt(PhysicalAddress rsdt_address, const StringView& signature)
{
    // FIXME: There's no validation of ACPI tables here. Use the checksum to validate the tables.
    VERIFY(signature.length() == 4);

    auto rsdt = map_typed<Structures::RSDT>(rsdt_address);

    for (u32 i = 0; i < ((rsdt->h.length - sizeof(Structures::SDTHeader)) / sizeof(u32)); i++) {
        if (match_table_signature(PhysicalAddress((FlatPtr)rsdt->table_ptrs[i]), signature))
            return PhysicalAddress((FlatPtr)rsdt->table_ptrs[i]);
    }
    return {};
}

void Parser::enable_aml_interpretation()
{
    VERIFY_NOT_REACHED();
}

void Parser::enable_aml_interpretation(File&)
{
    VERIFY_NOT_REACHED();
}

void Parser::enable_aml_interpretation(u8*, u32)
{
    VERIFY_NOT_REACHED();
}

void Parser::disable_aml_interpretation()
{
    VERIFY_NOT_REACHED();
}

}
