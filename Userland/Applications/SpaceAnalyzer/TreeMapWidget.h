/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Rect.h>

namespace SpaceAnalyzer {

struct TreeMapNode {
    virtual String name() const = 0;
    virtual i64 area() const = 0;
    virtual size_t num_children() const = 0;
    virtual TreeMapNode const& child_at(size_t i) const = 0;
    virtual void sort_children_by_area() const = 0;

protected:
    virtual ~TreeMapNode() = default;
};

struct TreeMap : public RefCounted<TreeMap> {
    virtual ~TreeMap() = default;
    virtual TreeMapNode const& root() const = 0;
};

class TreeMapWidget final : public GUI::Frame {
    C_OBJECT(TreeMapWidget)

public:
    virtual ~TreeMapWidget() override = default;
    Function<void()> on_path_change;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;
    size_t path_size() const;
    TreeMapNode const* path_node(size_t n) const;
    size_t viewpoint() const;
    void set_viewpoint(size_t);
    void set_tree(RefPtr<TreeMap> tree);

private:
    TreeMapWidget() = default;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    bool rect_can_contain_children(Gfx::IntRect const& rect) const;
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
    void lay_out_children(TreeMapNode const&, Gfx::IntRect const&, int depth, Function);
    void paint_cell_frame(GUI::Painter&, TreeMapNode const&, Gfx::IntRect const&, Gfx::IntRect const&, int depth, HasLabel has_label) const;
    Vector<int> path_to_position(Gfx::IntPoint const&);

    RefPtr<TreeMap> m_tree;
    Vector<int> m_path;
    size_t m_viewpoint { 0 };
    void const* m_selected_node_cache;
};

}
