/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <AK/JsonValue.h>
#include <AK/RefPtr.h>
#include <LibGUI/Variant.h>

namespace GUI {

const char* to_string(Variant::Type type)
{
    switch (type) {
    case Variant::Type::Invalid:
        return "Invalid";
    case Variant::Type::Bool:
        return "Bool";
    case Variant::Type::Int32:
        return "Int32";
    case Variant::Type::Int64:
        return "Int64";
    case Variant::Type::UnsignedInt32:
        return "UnsignedInt32";
    case Variant::Type::UnsignedInt64:
        return "UnsignedInt64";
    case Variant::Type::Float:
        return "Float";
    case Variant::Type::String:
        return "String";
    case Variant::Type::Bitmap:
        return "Bitmap";
    case Variant::Type::Color:
        return "Color";
    case Variant::Type::Icon:
        return "Icon";
    case Variant::Type::Point:
        return "Point";
    case Variant::Type::Size:
        return "Size";
    case Variant::Type::Rect:
        return "Rect";
    case Variant::Type::Font:
        return "Font";
    case Variant::Type::TextAlignment:
        return "TextAlignment";
    case Variant::Type::ColorRole:
        return "ColorRole";
    case Variant::Type::FlagRole:
        return "FlagRole";
    case Variant::Type::MetricRole:
        return "MetricRole";
    case Variant::Type::PathRole:
        return "PathRole";
    }
    VERIFY_NOT_REACHED();
}

Variant::Variant()
{
    m_value.as_string = nullptr;
}

Variant::~Variant()
{
    clear();
}

void Variant::clear()
{
    switch (m_type) {
    case Type::String:
        AK::unref_if_not_null(m_value.as_string);
        break;
    case Type::Bitmap:
        AK::unref_if_not_null(m_value.as_bitmap);
        break;
    case Type::Icon:
        AK::unref_if_not_null(m_value.as_icon);
        break;
    default:
        break;
    }
    m_type = Type::Invalid;
    m_value.as_string = nullptr;
}

Variant::Variant(Gfx::TextAlignment value)
    : m_type(Type::TextAlignment)
{
    m_value.as_text_alignment = value;
}

Variant::Variant(Gfx::ColorRole value)
    : m_type(Type::ColorRole)
{
    m_value.as_color_role = value;
}

Variant::Variant(Gfx::FlagRole value)
    : m_type(Type::FlagRole)
{
    m_value.as_flag_role = value;
}

Variant::Variant(Gfx::MetricRole value)
    : m_type(Type::MetricRole)
{
    m_value.as_metric_role = value;
}

Variant::Variant(Gfx::PathRole value)
    : m_type(Type::PathRole)
{
    m_value.as_path_role = value;
}

Variant::Variant(i32 value)
    : m_type(Type::Int32)
{
    m_value.as_i32 = value;
}

Variant::Variant(i64 value)
    : m_type(Type::Int64)
{
    m_value.as_i64 = value;
}

Variant::Variant(u32 value)
    : m_type(Type::UnsignedInt32)
{
    m_value.as_u32 = value;
}

Variant::Variant(u64 value)
    : m_type(Type::UnsignedInt64)
{
    m_value.as_u64 = value;
}

Variant::Variant(float value)
    : m_type(Type::Float)
{
    m_value.as_float = value;
}

Variant::Variant(bool value)
    : m_type(Type::Bool)
{
    m_value.as_bool = value;
}

Variant::Variant(const char* cstring)
    : Variant(String(cstring))
{
}

Variant::Variant(const FlyString& value)
    : Variant(String(value.impl()))
{
}

Variant::Variant(StringView value)
    : Variant(value.to_string())
{
}

Variant::Variant(const String& value)
    : m_type(Type::String)
{
    m_value.as_string = const_cast<StringImpl*>(value.impl());
    AK::ref_if_not_null(m_value.as_string);
}

Variant::Variant(const JsonValue& value)
{
    if (value.is_null()) {
        m_value.as_string = nullptr;
        return;
    }

    if (value.is_i32()) {
        m_type = Type::Int32;
        m_value.as_i32 = value.as_i32();
        return;
    }

    if (value.is_u32()) {
        m_type = Type::UnsignedInt32;
        m_value.as_u32 = value.as_u32();
        return;
    }

    if (value.is_i64()) {
        m_type = Type::Int64;
        m_value.as_i64 = value.as_i64();
        return;
    }

    if (value.is_u64()) {
        m_type = Type::UnsignedInt64;
        m_value.as_u64 = value.to_u64();
        return;
    }

    if (value.is_string()) {
        m_type = Type::String;
        m_value.as_string = value.as_string().impl();
        m_value.as_string->ref();
        return;
    }

    if (value.is_bool()) {
        m_type = Type::Bool;
        m_value.as_bool = value.as_bool();
        return;
    }

    VERIFY_NOT_REACHED();
}

Variant::Variant(const Gfx::Bitmap& value)
    : m_type(Type::Bitmap)
{
    m_value.as_bitmap = const_cast<Gfx::Bitmap*>(&value);
    AK::ref_if_not_null(m_value.as_bitmap);
}

Variant::Variant(const GUI::Icon& value)
    : m_type(Type::Icon)
{
    m_value.as_icon = &const_cast<GUI::IconImpl&>(value.impl());
    AK::ref_if_not_null(m_value.as_icon);
}

Variant::Variant(const Gfx::Font& value)
    : m_type(Type::Font)
{
    m_value.as_font = &const_cast<Gfx::Font&>(value);
    AK::ref_if_not_null(m_value.as_font);
}

Variant::Variant(Color color)
    : m_type(Type::Color)
{
    m_value.as_color = color.value();
}

Variant::Variant(const Gfx::IntPoint& point)
    : m_type(Type::Point)
{
    m_value.as_point = { point.x(), point.y() };
}

Variant::Variant(const Gfx::IntSize& size)
    : m_type(Type::Size)
{
    m_value.as_size = { size.width(), size.height() };
}

Variant::Variant(const Gfx::IntRect& rect)
    : m_type(Type::Rect)
{
    m_value.as_rect = (const RawRect&)rect;
}

Variant& Variant::operator=(const Variant& other)
{
    if (&other == this)
        return *this;
    clear();
    copy_from(other);
    return *this;
}

Variant& Variant::operator=(Variant&& other)
{
    if (&other == this)
        return *this;
    clear();
    move_from(move(other));
    return *this;
}

Variant::Variant(const Variant& other)
{
    copy_from(other);
}

void Variant::move_from(Variant&& other)
{
    m_type = other.m_type;
    m_value = other.m_value;
    other.m_type = Type::Invalid;
    other.m_value.as_string = nullptr;
}

void Variant::copy_from(const Variant& other)
{
    VERIFY(!is_valid());
    m_type = other.m_type;
    switch (m_type) {
    case Type::Bool:
        m_value.as_bool = other.m_value.as_bool;
        break;
    case Type::Int32:
        m_value.as_i32 = other.m_value.as_i32;
        break;
    case Type::Int64:
        m_value.as_i64 = other.m_value.as_i64;
        break;
    case Type::UnsignedInt32:
        m_value.as_u32 = other.m_value.as_u32;
        break;
    case Type::UnsignedInt64:
        m_value.as_u64 = other.m_value.as_u64;
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
    case Type::Font:
        m_value.as_font = other.m_value.as_font;
        AK::ref_if_not_null(m_value.as_font);
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
    case Type::TextAlignment:
        m_value.as_text_alignment = other.m_value.as_text_alignment;
        break;
    case Type::ColorRole:
        m_value.as_color_role = other.m_value.as_color_role;
        break;
    case Type::FlagRole:
        m_value.as_flag_role = other.m_value.as_flag_role;
        break;
    case Type::MetricRole:
        m_value.as_metric_role = other.m_value.as_metric_role;
        break;
    case Type::PathRole:
        m_value.as_path_role = other.m_value.as_path_role;
        break;
    case Type::Invalid:
        break;
    }
}

bool Variant::operator==(const Variant& other) const
{
    if (m_type != other.m_type)
        return to_string() == other.to_string();
    switch (m_type) {
    case Type::Bool:
        return as_bool() == other.as_bool();
    case Type::Int32:
        return as_i32() == other.as_i32();
    case Type::Int64:
        return as_i64() == other.as_i64();
    case Type::UnsignedInt32:
        return as_u32() == other.as_u32();
    case Type::UnsignedInt64:
        return as_u64() == other.as_u64();
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
    case Type::Font:
        return &as_font() == &other.as_font();
    case Type::TextAlignment:
        return m_value.as_text_alignment == other.m_value.as_text_alignment;
    case Type::ColorRole:
        return m_value.as_color_role == other.m_value.as_color_role;
    case Type::FlagRole:
        return m_value.as_flag_role == other.m_value.as_flag_role;
    case Type::MetricRole:
        return m_value.as_metric_role == other.m_value.as_metric_role;
    case Type::PathRole:
        return m_value.as_path_role == other.m_value.as_path_role;
    case Type::Invalid:
        return true;
    }
    VERIFY_NOT_REACHED();
}

bool Variant::operator<(const Variant& other) const
{
    if (m_type != other.m_type)
        return to_string() < other.to_string();
    switch (m_type) {
    case Type::Bool:
        return as_bool() < other.as_bool();
    case Type::Int32:
        return as_i32() < other.as_i32();
    case Type::Int64:
        return as_i64() < other.as_i64();
    case Type::UnsignedInt32:
        return as_u32() < other.as_u32();
    case Type::UnsignedInt64:
        return as_u64() < other.as_u64();
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
    case Type::Font:
    case Type::TextAlignment:
    case Type::ColorRole:
    case Type::FlagRole:
    case Type::MetricRole:
    case Type::PathRole:
        // FIXME: Figure out how to compare these.
        VERIFY_NOT_REACHED();
    case Type::Invalid:
        break;
    }
    VERIFY_NOT_REACHED();
}

String Variant::to_string() const
{
    switch (m_type) {
    case Type::Bool:
        return as_bool() ? "true" : "false";
    case Type::Int32:
        return String::number(as_i32());
    case Type::Int64:
        return String::number(as_i64());
    case Type::UnsignedInt32:
        return String::number(as_u32());
    case Type::UnsignedInt64:
        return String::number(as_u64());
    case Type::Float:
        return String::formatted("{:.2}", as_float());
    case Type::String:
        return as_string();
    case Type::Bitmap:
        return "[Gfx::Bitmap]";
    case Type::Icon:
        return "[GUI::Icon]";
    case Type::Color:
        return as_color().to_string();
    case Type::Point:
        return as_point().to_string();
    case Type::Size:
        return as_size().to_string();
    case Type::Rect:
        return as_rect().to_string();
    case Type::Font:
        return String::formatted("[Font: {}]", as_font().name());
    case Type::TextAlignment: {
        switch (m_value.as_text_alignment) {
        case Gfx::TextAlignment::Center:
            return "Gfx::TextAlignment::Center";
        case Gfx::TextAlignment::CenterLeft:
            return "Gfx::TextAlignment::CenterLeft";
        case Gfx::TextAlignment::CenterRight:
            return "Gfx::TextAlignment::CenterRight";
        case Gfx::TextAlignment::TopLeft:
            return "Gfx::TextAlignment::TopLeft";
        case Gfx::TextAlignment::TopRight:
            return "Gfx::TextAlignment::TopRight";
        default:
            VERIFY_NOT_REACHED();
        }
        return "";
    }
    case Type::ColorRole:
        return String::formatted("Gfx::ColorRole::{}", Gfx::to_string(m_value.as_color_role));
    case Type::FlagRole:
        return String::formatted("Gfx::FlagRole::{}", Gfx::to_string(m_value.as_flag_role));
    case Type::MetricRole:
        return String::formatted("Gfx::MetricRole::{}", Gfx::to_string(m_value.as_metric_role));
    case Type::PathRole:
        return String::formatted("Gfx::PathRole::{}", Gfx::to_string(m_value.as_path_role));
    case Type::Invalid:
        return "[null]";
    }
    VERIFY_NOT_REACHED();
}

}
