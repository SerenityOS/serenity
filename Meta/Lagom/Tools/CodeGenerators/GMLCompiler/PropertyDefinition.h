/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>

// Error type that owns its error text
// and allows constructing homogenous error messages for the property definition system.
struct PropertyError {
    String text;

    explicit PropertyError(String text)
        : text(move(text))
    {
    }

    // NOTE: Needs to be implicit so that TRY() can convert from AK::Error to PropertyError.
    PropertyError(AK::Error error)
        : text(MUST(String::from_utf8(error.string_literal())))
    {
    }

    template<typename... Parameters>
    static PropertyError format(AK::CheckedFormatString<Parameters...>&& format_string, Parameters const&... arguments)
    {
        return PropertyError { MUST(String::formatted(forward<AK::CheckedFormatString<Parameters...>>(format_string), forward<Parameters const&>(arguments)...)) };
    }

    template<typename... Parameters>
    static PropertyError in_widget(String const& widget_name, AK::CheckedFormatString<Parameters...>&& format_string, Parameters const&... arguments)
    {
        return PropertyError { MUST(String::formatted("in widget {}: {}", widget_name, MUST(String::formatted(forward<AK::CheckedFormatString<Parameters...>>(format_string), forward<Parameters const&>(arguments)...)))) };
    }
    PropertyError add_widget_and_property(String const& widget_name, String const& property_name) const&&
    {
        return PropertyError { MUST(String::formatted("in widget {}: in property {}: {}", widget_name, property_name, this->text)) };
    }
};

template<>
struct AK::Formatter<PropertyError> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PropertyError const& error)
    {
        return builder.put_string(error.text);
    }
};

class PropertyType;

struct CustomVariant final {
    CustomVariant(Vector<NonnullOwnPtr<PropertyType>> variant_types)
        : variant_types(move(variant_types))
    {
    }
    CustomVariant(CustomVariant const& other)
    {
        for (auto const& type : other.variant_types)
            variant_types.append(make<PropertyType>(*type));
    }
    CustomVariant() = default;
    AK_MAKE_DEFAULT_MOVABLE(CustomVariant);

    size_t element_count() const { return variant_types.size(); }

    Vector<NonnullOwnPtr<PropertyType>> variant_types;
};

struct CustomArray final {
    CustomArray(size_t min_values, size_t max_values, NonnullOwnPtr<PropertyType> element_type)
        : min_values(min_values)
        , max_values(max_values)
        , element_type(move(element_type))
    {
    }
    CustomArray(CustomArray const& other)
        : min_values(other.min_values)
        , max_values(other.max_values)
        , element_type(make<PropertyType>(*other.element_type))
    {
    }
    AK_MAKE_DEFAULT_MOVABLE(CustomArray);

    size_t min_values;
    size_t max_values;
    NonnullOwnPtr<PropertyType> element_type;
};

// Any type that is handled with its own custom initializer.
enum class SimpleType {
    Bool,
    I64,
    U64,
    Double,
    String,
    ByteString,
    Bitmap,
    Color,
    UIDimension,
    Margins,
};

struct EnumType {
    String name;
};

class PropertyType final : public Variant<SimpleType, EnumType, CustomVariant, CustomArray> {
public:
    static ErrorOr<PropertyType, PropertyError> parse(String const& type_name, Optional<u64> min_values, Optional<u64> max_values);

    ErrorOr<String, PropertyError> generate_initializer_for(JsonValue const& property_value) const;
    String name(Optional<size_t> element_count = {}) const;
};

// A property that can be set on a widget via GML.
struct PropertyDefinition {
    String name;
    String getter;
    String setter;
    String description;
    PropertyType type;
};

// A collection of properties for a certain widget type.
class WidgetProperties final {
    AK_MAKE_DEFAULT_COPYABLE(WidgetProperties);
    AK_MAKE_DEFAULT_MOVABLE(WidgetProperties);

public:
    static ErrorOr<HashMap<String, WidgetProperties>, PropertyError> parse_properties(JsonArray const&);
    // Copy all properties of a widget's direct (or indirect) parents to that widget.
    static ErrorOr<void, PropertyError> expand_inherited_properties(HashMap<String, WidgetProperties>& widgets);

    StringView cpp_identifier() const { return m_cpp_identifier.bytes_as_string_view(); }
    StringView header() const { return m_header.bytes_as_string_view(); }
    Optional<String> const& inherits() const { return m_inherits; }
    StringView description() const { return m_description.bytes_as_string_view(); }
    HashMap<String, PropertyDefinition> const& properties() const { return m_properties; }
    HashMap<String, PropertyDefinition>& properties() { return m_properties; }

private:
    WidgetProperties(String cpp_identifier, String header, Optional<String> inherits, String description)
        : m_cpp_identifier(move(cpp_identifier))
        , m_header(move(header))
        , m_inherits(move(inherits))
        , m_description(move(description))
    {
    }

    String m_cpp_identifier;
    String m_header;
    Optional<String> m_inherits;
    String m_description;

    HashMap<String, PropertyDefinition> m_properties;
};
