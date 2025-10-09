/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ArbitrarySizedEnum.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>
#include <Kernel/Arch/riscv64/Extensions.h>
#include <LibDeviceTree/DeviceTree.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

#define __ENUMERATE_RISCV_EXTENSION(name, _, cpu_feature_index) name = CPUFeature(1u) << cpu_feature_index,

// clang-format off
AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u256,
    ENUMERATE_RISCV_EXTENSIONS(__ENUMERATE_RISCV_EXTENSION)
    __End = CPUFeature(1u) << 255u)
// clang-format on

#undef __ENUMERATE_RISCV_EXTENSION

CPUFeature::Type isa_extensions_property_to_cpu_features(::DeviceTree::Property isa_extensions);
StringView cpu_feature_to_name(CPUFeature::Type const&);

}
