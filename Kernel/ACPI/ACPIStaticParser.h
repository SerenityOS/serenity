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

#include <Kernel/ACPI/ACPIParser.h>
#include <AK/OwnPtr.h>

namespace Kernel {
namespace ACPI {

    class StaticParser : Parser {
    public:
        static void initialize(PhysicalAddress rsdp);
        static void initialize_without_rsdp();
        static bool is_initialized();

        virtual PhysicalAddress find_table(const char* sig) override;
        virtual void try_acpi_reboot() override;
        virtual bool can_reboot() override;
        virtual bool can_shutdown() override { return false; }
        virtual void try_acpi_shutdown() override;
        virtual bool is_operable() override { return m_operable; }

    protected:
        StaticParser();
        explicit StaticParser(PhysicalAddress);

    private:
        void locate_static_data();
        void locate_main_system_description_table();
        void initialize_main_system_description_table();
        size_t get_table_size(PhysicalAddress);
        u8 get_table_revision(PhysicalAddress);
        void init_fadt();
        void init_facs();

        PhysicalAddress m_rsdp;
        PhysicalAddress m_main_system_description_table;

        Vector<PhysicalAddress> m_sdt_pointers;
        PhysicalAddress m_fadt;
        PhysicalAddress m_facs;

        bool m_xsdt_supported;
    };
}
}
