/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>

namespace GPU {

struct DeviceInfo final {
    ByteString vendor_name;
    ByteString device_name;
    unsigned num_texture_units;
    unsigned num_lights;
    unsigned max_clip_planes;
    unsigned max_texture_size;
    float max_texture_lod_bias;
    u8 stencil_bits;
    bool supports_npot_textures;
    bool supports_texture_clamp_to_edge;
    bool supports_texture_env_add;
};

}
