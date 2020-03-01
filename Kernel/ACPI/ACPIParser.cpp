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

namespace Kernel {
namespace ACPI {
    static Parser* s_acpi_parser;

    Parser& Parser::the()
    {
        ASSERT(s_acpi_parser != nullptr);
        return *s_acpi_parser;
    }

    void Parser::initialize_limited()
    {
        if (!Parser::is_initialized()) {
            s_acpi_parser = new Parser(false);
        }
    }

    bool Parser::is_initialized()
    {
        return (s_acpi_parser != nullptr);
    }

    Parser::Parser(bool usable)
    {
        if (usable) {
            klog() << "ACPI: Setting up a functional parser";
        } else {
            klog() << "ACPI: Limited Initialization. Vital functions are disabled by a request";
        }
        s_acpi_parser = this;
    }

    PhysicalAddress Parser::find_table(const char*)
    {
        klog() << "ACPI: Requested to search for a table, Abort!";
        return {};
    }

    void Parser::do_acpi_reboot()
    {
        klog() << "ACPI: Cannot invoke reboot!";
        ASSERT_NOT_REACHED();
    }

    void Parser::do_acpi_shutdown()
    {
        klog() << "ACPI: Cannot invoke shutdown!";
        ASSERT_NOT_REACHED();
    }

    void Parser::enable_aml_interpretation()
    {
        klog() << "ACPI: No AML Interpretation Allowed";
        ASSERT_NOT_REACHED();
    }
    void Parser::enable_aml_interpretation(File&)
    {
        klog() << "ACPI: No AML Interpretation Allowed";
        ASSERT_NOT_REACHED();
    }
    void Parser::enable_aml_interpretation(u8*, u32)
    {
        klog() << "ACPI: No AML Interpretation Allowed";
        ASSERT_NOT_REACHED();
    }
    void Parser::disable_aml_interpretation()
    {
        klog() << "ACPI Limited: No AML Interpretation Allowed";
        ASSERT_NOT_REACHED();
    }
    bool Parser::is_operable()
    {
        return false;
    }
}
}
