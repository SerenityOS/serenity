/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertyDeserializer.h"
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibGUI/Margins.h>
#include <LibGUI/UIDimensions.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace GUI {

template<>
ErrorOr<bool> PropertyDeserializer<bool>::operator()(JsonValue const& value) const
{
    if (value.is_bool())
        return value.as_bool();
    return Error::from_string_literal("Boolean is expected");
}

template<>
ErrorOr<String> PropertyDeserializer<String>::operator()(JsonValue const& value) const
{
    if (value.is_string()) {
        // FIXME: Port JsonValue to the new String class.
        return String::from_byte_string(value.as_string());
    }
    return Error::from_string_literal("UTF-8 string is expected");
}

template<>
ErrorOr<ByteString> PropertyDeserializer<ByteString>::operator()(JsonValue const& value) const
{
    if (value.is_string())
        return value.as_string();
    return Error::from_string_literal("String is expected");
}

template<>
ErrorOr<Gfx::IntRect> PropertyDeserializer<Gfx::IntRect>::operator()(JsonValue const& value) const
{
    if (!value.is_object() && !(value.is_array() && value.as_array().size() == 4))
        return Error::from_string_literal("An array with 4 integers or an object is expected");

    Gfx::IntRect rect;

    Optional<int> x;
    Optional<int> y;
    Optional<int> width;
    Optional<int> height;

    if (value.is_object()) {
        auto const& object = value.as_object();

        if (object.size() != 4 || !object.has("x"sv) || !object.has("y"sv) || !object.has("width"sv) || !object.has("height"sv))
            return Error::from_string_literal("Object with keys \"x\", \"y\", \"width\", and \"height\" is expected");

        x = object.get_i32("x"sv);
        y = object.get_i32("y"sv);
        width = object.get_i32("width"sv);
        height = object.get_i32("height"sv);
    } else {
        auto const& array = value.as_array();

        x = array[0].get_i32();
        y = array[1].get_i32();
        width = array[2].get_i32();
        height = array[3].get_i32();
    }

    if (!x.has_value())
        return Error::from_string_literal("X coordinate must be an integer");
    if (!y.has_value())
        return Error::from_string_literal("Y coordinate must be an integer");
    if (!width.has_value())
        return Error::from_string_literal("Width must be an integer");
    if (!height.has_value())
        return Error::from_string_literal("Height must be an integer");

    rect.set_x(x.value());
    rect.set_y(y.value());
    rect.set_width(width.value());
    rect.set_height(height.value());

    return rect;
}

template<>
ErrorOr<Gfx::IntSize> PropertyDeserializer<Gfx::IntSize>::operator()(JsonValue const& value) const
{
    if (!value.is_array() || value.as_array().size() != 2)
        return Error::from_string_literal("Expected array with 2 integers");

    auto const& array = value.as_array();

    auto const& width = array[0].get_i32();
    if (!width.has_value())
        return Error::from_string_literal("Width must be an integer");
    auto const& height = array[1].get_i32();
    if (!height.has_value())
        return Error::from_string_literal("Height must be an integer");

    Gfx::IntSize size;
    size.set_width(width.value());
    size.set_height(height.value());

    return size;
}

template<>
ErrorOr<GUI::Margins> PropertyDeserializer<GUI::Margins>::operator()(JsonValue const& value) const
{
    if (!value.is_array() || value.as_array().size() < 1 || value.as_array().size() > 4)
        return Error::from_string_literal("Expected non-empty array with up to 4 integers");

    auto const& array = value.as_array();
    auto size = array.size();

    int m[4];

    for (size_t i = 0; i < size; ++i) {
        auto const& margin = array[i].get_i32();
        if (!margin.has_value())
            return Error::from_string_literal("Margin value should be an integer");
        m[i] = margin.value();
    }

    if (size == 1)
        return GUI::Margins { m[0] };
    else if (size == 2)
        return GUI::Margins { m[0], m[1] };
    else if (size == 3)
        return GUI::Margins { m[0], m[1], m[2] };
    else
        return GUI::Margins { m[0], m[1], m[2], m[3] };
}

template<>
ErrorOr<UIDimension> PropertyDeserializer<UIDimension>::operator()(JsonValue const& value) const
{
    auto result = UIDimension::construct_from_json_value(value);
    if (result.has_value())
        return result.release_value();
    return Error::from_string_literal("Value is not a valid UIDimension");
}

template<>
ErrorOr<UISize> PropertyDeserializer<UISize>::operator()(JsonValue const& value) const
{
    if (!value.is_object() || !value.as_object().has("width"sv) || !value.as_object().has("height"sv))
        return Error::from_string_literal("Object with keys \"width\" and \"height\" is expected");

    auto const& object = value.as_object();

    auto const& width = object.get("width"sv).value();
    auto result_width = GUI::UIDimension::construct_from_json_value(width);
    if (!result_width.has_value())
        return Error::from_string_literal("width is not a valid UIDimension");

    auto const& height = object.get("height"sv).value();
    auto result_height = GUI::UIDimension::construct_from_json_value(height);
    if (!result_height.has_value())
        return Error::from_string_literal("height is not a valid UIDimension");

    return UISize { result_width.value(), result_height.value() };
}

template<>
ErrorOr<Color> PropertyDeserializer<Color>::operator()(JsonValue const& value) const
{
    if (value.is_string()) {
        auto c = Color::from_string(value.as_string());
        if (c.has_value())
            return c.release_value();
    }
    return Error::from_string_literal("Color is expected");
}

};
