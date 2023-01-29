/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/x86_64/CPUID.h>
#include <Kernel/Arch/x86_64/ProcessorInfo.h>

namespace Kernel {

ProcessorInfo::ProcessorInfo(Processor const& processor)
    : m_vendor_id_string(build_vendor_id_string())
    , m_hypervisor_vendor_id_string(build_hypervisor_vendor_id_string(processor))
    , m_brand_string(build_brand_string())
    , m_features_string(build_features_string(processor))
{
    CPUID cpuid(1);
    m_stepping = cpuid.eax() & 0xf;
    u32 model = (cpuid.eax() >> 4) & 0xf;
    u32 family = (cpuid.eax() >> 8) & 0xf;
    m_type = (cpuid.eax() >> 12) & 0x3;
    u32 extended_model = (cpuid.eax() >> 16) & 0xf;
    u32 extended_family = (cpuid.eax() >> 20) & 0xff;
    if (family == 15) {
        m_display_family = family + extended_family;
        m_display_model = model + (extended_model << 4);
    } else if (family == 6) {
        m_display_family = family;
        m_display_model = model + (extended_model << 4);
    } else {
        m_display_family = family;
        m_display_model = model;
    }

    // NOTE: Intel exposes detailed CPU's cache information in CPUID 04. On the
    // other hand, AMD uses CPUID's extended function set.
    if (m_vendor_id_string->view() == s_amd_vendor_id)
        populate_cache_sizes_amd();
    else if (m_vendor_id_string->view() == s_intel_vendor_id)
        populate_cache_sizes_intel();
}

static void emit_u32(StringBuilder& builder, u32 value)
{
    builder.appendff("{:c}{:c}{:c}{:c}",
        value & 0xff,
        (value >> 8) & 0xff,
        (value >> 16) & 0xff,
        (value >> 24) & 0xff);
}

NonnullOwnPtr<KString> ProcessorInfo::build_vendor_id_string()
{
    CPUID cpuid(0);
    StringBuilder builder;
    emit_u32(builder, cpuid.ebx());
    emit_u32(builder, cpuid.edx());
    emit_u32(builder, cpuid.ecx());
    // NOTE: This isn't necessarily fixed length and might have null terminators at the end.
    return KString::must_create(builder.string_view().trim("\0"sv, TrimMode::Right));
}

NonnullOwnPtr<KString> ProcessorInfo::build_hypervisor_vendor_id_string(Processor const& processor)
{
    if (!processor.has_feature(CPUFeature::HYPERVISOR))
        return KString::must_create({});

    CPUID cpuid(0x40000000);
    StringBuilder builder;
    emit_u32(builder, cpuid.ebx());
    emit_u32(builder, cpuid.ecx());
    emit_u32(builder, cpuid.edx());
    // NOTE: This isn't necessarily fixed length and might have null terminators at the end.
    return KString::must_create(builder.string_view().trim("\0"sv, TrimMode::Right));
}

NonnullOwnPtr<KString> ProcessorInfo::build_brand_string()
{
    u32 max_extended_leaf = CPUID(0x80000000).eax();

    if (max_extended_leaf < 0x80000004)
        return KString::must_create({});

    StringBuilder builder;
    auto append_brand_string_part_to_builder = [&](u32 i) {
        CPUID cpuid(0x80000002 + i);
        emit_u32(builder, cpuid.eax());
        emit_u32(builder, cpuid.ebx());
        emit_u32(builder, cpuid.ecx());
        emit_u32(builder, cpuid.edx());
    };
    append_brand_string_part_to_builder(0);
    append_brand_string_part_to_builder(1);
    append_brand_string_part_to_builder(2);
    // NOTE: This isn't necessarily fixed length and might have null terminators at the end.
    return KString::must_create(builder.string_view().trim("\0"sv, TrimMode::Right));
}

NonnullOwnPtr<KString> ProcessorInfo::build_features_string(Processor const& processor)
{
    StringBuilder builder;
    bool first = true;
    for (auto feature = CPUFeature::Type(1u); feature != CPUFeature::__End; feature <<= 1u) {
        if (processor.has_feature(feature)) {
            if (first)
                first = false;
            else
                MUST(builder.try_append(' '));
            MUST(builder.try_append(cpu_feature_to_name(feature)));
        }
    }
    return KString::must_create(builder.string_view());
}

void ProcessorInfo::populate_cache_sizes_amd()
{
    auto const max_extended_leaf = CPUID(0x80000000).eax();

    if (max_extended_leaf < 0x80000005)
        return;

    auto const l1_cache_info = CPUID(0x80000005);

    if (l1_cache_info.ecx() != 0) {
        m_l1_data_cache = Cache {
            .size = ((l1_cache_info.ecx() >> 24) & 0xff) * KiB,
            .line_size = l1_cache_info.ecx() & 0xff,
        };
    }

    if (l1_cache_info.edx() != 0) {
        m_l1_instruction_cache = Cache {
            .size = ((l1_cache_info.edx() >> 24) & 0xff) * KiB,
            .line_size = l1_cache_info.edx() & 0xff,
        };
    }

    if (max_extended_leaf < 0x80000006)
        return;

    auto const l2_l3_cache_info = CPUID(0x80000006);

    if (l2_l3_cache_info.ecx() != 0) {
        m_l2_cache = Cache {
            .size = ((l2_l3_cache_info.ecx() >> 16) & 0xffff) * KiB,
            .line_size = l2_l3_cache_info.ecx() & 0xff,
        };
    }

    if (l2_l3_cache_info.edx() != 0) {
        m_l3_cache = Cache {
            .size = (static_cast<u64>((l2_l3_cache_info.edx() >> 18)) & 0x3fff) * 512 * KiB,
            .line_size = l2_l3_cache_info.edx() & 0xff,
        };
    }
}

void ProcessorInfo::populate_cache_sizes_intel()
{
    auto const collect_cache_info = [](u32 ecx) {
        auto const cache_info = CPUID(0x04, ecx);
        auto const ways = ((cache_info.ebx() >> 22) & 0x3ff) + 1;
        auto const partitions = ((cache_info.ebx() >> 12) & 0x3ff) + 1;
        auto const line_size = (cache_info.ebx() & 0xfff) + 1;
        auto const sets = cache_info.ecx() + 1;

        return Cache {
            .size = ways * partitions * line_size * sets,
            .line_size = line_size
        };
    };

    // NOTE: Those ECX numbers are the one used on recent Intel CPUs, an algorithm
    //       also exists to retrieve them.
    m_l1_instruction_cache = collect_cache_info(0);
    m_l1_data_cache = collect_cache_info(1);
    m_l2_cache = collect_cache_info(2);
    m_l3_cache = collect_cache_info(3);
}

}
