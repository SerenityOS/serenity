/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Matrix4x4.h>
#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/CalculatedOr.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/CSS/TransformFunctions.h>

namespace Web::CSS {

using TransformValue = Variant<AngleOrCalculated, LengthPercentage, double>;

class Transformation {
public:
    Transformation(TransformFunction function, Vector<TransformValue>&& values);
    Gfx::FloatMatrix4x4 to_matrix(Painting::PaintableBox const&) const;

private:
    TransformFunction m_function;
    Vector<TransformValue> m_values;
};

}
