/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class Processor;

class ProcessorInfo {
    Processor& m_processor;
    String m_cpuid;
    String m_brandstr;
    String m_features;
    u32 m_display_model;
    u32 m_display_family;
    u32 m_stepping;
    u32 m_type;
    u32 m_apic_id;

public:
    ProcessorInfo(Processor& processor);

    const String& cpuid() const { return m_cpuid; }
    const String& brandstr() const { return m_brandstr; }
    const String& features() const { return m_features; }
    u32 display_model() const { return m_display_model; }
    u32 display_family() const { return m_display_family; }
    u32 stepping() const { return m_stepping; }
    u32 type() const { return m_type; }
    u32 apic_id() const { return m_apic_id; }

    void set_apic_id(u32 apic_id) { m_apic_id = apic_id; }
};

}
