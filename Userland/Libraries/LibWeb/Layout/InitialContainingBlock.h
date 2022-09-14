/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/LayoutPosition.h>

namespace Web::Layout {

class InitialContainingBlock final : public BlockContainer {
public:
    explicit InitialContainingBlock(DOM::Document&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~InitialContainingBlock() override;

    const DOM::Document& dom_node() const { return static_cast<const DOM::Document&>(*Node::dom_node()); }

    void paint_all_phases(PaintContext&);

    LayoutRange const& selection() const { return m_selection; }
    void set_selection(LayoutRange const&);
    void set_selection_end(LayoutPosition const&);

    void build_stacking_context_tree_if_needed();
    void recompute_selection_states();

private:
    void build_stacking_context_tree();
    virtual bool is_initial_containing_block_box() const override { return true; }

    LayoutRange m_selection;
};

template<>
inline bool Node::fast_is<InitialContainingBlock>() const { return is_initial_containing_block_box(); }

}
