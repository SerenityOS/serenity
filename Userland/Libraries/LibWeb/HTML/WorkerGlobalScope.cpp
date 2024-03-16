/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibWeb/Bindings/DedicatedWorkerExposedInterfaces.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/WorkerGlobalScopePrototype.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>
#include <LibWeb/HTML/WorkerLocation.h>
#include <LibWeb/HTML/WorkerNavigator.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(WorkerGlobalScope);

WorkerGlobalScope::WorkerGlobalScope(JS::Realm& realm, JS::NonnullGCPtr<Web::Page> page)
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

    WEB_SET_PROTOTYPE_FOR_INTERFACE(WorkerGlobalScope);

    WindowOrWorkerGlobalScopeMixin::initialize(realm);

    m_navigator = WorkerNavigator::create(*this);
}

void WorkerGlobalScope::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    WindowOrWorkerGlobalScopeMixin::visit_edges(visitor);

    visitor.visit(m_location);
    visitor.visit(m_navigator);
    visitor.visit(m_internal_port);
    visitor.visit(m_page);
}

void WorkerGlobalScope::finalize()
{
    Base::finalize();
    WindowOrWorkerGlobalScopeMixin::finalize();
}

void WorkerGlobalScope::set_internal_port(JS::NonnullGCPtr<MessagePort> port)
{
    m_internal_port = port;
    m_internal_port->set_worker_event_target(*this);
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

WebIDL::ExceptionOr<void> WorkerGlobalScope::post_message(JS::Value message, StructuredSerializeOptions const& options)
{
    return m_internal_port->post_message(message, options);
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
