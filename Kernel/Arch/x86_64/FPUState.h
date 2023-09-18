/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86_64/SIMDState.h>

VALIDATE_IS_X86()

namespace Kernel {

struct [[gnu::aligned(64), gnu::packed]] FPUState {
    SIMD::LegacyRegion legacy_region;
    SIMD::Header xsave_header;

    // FIXME: This should be dynamically allocated! For now, we only save the `YMM` registers here,
    // so this will do for now. The size of the area is queried via CPUID(EAX=0dh, ECX=2):EAX.
    // https://www.intel.com/content/dam/develop/external/us/en/documents/36945
    u8 ext_save_area[256];
};

}
