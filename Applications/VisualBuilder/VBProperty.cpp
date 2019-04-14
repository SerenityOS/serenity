#include "VBProperty.h"

VBProperty::VBProperty(const String& name, const GVariant& value)
    : m_name(name)
    , m_value(value)
{
}

VBProperty::VBProperty(const String& name, Function<GVariant(const GWidget&)>&& getter, Function<void(GWidget&, const GVariant&)>&& setter)
    : m_name(name)
    , m_getter(move(getter))
    , m_setter(move(setter))
{
    ASSERT(m_getter);
    ASSERT(m_setter);
}

VBProperty::~VBProperty()
{
}
