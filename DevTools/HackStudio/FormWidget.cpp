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

    editor().tool().on_second_paint(painter, event);
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
