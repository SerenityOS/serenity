/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Library/KString.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class Processor;

class ProcessorInfo {
public:
    ProcessorInfo(Processor const& processor);

    struct Cache {
        u64 size;
        u64 line_size;
    };

    StringView vendor_id_string() const { return m_vendor_id_string->view(); }
    StringView hypervisor_vendor_id_string() const { return m_hypervisor_vendor_id_string->view(); }
    StringView brand_string() const { return m_brand_string->view(); }
    StringView features_string() const { return m_features_string->view(); }
    u32 display_model() const { return m_display_model; }
    u32 display_family() const { return m_display_family; }
    u32 stepping() const { return m_stepping; }
    u32 type() const { return m_type; }
    u32 apic_id() const { return m_apic_id; }
    Optional<Cache> const& l1_data_cache() const { return m_l1_data_cache; }
    Optional<Cache> const& l1_instruction_cache() const { return m_l1_instruction_cache; }
    Optional<Cache> const& l2_cache() const { return m_l2_cache; }
    Optional<Cache> const& l3_cache() const { return m_l3_cache; }

    void set_apic_id(u32 apic_id) { m_apic_id = apic_id; }

    static constexpr StringView s_amd_vendor_id = "AuthenticAMD"sv;
    static constexpr StringView s_intel_vendor_id = "GenuineIntel"sv;

private:
    static NonnullOwnPtr<KString> build_vendor_id_string();
    static NonnullOwnPtr<KString> build_hypervisor_vendor_id_string(Processor const&);
    static NonnullOwnPtr<KString> build_brand_string();
    static NonnullOwnPtr<KString> build_features_string(Processor const&);

    void populate_cache_sizes_amd();
    void populate_cache_sizes_intel();

    NonnullOwnPtr<KString> m_vendor_id_string;
    NonnullOwnPtr<KString> m_hypervisor_vendor_id_string;
    NonnullOwnPtr<KString> m_brand_string;
    NonnullOwnPtr<KString> m_features_string;
    u32 m_display_model { 0 };
    u32 m_display_family { 0 };
    u32 m_stepping { 0 };
    u32 m_type { 0 };
    u32 m_apic_id { 0 };

    Optional<Cache> m_l1_data_cache;
    Optional<Cache> m_l1_instruction_cache;
    Optional<Cache> m_l2_cache;
    Optional<Cache> m_l3_cache;
};

}
