#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GDragOperation.h>
#include <LibGUI/GItemView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

GItemView::GItemView(GWidget* parent)
    : GAbstractView(parent)
{
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
    horizontal_scrollbar().set_visible(false);
}

GItemView::~GItemView()
{
}

void GItemView::scroll_into_view(const GModelIndex& index, Orientation orientation)
{
    GScrollableWidget::scroll_into_view(item_rect(index.row()), orientation);
}

void GItemView::resize_event(GResizeEvent& event)
{
    GAbstractView::resize_event(event);
    update_content_size();
}

void GItemView::did_update_model()
{
    GAbstractView::did_update_model();
    update_content_size();
    update();
}

void GItemView::update_content_size()
{
    if (!model())
        return set_content_size({});

    m_visual_column_count = available_size().width() / effective_item_size().width();
    if (m_visual_column_count)
        m_visual_row_count = ceil_div(model()->row_count(), m_visual_column_count);
    else
        m_visual_row_count = 0;

    int content_width = available_size().width();
    int content_height = m_visual_row_count * effective_item_size().height();

    set_content_size({ content_width, content_height });
}

Rect GItemView::item_rect(int item_index) const
{
    if (!m_visual_row_count || !m_visual_column_count)
        return {};
    int visual_row_index = item_index / m_visual_column_count;
    int visual_column_index = item_index % m_visual_column_count;
    return {
        visual_column_index * effective_item_size().width(),
        visual_row_index * effective_item_size().height(),
        effective_item_size().width(),
        effective_item_size().height()
    };
}

Vector<int> GItemView::items_intersecting_rect(const Rect& rect) const
{
    ASSERT(model());
    const auto& column_metadata = model()->column_metadata(model_column());
    const auto& font = column_metadata.font ? *column_metadata.font : this->font();
    Vector<int> item_indexes;
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        Rect item_rect;
        Rect icon_rect;
        Rect text_rect;
        auto item_text = model()->data(model()->index(item_index, model_column()));
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);
        if (icon_rect.intersects(rect) || text_rect.intersects(rect))
            item_indexes.append(item_index);
    }
    return item_indexes;
}

int GItemView::item_at_event_position(const Point& position) const
{
    ASSERT(model());
    // FIXME: Since all items are the same size, just compute the clicked item index
    //        instead of iterating over everything.
    auto adjusted_position = position.translated(0, vertical_scrollbar().value());
    const auto& column_metadata = model()->column_metadata(model_column());
    const auto& font = column_metadata.font ? *column_metadata.font : this->font();
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        Rect item_rect;
        Rect icon_rect;
        Rect text_rect;
        auto item_text = model()->data(model()->index(item_index, model_column()));
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);
        if (icon_rect.contains(adjusted_position) || text_rect.contains(adjusted_position))
            return item_index;
    }
    return -1;
}

void GItemView::mousedown_event(GMouseEvent& event)
{
    int item_index = item_at_event_position(event.position());

    if (event.button() == GMouseButton::Left) {
        m_left_mousedown_position = event.position();
        if (item_index == -1) {
            if(event.modifiers() & Mod_Ctrl) {
                selection().for_each_index([&](auto& index) {
                    m_rubber_band_remembered_selection.append(index);
                });
            } else {
                selection().clear();
            }
            m_rubber_banding = true;
            m_rubber_band_origin = event.position();
            m_rubber_band_current = event.position();
        } else {
            auto index = model()->index(item_index, m_model_column);
            if (event.modifiers() & Mod_Ctrl)
                selection().toggle(index);
            else if (selection().size() > 1)
                m_might_drag = true;
            else
                selection().set(index);
        }
    }

    GAbstractView::mousedown_event(event);
}

void GItemView::mouseup_event(GMouseEvent& event)
{
    if (m_rubber_banding && event.button() == GMouseButton::Left) {
        m_rubber_banding = false;
        m_rubber_band_remembered_selection.clear();
        update();
        return;
    }
    int item_index = item_at_event_position(event.position());
    auto index = model()->index(item_index, m_model_column);
    if((selection().size() > 1) & m_might_drag) {
        selection().set(index);
        m_might_drag = false;
    }
    GAbstractView::mouseup_event(event);
}

void GItemView::mousemove_event(GMouseEvent& event)
{
    if (!model())
        return GAbstractView::mousemove_event(event);

    if (m_rubber_banding) {
        if (m_rubber_band_current != event.position()) {
            m_rubber_band_current = event.position();
            auto rubber_band_rect = Rect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
            selection().clear();
            for (auto item_index : items_intersecting_rect(rubber_band_rect)) {
                selection().add(model()->index(item_index, model_column()));
            }
            if(event.modifiers() & Mod_Ctrl) {
                for (auto storeditem : m_rubber_band_remembered_selection) {
                    selection().add(storeditem);
                }
            }
            update();
            return;
        }
    }

    if (event.buttons() & GMouseButton::Left && !selection().is_empty()) {
        auto diff = event.position() - m_left_mousedown_position;
        auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
        constexpr int drag_distance_threshold = 5;
        if (distance_travelled_squared > (drag_distance_threshold)) {
            dbg() << "Initiate drag!";
            auto drag_operation = GDragOperation::construct();

            RefPtr<GraphicsBitmap> bitmap;

            StringBuilder text_builder;
            StringBuilder data_builder;
            int index_iterations = 0;
            selection().for_each_index([&](auto& index) {
                index_iterations++;
                auto text_data = model()->data(index);
                if (index_iterations == 0)
                    text_builder.append(" ");
                text_builder.append(text_data.to_string());
                if (!(index_iterations == selection().size()))
                    text_builder.append(", ");

                auto drag_data = model()->data(index, GModel::Role::DragData);
                data_builder.append(drag_data.to_string());
                data_builder.append('\n');

                if (!bitmap) {
                    GVariant icon_data = model()->data(index, GModel::Role::Icon);
                    if (icon_data.is_icon())
                        bitmap = icon_data.as_icon().bitmap_for_size(32);
                }
            });

            drag_operation->set_text(text_builder.to_string());
            drag_operation->set_bitmap(bitmap);
            drag_operation->set_data("url-list", data_builder.to_string());
            auto outcome = drag_operation->exec();
            switch (outcome) {
            case GDragOperation::Outcome::Accepted:
                dbg() << "Drag was accepted!";
                break;
            case GDragOperation::Outcome::Cancelled:
                dbg() << "Drag was cancelled!";
                break;
            default:
                ASSERT_NOT_REACHED();
                break;
            }
        }
    }

    GAbstractView::mousemove_event(event);
}

void GItemView::context_menu_event(GContextMenuEvent& event)
{
    if (!model())
        return;
    auto item_index = item_at_event_position(event.position());
    GModelIndex index;
    if (item_index != -1) {
        index = model()->index(item_index, m_model_column);
        if (!selection().contains(index))
            selection().set(index);
    } else {
        selection().clear();
    }
    if (on_context_menu_request)
        on_context_menu_request(index, event);
    GAbstractView::context_menu_event(event);
}

void GItemView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == GMouseButton::Left) {
        mousedown_event(event);
        selection().for_each_index([this](auto& index) {
            activate(index);
        });
    }
}

void GItemView::get_item_rects(int item_index, const Font& font, const GVariant& item_text, Rect& item_rect, Rect& icon_rect, Rect& text_rect) const
{
    item_rect = this->item_rect(item_index);
    icon_rect = { 0, 0, 32, 32 };
    icon_rect.center_within(item_rect);
    icon_rect.move_by(0, -font.glyph_height() - 6);
    text_rect = { 0, icon_rect.bottom() + 6 + 1, font.width(item_text.to_string()), font.glyph_height() };
    text_rect.center_horizontally_within(item_rect);
    text_rect.inflate(6, 4);
    text_rect.intersect(item_rect);
}

void GItemView::second_paint_event(GPaintEvent& event)
{
    if (!m_rubber_banding)
        return;

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    auto rubber_band_rect = Rect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
    painter.fill_rect(rubber_band_rect, parent_widget()->palette().rubber_band_fill());
    painter.draw_rect(rubber_band_rect, parent_widget()->palette().rubber_band_border());
}

void GItemView::paint_event(GPaintEvent& event)
{
    Color widget_background_color = palette().color(background_role());
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), widget_background_color);
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto column_metadata = model()->column_metadata(m_model_column);
    const Font& font = column_metadata.font ? *column_metadata.font : this->font();

    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {
        auto model_index = model()->index(item_index, m_model_column);
        bool is_selected_item = selection().contains(model_index);
        Color background_color;
        if (is_selected_item) {
            background_color = is_focused() ? palette().selection() : Color::from_rgb(0x606060);
        } else {
            background_color = widget_background_color;
        }

        auto icon = model()->data(model_index, GModel::Role::Icon);
        auto item_text = model()->data(model_index, GModel::Role::Display);

        Rect item_rect;
        Rect icon_rect;
        Rect text_rect;
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);

        if (icon.is_icon()) {
            if (auto bitmap = icon.as_icon().bitmap_for_size(icon_rect.width()))
                painter.draw_scaled_bitmap(icon_rect, *bitmap, bitmap->rect());
        }

        Color text_color;
        if (is_selected_item)
            text_color = palette().selection_text();
        else
            text_color = model()->data(model_index, GModel::Role::ForegroundColor).to_color(palette().color(foreground_role()));
        painter.fill_rect(text_rect, background_color);
        painter.draw_text(text_rect, item_text.to_string(), font, TextAlignment::Center, text_color, TextElision::Right);
    };
}

int GItemView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void GItemView::keydown_event(GKeyEvent& event)
{
    if (!model())
        return;
    if (!m_visual_row_count || !m_visual_column_count)
        return;

    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        selection().for_each_index([this](auto& index) {
            activate(index);
        });
        return;
    }
    if (event.key() == KeyCode::Key_Home) {
        auto new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_End) {
        auto new_index = model.index(model.row_count() - 1, 0);
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - m_visual_column_count, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + m_visual_column_count, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto old_index = selection().first();
        auto new_index = model.index(max(0, old_index.row() - items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto old_index = selection().first();
        auto new_index = model.index(min(model.row_count() - 1, old_index.row() + items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return GWidget::keydown_event(event);
}
