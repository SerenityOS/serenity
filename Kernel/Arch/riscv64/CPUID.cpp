/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/CPUID.h>

namespace Kernel {

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
