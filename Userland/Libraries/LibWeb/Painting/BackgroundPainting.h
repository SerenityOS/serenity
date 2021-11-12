/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_background(PaintContext&, Layout::NodeWithStyleAndBoxModelMetrics const&, Gfx::IntRect const&, Color background_color, Vector<CSS::BackgroundLayerData> const*, BorderRadiusData const&);

}
