#include <LibDraw/CharacterBitmap.h>
#include <LibGUI/GColumnsView.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

static const char* s_arrow_bitmap_data = {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "
};
static const int s_arrow_bitmap_width = 9;
static const int s_arrow_bitmap_height = 9;

GColumnsView::GColumnsView(GWidget* parent)
    : GAbstractView(parent)
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    m_columns.append({ {}, 0 });
}

GColumnsView::~GColumnsView()
{
}

void GColumnsView::paint_event(GPaintEvent& event)
{
    GAbstractView::paint_event(event);

    if (!model())
        return;

    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int column_x = 0;

    for (int i = 0; i < m_columns.size(); i++) {
        auto& column = m_columns[i];
        auto* next_column = i + 1 == m_columns.size() ? nullptr : &m_columns[i + 1];

        ASSERT(column.width > 0);

        int row_count = model()->row_count(column.parent_index);
        for (int row = 0; row < row_count; row++) {
            GModelIndex index = model()->index(row, m_model_column, column.parent_index);
            ASSERT(index.is_valid());

            bool is_selected_row = selection().contains(index);

            Color background_color = palette().color(background_role());
            Color text_color = palette().color(foreground_role());

            if (next_column != nullptr && next_column->parent_index == index)
                background_color = background_color.blend(palette().selection().with_alpha(100));

            if (is_selected_row) {
                background_color = palette().selection();
                text_color = palette().selection_text();
            }

            Rect row_rect { column_x, row * item_height(), column.width, item_height() };
            painter.fill_rect(row_rect, background_color);

            auto icon = model()->data(index, GModel::Role::Icon);
            Rect icon_rect = { column_x + icon_spacing(), 0, icon_size(), icon_size() };
            icon_rect.center_vertically_within(row_rect);
            if (icon.is_icon())
                if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size()))
                    painter.blit(icon_rect.location(), *bitmap, bitmap->rect());

            Rect text_rect = {
                icon_rect.right() + 1 + icon_spacing(), row * item_height(),
                column.width - icon_spacing() - icon_size() - icon_spacing() - icon_spacing() - s_arrow_bitmap_width - icon_spacing(), item_height()
            };
            auto text = model()->data(index).to_string();
            painter.draw_text(text_rect, text, TextAlignment::CenterLeft, text_color);

            bool expandable = model()->row_count(index) > 0;
            if (expandable) {
                Rect arrow_rect = {
                    text_rect.right() + 1 + icon_spacing(), 0,
                    s_arrow_bitmap_width, s_arrow_bitmap_height
                };
                arrow_rect.center_vertically_within(row_rect);
                static auto& arrow_bitmap = CharacterBitmap::create_from_ascii(s_arrow_bitmap_data, s_arrow_bitmap_width, s_arrow_bitmap_height).leak_ref();
                painter.draw_bitmap(arrow_rect.location(), arrow_bitmap, text_color);
            }
        }

        painter.draw_line({ column_x + column.width, 0 }, { column_x + column.width, frame_inner_rect().bottom() }, palette().button());
        column_x += column.width + 1;
    }
}

void GColumnsView::push_column(GModelIndex& parent_index)
{
    ASSERT(model());

    // Drop columns at the end.
    GModelIndex grandparent = model()->parent_index(parent_index);
    for (int i = m_columns.size() - 1; i > 0; i--) {
        if (m_columns[i].parent_index == grandparent)
            break;
        m_columns.shrink(i);
        dbg() << "Dropping column " << i;
    }

    // Add the new column.
    dbg() << "Adding a new column";
    m_columns.append({ parent_index, 0 });
    update_column_sizes();
    update();
}

void GColumnsView::update_column_sizes()
{
    if (!model())
        return;

    int total_width = 0;
    int total_height = 0;

    for (auto& column : m_columns) {
        int row_count = model()->row_count(column.parent_index);

        int column_height = row_count * item_height();
        if (column_height > total_height)
            total_height = column_height;

        column.width = 10;
        for (int row = 0; row < row_count; row++) {
            GModelIndex index = model()->index(row, m_model_column, column.parent_index);
            ASSERT(index.is_valid());
            auto text = model()->data(index).to_string();
            int row_width = icon_spacing() + icon_size() + icon_spacing() + font().width(text) + icon_spacing() + s_arrow_bitmap_width + icon_spacing();
            if (row_width > column.width)
                column.width = row_width;
        }
        total_width += column.width + 1;
    }

    set_content_size({ total_width, total_height });
}

GModelIndex GColumnsView::index_at_event_position(const Point& a_position) const
{
    if (!model())
        return {};

    auto position = a_position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());

    int column_x = 0;

    for (auto& column : m_columns) {
        if (position.x() < column_x)
            break;
        if (position.x() > column_x + column.width) {
            column_x += column.width;
            continue;
        }

        int row = position.y() / item_height();
        int row_count = model()->row_count(column.parent_index);
        if (row >= row_count)
            return {};

        return model()->index(row, m_model_column, column.parent_index);
    }

    return {};
}

void GColumnsView::mousedown_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != GMouseButton::Left)
        return;

    auto index = index_at_event_position(event.position());
    if (!index.is_valid()) {
        selection().clear();
        return;
    }

    if (event.modifiers() & Mod_Ctrl) {
        selection().toggle(index);
    } else {
        selection().set(index);
        if (model()->row_count(index))
            push_column(index);
    }
}

void GColumnsView::did_update_model()
{
    GAbstractView::did_update_model();

    // FIXME: Don't drop the columns on minor updates.
    dbg() << "Model was updated; dropping columns :(";
    m_columns.clear();
    m_columns.append({ {}, 0 });

    update_column_sizes();
    update();
}

void GColumnsView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != GMouseButton::Left)
        return;

    mousedown_event(event);
    activate_selected();
}

void GColumnsView::context_menu_event(GContextMenuEvent& event)
{
    if (!model())
        return;

    auto index = index_at_event_position(event.position());
    if (index.is_valid()) {
        if (!selection().contains(index))
            selection().set(index);
    } else {
        selection().clear();
    }
    if (on_context_menu_request)
        on_context_menu_request(index, event);
}

void GColumnsView::keydown_event(GKeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();

    if (event.key() == KeyCode::Key_Return) {
        activate_selected();
        return;
    }

    if (event.key() == KeyCode::Key_Up) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            auto parent_index = model.parent_index(old_index);
            int row = old_index.row() > 0 ? old_index.row() - 1 : 0;
            new_index = model.sibling(row, old_index.column(), parent_index);
        } else {
            new_index = model.index(0, m_model_column, {});
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            update();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Down) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            auto parent_index = model.parent_index(old_index);
            int row = old_index.row() + 1;
            new_index = model.sibling(row, old_index.column(), parent_index);
        } else {
            new_index = model.index(0, m_model_column, {});
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            update();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Left) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.parent_index(old_index);
        } else {
            new_index = model.index(0, m_model_column, {});
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            update();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Right) {
        GModelIndex old_index, new_index;
        if (!selection().is_empty()) {
            old_index = selection().first();
            new_index = model.index(0, m_model_column, old_index);
        } else {
            new_index = model.index(0, m_model_column, {});
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            if (model.is_valid(old_index))
                push_column(old_index);
            update();
        }
        return;
    }
}
