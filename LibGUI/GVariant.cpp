#include <LibGUI/GVariant.h>

GVariant::GVariant()
{
    m_value.as_string = nullptr;
}

GVariant::~GVariant()
{
    clear();
}

void GVariant::clear()
{
    switch (m_type) {
    case Type::String:
        AK::deref_if_not_null(m_value.as_string);
        break;
    case Type::Bitmap:
        AK::deref_if_not_null(m_value.as_bitmap);
        break;
    case Type::Icon:
        AK::deref_if_not_null(m_value.as_icon);
        break;
    default:
        break;
    }
    m_type = Type::Invalid;
    m_value.as_string = nullptr;
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
    AK::ref_if_not_null(m_value.as_string);
}

GVariant::GVariant(const GraphicsBitmap& value)
    : m_type(Type::Bitmap)
{
    m_value.as_bitmap = const_cast<GraphicsBitmap*>(&value);
    AK::ref_if_not_null(m_value.as_bitmap);
}

GVariant::GVariant(const GIcon& value)
    : m_type(Type::Icon)
{
    m_value.as_icon = &const_cast<GIconImpl&>(value.impl());
    AK::ref_if_not_null(m_value.as_icon);
}

GVariant::GVariant(Color color)
    : m_type(Type::Color)
{
    m_value.as_color = color.value();
}

GVariant::GVariant(const Point& point)
    : m_type(Type::Point)
{
    m_value.as_point = { point.x(), point.y() };
}

GVariant::GVariant(const Size& size)
    : m_type(Type::Size)
{
    m_value.as_size = { size.width(), size.height() };
}

GVariant::GVariant(const Rect& rect)
    : m_type(Type::Rect)
{
    m_value.as_rect = (const RawRect&)rect;
}

GVariant& GVariant::operator=(const GVariant& other)
{
    if (&other == this)
        return *this;
    clear();
    copy_from(other);
    return *this;
}

GVariant& GVariant::operator=(GVariant&& other)
{
    if (&other == this)
        return *this;
    // FIXME: Move, not copy!
    clear();
    copy_from(other);
    other.clear();
    return *this;
}

GVariant::GVariant(const GVariant& other)
{
    copy_from(other);
}

void GVariant::copy_from(const GVariant& other)
{
    ASSERT(!is_valid());
    m_type = other.m_type;
    switch (m_type) {
    case Type::Bool:
        m_value.as_bool = other.m_value.as_bool;
        break;
    case Type::Int:
        m_value.as_int = other.m_value.as_int;
        break;
    case Type::Float:
        m_value.as_float = other.m_value.as_float;
        break;
    case Type::String:
        m_value.as_string = other.m_value.as_string;
        AK::ref_if_not_null(m_value.as_bitmap);
        break;
    case Type::Bitmap:
        m_value.as_bitmap = other.m_value.as_bitmap;
        AK::ref_if_not_null(m_value.as_bitmap);
        break;
    case Type::Icon:
        m_value.as_icon = other.m_value.as_icon;
        AK::ref_if_not_null(m_value.as_icon);
        break;
    case Type::Color:
        m_value.as_color = other.m_value.as_color;
        break;
    case Type::Point:
        m_value.as_point = other.m_value.as_point;
        break;
    case Type::Size:
        m_value.as_size = other.m_value.as_size;
        break;
    case Type::Rect:
        m_value.as_rect = other.m_value.as_rect;
        break;
    case Type::Invalid:
        break;
    }
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
    case Type::Point:
        return as_point() == other.as_point();
    case Type::Size:
        return as_size() == other.as_size();
    case Type::Rect:
        return as_rect() == other.as_rect();
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
    case Type::Point:
    case Type::Size:
    case Type::Rect:
        // FIXME: Figure out how to compare these.
        ASSERT_NOT_REACHED();
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
}

String GVariant::to_string() const
{
    switch (m_type) {
    case Type::Bool:
        return as_bool() ? "true" : "false";
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
    case Type::Point:
        return as_point().to_string();
    case Type::Size:
        return as_size().to_string();
    case Type::Rect:
        return as_rect().to_string();
    case Type::Invalid:
        return "[null]";
        break;
    }
    ASSERT_NOT_REACHED();
}
