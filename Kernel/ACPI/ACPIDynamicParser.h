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

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/ACPI/ACPIStaticParser.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

class ACPIDynamicParser final : public IRQHandler
    , ACPIStaticParser {
public:
    static void initialize(ACPI_RAW::RSDPDescriptor20& rsdp);
    static void initialize_without_rsdp();

    virtual void enable_aml_interpretation() override;
    virtual void enable_aml_interpretation(File& dsdt_file) override;
    virtual void enable_aml_interpretation(u8* physical_dsdt, u32 dsdt_payload_legnth) override;
    virtual void disable_aml_interpretation() override;
    virtual void do_acpi_shutdown() override;

protected:
    ACPIDynamicParser();
    explicit ACPIDynamicParser(ACPI_RAW::RSDPDescriptor20&);

private:
    void build_namespace();
    // ^IRQHandler
    virtual void handle_irq() override;

    OwnPtr<Region> m_acpi_namespace;
};
