#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTreeView.h>

//#define DEBUG_ITEM_RECTS

struct GTreeView::MetadataForIndex {
    bool open { false };
};

GTreeView::MetadataForIndex& GTreeView::ensure_metadata_for_index(const GModelIndex& index) const
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

GTreeView::GTreeView(GWidget* parent)
    : GAbstractColumnView(parent)
{
    set_size_columns_to_fit_content(true);
    set_headers_visible(false);
    m_expand_bitmap = GraphicsBitmap::load_from_file("/res/icons/treeview-expand.png");
    m_collapse_bitmap = GraphicsBitmap::load_from_file("/res/icons/treeview-collapse.png");
}

GTreeView::~GTreeView()
{
}

GModelIndex GTreeView::index_at_event_position(const Point& a_position, bool& is_toggle) const
{
    auto position = a_position.translated(0, -header_height()).translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
    is_toggle = false;
    if (!model())
        return {};
    GModelIndex result;
    traverse_in_paint_order([&](const GModelIndex& index, const Rect& rect, const Rect& toggle_rect, int) {
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

void GTreeView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);
    if (!index.is_valid())
        return;

    if (event.button() == GMouseButton::Left) {
        if (selection().first() != index)
            selection().set(index);

        if (model.row_count(index))
            toggle_index(index);
        else
            activate(index);
    }
}

void GTreeView::toggle_index(const GModelIndex& index)
{
    ASSERT(model()->row_count(index));
    auto& metadata = ensure_metadata_for_index(index);
    metadata.open = !metadata.open;
    update_column_sizes();
    update_content_size();
    update();
}

template<typename Callback>
void GTreeView::traverse_in_paint_order(Callback callback) const
{
    ASSERT(model());
    auto& model = *this->model();
    int indent_level = 1;
    int y_offset = 0;
    int tree_column = model.tree_column();
    int tree_column_x_offset = 0;

    for (int i = 0; i < tree_column; ++i) {
        tree_column_x_offset += column_width(i);
    }

    Function<IterationDecision(const GModelIndex&)> traverse_index = [&](const GModelIndex& index) {
        int row_count_at_index = model.row_count(index);
        if (index.is_valid()) {
            auto& metadata = ensure_metadata_for_index(index);
            int x_offset = tree_column_x_offset + horizontal_padding() + indent_level * indent_width_in_pixels();
            auto node_text = model.data(index, GModel::Role::Display).to_string();
            Rect rect = {
                x_offset, y_offset,
                icon_size() + icon_spacing() + text_padding() + font().width(node_text) + text_padding(), item_height()
            };
            Rect toggle_rect;
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
        if (traverse_index(model.index(root_index, model.tree_column(), GModelIndex())) == IterationDecision::Break)
            break;
    }
}

void GTreeView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), SystemColor::Base);

    if (!model())
        return;
    auto& model = *this->model();

    painter.translate(frame_inner_rect().location());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto visible_content_rect = this->visible_content_rect();
    int tree_column = model.tree_column();
    int tree_column_x_offset = 0;
    for (int i = 0; i < tree_column; ++i) {
        tree_column_x_offset += column_width(i);
    }
    int y_offset = header_height();

    int painted_row_index = 0;

    traverse_in_paint_order([&](const GModelIndex& index, const Rect& a_rect, const Rect& a_toggle_rect, int indent_level) {
        if (!a_rect.intersects_vertically(visible_content_rect))
            return IterationDecision::Continue;

        auto rect = a_rect.translated(0, y_offset);
        auto toggle_rect = a_toggle_rect.translated(0, y_offset);

#ifdef DEBUG_ITEM_RECTS
        painter.fill_rect(rect, Color::WarmGray);
#endif

        bool is_selected_row = selection().contains(index);

        Color text_color = Color::Black;
        if (is_selected_row)
            text_color = Color::White;

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            key_column_background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
        } else {
            if (alternating_row_colors() && (painted_row_index % 2)) {
                background_color = Color(220, 220, 220);
                key_column_background_color = Color(200, 200, 200);
            } else {
                background_color = SystemColor::Base;
                key_column_background_color = Color(220, 220, 220);
            }
        }

        Rect row_rect { 0, rect.y(), frame_inner_rect().width(), rect.height() };
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
                Rect cell_rect(horizontal_padding() + x_offset, rect.y(), column_width, item_height());
                auto cell_index = model.sibling(index.row(), column_index, index.parent());

                if (auto* delegate = column_data(column_index).cell_painting_delegate.ptr()) {
                    delegate->paint(painter, cell_rect, model, cell_index);
                } else {
                    auto data = model.data(cell_index);

                    if (data.is_bitmap()) {
                        painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
                    } else if (data.is_icon()) {
                        if (auto bitmap = data.as_icon().bitmap_for_size(16))
                            painter.blit(cell_rect.location(), *bitmap, bitmap->rect());
                    } else {
                        if (!is_selected_row)
                            text_color = model.data(cell_index, GModel::Role::ForegroundColor).to_color(Color::Black);
                        painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color, TextElision::Right);
                    }
                }
            } else {
                // It's the tree column!
                Rect icon_rect = { rect.x(), rect.y(), icon_size(), icon_size() };
                auto icon = model.data(index, GModel::Role::Icon);
                if (icon.is_icon()) {
                    if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size()))
                        painter.blit(icon_rect.location(), *bitmap, bitmap->rect());
                }
                Rect text_rect = {
                    icon_rect.right() + 1 + icon_spacing(), rect.y(),
                    rect.width() - icon_size() - icon_spacing(), rect.height()
                };
                auto node_text = model.data(index, GModel::Role::Display).to_string();
                painter.draw_text(text_rect, node_text, TextAlignment::Center, text_color);
                auto index_at_indent = index;
                for (int i = indent_level; i > 0; --i) {
                    auto parent_of_index_at_indent = index_at_indent.parent();
                    bool index_at_indent_is_last_in_parent = index_at_indent.row() == model.row_count(parent_of_index_at_indent) - 1;
                    Point a { tree_column_x_offset + horizontal_padding() + indent_width_in_pixels() * i - icon_size() / 2, rect.y() - 2 };
                    Point b { a.x(), a.y() + item_height() - 1 };
                    if (index_at_indent_is_last_in_parent)
                        b.set_y(rect.center().y());
                    if (!(i != indent_level && index_at_indent_is_last_in_parent))
                        painter.draw_line(a, b, Color::MidGray);

                    if (i == indent_level) {
                        Point c { a.x(), rect.center().y() };
                        Point d { c.x() + icon_size() / 2, c.y() };
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

void GTreeView::scroll_into_view(const GModelIndex& a_index, Orientation orientation)
{
    if (!a_index.is_valid())
        return;
    Rect found_rect;
    traverse_in_paint_order([&](const GModelIndex& index, const Rect& rect, const Rect&, int) {
        if (index == a_index) {
            found_rect = rect;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    GScrollableWidget::scroll_into_view(found_rect, orientation);
}

void GTreeView::did_update_model()
{
    m_view_metadata.clear();
    GAbstractColumnView::did_update_model();
}

void GTreeView::did_update_selection()
{
    GAbstractView::did_update_selection();
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

void GTreeView::keydown_event(GKeyEvent& event)
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
        GModelIndex previous_index;
        GModelIndex found_index;
        traverse_in_paint_order([&](const GModelIndex& index, const Rect&, const Rect&, int) {
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
        GModelIndex previous_index;
        GModelIndex found_index;
        traverse_in_paint_order([&](const GModelIndex& index, const Rect&, const Rect&, int) {
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
    if (event.key() == KeyCode::Key_Left) {
        if (cursor_index.is_valid() && model()->row_count(cursor_index)) {
            auto& metadata = ensure_metadata_for_index(cursor_index);
            if (metadata.open) {
                metadata.open = false;
                update_column_sizes();
                update_content_size();
                update();
            }
            return;
        }
    }
    if (event.key() == KeyCode::Key_Right) {
        if (cursor_index.is_valid() && model()->row_count(cursor_index)) {
            auto& metadata = ensure_metadata_for_index(cursor_index);
            if (!metadata.open) {
                metadata.open = true;
                update_column_sizes();
                update_content_size();
                update();
            }
            return;
        }
    }
}

int GTreeView::item_count() const
{
    int count = 0;
    traverse_in_paint_order([&](const GModelIndex&, const Rect&, const Rect&, int) {
        ++count;
        return IterationDecision::Continue;
    });
    return count;
}

void GTreeView::update_column_sizes()
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
    traverse_in_paint_order([&](const GModelIndex&, const Rect& rect, const Rect&, int) {
        tree_column_width = max(rect.right() - tree_column_x_offset, tree_column_width);
        return IterationDecision::Continue;
    });

    auto& column_data = this->column_data(tree_column);
    column_data.width = max(column_data.width, tree_column_width);
    column_data.has_initialized_width = true;
}
