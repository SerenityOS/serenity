/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGL/DeviceInfo.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Enums.h>

namespace SoftGPU {

static constexpr GL::DeviceInfo device_info {
    .vendor_name = "SerenityOS"sv,
    .device_name = "SoftGPU"sv,
    .num_texture_units = NUM_SAMPLERS,
    .num_lights = NUM_LIGHTS,
    .stencil_bits = sizeof(StencilType) * 8,
    .supports_npot_textures = true,
};

}
