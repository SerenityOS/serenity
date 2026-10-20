/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
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
#include <LibACPI/Definitions.h>
#include <LibACPI/Table.h>

namespace Kernel::ACPI {

static Parser* s_acpi_parser;

Parser* Parser::the()
{
    return s_acpi_parser;
}

ErrorOr<void> Parser::initialize(PhysicalAddress rsdp)
{
    VERIFY(!s_acpi_parser);

    ArmedScopeGuard guard = [] {
        // Make sure ACPI::is_enabled returns false.
        delete s_acpi_parser;
        s_acpi_parser = nullptr;
    };

    s_acpi_parser = new (nothrow) Parser(rsdp);
    if (!s_acpi_parser)
        return ENOMEM;

    TRY(s_acpi_parser->locate_main_system_description_table());
    TRY(s_acpi_parser->initialize_main_system_description_table());

    guard.disarm();
    return {};
}

void Parser::enumerate_static_tables(Function<void(StringView, PhysicalAddress, size_t)> callback)
{
    for (auto& p_table : m_sdt_pointers) {
        auto table = Memory::map_typed<::ACPI::Structures::SDTHeader>(p_table).release_value_but_fixme_should_propagate_errors();
        callback({ table->sig, 4 }, p_table, table->length);
    }
}

UNMAP_AFTER_INIT ErrorOr<void> Parser::full_initialize()
{
    TRY(process_fadt_data());
    TRY(process_dsdt());

    return {};
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> Parser::find_table(StringView signature)
{
    dbgln_if(ACPI_DEBUG, "ACPI: Calling Find Table method!");
    for (auto p_sdt : m_sdt_pointers) {
        auto sdt_or_error = Memory::map_typed<::ACPI::Structures::SDTHeader>(p_sdt);
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

bool Parser::handle_interrupt()
{
    TODO();
}

UNMAP_AFTER_INIT void Parser::enable_aml_parsing()
{
    // FIXME: When enabled, do other things to "parse AML".
    m_can_process_bytecode = true;
}

UNMAP_AFTER_INIT ErrorOr<void> Parser::process_fadt_data()
{
    dmesgln("ACPI: Initializing Fixed ACPI data");

    auto maybe_fadt = find_table("FACP"sv);
    if (!maybe_fadt.has_value() || maybe_fadt->is_null()) {
        dbgln("ACPI: Failed to find the FADT table");
        return ENOENT;
    }
    m_fadt = maybe_fadt.release_value();

    dbgln_if(ACPI_DEBUG, "ACPI: FADT @ {}", m_fadt);

    auto sdt = TRY(Memory::map_typed<::ACPI::Structures::FADT>(m_fadt));
    dmesgln("ACPI: Fixed ACPI data, Revision {}, length: {} bytes", static_cast<size_t>(sdt->h.revision), static_cast<size_t>(sdt->h.length));
    m_x86_specific_flags.cmos_rtc_not_present = has_flag(sdt->ia_pc_boot_arch_flags, ::ACPI::FADTFlags::IA_PC_Flags::CMOS_RTC_Not_Present);

    // FIXME: QEMU doesn't report that we have an i8042 controller in these flags, even if it should (when FADT revision is 3),
    // Later on, we need to make sure that we enumerate the ACPI namespace (AML encoded), instead of just using this value.
    m_x86_specific_flags.keyboard_8042 = (sdt->h.revision <= 3) || has_flag(sdt->ia_pc_boot_arch_flags, ::ACPI::FADTFlags::IA_PC_Flags::PS2_8042);

    m_x86_specific_flags.legacy_devices = has_flag(sdt->ia_pc_boot_arch_flags, ::ACPI::FADTFlags::IA_PC_Flags::Legacy_Devices);
    m_x86_specific_flags.msi_not_supported = has_flag(sdt->ia_pc_boot_arch_flags, ::ACPI::FADTFlags::IA_PC_Flags::MSI_Not_Supported);
    m_x86_specific_flags.vga_not_present = has_flag(sdt->ia_pc_boot_arch_flags, ::ACPI::FADTFlags::IA_PC_Flags::VGA_Not_Present);

    m_hardware_flags.cpu_software_sleep = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::CPU_SW_SLP);
    m_hardware_flags.docking_capability = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::DCK_CAP);
    m_hardware_flags.fix_rtc = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::FIX_RTC);
    m_hardware_flags.force_apic_cluster_model = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::FORCE_APIC_CLUSTER_MODEL);
    m_hardware_flags.force_apic_physical_destination_mode = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::FORCE_APIC_PHYSICAL_DESTINATION_MODE);
    m_hardware_flags.hardware_reduced_acpi = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::HW_REDUCED_ACPI);
    m_hardware_flags.headless = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::HEADLESS);
    m_hardware_flags.low_power_s0_idle_capable = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::LOW_POWER_S0_IDLE_CAPABLE);
    m_hardware_flags.multiprocessor_c2 = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::P_LVL2_UP);
    m_hardware_flags.pci_express_wake = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::PCI_EXP_WAK);
    m_hardware_flags.power_button = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::PWR_BUTTON);
    m_hardware_flags.processor_c1 = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::PROC_C1);
    m_hardware_flags.remote_power_on_capable = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::REMOTE_POWER_ON_CAPABLE);
    m_hardware_flags.reset_register_supported = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::RESET_REG_SUPPORTED);
    m_hardware_flags.rtc_s4 = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::RTC_s4);
    m_hardware_flags.s4_rtc_status_valid = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::S4_RTC_STS_VALID);
    m_hardware_flags.sealed_case = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::SEALED_CASE);
    m_hardware_flags.sleep_button = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::SLP_BUTTON);
    m_hardware_flags.timer_value_extension = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::TMR_VAL_EXT);
    m_hardware_flags.use_platform_clock = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::USE_PLATFORM_CLOCK);
    m_hardware_flags.wbinvd = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::WBINVD);
    m_hardware_flags.wbinvd_flush = has_flag(sdt->flags, ::ACPI::FADTFlags::FeatureFlags::WBINVD_FLUSH);

    m_interrupt_handler = TRY(InterruptHandler::create(*this, sdt->sci_int));

    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> Parser::process_dsdt()
{
    auto sdt = TRY(Memory::map_typed<::ACPI::Structures::FADT>(m_fadt));

    // Add DSDT-pointer to expose the full table in /sys/firmware/acpi/
    TRY(m_sdt_pointers.try_append(PhysicalAddress(sdt->dsdt_ptr)));

    auto dsdt_or_error = Memory::map_typed<::ACPI::Structures::DSDT>(PhysicalAddress(sdt->dsdt_ptr));
    if (dsdt_or_error.is_error()) {
        dmesgln("ACPI: DSDT is unmappable");
        return {};
    }
    dmesgln("ACPI: Using DSDT @ {} with {} bytes", PhysicalAddress(sdt->dsdt_ptr), dsdt_or_error.value()->h.length);

    return {};
}

bool Parser::can_reboot()
{
    auto fadt_or_error = Memory::map_typed<::ACPI::Structures::FADT>(m_fadt);
    if (fadt_or_error.is_error())
        return false;
    if (fadt_or_error.value()->h.revision < 2)
        return false;
    return m_hardware_flags.reset_register_supported;
}

ErrorOr<void> Parser::access_generic_address(::ACPI::Structures::GenericAddressStructure const& structure, u32 value)
{
    switch ((::ACPI::GenericAddressStructure::AddressSpace)structure.address_space) {
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemIO: {
#if ARCH(X86_64)
        IOAddress address(structure.address);
        dbgln("ACPI: Sending value {:x} to {}", value, address);
        switch (structure.access_size) {
        case ::ACPI::GenericAddressStructure::AccessSize::QWord: {
            dbgln("Trying to send QWord to IO port");
            return EINVAL;
        }
        case ::ACPI::GenericAddressStructure::AccessSize::Undefined: {
            dbgln("ACPI Warning: Unknown access size {}", to_underlying(structure.access_size));
            if (structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::QWord || structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::Undefined) {
                return EINVAL;
            }

            dbgln("ACPI: Bit Width - {} bits", to_underlying(structure.bit_width));
            address.out(value, to_underlying(structure.bit_width));
            break;
        }
        default:
            address.out(value, (8 << (to_underlying(structure.access_size) - 1)));
            break;
        }
#endif
        return {};
    }
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory: {
        dbgln("ACPI: Sending value {:x} to {}", value, PhysicalAddress(structure.address));
        switch ((::ACPI::GenericAddressStructure::AccessSize)structure.access_size) {
        case ::ACPI::GenericAddressStructure::AccessSize::Byte:
            *TRY(Memory::map_typed<u8>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::Word:
            *TRY(Memory::map_typed<u16>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::DWord:
            *TRY(Memory::map_typed<u32>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::QWord: {
            *TRY(Memory::map_typed<u64>(PhysicalAddress(structure.address))) = value;
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
        return {};
    }
    case ::ACPI::GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
        // According to https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#address-space-format,
        // PCI addresses must be confined to devices on Segment group 0, bus 0.
        auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
        dbgln("ACPI: Sending value {:x} to {}", value, pci_address);

        u32 offset_in_pci_address = structure.address & 0xFFFF;
        if (structure.access_size == ::ACPI::GenericAddressStructure::AccessSize::QWord || structure.access_size == ::ACPI::GenericAddressStructure::AccessSize::Undefined) {
            dbgln("Trying to send QWord or Undefined to PCI configuration space");
            return EINVAL;
        }

        auto& pci_device_identifier = PCI::get_device_identifier(pci_address);
        PCI::raw_access(pci_device_identifier, offset_in_pci_address, (1 << (to_underlying(structure.access_size) - 1)), value);
        return {};
    }
    default:
        dbgln("ACPI: Tried to access an unsupported address space");
        return EINVAL;
    }

    VERIFY_NOT_REACHED();
}

bool Parser::validate_reset_register(Memory::TypedMapping<::ACPI::Structures::FADT> const& fadt)
{
    // According to https://uefi.org/specs/ACPI/6.5/04_ACPI_Hardware_Specification.html#reset-register,
    // the reset register can only be located in I/O bus, PCI bus or memory-mapped.
    return (fadt->reset_reg.address_space == ::ACPI::GenericAddressStructure::AddressSpace::PCIConfigurationSpace || fadt->reset_reg.address_space == ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory || fadt->reset_reg.address_space == ::ACPI::GenericAddressStructure::AddressSpace::SystemIO);
}

void Parser::try_acpi_reboot()
{
    InterruptDisabler disabler;
    if (!can_reboot()) {
        dmesgln("ACPI: Reboot not supported!");
        return;
    }
    dbgln_if(ACPI_DEBUG, "ACPI: Rebooting, probing FADT ({})", m_fadt);

    auto fadt_or_error = Memory::map_typed<::ACPI::Structures::FADT>(m_fadt);
    if (fadt_or_error.is_error()) {
        dmesgln("ACPI: Failed probing FADT {}", fadt_or_error.error());
        return;
    }
    auto fadt = fadt_or_error.release_value();

    VERIFY(validate_reset_register(fadt));
    if (auto maybe_error = access_generic_address(fadt->reset_reg, fadt->reset_value); maybe_error.is_error()) {
        dmesgln("ACPI: Failed to write FADT reset register: {}", maybe_error.release_error());
        return;
    }

    Processor::halt();
}

void Parser::try_acpi_shutdown()
{
    dmesgln("ACPI: Shutdown is not supported with the current configuration, aborting!");
}

ErrorOr<size_t> Parser::get_table_size(PhysicalAddress table_header)
{
    InterruptDisabler disabler;
    dbgln_if(ACPI_DEBUG, "ACPI: Checking SDT Length");
    auto mapped = TRY(Memory::map_typed<::ACPI::Structures::SDTHeader>(table_header));
    auto length = mapped->length;
    return length;
}

UNMAP_AFTER_INIT ErrorOr<void> Parser::initialize_main_system_description_table()
{
    dbgln_if(ACPI_DEBUG, "ACPI: Checking Main SDT Length to choose the correct mapping size");
    VERIFY(!m_main_system_description_table.is_null());
    auto length = TRY(get_table_size(m_main_system_description_table));

    auto sdt = TRY(Memory::map_typed<::ACPI::Structures::SDTHeader>(m_main_system_description_table, length));

    dmesgln("ACPI: Main Description Table valid? {}", ::ACPI::validate_table(*sdt));

    if (m_xsdt_supported) {
        auto const* xsdt = reinterpret_cast<::ACPI::Structures::XSDT const*>(sdt.ptr());
        dmesgln("ACPI: Using XSDT, enumerating tables @ {}", m_main_system_description_table);
        dmesgln("ACPI: XSDT revision {}, total length: {}", sdt->revision, length);
        dbgln_if(ACPI_DEBUG, "ACPI: XSDT pointer @ {}", VirtualAddress { xsdt });
        for (u32 i = 0; i < ((length - sizeof(::ACPI::Structures::SDTHeader)) / sizeof(u64)); i++) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found new table [{0}], @ V{1:p} - P{1:p}", i, &xsdt->table_ptrs[i]);
            TRY(m_sdt_pointers.try_append(PhysicalAddress(xsdt->table_ptrs[i])));
        }
    } else {
        auto const* rsdt = reinterpret_cast<::ACPI::Structures::RSDT const*>(sdt.ptr());
        dmesgln("ACPI: Using RSDT, enumerating tables @ {}", m_main_system_description_table);
        dmesgln("ACPI: RSDT revision {}, total length: {}", sdt->revision, length);
        dbgln_if(ACPI_DEBUG, "ACPI: RSDT pointer @ {}", VirtualAddress { rsdt });
        for (u32 i = 0; i < ((length - sizeof(::ACPI::Structures::SDTHeader)) / sizeof(u32)); i++) {
            dbgln_if(ACPI_DEBUG, "ACPI: Found new table [{0}], @ V{1:p} - P{1:p}", i, &rsdt->table_ptrs[i]);
            TRY(m_sdt_pointers.try_append(PhysicalAddress(rsdt->table_ptrs[i])));
        }
    }

    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> Parser::locate_main_system_description_table()
{
    auto rsdp = TRY(Memory::map_typed<::ACPI::Structures::RSDPDescriptor20>(m_rsdp));
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

    return {};
}

UNMAP_AFTER_INIT Parser::Parser(PhysicalAddress rsdp)
    : m_rsdp(rsdp)
{
    dmesgln("ACPI: Using RSDP @ {}", rsdp);
}

}
