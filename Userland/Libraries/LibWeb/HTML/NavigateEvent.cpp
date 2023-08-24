/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Console.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/NavigateEventPrototype.h>
#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/NavigateEvent.h>
#include <LibWeb/HTML/NavigationDestination.h>
#include <LibWeb/XHR/FormData.h>

namespace Web::HTML {

JS::NonnullGCPtr<NavigateEvent> NavigateEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, NavigateEventInit const& event_init)
{
    return realm.heap().allocate<NavigateEvent>(realm, realm, event_name, event_init);
}

NavigateEvent::NavigateEvent(JS::Realm& realm, FlyString const& event_name, NavigateEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_navigation_type(event_init.navigation_type)
    , m_destination(*event_init.destination)
    , m_can_intercept(event_init.can_intercept)
    , m_user_initiated(event_init.user_initiated)
    , m_hash_change(event_init.hash_change)
    , m_signal(*event_init.signal)
    , m_form_data(event_init.form_data)
    , m_download_request(event_init.download_request)
    , m_info(event_init.info.value_or(JS::js_undefined()))
    , m_has_ua_visual_transition(event_init.has_ua_visual_transition)
{
}

NavigateEvent::~NavigateEvent() = default;

void NavigateEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::NavigateEventPrototype>(realm, "NavigateEvent"));
}

void NavigateEvent::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& handler : m_navigation_handler_list)
        visitor.visit(handler);
    visitor.visit(m_abort_controller);
    visitor.visit(m_destination);
    visitor.visit(m_signal);
    visitor.visit(m_form_data);
    visitor.visit(m_info);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigateevent-intercept
WebIDL::ExceptionOr<void> NavigateEvent::intercept(NavigationInterceptOptions const& options)
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    // The intercept(options) method steps are:

    // 1. Perform shared checks given this.
    TRY(perform_shared_checks());

    // 2. If this's canIntercept attribute was initialized to false, then throw a "SecurityError" DOMException.
    if (!m_can_intercept)
        return WebIDL::SecurityError::create(realm, "NavigateEvent cannot be intercepted");

    // 3. If this's dispatch flag is unset, then throw an "InvalidStateError" DOMException.
    if (!this->dispatched())
        return WebIDL::InvalidStateError::create(realm, "NavigationEvent is not dispatched yet");

    // 4. Assert: this's interception state is either "none" or "intercepted".
    VERIFY(m_interception_state == InterceptionState::None || m_interception_state == InterceptionState::Intercepted);

    // 5. Set this's interception state to "intercepted".
    m_interception_state = InterceptionState::Intercepted;

    // 6. If options["handler"] exists, then append it to this's navigation handler list.
    if (options.handler != nullptr)
        TRY_OR_THROW_OOM(vm, m_navigation_handler_list.try_append(*options.handler));

    // 7. If options["focusReset"] exists, then:
    if (options.focus_reset.has_value()) {
        // 1.  If this's focus reset behavior is not null, and it is not equal to options["focusReset"],
        //     then the user agent may report a warning to the console indicating that the focusReset option
        //     for a previous call to intercept() was overridden by this new value, and the previous value
        //     will be ignored.
        if (m_focus_reset_behavior.has_value() && *m_focus_reset_behavior != *options.focus_reset) {
            auto& console = realm.intrinsics().console_object()->console();
            console.output_debug_message(JS::Console::LogLevel::Warn,
                TRY_OR_THROW_OOM(vm, String::formatted("focusReset behavior on NavigationEvent overriden (was: {}, now: {})", *m_focus_reset_behavior, *options.focus_reset)));
        }

        // 2. Set this's focus reset behavior to options["focusReset"].
        m_focus_reset_behavior = options.focus_reset;
    }

    // 8. If options["scroll"] exists, then:
    if (options.scroll.has_value()) {
        // 1. If this's scroll behavior is not null, and it is not equal to options["scroll"], then the user
        //    agent may report a warning to the console indicating that the scroll option for a previous call
        //    to intercept() was overridden by this new value, and the previous value will be ignored.
        if (m_scroll_behavior.has_value() && *m_scroll_behavior != *options.scroll) {
            auto& console = realm.intrinsics().console_object()->console();
            console.output_debug_message(JS::Console::LogLevel::Warn,
                TRY_OR_THROW_OOM(vm, String::formatted("scroll option on NavigationEvent overriden (was: {}, now: {})", *m_scroll_behavior, *options.scroll)));
        }

        // 2. Set this's scroll behavior to options["scroll"].
        m_scroll_behavior = options.scroll;
    }
    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigateevent-scroll
WebIDL::ExceptionOr<void> NavigateEvent::scroll()
{
    // The scroll() method steps are:
    // 1. Perform shared checks given this.
    TRY(perform_shared_checks());

    // 2. If this's interception state is not "committed", then throw an "InvalidStateError" DOMException.
    if (m_interception_state != InterceptionState::Committed)
        return WebIDL::InvalidStateError::create(realm(), "Cannot scroll NavigationEvent that is not committed");

    // 3. Process scroll behavior given this.
    process_scroll_behavior();

    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigateevent-perform-shared-checks
WebIDL::ExceptionOr<void> NavigateEvent::perform_shared_checks()
{
    // To perform shared checks for a NavigateEvent event:

    // 1. If event's relevant global object's associated Document is not fully active,
    //    then throw an "InvalidStateError" DOMException.
    auto& associated_document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();
    if (!associated_document.is_fully_active())
        return WebIDL::InvalidStateError::create(realm(), "Document is not fully active");

    // 2. If event's isTrusted attribute was initialized to false, then throw a "SecurityError" DOMException.
    if (!this->is_trusted())
        return WebIDL::SecurityError::create(realm(), "NavigateEvent is not trusted");

    // 3. If event's canceled flag is set, then throw an "InvalidStateError" DOMException.
    if (this->cancelled())
        return WebIDL::InvalidStateError::create(realm(), "NavigateEvent already cancelled");

    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#process-scroll-behavior
void NavigateEvent::process_scroll_behavior()
{
    // To process scroll behavior given a NavigateEvent event:

    // 1. Assert: event's interception state is "committed".
    VERIFY(m_interception_state == InterceptionState::Committed);

    // 2. Set event's interception state to "scrolled".
    m_interception_state = InterceptionState::Scrolled;

    // FIXME: 3. If event's navigationType was initialized to "traverse" or "reload", then restore scroll position data
    //           given event's relevant global object's navigable's active session history entry.
    if (m_navigation_type == Bindings::NavigationType::Traverse || m_navigation_type == Bindings::NavigationType::Reload) {
        dbgln("FIXME: restore scroll position data after traversal or reload navigation");
    }

    // 4. Otherwise:
    else {
        // 1. Let document be event's relevant global object's associated Document.
        auto& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

        // 2. If document's indicated part is null, then scroll to the beginning of the document given document. [CSSOMVIEW]
        auto indicated_part = document.determine_the_indicated_part();
        if (indicated_part.has<DOM::Element*>() && indicated_part.get<DOM::Element*>() == nullptr) {
            document.scroll_to_the_beginning_of_the_document();
        }

        // 3. Otherwise, scroll to the fragment given document.
        else {
            // FIXME: This will re-determine the indicated part. Can we avoid this extra work?
            document.scroll_to_the_fragment();
        }
    }
}

}
