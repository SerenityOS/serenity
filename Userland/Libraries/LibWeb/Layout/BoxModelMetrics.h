/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Size.h>
#include <LibWeb/PixelUnits.h>

namespace Web::Layout {

struct PixelBox {
    CSSPixels top { 0 };
    CSSPixels right { 0 };
    CSSPixels bottom { 0 };
    CSSPixels left { 0 };
};

struct BoxModelMetrics {
public:
    PixelBox margin;
    PixelBox padding;
    PixelBox border;
    PixelBox inset;

    PixelBox margin_box() const;
    PixelBox padding_box() const;
    PixelBox border_box() const;
};

}
