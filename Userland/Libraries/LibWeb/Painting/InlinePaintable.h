/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class InlinePaintable final : public Paintable {
public:
    static NonnullRefPtr<InlinePaintable> create(Layout::InlineNode const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::InlineNode const& layout_node() const;
    auto const& box_model() const { return layout_node().box_model(); }

private:
    InlinePaintable(Layout::InlineNode const&);

    template<typename Callback>
    void for_each_fragment(Callback) const;
};

}
