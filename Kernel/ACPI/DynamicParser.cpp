/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/DynamicParser.h>
#include <Kernel/ACPI/Parser.h>
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
    // FIXME: Implement AML Interpretation to perform ACPI shutdown
    VERIFY_NOT_REACHED();
}

void DynamicParser::build_namespace()
{
    Vector<PhysicalAddress> aml_table_addresses;
    auto fadt = Memory::map_typed<Structures::FADT>(m_fadt);
    aml_table_addresses.append(PhysicalAddress(fadt->dsdt_ptr));
    m_acpi_namespace_scope = GlobalScope::must_create(aml_table_addresses);
}

}
