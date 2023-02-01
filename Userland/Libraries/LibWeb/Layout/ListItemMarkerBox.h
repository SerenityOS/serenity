/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class ListItemMarkerBox final : public Box {
    JS_CELL(ListItemMarkerBox, Box);

public:
    explicit ListItemMarkerBox(DOM::Document&, CSS::ListStyleType, size_t index, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~ListItemMarkerBox() override;

    DeprecatedString const& text() const { return m_text; }

    virtual JS::GCPtr<Painting::Paintable> create_paintable() const override;

    CSS::ListStyleType list_style_type() const { return m_list_style_type; }

private:
    virtual bool is_list_item_marker_box() const final { return true; }
    virtual bool can_have_children() const override { return false; }

    CSS::ListStyleType m_list_style_type { CSS::ListStyleType::None };
    size_t m_index;

    DeprecatedString m_text {};
};

template<>
inline bool Node::fast_is<ListItemMarkerBox>() const { return is_list_item_marker_box(); }

}
