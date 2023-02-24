/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
    virtual ~TreeView() override = default;

    virtual void scroll_into_view(ModelIndex const&, bool scroll_horizontally, bool scroll_vertically) override;

    virtual int item_count() const override;
    virtual void toggle_index(ModelIndex const&) override;
    bool is_toggled(ModelIndex const& index);

    void expand_tree(ModelIndex const& root = {});
    void collapse_tree(ModelIndex const& root = {});

    void expand_all_parents_of(ModelIndex const&);

    Function<void(ModelIndex const&, bool const)> on_toggle;

    void set_should_fill_selected_rows(bool fill) { m_should_fill_selected_rows = fill; }
    bool should_fill_selected_rows() const { return m_should_fill_selected_rows; }

    virtual int vertical_padding() const override { return m_vertical_padding; }

    virtual Gfx::IntRect content_rect(ModelIndex const&) const override;
    virtual Gfx::IntRect paint_invalidation_rect(ModelIndex const& index) const override { return content_rect(index); }

    virtual int minimum_column_width(int column) override;

protected:
    TreeView();

    virtual void mousedown_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;

    virtual void did_update_selection() override;
    virtual void model_did_update(unsigned flags) override;
    virtual void move_cursor(CursorMovement, SelectionUpdate) override;

private:
    virtual ModelIndex index_at_event_position(Gfx::IntPoint, bool& is_toggle) const override;

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

    MetadataForIndex& ensure_metadata_for_index(ModelIndex const&) const;
    void set_open_state_of_all_in_subtree(ModelIndex const& root, bool open);

    mutable HashMap<ModelIndex, NonnullOwnPtr<MetadataForIndex>> m_view_metadata;

    RefPtr<Gfx::Bitmap> m_expand_bitmap;
    RefPtr<Gfx::Bitmap> m_collapse_bitmap;

    bool m_should_fill_selected_rows { false };
    int m_vertical_padding { 6 };
};

}
