/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Badge.h>
#include <AK/JsonObject.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <stdio.h>

namespace Core {

IntrusiveList<&Object::m_all_objects_list_node>& Object::all_objects()
{
    static IntrusiveList<&Object::m_all_objects_list_node> objects;
    return objects;
}

Object::Object(Object* parent)
    : m_parent(parent)
{
    all_objects().append(*this);
    if (m_parent)
        m_parent->add_child(*this);

    REGISTER_READONLY_STRING_PROPERTY("class_name", class_name);
    REGISTER_STRING_PROPERTY("name", name, set_name);

    register_property(
        "address", [this] { return FlatPtr(this); },
        [](auto&) { return false; });

    register_property(
        "parent", [this] { return FlatPtr(this->parent()); },
        [](auto&) { return false; });
}

Object::~Object()
{
    // NOTE: We move our children out to a stack vector to prevent other
    //       code from trying to iterate over them.
    auto children = move(m_children);
    // NOTE: We also unparent the children, so that they won't try to unparent
    //       themselves in their own destructors.
    for (auto& child : children)
        child.m_parent = nullptr;

    all_objects().remove(*this);
    stop_timer();
    if (m_parent)
        m_parent->remove_child(*this);
}

void Object::event(Core::Event& event)
{
    switch (event.type()) {
    case Core::Event::Timer:
        return timer_event(static_cast<TimerEvent&>(event));
    case Core::Event::ChildAdded:
    case Core::Event::ChildRemoved:
        return child_event(static_cast<ChildEvent&>(event));
    case Core::Event::Invalid:
        VERIFY_NOT_REACHED();
        break;
    case Core::Event::Custom:
        return custom_event(static_cast<CustomEvent&>(event));
    default:
        break;
    }
}

ErrorOr<void> Object::try_add_child(Object& object)
{
    // FIXME: Should we support reparenting objects?
    VERIFY(!object.parent() || object.parent() == this);
    TRY(m_children.try_append(object));
    object.m_parent = this;
    Core::ChildEvent child_event(Core::Event::ChildAdded, object);
    event(child_event);
    return {};
}

void Object::add_child(Object& object)
{
    MUST(try_add_child(object));
}

void Object::insert_child_before(Object& new_child, Object& before_child)
{
    // FIXME: Should we support reparenting objects?
    VERIFY(!new_child.parent() || new_child.parent() == this);
    new_child.m_parent = this;
    m_children.insert_before_matching(new_child, [&](auto& existing_child) { return existing_child.ptr() == &before_child; });
    Core::ChildEvent child_event(Core::Event::ChildAdded, new_child, &before_child);
    event(child_event);
}

void Object::remove_child(Object& object)
{
    for (size_t i = 0; i < m_children.size(); ++i) {
        if (m_children.ptr_at(i).ptr() == &object) {
            // NOTE: We protect the child so it survives the handling of ChildRemoved.
            NonnullRefPtr<Object> protector = object;
            object.m_parent = nullptr;
            m_children.remove(i);
            Core::ChildEvent child_event(Core::Event::ChildRemoved, object);
            event(child_event);
            return;
        }
    }
    VERIFY_NOT_REACHED();
}

void Object::remove_all_children()
{
    while (!m_children.is_empty())
        m_children.first().remove_from_parent();
}

void Object::timer_event(Core::TimerEvent&)
{
}

void Object::child_event(Core::ChildEvent&)
{
}

void Object::custom_event(CustomEvent&)
{
}

void Object::start_timer(int ms, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    if (m_timer_id) {
        dbgln("{} {:p} already has a timer!", class_name(), this);
        VERIFY_NOT_REACHED();
    }

    m_timer_id = Core::EventLoop::register_timer(*this, ms, true, fire_when_not_visible);
}

void Object::stop_timer()
{
    if (!m_timer_id)
        return;
    bool success = Core::EventLoop::unregister_timer(m_timer_id);
    VERIFY(success);
    m_timer_id = 0;
}

void Object::dump_tree(int indent)
{
    for (int i = 0; i < indent; ++i) {
        out(" ");
    }
    out("{}{{{:p}}}", class_name(), this);
    if (!name().is_null())
        out(" {}", name());
    outln();

    for_each_child([&](auto& child) {
        child.dump_tree(indent + 2);
        return IterationDecision::Continue;
    });
}

void Object::deferred_invoke(Function<void()> invokee)
{
    Core::deferred_invoke([invokee = move(invokee), strong_this = NonnullRefPtr(*this)] { invokee(); });
}

void Object::save_to(JsonObject& json)
{
    for (auto& it : m_properties) {
        auto& property = it.value;
        json.set(property->name(), property->get());
    }
}

JsonValue Object::property(String const& name) const
{
    auto it = m_properties.find(name);
    if (it == m_properties.end())
        return JsonValue();
    return it->value->get();
}

bool Object::set_property(String const& name, JsonValue const& value)
{
    auto it = m_properties.find(name);
    if (it == m_properties.end())
        return false;
    return it->value->set(value);
}

bool Object::is_ancestor_of(const Object& other) const
{
    if (&other == this)
        return false;
    for (auto* ancestor = other.parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor == this)
            return true;
    }
    return false;
}

void Object::dispatch_event(Core::Event& e, Object* stay_within)
{
    VERIFY(!stay_within || stay_within == this || stay_within->is_ancestor_of(*this));
    auto* target = this;
    do {
        // If there's an event filter on this target, ask if it wants to swallow this event.
        if (target->m_event_filter && !target->m_event_filter(e))
            return;
        target->event(e);
        target = target->parent();
        if (target == stay_within) {
            // Prevent the event from bubbling any further.
            return;
        }
    } while (target && !e.is_accepted());
}

bool Object::is_visible_for_timer_purposes() const
{
    if (parent())
        return parent()->is_visible_for_timer_purposes();
    return true;
}

void Object::increment_inspector_count(Badge<InspectorServerConnection>)
{
    ++m_inspector_count;
    if (m_inspector_count == 1)
        did_begin_inspection();
}

void Object::decrement_inspector_count(Badge<InspectorServerConnection>)
{
    --m_inspector_count;
    if (!m_inspector_count)
        did_end_inspection();
}

void Object::register_property(const String& name, Function<JsonValue()> getter, Function<bool(const JsonValue&)> setter)
{
    m_properties.set(name, make<Property>(name, move(getter), move(setter)));
}

void Object::set_event_filter(Function<bool(Core::Event&)> filter)
{
    m_event_filter = move(filter);
}

static HashMap<StringView, ObjectClassRegistration*>& object_classes()
{
    static HashMap<StringView, ObjectClassRegistration*>* map;
    if (!map)
        map = new HashMap<StringView, ObjectClassRegistration*>;
    return *map;
}

ObjectClassRegistration::ObjectClassRegistration(StringView class_name, Function<RefPtr<Object>()> factory, ObjectClassRegistration* parent_class)
    : m_class_name(class_name)
    , m_factory(move(factory))
    , m_parent_class(parent_class)
{
    object_classes().set(class_name, this);
}

ObjectClassRegistration::~ObjectClassRegistration()
{
}

bool ObjectClassRegistration::is_derived_from(const ObjectClassRegistration& base_class) const
{
    if (&base_class == this)
        return true;
    if (!m_parent_class)
        return false;
    return m_parent_class->is_derived_from(base_class);
}

void ObjectClassRegistration::for_each(Function<void(const ObjectClassRegistration&)> callback)
{
    for (auto& it : object_classes()) {
        callback(*it.value);
    }
}

const ObjectClassRegistration* ObjectClassRegistration::find(StringView class_name)
{
    return object_classes().get(class_name).value_or(nullptr);
}
}
