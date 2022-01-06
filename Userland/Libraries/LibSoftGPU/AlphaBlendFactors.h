/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector4.h>

namespace SoftGPU {

struct AlphaBlendFactors final {
    FloatVector4 src_constant {};
    float src_factor_src_alpha = 0;
    float src_factor_dst_alpha = 0;
    float src_factor_src_color = 0;
    float src_factor_dst_color = 0;

    FloatVector4 dst_constant {};
    float dst_factor_src_alpha = 0;
    float dst_factor_dst_alpha = 0;
    float dst_factor_src_color = 0;
    float dst_factor_dst_color = 0;
};

}
