/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::Layout {

struct PixelBox {
    float top { 0 };
    float right { 0 };
    float bottom { 0 };
    float left { 0 };
};

struct BoxModelMetrics {
public:
    PixelBox margin;
    PixelBox padding;
    PixelBox border;
    PixelBox offset;

    PixelBox margin_box() const;
    PixelBox padding_box() const;
    PixelBox border_box() const;
};

}
