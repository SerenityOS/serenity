/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/CompositingLayer.h>
#include <LibWeb/Selection/Selection.h>

namespace Web::Layout {

class Viewport final : public BlockContainer {
    JS_CELL(Viewport, BlockContainer);

public:
    explicit Viewport(DOM::Document&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Viewport() override;

    const DOM::Document& dom_node() const { return static_cast<const DOM::Document&>(*Node::dom_node()); }

    void paint_all_phases(PaintContext&);

    JS::GCPtr<Selection::Selection> selection() const;

    void build_stacking_context_tree_if_needed();
    void recompute_selection_states();

    void rebuild_compositing_layers();
    void build_compositing_layers_if_needed();

    void invalidate(DevicePixelRect rect);

private:
    void build_stacking_context_tree();
    void build_compositing_layers(Painting::StackingContext*);

    virtual bool is_viewport() const override { return true; }

    Vector<OwnPtr<Painting::CompositingLayer>> m_compositing_layers;
};

template<>
inline bool Node::fast_is<Viewport>() const { return is_viewport(); }

}
