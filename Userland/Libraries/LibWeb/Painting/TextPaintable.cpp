/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/TextPaintable.h>

namespace Web::Painting {

NonnullOwnPtr<TextPaintable> TextPaintable::create(Layout::TextNode const& layout_node)
{
    return adopt_own(*new TextPaintable(layout_node));
}

TextPaintable::TextPaintable(Layout::TextNode const& layout_node)
    : Paintable(layout_node)
{
}

}
