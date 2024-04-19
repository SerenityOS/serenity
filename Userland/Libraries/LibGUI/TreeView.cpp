/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/EventReceiver.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TreeView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, TreeView)

namespace GUI {

struct TreeView::MetadataForIndex {
    bool open { false };
};

TreeView::MetadataForIndex& TreeView::ensure_metadata_for_index(ModelIndex const& index) const
{
    VERIFY(index.is_valid());
    auto it = m_view_metadata.find(index);
    if (it != m_view_metadata.end())
        return *it->value;
    auto new_metadata = make<MetadataForIndex>();
    auto& new_metadata_ref = *new_metadata;
    m_view_metadata.set(index, move(new_metadata));
    return new_metadata_ref;
}

TreeView::TreeView()
{
    REGISTER_BOOL_PROPERTY("should_fill_selected_rows", should_fill_selected_rows, set_should_fill_selected_rows);
    set_selection_behavior(SelectionBehavior::SelectItems);
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_column_headers_visible(false);
    m_expand_bitmap = Gfx::Bitmap::load_from_file("/res/icons/serenity/treeview-expand.png"sv).release_value_but_fixme_should_propagate_errors();
    m_collapse_bitmap = Gfx::Bitmap::load_from_file("/res/icons/serenity/treeview-collapse.png"sv).release_value_but_fixme_should_propagate_errors();
}

ModelIndex TreeView::index_at_event_position(Gfx::IntPoint a_position, bool& is_toggle) const
{
    auto position = a_position.translated(0, -column_header().height()).translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
    is_toggle = false;
    if (!model())
        return {};
    ModelIndex result;
    traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const& rect, Gfx::IntRect const& toggle_rect, int) {
        if (toggle_rect.contains(position)) {
            result = index;
            is_toggle = true;
            return IterationDecision::Break;
        }
        if (rect.contains_vertically(position.y())) {
            result = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

void TreeView::doubleclick_event(MouseEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);
    if (!index.is_valid())
        return;

    if (event.button() == MouseButton::Primary) {
        set_cursor(index, SelectionUpdate::Set);

        if (model.row_count(index))
            toggle_index(index);
        else
            activate(index);
    }
}

void TreeView::set_open_state_of_all_in_subtree(ModelIndex const& root, bool open)
{
    if (root.is_valid()) {
        ensure_metadata_for_index(root).open = open;
        if (model()->row_count(root)) {
            if (on_toggle)
                on_toggle(root, open);
        }
    }
    int row_count = model()->row_count(root);
    int column = model()->tree_column();
    for (int row = 0; row < row_count; ++row) {
        auto index = model()->index(row, column, root);
        set_open_state_of_all_in_subtree(index, open);
    }
}

void TreeView::expand_all_parents_of(ModelIndex const& index)
{
    if (!model())
        return;

    auto current = index;
    while (current.is_valid()) {
        ensure_metadata_for_index(current).open = true;
        if (on_toggle)
            on_toggle(current, true);
        current = current.parent();
    }
    update_column_sizes();
    update_content_size();
    update();
}

void TreeView::expand_tree(ModelIndex const& root)
{
    if (!model())
        return;
    set_open_state_of_all_in_subtree(root, true);
    update_column_sizes();
    update_content_size();
    update();
}

void TreeView::collapse_tree(ModelIndex const& root)
{
    if (!model())
        return;
    set_open_state_of_all_in_subtree(root, false);
    update_column_sizes();
    update_content_size();
    update();
}

void TreeView::toggle_index(ModelIndex const& index)
{
    VERIFY(model()->row_count(index));
    auto& metadata = ensure_metadata_for_index(index);
    metadata.open = !metadata.open;

    if (!metadata.open && index.is_parent_of(cursor_index()))
        set_cursor(index, SelectionUpdate::Set);

    if (on_toggle)
        on_toggle(index, metadata.open);
    update_column_sizes();
    update_content_size();
    update();
}

bool TreeView::is_toggled(ModelIndex const& index)
{
    if (model()->row_count(index) == 0) {
        if (model()->parent_index(index).is_valid())
            return is_toggled(model()->parent_index(index));
        return false;
    }

    auto& metadata = ensure_metadata_for_index(index);
    return metadata.open;
}

template<typename Callback>
void TreeView::traverse_in_paint_order(Callback callback) const
{
    VERIFY(model());
    auto& model = *this->model();
    auto tree_column = model.tree_column();
    int indent_level = 1;
    int y_offset = 0;
    int tree_column_x_offset = this->tree_column_x_offset();

    Function<IterationDecision(ModelIndex const&)> traverse_index = [&](ModelIndex const& index) {
        int row_count_at_index = model.row_count(index);
        if (index.is_valid()) {
            auto& metadata = ensure_metadata_for_index(index);
            int x_offset = tree_column_x_offset + horizontal_padding() + indent_level * indent_width_in_pixels();
            auto node_text = index.data().to_byte_string();
            Gfx::IntRect rect = {
                x_offset, y_offset,
                static_cast<int>(ceilf(icon_size() + icon_spacing() + text_padding() + font_for_index(index)->width(node_text) + text_padding())), row_height()
            };
            Gfx::IntRect toggle_rect;
            if (row_count_at_index > 0) {
                int toggle_x = tree_column_x_offset + horizontal_padding() + (indent_width_in_pixels() * indent_level) - (icon_size() / 2) - 4;
                toggle_rect = { toggle_x, rect.y(), toggle_size(), toggle_size() };
                toggle_rect.center_vertically_within(rect);
            }
            if (callback(index, rect, toggle_rect, indent_level) == IterationDecision::Break)
                return IterationDecision::Break;
            y_offset += row_height();
            // NOTE: Skip traversing children if this index is closed!
            if (!metadata.open)
                return IterationDecision::Continue;
        }

        if (indent_level > 0 && !index.is_valid())
            return IterationDecision::Continue;

        ++indent_level;
        int row_count = model.row_count(index);
        for (int i = 0; i < row_count; ++i) {
            if (traverse_index(model.index(i, tree_column, index)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        --indent_level;
        return IterationDecision::Continue;
    };
    int root_count = model.row_count();
    for (int root_index = 0; root_index < root_count; ++root_index) {
        if (traverse_index(model.index(root_index, tree_column, ModelIndex())) == IterationDecision::Break)
            break;
    }
}

void TreeView::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);
    Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    if (fill_with_background_color())
        painter.fill_rect(event.rect(), palette().color(background_role()));

    if (!model())
        return;
    auto& model = *this->model();

    painter.translate(frame_inner_rect().location());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto visible_content_rect = this->visible_content_rect();
    int tree_column = model.tree_column();
    int column_count = model.column_count();
    int tree_column_x_offset = this->tree_column_x_offset();

    int y_offset = column_header().height();

    int painted_row_index = 0;

    traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const& a_rect, Gfx::IntRect const& a_toggle_rect, int indent_level) {
        if (!a_rect.intersects_vertically(visible_content_rect))
            return IterationDecision::Continue;

        auto rect = a_rect.translated(0, y_offset);
        auto toggle_rect = a_toggle_rect.translated(0, y_offset);

        if constexpr (ITEM_RECTS_DEBUG)
            painter.fill_rect(rect, Color::WarmGray);

        bool is_selected_row = selection().contains(index);

        Color text_color = palette().color(foreground_role());
        if (is_selected_row && should_fill_selected_rows())
            text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();

        Color background_color;
        if (is_selected_row) {
            background_color = is_focused() ? palette().selection() : palette().inactive_selection();
        } else {
            if (alternating_row_colors() && (painted_row_index % 2)) {
                background_color = Color(220, 220, 220);
            } else {
                background_color = palette().color(background_role());
            }
        }

        int row_width = 0;
        for (int column_index = 0; column_index < column_count; ++column_index) {
            if (!column_header().is_section_visible(column_index))
                continue;
            row_width += this->column_width(column_index) + horizontal_padding() * 2;
        }
        if (frame_inner_rect().width() > row_width) {
            row_width = frame_inner_rect().width();
        }

        Gfx::IntRect row_rect { 0, rect.y(), row_width, rect.height() };

        if (!is_selected_row || should_fill_selected_rows())
            painter.fill_rect(row_rect, background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < column_count; ++column_index) {
            if (!column_header().is_section_visible(column_index))
                continue;
            int column_width = this->column_width(column_index);

            painter.draw_rect(toggle_rect, text_color);

            if (column_index != tree_column) {
                Gfx::IntRect cell_rect(horizontal_padding() + x_offset, rect.y(), column_width, row_height());
                auto cell_index = model.index(index.row(), column_index, index.parent());

                auto* delegate = column_painting_delegate(column_index);
                if (delegate && delegate->should_paint(cell_index)) {
                    delegate->paint(painter, cell_rect, palette(), cell_index);
                } else {
                    auto data = cell_index.data();

                    if (data.is_bitmap()) {
                        painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
                    } else if (data.is_icon()) {
                        if (auto bitmap = data.as_icon().bitmap_for_size(16)) {
                            auto opacity = cell_index.data(ModelRole::IconOpacity).as_float_or(1.0f);
                            painter.blit(cell_rect.location(), *bitmap, bitmap->rect(), opacity);
                        }
                    } else {
                        auto text_alignment = cell_index.data(ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterLeft);
                        draw_item_text(painter, cell_index, is_selected_row, cell_rect, data.to_byte_string(), font_for_index(cell_index), text_alignment, Gfx::TextElision::Right);
                    }
                }
            } else {
                // It's the tree column!
                int indent_width = indent_width_in_pixels() * indent_level;

                Gfx::IntRect icon_rect = { rect.x(), rect.y(), icon_size(), icon_size() };
                icon_rect.center_vertically_within(rect);
                Gfx::IntRect background_rect = {
                    icon_rect.right() + icon_spacing(), rect.y(),
                    min(rect.width(), column_width - indent_width) - icon_size() - icon_spacing(), rect.height()
                };
                Gfx::IntRect text_rect = background_rect.shrunken(text_padding() * 2, 0);

                painter.fill_rect(background_rect, background_color);

                auto icon = index.data(ModelRole::Icon);
                if (icon.is_icon()) {
                    if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size())) {
                        if (m_hovered_index.is_valid() && m_hovered_index.parent() == index.parent() && m_hovered_index.row() == index.row()) {
                            painter.blit_brightened(icon_rect.location(), *bitmap, bitmap->rect());
                        } else {
                            auto opacity = index.data(ModelRole::IconOpacity).as_float_or(1.0f);
                            painter.blit(icon_rect.location(), *bitmap, bitmap->rect(), opacity);
                        }
                    }
                }
                auto display_data = index.data();
                if (display_data.is_string() || display_data.is_u32() || display_data.is_i32() || display_data.is_u64() || display_data.is_i64() || display_data.is_bool() || display_data.is_float())
                    draw_item_text(painter, index, is_selected_row, text_rect, display_data.to_byte_string(), font_for_index(index), Gfx::TextAlignment::CenterLeft, Gfx::TextElision::Right);

                if (selection_behavior() == SelectionBehavior::SelectItems && is_focused() && index == cursor_index()) {
                    painter.draw_rect(background_rect, palette().color(background_role()));
                    painter.draw_focus_rect(background_rect, palette().focus_outline());
                }

                auto index_at_indent = index;
                for (int i = indent_level; i > 0; --i) {
                    auto parent_of_index_at_indent = index_at_indent.parent();
                    bool index_at_indent_is_last_in_parent = index_at_indent.row() == model.row_count(parent_of_index_at_indent) - 1;
                    Gfx::IntPoint a { tree_column_x_offset + horizontal_padding() + indent_width_in_pixels() * i - icon_size() / 2, rect.y() - 2 };
                    Gfx::IntPoint b { a.x(), a.y() + row_height() - 1 };
                    if (index_at_indent_is_last_in_parent)
                        b.set_y(rect.center().y());
                    if (!(i != indent_level && index_at_indent_is_last_in_parent))
                        painter.draw_line(a, b, Color::MidGray);

                    if (i == indent_level) {
                        Gfx::IntPoint c { a.x(), rect.center().y() };
                        Gfx::IntPoint d { c.x() + icon_size() / 2, c.y() };
                        painter.draw_line(c, d, Color::MidGray);
                    }
                    index_at_indent = parent_of_index_at_indent;
                }

                if (!toggle_rect.is_empty()) {
                    auto& metadata = ensure_metadata_for_index(index);
                    if (metadata.open)
                        painter.blit(toggle_rect.location(), *m_collapse_bitmap, m_collapse_bitmap->rect());
                    else
                        painter.blit(toggle_rect.location(), *m_expand_bitmap, m_expand_bitmap->rect());
                }

                if (has_pending_drop() && index == drop_candidate_index()) {
                    painter.draw_rect(rect, palette().selection(), true);
                }
            }
            x_offset += column_width + horizontal_padding() * 2;
        }

        if (selection_behavior() == SelectionBehavior::SelectRows && is_focused() && index == cursor_index()) {
            painter.draw_rect(row_rect, palette().color(background_role()));
            painter.draw_focus_rect(row_rect, palette().focus_outline());
        }

        return IterationDecision::Continue;
    });
}

void TreeView::scroll_into_view(ModelIndex const& a_index, bool, bool scroll_vertically)
{
    if (!a_index.is_valid())
        return;
    Gfx::IntRect found_rect;
    traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const& rect, Gfx::IntRect const&, int) {
        if (index == a_index) {
            found_rect = rect;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    AbstractScrollableWidget::scroll_into_view(found_rect, false, scroll_vertically);
}

void TreeView::model_did_update(unsigned flags)
{
    if (flags == Model::UpdateFlag::InvalidateAllIndices) {
        m_view_metadata.clear();
    }

    AbstractTableView::model_did_update(flags);
}

void TreeView::did_update_selection()
{
    AbstractView::did_update_selection();
    if (!model())
        return;
    auto index = selection().first();
    if (!index.is_valid())
        return;

    if (activates_on_selection())
        activate(index);
}

void TreeView::mousedown_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousedown_event(event);

    if (event.button() != MouseButton::Primary)
        return AbstractView::mousedown_event(event);

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);

    if (index.is_valid() && is_toggle && model()->row_count(index)) {
        if (event.alt()) {
            if (is_toggled(index)) {
                collapse_tree(index);
            } else {
                expand_tree(index);
            }
            return;
        }
        toggle_index(index);
        return;
    }

    AbstractView::mousedown_event(event);
}

void TreeView::keydown_event(KeyEvent& event)
{
    if (!model())
        return AbstractTableView::keydown_event(event);

    if (event.key() == KeyCode::Key_Space) {
        if (model()->row_count(cursor_index()))
            toggle_index(cursor_index());
        return;
    }

    auto open_tree_node = [&](bool open, MetadataForIndex& metadata) {
        if (on_toggle)
            on_toggle(cursor_index(), open);
        metadata.open = open;
        update_column_sizes();
        update_content_size();
        update();
    };

    if (event.key() == KeyCode::Key_Left) {
        if (cursor_index().is_valid() && model()->row_count(cursor_index())) {
            if (event.ctrl()) {
                collapse_tree(cursor_index());
                return;
            }

            auto& metadata = ensure_metadata_for_index(cursor_index());
            if (metadata.open) {
                open_tree_node(false, metadata);
                return;
            }
        }
        if (cursor_index().is_valid() && cursor_index().parent().is_valid()) {
            set_cursor(cursor_index().parent(), SelectionUpdate::Set);
            return;
        }
    }

    if (event.key() == KeyCode::Key_Right) {
        if (cursor_index().is_valid() && model()->row_count(cursor_index())) {
            if (event.ctrl()) {
                expand_tree(cursor_index());
                return;
            }

            auto& metadata = ensure_metadata_for_index(cursor_index());
            if (!metadata.open) {
                open_tree_node(true, metadata);
                return;
            }

            auto new_cursor = model()->index(0, model()->tree_column(), cursor_index());
            set_cursor(new_cursor, SelectionUpdate::Set);
            return;
        }
    }

    if (event.key() == KeyCode::Key_Return) {
        if (cursor_index().is_valid() && model()->row_count(cursor_index())) {
            toggle_index(cursor_index());
            return;
        }
    }

    AbstractTableView::keydown_event(event);
}

void TreeView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
{
    auto& model = *this->model();

    if (!cursor_index().is_valid())
        set_cursor(model.index(0, model.tree_column(), cursor_index()), SelectionUpdate::Set);

    auto find_last_index_in_tree = [&](ModelIndex const tree_index) -> ModelIndex {
        auto last_index = tree_index;
        size_t row_count = model.row_count(last_index);
        while (row_count > 0) {
            last_index = model.index(row_count - 1, model.tree_column(), last_index);

            if (last_index.is_valid()) {
                if (model.row_count(last_index) == 0)
                    break;
                auto& metadata = ensure_metadata_for_index(last_index);
                if (!metadata.open)
                    break;
            }

            row_count = model.row_count(last_index);
        }
        return last_index;
    };

    auto step_up = [&](ModelIndex const current_index) -> ModelIndex {
        // Traverse into parent index if we're at the top of our subtree
        if (current_index.row() == 0) {
            auto parent_index = current_index.parent();
            if (parent_index.is_valid())
                return parent_index;
            return current_index;
        }

        // If previous index is closed, move to it immediately
        auto previous_index = model.index(current_index.row() - 1, model.tree_column(), current_index.parent());
        if (model.row_count(previous_index) == 0)
            return previous_index;
        auto& metadata = ensure_metadata_for_index(previous_index);
        if (!metadata.open)
            return previous_index;

        // Return very last index inside of open previous index
        return find_last_index_in_tree(previous_index);
    };

    auto step_down = [&](ModelIndex const current_index) -> ModelIndex {
        if (!current_index.is_valid())
            return current_index;

        // Step in when node is open
        if (model.row_count(current_index) > 0) {
            auto& metadata = ensure_metadata_for_index(current_index);
            if (metadata.open)
                return model.index(0, model.tree_column(), current_index);
        }

        // Find the parent index in which we must step one down
        auto child_index = current_index;
        auto parent_index = child_index.parent();
        int row_count = model.row_count(parent_index);
        while (child_index.is_valid() && child_index.row() >= row_count - 1) {
            child_index = parent_index;
            parent_index = parent_index.parent();
            row_count = model.row_count(parent_index);
        }

        // Step one down
        if (!child_index.is_valid())
            return current_index;
        return model.index(child_index.row() + 1, child_index.column(), parent_index);
    };

    switch (movement) {
    case CursorMovement::Up: {
        auto new_index = step_up(cursor_index());
        if (new_index.is_valid())
            set_cursor(new_index, selection_update);
        break;
    }
    case CursorMovement::Down: {
        auto new_index = step_down(cursor_index());
        if (new_index.is_valid())
            set_cursor(new_index, selection_update);
        return;
    }
    case CursorMovement::Home: {
        ModelIndex first_index = model.index(0, model.tree_column(), ModelIndex());
        if (first_index.is_valid())
            set_cursor(first_index, selection_update);
        return;
    }
    case CursorMovement::End: {
        auto last_index = find_last_index_in_tree({});
        if (last_index.is_valid())
            set_cursor(last_index, selection_update);
        return;
    }
    case CursorMovement::PageUp: {
        int const items_per_page = visible_content_rect().height() / row_height();
        auto new_index = cursor_index();
        for (int step = 0; step < items_per_page; ++step)
            new_index = step_up(new_index);
        if (new_index.is_valid())
            set_cursor(new_index, selection_update);
        return;
    }
    case CursorMovement::PageDown: {
        int const items_per_page = visible_content_rect().height() / row_height();
        auto new_index = cursor_index();
        for (int step = 0; step < items_per_page; ++step)
            new_index = step_down(new_index);
        if (new_index.is_valid())
            set_cursor(new_index, selection_update);
        return;
    }
    case CursorMovement::Left:
    case CursorMovement::Right:
        // There is no left/right in a treeview, those keys expand/collapse items instead.
        break;
    }
}

int TreeView::item_count() const
{
    int count = 0;
    traverse_in_paint_order([&](ModelIndex const&, Gfx::IntRect const&, Gfx::IntRect const&, int) {
        ++count;
        return IterationDecision::Continue;
    });
    return count;
}

void TreeView::auto_resize_column(int column)
{
    if (!model())
        return;

    if (!column_header().is_section_visible(column))
        return;

    auto& model = *this->model();

    int header_width = column_header().font().width(model.column_name(column).release_value_but_fixme_should_propagate_errors());
    if (column == m_key_column && model.is_column_sortable(column))
        header_width += HeaderView::sorting_arrow_width + HeaderView::sorting_arrow_offset;
    int column_width = header_width;

    bool is_empty = true;
    traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const&, Gfx::IntRect const&, int indent_level) {
        auto cell_data = model.index(index.row(), column, index.parent()).data();
        int cell_width = 0;
        if (cell_data.is_icon()) {
            cell_width = cell_data.as_icon().bitmap_for_size(16)->width();
        } else if (cell_data.is_bitmap()) {
            cell_width = cell_data.as_bitmap().width();
        } else if (cell_data.is_valid()) {
            cell_width = font().width(cell_data.to_byte_string());
        }
        if (is_empty && cell_width > 0)
            is_empty = false;
        if (column == model.tree_column())
            cell_width += horizontal_padding() * 2 + indent_level * indent_width_in_pixels() + icon_size() / 2;
        column_width = max(column_width, cell_width);
        return IterationDecision::Continue;
    });

    auto default_column_width = column_header().default_section_size(column);
    if (is_empty && column_header().is_default_section_size_initialized(column))
        column_header().set_section_size(column, default_column_width);
    else
        column_header().set_section_size(column, column_width);
}

void TreeView::update_column_sizes()
{
    if (!model())
        return;

    auto& model = *this->model();
    int column_count = model.column_count();
    int tree_column = model.tree_column();

    for (int column = 0; column < column_count; ++column) {
        if (column == tree_column)
            continue;
        if (!column_header().is_section_visible(column))
            continue;
        int header_width = column_header().font().width(model.column_name(column).release_value_but_fixme_should_propagate_errors());
        if (column == m_key_column && model.is_column_sortable(column))
            header_width += HeaderView::sorting_arrow_width + HeaderView::sorting_arrow_offset;
        int column_width = header_width;
        traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const&, Gfx::IntRect const&, int) {
            auto cell_data = model.index(index.row(), column, index.parent()).data();
            int cell_width = 0;
            if (cell_data.is_icon()) {
                cell_width = cell_data.as_icon().bitmap_for_size(16)->width();
            } else if (cell_data.is_bitmap()) {
                cell_width = cell_data.as_bitmap().width();
            } else if (cell_data.is_valid()) {
                cell_width = font().width(cell_data.to_byte_string());
            }
            column_width = max(column_width, cell_width);
            return IterationDecision::Continue;
        });

        set_column_width(column, max(this->column_width(column), column_width));
    }

    int tree_column_header_width = column_header().font().width(model.column_name(tree_column).release_value_but_fixme_should_propagate_errors());
    if (tree_column == m_key_column && model.is_column_sortable(tree_column))
        tree_column_header_width += HeaderView::sorting_arrow_width + HeaderView::sorting_arrow_offset;
    int tree_column_width = tree_column_header_width;
    traverse_in_paint_order([&](ModelIndex const& index, Gfx::IntRect const&, Gfx::IntRect const&, int indent_level) {
        auto cell_data = model.index(index.row(), tree_column, index.parent()).data();
        int cell_width = 0;
        if (cell_data.is_valid()) {
            cell_width = font().width(cell_data.to_byte_string());
            cell_width += horizontal_padding() * 2 + indent_level * indent_width_in_pixels() + icon_size() / 2 + text_padding() * 2;
        }
        tree_column_width = max(tree_column_width, cell_width);
        return IterationDecision::Continue;
    });

    set_column_width(tree_column, tree_column_width);
}

int TreeView::tree_column_x_offset() const
{
    int tree_column = model()->tree_column();
    int offset = 0;
    for (int i = 0; i < tree_column; ++i) {
        if (column_header().is_section_visible(i)) {
            offset += column_width(i);
            offset += horizontal_padding() * 2;
        }
    }
    return offset;
}

Gfx::IntRect TreeView::content_rect(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};

    Gfx::IntRect found_rect;
    traverse_in_paint_order([&](ModelIndex const& current_index, Gfx::IntRect const& rect, Gfx::IntRect const&, int) {
        if (index == current_index) {
            found_rect = rect;
            found_rect.translate_by(0, column_header().height());
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_rect;
}

int TreeView::minimum_column_width(int column)
{
    if (column != model()->tree_column()) {
        return 2;
    }

    int maximum_indent_level = 1;

    traverse_in_paint_order([&](ModelIndex const&, Gfx::IntRect const&, Gfx::IntRect const&, int indent_level) {
        maximum_indent_level = max(maximum_indent_level, indent_level);
        return IterationDecision::Continue;
    });

    return indent_width_in_pixels() * maximum_indent_level + icon_size() + icon_spacing() + 2;
}

}
