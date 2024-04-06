/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Range.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Layout {

JS_DEFINE_ALLOCATOR(Viewport);

Viewport::Viewport(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockContainer(document, &document, move(style))
{
}

Viewport::~Viewport() = default;

JS::GCPtr<Painting::Paintable> Viewport::create_paintable() const
{
    return Painting::ViewportPaintable::create(*this);
}

}
