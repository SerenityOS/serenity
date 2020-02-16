/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/ACPI/ACPIDynamicParser.h>
#include <Kernel/ACPI/ACPIParser.h>

namespace Kernel {

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

}
