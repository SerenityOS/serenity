/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

Object::Object(Object* parent)
    : m_parent(parent)
{
    if (m_parent)
        m_parent->add_child(*this);
}

Object::~Object()
{
    // NOTE: We move our children out to a stack vector to prevent other
    //       code from trying to iterate over them.
    auto children = move(m_children);
    // NOTE: We also unparent the children, so that they won't try to unparent
    //       themselves in their own destructors.
    for (auto& child : children)
        child->m_parent = nullptr;

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
        if (m_children[i] == &object) {
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
        m_children.first()->remove_from_parent();
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
    if (!success) {
        dbgln("{} {:p} could not unregister timer {}", class_name(), this, m_timer_id);
    }
    m_timer_id = 0;
}

void Object::deferred_invoke(Function<void()> invokee)
{
    Core::deferred_invoke([invokee = move(invokee), strong_this = NonnullRefPtr(*this)] { invokee(); });
}

bool Object::is_ancestor_of(Object const& other) const
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

void Object::set_event_filter(Function<bool(Core::Event&)> filter)
{
    m_event_filter = move(filter);
}

}
