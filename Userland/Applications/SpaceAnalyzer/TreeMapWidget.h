/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tree.h"
#include <LibGUI/Frame.h>
#include <LibGfx/Rect.h>

namespace SpaceAnalyzer {

class TreeMapWidget final : public GUI::Frame {
    C_OBJECT(TreeMapWidget)

public:
    virtual ~TreeMapWidget() override = default;
    Function<void()> on_path_change;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;
    size_t path_size() const;
    TreeNode const* path_node(size_t n) const;
    size_t viewpoint() const;
    void set_viewpoint(size_t);
    void set_tree(RefPtr<Tree> tree);

private:
    TreeMapWidget() = default;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    bool rect_can_contain_label(Gfx::IntRect const& rect) const;

    enum class HasLabel {
        Yes,
        No
    };
    enum class IsRemainder {
        Yes,
        No
    };

    template<typename Function>
    void lay_out_children(TreeNode const&, Gfx::IntRect const&, int depth, Function);
    void paint_cell_frame(GUI::Painter&, TreeNode const&, Gfx::IntRect const&, Gfx::IntRect const&, int depth, HasLabel has_label) const;
    Vector<int> path_to_position(Gfx::IntPoint);

    RefPtr<Tree> m_tree;
    Vector<int> m_path;
    size_t m_viewpoint { 0 };
    void const* m_selected_node_cache;
};

}
