#include <LibGUI/GVariant.h>

GVariant::~GVariant()
{
    switch (m_type) {
    case Type::String:
        if (m_value.as_string)
            m_value.as_string->release();
        break;
    case Type::Bitmap:
        if (m_value.as_bitmap)
            m_value.as_bitmap->release();
        break;
    default:
        break;
    }
}

GVariant::GVariant(int value)
    : m_type(Type::Int)
{
    m_value.as_int = value;
}

GVariant::GVariant(float value)
    : m_type(Type::Float)
{
    m_value.as_float = value;
}

GVariant::GVariant(bool value)
    : m_type(Type::Bool)
{
    m_value.as_bool = value;
}

GVariant::GVariant(const String& value)
    : m_type(Type::String)
{
    m_value.as_string = const_cast<StringImpl*>(value.impl());
    AK::retain_if_not_null(m_value.as_string);
}

GVariant::GVariant(const GraphicsBitmap& value)
    : m_type(Type::Bitmap)
{
    m_value.as_bitmap = const_cast<GraphicsBitmap*>(&value);
    AK::retain_if_not_null(m_value.as_bitmap);
}

String GVariant::to_string() const
{
    switch (m_type) {
    case Type::Bool:
        return as_bool() ? "True" : "False";
    case Type::Int:
        return String::format("%d", as_int());
    case Type::Float:
        return String::format("%f", as_float());
    case Type::String:
        return as_string();
    case Type::Bitmap:
        return "[GraphicsBitmap]";
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
}

