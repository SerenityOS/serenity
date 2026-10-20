/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <LibACPI/Definitions.h>

namespace Kernel::ACPI {

class Parser final {
public:
    static Parser* the();

    static ErrorOr<void> initialize(PhysicalAddress rsdp);

    Optional<PhysicalAddress> find_table(StringView signature);
    ErrorOr<void> full_initialize();

    void try_acpi_reboot();
    bool can_reboot();
    void try_acpi_shutdown();
    bool can_shutdown() { return false; }

    void enable_aml_parsing();

    PhysicalAddress rsdp() const { return m_rsdp; }
    PhysicalAddress main_system_description_table() const { return m_main_system_description_table; }
    bool is_xsdt_supported() const { return m_xsdt_supported; }

    void enumerate_static_tables(Function<void(StringView, PhysicalAddress, size_t)>);

    bool have_8042() const
    {
        return m_x86_specific_flags.keyboard_8042;
    }

    ::ACPI::FADTFlags::HardwareFeatures const& hardware_features() const { return m_hardware_flags; }
    ::ACPI::FADTFlags::x86_Specific_Flags const& x86_specific_flags() const { return m_x86_specific_flags; }

    ~Parser() = default;

private:
    class InterruptHandler final : public IRQHandler {
    public:
        static ErrorOr<NonnullOwnPtr<InterruptHandler>> create(Parser& parser, InterruptNumber interrupt_number)
        {
            auto interrupt_handler = TRY(adopt_nonnull_own_or_enomem(new (nothrow) InterruptHandler(parser, interrupt_number)));
            // TODO: interrupt_handler->enable_irq();
            return interrupt_handler;
        }

    private:
        InterruptHandler(Parser& parser, InterruptNumber interrupt_number)
            : IRQHandler(interrupt_number)
            , m_parser(parser)
        {
        }

        virtual StringView purpose() const override { return "ACPI Parser Handler"sv; }
        virtual bool handle_irq() override
        {
            return m_parser.handle_interrupt();
        }

        Parser& m_parser;
    };

    Parser(PhysicalAddress rsdp);

    bool handle_interrupt();

    ErrorOr<void> locate_main_system_description_table();
    ErrorOr<void> initialize_main_system_description_table();
    ErrorOr<size_t> get_table_size(PhysicalAddress);
    ErrorOr<void> process_fadt_data();
    ErrorOr<void> process_dsdt();

    bool validate_reset_register(Memory::TypedMapping<::ACPI::Structures::FADT> const&);
    ErrorOr<void> access_generic_address(::ACPI::Structures::GenericAddressStructure const&, u32 value);

    OwnPtr<InterruptHandler> m_interrupt_handler;

    PhysicalAddress m_rsdp;
    PhysicalAddress m_main_system_description_table;

    Vector<PhysicalAddress> m_sdt_pointers;
    PhysicalAddress m_fadt;

    bool m_xsdt_supported { false };
    bool m_can_process_bytecode { false };
    ::ACPI::FADTFlags::HardwareFeatures m_hardware_flags;
    ::ACPI::FADTFlags::x86_Specific_Flags m_x86_specific_flags;
};

}
