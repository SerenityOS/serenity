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

#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TreeView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>

//#define DEBUG_ITEM_RECTS

namespace GUI {

struct TreeView::MetadataForIndex {
    bool open { false };
};

TreeView::MetadataForIndex& TreeView::ensure_metadata_for_index(const ModelIndex& index) const
{
    ASSERT(index.is_valid());
    auto it = m_view_metadata.find(index.internal_data());
    if (it != m_view_metadata.end())
        return *it->value;
    auto new_metadata = make<MetadataForIndex>();
    auto& new_metadata_ref = *new_metadata;
    m_view_metadata.set(index.internal_data(), move(new_metadata));
    return new_metadata_ref;
}

TreeView::TreeView(Widget* parent)
    : AbstractTableView(parent)
{
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_size_columns_to_fit_content(true);
    set_headers_visible(false);
    m_expand_bitmap = Gfx::Bitmap::load_from_file("/res/icons/treeview-expand.png");
    m_collapse_bitmap = Gfx::Bitmap::load_from_file("/res/icons/treeview-collapse.png");
}

TreeView::~TreeView()
{
}

ModelIndex TreeView::index_at_event_position(const Gfx::Point& a_position, bool& is_toggle) const
{
    auto position = a_position.translated(0, -header_height()).translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
    is_toggle = false;
    if (!model())
        return {};
    ModelIndex result;
    traverse_in_paint_order([&](const ModelIndex& index, const Gfx::Rect& rect, const Gfx::Rect& toggle_rect, int) {
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

    if (event.button() == MouseButton::Left) {
        if (selection().first() != index)
            selection().set(index);

        if (model.row_count(index))
            toggle_index(index);
        else
            activate(index);
    }
}

void TreeView::toggle_index(const ModelIndex& index)
{
    ASSERT(model()->row_count(index));
    auto& metadata = ensure_metadata_for_index(index);
    metadata.open = !metadata.open;
    update_column_sizes();
    update_content_size();
    update();
}

template<typename Callback>
void TreeView::traverse_in_paint_order(Callback callback) const
{
    ASSERT(model());
    auto& model = *this->model();
    int indent_level = 1;
    int y_offset = 0;
    int tree_column = model.tree_column();
    int tree_column_x_offset = 0;

    for (int i = 0; i < tree_column; ++i) {
        if (!is_column_hidden(i))
            tree_column_x_offset += column_width(i);
    }

    Function<IterationDecision(const ModelIndex&)> traverse_index = [&](const ModelIndex& index) {
        int row_count_at_index = model.row_count(index);
        if (index.is_valid()) {
            auto& metadata = ensure_metadata_for_index(index);
            int x_offset = tree_column_x_offset + horizontal_padding() + indent_level * indent_width_in_pixels();
            auto node_text = model.data(index, Model::Role::Display).to_string();
            Gfx::Rect rect = {
                x_offset, y_offset,
                icon_size() + icon_spacing() + text_padding() + font().width(node_text) + text_padding(), item_height()
            };
            Gfx::Rect toggle_rect;
            if (row_count_at_index > 0) {
                int toggle_x = tree_column_x_offset + horizontal_padding() + indent_width_in_pixels() * indent_level - icon_size() / 2 - 4;
                toggle_rect = { toggle_x, rect.y(), toggle_size(), toggle_size() };
                toggle_rect.center_vertically_within(rect);
            }
            if (callback(index, rect, toggle_rect, indent_level) == IterationDecision::Break)
                return IterationDecision::Break;
            y_offset += item_height();
            // NOTE: Skip traversing children if this index is closed!
            if (!metadata.open)
                return IterationDecision::Continue;
        }

        ++indent_level;
        int row_count = model.row_count(index);
        for (int i = 0; i < row_count; ++i) {
            if (traverse_index(model.index(i, model.tree_column(), index)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        --indent_level;
        return IterationDecision::Continue;
    };
    int root_count = model.row_count();
    for (int root_index = 0; root_index < root_count; ++root_index) {
        if (traverse_index(model.index(root_index, model.tree_column(), ModelIndex())) == IterationDecision::Break)
            break;
    }
}

void TreeView::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);
    Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), palette().color(background_role()));

    if (!model())
        return;
    auto& model = *this->model();

    painter.translate(frame_inner_rect().location());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto visible_content_rect = this->visible_content_rect();
    int tree_column = model.tree_column();
    int tree_column_x_offset = 0;
    for (int i = 0; i < tree_column; ++i) {
        if (!is_column_hidden(i))
            tree_column_x_offset += column_width(i);
    }
    int y_offset = header_height();

    int painted_row_index = 0;

    traverse_in_paint_order([&](const ModelIndex& index, const Gfx::Rect& a_rect, const Gfx::Rect& a_toggle_rect, int indent_level) {
        if (!a_rect.intersects_vertically(visible_content_rect))
            return IterationDecision::Continue;

        auto rect = a_rect.translated(0, y_offset);
        auto toggle_rect = a_toggle_rect.translated(0, y_offset);

#ifdef DEBUG_ITEM_RECTS
        painter.fill_rect(rect, Color::WarmGray);
#endif

        bool is_selected_row = selection().contains(index);

        Color text_color = palette().color(foreground_role());
        if (is_selected_row)
            text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row) {
            background_color = is_focused() ? palette().selection() : palette().inactive_selection();
            key_column_background_color = is_focused() ? palette().selection() : palette().inactive_selection();
        } else {
            if (alternating_row_colors() && (painted_row_index % 2)) {
                background_color = Color(220, 220, 220);
                key_column_background_color = Color(200, 200, 200);
            } else {
                background_color = palette().color(background_role());
                key_column_background_color = Color(220, 220, 220);
            }
        }

        Gfx::Rect row_rect { 0, rect.y(), frame_inner_rect().width(), rect.height() };
        painter.fill_rect(row_rect, background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < model.column_count(); ++column_index) {
            if (is_column_hidden(column_index))
                continue;
            auto column_metadata = model.column_metadata(column_index);
            int column_width = this->column_width(column_index);
            auto& font = column_metadata.font ? *column_metadata.font : this->font();

            painter.draw_rect(toggle_rect, text_color);

            if (column_index != tree_column) {
                Gfx::Rect cell_rect(horizontal_padding() + x_offset, rect.y(), column_width, item_height());
                auto cell_index = model.sibling(index.row(), column_index, index.parent());

                if (auto* delegate = column_data(column_index).cell_painting_delegate.ptr()) {
                    delegate->paint(painter, cell_rect, palette(), model, cell_index);
                } else {
                    auto data = model.data(cell_index);

                    if (data.is_bitmap()) {
                        painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
                    } else if (data.is_icon()) {
                        if (auto bitmap = data.as_icon().bitmap_for_size(16))
                            painter.blit(cell_rect.location(), *bitmap, bitmap->rect());
                    } else {
                        if (!is_selected_row)
                            text_color = model.data(cell_index, Model::Role::ForegroundColor).to_color(palette().color(foreground_role()));
                        painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color, Gfx::TextElision::Right);
                    }
                }
            } else {
                // It's the tree column!
                Gfx::Rect icon_rect = { rect.x(), rect.y(), icon_size(), icon_size() };
                auto icon = model.data(index, Model::Role::Icon);
                if (icon.is_icon()) {
                    if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size()))
                        painter.blit(icon_rect.location(), *bitmap, bitmap->rect());
                }
                Gfx::Rect text_rect = {
                    icon_rect.right() + 1 + icon_spacing(), rect.y(),
                    rect.width() - icon_size() - icon_spacing(), rect.height()
                };
                auto node_text = model.data(index, Model::Role::Display).to_string();
                painter.draw_text(text_rect, node_text, Gfx::TextAlignment::Center, text_color);
                auto index_at_indent = index;
                for (int i = indent_level; i > 0; --i) {
                    auto parent_of_index_at_indent = index_at_indent.parent();
                    bool index_at_indent_is_last_in_parent = index_at_indent.row() == model.row_count(parent_of_index_at_indent) - 1;
                    Gfx::Point a { tree_column_x_offset + horizontal_padding() + indent_width_in_pixels() * i - icon_size() / 2, rect.y() - 2 };
                    Gfx::Point b { a.x(), a.y() + item_height() - 1 };
                    if (index_at_indent_is_last_in_parent)
                        b.set_y(rect.center().y());
                    if (!(i != indent_level && index_at_indent_is_last_in_parent))
                        painter.draw_line(a, b, Color::MidGray);

                    if (i == indent_level) {
                        Gfx::Point c { a.x(), rect.center().y() };
                        Gfx::Point d { c.x() + icon_size() / 2, c.y() };
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
            }
            x_offset += column_width + horizontal_padding() * 2;
        }

        return IterationDecision::Continue;
    });

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, vertical_scrollbar().value());
    paint_headers(painter);
}

void TreeView::scroll_into_view(const ModelIndex& a_index, Orientation orientation)
{
    if (!a_index.is_valid())
        return;
    Gfx::Rect found_rect;
    traverse_in_paint_order([&](const ModelIndex& index, const Gfx::Rect& rect, const Gfx::Rect&, int) {
        if (index == a_index) {
            found_rect = rect;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    ScrollableWidget::scroll_into_view(found_rect, orientation);
}

void TreeView::did_update_model()
{
    m_view_metadata.clear();
    AbstractTableView::did_update_model();
}

void TreeView::did_update_selection()
{
    AbstractView::did_update_selection();
    ASSERT(model());
    auto index = selection().first();
    if (!index.is_valid())
        return;
#if 0
    bool opened_any = false;
    for (auto current = index; current.is_valid(); current = current.parent()) {
        auto& metadata_for_ancestor = ensure_metadata_for_index(current);
        if (!metadata_for_ancestor.open) {
            metadata_for_ancestor.open = true;
            opened_any = true;
        }
    }
    if (opened_any)
        update_content_size();
    update();
#endif
    if (activates_on_selection())
        activate(index);
}

void TreeView::keydown_event(KeyEvent& event)
{
    if (!model())
        return;
    auto cursor_index = selection().first();

    if (event.key() == KeyCode::Key_Space) {
        if (model()->row_count(cursor_index))
            toggle_index(cursor_index);
        return;
    }

    if (event.key() == KeyCode::Key_Up) {
        ModelIndex previous_index;
        ModelIndex found_index;
        traverse_in_paint_order([&](const ModelIndex& index, const Gfx::Rect&, const Gfx::Rect&, int) {
            if (index == cursor_index) {
                found_index = previous_index;
                return IterationDecision::Break;
            }
            previous_index = index;
            return IterationDecision::Continue;
        });
        if (found_index.is_valid()) {
            selection().set(found_index);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        ModelIndex previous_index;
        ModelIndex found_index;
        traverse_in_paint_order([&](const ModelIndex& index, const Gfx::Rect&, const Gfx::Rect&, int) {
            if (previous_index == cursor_index) {
                found_index = index;
                return IterationDecision::Break;
            }
            previous_index = index;
            return IterationDecision::Continue;
        });
        if (found_index.is_valid())
            selection().set(found_index);
        return;
    }

    auto open_tree_node = [&](bool open, MetadataForIndex& metadata) {
        metadata.open = open;
        update_column_sizes();
        update_content_size();
        update();
    };

    if (event.key() == KeyCode::Key_Left) {
        if (cursor_index.is_valid() && model()->row_count(cursor_index)) {
            auto& metadata = ensure_metadata_for_index(cursor_index);
            if (metadata.open)
                open_tree_node(false, metadata);
            return;
        }
    }

    if (event.key() == KeyCode::Key_Right) {
        if (cursor_index.is_valid() && model()->row_count(cursor_index)) {
            auto& metadata = ensure_metadata_for_index(cursor_index);
            if (!metadata.open)
                open_tree_node(true, metadata);
            return;
        }
    }

    if (event.key() == KeyCode::Key_Return) {
        if (cursor_index.is_valid() && model()->row_count(cursor_index)) {
            auto& metadata = ensure_metadata_for_index(cursor_index);
            open_tree_node(!metadata.open, metadata);
            return;
        }
    }
}

int TreeView::item_count() const
{
    int count = 0;
    traverse_in_paint_order([&](const ModelIndex&, const Gfx::Rect&, const Gfx::Rect&, int) {
        ++count;
        return IterationDecision::Continue;
    });
    return count;
}

void TreeView::update_column_sizes()
{
    if (!size_columns_to_fit_content())
        return;

    if (!model())
        return;

    auto& model = *this->model();
    int column_count = model.column_count();
    int row_count = model.row_count();
    int tree_column = model.tree_column();
    int tree_column_x_offset = 0;

    for (int column = 0; column < column_count; ++column) {
        if (column == tree_column)
            continue;
        if (is_column_hidden(column))
            continue;
        int header_width = header_font().width(model.column_name(column));
        int column_width = header_width;

        for (int row = 0; row < row_count; ++row) {
            auto cell_data = model.data(model.index(row, column));
            int cell_width = 0;
            if (cell_data.is_bitmap()) {
                cell_width = cell_data.as_bitmap().width();
            } else {
                cell_width = font().width(cell_data.to_string());
            }
            column_width = max(column_width, cell_width);
        }
        auto& column_data = this->column_data(column);
        column_data.width = max(column_data.width, column_width);
        column_data.has_initialized_width = true;

        if (column < tree_column)
            tree_column_x_offset += column_width;
    }

    int tree_column_header_width = header_font().width(model.column_name(tree_column));
    int tree_column_width = tree_column_header_width;
    traverse_in_paint_order([&](const ModelIndex&, const Gfx::Rect& rect, const Gfx::Rect&, int) {
        tree_column_width = max(rect.right() - tree_column_x_offset, tree_column_width);
        return IterationDecision::Continue;
    });

    auto& column_data = this->column_data(tree_column);
    column_data.width = max(column_data.width, tree_column_width);
    column_data.has_initialized_width = true;
}

}
