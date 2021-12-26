/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/HashMap.h>
#include <LibGUI/AbstractTableView.h>

namespace GUI {

class TreeView : public AbstractTableView {
    C_OBJECT(TreeView)
public:
    virtual ~TreeView() override;

    virtual void scroll_into_view(const ModelIndex&, bool scroll_horizontally, bool scroll_vertically) override;

    virtual int item_count() const override;
    virtual void toggle_index(const ModelIndex&) override;

    void expand_tree(const ModelIndex& root = {});
    void collapse_tree(const ModelIndex& root = {});

    void expand_all_parents_of(const ModelIndex&);

    Function<void(const ModelIndex&, const bool)> on_toggle;

protected:
    TreeView();

    virtual void paint_event(PaintEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;

    virtual void did_update_selection() override;
    virtual void did_update_model(unsigned flags) override;
    virtual void move_cursor(CursorMovement, SelectionUpdate) override;

private:
    virtual ModelIndex index_at_event_position(const Gfx::IntPoint&, bool& is_toggle) const override;

    int row_height() const { return 16; }
    int max_item_width() const { return frame_inner_rect().width(); }
    int indent_width_in_pixels() const { return 16; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int toggle_size() const { return 9; }
    int text_padding() const { return 2; }
    int tree_column_x_offset() const;
    virtual void update_column_sizes() override;

    template<typename Callback>
    void traverse_in_paint_order(Callback) const;

    struct MetadataForIndex;

    MetadataForIndex& ensure_metadata_for_index(const ModelIndex&) const;
    void set_open_state_of_all_in_subtree(const ModelIndex& root, bool open);

    mutable HashMap<void*, NonnullOwnPtr<MetadataForIndex>> m_view_metadata;

    RefPtr<Gfx::Bitmap> m_expand_bitmap;
    RefPtr<Gfx::Bitmap> m_collapse_bitmap;
};

}
