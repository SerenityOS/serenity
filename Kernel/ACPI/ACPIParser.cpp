#include <Kernel/ACPI/ACPIParser.h>

static ACPIParser* s_acpi_parser;

ACPIParser& ACPIParser::the()
{
    ASSERT(s_acpi_parser != nullptr);
    return *s_acpi_parser;
}

void ACPIParser::initialize_limited()
{
    if (!ACPIParser::is_initialized()) {
        s_acpi_parser = new ACPIParser(false);
    }
}

bool ACPIParser::is_initialized()
{
    return (s_acpi_parser != nullptr);
}

ACPIParser::ACPIParser(bool usable)
{
    if (usable) {
        kprintf("ACPI: Setting up a functional parser\n");
    } else {
        kprintf("ACPI: Limited Initialization. Vital functions are disabled by a request\n");
    }
    s_acpi_parser = this;
}

ACPI_RAW::SDTHeader* ACPIParser::find_table(const char*)
{
    kprintf("ACPI: Requested to search for a table, Abort!\n");
    return nullptr;
}

void ACPIParser::mmap(VirtualAddress, PhysicalAddress, u32)
{
    ASSERT_NOT_REACHED();
}

void ACPIParser::mmap_region(Region&, PhysicalAddress)
{
    ASSERT_NOT_REACHED();
}

void ACPIParser::do_acpi_reboot()
{
    kprintf("ACPI: Cannot invoke reboot!\n");
    ASSERT_NOT_REACHED();
}

void ACPIParser::do_acpi_shutdown()
{
    kprintf("ACPI: Cannot invoke shutdown!\n");
    ASSERT_NOT_REACHED();
}

void ACPIParser::enable_aml_interpretation()
{
    kprintf("ACPI: No AML Interpretation Allowed\n");
    ASSERT_NOT_REACHED();
}
void ACPIParser::enable_aml_interpretation(File&)
{
    kprintf("ACPI: No AML Interpretation Allowed\n");
    ASSERT_NOT_REACHED();
}
void ACPIParser::enable_aml_interpretation(u8*, u32)
{
    kprintf("ACPI: No AML Interpretation Allowed\n");
    ASSERT_NOT_REACHED();
}
void ACPIParser::disable_aml_interpretation()
{
    kprintf("ACPI Limited: No AML Interpretation Allowed\n");
    ASSERT_NOT_REACHED();
}
bool ACPIParser::is_operable()
{
    return false;
}