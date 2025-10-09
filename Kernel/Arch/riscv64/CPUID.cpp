/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/CPUID.h>

namespace Kernel {

CPUFeature::Type isa_extensions_property_to_cpu_features(::DeviceTree::Property isa_extensions)
{
    auto features = CPUFeature::Type(0u);

    isa_extensions.for_each_string([&features](StringView extension_name) -> IterationDecision {
#define __ENUMERATE_RISCV_EXTENSION(feature_name, name, _) \
    if (extension_name == #name) {                         \
        features |= CPUFeature::feature_name;              \
        return IterationDecision::Continue;                \
    }
        ENUMERATE_RISCV_EXTENSIONS(__ENUMERATE_RISCV_EXTENSION)
#undef __ENUMERATE_RISCV_EXTENSION

        return IterationDecision::Continue;
    });

    return features;
}

StringView cpu_feature_to_name(CPUFeature::Type const& feature)
{
#define __ENUMERATE_RISCV_EXTENSION(feature_name, _, _2) \
    if (feature == CPUFeature::feature_name)             \
        return #feature_name##sv;
    ENUMERATE_RISCV_EXTENSIONS(__ENUMERATE_RISCV_EXTENSION)
#undef __ENUMERATE_RISCV_EXTENSION

    VERIFY_NOT_REACHED();
}

}
