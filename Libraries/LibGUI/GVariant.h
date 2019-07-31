#pragma once

#include <AK/AKString.h>
#include <LibGUI/GIcon.h>
#include <LibDraw/GraphicsBitmap.h>

namespace AK {
class JsonValue;
}

class GVariant {
public:
    GVariant();
    GVariant(bool);
    GVariant(float);
    GVariant(int);
    GVariant(unsigned);
    GVariant(const char*);
    GVariant(const String&);
    GVariant(const GraphicsBitmap&);
    GVariant(const GIcon&);
    GVariant(const Point&);
    GVariant(const Size&);
    GVariant(const Rect&);
    GVariant(const AK::JsonValue&);
    GVariant(Color);

    GVariant(const GVariant&);
    GVariant& operator=(const GVariant&);

    GVariant(GVariant&&) = delete;
    GVariant& operator=(GVariant&&);

    void clear();
    ~GVariant();

    enum class Type {
        Invalid,
        Bool,
        Int,
        UnsignedInt,
        Float,
        String,
        Bitmap,
        Color,
        Icon,
        Point,
        Size,
        Rect,
    };

    bool is_valid() const { return m_type != Type::Invalid; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_int() const { return m_type == Type::Int; }
    bool is_uint() const { return m_type == Type::UnsignedInt; }
    bool is_float() const { return m_type == Type::Float; }
    bool is_string() const { return m_type == Type::String; }
    bool is_bitmap() const { return m_type == Type::Bitmap; }
    bool is_color() const { return m_type == Type::Color; }
    bool is_icon() const { return m_type == Type::Icon; }
    bool is_point() const { return m_type == Type::Point; }
    bool is_size() const { return m_type == Type::Size; }
    bool is_rect() const { return m_type == Type::Rect; }
    Type type() const { return m_type; }

    bool as_bool() const
    {
        ASSERT(type() == Type::Bool);
        return m_value.as_bool;
    }

    bool to_bool() const
    {
        if (type() == Type::Bool)
            return as_bool();
        if (type() == Type::String)
            return !!m_value.as_string;
        if (type() == Type::Int)
            return m_value.as_int != 0;
        if (type() == Type::UnsignedInt)
            return m_value.as_uint != 0;
        if (type() == Type::Rect)
            return !as_rect().is_null();
        if (type() == Type::Size)
            return !as_size().is_null();
        if (type() == Type::Point)
            return !as_point().is_null();
        return is_valid();
    }

    int as_int() const
    {
        ASSERT(type() == Type::Int);
        return m_value.as_int;
    }

    unsigned as_uint() const
    {
        ASSERT(type() == Type::UnsignedInt);
        return m_value.as_uint;
    }

    int to_int() const
    {
        if (is_int())
            return as_int();
        if (is_bool())
            return as_bool() ? 1 : 0;
        if (is_float())
            return (int)as_float();
        if (is_uint()) {
            ASSERT(as_uint() <= INT32_MAX);
            return (int)as_uint();
        }
        if (is_string()) {
            bool ok;
            int value = as_string().to_int(ok);
            if (!ok)
                return 0;
            return value;
        }
        return 0;
    }

    float as_float() const
    {
        ASSERT(type() == Type::Float);
        return m_value.as_float;
    }

    Point as_point() const
    {
        return { m_value.as_point.x, m_value.as_point.y };
    }

    Size as_size() const
    {
        return { m_value.as_size.width, m_value.as_size.height };
    }

    Rect as_rect() const
    {
        return { as_point(), as_size() };
    }

    String as_string() const
    {
        ASSERT(type() == Type::String);
        return m_value.as_string;
    }

    const GraphicsBitmap& as_bitmap() const
    {
        ASSERT(type() == Type::Bitmap);
        return *m_value.as_bitmap;
    }

    GIcon as_icon() const
    {
        ASSERT(type() == Type::Icon);
        return GIcon(*m_value.as_icon);
    }

    Color as_color() const
    {
        ASSERT(type() == Type::Color);
        return Color::from_rgba(m_value.as_color);
    }

    Color to_color() const
    {
        if (is_color())
            return as_color();
        return Color();
    }

    Color to_color(Color default_value) const
    {
        if (type() == Type::Color)
            return as_color();
        return default_value;
    }

    String to_string() const;

    bool operator==(const GVariant&) const;
    bool operator<(const GVariant&) const;

private:
    void copy_from(const GVariant&);

    struct RawPoint {
        int x;
        int y;
    };

    struct RawSize {
        int width;
        int height;
    };

    struct RawRect {
        RawPoint location;
        RawSize size;
    };

    union {
        StringImpl* as_string;
        GraphicsBitmap* as_bitmap;
        GIconImpl* as_icon;
        bool as_bool;
        int as_int;
        unsigned as_uint;
        float as_float;
        RGBA32 as_color;
        RawPoint as_point;
        RawSize as_size;
        RawRect as_rect;
    } m_value;

    Type m_type { Type::Invalid };
};

const char* to_string(GVariant::Type);
