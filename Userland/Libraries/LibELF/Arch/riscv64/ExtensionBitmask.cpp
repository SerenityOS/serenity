/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/API/RISCVExtensionBitmask.h>
#include <LibELF/Arch/riscv64/ExtensionBitmask.h>
#include <sys/archctl.h>

namespace ELF {

void __get_riscv_feature_bits(void* feature_bits, void* cpu_model)
{
    // libgcc/compiler-rt will call this function to get info about the CPU for __init_riscv_feature_bits(void*).
    // RISCVFeatureBits::length is set to the length of the features array.

    auto feature_bits_ = static_cast<RISCVFeatureBits*>(feature_bits);

    archctl(ARCHCTL_RISCV64_GET_CPU_INFO, feature_bits_->length, feature_bits_->features, cpu_model);
}

}
