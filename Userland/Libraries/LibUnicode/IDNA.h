/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Utf32View.h>

namespace Unicode::IDNA {

enum class MappingStatus : u8 {
    Valid,
    Ignored,
    Mapped,
    Deviation,
    Disallowed,
    DisallowedStd3Valid,
    DisallowedStd3Mapped,
};

enum class IDNA2008Status : u8 {
    NV8,
    XV8,
};

struct Mapping {
    MappingStatus status;
    IDNA2008Status idna_2008_status;
    Utf32View mapped_to;
};

}
