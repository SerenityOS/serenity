/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/Name.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/DynamicParser.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/IO.h>
#include <Kernel/Sections.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT DynamicParser::DynamicParser(PhysicalAddress rsdp)
    : IRQHandler(9)
    , Parser(rsdp)
{
    dmesgln("ACPI: Dynamic Parsing Enabled, Can parse AML");
    build_namespace();
    register_interrupt_handler();
}

bool DynamicParser::handle_irq(const RegisterState&)
{
    // FIXME: Implement IRQ handling of ACPI signals!
    VERIFY_NOT_REACHED();
}

void DynamicParser::enable_aml_interpretation()
{
    // FIXME: Implement AML Interpretation
    VERIFY_NOT_REACHED();
}
void DynamicParser::enable_aml_interpretation(File&)
{
    // FIXME: Implement AML Interpretation
    VERIFY_NOT_REACHED();
}
void DynamicParser::enable_aml_interpretation(u8*, u32)
{
    // FIXME: Implement AML Interpretation
    VERIFY_NOT_REACHED();
}
void DynamicParser::disable_aml_interpretation()
{
    // FIXME: Implement AML Interpretation
    VERIFY_NOT_REACHED();
}
void DynamicParser::try_acpi_shutdown()
{
    auto fadt = Memory::map_typed<Structures::FADT>(m_fadt);
    // Note: We temporarily enable ACPI shutdown by enabling ACPI mode!

    // FIXME: On real hardware, we need to call the _PTS method,  and to set the
    // PM1b_CNT register with (SLP_TYPb | SLP_ENb).
    // Also, we may need to call the _GTS method on older machines,
    bool acpi_mode_enabled = false;
    for (auto timeout_elapsed_in_milliseconds = 0; timeout_elapsed_in_milliseconds < 2000; timeout_elapsed_in_milliseconds++) {
        IO::out16(fadt->smi_cmd, fadt->acpi_enable_value);
        if (IO::in16(fadt->PM1a_CNT_BLK) & 1) {
            acpi_mode_enabled = true;
            break;
        }
        IO::delay(1000);
    }
    VERIFY(acpi_mode_enabled);
    // Note: We read the first value which according to ACPI spec is SLP_TYPa in the S5 Package.
    // Note: For this register, SLP_ENa is in the 13th bit.
    IO::out16(fadt->PM1a_CNT_BLK, 1 << 13 | ((m_s5_package->element_at(0)->as_unsigned_integer() & 0x7) << 10));
}

void DynamicParser::enumerate_objects_in_a_scope(const ScopeBase& scope_base)
{
    scope_base.for_each_named_object([&](const NamedObject& named_object) {
        switch (named_object.type()) {
        case NamedObject::Type::Scope:
            enumerate_objects_in_a_scope(static_cast<const ACPI::Scope&>(named_object));
            return;
        case NamedObject::Type::Device:
            m_acpi_devices.append(static_cast<const ACPI::Device&>(named_object));
            return;
        case NamedObject::Type::Name:
            if (named_object.name_string().full_name() == "_S5_") {
                m_s5_package = static_cast<const Name&>(named_object).as_elements_package();
            }
            return;
        default:
            return;
        }
    });
}

void DynamicParser::build_namespace()
{
    Vector<PhysicalAddress> aml_table_addresses;
    auto fadt = Memory::map_typed<Structures::FADT>(m_fadt);
    aml_table_addresses.append(PhysicalAddress(fadt->dsdt_ptr));
    m_acpi_namespace_scope = GlobalScope::must_create(aml_table_addresses);
    dbgln("Global Namespace Named objects count {}", m_acpi_namespace_scope->named_objects_count_slow());
    enumerate_objects_in_a_scope(*m_acpi_namespace_scope);
}

}
