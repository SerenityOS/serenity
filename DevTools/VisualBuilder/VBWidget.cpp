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
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GSlider.h>

VBWidget::VBWidget(VBWidgetType type, VBForm& form)
    : m_type(type)
    , m_form(form)
    , m_property_model(VBWidgetPropertyModel::create(*this))
{
    m_gwidget = VBWidgetRegistry::build_gwidget(*this, type, &form, m_properties);
    m_form.m_gwidget_map.set(m_gwidget, this);
    setup_properties();
}

VBWidget::~VBWidget()
{
    m_form.m_gwidget_map.remove(m_gwidget);
    m_form.m_selected_widgets.remove(this);
    delete m_gwidget;
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

void VBWidget::add_property(const String& name, Function<GVariant(const GWidget&)>&& getter, Function<void(GWidget&, const GVariant&)>&& setter)
{
    auto& prop = property(name);
    prop.m_getter = move(getter);
    prop.m_setter = move(setter);
}

#define VB_ADD_PROPERTY(gclass, name, getter, setter, variant_type) \
    add_property(name, \
        [] (auto& widget) -> GVariant { return ((const gclass&)widget).getter(); }, \
        [] (auto& widget, auto& value) { ((gclass&)widget).setter(value.to_ ## variant_type()); } \
    )

void VBWidget::setup_properties()
{
    VB_ADD_PROPERTY(GWidget, "width", width, set_width, int);
    VB_ADD_PROPERTY(GWidget, "height", height, set_height, int);
    VB_ADD_PROPERTY(GWidget, "x", x, set_x, int);
    VB_ADD_PROPERTY(GWidget, "y", y, set_y, int);
    VB_ADD_PROPERTY(GWidget, "visible", is_visible, set_visible, bool);
    VB_ADD_PROPERTY(GWidget, "enabled", is_enabled, set_enabled, bool);
    VB_ADD_PROPERTY(GWidget, "tooltip", tooltip, set_tooltip, string);
    VB_ADD_PROPERTY(GWidget, "backcolor", background_color, set_background_color, color);
    VB_ADD_PROPERTY(GWidget, "forecolor", foreground_color, set_foreground_color, color);
    VB_ADD_PROPERTY(GWidget, "autofill", fill_with_background_color, set_fill_with_background_color, bool);

    if (m_type == VBWidgetType::GLabel) {
        VB_ADD_PROPERTY(GLabel, "text", text, set_text, string);
    }

    if (m_type == VBWidgetType::GButton) {
        VB_ADD_PROPERTY(GButton, "caption", caption, set_caption, string);
    }

    if (m_type == VBWidgetType::GGroupBox) {
        VB_ADD_PROPERTY(GGroupBox, "title", title, set_title, string);
    }

    if (m_type == VBWidgetType::GScrollBar) {
        VB_ADD_PROPERTY(GScrollBar, "min", min, set_min, int);
        VB_ADD_PROPERTY(GScrollBar, "max", max, set_max, int);
        VB_ADD_PROPERTY(GScrollBar, "value", value, set_value, int);
        VB_ADD_PROPERTY(GScrollBar, "step", step, set_step, int);
    }

    if (m_type == VBWidgetType::GSpinBox) {
        VB_ADD_PROPERTY(GSpinBox, "min", min, set_min, int);
        VB_ADD_PROPERTY(GSpinBox, "max", max, set_max, int);
        VB_ADD_PROPERTY(GSpinBox, "value", value, set_value, int);
    }

    if (m_type == VBWidgetType::GProgressBar) {
        VB_ADD_PROPERTY(GProgressBar, "min", min, set_min, int);
        VB_ADD_PROPERTY(GProgressBar, "max", max, set_max, int);
        VB_ADD_PROPERTY(GProgressBar, "value", value, set_value, int);
    }

    if (m_type == VBWidgetType::GSlider) {
        VB_ADD_PROPERTY(GSlider, "min", min, set_min, int);
        VB_ADD_PROPERTY(GSlider, "max", max, set_max, int);
        VB_ADD_PROPERTY(GSlider, "value", value, set_value, int);
    }

    if (m_type == VBWidgetType::GTextEditor) {
        VB_ADD_PROPERTY(GTextEditor, "text", text, set_text, string);
        VB_ADD_PROPERTY(GTextEditor, "ruler_visible", is_ruler_visible, set_ruler_visible, bool);
    }

    if (m_type == VBWidgetType::GCheckBox) {
        VB_ADD_PROPERTY(GCheckBox, "caption", caption, set_caption, string);
        VB_ADD_PROPERTY(GCheckBox, "checked", is_checked, set_checked, bool);
    }
}

void VBWidget::synchronize_properties()
{
    for (auto& prop : m_properties) {
        if (prop->m_getter)
            prop->m_value = prop->m_getter(*gwidget());
    }

    m_property_model->update();
}

VBProperty& VBWidget::property(const String& name)
{
    for (auto& prop : m_properties) {
        if (prop->name() == name)
            return *prop;
    }
    m_properties.append(make<VBProperty>(*this, name, GVariant()));
    return *m_properties.last();
}

void VBWidget::property_did_change()
{
    m_form.update();
}

void VBWidget::capture_transform_origin_rect()
{
    m_transform_origin_rect = rect();
}
