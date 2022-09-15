/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/BackdropFilter.h>
#include <LibWeb/Forward.h>

namespace Web::Painting {

void apply_filter_list(Gfx::Bitmap& target_bitmap, Layout::Node const& node, Span<CSS::FilterFunction const> filter_list);

void apply_backdrop_filter(PaintContext&, Layout::Node const&, Gfx::FloatRect const&, BorderRadiiData const&, CSS::BackdropFilter const&);

}
