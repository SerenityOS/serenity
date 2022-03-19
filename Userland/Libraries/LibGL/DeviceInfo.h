/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

namespace GL {

struct DeviceInfo final {
    StringView vendor_name;
    StringView device_name;
    unsigned num_texture_units;
    unsigned num_lights;
    u8 stencil_bits;
    bool supports_npot_textures;
};

}
