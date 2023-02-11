/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/CheckBoxPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<CheckBoxPaintable> CheckBoxPaintable::create(Layout::CheckBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<CheckBoxPaintable>(layout_box);
}

CheckBoxPaintable::CheckBoxPaintable(Layout::CheckBox const& layout_box)
    : LabelablePaintable(layout_box)
{
}

Layout::CheckBox const& CheckBoxPaintable::layout_box() const
{
    return static_cast<Layout::CheckBox const&>(layout_node());
}

Layout::CheckBox& CheckBoxPaintable::layout_box()
{
    return static_cast<Layout::CheckBox&>(layout_node());
}

}
