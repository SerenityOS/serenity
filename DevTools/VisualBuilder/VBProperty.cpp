#include "VBProperty.h"
#include "VBWidget.h"

VBProperty::VBProperty(VBWidget& widget, const String& name, const GVariant& value)
    : m_widget(widget)
    , m_name(name)
    , m_value(value)
{
}

VBProperty::VBProperty(VBWidget& widget, const String& name, Function<GVariant(const GWidget&)>&& getter, Function<void(GWidget&, const GVariant&)>&& setter)
    : m_widget(widget)
    , m_name(name)
    , m_getter(move(getter))
    , m_setter(move(setter))
{
    ASSERT(m_getter);
    ASSERT(m_setter);
}

VBProperty::~VBProperty()
{
}

void VBProperty::set_value(const GVariant& value)
{
    if (m_value == value)
        return;
    m_value = value;
    if (m_setter)
        m_setter(*m_widget.gwidget(), value);
    m_widget.property_did_change();
}
