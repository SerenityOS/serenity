/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Error.h>
#include <LibGfx/Point.h>

namespace Gfx::JPEGXL {

// I.3.2 - Natural ordering of the DCT coefficients

// There are 13 Order ID and 3 color components.
using DCTOrderDescription = Array<Array<Span<Point<u32>>, 3>, 13>;

namespace DCTNaturalOrder {

ErrorOr<DCTOrderDescription const*> the();

};

}
