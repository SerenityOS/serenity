#include "VBProperty.h"

VBProperty::VBProperty(const String& name, const GVariant& value)
    : m_name(name)
    , m_value(value)
{
}

VBProperty::~VBProperty()
{
}
