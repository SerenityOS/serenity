/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/NavigationPrototype.h>
#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/ErrorEvent.h>
#include <LibWeb/HTML/NavigateEvent.h>
#include <LibWeb/HTML/Navigation.h>
#include <LibWeb/HTML/NavigationCurrentEntryChangeEvent.h>
#include <LibWeb/HTML/NavigationHistoryEntry.h>
#include <LibWeb/HTML/NavigationTransition.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

static NavigationResult navigation_api_method_tracker_derived_result(JS::NonnullGCPtr<NavigationAPIMethodTracker> api_method_tracker);

NavigationAPIMethodTracker::NavigationAPIMethodTracker(JS::NonnullGCPtr<Navigation> navigation,
    Optional<String> key,
    JS::Value info,
    Optional<SerializationRecord> serialized_state,
    JS::GCPtr<NavigationHistoryEntry> commited_to_entry,
    JS::NonnullGCPtr<WebIDL::Promise> committed_promise,
    JS::NonnullGCPtr<WebIDL::Promise> finished_promise)
    : navigation(navigation)
    , key(move(key))
    , info(info)
    , serialized_state(move(serialized_state))
    , commited_to_entry(commited_to_entry)
    , committed_promise(committed_promise)
    , finished_promise(finished_promise)
{
}

void NavigationAPIMethodTracker::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(navigation);
    visitor.visit(info);
    visitor.visit(commited_to_entry);
    visitor.visit(committed_promise);
    visitor.visit(finished_promise);
}

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
    visitor.visit(m_ongoing_navigate_event);
    visitor.visit(m_ongoing_api_method_tracker);
    visitor.visit(m_upcoming_non_traverse_api_method_tracker);
    for (auto& key_and_tracker : m_upcoming_traverse_api_method_trackers)
        visitor.visit(key_and_tracker.value);
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

    // FIXME 2. Assert: navigation's current entry index is not −1.
    // FIXME: This should not happen once Navigable's algorithms properly set up NHEs.
    if (m_current_entry_index == -1)
        return nullptr;

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
        return WebIDL::InvalidStateError::create(realm(), "Cannot update current NavigationHistoryEntry when there is no current entry"_fly_string);

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

    // FIXME 2. Assert: navigation's current entry index is not −1.
    // FIXME: This should not happen once Navigable's algorithms properly set up NHEs.
    if (m_current_entry_index == -1)
        return false;

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

    // FIXME 2. Assert: navigation's current entry index is not −1.
    // FIXME: This should not happen once Navigable's algorithms properly set up NHEs.
    if (m_current_entry_index == -1)
        return false;

    // 3. If this's current entry index is equal to this's entry list's size, then return false.
    // 4. Return true.
    return (m_current_entry_index != static_cast<i64>(m_entry_list.size()));
}

HistoryHandlingBehavior to_history_handling_behavior(Bindings::NavigationHistoryBehavior b)
{
    // A history handling behavior is a NavigationHistoryBehavior that is either "push" or "replace",
    // i.e., that has been resolved away from any initial "auto" value.
    VERIFY(b != Bindings::NavigationHistoryBehavior::Auto);

    switch (b) {
    case Bindings::NavigationHistoryBehavior::Push:
        return HistoryHandlingBehavior::Push;
    case Bindings::NavigationHistoryBehavior::Replace:
        return HistoryHandlingBehavior::Replace;
    case Bindings::NavigationHistoryBehavior::Auto:
        VERIFY_NOT_REACHED();
    };
    VERIFY_NOT_REACHED();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-navigate
WebIDL::ExceptionOr<NavigationResult> Navigation::navigate(String url, NavigationNavigateOptions const& options)
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    // The navigate(options) method steps are:

    // 1. Parse url relative to this's relevant settings object.
    //    If that returns failure, then return an early error result for a "SyntaxError" DOMException.
    //    Otherwise, let urlRecord be the resulting URL record.
    auto url_record = relevant_settings_object(*this).parse_url(url);
    if (!url_record.is_valid())
        return early_error_result(WebIDL::SyntaxError::create(realm, "Cannot navigate to Invalid URL"_fly_string));

    // 2. Let document be this's relevant global object's associated Document.
    auto& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

    // 3. If options["history"] is "push", and the navigation must be a replace given urlRecord and document,
    //    then return an early error result for a "NotSupportedError" DOMException.
    if (options.history == Bindings::NavigationHistoryBehavior::Push && navigation_must_be_a_replace(url_record, document))
        return early_error_result(WebIDL::NotSupportedError::create(realm, "Navigation must be a replace, but push was requested"_fly_string));

    // 4. Let state be options["state"], if it exists; otherwise, undefined.
    auto state = options.state.value_or(JS::js_undefined());

    // 5. Let serializedState be StructuredSerializeForStorage(state).
    //    If this throws an exception, then return an early error result for that exception.
    // FIXME: Fix this spec grammaro in the note
    // NOTE: It is importantly to perform this step early, since serialization can invoke web developer code,
    //       which in turn might change various things we check in later steps.
    auto serialized_state_or_error = structured_serialize_for_storage(vm, state);
    if (serialized_state_or_error.is_error()) {
        return early_error_result(serialized_state_or_error.release_error());
    }

    auto serialized_state = serialized_state_or_error.release_value();

    // 6. If document is not fully active, then return an early error result for an "InvalidStateError" DOMException.
    if (!document.is_fully_active())
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document is not fully active"_fly_string));

    // 7. If document's unload counter is greater than 0, then return an early error result for an "InvalidStateError" DOMException.
    if (document.unload_counter() > 0)
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document already unloaded"_fly_string));

    // 8. Let info be options["info"], if it exists; otherwise, undefined.
    auto info = options.info.value_or(JS::js_undefined());

    // 9. Let apiMethodTracker be the result of maybe setting the upcoming non-traverse API method tracker for this
    //    given info and serializedState.
    auto api_method_tracker = maybe_set_the_upcoming_non_traverse_api_method_tracker(info, serialized_state);

    // 10. Navigate document's node navigable to urlRecord using document,
    //     with historyHandling set to options["history"] and navigationAPIState set to serializedState.
    // FIXME: Fix spec typo here
    // NOTE: Unlike location.assign() and friends, which are exposed across origin-domain boundaries,
    //       navigation.navigate() can only be accessed by code with direct synchronous access to the
    ///      window.navigation property. Thus, we avoid the complications about attributing the source document
    //       of the navigation, and we don't need to deal with the allowed by sandboxing to navigate check and its
    //       acccompanying exceptionsEnabled flag. We just treat all navigations as if they come from the Document
    //       corresponding to this Navigation object itself (i.e., document).
    [[maybe_unused]] auto history_handling_behavior = to_history_handling_behavior(options.history);
    // FIXME: Actually call navigate once Navigables are implemented enough to guarantee a node navigable on
    //        an active document that's not being unloaded.
    //        document.navigable().navigate(url, document, history behavior, state)

    // 11. If this's upcoming non-traverse API method tracker is apiMethodTracker, then:
    // NOTE: If the upcoming non-traverse API method tracker is still apiMethodTracker, this means that the navigate
    //       algorithm bailed out before ever getting to the inner navigate event firing algorithm which would promote
    //       that upcoming API method tracker to ongoing.
    if (m_upcoming_non_traverse_api_method_tracker == api_method_tracker) {
        m_upcoming_non_traverse_api_method_tracker = nullptr;
        return early_error_result(WebIDL::AbortError::create(realm, "Navigation aborted"_fly_string));
    }

    // 12. Return a navigation API method tracker-derived result for apiMethodTracker.
    return navigation_api_method_tracker_derived_result(api_method_tracker);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-reload
WebIDL::ExceptionOr<NavigationResult> Navigation::reload(NavigationReloadOptions const& options)
{
    auto& realm = this->realm();
    auto& vm = this->vm();
    // The reload(options) method steps are:

    // 1. Let document be this's relevant global object's associated Document.
    auto& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

    // 2. Let serializedState be StructuredSerializeForStorage(undefined).
    auto serialized_state = MUST(structured_serialize_for_storage(vm, JS::js_undefined()));

    // 3. If options["state"] exists, then set serializedState to StructuredSerializeForStorage(options["state"]).
    //    If this throws an exception, then return an early error result for that exception.
    // NOTE: It is importantly to perform this step early, since serialization can invoke web developer
    //       code, which in turn might change various things we check in later steps.
    if (options.state.has_value()) {
        auto serialized_state_or_error = structured_serialize_for_storage(vm, options.state.value());
        if (serialized_state_or_error.is_error())
            return early_error_result(serialized_state_or_error.release_error());
        serialized_state = serialized_state_or_error.release_value();
    }

    // 4. Otherwise:
    else {
        // 1. Let current be the current entry of this.
        auto current = current_entry();

        // 2. If current is not null, then set serializedState to current's session history entry's navigation API state.
        if (current != nullptr)
            serialized_state = current->session_history_entry().navigation_api_state;
    }

    // 5. If document is not fully active, then return an early error result for an "InvalidStateError" DOMException.
    if (!document.is_fully_active())
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document is not fully active"_fly_string));

    // 6. If document's unload counter is greater than 0, then return an early error result for an "InvalidStateError" DOMException.
    if (document.unload_counter() > 0)
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document already unloaded"_fly_string));

    // 7. Let info be options["info"], if it exists; otherwise, undefined.
    auto info = options.info.value_or(JS::js_undefined());

    // 8. Let apiMethodTracker be the result of maybe setting the upcoming non-traverse API method tracker for this given info and serializedState.
    auto api_method_tracker = maybe_set_the_upcoming_non_traverse_api_method_tracker(info, serialized_state);

    // 9. Reload document's node navigable with navigationAPIState set to serializedState.
    // FIXME: Actually call reload once Navigables are implemented enough to guarantee a node navigable on
    //        an active document that's not being unloaded.
    //        document.navigable().reload(state)

    return navigation_api_method_tracker_derived_result(api_method_tracker);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-traverseto
WebIDL::ExceptionOr<NavigationResult> Navigation::traverse_to(String key, NavigationOptions const& options)
{
    auto& realm = this->realm();
    // The traverseTo(key, options) method steps are:

    // 1. If this's current entry index is −1, then return an early error result for an "InvalidStateError" DOMException.
    if (m_current_entry_index == -1)
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Cannot traverseTo: no current session history entry"_fly_string));

    // 2. If this's entry list does not contain a NavigationHistoryEntry whose session history entry's navigation API key equals key,
    //    then return an early error result for an "InvalidStateError" DOMException.
    auto it = m_entry_list.find_if([&key](auto const& entry) {
        return entry->session_history_entry().navigation_api_key == key;
    });
    if (it == m_entry_list.end())
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Cannot traverseTo: key not found in session history list"_fly_string));

    // 3. Return the result of performing a navigation API traversal given this, key, and options.
    return perform_a_navigation_api_traversal(key, options);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#performing-a-navigation-api-traversal
WebIDL::ExceptionOr<NavigationResult> Navigation::back(NavigationOptions const& options)
{
    auto& realm = this->realm();
    // The back(options) method steps are:

    // 1. If this's current entry index is −1 or 0, then return an early error result for an "InvalidStateError" DOMException.
    if (m_current_entry_index == -1 || m_current_entry_index == 0)
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Cannot navigate back: no previous session history entry"_fly_string));

    // 2. Let key be this's entry list[this's current entry index − 1]'s session history entry's navigation API key.
    auto key = m_entry_list[m_current_entry_index - 1]->session_history_entry().navigation_api_key;

    // 3. Return the result of performing a navigation API traversal given this, key, and options.
    return perform_a_navigation_api_traversal(key, options);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-forward
WebIDL::ExceptionOr<NavigationResult> Navigation::forward(NavigationOptions const& options)
{
    auto& realm = this->realm();
    // The forward(options) method steps are:

    // 1. If this's current entry index is −1 or is equal to this's entry list's size − 1,
    //    then return an early error result for an "InvalidStateError" DOMException.
    if (m_current_entry_index == -1 || m_current_entry_index == static_cast<i64>(m_entry_list.size() - 1))
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Cannot navigate forward: no next session history entry"_fly_string));

    // 2. Let key be this's entry list[this's current entry index + 1]'s session history entry's navigation API key.
    auto key = m_entry_list[m_current_entry_index + 1]->session_history_entry().navigation_api_key;

    // 3. Return the result of performing a navigation API traversal given this, key, and options.
    return perform_a_navigation_api_traversal(key, options);
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

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-api-early-error-result
NavigationResult Navigation::early_error_result(AnyException e)
{
    auto& vm = this->vm();

    // An early error result for an exception e is a NavigationResult dictionary instance given by
    // «[ "committed" → a promise rejected with e, "finished" → a promise rejected with e ]».
    auto throw_completion = Bindings::dom_exception_to_throw_completion(vm, e);
    return {
        .committed = WebIDL::create_rejected_promise(realm(), *throw_completion.value())->promise(),
        .finished = WebIDL::create_rejected_promise(realm(), *throw_completion.value())->promise(),
    };
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-api-method-tracker-derived-result
NavigationResult navigation_api_method_tracker_derived_result(JS::NonnullGCPtr<NavigationAPIMethodTracker> api_method_tracker)
{
    // A navigation API method tracker-derived result for a navigation API method tracker is a NavigationResult
    /// dictionary instance given by «[ "committed" apiMethodTracker's committed promise, "finished" → apiMethodTracker's finished promise ]».
    return {
        api_method_tracker->committed_promise->promise(),
        api_method_tracker->finished_promise->promise(),
    };
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#upcoming-non-traverse-api-method-tracker
JS::NonnullGCPtr<NavigationAPIMethodTracker> Navigation::maybe_set_the_upcoming_non_traverse_api_method_tracker(JS::Value info, Optional<SerializationRecord> serialized_state)
{
    auto& realm = relevant_realm(*this);
    auto& vm = this->vm();
    // To maybe set the upcoming non-traverse API method tracker given a Navigation navigation,
    // a JavaScript value info, and a serialized state-or-null serializedState:

    // 1. Let committedPromise and finishedPromise be new promises created in navigation's relevant realm.
    auto committed_promise = WebIDL::create_promise(realm);
    auto finished_promise = WebIDL::create_promise(realm);

    // 2. Mark as handled finishedPromise.
    // NOTE: The web developer doesn’t necessarily care about finishedPromise being rejected:
    //       - They might only care about committedPromise.
    //       - They could be doing multiple synchronous navigations within the same task,
    //         in which case all but the last will be aborted (causing their finishedPromise to reject).
    //         This could be an application bug, but also could just be an emergent feature of disparate
    //         parts of the application overriding each others' actions.
    //       - They might prefer to listen to other transition-failure signals instead of finishedPromise, e.g.,
    //         the navigateerror event, or the navigation.transition.finished promise.
    //       As such, we mark it as handled to ensure that it never triggers unhandledrejection events.
    WebIDL::mark_promise_as_handled(finished_promise);

    // 3. Let apiMethodTracker be a new navigation API method tracker with:
    //     navigation object: navigation
    //     key:               null
    //     info:              info
    //     serialized state:  serializedState
    //     comitted-to entry: null
    //     comitted promise:  committedPromise
    //     finished promise:  finishedPromise
    auto api_method_tracker = vm.heap().allocate_without_realm<NavigationAPIMethodTracker>(
        /* .navigation = */ *this,
        /* .key = */ OptionalNone {},
        /* .info = */ info,
        /* .serialized_state = */ move(serialized_state),
        /* .commited_to_entry = */ nullptr,
        /* .committed_promise = */ committed_promise,
        /* .finished_promise = */ finished_promise);

    // 4. Assert: navigation's upcoming non-traverse API method tracker is null.
    VERIFY(m_upcoming_non_traverse_api_method_tracker == nullptr);

    // 5. If navigation does not have entries and events disabled,
    //    then set navigation's upcoming non-traverse API method tracker to apiMethodTracker.
    // NOTE: If navigation has entries and events disabled, then committedPromise and finishedPromise will never fulfill
    //      (since we never create a NavigationHistoryEntry object for such Documents, and so we have nothing to resolve them with);
    //      there is no NavigationHistoryEntry to apply serializedState to; and there is no navigate event to include info with.
    //      So, we don't need to track this API method call after all.
    if (!has_entries_and_events_disabled())
        m_upcoming_non_traverse_api_method_tracker = api_method_tracker;

    // 6. Return apiMethodTracker.
    return api_method_tracker;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#add-an-upcoming-traverse-api-method-tracker
JS::NonnullGCPtr<NavigationAPIMethodTracker> Navigation::add_an_upcoming_traverse_api_method_tracker(String destination_key, JS::Value info)
{
    auto& vm = this->vm();
    auto& realm = relevant_realm(*this);
    // To add an upcoming traverse API method tracker given a Navigation navigation, a string destinationKey, and a JavaScript value info:

    // 1. Let committedPromise and finishedPromise be new promises created in navigation's relevant realm.
    auto committed_promise = WebIDL::create_promise(realm);
    auto finished_promise = WebIDL::create_promise(realm);

    // 2. Mark as handled finishedPromise.
    // NOTE: See the previous discussion about why this is done
    //       https://html.spec.whatwg.org/multipage/nav-history-apis.html#note-mark-as-handled-navigation-api-finished
    WebIDL::mark_promise_as_handled(*finished_promise);

    // 3. Let apiMethodTracker be a new navigation API method tracker with:
    //     navigation object: navigation
    //     key:               destinationKey
    //     info:              info
    //     serialized state:  null
    //     comitted-to entry: null
    //     comitted promise:  committedPromise
    //     finished promise:  finishedPromise
    auto api_method_tracker = vm.heap().allocate_without_realm<NavigationAPIMethodTracker>(
        /* .navigation = */ *this,
        /* .key = */ destination_key,
        /* .info = */ info,
        /* .serialized_state = */ OptionalNone {},
        /* .commited_to_entry = */ nullptr,
        /* .committed_promise = */ committed_promise,
        /* .finished_promise = */ finished_promise);

    // 4. Set navigation's upcoming traverse API method trackers[key] to apiMethodTracker.
    // FIXME: Fix spec typo key --> destinationKey
    m_upcoming_traverse_api_method_trackers.set(destination_key, api_method_tracker);

    // 5. Return apiMethodTracker.
    return api_method_tracker;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#performing-a-navigation-api-traversal
WebIDL::ExceptionOr<NavigationResult> Navigation::perform_a_navigation_api_traversal(String key, NavigationOptions const& options)
{
    auto& realm = this->realm();
    // To perform a navigation API traversal given a Navigation navigation, a string key, and a NavigationOptions options:

    // 1. Let document be this's relevant global object's associated Document.
    auto& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

    // 2. If document is not fully active, then return an early error result for an "InvalidStateError" DOMException.
    if (!document.is_fully_active())
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document is not fully active"_fly_string));

    // 3. If document's unload counter is greater than 0, then return an early error result for an "InvalidStateError" DOMException.
    if (document.unload_counter() > 0)
        return early_error_result(WebIDL::InvalidStateError::create(realm, "Document already unloaded"_fly_string));

    // 4. Let current be the current entry of navigation.
    auto current = current_entry();

    // 5. If key equals current's session history entry's navigation API key, then return
    //    «[ "committed" → a promise resolved with current, "finished" → a promise resolved with current ]».
    if (key == current->session_history_entry().navigation_api_key) {
        return NavigationResult {
            .committed = WebIDL::create_resolved_promise(realm, current)->promise(),
            .finished = WebIDL::create_resolved_promise(realm, current)->promise()
        };
    }

    // 6. If navigation's upcoming traverse API method trackers[key] exists,
    //    then return a navigation API method tracker-derived result for navigation's upcoming traverse API method trackers[key].
    if (auto maybe_tracker = m_upcoming_traverse_api_method_trackers.get(key); maybe_tracker.has_value())
        return navigation_api_method_tracker_derived_result(maybe_tracker.value());

    // 7. Let info be options["info"], if it exists; otherwise, undefined.
    auto info = options.info.value_or(JS::js_undefined());

    // 8. Let apiMethodTracker be the result of adding an upcoming traverse API method tracker for navigation given key and info.
    auto api_method_tracker = add_an_upcoming_traverse_api_method_tracker(key, info);

    // 9. Let navigable be document's node navigable.
    auto navigable = document.navigable();

    // 10. Let traversable be navigable's traversable navigable.
    auto traversable = navigable->traversable_navigable();

    // 11. Let sourceSnapshotParams be the result of snapshotting source snapshot params given document.
    auto source_snapshot_params = document.snapshot_source_snapshot_params();

    // 12. Append the following session history traversal steps to traversable:
    traversable->append_session_history_traversal_steps([key, api_method_tracker, navigable, source_snapshot_params, this] {
        // 1. Let navigableSHEs be the result of getting session history entries given navigable.
        auto navigable_shes = navigable->get_session_history_entries();

        // 2. Let targetSHE be the session history entry in navigableSHEs whose navigation API key is key. If no such entry exists, then:
        auto it = navigable_shes.find_if([&key](auto const& entry) {
            return entry->navigation_api_key == key;
        });
        if (it == navigable_shes.end()) {
            // NOTE: This path is taken if navigation's entry list was outdated compared to navigableSHEs,
            //       which can occur for brief periods while all the relevant threads and processes are being synchronized in reaction to a history change.

            // 1. Queue a global task on the navigation and traversal task source given navigation's relevant global object
            //    to reject the finished promise for apiMethodTracker with an "InvalidStateError" DOMException.
            queue_global_task(HTML::Task::Source::NavigationAndTraversal, relevant_global_object(*this), [this, api_method_tracker] {
                auto& reject_realm = relevant_realm(*this);
                WebIDL::reject_promise(reject_realm, api_method_tracker->finished_promise,
                    WebIDL::InvalidStateError::create(reject_realm, "Cannot traverse with stale session history entry"_fly_string));
            });

            // 2. Abort these steps.
            return;
        }
        auto target_she = *it;

        // 3. If targetSHE is navigable's active session history entry, then abort these steps.
        // NOTE: This can occur if a previously queued traversal already took us to this session history entry.
        //       In that case the previous traversal will have dealt with apiMethodTracker already.
        if (target_she == navigable->active_session_history_entry())
            return;

        // FIXME: 4. Let result be the result of applying the traverse history step given by targetSHE's step to traversable,
        //    given sourceSnapshotParams, navigable, and "none".
        (void)source_snapshot_params;

        // NOTE: When result is "canceled-by-beforeunload" or "initiator-disallowed", the navigate event was never fired,
        //       aborting the ongoing navigation would not be correct; it would result in a navigateerror event without a
        //       preceding navigate event. In the "canceled-by-navigate" case, navigate is fired, but the inner navigate event
        //       firing algorithm will take care of aborting the ongoing navigation.

        // FIXME: 5. If result is "canceled-by-beforeunload", then queue a global task on the navigation and traversal task source
        //           given navigation's relevant global object to reject the finished promise for apiMethodTracker with a
        //           new "AbortError"DOMException created in navigation's relevant realm.

        // FIXME: 6. If result is "initiator-disallowed", then queue a global task on the navigation and traversal task source
        //           given navigation's relevant global object to reject the finished promise for apiMethodTracker with a
        //           new "SecurityError" DOMException created in navigation's relevant realm.
    });

    // 13. Return a navigation API method tracker-derived result for apiMethodTracker.
    return navigation_api_method_tracker_derived_result(api_method_tracker);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#abort-the-ongoing-navigation
void Navigation::abort_the_ongoing_navigation(Optional<JS::NonnullGCPtr<WebIDL::DOMException>> error)
{
    auto& realm = relevant_realm(*this);

    // To abort the ongoing navigation given a Navigation navigation and an optional DOMException error:

    // 1. Let event be navigation's ongoing navigate event.
    auto event = ongoing_navigate_event();

    // 2. Assert: event is not null.
    VERIFY(event != nullptr);

    // 3. Set navigation's focus changed during ongoing navigation to false.
    m_focus_changed_during_ongoing_navigation = false;

    // 4. Set navigation's suppress normal scroll restoration during ongoing navigation to false.
    m_suppress_scroll_restoration_during_ongoing_navigation = false;

    // 5. If error was not given, then let error be a new "AbortError" DOMException created in navigation's relevant realm.
    if (!error.has_value())
        error = WebIDL::AbortError::create(realm, "Navigation aborted"_fly_string);

    VERIFY(error.has_value());

    // 6. If event's dispatch flag is set, then set event's canceled flag to true.
    if (event->dispatched())
        event->set_cancelled(true);

    // 7. Signal abort on event's abort controller given error.
    event->abort_controller()->abort(error.value());

    // 8. Set navigation's ongoing navigate event to null.
    m_ongoing_navigate_event = nullptr;

    // 9. Fire an event named navigateerror at navigation using ErrorEvent, with error initialized to error,
    //   and message, filename, lineno, and colno initialized to appropriate values that can be extracted
    //   from error and the current JavaScript stack in the same underspecified way that the report the exception algorithm does.
    ErrorEventInit event_init = {};
    event_init.error = error.value();
    // FIXME: Extract information from the exception and the JS context in the wishy-washy way the spec says here.
    event_init.filename = String {};
    event_init.colno = 0;
    event_init.lineno = 0;
    event_init.message = String {};

    dispatch_event(ErrorEvent::create(realm, EventNames::navigateerror, event_init));

    // 10. If navigation's ongoing API method tracker is non-null, then reject the finished promise for apiMethodTracker with error.
    if (m_ongoing_api_method_tracker != nullptr)
        WebIDL::reject_promise(realm, m_ongoing_api_method_tracker->finished_promise, error.value());

    // 11. If navigation's transition is not null, then:
    if (m_transition != nullptr) {
        // 1. Reject navigation's transition's finished promise with error.
        m_transition->finished()->reject(error.value());

        // 2. Set navigation's transition to null.
        m_transition = nullptr;
    }
}

}
