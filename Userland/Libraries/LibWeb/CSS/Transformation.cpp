/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Transformation.h"
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::CSS {

Transformation::Transformation(TransformFunction function, Vector<TransformValue>&& values)
    : m_function(function)
    , m_values(move(values))
{
}

Gfx::FloatMatrix4x4 Transformation::to_matrix(Painting::PaintableBox const& paintable_box) const
{
    auto count = m_values.size();
    auto value = [&](size_t index, CSSPixels const& reference_length = 0) -> float {
        return m_values[index].visit(
            [&](CSS::LengthPercentage const& value) -> double {
                return value.resolved(paintable_box.layout_node(), reference_length).to_px(paintable_box.layout_node()).to_float();
            },
            [&](CSS::AngleOrCalculated const& value) {
                return AK::to_radians(value.resolved(paintable_box.layout_node()).to_degrees());
            },
            [](double value) {
                return value;
            });
    };

    auto reference_box = paintable_box.absolute_rect();
    auto width = reference_box.width();
    auto height = reference_box.height();

    switch (m_function) {
    case CSS::TransformFunction::Matrix:
        if (count == 6)
            return Gfx::FloatMatrix4x4(value(0), value(2), 0, value(4),
                value(1), value(3), 0, value(5),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Matrix3d:
        if (count == 16)
            return Gfx::FloatMatrix4x4(value(0), value(4), value(8), value(12),
                value(1), value(5), value(9), value(13),
                value(2), value(6), value(10), value(14),
                value(3), value(7), value(11), value(15));
        break;
    case CSS::TransformFunction::Translate:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, value(1, height),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Translate3d:
        return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
            0, 1, 0, value(1, height),
            0, 0, 1, value(2),
            0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, 1, 0, value(0, height),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateZ:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, value(0),
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Scale:
        if (count == 1)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, value(0), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, value(1), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, value(0), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::RotateX:
        if (count == 1)
            return Gfx::rotation_matrix({ 1.0f, 0.0f, 0.0f }, value(0));
        break;
    case CSS::TransformFunction::RotateY:
        if (count == 1)
            return Gfx::rotation_matrix({ 0.0f, 1.0f, 0.0f }, value(0));
        break;
    case CSS::TransformFunction::Rotate:
    case CSS::TransformFunction::RotateZ:
        if (count == 1)
            return Gfx::rotation_matrix({ 0.0f, 0.0f, 1.0f }, value(0));
        break;
    case CSS::TransformFunction::Skew:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                tanf(value(1)), 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::SkewX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::SkewY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                tanf(value(0)), 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    }
    dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Unhandled transformation function {} with {} arguments", to_string(m_function), m_values.size());
    return Gfx::FloatMatrix4x4::identity();
}

}
