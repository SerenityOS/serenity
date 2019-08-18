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
    all_objects().remove(*this);
    stop_timer();
    if (m_parent)
        m_parent->remove_child(*this);
    auto children_to_delete = move(m_children);
    for (auto* child : children_to_delete)
        delete child;
}

void CObject::event(CEvent& event)
{
    switch (event.type()) {
    case CEvent::Timer:
        return timer_event(static_cast<CTimerEvent&>(event));
    case CEvent::DeferredDestroy:
        delete this;
        break;
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
    m_children.append(&object);
    event(*make<CChildEvent>(CEvent::ChildAdded, object));
}

void CObject::remove_child(CObject& object)
{
    for (ssize_t i = 0; i < m_children.size(); ++i) {
        if (m_children[i] == &object) {
            m_children.remove(i);
            event(*make<CChildEvent>(CEvent::ChildRemoved, object));
            return;
        }
    }
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

void CObject::start_timer(int ms)
{
    if (m_timer_id) {
        dbgprintf("CObject{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }

    m_timer_id = CEventLoop::register_timer(*this, ms, true);
}

void CObject::stop_timer()
{
    if (!m_timer_id)
        return;
    bool success = CEventLoop::unregister_timer(m_timer_id);
    ASSERT(success);
    m_timer_id = 0;
}

void CObject::delete_later()
{
    CEventLoop::current().post_event(*this, make<CEvent>(CEvent::DeferredDestroy));
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
