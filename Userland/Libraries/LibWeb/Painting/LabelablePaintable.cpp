/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/LabelablePaintable.h>

namespace Web::Painting {

Layout::LabelableNode const& LabelablePaintable::layout_box() const
{
    return static_cast<Layout::LabelableNode const&>(PaintableBox::layout_box());
}

Layout::LabelableNode& LabelablePaintable::layout_box()
{
    return static_cast<Layout::LabelableNode&>(PaintableBox::layout_box());
}

LabelablePaintable::LabelablePaintable(Layout::LabelableNode const& layout_node)
    : PaintableBox(layout_node)
{
}

}
