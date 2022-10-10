/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Matrix3x3.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>
#include <LibVideo/DecoderError.h>

namespace Video {

DecoderErrorOr<FloatMatrix3x3> get_conversion_matrix(ColorPrimaries input_primaries, ColorPrimaries output_primaries);

}
