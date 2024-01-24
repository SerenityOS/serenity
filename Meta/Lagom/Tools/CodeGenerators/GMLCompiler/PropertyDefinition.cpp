/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertyDefinition.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibGUI/UIDimensions.h>
#include <LibGfx/Color.h>

static ErrorOr<String> escape_string(JsonValue to_escape)
{
    auto string = TRY(String::from_byte_string(to_escape.as_string()));

    // All C++ simple escape sequences; see https://en.cppreference.com/w/cpp/language/escape
    // Other commonly-escaped characters are hard-to-type Unicode and therefore fine to include verbatim in UTF-8 coded strings.
    static HashMap<StringView, StringView> escape_sequences = {
        { "\\"sv, "\\\\"sv }, // This needs to be the first because otherwise the the backslashes of other items will be double escaped
        { "\0"sv, "\\0"sv },
        { "\'"sv, "\\'"sv },
        { "\""sv, "\\\""sv },
        { "\a"sv, "\\a"sv },
        { "\b"sv, "\\b"sv },
        { "\f"sv, "\\f"sv },
        { "\n"sv, "\\n"sv },
        { "\r"sv, "\\r"sv },
        { "\t"sv, "\\t"sv },
        { "\v"sv, "\\v"sv },
    };

    for (auto const& entries : escape_sequences)
        string = TRY(string.replace(entries.key, entries.value, ReplaceMode::All));

    return string;
}

ErrorOr<PropertyType, PropertyError> PropertyType::parse(String const& type_name, Optional<u64> min_values, Optional<u64> max_values)
{
    if (type_name == "String"sv) {
        return PropertyType { SimpleType::String };
    } else if (type_name == "ByteString"sv) {
        return PropertyType { SimpleType::ByteString };
    } else if (type_name == "i64"sv) {
        return PropertyType { SimpleType::I64 };
    } else if (type_name == "u64"sv) {
        return PropertyType { SimpleType::U64 };
    } else if (type_name == "double"sv) {
        return PropertyType { SimpleType::Double };
    } else if (type_name == "bool"sv) {
        return PropertyType { SimpleType::Bool };
    } else if (type_name == "Gfx::Bitmap"sv) {
        return PropertyType { SimpleType::Bitmap };
    } else if (type_name == "Gfx::Color"sv) {
        return PropertyType { SimpleType::Color };
    } else if (type_name == "GUI::UIDimension"sv) {
        return PropertyType { SimpleType::UIDimension };
    } else if (type_name == "GUI::Margins"sv) {
        return PropertyType { SimpleType::Margins };
    } else if (type_name.starts_with_bytes("Array"sv)) {
        auto inner_type_part = TRY(type_name.split('<'));
        if (inner_type_part.size() != 2)
            return PropertyError::format("No inner type specified for '{}'", type_name);
        auto maybe_inner_type_name = TRY(inner_type_part.take_last().split('>'));
        if (maybe_inner_type_name.is_empty())
            return PropertyError::format("No inner type specified for '{}'", type_name);
        auto const inner_type_name = maybe_inner_type_name.take_first();
        if (!max_values.has_value() || !min_values.has_value())
            return PropertyError::format("Missing array element bounds for type '{}'", type_name);
        return PropertyType { CustomArray {
            min_values.release_value(),
            max_values.release_value(),
            make<PropertyType>(TRY(parse(inner_type_name, {}, {}))),
        } };
    } else if (type_name.starts_with_bytes("Variant"sv)) {
        auto inner_type_parts = TRY(type_name.split('<'));
        if (inner_type_parts.size() != 2)
            return PropertyError::format("No inner variant types specified for '{}'", type_name);
        auto maybe_inner_type_name = TRY(inner_type_parts.take_last().split('>'));
        if (maybe_inner_type_name.is_empty())
            return PropertyError::format("No inner variant types specified for '{}'", type_name);
        auto const inner_type_names = TRY(maybe_inner_type_name.take_first().split(','));

        CustomVariant type;
        for (auto const& inner_type_name : inner_type_names) {
            auto const trimmed_type_name = TRY(inner_type_name.trim_ascii_whitespace());
            TRY(type.variant_types.try_append(make<PropertyType>(TRY(parse(trimmed_type_name, {}, {})))));
        }
        return PropertyType { move(type) };
    } else {
        return PropertyType { EnumType { type_name } };
    }
}

String PropertyType::name(Optional<size_t> element_count) const
{
    return visit([=](SimpleType const& type) -> String {
        switch (type) {
        case SimpleType::Bool:
            return "bool"_string;
        case SimpleType::I64:
            return "i64"_string;
        case SimpleType::U64:
            return "u64"_string;
        case SimpleType::Double:
            return "double"_string;
        case SimpleType::String:
            return "::AK::String"_string;
        case SimpleType::ByteString:
            return "::AK::ByteString"_string;
        case SimpleType::Bitmap:
            return "::Gfx::Bitmap"_string;
        case SimpleType::Color:
            return "::Gfx::Color"_string;
        case SimpleType::UIDimension:
            return "::GUI::UIDimension"_string;
        case SimpleType::Margins:
            return "::GUI::Margins"_string;
        } },
        [=](EnumType const& type) -> String {
            return type.name;
        },
        [=](CustomVariant const& type) -> String {
            StringBuilder builder;
            builder.append("::AK::Variant<"sv);
            Vector<String> strings;
            for (auto const& subtype : type.variant_types)
                strings.append(subtype->name());
            builder.join(", "sv, strings);
            builder.append('>');
            return builder.to_string_without_validation();
        },
        [&](CustomArray const& type) -> String {
            // Try to make use of C++ template argument deduction, if necessary.
            if (!element_count.has_value())
                return "Array"_string;
            return MUST(String::formatted("Array<{}, {}>", type.element_type->name(), element_count.release_value()));
        });
}

ErrorOr<String, PropertyError> PropertyType::generate_initializer_for(JsonValue const& property) const
{
    return visit([=](SimpleType const& type) -> ErrorOr<String, PropertyError> {
        switch (type) {
        case SimpleType::Bool:
            if (!property.is_bool())
                return PropertyError::format("Non-bool value {} supplied for boolean property", property);
            return TRY(String::formatted("{}", property.as_bool()));
        case SimpleType::I64:
            if (!property.is_integer<i64>())
                return PropertyError::format("Non-integer value {} supplied for i64 property", property);
            return TRY(String::formatted("static_cast<i64>({})", property.as_integer<i64>()));
        case SimpleType::U64:
            if (!property.is_integer<u64>())
                return PropertyError::format("Non-integer value {} supplied for u64 property", property);
            return TRY(String::formatted("static_cast<u64>({})", property.as_integer<u64>()));
        case SimpleType::Double:
            if (!property.is_number())
                return PropertyError::format("Non-number value {} supplied for double property", property);
            return TRY(String::formatted("static_cast<double>({})", property.get_double_with_precision_loss().release_value()));
        case SimpleType::String:
            if (!property.is_string())
                return PropertyError::format("Non-string value {} supplied for string property", property);
            return TRY(String::formatted(R"~~~("{}"_string)~~~", TRY(escape_string(property))));
        case SimpleType::ByteString:
            if (!property.is_string())
                return PropertyError::format("Non-string value {} supplied for byte string property", property);
            return TRY(String::formatted(R"~~~("{}"sv)~~~", TRY(escape_string(property))));
        case SimpleType::Bitmap:
            if (!property.is_string())
                return PropertyError::format("Non-string value {} supplied for bitmap property", property);
            return TRY(String::formatted(R"~~~(TRY(Gfx::Bitmap::load_from_file("{}"sv)))~~~", TRY(escape_string(property))));
        case SimpleType::Color: {
            if (!property.is_string())
                return PropertyError::format("Non-string value {} supplied for color property", property);
            auto maybe_color = Gfx::Color::from_string(property.as_string());
            if (!maybe_color.has_value())
                return PropertyError::format("Invalid color {}", property);
            auto const color = maybe_color.release_value();
            return TRY(String::formatted("::Gfx::Color {{ {}, {}, {}, {} }}", color.red(), color.green(), color.blue(), color.alpha()));
        }
        case SimpleType::UIDimension: {
            auto maybe_dimension =  GUI::UIDimension::construct_from_json_value(property);
            if (!maybe_dimension.has_value())
                return PropertyError::format("Invalid UI dimension {}", property);
            return TRY(maybe_dimension->as_cpp_source());
        }
        case SimpleType::Margins: {
            if (!property.is_array())
                return PropertyError::format("Non-array value {} supplied for margins property", property);
            auto const element_count = property.as_array().size();
            if (element_count < 1 || element_count > 4)
                return PropertyError::format("margins array must have between 1 and 4 elements, but {} were given", element_count);
            StringBuilder builder;
            TRY(builder.try_appendff("{} {{", name(element_count)));
            for (auto const& child_value : property.as_array().values())
                builder.appendff("{}, ", TRY(PropertyType { SimpleType::I64 }.generate_initializer_for(child_value)));
            TRY(builder.try_append('}'));
            return TRY(builder.to_string());
        }
        } },
        [=](EnumType const& type) -> ErrorOr<String, PropertyError> {
            if (!property.is_string())
                return PropertyError::format("Non-string value {} supplied for enum property", property);
            return TRY(String::formatted("{}::{}", type.name, property.as_string()));
        },
        [=](CustomVariant const& type) -> ErrorOr<String, PropertyError> {
            // Find the first variant subtype that accepts the value.
            for (auto const& subtype : type.variant_types) {
                auto maybe_initializer = subtype->generate_initializer_for(property);
                if (!maybe_initializer.is_error())
                    return maybe_initializer.release_value();
            }
            return PropertyError::format("Invalid value {} for variant property {}", property, name());
        },
        [=](CustomArray const& type) -> ErrorOr<String, PropertyError> {
            if (!property.is_array())
                return PropertyError::format("Non-array value {} supplied for array property {}", property, name());
            auto const element_count = property.as_array().size();
            StringBuilder builder;
            TRY(builder.try_appendff("{} {{", name(element_count)));
            for (auto const& child_value : property.as_array().values())
                builder.appendff("{}, ", TRY(type.element_type->generate_initializer_for(child_value)));
            TRY(builder.try_append('}'));
            return TRY(builder.to_string());
        });
}
ErrorOr<HashMap<String, WidgetProperties>, PropertyError> WidgetProperties::parse_properties(JsonArray const& property_definitions)
{
    HashMap<String, WidgetProperties> widgets;

    for (auto const& widget : property_definitions.values()) {
        if (!widget.is_object())
            return PropertyError::format("Widget definition {} is not an object", widget);

        auto const& widget_object = widget.as_object();
        auto maybe_name = widget_object.get_byte_string("name"sv);
        auto maybe_header = widget_object.get_byte_string("header"sv);
        auto maybe_inherits = widget_object.get_byte_string("inherits"sv);
        auto maybe_description = widget_object.get_byte_string("description"sv);
        auto maybe_properties = widget_object.get_array("properties"sv);
        if (!maybe_name.has_value() || !maybe_header.has_value())
            return PropertyError::format("in JSON object {}: Name or header of widget is missing", widget);

        WidgetProperties widget_properties {
            TRY(String::from_byte_string(maybe_name.release_value())),
            TRY(String::from_byte_string(maybe_header.release_value())),
            TRY(maybe_inherits.map([](auto inherits) { return String::from_byte_string(inherits); })),
            TRY(String::from_byte_string(maybe_description.value_or(""))),
        };

        if (maybe_properties.has_value()) {
            auto const& properties = maybe_properties.release_value();
            for (auto const& property : properties.values()) {
                if (!property.is_object())
                    return PropertyError::in_widget(widget_properties.m_cpp_identifier, "Property {} is not an object", property);

                auto const& property_object = property.as_object();
                auto maybe_property_name = property_object.get_byte_string("name"sv);
                auto maybe_property_getter = property_object.get_byte_string("getter"sv);
                auto maybe_property_setter = property_object.get_byte_string("setter"sv);
                auto maybe_property_description = property_object.get_byte_string("description"sv);
                auto maybe_property_type_string = property_object.get_byte_string("type"sv);
                auto maybe_property_min_values = property_object.get_u64("min_values"sv);
                auto maybe_property_max_values = property_object.get_u64("max_values"sv);
                if (!maybe_property_name.has_value() || !maybe_property_type_string.has_value())
                    return PropertyError::in_widget(widget_properties.m_cpp_identifier, "Name or type of property is missing in JSON object {}", property);

                auto property_name = TRY(String::from_byte_string(maybe_property_name.release_value()));
                auto maybe_property_type = PropertyType::parse(TRY(String::from_byte_string(maybe_property_type_string.release_value())), maybe_property_min_values, maybe_property_max_values);
                if (maybe_property_type.is_error())
                    return maybe_property_type.release_error().add_widget_and_property(widget_properties.m_cpp_identifier, property_name);

                widget_properties.m_properties.set(property_name,
                    PropertyDefinition {
                        .name = property_name,
                        .getter = TRY(maybe_property_getter.map([](auto getter) {
                            return String::from_byte_string(getter);
                        })).value_or(property_name),
                        .setter = TRY(TRY(maybe_property_setter.map([](auto setter) {
                            return String::from_byte_string(setter);
                        })).try_value_or_lazy_evaluated([&] {
                            return String::formatted("set_{}", property_name);
                        })),
                        .description = TRY(String::from_byte_string(maybe_property_description.value_or({}))),
                        .type = maybe_property_type.release_value(),
                    });
            }
        }

        TRY(widgets.try_set(widget_properties.m_cpp_identifier, move(widget_properties)));
    }

    return widgets;
}

static ErrorOr<void, PropertyError> expand_widget(String const& widget_name, HashMap<String, WidgetProperties>& widgets, HashTable<String>& expanded_widgets)
{
    // Prevent repeated work by keeping track of which widgets have been expanded already.
    if (expanded_widgets.contains(widget_name))
        return {};
    expanded_widgets.set(widget_name);

    auto properties = widgets.get(widget_name);

    // No inheritance -> no work to do.
    if (!properties.has_value() || !properties->inherits().has_value())
        return {};

    // Ensure that all direct and indirect parents are expanded.
    TRY(expand_widget(properties->inherits().value(), widgets, expanded_widgets));

    // Set all parent properties on this widget as well.
    auto const maybe_parent = widgets.get(properties->inherits().value());
    if (!maybe_parent.has_value())
        return PropertyError::in_widget(widget_name, "Couldn't find parent widget {}", properties->inherits().value());

    auto const& parent = maybe_parent.value();
    for (auto const& parent_property : parent.properties()) {
        properties->properties().ensure(parent_property.key, [&] { return parent_property.value; });
    }
    return {};
}

ErrorOr<void, PropertyError> WidgetProperties::expand_inherited_properties(HashMap<String, WidgetProperties>& widgets)
{
    HashTable<String> expanded_widgets;
    for (auto& entry : widgets)
        TRY(expand_widget(entry.key, widgets, expanded_widgets));

    return {};
}
