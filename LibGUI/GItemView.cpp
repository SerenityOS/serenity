#include <LibGUI/GItemView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GPainter.h>
#include <Kernel/KeyCode.h>

GItemView::GItemView(GWidget* parent)
    : GAbstractView(parent)
{
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
        return set_content_size({ });

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
        return { };
    int visual_row_index = item_index / m_visual_column_count;
    int visual_column_index = item_index % m_visual_column_count;
    return {
        visual_column_index * effective_item_size().width(),
        visual_row_index * effective_item_size().height(),
        effective_item_size().width(),
        effective_item_size().height()
    };
}

void GItemView::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        // FIXME: Since all items are the same size, just compute the clicked item index
        //        instead of iterating over everything.
        auto adjusted_position = event.position().translated(0, vertical_scrollbar().value());
        for (int i = 0; i < item_count(); ++i) {
            if (item_rect(i).contains(adjusted_position)) {
                model()->set_selected_index(model()->index(i, 0));
                update();
                return;
            }
        }
        model()->set_selected_index({ });
        update();
    }
}

void GItemView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == GMouseButton::Left) {
        mousedown_event(event);
        activate(model()->selected_index());
    }
}

void GItemView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());    
    painter.fill_rect(event.rect(), Color::White);
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto column_metadata = model()->column_metadata(m_model_column);
    const Font& font = column_metadata.font ? *column_metadata.font : this->font();

    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {
        bool is_selected_item = item_index == model()->selected_index().row();
        Color background_color;
        if (is_selected_item) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
        } else {
            background_color = Color::White;
        }

        Rect item_rect = this->item_rect(item_index);
        auto model_index = model()->index(item_index, m_model_column);

        auto icon = model()->data(model_index, GModel::Role::Icon);
        auto item_text = model()->data(model_index, GModel::Role::Display);

        Rect icon_rect = { 0, 0, 32, 32 };
        icon_rect.center_within(item_rect);
        icon_rect.move_by(0, -font.glyph_height() - 6);

        if (icon.is_icon()) {
            if (auto bitmap = icon.as_icon().bitmap_for_size(icon_rect.width()))
                painter.draw_scaled_bitmap(icon_rect, *bitmap, bitmap->rect());
        }

        Rect text_rect { 0, icon_rect.bottom() + 6 + 1, font.width(item_text.to_string()), font.glyph_height() };
        text_rect.center_horizontally_within(item_rect);
        text_rect.inflate(6, 4);

        Color text_color;
        if (is_selected_item)
            text_color = Color::White;
        else
            text_color = model()->data(model_index, GModel::Role::ForegroundColor).to_color(Color::Black);
        painter.fill_rect(text_rect, background_color);
        painter.draw_text(text_rect, item_text.to_string(), font, TextAlignment::Center, text_color);
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
        activate(model.selected_index());
        return;
    }
    if (event.key() == KeyCode::Key_Home) {
        auto new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_End) {
        auto new_index = model.index(model.row_count() - 1, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = model.index(model.selected_index().row() - m_visual_column_count, model.selected_index().column());
        else
            new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = model.index(model.selected_index().row() + m_visual_column_count, model.selected_index().column());
        else
            new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = model.index(model.selected_index().row() - 1, model.selected_index().column());
        else
            new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = model.index(model.selected_index().row() + 1, model.selected_index().column());
        else
            new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto new_index = model.index(max(0, model.selected_index().row() - items_per_page), model.selected_index().column());
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto new_index = model.index(min(model.row_count() - 1, model.selected_index().row() + items_per_page), model.selected_index().column());
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return GWidget::keydown_event(event);
}
