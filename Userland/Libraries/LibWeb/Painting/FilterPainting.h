/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/Filter.h>
#include <LibWeb/Forward.h>

namespace Web::Painting {

void apply_filter_list(Gfx::Bitmap& target_bitmap, ReadonlySpan<CSS::ResolvedFilter::FilterFunction> filter_list);

void apply_backdrop_filter(PaintContext&, CSSPixelRect const&, BorderRadiiData const&, CSS::ResolvedFilter const&);

}
