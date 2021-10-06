/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class InitialContainingBlock final : public BlockContainer {
public:
    explicit InitialContainingBlock(DOM::Document&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~InitialContainingBlock() override;

    const DOM::Document& dom_node() const { return static_cast<const DOM::Document&>(*Node::dom_node()); }

    void paint_all_phases(PaintContext&);

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    const LayoutRange& selection() const { return m_selection; }
    void set_selection(const LayoutRange&);
    void set_selection_end(const LayoutPosition&);

    void build_stacking_context_tree();

    void recompute_selection_states();

private:
    virtual bool is_initial_containing_block_box() const override { return true; }

    LayoutRange m_selection;
};

template<>
inline bool Node::fast_is<InitialContainingBlock>() const { return is_initial_containing_block_box(); }

}
