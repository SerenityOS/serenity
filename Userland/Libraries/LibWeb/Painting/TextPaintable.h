/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class TextPaintable : public Paintable {
public:
    static NonnullOwnPtr<TextPaintable> create(Layout::TextNode const&);

    Layout::TextNode const& layout_node() const { return static_cast<Layout::TextNode const&>(Paintable::layout_node()); }

private:
    explicit TextPaintable(Layout::TextNode const&);
};

}
