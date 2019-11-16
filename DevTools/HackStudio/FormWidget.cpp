#include "FormWidget.h"
#include "FormEditorWidget.h"
#include "Tool.h"
#include <LibGUI/GPainter.h>

FormWidget::FormWidget(FormEditorWidget& parent)
    : GWidget(&parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::WarmGray);
    set_relative_rect(5, 5, 400, 300);

    set_greedy_for_hits(true);
}

FormWidget::~FormWidget()
{
}

FormEditorWidget& FormWidget::editor()
{
    return static_cast<FormEditorWidget&>(*parent());
}

const FormEditorWidget& FormWidget::editor() const
{
    return static_cast<const FormEditorWidget&>(*parent());
}

void FormWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::from_rgb(0x404040));
        }
    }
}

void FormWidget::second_paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    if (!editor().selection().is_empty()) {
        for_each_child_widget([&](auto& child) {
            if (editor().selection().contains(child)) {
                painter.draw_rect(child.relative_rect(), Color::Blue);
            }
            return IterationDecision::Continue;
        });
    }

    if (m_rubber_banding)
        painter.draw_rect(rubber_band_rect(), Color::MidMagenta);
}

void FormWidget::mousedown_event(GMouseEvent& event)
{
    editor().tool().on_mousedown(event);
}

void FormWidget::mouseup_event(GMouseEvent& event)
{
    editor().tool().on_mouseup(event);
}

void FormWidget::mousemove_event(GMouseEvent& event)
{
    editor().tool().on_mousemove(event);
}

void FormWidget::keydown_event(GKeyEvent& event)
{
    editor().tool().on_keydown(event);
}

void FormWidget::set_rubber_banding(Badge<CursorTool>, bool value)
{
    if (m_rubber_banding == value)
        return;
    m_rubber_banding = value;
    update();
}

void FormWidget::set_rubber_band_origin(Badge<CursorTool>, const Point& origin)
{
    if (m_rubber_band_origin == origin)
        return;
    m_rubber_band_origin = origin;
    m_rubber_band_position = origin;
    update();
}

void FormWidget::set_rubber_band_position(Badge<CursorTool>, const Point& position)
{
    if (m_rubber_band_position == position)
        return;
    m_rubber_band_position = position;

    auto rubber_band_rect = this->rubber_band_rect();

    editor().selection().clear();
    for_each_child_widget([&](auto& child) {
        if (child.relative_rect().intersects(rubber_band_rect))
            editor().selection().add(child);
        return IterationDecision::Continue;
    });

    update();
}

static Rect rect_from_two_points(const Point& a, const Point& b)
{
    if (a.x() <= b.x()) {
        if (a.y() <= b.y())
            return { a, { b.x() - a.x(), b.y() - a.y() } };
        int height = a.y() - b.y();
        return { a.x(), a.y() - height, b.x() - a.x(), height };
    }
    if (a.y() >= b.y())
        return { b, { a.x() - b.x(), a.y() - b.y() } };
    int height = b.y() - a.y();
    return { b.x(), b.y() - height, a.x() - b.x(), height };
}

Rect FormWidget::rubber_band_rect() const
{
    if (!m_rubber_banding)
        return {};
    return rect_from_two_points(m_rubber_band_origin, m_rubber_band_position);
}
