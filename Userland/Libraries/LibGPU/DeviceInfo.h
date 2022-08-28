/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace GPU {

struct DeviceInfo final {
    String vendor_name;
    String device_name;
    unsigned num_texture_units;
    unsigned num_lights;
    unsigned max_clip_planes;
    u8 stencil_bits;
    bool supports_npot_textures;
    bool supports_texture_env_add;
};

}
