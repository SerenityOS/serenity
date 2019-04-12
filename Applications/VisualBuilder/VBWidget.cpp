#include "VBWidget.h"
#include "VBForm.h"
#include "VBProperty.h"
#include "VBWidgetRegistry.h"
#include "VBWidgetPropertyModel.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GProgressBar.h>

VBWidget::VBWidget(VBWidgetType type, VBForm& form)
    : m_type(type)
    , m_form(form)
    , m_property_model(VBWidgetPropertyModel::create(*this))
{
    m_gwidget = VBWidgetRegistry::build_gwidget(type, &form, m_properties);
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
    if (rect == m_gwidget->relative_rect())
        return;
    m_gwidget->set_relative_rect(rect);
    synchronize_properties();
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

void VBWidget::for_each_property(Function<void(VBProperty&)> callback)
{
    for (auto& it : m_properties) {
        callback(*it);
    }
}

void VBWidget::synchronize_properties()
{
    property("width").set_value(m_gwidget->width());
    property("height").set_value(m_gwidget->height());
    property("x").set_value(m_gwidget->x());
    property("y").set_value(m_gwidget->y());
    property("visible").set_value(m_gwidget->is_visible());
    property("enabled").set_value(m_gwidget->is_enabled());
    property("tooltip").set_value(m_gwidget->tooltip());
    property("background_color").set_value(m_gwidget->background_color());
    property("foreground_color").set_value(m_gwidget->foreground_color());

    if (m_type == VBWidgetType::GLabel) {
        auto& widget = *static_cast<GLabel*>(m_gwidget);
        property("text").set_value(widget.text());
    }

    if (m_type == VBWidgetType::GButton) {
        auto& widget = *static_cast<GButton*>(m_gwidget);
        property("caption").set_value(widget.caption());
    }

    if (m_type == VBWidgetType::GScrollBar) {
        auto& widget = *static_cast<GScrollBar*>(m_gwidget);
        property("min").set_value(widget.min());
        property("max").set_value(widget.max());
        property("value").set_value(widget.value());
        property("step").set_value(widget.step());
    }

    if (m_type == VBWidgetType::GSpinBox) {
        auto& widget = *static_cast<GSpinBox*>(m_gwidget);
        property("min").set_value(widget.min());
        property("max").set_value(widget.max());
        property("value").set_value(widget.value());
    }

    if (m_type == VBWidgetType::GProgressBar) {
        auto& widget = *static_cast<GProgressBar*>(m_gwidget);
        property("min").set_value(widget.min());
        property("max").set_value(widget.max());
        property("value").set_value(widget.value());
    }

    if (m_type == VBWidgetType::GTextEditor) {
        auto& widget = *static_cast<GTextEditor*>(m_gwidget);
        property("text").set_value(widget.text());
        property("ruler_visible").set_value(widget.is_ruler_visible());
    }

    m_property_model->update();
}

VBProperty& VBWidget::property(const String& name)
{
    for (auto& prop : m_properties) {
        if (prop->name() == name)
            return *prop;
    }
    m_properties.append(make<VBProperty>(name, GVariant()));
    return *m_properties.last();
}
