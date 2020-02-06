/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonValue.h>
#include <LibGUI/GVariant.h>

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
    case Variant::Type::UnsignedInt:
        return "UnsignedInt";
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
    }
    ASSERT_NOT_REACHED();
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

Variant::Variant(unsigned value)
    : m_type(Type::UnsignedInt)
{
    m_value.as_uint = value;
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
        m_type = Type::UnsignedInt;
        m_value.as_uint = value.as_u32();
        return;
    }

    if (value.is_i64()) {
        m_type = Type::Int64;
        m_value.as_i64 = value.as_i64();
        return;
    }

    if (value.is_u64()) {
        // FIXME: Variant should have a 64-bit internal type.
        m_type = Type::UnsignedInt;
        m_value.as_uint = value.to_u32();
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

    ASSERT_NOT_REACHED();
}

Variant::Variant(const Gfx::Bitmap& value)
    : m_type(Type::Bitmap)
{
    m_value.as_bitmap = const_cast<Gfx::Bitmap*>(&value);
    AK::ref_if_not_null(m_value.as_bitmap);
}

Variant::Variant(const GIcon& value)
    : m_type(Type::Icon)
{
    m_value.as_icon = &const_cast<GIconImpl&>(value.impl());
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

Variant::Variant(const Gfx::Point& point)
    : m_type(Type::Point)
{
    m_value.as_point = { point.x(), point.y() };
}

Variant::Variant(const Gfx::Size& size)
    : m_type(Type::Size)
{
    m_value.as_size = { size.width(), size.height() };
}

Variant::Variant(const Gfx::Rect& rect)
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
    // FIXME: Move, not copy!
    clear();
    copy_from(other);
    other.clear();
    return *this;
}

Variant::Variant(const Variant& other)
{
    copy_from(other);
}

void Variant::copy_from(const Variant& other)
{
    ASSERT(!is_valid());
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
    case Type::UnsignedInt:
        m_value.as_uint = other.m_value.as_uint;
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
    case Type::UnsignedInt:
        return as_uint() == other.as_uint();
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
    case Type::Invalid:
        return true;
    }
    ASSERT_NOT_REACHED();
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
    case Type::UnsignedInt:
        return as_uint() < other.as_uint();
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
        // FIXME: Figure out how to compare these.
        ASSERT_NOT_REACHED();
    case Type::Invalid:
        break;
    }
    ASSERT_NOT_REACHED();
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
    case Type::UnsignedInt:
        return String::number(as_uint());
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
    case Type::Font:
        return String::format("[Font: %s]", as_font().name().characters());
    case Type::Invalid:
        return "[null]";
        break;
    }
    ASSERT_NOT_REACHED();
}

}
