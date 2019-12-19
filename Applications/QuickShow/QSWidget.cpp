#include "QSWidget.h"
#include <AK/URL.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWindow.h>

QSWidget::QSWidget(GWidget* parent)
    : GFrame(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    set_fill_with_background_color(true);
    set_background_color(Color::Black);
}

QSWidget::~QSWidget()
{
}

void QSWidget::set_bitmap(NonnullRefPtr<GraphicsBitmap> bitmap)
{
    m_bitmap = move(bitmap);
}

void QSWidget::relayout()
{
    Size new_size;
    float scale_factor = (float)m_scale / 100.0f;
    new_size.set_width(m_bitmap->width() * scale_factor);
    new_size.set_height(m_bitmap->height() * scale_factor);
    m_bitmap_rect.set_size(new_size);
    update();
}

void QSWidget::resize_event(GResizeEvent& event)
{
    relayout();
    GWidget::resize_event(event);
}

void QSWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.draw_scaled_bitmap(m_bitmap_rect, *m_bitmap, m_bitmap->rect());
}

void QSWidget::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    m_pan_origin = event.position();
    m_pan_bitmap_origin = m_bitmap_rect.location();
}

void QSWidget::mouseup_event(GMouseEvent& event)
{
    UNUSED_PARAM(event);
}

void QSWidget::mousemove_event(GMouseEvent& event)
{
    if (!(event.buttons() & GMouseButton::Left))
        return;

    auto delta = event.position() - m_pan_origin;
    m_bitmap_rect.set_location(m_pan_bitmap_origin.translated(delta));
    update();
}

void QSWidget::mousewheel_event(GMouseEvent& event)
{
    auto old_scale = m_scale;
    auto old_scale_factor = (float)m_scale / 100.0f;
    auto zoom_point = event.position().translated(-m_bitmap_rect.location());
    zoom_point.set_x((float)zoom_point.x() / old_scale_factor);
    zoom_point.set_y((float)zoom_point.y() / old_scale_factor);
    m_scale += -event.wheel_delta() * 10;
    if (m_scale < 10)
        m_scale = 10;
    if (m_scale > 1000)
        m_scale = 1000;
    relayout();
    auto new_scale_factor = (float)m_scale / 100.0f;
    auto scale_factor_change = new_scale_factor - old_scale_factor;
    m_bitmap_rect.move_by(-Point((float)zoom_point.x() * scale_factor_change, (float)zoom_point.y() * scale_factor_change));
    if (old_scale != m_scale) {
        if (on_scale_change)
            on_scale_change(m_scale);
    }
}

void QSWidget::set_path(const String& path)
{
    m_path = path;
}

void QSWidget::drop_event(GDropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.data_type() == "url-list") {
        auto lines = event.data().split_view('\n');
        if (lines.is_empty())
            return;
        if (lines.size() > 1) {
            GMessageBox::show("QuickShow can only open one file at a time!", "One at a time please!", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            return;
        }
        URL url(lines[0]);
        auto bitmap = GraphicsBitmap::load_from_file(url.path());
        if (!bitmap) {
            GMessageBox::show(String::format("Failed to open %s", url.to_string().characters()), "Cannot open image", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            return;
        }

        m_path = url.path();
        m_bitmap = bitmap;
        m_scale = 100;
        if (on_scale_change)
            on_scale_change(m_scale);
        relayout();
        m_bitmap_rect.center_within(rect());
    }
}
