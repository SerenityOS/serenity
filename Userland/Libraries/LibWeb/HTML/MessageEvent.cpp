/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/MessageEvent.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(MessageEvent);

JS::NonnullGCPtr<MessageEvent> MessageEvent::create(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return realm.heap().allocate<MessageEvent>(realm, realm, event_name, event_init);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<MessageEvent>> MessageEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

MessageEvent::MessageEvent(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_data(event_init.data)
    , m_origin(event_init.origin)
    , m_last_event_id(event_init.last_event_id)
    , m_source(event_init.source)
{
    m_ports.ensure_capacity(event_init.ports.size());
    for (auto& port : event_init.ports) {
        VERIFY(port);
        m_ports.unchecked_append(*port);
    }
}

MessageEvent::~MessageEvent() = default;

void MessageEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(MessageEvent);
}

void MessageEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
    visitor.visit(m_ports_array);
    for (auto& port : m_ports)
        visitor.visit(port);
}

Variant<JS::Handle<WindowProxy>, JS::Handle<MessagePort>, Empty> MessageEvent::source() const
{
    if (!m_source.has_value())
        return Empty {};

    return m_source.value().downcast<JS::Handle<WindowProxy>, JS::Handle<MessagePort>>();
}

JS::NonnullGCPtr<JS::Object> MessageEvent::ports() const
{
    if (!m_ports_array) {
        Vector<JS::Value> port_vector;
        for (auto const& port : m_ports) {
            port_vector.append(port);
        }
        m_ports_array = JS::Array::create_from(realm(), port_vector);
        MUST(m_ports_array->set_integrity_level(IntegrityLevel::Frozen));
    }
    return *m_ports_array;
}

}
