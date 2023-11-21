/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/DedicatedWorkerExposedInterfaces.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/WorkerGlobalScopePrototype.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>
#include <LibWeb/HTML/WorkerLocation.h>
#include <LibWeb/HTML/WorkerNavigator.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(WorkerGlobalScope);

WorkerGlobalScope::WorkerGlobalScope(JS::Realm& realm, Web::Page& page)
    : DOM::EventTarget(realm)
    , m_page(page)
{
}

WorkerGlobalScope::~WorkerGlobalScope() = default;

void WorkerGlobalScope::initialize_web_interfaces(Badge<WorkerEnvironmentSettingsObject>)
{
    auto& realm = this->realm();
    Base::initialize(realm);

    // FIXME: Handle shared worker
    add_dedicated_worker_exposed_interfaces(*this);

    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::WorkerGlobalScopePrototype>(realm, "WorkerGlobalScope"_fly_string));

    WindowOrWorkerGlobalScopeMixin::initialize(realm);

    m_navigator = WorkerNavigator::create(*this);
}

void WorkerGlobalScope::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    WindowOrWorkerGlobalScopeMixin::visit_edges(visitor);

    visitor.visit(m_location);
    visitor.visit(m_navigator);
}

void WorkerGlobalScope::set_outside_port(NonnullOwnPtr<Core::BufferedLocalSocket> port)
{
    m_outside_port = move(port);

    // FIXME: Hide this logic in MessagePort
    m_outside_port->set_notifications_enabled(true);
    m_outside_port->on_ready_to_read = [this] {
        auto& vm = this->vm();
        auto& realm = this->realm();

        auto num_bytes_ready = MUST(m_outside_port->pending_bytes());
        switch (m_outside_port_state) {
        case PortState::Header: {
            if (num_bytes_ready < 8)
                break;
            auto const magic = MUST(m_outside_port->read_value<u32>());
            if (magic != 0xDEADBEEF) {
                m_outside_port_state = PortState::Error;
                break;
            }
            m_outside_port_incoming_message_size = MUST(m_outside_port->read_value<u32>());
            num_bytes_ready -= 8;
            m_outside_port_state = PortState::Data;
        }
            [[fallthrough]];
        case PortState::Data: {
            if (num_bytes_ready < m_outside_port_incoming_message_size)
                break;
            SerializationRecord rec; // FIXME: Keep in class scope
            rec.resize(m_outside_port_incoming_message_size / sizeof(u32));
            MUST(m_outside_port->read_until_filled(to_bytes(rec.span())));

            TemporaryExecutionContext cxt(relevant_settings_object(*this));
            MessageEventInit event_init {};
            event_init.data = MUST(structured_deserialize(vm, rec, realm, {}));
            // FIXME: Fill in the rest of the info from MessagePort

            this->dispatch_event(MessageEvent::create(realm, EventNames::message, event_init));

            m_outside_port_state = PortState::Header;
            break;
        }
        case PortState::Error:
            VERIFY_NOT_REACHED();
            break;
        }
    };
}

// https://html.spec.whatwg.org/multipage/workers.html#importing-scripts-and-libraries
WebIDL::ExceptionOr<void> WorkerGlobalScope::import_scripts(Vector<String> urls)
{
    // The algorithm may optionally be customized by supplying custom perform the fetch hooks,
    // which if provided will be used when invoking fetch a classic worker-imported script.
    // NOTE: Service Workers is an example of a specification that runs this algorithm with its own options for the perform the fetch hook.

    // FIXME: 1. If worker global scope's type is "module", throw a TypeError exception.
    // FIXME: 2. Let settings object be the current settings object.

    // 3. If urls is empty, return.
    if (urls.is_empty())
        return {};

    // FIXME: 4. Parse each value in urls relative to settings object. If any fail, throw a "SyntaxError" DOMException.
    // FIXME: 5. For each url in the resulting URL records, run these substeps:
    //     1. Fetch a classic worker-imported script given url and settings object, passing along any custom perform the fetch steps provided.
    //        If this succeeds, let script be the result. Otherwise, rethrow the exception.
    //     2. Run the classic script script, with the rethrow errors argument set to true.
    //        NOTE: script will run until it either returns, fails to parse, fails to catch an exception,
    //              or gets prematurely aborted by the terminate a worker algorithm defined above.
    //        If an exception was thrown or if the script was prematurely aborted, then abort all these steps,
    //        letting the exception or aborting continue to be processed by the calling script.

    return {};
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-workerglobalscope-location
JS::NonnullGCPtr<WorkerLocation> WorkerGlobalScope::location() const
{
    // The location attribute must return the WorkerLocation object whose associated WorkerGlobalScope object is the WorkerGlobalScope object.
    return *m_location;
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-navigator
JS::NonnullGCPtr<WorkerNavigator> WorkerGlobalScope::navigator() const
{
    // The navigator attribute of the WorkerGlobalScope interface must return an instance of the WorkerNavigator interface,
    // which represents the identity and state of the user agent (the client).
    return *m_navigator;
}

WebIDL::ExceptionOr<void> WorkerGlobalScope::post_message(JS::Value message, JS::Value)
{
    auto& realm = this->realm();
    auto& vm = this->vm();

    // FIXME: Use the with-transfer variant, which should(?) prepend the magic + size at the front
    auto data = TRY(structured_serialize(vm, message));

    Array<u32, 2> header = { 0xDEADBEEF, static_cast<u32>(data.size() * sizeof(u32)) };

    if (auto const err = m_outside_port->write_until_depleted(to_readonly_bytes(header.span())); err.is_error())
        return WebIDL::DataCloneError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("{}", err.error())));

    if (auto const err = m_outside_port->write_until_depleted(to_readonly_bytes(data.span())); err.is_error())
        return WebIDL::DataCloneError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("{}", err.error())));

    return {};
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                               \
    void WorkerGlobalScope::set_##attribute_name(WebIDL::CallbackType* value) \
    {                                                                         \
        set_event_handler_attribute(event_name, move(value));                 \
    }                                                                         \
    WebIDL::CallbackType* WorkerGlobalScope::attribute_name()                 \
    {                                                                         \
        return event_handler_attribute(event_name);                           \
    }
ENUMERATE_WORKER_GLOBAL_SCOPE_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
