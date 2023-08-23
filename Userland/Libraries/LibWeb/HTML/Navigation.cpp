/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/NavigationPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Navigation.h>
#include <LibWeb/HTML/NavigationCurrentEntryChangeEvent.h>
#include <LibWeb/HTML/NavigationHistoryEntry.h>
#include <LibWeb/HTML/NavigationTransition.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

JS::NonnullGCPtr<Navigation> Navigation::create(JS::Realm& realm)
{
    return realm.heap().allocate<Navigation>(realm, realm);
}

Navigation::Navigation(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

Navigation::~Navigation() = default;

void Navigation::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::NavigationPrototype>(realm, "Navigation"));
}

void Navigation::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& entry : m_entry_list)
        visitor.visit(entry);
    visitor.visit(m_transition);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-entries
Vector<JS::NonnullGCPtr<NavigationHistoryEntry>> Navigation::entries() const
{
    // The entries() method steps are:

    // 1. If this has entries and events disabled, then return the empty list.
    if (has_entries_and_events_disabled())
        return {};

    // 2. Return this's entry list.
    //    NOTE: Recall that because of Web IDL's sequence type conversion rules,
    //          this will create a new JavaScript array object on each call.
    //          That is, navigation.entries() !== navigation.entries().
    return m_entry_list;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-current-entry
JS::GCPtr<NavigationHistoryEntry> Navigation::current_entry() const
{
    // The current entry of a Navigation navigation is the result of running the following steps:

    // 1. If navigation has entries and events disabled, then return null.
    if (has_entries_and_events_disabled())
        return nullptr;

    // 2. Assert: navigation's current entry index is not −1.
    VERIFY(m_current_entry_index != -1);

    // 3. Return navigation's entry list[navigation's current entry index].
    return m_entry_list[m_current_entry_index];
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-updatecurrententry
WebIDL::ExceptionOr<void> Navigation::update_current_entry(NavigationUpdateCurrentEntryOptions options)
{
    // The updateCurrentEntry(options) method steps are:

    // 1. Let current be the current entry of this.
    auto current = current_entry();

    // 2. If current is null, then throw an "InvalidStateError" DOMException.
    if (current == nullptr)
        return WebIDL::InvalidStateError::create(realm(), "Cannot update current NavigationHistoryEntry when there is no current entry"sv);

    // 3. Let serializedState be StructuredSerializeForStorage(options["state"]), rethrowing any exceptions.
    auto serialized_state = TRY(structured_serialize_for_storage(vm(), options.state));

    // 4. Set current's session history entry's navigation API state to serializedState.
    current->session_history_entry().navigation_api_state = serialized_state;

    // 5. Fire an event named currententrychange at this using NavigationCurrentEntryChangeEvent,
    //    with its navigationType attribute initialized to null and its from initialized to current.
    NavigationCurrentEntryChangeEventInit event_init = {};
    event_init.navigation_type = {};
    event_init.from = current;
    dispatch_event(HTML::NavigationCurrentEntryChangeEvent::create(realm(), HTML::EventNames::currententrychange, event_init));

    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-cangoback
bool Navigation::can_go_back() const
{
    // The canGoBack getter steps are:

    // 1. If this has entries and events disabled, then return false.
    if (has_entries_and_events_disabled())
        return false;

    // 2. Assert: this's current entry index is not −1.
    VERIFY(m_current_entry_index != -1);

    // 3. If this's current entry index is 0, then return false.
    // 4. Return true.
    return (m_current_entry_index != 0);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-cangoforward
bool Navigation::can_go_forward() const
{
    // The canGoForward getter steps are:

    // 1. If this has entries and events disabled, then return false.
    if (has_entries_and_events_disabled())
        return false;

    // 2. Assert: this's current entry index is not −1.
    VERIFY(m_current_entry_index != -1);

    // 3. If this's current entry index is equal to this's entry list's size, then return false.
    // 4. Return true.
    return (m_current_entry_index != static_cast<i64>(m_entry_list.size()));
}

void Navigation::set_onnavigate(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::navigate, event_handler);
}

WebIDL::CallbackType* Navigation::onnavigate()
{
    return event_handler_attribute(HTML::EventNames::navigate);
}

void Navigation::set_onnavigatesuccess(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::navigatesuccess, event_handler);
}

WebIDL::CallbackType* Navigation::onnavigatesuccess()
{
    return event_handler_attribute(HTML::EventNames::navigatesuccess);
}

void Navigation::set_onnavigateerror(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::navigateerror, event_handler);
}

WebIDL::CallbackType* Navigation::onnavigateerror()
{
    return event_handler_attribute(HTML::EventNames::navigateerror);
}

void Navigation::set_oncurrententrychange(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::currententrychange, event_handler);
}

WebIDL::CallbackType* Navigation::oncurrententrychange()
{
    return event_handler_attribute(HTML::EventNames::currententrychange);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#has-entries-and-events-disabled
bool Navigation::has_entries_and_events_disabled() const
{
    // A Navigation navigation has entries and events disabled if the following steps return true:

    // 1. Let document be navigation's relevant global object's associated Document.
    auto const& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

    // 2. If document is not fully active, then return true.
    if (!document.is_fully_active())
        return true;

    // 3. If document's is initial about:blank is true, then return true.
    if (document.is_initial_about_blank())
        return true;

    // 4. If document's origin is opaque, then return true.
    if (document.origin().is_opaque())
        return true;

    // 5. Return false.
    return false;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#getting-the-navigation-api-entry-index
i64 Navigation::get_the_navigation_api_entry_index(SessionHistoryEntry const& she) const
{
    // To get the navigation API entry index of a session history entry she within a Navigation navigation:

    // 1. Let index be 0.
    i64 index = 0;

    // 2. For each nhe of navigation's entry list:
    for (auto const& nhe : m_entry_list) {
        // 1. If nhe's session history entry is equal to she, then return index.
        if (&nhe->session_history_entry() == &she)
            return index;

        // 2. Increment index by 1.
        ++index;
    }

    // 3. Return −1.
    return -1;
}

}
