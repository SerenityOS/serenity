/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/CPUID.h>
#include <Kernel/Arch/riscv64/ProcessorInfo.h>
#include <Kernel/Arch/riscv64/SBI.h>

namespace Kernel {

ProcessorInfo::ProcessorInfo()
{
    if (!SBI::is_legacy()) {
        m_mvendorid = MUST(SBI::Base::get_mvendorid());
        m_marchid = MUST(SBI::Base::get_marchid());
        m_mimpid = MUST(SBI::Base::get_mimpid());
    }
}

void ProcessorInfo::build_isa_string(Processor const& processor)
{
    StringBuilder builder;

    MUST(builder.try_append("RV64"sv));

    bool first_multi_letter_extension = true;
    for (auto extension = CPUFeature::Type(1u); extension != CPUFeature::__End; extension <<= 1u) {
        if (processor.has_feature(extension)) {
            auto extension_name = cpu_feature_to_name(extension);
            if (extension_name.length() > 1) {
                if (first_multi_letter_extension)
                    first_multi_letter_extension = false;
                else
                    MUST(builder.try_append('_'));
            }
            MUST(builder.try_append(extension_name));
        }
    }

    m_isa_string = KString::must_create(builder.string_view());
}

}
