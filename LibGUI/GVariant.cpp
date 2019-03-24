#include <LibGUI/GVariant.h>

GVariant::GVariant()
{
}

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
    case Type::Icon:
        if (m_value.as_icon)
            m_value.as_icon->release();
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

GVariant::GVariant(const GIcon& value)
    : m_type(Type::Icon)
{
    m_value.as_icon = &const_cast<GIconImpl&>(value.impl());
    AK::retain_if_not_null(m_value.as_icon);
}

GVariant::GVariant(Color color)
    : m_type(Type::Color)
{
    m_value.as_color = color.value();
}

bool GVariant::operator==(const GVariant& other) const
{
    if (m_type != other.m_type)
        return to_string() == other.to_string();
    switch (m_type) {
    case Type::Bool:
        return as_bool() == other.as_bool();
    case Type::Int:
        return as_int() == other.as_int();
    case Type::Float:
        return as_float() == other.as_float();
    case Type::String:
        return as_string() == other.as_string();
    case Type::Bitmap:
        return m_value.as_bitmap == other.m_value.as_bitmap;
    case Type::Icon:
        return m_value.as_icon == other.m_value.as_icon;
    case Type::Color:
        return m_value.as_color == other.m_value.as_color;
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
}

bool GVariant::operator<(const GVariant& other) const
{
    if (m_type != other.m_type)
        return to_string() < other.to_string();
    switch (m_type) {
    case Type::Bool:
        return as_bool() < other.as_bool();
    case Type::Int:
        return as_int() < other.as_int();
    case Type::Float:
        return as_float() < other.as_float();
    case Type::String:
        return as_string() < other.as_string();
    case Type::Bitmap:
        // FIXME: Maybe compare bitmaps somehow differently?
        return m_value.as_bitmap < other.m_value.as_bitmap;
    case Type::Icon:
        // FIXME: Maybe compare icons somehow differently?
        return m_value.as_icon < other.m_value.as_icon;
    case Type::Color:
        return m_value.as_color < other.m_value.as_color;
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
}

String GVariant::to_string() const
{
    switch (m_type) {
    case Type::Bool:
        return as_bool() ? "True" : "False";
    case Type::Int:
        return String::format("%d", as_int());
    case Type::Float:
        return String::format("%f", (double)as_float());
    case Type::String:
        return as_string();
    case Type::Bitmap:
        return "[GraphicsBitmap]";
    case Type::Icon:
        return "[GIcon]";
    case Type::Color:
        return as_color().to_string();
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
}

