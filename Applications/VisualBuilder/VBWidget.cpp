#include "VBWidget.h"
#include "VBForm.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GProgressBar.h>

static GWidget* build_gwidget(WidgetType type, GWidget* parent)
{
    switch (type) {
    case WidgetType::GWidget:
        return new GWidget(parent);
    case WidgetType::GLabel:
        return new GLabel(parent);
    case WidgetType::GButton:
        return new GButton(parent);
    case WidgetType::GSpinBox:
        return new GSpinBox(parent);
    case WidgetType::GTextEditor: {
        auto* editor = new GTextEditor(GTextEditor::Type::MultiLine, parent);
        editor->set_ruler_visible(false);
        return editor;
    }
    case WidgetType::GProgressBar: {
        auto* bar = new GProgressBar(parent);
        bar->set_format(GProgressBar::Format::NoText);
        bar->set_range(0, 100);
        bar->set_value(50);
        return bar;
    }
    default:
        ASSERT_NOT_REACHED();
        return nullptr;
    }
}

VBWidget::VBWidget(WidgetType type, VBForm& form)
    : m_type(type)
    , m_form(form)
{
    m_gwidget = build_gwidget(type, &form);
}

VBWidget::~VBWidget()
{
}

Rect VBWidget::rect() const
{
    return m_gwidget->relative_rect();
}

void VBWidget::set_rect(const Rect& rect)
{
    m_gwidget->set_relative_rect(rect);
}

bool VBWidget::is_selected() const
{
    return m_form.is_selected(*this);
}

Rect VBWidget::grabber_rect(Direction direction) const
{
    int grabber_size = 5;
    int half_grabber_size = grabber_size / 2;
    switch (direction) {
    case Direction::Left:
        return { rect().x() - half_grabber_size, rect().center().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::UpLeft:
        return { rect().x() - half_grabber_size, rect().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Up:
        return { rect().center().x() - half_grabber_size, rect().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::UpRight:
        return { rect().right() - half_grabber_size, rect().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Right:
        return { rect().right() - half_grabber_size, rect().center().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::DownLeft:
        return { rect().x() - half_grabber_size, rect().bottom() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Down:
        return { rect().center().x() - half_grabber_size, rect().bottom() - half_grabber_size, grabber_size, grabber_size };
    case Direction::DownRight:
        return { rect().right() - half_grabber_size, rect().bottom() - half_grabber_size, grabber_size, grabber_size };
    default:
        ASSERT_NOT_REACHED();
    }
}

Direction VBWidget::grabber_at(const Point& position) const
{
    Direction found_grabber = Direction::None;
    for_each_direction([&] (Direction direction) {
        if (grabber_rect(direction).contains(position))
            found_grabber = direction;
    });
    return found_grabber;
}
