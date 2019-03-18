#pragma once

#include <AK/AKString.h>
#include <SharedGraphics/GraphicsBitmap.h>

class GVariant {
public:
    GVariant();
    GVariant(bool);
    GVariant(float);
    GVariant(int);
    GVariant(const String&);
    GVariant(const GraphicsBitmap&);
    GVariant(Color);
    ~GVariant();

    enum class Type {
        Invalid,
        Bool,
        Int,
        Float,
        String,
        Bitmap,
        Color,
    };

    bool is_valid() const { return m_type != Type::Invalid; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_int() const { return m_type == Type::Int; }
    bool is_float() const { return m_type == Type::Float; }
    bool is_string() const { return m_type == Type::String; }
    bool is_bitmap() const { return m_type == Type::Bitmap; }
    bool is_color() const { return m_type == Type::Color; }
    Type type() const { return m_type; }

    bool as_bool() const
    {
        ASSERT(type() == Type::Bool);
        return m_value.as_bool;
    }

    int as_int() const
    {
        ASSERT(type() == Type::Int);
        return m_value.as_int;
    }

    float as_float() const
    {
        ASSERT(type() == Type::Float);
        return m_value.as_float;
    }

    String as_string() const
    {
        ASSERT(type() == Type::String);
        return *m_value.as_string;
    }

    const GraphicsBitmap& as_bitmap() const
    {
        ASSERT(type() == Type::Bitmap);
        return *m_value.as_bitmap;
    }

    Color as_color() const
    {
        ASSERT(type() == Type::Color);
        return Color::from_rgba(m_value.as_color);
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
    union {
        StringImpl* as_string;
        GraphicsBitmap* as_bitmap;
        bool as_bool;
        int as_int;
        float as_float;
        RGBA32 as_color;
    } m_value; 

    Type m_type { Type::Invalid };
};
