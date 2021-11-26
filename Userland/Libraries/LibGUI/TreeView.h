/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    void set_should_fill_selected_rows(bool fill) { m_should_fill_selected_rows = fill; }
    bool should_fill_selected_rows() const { return m_should_fill_selected_rows; }

    virtual int vertical_padding() const override { return m_vertical_padding; }

    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const& index) const override { return content_rect(index); }

    virtual int minimum_column_width(int column) override;

protected:
    TreeView();

    virtual void paint_event(PaintEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;

    virtual void did_update_selection() override;
    virtual void model_did_update(unsigned flags) override;
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
    virtual void auto_resize_column(int column) override;

    template<typename Callback>
    void traverse_in_paint_order(Callback) const;

    struct MetadataForIndex;

    MetadataForIndex& ensure_metadata_for_index(const ModelIndex&) const;
    void set_open_state_of_all_in_subtree(const ModelIndex& root, bool open);

    mutable HashMap<void*, NonnullOwnPtr<MetadataForIndex>> m_view_metadata;

    RefPtr<Gfx::Bitmap> m_expand_bitmap;
    RefPtr<Gfx::Bitmap> m_collapse_bitmap;

    bool m_should_fill_selected_rows { false };
    int m_vertical_padding { 6 };
};

}
