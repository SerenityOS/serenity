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
#include <Kernel/ACPI/Initialize.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {
namespace ACPI {

class Parser {
public:
    static Parser* the();

    template<typename ParserType>
    static void initialize(PhysicalAddress rsdp)
    {
        set_the(*new ParserType(rsdp));
    }

    virtual PhysicalAddress find_table(const StringView& signature);

    virtual void try_acpi_reboot();
    virtual bool can_reboot();
    virtual void try_acpi_shutdown();
    virtual bool can_shutdown() { return false; }

    const FADTFlags::HardwareFeatures& hardware_features() const { return m_hardware_flags; }
    const FADTFlags::x86_Specific_Flags& x86_specific_flags() const { return m_x86_specific_flags; }

    virtual void enable_aml_interpretation();
    virtual void enable_aml_interpretation(File&);
    virtual void enable_aml_interpretation(u8*, u32);
    virtual void disable_aml_interpretation();

protected:
    explicit Parser(PhysicalAddress rsdp);

private:
    static void set_the(Parser&);

    void locate_static_data();
    void locate_main_system_description_table();
    void initialize_main_system_description_table();
    size_t get_table_size(PhysicalAddress);
    u8 get_table_revision(PhysicalAddress);
    void init_fadt();
    void init_facs();

    bool validate_reset_register();
    void access_generic_address(const Structures::GenericAddressStructure&, u32 value);

    PhysicalAddress m_rsdp;
    PhysicalAddress m_main_system_description_table;

    Vector<PhysicalAddress> m_sdt_pointers;
    PhysicalAddress m_fadt;
    PhysicalAddress m_facs;

    bool m_xsdt_supported { false };
    FADTFlags::HardwareFeatures m_hardware_flags;
    FADTFlags::x86_Specific_Flags m_x86_specific_flags;
};

}
}
