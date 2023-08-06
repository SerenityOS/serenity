/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventReceiver.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Property.h>

namespace GUI {

#define REGISTER_ABSTRACT_GUI_OBJECT(namespace_, class_name)                                                                                                                               \
    namespace GUI::Registration {                                                                                                                                                          \
    ::GUI::ObjectClassRegistration registration_##class_name(#namespace_ "::" #class_name##sv, []() { return Error::from_string_literal("Attempted to construct an abstract object."); }); \
    }

#define REGISTER_GUI_OBJECT(namespace_, class_name)                                                                                                    \
    namespace GUI::Registration {                                                                                                                      \
    ::GUI::ObjectClassRegistration registration_##class_name(#namespace_ "::" #class_name##sv, []() { return namespace_::class_name::try_create(); }); \
    }

class ObjectClassRegistration {
    AK_MAKE_NONCOPYABLE(ObjectClassRegistration);
    AK_MAKE_NONMOVABLE(ObjectClassRegistration);

public:
    ObjectClassRegistration(StringView class_name, Function<ErrorOr<NonnullRefPtr<Object>>()> factory, ObjectClassRegistration* parent_class = nullptr);
    ~ObjectClassRegistration() = default;

    StringView class_name() const { return m_class_name; }
    ObjectClassRegistration const* parent_class() const { return m_parent_class; }
    ErrorOr<NonnullRefPtr<Object>> construct() const { return m_factory(); }
    bool is_derived_from(ObjectClassRegistration const& base_class) const;

    static void for_each(Function<void(ObjectClassRegistration const&)>);
    static ObjectClassRegistration const* find(StringView class_name);

private:
    StringView m_class_name;
    Function<ErrorOr<NonnullRefPtr<Object>>()> m_factory;
    ObjectClassRegistration* m_parent_class { nullptr };
};

class Object : public Core::EventReceiver {
    C_OBJECT_ABSTRACT(Object);

public:
    virtual ~Object() override;

    bool set_property(DeprecatedString const& name, JsonValue const& value);
    JsonValue property(DeprecatedString const& name) const;
    HashMap<DeprecatedString, NonnullOwnPtr<Property>> const& properties() const { return m_properties; }

protected:
    explicit Object(Core::EventReceiver* parent = nullptr);

    void register_property(DeprecatedString const& name, Function<JsonValue()> getter, Function<bool(JsonValue const&)> setter = nullptr);

private:
    HashMap<DeprecatedString, NonnullOwnPtr<Property>> m_properties;
};

}

#define REGISTER_INT_PROPERTY(property_name, getter, setter) \
    register_property(                                       \
        property_name,                                       \
        [this] { return this->getter(); },                   \
        [this](auto& value) {                                \
            this->setter(value.template to_number<int>());   \
            return true;                                     \
        });

#define REGISTER_BOOL_PROPERTY(property_name, getter, setter) \
    register_property(                                        \
        property_name,                                        \
        [this] { return this->getter(); },                    \
        [this](auto& value) {                                 \
            this->setter(value.to_bool());                    \
            return true;                                      \
        });

// FIXME: Port JsonValue to the new String class.
#define REGISTER_STRING_PROPERTY(property_name, getter, setter)                                                                           \
    register_property(                                                                                                                    \
        property_name,                                                                                                                    \
        [this]() { return this->getter().to_deprecated_string(); },                                                                       \
        [this](auto& value) {                                                                                                             \
            this->setter(String::from_deprecated_string(value.to_deprecated_string()).release_value_but_fixme_should_propagate_errors()); \
            return true;                                                                                                                  \
        });

#define REGISTER_DEPRECATED_STRING_PROPERTY(property_name, getter, setter) \
    register_property(                                                     \
        property_name,                                                     \
        [this] { return this->getter(); },                                 \
        [this](auto& value) {                                              \
            this->setter(value.to_deprecated_string());                    \
            return true;                                                   \
        });

#define REGISTER_READONLY_STRING_PROPERTY(property_name, getter) \
    register_property(                                           \
        property_name,                                           \
        [this] { return this->getter(); },                       \
        {});

#define REGISTER_WRITE_ONLY_STRING_PROPERTY(property_name, setter) \
    register_property(                                             \
        property_name,                                             \
        {},                                                        \
        [this](auto& value) {                                      \
            this->setter(value.to_deprecated_string());            \
            return true;                                           \
        });

#define REGISTER_READONLY_SIZE_PROPERTY(property_name, getter) \
    register_property(                                         \
        property_name,                                         \
        [this] {                                               \
            auto size = this->getter();                        \
            JsonArray size_array;                              \
            size_array.must_append(size.width());              \
            size_array.must_append(size.height());             \
            return size_array;                                 \
        },                                                     \
        {});

#define REGISTER_RECT_PROPERTY(property_name, getter, setter)                       \
    register_property(                                                              \
        property_name,                                                              \
        [this] {                                                                    \
            auto rect = this->getter();                                             \
            JsonObject rect_object;                                                 \
            rect_object.set("x"sv, rect.x());                                       \
            rect_object.set("y"sv, rect.y());                                       \
            rect_object.set("width"sv, rect.width());                               \
            rect_object.set("height"sv, rect.height());                             \
            return rect_object;                                                     \
        },                                                                          \
        [this](auto& value) {                                                       \
            Gfx::IntRect rect;                                                      \
            if (value.is_object()) {                                                \
                rect.set_x(value.as_object().get_i32("x"sv).value_or(0));           \
                rect.set_y(value.as_object().get_i32("y"sv).value_or(0));           \
                rect.set_width(value.as_object().get_i32("width"sv).value_or(0));   \
                rect.set_height(value.as_object().get_i32("height"sv).value_or(0)); \
            } else if (value.is_array() && value.as_array().size() == 4) {          \
                rect.set_x(value.as_array()[0].to_i32());                           \
                rect.set_y(value.as_array()[1].to_i32());                           \
                rect.set_width(value.as_array()[2].to_i32());                       \
                rect.set_height(value.as_array()[3].to_i32());                      \
            } else {                                                                \
                return false;                                                       \
            }                                                                       \
            setter(rect);                                                           \
                                                                                    \
            return true;                                                            \
        });

#define REGISTER_SIZE_PROPERTY(property_name, getter, setter) \
    register_property(                                        \
        property_name,                                        \
        [this] {                                              \
            auto size = this->getter();                       \
            JsonArray size_array;                             \
            size_array.must_append(size.width());             \
            size_array.must_append(size.height());            \
            return size_array;                                \
        },                                                    \
        [this](auto& value) {                                 \
            if (!value.is_array())                            \
                return false;                                 \
            Gfx::IntSize size;                                \
            size.set_width(value.as_array()[0].to_i32());     \
            size.set_height(value.as_array()[1].to_i32());    \
            setter(size);                                     \
            return true;                                      \
        });

#define REGISTER_ENUM_PROPERTY(property_name, getter, setter, EnumType, ...) \
    register_property(                                                       \
        property_name,                                                       \
        [this]() -> JsonValue {                                              \
            struct {                                                         \
                EnumType enum_value;                                         \
                DeprecatedString string_value;                               \
            } options[] = { __VA_ARGS__ };                                   \
            auto enum_value = getter();                                      \
            for (size_t i = 0; i < array_size(options); ++i) {               \
                auto& option = options[i];                                   \
                if (enum_value == option.enum_value)                         \
                    return option.string_value;                              \
            }                                                                \
            return JsonValue();                                              \
        },                                                                   \
        [this](auto& value) {                                                \
            struct {                                                         \
                EnumType enum_value;                                         \
                DeprecatedString string_value;                               \
            } options[] = { __VA_ARGS__ };                                   \
            if (!value.is_string())                                          \
                return false;                                                \
            auto string_value = value.as_string();                           \
            for (size_t i = 0; i < array_size(options); ++i) {               \
                auto& option = options[i];                                   \
                if (string_value == option.string_value) {                   \
                    setter(option.enum_value);                               \
                    return true;                                             \
                }                                                            \
            }                                                                \
            return false;                                                    \
        })

#define REGISTER_TEXT_ALIGNMENT_PROPERTY(property_name, getter, setter) \
    REGISTER_ENUM_PROPERTY(                                             \
        property_name, getter, setter, Gfx::TextAlignment,              \
        { Gfx::TextAlignment::Center, "Center" },                       \
        { Gfx::TextAlignment::CenterLeft, "CenterLeft" },               \
        { Gfx::TextAlignment::CenterRight, "CenterRight" },             \
        { Gfx::TextAlignment::TopCenter, "TopCenter" },                 \
        { Gfx::TextAlignment::TopLeft, "TopLeft" },                     \
        { Gfx::TextAlignment::TopRight, "TopRight" },                   \
        { Gfx::TextAlignment::BottomCenter, "BottomCenter" },           \
        { Gfx::TextAlignment::BottomLeft, "BottomLeft" },               \
        { Gfx::TextAlignment::BottomRight, "BottomRight" })

#define REGISTER_FONT_WEIGHT_PROPERTY(property_name, getter, setter) \
    REGISTER_ENUM_PROPERTY(                                          \
        property_name, getter, setter, unsigned,                     \
        { Gfx::FontWeight::Thin, "Thin" },                           \
        { Gfx::FontWeight::ExtraLight, "ExtraLight" },               \
        { Gfx::FontWeight::Light, "Light" },                         \
        { Gfx::FontWeight::Regular, "Regular" },                     \
        { Gfx::FontWeight::Medium, "Medium" },                       \
        { Gfx::FontWeight::SemiBold, "SemiBold" },                   \
        { Gfx::FontWeight::Bold, "Bold" },                           \
        { Gfx::FontWeight::ExtraBold, "ExtraBold" },                 \
        { Gfx::FontWeight::Black, "Black" },                         \
        { Gfx::FontWeight::ExtraBlack, "ExtraBlack" })

#define REGISTER_TEXT_WRAPPING_PROPERTY(property_name, getter, setter) \
    REGISTER_ENUM_PROPERTY(                                            \
        property_name, getter, setter, Gfx::TextWrapping,              \
        { Gfx::TextWrapping::Wrap, "Wrap" },                           \
        { Gfx::TextWrapping::DontWrap, "DontWrap" })
