/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/KString.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class Processor;

class ProcessorInfo {
    NonnullOwnPtr<KString> m_cpuid;
    NonnullOwnPtr<KString> m_brand;
    NonnullOwnPtr<KString> m_features;
    u32 m_display_model { 0 };
    u32 m_display_family { 0 };
    u32 m_stepping { 0 };
    u32 m_type { 0 };
    u32 m_apic_id { 0 };

public:
    ProcessorInfo(Processor const& processor);

    static NonnullOwnPtr<KString> query_processor_cpuid();
    static NonnullOwnPtr<KString> query_processor_brand();

    StringView cpuid() const { return m_cpuid->view(); }
    StringView brand() const { return m_brand->view(); }
    StringView features() const { return m_features->view(); }
    u32 display_model() const { return m_display_model; }
    u32 display_family() const { return m_display_family; }
    u32 stepping() const { return m_stepping; }
    u32 type() const { return m_type; }
    u32 apic_id() const { return m_apic_id; }

    void set_apic_id(u32 apic_id) { m_apic_id = apic_id; }
};

}
