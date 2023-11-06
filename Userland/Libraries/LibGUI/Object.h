/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventReceiver.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Property.h>
#include <LibGUI/PropertyDeserializer.h>

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

    bool set_property(ByteString const& name, JsonValue const& value);
    JsonValue property(ByteString const& name) const;
    HashMap<ByteString, NonnullOwnPtr<Property>> const& properties() const { return m_properties; }

protected:
    explicit Object(Core::EventReceiver* parent = nullptr);

    template<typename Getter, typename Deserializer, typename Setter>
    void register_property(StringView name, Getter&& getter, Deserializer&& deserializer, Setter&& setter)
    {
        Function<JsonValue()> getter_fn = nullptr;
        Function<bool(JsonValue const&)> setter_fn = nullptr;

        if constexpr (!SameAs<Getter, nullptr_t>) {
            static_assert(ConvertibleTo<InvokeResult<Getter>, JsonValue>);
            getter_fn = [captured_getter = forward<Getter>(getter)]() -> JsonValue {
                return captured_getter();
            };
        }

        static_assert(SameAs<Deserializer, nullptr_t> == SameAs<Setter, nullptr_t>);
        if constexpr (!SameAs<Deserializer, nullptr_t>) {
            using DeserializerReturnValue = RemoveReference<InvokeResult<Deserializer, JsonValue const&>>;
            static_assert(SpecializationOf<DeserializerReturnValue, ErrorOr>);
            using DeserializedValue = RemoveReference<typename DeserializerReturnValue::ResultType>;
            // FIXME: Should we allow setter to fail?
            static_assert(SameAs<InvokeResult<Setter, DeserializedValue&&>, void>);

            setter_fn = [captured_deserializer = forward<Deserializer>(deserializer), captured_setter = forward<Setter>(setter)](JsonValue const& value) -> bool {
                DeserializerReturnValue deserializer_return_value = captured_deserializer(value);
                if (deserializer_return_value.is_error()) {
                    // FIXME: Propagate error up to a place where we have enough context to print/show meaningful message.
                    dbgln("Got error while deserializing GML property: {}", deserializer_return_value.error());
                    return false;
                }
                DeserializedValue deserialized_value = deserializer_return_value.release_value();
                captured_setter(move(deserialized_value));
                return true;
            };
        }

        register_property(name, move(getter_fn), move(setter_fn));
    }

private:
    void register_property(ByteString const& name, Function<JsonValue()> getter, Function<bool(JsonValue const&)> setter = nullptr);

    HashMap<ByteString, NonnullOwnPtr<Property>> m_properties;
};

}

#define REGISTER_INT_PROPERTY(property_name, getter, setter) \
    register_property(                                       \
        property_name##sv,                                   \
        [this] { return this->getter(); },                   \
        ::GUI::PropertyDeserializer<int> {},                 \
        [this](auto const& value) { return setter(value); });

#define REGISTER_BOOL_PROPERTY(property_name, getter, setter) \
    register_property(                                        \
        property_name##sv,                                    \
        [this] { return this->getter(); },                    \
        ::GUI::PropertyDeserializer<bool> {},                 \
        [this](auto const& value) { return setter(value); });

#define REGISTER_STRING_PROPERTY(property_name, getter, setter) \
    register_property(                                          \
        property_name##sv,                                      \
        [this] { return this->getter().to_byte_string(); },     \
        ::GUI::PropertyDeserializer<String> {},                 \
        [this](auto const& value) { return setter(value); });

#define REGISTER_DEPRECATED_STRING_PROPERTY(property_name, getter, setter) \
    register_property(                                                     \
        property_name##sv,                                                 \
        [this] { return this->getter(); },                                 \
        ::GUI::PropertyDeserializer<ByteString> {},                        \
        [this](auto const& value) { return setter(value); });

#define REGISTER_READONLY_STRING_PROPERTY(property_name, getter) \
    register_property(                                           \
        property_name##sv,                                       \
        [this] { return this->getter(); },                       \
        nullptr,                                                 \
        nullptr);

#define REGISTER_WRITE_ONLY_STRING_PROPERTY(property_name, setter) \
    register_property(                                             \
        property_name##sv,                                         \
        nullptr,                                                   \
        ::GUI::PropertyDeserializer<ByteString> {},                \
        [this](auto const& value) { return setter(value); });

#define REGISTER_READONLY_SIZE_PROPERTY(property_name, getter) \
    register_property(                                         \
        property_name##sv,                                     \
        [this] {                                               \
            auto size = this->getter();                        \
            JsonArray size_array;                              \
            size_array.must_append(size.width());              \
            size_array.must_append(size.height());             \
            return size_array;                                 \
        },                                                     \
        nullptr,                                               \
        nullptr);

#define REGISTER_RECT_PROPERTY(property_name, getter, setter) \
    register_property(                                        \
        property_name##sv,                                    \
        [this] {                                              \
            auto rect = this->getter();                       \
            JsonObject rect_object;                           \
            rect_object.set("x"sv, rect.x());                 \
            rect_object.set("y"sv, rect.y());                 \
            rect_object.set("width"sv, rect.width());         \
            rect_object.set("height"sv, rect.height());       \
            return rect_object;                               \
        },                                                    \
        ::GUI::PropertyDeserializer<Gfx::IntRect> {},         \
        [this](auto const& value) { return setter(value); });

#define REGISTER_SIZE_PROPERTY(property_name, getter, setter) \
    register_property(                                        \
        property_name##sv,                                    \
        [this] {                                              \
            auto size = this->getter();                       \
            JsonArray size_array;                             \
            size_array.must_append(size.width());             \
            size_array.must_append(size.height());            \
            return size_array;                                \
        },                                                    \
        ::GUI::PropertyDeserializer<Gfx::IntSize> {},         \
        [this](auto const& value) { return setter(value); });

#define REGISTER_ENUM_PROPERTY(property_name, getter, setter, EnumType, ...)                 \
    register_property(                                                                       \
        property_name##sv,                                                                   \
        [this]() -> JsonValue {                                                              \
            struct {                                                                         \
                EnumType enum_value;                                                         \
                ByteString string_value;                                                     \
            } options[] = { __VA_ARGS__ };                                                   \
            auto enum_value = getter();                                                      \
            for (size_t i = 0; i < array_size(options); ++i) {                               \
                auto const& option = options[i];                                             \
                if (enum_value == option.enum_value)                                         \
                    return option.string_value;                                              \
            }                                                                                \
            VERIFY_NOT_REACHED();                                                            \
        },                                                                                   \
        [](JsonValue const& value) -> ErrorOr<EnumType> {                                    \
            if (!value.is_string())                                                          \
                return Error::from_string_literal("String is expected");                     \
            auto string = value.as_string();                                                 \
            struct {                                                                         \
                EnumType enum_value;                                                         \
                ByteString string_value;                                                     \
            } options[] = { __VA_ARGS__ };                                                   \
            for (size_t i = 0; i < array_size(options); ++i) {                               \
                auto const& option = options[i];                                             \
                if (string == option.string_value)                                           \
                    return option.enum_value;                                                \
            }                                                                                \
            return Error::from_string_literal("Value is not a valid option for " #EnumType); \
        },                                                                                   \
        [this](auto const& value) { return setter(value); })

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
