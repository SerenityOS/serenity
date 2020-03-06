/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#pragma once

#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/VM/Region.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>
#include <LibBareMetal/Memory/VirtualAddress.h>

namespace Kernel {
namespace ACPI {
class Parser {
public:
    static Parser& the();

    static bool is_initialized();
    static void initialize_limited();
    virtual PhysicalAddress find_table(const char* sig);

    virtual void try_acpi_reboot();
    virtual bool can_reboot() { return false; }
    virtual void try_acpi_shutdown();
    virtual bool can_shutdown() { return false; }

    virtual void enable_aml_interpretation();
    virtual void enable_aml_interpretation(File&);
    virtual void enable_aml_interpretation(u8*, u32);
    virtual void disable_aml_interpretation();
    virtual bool is_operable();

protected:
    explicit Parser(bool usable);
    bool m_operable;
};
}
}
