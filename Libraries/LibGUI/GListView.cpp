#include <Kernel/KeyCode.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

GListView::GListView(GWidget* parent)
    : GAbstractView(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
}

GListView::~GListView()
{
}

void GListView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width = 0;
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        auto text = model()->data(model()->index(row, m_model_column), GModel::Role::Display);
        content_width = max(content_width, font().width(text.to_string()));
    }

    content_width = max(content_width, widget_inner_rect().width());

    int content_height = item_count() * item_height();
    set_content_size({ content_width, content_height });
}

void GListView::resize_event(GResizeEvent& event)
{
    update_content_size();
    GAbstractView::resize_event(event);
}

void GListView::did_update_model()
{
    GAbstractView::did_update_model();
    update_content_size();
    update();
}

Rect GListView::content_rect(int row) const
{
    return { 0, row * item_height(), content_width(), item_height() };
}

Rect GListView::content_rect(const GModelIndex& index) const
{
    return content_rect(index.row());
}

Point GListView::adjusted_position(const Point& position)
{
    return position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
}

void GListView::mousedown_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != GMouseButton::Left)
        return;

    auto adjusted_position = this->adjusted_position(event.position());
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        if (!content_rect(row).contains(adjusted_position))
            continue;
        auto index = model()->index(row, m_model_column);
        if (event.modifiers() & Mod_Ctrl)
            selection().toggle(index);
        else
            selection().set(index);
        return;
    }
    selection().clear();
}

void GListView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    if (!model())
        return;

    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int exposed_width = max(content_size().width(), width());
    int painted_item_index = 0;

    for (int row_index = 0; row_index < model()->row_count(); ++row_index) {
        bool is_selected_row = selection().contains_row(row_index);
        int y = painted_item_index * item_height();

        Color background_color;
        if (is_selected_row) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
        } else {
            if (alternating_row_colors() && (painted_item_index % 2))
                background_color = Color(210, 210, 210);
            else
                background_color = SystemColor::Base;
        }

        auto column_metadata = model()->column_metadata(m_model_column);

        Rect row_rect(0, y, content_width(), item_height());
        painter.fill_rect(row_rect, background_color);
        auto index = model()->index(row_index, m_model_column);
        auto data = model()->data(index);
        auto font = font_for_index(index);
        if (data.is_bitmap()) {
            painter.blit(row_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
        } else if (data.is_icon()) {
            if (auto bitmap = data.as_icon().bitmap_for_size(16))
                painter.blit(row_rect.location(), *bitmap, bitmap->rect());
        } else {
            Color text_color;
            if (is_selected_row)
                text_color = Color::White;
            else
                text_color = model()->data(index, GModel::Role::ForegroundColor).to_color(SystemColor::WindowText);
            auto text_rect = row_rect;
            text_rect.move_by(horizontal_padding(), 0);
            text_rect.set_width(text_rect.width() - horizontal_padding() * 2);
            painter.draw_text(text_rect, data.to_string(), font, column_metadata.text_alignment, text_color);
        }

        ++painted_item_index;
    };

    Rect unpainted_rect(0, painted_item_index * item_height(), exposed_width, height());
    painter.fill_rect(unpainted_rect, SystemColor::Base);
}

int GListView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void GListView::keydown_event(GKeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        selection().for_each_index([this](auto& index) {
            activate(index);
        });
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
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
    if (event.key() == KeyCode::Key_Down) {
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
        int items_per_page = visible_content_rect().height() / item_height();
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
        int items_per_page = visible_content_rect().height() / item_height();
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

void GListView::scroll_into_view(const GModelIndex& index, Orientation orientation)
{
    auto rect = content_rect(index.row());
    GScrollableWidget::scroll_into_view(rect, orientation);
}

void GListView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == GMouseButton::Left) {
        if (!selection().is_empty()) {
            if (is_editable()) {
                begin_editing(selection().first());
            } else {
                selection().for_each_index([this](auto& index) {
                    activate(index);
                });
            }
        }
    }
}
