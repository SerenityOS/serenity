/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Painting/TextPaintable.h>

namespace Web::Painting {

NonnullRefPtr<TextPaintable> TextPaintable::create(Layout::TextNode const& layout_node)
{
    return adopt_ref(*new TextPaintable(layout_node));
}

TextPaintable::TextPaintable(Layout::TextNode const& layout_node)
    : Paintable(layout_node)
{
}

}
