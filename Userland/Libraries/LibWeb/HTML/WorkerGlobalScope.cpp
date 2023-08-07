/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/WorkerGlobalScopePrototype.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>
#include <LibWeb/HTML/WorkerLocation.h>
#include <LibWeb/HTML/WorkerNavigator.h>

namespace Web::HTML {

WorkerGlobalScope::WorkerGlobalScope(JS::Realm& realm)
    : DOM::EventTarget(realm)

{
}

WorkerGlobalScope::~WorkerGlobalScope() = default;

void WorkerGlobalScope::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    m_navigator = MUST(WorkerNavigator::create(*this));
}

void WorkerGlobalScope::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    WindowOrWorkerGlobalScopeMixin::visit_edges(visitor);

    visitor.visit(m_location.ptr());
    visitor.visit(m_navigator.ptr());
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
