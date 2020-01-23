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
#include <AK/kstdio.h>
#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CObject.h>
#include <stdio.h>

IntrusiveList<CObject, &CObject::m_all_objects_list_node>& CObject::all_objects()
{
    static IntrusiveList<CObject, &CObject::m_all_objects_list_node> objects;
    return objects;
}

CObject::CObject(CObject* parent, bool is_widget)
    : m_parent(parent)
    , m_widget(is_widget)
{
    all_objects().append(*this);
    if (m_parent)
        m_parent->add_child(*this);
}

CObject::~CObject()
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

void CObject::event(CEvent& event)
{
    switch (event.type()) {
    case CEvent::Timer:
        return timer_event(static_cast<CTimerEvent&>(event));
    case CEvent::ChildAdded:
    case CEvent::ChildRemoved:
        return child_event(static_cast<CChildEvent&>(event));
    case CEvent::Invalid:
        ASSERT_NOT_REACHED();
        break;
    case CEvent::Custom:
        return custom_event(static_cast<CCustomEvent&>(event));
    default:
        break;
    }
}

void CObject::add_child(CObject& object)
{
    // FIXME: Should we support reparenting objects?
    ASSERT(!object.parent() || object.parent() == this);
    object.m_parent = this;
    m_children.append(object);
    event(*make<CChildEvent>(CEvent::ChildAdded, object));
}

void CObject::insert_child_before(CObject& new_child, CObject& before_child)
{
    // FIXME: Should we support reparenting objects?
    ASSERT(!new_child.parent() || new_child.parent() == this);
    new_child.m_parent = this;
    m_children.insert_before_matching(new_child, [&](auto& existing_child) { return existing_child.ptr() == &before_child; });
    event(*make<CChildEvent>(CEvent::ChildAdded, new_child, &before_child));
}

void CObject::remove_child(CObject& object)
{
    for (int i = 0; i < m_children.size(); ++i) {
        if (m_children.ptr_at(i).ptr() == &object) {
            // NOTE: We protect the child so it survives the handling of ChildRemoved.
            NonnullRefPtr<CObject> protector = object;
            object.m_parent = nullptr;
            m_children.remove(i);
            event(*make<CChildEvent>(CEvent::ChildRemoved, object));
            return;
        }
    }
    ASSERT_NOT_REACHED();
}

void CObject::timer_event(CTimerEvent&)
{
}

void CObject::child_event(CChildEvent&)
{
}

void CObject::custom_event(CCustomEvent&)
{
}

void CObject::start_timer(int ms, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    if (m_timer_id) {
        dbgprintf("CObject{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }

    m_timer_id = CEventLoop::register_timer(*this, ms, true, fire_when_not_visible);
}

void CObject::stop_timer()
{
    if (!m_timer_id)
        return;
    bool success = CEventLoop::unregister_timer(m_timer_id);
    ASSERT(success);
    m_timer_id = 0;
}

void CObject::dump_tree(int indent)
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

void CObject::deferred_invoke(Function<void(CObject&)> invokee)
{
    CEventLoop::current().post_event(*this, make<CDeferredInvocationEvent>(move(invokee)));
}

void CObject::save_to(JsonObject& json)
{
    json.set("class_name", class_name());
    json.set("address", String::format("%p", this));
    json.set("name", name());
    json.set("parent", String::format("%p", parent()));
}

bool CObject::is_ancestor_of(const CObject& other) const
{
    if (&other == this)
        return false;
    for (auto* ancestor = other.parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor == this)
            return true;
    }
    return false;
}

void CObject::dispatch_event(CEvent& e, CObject* stay_within)
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

bool CObject::is_visible_for_timer_purposes() const
{
    if (parent())
        return parent()->is_visible_for_timer_purposes();
    return true;
}
