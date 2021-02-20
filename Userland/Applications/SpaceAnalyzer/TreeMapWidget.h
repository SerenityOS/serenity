/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Rect.h>

namespace SpaceAnalyzer {

struct TreeMapNode {
    virtual String name() const = 0;
    virtual int64_t area() const = 0;
    virtual size_t num_children() const = 0;
    virtual const TreeMapNode& child_at(size_t i) const = 0;
    virtual void sort_children_by_area() const = 0;
};

struct TreeMap : public RefCounted<TreeMap> {
    virtual ~TreeMap() { }
    virtual const TreeMapNode& root() const = 0;
};

class TreeMapWidget final : public GUI::Frame {
    C_OBJECT(TreeMapWidget)

public:
    virtual ~TreeMapWidget() override;
    Function<void()> on_path_change;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;
    size_t path_size() const;
    const TreeMapNode* path_node(size_t n) const;
    size_t viewpoint() const;
    void set_viewpoint(size_t);
    void set_tree(RefPtr<TreeMap> tree);

private:
    TreeMapWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;

    bool rect_can_contain_children(const Gfx::IntRect& rect) const;
    bool rect_can_contain_label(const Gfx::IntRect& rect) const;
    Gfx::IntRect inner_rect_for_frame(const Gfx::IntRect& rect) const;

    enum class IsVisualLeaf {
        Yes,
        No
    };
    enum class IsRemainder {
        Yes,
        No
    };

    template<typename Function>
    void lay_out_children(const TreeMapNode&, const Gfx::IntRect&, int depth, Function);
    void paint_cell_frame(GUI::Painter&, const TreeMapNode&, const Gfx::IntRect&, int depth, bool fill) const;
    Vector<int> path_to_position(const Gfx::IntPoint&);

    RefPtr<TreeMap> m_tree;
    Vector<int> m_path;
    size_t m_viewpoint;
    const void* m_selected_node_cache;
};

}
