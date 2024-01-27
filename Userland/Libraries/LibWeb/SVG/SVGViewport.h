/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/ViewBox.h>

namespace Web::SVG {

class SVGViewport {
public:
    virtual Optional<ViewBox> view_box() const = 0;
    virtual Optional<PreserveAspectRatio> preserve_aspect_ratio() const = 0;
    virtual ~SVGViewport() = default;
};

}
