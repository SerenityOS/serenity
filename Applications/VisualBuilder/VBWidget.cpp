#include "VBWidget.h"
#include "VBForm.h"
#include "VBProperty.h"
#include "VBWidgetRegistry.h"
#include "VBWidgetPropertyModel.h"
#include <LibGUI/GPainter.h>

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
    property_by_name("width")->set_value(m_gwidget->width());
    property_by_name("height")->set_value(m_gwidget->height());
    property_by_name("x")->set_value(m_gwidget->x());
    property_by_name("y")->set_value(m_gwidget->y());
    property_by_name("visible")->set_value(m_gwidget->is_visible());
    property_by_name("enabled")->set_value(m_gwidget->is_enabled());
    property_by_name("tooltip")->set_value(m_gwidget->tooltip());
    property_by_name("background_color")->set_value(m_gwidget->background_color());
    property_by_name("foreground_color")->set_value(m_gwidget->foreground_color());
    m_property_model->update();
}

VBProperty* VBWidget::property_by_name(const String& name)
{
    for (auto& property : m_properties) {
        if (property->name() == name)
            return property.ptr();
    }
    return nullptr;
}
