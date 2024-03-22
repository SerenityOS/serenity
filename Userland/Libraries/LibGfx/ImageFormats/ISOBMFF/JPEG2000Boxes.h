/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Boxes.h"

namespace Gfx::ISOBMFF {

struct JPEG2000HeaderBox final : public SuperBox {
    BOX_SUBTYPE(JPEG2000HeaderBox);
};

// I.5.3.1 Image Header box
struct JPEG2000ImageHeaderBox final : public Box {
    BOX_SUBTYPE(JPEG2000ImageHeaderBox);

    u32 height { 0 };
    u32 width { 0 };
    u16 num_components { 0 };
    u8 bits_per_component { 0 };
    u8 compression_type { 0 };
    u8 is_colorspace_unknown { 0 };
    u8 contains_intellectual_property_rights { 0 };
};

struct JPEG2000SignatureBox final : public Box {
    BOX_SUBTYPE(JPEG2000SignatureBox);

    u32 signature { 0 };
};

}
