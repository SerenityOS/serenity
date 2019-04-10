#include "VBForm.h"
#include "VBWidget.h"
#include <LibGUI/GPainter.h>

VBForm::VBForm(const String& name, GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);

    auto box1 = VBWidget::create(*this);
    box1->set_rect({ 10, 10, 61, 41 });
    m_widgets.append(move(box1));

    auto box2 = VBWidget::create(*this);
    box2->set_rect({ 100, 100, 161, 141 });
    m_widgets.append(move(box2));
}

VBForm::~VBForm()
{
}

void VBForm::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::Black);
        }
    }

    for (auto& widget : m_widgets) {
        widget->paint(painter);
    }
}

bool VBForm::is_selected(const VBWidget& widget) const
{
    return &widget == m_selected_widget.ptr();
}

VBWidget* VBForm::widget_at(const Point& position)
{
    for (int i = m_widgets.size() - 1; i >= 0; --i) {
        auto& widget = *m_widgets[i];
        if (widget.rect().contains(position))
            return &widget;
    }
    return nullptr;
}

void VBForm::mousedown_event(GMouseEvent& event)
{
    auto* widget = widget_at(event.position());
    if (!widget) {
        if (m_selected_widget) {
            m_selected_widget = nullptr;
            update();
        }
        return;
    }
    if (event.button() == GMouseButton::Left) {
        m_selected_widget = widget->make_weak_ptr();
        m_transform_event_origin = event.position();
        m_transform_widget_origin_rect = widget->rect();
        update();
    }
}

void VBForm::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() & GMouseButton::Left && m_selected_widget) {
        auto delta = event.position() - m_transform_event_origin;
        auto new_rect = m_transform_widget_origin_rect.translated(delta);
        new_rect.set_x(new_rect.x() - (new_rect.x() % m_grid_size));
        new_rect.set_y(new_rect.y() - (new_rect.y() % m_grid_size));
        m_selected_widget->set_rect(new_rect);
        update();
    }
}

void VBForm::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        m_transform_event_origin = { };
        m_transform_widget_origin_rect = { };
    }
}
