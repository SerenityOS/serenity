/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/GlobalScope.h>
#include <Kernel/ACPI/Parser.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel::ACPI {

class DynamicParser final
    : public IRQHandler
    , public Parser {
    friend class Parser;

public:
    virtual void enable_aml_interpretation() override;
    virtual void enable_aml_interpretation(File& dsdt_file) override;
    virtual void enable_aml_interpretation(u8* physical_dsdt, u32 dsdt_payload_legnth) override;
    virtual void disable_aml_interpretation() override;
    virtual void try_acpi_shutdown() override;
    virtual bool can_shutdown() override { return true; }
    virtual StringView purpose() const override { return "ACPI Parser"; }

protected:
    explicit DynamicParser(PhysicalAddress rsdp);

private:
    void build_namespace();
    // ^IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    OwnPtr<Memory::Region> m_acpi_namespace;
    OwnPtr<GlobalScope> m_acpi_namespace_scope;
};

}
