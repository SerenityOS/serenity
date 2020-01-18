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