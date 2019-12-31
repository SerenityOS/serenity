#include <Kernel/ACPI/ACPIDynamicParser.h>
#include <Kernel/ACPI/ACPIParser.h>

void ACPIDynamicParser::initialize(ACPI_RAW::RSDPDescriptor20& rsdp)
{
    if (!ACPIStaticParser::is_initialized()) {
        new ACPIDynamicParser(rsdp);
    }
}
void ACPIDynamicParser::initialize_without_rsdp()
{
    if (!ACPIStaticParser::is_initialized()) {
        new ACPIDynamicParser();
    }
}

ACPIDynamicParser::ACPIDynamicParser()
    : IRQHandler(9)
    , ACPIStaticParser()

{
    kprintf("ACPI: Dynamic Parsing Enabled, Can parse AML\n");
}
ACPIDynamicParser::ACPIDynamicParser(ACPI_RAW::RSDPDescriptor20& rsdp)
    : IRQHandler(9)
    , ACPIStaticParser(rsdp)
{
    kprintf("ACPI: Dynamic Parsing Enabled, Can parse AML\n");
}

void ACPIDynamicParser::handle_irq()
{
    // FIXME: Implement IRQ handling of ACPI signals!
    ASSERT_NOT_REACHED();
}

void ACPIDynamicParser::enable_aml_interpretation()
{
    // FIXME: Implement AML Interpretation
    ASSERT_NOT_REACHED();
}
void ACPIDynamicParser::enable_aml_interpretation(File&)
{
    // FIXME: Implement AML Interpretation
    ASSERT_NOT_REACHED();
}
void ACPIDynamicParser::enable_aml_interpretation(u8*, u32)
{
    // FIXME: Implement AML Interpretation
    ASSERT_NOT_REACHED();
}
void ACPIDynamicParser::disable_aml_interpretation()
{
    // FIXME: Implement AML Interpretation
    ASSERT_NOT_REACHED();
}
void ACPIDynamicParser::do_acpi_shutdown()
{
    // FIXME: Implement AML Interpretation to perform ACPI shutdown
    ASSERT_NOT_REACHED();
}

void ACPIDynamicParser::build_namespace()
{
    // FIXME: Implement AML Interpretation to build the ACPI namespace
    ASSERT_NOT_REACHED();
}