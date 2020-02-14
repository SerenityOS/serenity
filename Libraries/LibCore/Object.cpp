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

#include <AK/Assertions.h>
#include <AK/JsonObject.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <stdio.h>

namespace Core {

IntrusiveList<Object, &Object::m_all_objects_list_node>& Object::all_objects()
{
    static IntrusiveList<Object, &Object::m_all_objects_list_node> objects;
    return objects;
}

Object::Object(Object* parent, bool is_widget)
    : m_parent(parent)
    , m_widget(is_widget)
{
    all_objects().append(*this);
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
        ASSERT_NOT_REACHED();
        break;
    case Core::Event::Custom:
        return custom_event(static_cast<CustomEvent&>(event));
    default:
        break;
    }
}

void Object::add_child(Object& object)
{
    // FIXME: Should we support reparenting objects?
    ASSERT(!object.parent() || object.parent() == this);
    object.m_parent = this;
    m_children.append(object);
    event(*make<Core::ChildEvent>(Core::Event::ChildAdded, object));
}

void Object::insert_child_before(Object& new_child, Object& before_child)
{
    // FIXME: Should we support reparenting objects?
    ASSERT(!new_child.parent() || new_child.parent() == this);
    new_child.m_parent = this;
    m_children.insert_before_matching(new_child, [&](auto& existing_child) { return existing_child.ptr() == &before_child; });
    event(*make<Core::ChildEvent>(Core::Event::ChildAdded, new_child, &before_child));
}

void Object::remove_child(Object& object)
{
    for (int i = 0; i < m_children.size(); ++i) {
        if (m_children.ptr_at(i).ptr() == &object) {
            // NOTE: We protect the child so it survives the handling of ChildRemoved.
            NonnullRefPtr<Object> protector = object;
            object.m_parent = nullptr;
            m_children.remove(i);
            event(*make<Core::ChildEvent>(Core::Event::ChildRemoved, object));
            return;
        }
    }
    ASSERT_NOT_REACHED();
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
        dbgprintf("Object{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }

    m_timer_id = Core::EventLoop::register_timer(*this, ms, true, fire_when_not_visible);
}

void Object::stop_timer()
{
    if (!m_timer_id)
        return;
    bool success = Core::EventLoop::unregister_timer(m_timer_id);
    ASSERT(success);
    m_timer_id = 0;
}

void Object::dump_tree(int indent)
{
    for (int i = 0; i < indent; ++i) {
        printf(" ");
    }
    printf("%s{%p}\n", class_name(), this);

    for_each_child([&](auto& child) {
        child.dump_tree(indent + 2);
        return IterationDecision::Continue;
    });
}

void Object::deferred_invoke(Function<void(Object&)> invokee)
{
    Core::EventLoop::current().post_event(*this, make<Core::DeferredInvocationEvent>(move(invokee)));
}

void Object::save_to(JsonObject& json)
{
    json.set("class_name", class_name());
    json.set("address", String::format("%p", this));
    json.set("name", name());
    json.set("parent", String::format("%p", parent()));
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
    ASSERT(!stay_within || stay_within == this || stay_within->is_ancestor_of(*this));
    auto* target = this;
    do {
        target->event(e);
        target = target->parent();
        if (target == stay_within) {
            // Prevent the event from bubbling any further.
            e.accept();
            break;
        }
    } while (target && !e.is_accepted());
}

bool Object::is_visible_for_timer_purposes() const
{
    if (parent())
        return parent()->is_visible_for_timer_purposes();
    return true;
}

const LogStream& operator<<(const LogStream& stream, const Object& object)
{
    return stream << object.class_name() << '{' << &object << '}';
}

}
