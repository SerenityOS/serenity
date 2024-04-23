/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_background(PaintContext&, Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelRect const&, Color background_color, CSS::ImageRendering, Vector<CSS::BackgroundLayerData> const*, BorderRadiiData const&);

}
