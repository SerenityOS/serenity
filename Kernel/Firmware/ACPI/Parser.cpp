/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/StringView.h>
#include <AK/Try.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#    include <Kernel/Arch/x86_64/IO.h>
#endif
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

static Parser* s_acpi_parser;

Parser* Parser::the()
{
    return s_acpi_parser;
}

void Parser::must_initialize(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number)
{
    VERIFY(!s_acpi_parser);
    s_acpi_parser = new (nothrow) Parser(rsdp, fadt, irq_number);
    VERIFY(s_acpi_parser);
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ACPISysFSComponent> ACPISysFSComponent::create(StringView name, PhysicalAddress paddr, size_t table_size)
{
    // FIXME: Handle allocation failure gracefully
    auto table_name = KString::must_create(name);
    return adopt_lock_ref(*new (nothrow) ACPISysFSComponent(move(table_name), paddr, table_size));
}

ErrorOr<size_t> ACPISysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> ACPISysFSComponent::try_to_generate_buffer() const
{
    auto acpi_blob = TRY(Memory::map_typed<u8>((m_paddr), m_length));
    return KBuffer::try_create_with_bytes("ACPISysFSComponent: Blob"sv, Span<u8> { acpi_blob.ptr(), m_length });
}

UNMAP_AFTER_INIT ACPISysFSComponent::ACPISysFSComponent(NonnullOwnPtr<KString> table_name, PhysicalAddress paddr, size_t table_size)
    : SysFSComponent()
    , m_paddr(paddr)
    , m_length(table_size)
    , m_table_name(move(table_name))
{
}

UNMAP_AFTER_INIT void ACPISysFSDirectory::find_tables_and_register_them_as_components()
{
    size_t ssdt_count = 0;
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        ACPI::Parser::the()->enumerate_static_tables([&](StringView signature, PhysicalAddress p_table, size_t length) {
            if (signature == "SSDT") {
                auto component_name = KString::formatted("{:4s}{}", signature.characters_without_null_termination(), ssdt_count).release_value_but_fixme_should_propagate_errors();
                list.append(ACPISysFSComponent::create(component_name->view(), p_table, length));
                ssdt_count++;
                return;
            }
            list.append(ACPISysFSComponent::create(signature, p_table, length));
        });
        return {};
    }));

    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        auto rsdp = Memory::map_typed<Structures::RSDPDescriptor20>(ACPI::Parser::the()->rsdp()).release_value_but_fixme_should_propagate_errors();
        list.append(ACPISysFSComponent::create("RSDP"sv, ACPI::Parser::the()->rsdp(), rsdp->base.revision == 0 ? sizeof(Structures::RSDPDescriptor) : rsdp->length));
        auto main_system_description_table = Memory::map_typed<Structures::SDTHeader>(ACPI::Parser::the()->main_system_description_table()).release_value_but_fixme_should_propagate_errors();
        if (ACPI::Parser::the()->is_xsdt_supported()) {
            list.append(ACPISysFSComponent::create("XSDT"sv, ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
        } else {
            list.append(ACPISysFSComponent::create("RSDT"sv, ACPI::Parser::the()->main_system_description_table(), main_system_description_table->length));
        }
        return {};
    }));
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ACPISysFSDirectory> ACPISysFSDirectory::must_create(SysFSFirmwareDirectory& firmware_directory)
{
    auto acpi_directory = MUST(adopt_nonnull_lock_ref_or_enomem(new (nothrow) ACPISysFSDirectory(firmware_directory)));
    acpi_directory->find_tables_and_register_them_as_components();
    return acpi_directory;
}

UNMAP_AFTER_INIT ACPISysFSDirectory::ACPISysFSDirectory(SysFSFirmwareDirectory& firmware_directory)
    : SysFSDirectory(firmware_directory)
{
}

void Parser::enumerate_static_tables(Function<void(StringView, PhysicalAddress, size_t)> callback)
{
    for (auto& p_table : m_sdt_pointers) {
        auto table = Memory::map_typed<Structures::SDTHeader>(p_table).release_value_but_fixme_should_propagate_errors();
        callback({ table->sig, 4 }, p_table, table->length);
    }
}

static bool validate_table(Structures::SDTHeader const&, size_t length);

UNMAP_AFTER_INIT void Parser::locate_static_data()
{
    locate_main_system_description_table();
    initialize_main_system_description_table();
    process_fadt_data();
    process_dsdt();
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> Parser::find_table(StringView signature)
{
    dbgln_if(ACPI_DEBUG, "ACPI: Calling Find Table method!");
    for (auto p_sdt : m_sdt_pointers) {
        auto sdt_or_error = Memory::map_typed<Structures::SDTHeader>(p_sdt);
        if (sdt_or_error.is_error()) {
            dbgln_if(ACPI_DEBUG, "ACPI: Failed mapping Table @ {}", p_sdt);
            continue;
        }
        dbgln_if(ACPI_DEBUG, "ACPI: Examining Table @ {}", p_sdt);
        if (!strncmp(sdt_or_error.value()->sig, signature.characters_without_null_termination(), 4)) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found Table @ {}", p_sdt);
            return p_sdt;
        }
    }
    return {};
}

bool Parser::handle_irq()
{
    TODO();
}

UNMAP_AFTER_INIT void Parser::enable_aml_parsing()
{
    // FIXME: When enabled, do other things to "parse AML".
    m_can_process_bytecode = true;
}

UNMAP_AFTER_INIT void Parser::process_fadt_data()
{
    dmesgln("ACPI: Initializing Fixed ACPI data");

    VERIFY(!m_fadt.is_null());
    dbgln_if(ACPI_DEBUG, "ACPI: FADT @ {}", m_fadt);

    auto sdt = Memory::map_typed<Structures::FADT>(m_fadt).release_value_but_fixme_should_propagate_errors();
    dmesgln("ACPI: Fixed ACPI data, Revision {}, length: {} bytes", (size_t)sdt->h.revision, (size_t)sdt->h.length);
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

UNMAP_AFTER_INIT void Parser::process_dsdt()
{
    auto sdt = Memory::map_typed<Structures::FADT>(m_fadt).release_value_but_fixme_should_propagate_errors();

    // Add DSDT-pointer to expose the full table in /sys/firmware/acpi/
    m_sdt_pointers.append(PhysicalAddress(sdt->dsdt_ptr));

    auto dsdt_or_error = Memory::map_typed<Structures::DSDT>(PhysicalAddress(sdt->dsdt_ptr));
    if (dsdt_or_error.is_error()) {
        dmesgln("ACPI: DSDT is unmappable");
        return;
    }
    dmesgln("ACPI: Using DSDT @ {} with {} bytes", PhysicalAddress(sdt->dsdt_ptr), dsdt_or_error.value()->h.length);
}

bool Parser::can_reboot()
{
    auto fadt_or_error = Memory::map_typed<Structures::FADT>(m_fadt);
    if (fadt_or_error.is_error())
        return false;
    if (fadt_or_error.value()->h.revision < 2)
        return false;
    return m_hardware_flags.reset_register_supported;
}

void Parser::access_generic_address(Structures::GenericAddressStructure const& structure, u32 value)
{
    switch ((GenericAddressStructure::AddressSpace)structure.address_space) {
    case GenericAddressStructure::AddressSpace::SystemIO: {
#if ARCH(X86_64)
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
#endif
        return;
    }
    case GenericAddressStructure::AddressSpace::SystemMemory: {
        dbgln("ACPI: Sending value {:x} to {}", value, PhysicalAddress(structure.address));
        switch ((GenericAddressStructure::AccessSize)structure.access_size) {
        case GenericAddressStructure::AccessSize::Byte:
            *Memory::map_typed<u8>(PhysicalAddress(structure.address)).release_value_but_fixme_should_propagate_errors() = value;
            break;
        case GenericAddressStructure::AccessSize::Word:
            *Memory::map_typed<u16>(PhysicalAddress(structure.address)).release_value_but_fixme_should_propagate_errors() = value;
            break;
        case GenericAddressStructure::AccessSize::DWord:
            *Memory::map_typed<u32>(PhysicalAddress(structure.address)).release_value_but_fixme_should_propagate_errors() = value;
            break;
        case GenericAddressStructure::AccessSize::QWord: {
            *Memory::map_typed<u64>(PhysicalAddress(structure.address)).release_value_but_fixme_should_propagate_errors() = value;
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
        return;
    }
    case GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
        // According to https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#address-space-format,
        // PCI addresses must be confined to devices on Segment group 0, bus 0.
        auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
        dbgln("ACPI: Sending value {:x} to {}", value, pci_address);
        u32 offset_in_pci_address = structure.address & 0xFFFF;
        if (structure.access_size == (u8)GenericAddressStructure::AccessSize::QWord) {
            dbgln("Trying to send QWord to PCI configuration space");
            VERIFY_NOT_REACHED();
        }
        VERIFY(structure.access_size != (u8)GenericAddressStructure::AccessSize::Undefined);
        auto& pci_device_identifier = PCI::get_device_identifier(pci_address);
        PCI::raw_access(pci_device_identifier, offset_in_pci_address, (1 << (structure.access_size - 1)), value);
        return;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

bool Parser::validate_reset_register(Memory::TypedMapping<Structures::FADT> const& fadt)
{
    // According to https://uefi.org/specs/ACPI/6.5/04_ACPI_Hardware_Specification.html#reset-register,
    // the reset register can only be located in I/O bus, PCI bus or memory-mapped.
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

    auto fadt_or_error = Memory::map_typed<Structures::FADT>(m_fadt);
    if (fadt_or_error.is_error()) {
        dmesgln("ACPI: Failed probing FADT {}", fadt_or_error.error());
        return;
    }
    auto fadt = fadt_or_error.release_value();
    VERIFY(validate_reset_register(fadt));
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
    return Memory::map_typed<Structures::SDTHeader>(table_header).release_value_but_fixme_should_propagate_errors()->length;
}

u8 Parser::get_table_revision(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
    dbgln_if(ACPI_DEBUG, "ACPI: Checking SDT Revision");
    return Memory::map_typed<Structures::SDTHeader>(table_header).release_value_but_fixme_should_propagate_errors()->revision;
}

UNMAP_AFTER_INIT void Parser::initialize_main_system_description_table()
{
    dbgln_if(ACPI_DEBUG, "ACPI: Checking Main SDT Length to choose the correct mapping size");
    VERIFY(!m_main_system_description_table.is_null());
    auto length = get_table_size(m_main_system_description_table);
    auto revision = get_table_revision(m_main_system_description_table);

    auto sdt = Memory::map_typed<Structures::SDTHeader>(m_main_system_description_table, length).release_value_but_fixme_should_propagate_errors();

    dmesgln("ACPI: Main Description Table valid? {}", validate_table(*sdt, length));

    if (m_xsdt_supported) {
        auto& xsdt = (Structures::XSDT const&)*sdt;
        dmesgln("ACPI: Using XSDT, enumerating tables @ {}", m_main_system_description_table);
        dmesgln("ACPI: XSDT revision {}, total length: {}", revision, length);
        dbgln_if(ACPI_DEBUG, "ACPI: XSDT pointer @ {}", VirtualAddress { &xsdt });
        for (u32 i = 0; i < ((length - sizeof(Structures::SDTHeader)) / sizeof(u64)); i++) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found new table [{0}], @ V{1:p} - P{1:p}", i, &xsdt.table_ptrs[i]);
            m_sdt_pointers.append(PhysicalAddress(xsdt.table_ptrs[i]));
        }
    } else {
        auto& rsdt = (Structures::RSDT const&)*sdt;
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
    auto rsdp = Memory::map_typed<Structures::RSDPDescriptor20>(m_rsdp).release_value_but_fixme_should_propagate_errors();
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

UNMAP_AFTER_INIT Parser::Parser(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number)
    : IRQHandler(irq_number)
    , m_rsdp(rsdp)
    , m_fadt(fadt)
{
    dmesgln("ACPI: Using RSDP @ {}", rsdp);
    locate_static_data();
}

static bool validate_table(Structures::SDTHeader const& v_header, size_t length)
{
    u8 checksum = 0;
    auto* sdt = (u8 const*)&v_header;
    for (size_t i = 0; i < length; i++)
        checksum += sdt[i];
    if (checksum == 0)
        return true;
    return false;
}

}
