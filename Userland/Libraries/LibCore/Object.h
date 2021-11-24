/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/TypeCasts.h>
#include <AK/Weakable.h>
#include <LibCore/Forward.h>
#include <LibCore/Property.h>

namespace Core {

#define REGISTER_ABSTRACT_CORE_OBJECT(namespace_, class_name)                                                                 \
    namespace Core {                                                                                                          \
    namespace Registration {                                                                                                  \
    Core::ObjectClassRegistration registration_##class_name(#namespace_ "::" #class_name, []() { return RefPtr<Object>(); }); \
    }                                                                                                                         \
    }

#define REGISTER_CORE_OBJECT(namespace_, class_name)                                                                                             \
    namespace Core {                                                                                                                             \
    namespace Registration {                                                                                                                     \
    Core::ObjectClassRegistration registration_##class_name(#namespace_ "::" #class_name, []() { return namespace_::class_name::construct(); }); \
    }                                                                                                                                            \
    }

class ObjectClassRegistration {
    AK_MAKE_NONCOPYABLE(ObjectClassRegistration);
    AK_MAKE_NONMOVABLE(ObjectClassRegistration);

public:
    ObjectClassRegistration(StringView class_name, Function<RefPtr<Object>()> factory, ObjectClassRegistration* parent_class = nullptr);
    ~ObjectClassRegistration();

    String class_name() const { return m_class_name; }
    const ObjectClassRegistration* parent_class() const { return m_parent_class; }
    RefPtr<Object> construct() const { return m_factory(); }
    bool is_derived_from(const ObjectClassRegistration& base_class) const;

    static void for_each(Function<void(const ObjectClassRegistration&)>);
    static const ObjectClassRegistration* find(StringView class_name);

private:
    StringView m_class_name;
    Function<RefPtr<Object>()> m_factory;
    ObjectClassRegistration* m_parent_class { nullptr };
};

class InspectorServerConnection;

enum class TimerShouldFireWhenNotVisible {
    No = 0,
    Yes
};

#define C_OBJECT(klass)                                                                  \
public:                                                                                  \
    virtual const char* class_name() const override { return #klass; }                   \
    template<typename Klass = klass, class... Args>                                      \
    static NonnullRefPtr<klass> construct(Args&&... args)                                \
    {                                                                                    \
        return adopt_ref(*new Klass(forward<Args>(args)...));                            \
    }                                                                                    \
    template<typename Klass = klass, class... Args>                                      \
    static ErrorOr<NonnullRefPtr<klass>> try_create(Args&&... args)                      \
    {                                                                                    \
        return adopt_nonnull_ref_or_enomem(new (nothrow) Klass(forward<Args>(args)...)); \
    }

#define C_OBJECT_ABSTRACT(klass) \
public:                          \
    virtual const char* class_name() const override { return #klass; }

class Object
    : public RefCounted<Object>
    , public Weakable<Object> {
    // NOTE: No C_OBJECT macro for Core::Object itself.

    AK_MAKE_NONCOPYABLE(Object);
    AK_MAKE_NONMOVABLE(Object);

    IntrusiveListNode<Object> m_all_objects_list_node;

public:
    virtual ~Object();

    virtual const char* class_name() const = 0;

    const String& name() const { return m_name; }
    void set_name(String name) { m_name = move(name); }

    NonnullRefPtrVector<Object>& children() { return m_children; }
    const NonnullRefPtrVector<Object>& children() const { return m_children; }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto& child : m_children) {
            if (callback(child) == IterationDecision::Break)
                return;
        }
    }

    template<typename T, typename Callback>
    void for_each_child_of_type(Callback callback) requires IsBaseOf<Object, T>;

    template<typename T>
    T* find_child_of_type_named(const String&) requires IsBaseOf<Object, T>;

    template<typename T>
    T* find_descendant_of_type_named(const String&) requires IsBaseOf<Object, T>;

    bool is_ancestor_of(const Object&) const;

    Object* parent() { return m_parent; }
    const Object* parent() const { return m_parent; }

    void start_timer(int ms, TimerShouldFireWhenNotVisible = TimerShouldFireWhenNotVisible::No);
    void stop_timer();
    bool has_timer() const { return m_timer_id; }

    ErrorOr<void> try_add_child(Object&);

    void add_child(Object&);
    void insert_child_before(Object& new_child, Object& before_child);
    void remove_child(Object&);
    void remove_all_children();

    void set_event_filter(Function<bool(Core::Event&)>);

    void dump_tree(int indent = 0);

    void deferred_invoke(Function<void()>);

    void save_to(JsonObject&);

    bool set_property(String const& name, const JsonValue& value);
    JsonValue property(String const& name) const;
    const HashMap<String, NonnullOwnPtr<Property>>& properties() const { return m_properties; }

    static IntrusiveList<&Object::m_all_objects_list_node>& all_objects();

    void dispatch_event(Core::Event&, Object* stay_within = nullptr);

    void remove_from_parent()
    {
        if (m_parent)
            m_parent->remove_child(*this);
    }

    template<class T, class... Args>
    inline T& add(Args&&... args)
    {
        auto child = T::construct(forward<Args>(args)...);
        add_child(*child);
        return child;
    }

    template<class T, class... Args>
    inline ErrorOr<NonnullRefPtr<T>> try_add(Args&&... args)
    {
        auto child = TRY(T::try_create(forward<Args>(args)...));
        TRY(try_add_child(*child));
        return child;
    }

    virtual bool is_visible_for_timer_purposes() const;

    bool is_being_inspected() const { return m_inspector_count; }

    void increment_inspector_count(Badge<InspectorServerConnection>);
    void decrement_inspector_count(Badge<InspectorServerConnection>);

    virtual bool load_from_json(const JsonObject&, RefPtr<Core::Object> (*)(const String&)) { return false; }

protected:
    explicit Object(Object* parent = nullptr);

    void register_property(const String& name, Function<JsonValue()> getter, Function<bool(const JsonValue&)> setter = nullptr);

    virtual void event(Core::Event&);

    virtual void timer_event(TimerEvent&);
    virtual void custom_event(CustomEvent&);

    // NOTE: You may get child events for children that are not yet fully constructed!
    virtual void child_event(ChildEvent&);

    virtual void did_begin_inspection() { }
    virtual void did_end_inspection() { }

private:
    Object* m_parent { nullptr };
    String m_name;
    int m_timer_id { 0 };
    unsigned m_inspector_count { 0 };
    HashMap<String, NonnullOwnPtr<Property>> m_properties;
    NonnullRefPtrVector<Object> m_children;
    Function<bool(Core::Event&)> m_event_filter;
};

}

template<>
struct AK::Formatter<Core::Object> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, const Core::Object& value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}({})", value.class_name(), &value);
    }
};

namespace Core {
template<typename T, typename Callback>
inline void Object::for_each_child_of_type(Callback callback) requires IsBaseOf<Object, T>
{
    for_each_child([&](auto& child) {
        if (is<T>(child))
            return callback(static_cast<T&>(child));
        return IterationDecision::Continue;
    });
}

template<typename T>
T* Object::find_child_of_type_named(const String& name) requires IsBaseOf<Object, T>
{
    T* found_child = nullptr;
    for_each_child_of_type<T>([&](auto& child) {
        if (child.name() == name) {
            found_child = &child;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return found_child;
}

template<typename T>
T* Object::find_descendant_of_type_named(String const& name) requires IsBaseOf<Object, T>
{
    if (is<T>(*this) && this->name() == name) {
        return static_cast<T*>(this);
    }
    T* found_child = nullptr;
    for_each_child([&](auto& child) {
        found_child = child.template find_descendant_of_type_named<T>(name);
        if (found_child)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    return found_child;
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

#define REGISTER_STRING_PROPERTY(property_name, getter, setter) \
    register_property(                                          \
        property_name,                                          \
        [this] { return this->getter(); },                      \
        [this](auto& value) {                                   \
            this->setter(value.to_string());                    \
            return true;                                        \
        });

#define REGISTER_READONLY_STRING_PROPERTY(property_name, getter) \
    register_property(                                           \
        property_name,                                           \
        [this] { return this->getter(); },                       \
        {});

#define REGISTER_RECT_PROPERTY(property_name, getter, setter)          \
    register_property(                                                 \
        property_name,                                                 \
        [this] {                                                       \
            auto rect = this->getter();                                \
            JsonObject rect_object;                                    \
            rect_object.set("x", rect.x());                            \
            rect_object.set("y", rect.y());                            \
            rect_object.set("width", rect.width());                    \
            rect_object.set("height", rect.height());                  \
            return rect_object;                                        \
        },                                                             \
        [this](auto& value) {                                          \
            if (!value.is_object())                                    \
                return false;                                          \
            Gfx::IntRect rect;                                         \
            rect.set_x(value.as_object().get("x").to_i32());           \
            rect.set_y(value.as_object().get("y").to_i32());           \
            rect.set_width(value.as_object().get("width").to_i32());   \
            rect.set_height(value.as_object().get("height").to_i32()); \
            setter(rect);                                              \
            return true;                                               \
        });

#define REGISTER_SIZE_PROPERTY(property_name, getter, setter)          \
    register_property(                                                 \
        property_name,                                                 \
        [this] {                                                       \
            auto size = this->getter();                                \
            JsonObject size_object;                                    \
            size_object.set("width", size.width());                    \
            size_object.set("height", size.height());                  \
            return size_object;                                        \
        },                                                             \
        [this](auto& value) {                                          \
            if (!value.is_object())                                    \
                return false;                                          \
            Gfx::IntSize size;                                         \
            size.set_width(value.as_object().get("width").to_i32());   \
            size.set_height(value.as_object().get("height").to_i32()); \
            setter(size);                                              \
            return true;                                               \
        });

#define REGISTER_ENUM_PROPERTY(property_name, getter, setter, EnumType, ...) \
    register_property(                                                       \
        property_name,                                                       \
        [this]() -> JsonValue {                                              \
            struct {                                                         \
                EnumType enum_value;                                         \
                String string_value;                                         \
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
                String string_value;                                         \
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
        { Gfx::TextAlignment::TopLeft, "TopLeft" },                     \
        { Gfx::TextAlignment::TopRight, "TopRight" },                   \
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
}
