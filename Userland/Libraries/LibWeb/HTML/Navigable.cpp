/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentLoading.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Infrastructure/URL.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/Navigation.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/POSTResource.h>
#include <LibWeb/HTML/SandboxingFlagSet.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/StructuredSerialize.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/XHR/FormData.h>

namespace Web::HTML {

class ResponseHolder : public JS::Cell {
    JS_CELL(ResponseHolder, JS::Cell);

public:
    [[nodiscard]] static JS::NonnullGCPtr<ResponseHolder> create(JS::VM& vm)
    {
        return vm.heap().allocate_without_realm<ResponseHolder>();
    }

    [[nodiscard]] JS::GCPtr<Fetch::Infrastructure::Response> response() const { return m_response; }
    void set_response(JS::GCPtr<Fetch::Infrastructure::Response> response) { m_response = response; }

    virtual void visit_edges(Cell::Visitor& visitor) override
    {
        visitor.visit(m_response);
    }

private:
    JS::GCPtr<Fetch::Infrastructure::Response> m_response;
};

static HashTable<Navigable*>& all_navigables()
{
    static HashTable<Navigable*> set;
    return set;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#child-navigable
Vector<JS::Handle<Navigable>> Navigable::child_navigables() const
{
    Vector<JS::Handle<Navigable>> results;
    for (auto& entry : all_navigables()) {
        if (entry->parent() == this)
            results.append(entry);
    }

    return results;
}

Navigable::Navigable()
{
    all_navigables().set(this);
}

Navigable::~Navigable()
{
    all_navigables().remove(this);
}

void Navigable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent);
    visitor.visit(m_current_session_history_entry);
    visitor.visit(m_active_session_history_entry);
    visitor.visit(m_container);
}

JS::GCPtr<Navigable> Navigable::navigable_with_active_document(JS::NonnullGCPtr<DOM::Document> document)
{
    for (auto* navigable : all_navigables()) {
        if (navigable->active_document() == document)
            return navigable;
    }
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#initialize-the-navigable
ErrorOr<void> Navigable::initialize_navigable(JS::NonnullGCPtr<DocumentState> document_state, JS::GCPtr<Navigable> parent)
{
    static int next_id = 0;
    m_id = TRY(String::number(next_id++));

    // 1. Let entry be a new session history entry, with
    JS::NonnullGCPtr<SessionHistoryEntry> entry = *heap().allocate_without_realm<SessionHistoryEntry>();

    // URL: document's URL
    entry->url = document_state->document()->url();

    // document state: documentState
    entry->document_state = document_state;

    // 2. Set navigable's current session history entry to entry.
    m_current_session_history_entry = entry;

    // 3. Set navigable's active session history entry to entry.
    m_active_session_history_entry = entry;

    // 4. Set navigable's parent to parent.
    m_parent = parent;

    return {};
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-the-target-history-entry
JS::GCPtr<SessionHistoryEntry> Navigable::get_the_target_history_entry(int target_step) const
{
    // 1. Let entries be the result of getting session history entries for navigable.
    auto& entries = get_session_history_entries();

    // 2. Return the item in entries that has the greatest step less than or equal to step.
    JS::GCPtr<SessionHistoryEntry> result = nullptr;
    for (auto& entry : entries) {
        auto entry_step = entry->step.get<int>();
        if (entry_step <= target_step) {
            if (!result || result->step.get<int>() < entry_step) {
                result = entry;
            }
        }
    }

    return result;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#activate-history-entry
void Navigable::activate_history_entry(JS::GCPtr<SessionHistoryEntry> entry)
{
    // FIXME: 1. Save persisted state to the navigable's active session history entry.

    // 2. Let newDocument be entry's document.
    JS::GCPtr<DOM::Document> new_document = entry->document_state->document().ptr();

    // 3. Assert: newDocument's is initial about:blank is false, i.e., we never traverse
    //    back to the initial about:blank Document because it always gets replaced when we
    //    navigate away from it.
    VERIFY(!new_document->is_initial_about_blank());

    // 4. Set navigable's active session history entry to entry.
    m_active_session_history_entry = entry;

    // 5. Make active newDocument.
    new_document->make_active();

    // Not in the spec:
    if (is<TraversableNavigable>(*this) && parent() == nullptr) {
        if (auto* page = active_browsing_context()->page()) {
            page->client().page_did_start_loading(entry->url, false);
        }
    }
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-document
JS::GCPtr<DOM::Document> Navigable::active_document()
{
    // A navigable's active document is its active session history entry's document.
    return m_active_session_history_entry->document_state->document();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-bc
JS::GCPtr<BrowsingContext> Navigable::active_browsing_context()
{
    // A navigable's active browsing context is its active document's browsing context.
    // If this navigable is a traversable navigable, then its active browsing context will be a top-level browsing context.
    if (auto document = active_document())
        return document->browsing_context();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-wp
JS::GCPtr<HTML::WindowProxy> Navigable::active_window_proxy()
{
    // A navigable's active WindowProxy is its active browsing context's associated WindowProxy.
    if (auto browsing_context = active_browsing_context())
        return browsing_context->window_proxy();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-window
JS::GCPtr<HTML::Window> Navigable::active_window()
{
    // A navigable's active window is its active WindowProxy's [[Window]].
    if (auto window_proxy = active_window_proxy())
        return window_proxy->window();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-target
String Navigable::target_name() const
{
    // A navigable's target name is its active session history entry's document state's navigable target name.
    return active_session_history_entry()->document_state->navigable_target_name();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-container
JS::GCPtr<NavigableContainer> Navigable::container() const
{
    // The container of a navigable navigable is the navigable container whose nested navigable is navigable, or null if there is no such element.
    return NavigableContainer::navigable_container_with_content_navigable(const_cast<Navigable&>(*this));
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-container-document
JS::GCPtr<DOM::Document> Navigable::container_document() const
{
    auto container = this->container();

    // 1. If navigable's container is null, then return null.
    if (!container)
        return nullptr;

    // 2. Return navigable's container's node document.
    return container->document();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-traversable
JS::GCPtr<TraversableNavigable> Navigable::traversable_navigable() const
{
    // 1. Let navigable be inputNavigable.
    auto navigable = const_cast<Navigable*>(this);

    // 2. While navigable is not a traversable navigable, set navigable to navigable's parent.
    while (navigable && !is<TraversableNavigable>(*navigable))
        navigable = navigable->parent();

    // 3. Return navigable.
    return static_cast<TraversableNavigable*>(navigable);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-top
JS::GCPtr<TraversableNavigable> Navigable::top_level_traversable()
{
    // 1. Let navigable be inputNavigable.
    auto navigable = this;

    // 2. While navigable's parent is not null, set navigable to navigable's parent.
    while (navigable->parent())
        navigable = navigable->parent();

    // 3. Return navigable.
    return verify_cast<TraversableNavigable>(navigable);
}

Navigable::ChosenNavigable Navigable::choose_a_navigable(StringView name, TokenizedFeature::NoOpener, ActivateTab)
{
    // 1. Let chosen be null.
    JS::GCPtr<Navigable> chosen = nullptr;

    // 2. Let windowType be "existing or none".
    auto window_type = WindowType::ExistingOrNone;

    // 3. Let sandboxingFlagSet be current's active document's active sandboxing flag set.
    [[maybe_unused]] auto sandboxing_flag_set = active_document()->active_sandboxing_flag_set();

    // 4. If name is the empty string or an ASCII case-insensitive match for "_self", then set chosen to currentNavigable.
    if (name.is_empty() || Infra::is_ascii_case_insensitive_match(name, "_self"sv)) {
        chosen = this;
    }
    // 5. Otherwise, if name is an ASCII case-insensitive match for "_parent",
    //    set chosen to currentNavigable's parent, if any, and currentNavigable otherwise.
    else if (Infra::is_ascii_case_insensitive_match(name, "_parent"sv)) {
        if (auto parent = this->parent())
            chosen = parent;
        else
            chosen = this;
    }
    // 6. Otherwise, if name is an ASCII case-insensitive match for "_top",
    //    set chosen to currentNavigable's traversable navigable.
    else if (Infra::is_ascii_case_insensitive_match(name, "_top"sv)) {
        chosen = traversable_navigable();
    }
    //  7. Otherwise, if name is not an ASCII case-insensitive match for "_blank",
    //     there exists a navigable whose target name is the same as name, currentNavigable's
    //     active browsing context is familiar with that navigable's active browsing context,
    //     and the user agent determines that the two browsing contexts are related enough that
    //     it is ok if they reach each other, set chosen to that navigable. If there are multiple
    //     matching navigables, the user agent should pick one in some arbitrary consistent manner,
    //     such as the most recently opened, most recently focused, or more closely related, and set
    //     chosen to it.
    else if (!Infra::is_ascii_case_insensitive_match(name, "_blank"sv)) {
        TODO();
    }
    // Otherwise, a new top-level traversable is being requested, and what happens depends on the
    // user agent's configuration and abilities — it is determined by the rules given for the first
    // applicable option from the following list:
    else {
        TODO();
    }

    return { chosen.ptr(), window_type };
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-session-history-entries
Vector<JS::NonnullGCPtr<SessionHistoryEntry>>& Navigable::get_session_history_entries() const
{
    // 1. Let traversable be navigable's traversable navigable.
    auto traversable = traversable_navigable();

    // FIXME 2. Assert: this is running within traversable's session history traversal queue.

    // 3. If navigable is traversable, return traversable's session history entries.
    if (this == traversable)
        return traversable->session_history_entries();

    // 4. Let docStates be an empty ordered set of document states.
    Vector<JS::GCPtr<DocumentState>> doc_states;

    // 5. For each entry of traversable's session history entries, append entry's document state to docStates.
    for (auto& entry : traversable->session_history_entries())
        doc_states.append(entry->document_state);

    // 6. For each docState of docStates:
    while (!doc_states.is_empty()) {
        auto doc_state = doc_states.take_first();

        // 1. For each nestedHistory of docState's nested histories:
        for (auto& nested_history : doc_state->nested_histories()) {
            // 1. If nestedHistory's id equals navigable's id, return nestedHistory's entries.
            if (nested_history.id == id())
                return nested_history.entries;

            // 2. For each entry of nestedHistory's entries, append entry's document state to docStates.
            for (auto& entry : nested_history.entries)
                doc_states.append(entry->document_state);
        }
    }

    VERIFY_NOT_REACHED();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#create-navigation-params-from-a-srcdoc-resource
static WebIDL::ExceptionOr<NavigationParams> create_navigation_params_from_a_srcdoc_resource(JS::GCPtr<SessionHistoryEntry> entry, JS::GCPtr<Navigable> navigable, SourceSnapshotParams const&, Optional<String> navigation_id)
{
    auto& vm = navigable->vm();
    auto& realm = navigable->active_window()->realm();

    // 1. Let documentResource be entry's document state's resource.
    auto document_resource = entry->document_state->resource();
    VERIFY(document_resource.has<String>());

    // 2. Let response be a new response with
    //    URL: about:srcdoc
    //    header list: (`Content-Type`, `text/html`)
    //    body: the UTF-8 encoding of documentResource, as a body
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(AK::URL("about:srcdoc"));
    auto header = TRY_OR_THROW_OOM(vm, Fetch::Infrastructure::Header::from_string_pair("Content-Type"sv, "text/html"sv));
    TRY_OR_THROW_OOM(vm, response->header_list()->append(move(header)));
    response->set_body(TRY(Fetch::Infrastructure::byte_sequence_as_body(realm, document_resource.get<String>().bytes())));

    // FIXME: 3. Let responseOrigin be the result of determining the origin given response's URL, targetSnapshotParams's sandboxing flags, null, and entry's document state's origin.

    // 4. Let coop be a new cross-origin opener policy.
    CrossOriginOpenerPolicy coop;

    // 5. Let coopEnforcementResult be a new cross-origin opener policy enforcement result with
    //    url: response's URL
    //    FIXME: origin: responseOrigin
    //    cross-origin opener policy: coop
    CrossOriginOpenerPolicyEnforcementResult coop_enforcement_result {
        .url = *response->url(),
        .origin = Origin {},
        .cross_origin_opener_policy = coop
    };

    // FIXME: 6. Let policyContainer be the result of determining navigation params policy container given response's URL, entry's document state's history policy container, null, navigable's container document's policy container, and null.

    // 7. Return a new navigation params, with
    //    id: navigationId
    //    request: null
    //    response: response
    //    FIXME: origin: responseOrigin
    //    FIXME: policy container: policyContainer
    //    FIXME: final sandboxing flag set: targetSnapshotParams's sandboxing flags
    //    cross-origin opener policy: coop
    //    COOP enforcement result: coopEnforcementResult
    //    reserved environment: null
    //    navigable: navigable
    //    FIXME: navigation timing type: navTimingType
    //    fetch controller: null
    //    commit early hints: null
    HTML::NavigationParams navigation_params {
        .id = navigation_id,
        .request = {},
        .response = *response,
        .origin = Origin {},
        .policy_container = PolicyContainer {},
        .final_sandboxing_flag_set = SandboxingFlagSet {},
        .cross_origin_opener_policy = move(coop),
        .coop_enforcement_result = move(coop_enforcement_result),
        .reserved_environment = {},
        .browsing_context = navigable->active_browsing_context(),
        .navigable = navigable,
    };

    return { navigation_params };
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#create-navigation-params-by-fetching
static WebIDL::ExceptionOr<Optional<NavigationParams>> create_navigation_params_by_fetching(JS::GCPtr<SessionHistoryEntry> entry, JS::GCPtr<Navigable> navigable, SourceSnapshotParams const& source_snapshot_params, Optional<String> navigation_id)
{
    auto& vm = navigable->vm();
    auto& realm = navigable->active_window()->realm();

    // FIXME: 1. Assert: this is running in parallel.

    // 2. Let documentResource be entry's document state's resource.
    auto document_resource = entry->document_state->resource();

    // 3. Let request be a new request, with
    //    url: entry's URL
    //    client: sourceSnapshotParams's fetch client
    //    destination: "document"
    //    credentials mode: "include"
    //    use-URL-credentials flag: set
    //    redirect mode: "manual"
    //    replaces client id: navigable's active document's relevant settings object's id
    //    mode: "navigate"
    //    referrer: entry's document state's request referrer
    //    FIXME: referrer policy: entry's document state's request referrer policy
    auto request = Fetch::Infrastructure::Request::create(vm);
    request->set_url(entry->url);
    request->set_client(source_snapshot_params.fetch_client);
    request->set_destination(Fetch::Infrastructure::Request::Destination::Document);
    request->set_credentials_mode(Fetch::Infrastructure::Request::CredentialsMode::Include);
    request->set_use_url_credentials(true);
    request->set_redirect_mode(Fetch::Infrastructure::Request::RedirectMode::Manual);
    auto replaces_client_id = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(navigable->active_document()->relevant_settings_object().id));
    request->set_replaces_client_id(replaces_client_id);
    request->set_mode(Fetch::Infrastructure::Request::Mode::Navigate);
    request->set_referrer(entry->document_state->request_referrer());

    // 4. If documentResource is a POST resource, then:
    if (document_resource.has<POSTResource>()) {
        // 1. Set request's method to `POST`.
        request->set_method(TRY_OR_THROW_OOM(vm, ByteBuffer::copy("post"sv.bytes())));

        // 2. Set request's body to documentResource's request body.
        request->set_body(document_resource.get<POSTResource>().request_body.value());

        // 3. Set `Content-Type` to documentResource's request content-type in request's header list.
        auto request_content_type = document_resource.get<POSTResource>().request_content_type;
        auto request_content_type_string = [request_content_type]() {
            switch (request_content_type) {
            case POSTResource::RequestContentType::ApplicationXWWWFormUrlencoded:
                return "application/x-www-form-urlencoded"sv;
            case POSTResource::RequestContentType::MultipartFormData:
                return "multipart/form-data"sv;
            case POSTResource::RequestContentType::TextPlain:
                return "text/plain"sv;
            default:
                VERIFY_NOT_REACHED();
            }
        }();
        auto header = TRY_OR_THROW_OOM(vm, Fetch::Infrastructure::Header::from_string_pair("Content-Type"sv, request_content_type_string));
        TRY_OR_THROW_OOM(vm, request->header_list()->append(move(header)));
    }

    // 5. If entry's document state's reload pending is true, then set request's reload-navigation flag.
    if (entry->document_state->reload_pending())
        request->set_reload_navigation(true);

    // 6. Otherwise, if entry's document state's ever populated is true, then set request's history-navigation flag.
    if (entry->document_state->ever_populated())
        request->set_history_navigation(true);

    // 9. Let response be null.
    // NOTE: We use a heap-allocated cell to hold the response pointer because the processResponse callback below
    //       might use it after this stack is freed.
    auto response_holder = ResponseHolder::create(vm);

    // 10. Let responseOrigin be null.
    Optional<HTML::Origin> response_origin;

    // 11. Let fetchController be null.
    JS::GCPtr<Fetch::Infrastructure::FetchController> fetch_controller = nullptr;

    // 13. Let finalSandboxFlags be an empty sandboxing flag set.
    SandboxingFlagSet final_sandbox_flags = {};

    // 16. Let locationURL be null.
    ErrorOr<Optional<AK::URL>> location_url { OptionalNone {} };

    // 17. Let currentURL be request's current URL.
    AK::URL current_url = request->current_url();

    // FIXME: 18. Let commitEarlyHints be null.

    // 19. While true:
    while (true) {
        // FIXME: 1. If request's reserved client is not null and currentURL's origin is not the same as request's reserved client's creation URL's origin, then:
        // FIXME: 2. If request's reserved client is null, then:
        // FIXME: 3. If the result of should navigation request of type be blocked by Content Security Policy? given request and cspNavigationType is "Blocked", then set response to a network error and break. [CSP]

        // 4. Set response to null.
        response_holder->set_response(nullptr);

        // 5. If fetchController is null, then set fetchController to the result of fetching request,
        //    with processEarlyHintsResponse set to processEarlyHintsResponseas defined below, processResponse
        //    set to processResponse as defined below, and useParallelQueue set to true.
        if (!fetch_controller) {
            // FIXME: Let processEarlyHintsResponse be the following algorithm given a response earlyResponse:

            // Let processResponse be the following algorithm given a response fetchedResponse:
            auto process_response = [response_holder](JS::NonnullGCPtr<Fetch::Infrastructure::Response> fetch_response) {
                // 1. Set response to fetchedResponse.
                response_holder->set_response(fetch_response);
            };

            fetch_controller = TRY(Fetch::Fetching::fetch(
                realm,
                request,
                Fetch::Infrastructure::FetchAlgorithms::create(vm,
                    {
                        .process_request_body_chunk_length = {},
                        .process_request_end_of_body = {},
                        .process_early_hints_response = {},
                        .process_response = move(process_response),
                        .process_response_end_of_body = {},
                        .process_response_consume_body = {},
                    }),
                Fetch::Fetching::UseParallelQueue::Yes));
        }
        // 6. Otherwise, process the next manual redirect for fetchController.
        else {
            fetch_controller->process_next_manual_redirect();
        }

        // 7. Wait until either response is non-null, or navigable's ongoing navigation changes to no longer equal navigationId.
        Platform::EventLoopPlugin::the().spin_until([&]() {
            if (response_holder->response() != nullptr)
                return true;

            if (navigation_id.has_value() && (!navigable->ongoing_navigation().has<String>() || navigable->ongoing_navigation().get<String>() != *navigation_id))
                return true;

            return false;
        });
        // If the latter condition occurs, then abort fetchController, and return. Otherwise, proceed onward.
        if (navigation_id.has_value() && (!navigable->ongoing_navigation().has<String>() || navigable->ongoing_navigation().get<String>() != *navigation_id)) {
            fetch_controller->abort(realm, {});
            return OptionalNone {};
        }

        // 8. If request's body is null, then set entry's document state's resource to null.
        if (!request->body().has<Empty>()) {
            entry->document_state->set_resource(Empty {});
        }

        // 11. Set responseOrigin to the result of determining the origin given response's URL, finalSandboxFlags,
        //     entry's document state's initiator origin, and null.
        response_origin = determine_the_origin(*response_holder->response()->url(), final_sandbox_flags, entry->document_state->initiator_origin(), {});

        // 14. Set locationURL to response's location URL given currentURL's fragment.
        auto location_url = response_holder->response()->location_url(current_url.fragment());

        VERIFY(!location_url.is_error());

        // 15. If locationURL is failure or null, then break.
        if (location_url.is_error() || !location_url.value().has_value()) {
            break;
        }

        // 16. Assert: locationURL is a URL.
        VERIFY(location_url.value()->is_valid());

        // FIXME: 17. Set entry's serialized state to StructuredSerializeForStorage(null).

        // 18. Let oldDocState be entry's document state.
        auto old_doc_state = entry->document_state;

        // 19. Set entry's document state to a new document state, with
        // history policy container: a clone of the oldDocState's history policy container if it is non-null; null otherwise
        // request referrer: oldDocState's request referrer
        // request referrer policy: oldDocState's request referrer policy
        // origin: oldDocState's origin
        // resource: oldDocState's resource
        // ever populated: oldDocState's ever populated
        // navigable target name: oldDocState's navigable target name
        entry->document_state = navigable->heap().allocate_without_realm<DocumentState>();
        entry->document_state->set_history_policy_container(old_doc_state->history_policy_container());
        entry->document_state->set_request_referrer(old_doc_state->request_referrer());
        entry->document_state->set_request_referrer_policy(old_doc_state->request_referrer_policy());
        entry->document_state->set_origin(old_doc_state->origin());
        entry->document_state->set_resource(old_doc_state->resource());
        entry->document_state->set_ever_populated(old_doc_state->ever_populated());
        entry->document_state->set_navigable_target_name(old_doc_state->navigable_target_name());

        // 20. If locationURL's scheme is not an HTTP(S) scheme, then:
        if (!Fetch::Infrastructure::is_http_or_https_scheme(location_url.value()->scheme())) {
            // 1. Set entry's document state's resource to null.
            entry->document_state->set_resource(Empty {});

            // 2. Break.
            break;
        }

        // 21. Set currentURL to locationURL.
        current_url = location_url.value().value();

        // 22. Set entry's URL to currentURL.
        entry->url = current_url;
    }

    // FIXME: 20. If locationURL is a URL whose scheme is not a fetch scheme, then return a new non-fetch scheme navigation params, with
    //            initiator origin request's current URL's origin
    if (!location_url.is_error() && location_url.value().has_value() && !Fetch::Infrastructure::is_fetch_scheme(location_url.value().value().scheme())) {
        TODO();
    }

    // 21. If any of the following are true:
    //       - response is a network error;
    //       - locationURL is failure; or
    //       - locationURL is a URL whose scheme is a fetch scheme
    //     then return null.
    if (response_holder->response()->is_network_error() || location_url.is_error() || (location_url.value().has_value() && Fetch::Infrastructure::is_fetch_scheme(location_url.value().value().scheme()))) {
        return OptionalNone {};
    }

    // 22. Assert: locationURL is null and response is not a network error.
    VERIFY(!location_url.value().has_value());
    VERIFY(!response_holder->response()->is_network_error());

    // FIXME: 23. Let resultPolicyContainer be the result of determining navigation params policy container given response's
    //        URL, entry's document state's history policy container, sourceSnapshotParams's source policy container,
    //        null, and responsePolicyContainer.

    // 25. Return a new navigation params, with
    //     id: navigationId
    //     request: request
    //     response: response
    //     origin: responseOrigin
    //     FIXME: policy container: resultPolicyContainer
    //     FIXME: final sandboxing flag set: finalSandboxFlags
    //     FIXME: cross-origin opener policy: responseCOOP
    //     FIXME: COOP enforcement result: coopEnforcementResult
    //     FIXME: reserved environment: request's reserved client
    //     navigable: navigable
    //     FIXME: navigation timing type: navTimingType
    //     fetch controller: fetchController
    //     FIXME: commit early hints: commitEarlyHints
    HTML::NavigationParams navigation_params {
        .id = navigation_id,
        .request = request,
        .response = *response_holder->response(),
        .origin = *response_origin,
        .policy_container = PolicyContainer {},
        .final_sandboxing_flag_set = SandboxingFlagSet {},
        .cross_origin_opener_policy = CrossOriginOpenerPolicy {},
        .coop_enforcement_result = CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .browsing_context = navigable->active_browsing_context(),
        .navigable = navigable,
        .fetch_controller = fetch_controller,
    };

    return { navigation_params };
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#attempt-to-populate-the-history-entry's-document
WebIDL::ExceptionOr<void> Navigable::populate_session_history_entry_document(JS::GCPtr<SessionHistoryEntry> entry, Optional<NavigationParams> navigation_params, Optional<String> navigation_id, SourceSnapshotParams const& source_snapshot_params, bool allow_POST, Function<void()> completion_steps)
{
    // FIXME: 1. Assert: this is running in parallel.

    // 2. Assert: if navigationParams is non-null, then navigationParams's response is non-null.
    if (navigation_params.has_value())
        VERIFY(navigation_params->response);

    // 3. Let currentBrowsingContext be navigable's active browsing context.
    [[maybe_unused]] auto current_browsing_context = active_browsing_context();

    // 4. Let documentResource be entry's document state's resource.
    auto document_resource = entry->document_state->resource();

    // 5. If navigationParams is null, then:
    if (!navigation_params.has_value()) {
        // 1. If documentResource is a string, then set navigationParams to the result
        //    of creating navigation params from a srcdoc resource given entry, navigable,
        //    targetSnapshotParams, navigationId, and navTimingType.
        if (document_resource.has<String>()) {
            navigation_params = create_navigation_params_from_a_srcdoc_resource(entry, this, source_snapshot_params, navigation_id).release_value_but_fixme_should_propagate_errors();
        }
        // 2. Otherwise, if both of the following are true:
        //    - entry's URL's scheme is a fetch scheme; and
        //    - documentResource is null, or allowPOST is true and documentResource's request body is not failure (FIXME: check if request body is not failure)
        else if (Fetch::Infrastructure::is_fetch_scheme(entry->url.scheme()) && (document_resource.has<Empty>() || allow_POST)) {
            navigation_params = create_navigation_params_by_fetching(entry, this, source_snapshot_params, navigation_id).release_value_but_fixme_should_propagate_errors();
        }
        // FIXME: 3. Otherwise, if entry's URL's scheme is not a fetch scheme, then set navigationParams to a new non-fetch scheme navigation params, with
        //    initiator origin: entry's document state's initiator origin
        else {
            TODO();
        }
    }

    // 6. Queue a global task on the navigation and traversal task source, given navigable's active window, to run these steps:
    queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [this, entry, navigation_params, navigation_id, completion_steps = move(completion_steps)] {
        // 1. If navigable's ongoing navigation no longer equals navigationId, then run completionSteps and return.
        if (navigation_id.has_value() && (!ongoing_navigation().has<String>() || ongoing_navigation().get<String>() != *navigation_id)) {
            completion_steps();
            return;
        }

        // 2. Let failure be false.
        auto failure = false;

        // FIXME: 3. If navigationParams is a non-fetch scheme navigation params, then set entry's document state's document to the result of running attempt to create a non-fetch
        //    scheme document given entry's URL, navigable, targetSnapshotParams's sandboxing flags, navigationId, navTimingType, sourceSnapshotParams's has transient
        //    activation, and navigationParams's initiator origin.

        // 4. Otherwise, if navigationParams is null, then set failure to true.
        if (!navigation_params.has_value()) {
            failure = true;
        }

        // FIXME: 5. Otherwise, if the result of should navigation response to navigation request of type in target be blocked by Content Security Policy? given navigationParams's request,
        //    navigationParams's response, navigationParams's policy container's CSP list, cspNavigationType, and navigable is "Blocked", then set failure to true.

        // FIXME: 6. Otherwise, if navigationParams's reserved environment is non-null and the result of checking a navigation response's adherence to its embedder policy given
        //    navigationParams's response, navigable, and navigationParams's policy container's embedder policy is false, then set failure to true.

        // 8. If failure is true, then:
        if (failure) {
            // 1. Set entry's document state's document to the result of creating a document for inline content that doesn't have a DOM, given navigable, null, and navTimingType.
            //    The inline content should indicate to the user the sort of error that occurred.
            // FIXME: Use SourceGenerator to produce error page from file:///res/html/error.html
            //        and display actual error from fetch response.
            auto error_html = String::formatted("<h1>Failed to load {}</h1>"sv, entry->url).release_value_but_fixme_should_propagate_errors();
            entry->document_state->set_document(create_document_for_inline_content(this, navigation_id, error_html));

            // 2. Set entry's document state's document's salvageable to false.
            entry->document_state->document()->set_salvageable(false);

            // FIXME: 3. If navigationParams is not null, then:
            if (navigation_params.has_value()) {
                TODO();
            }
        }
        // FIXME: 9. Otherwise, if navigationParams's response's status is 204 or 205, then:
        else if (navigation_params->response->status() == 204 || navigation_params->response->status() == 205) {
            // 1. Run completionSteps.
            completion_steps();

            // 2. Return.
            return;
        }
        // FIXME: 10. Otherwise, if navigationParams's response has a `Content-Disposition`
        //            header specifying the attachment disposition type, then:
        // 11. Otherwise:
        else {
            // 1. Let document be the result of loading a document given navigationParams, sourceSnapshotParams,
            //    and entry's document state's initiator origin.
            auto document = load_document(navigation_params);

            // 2. If document is null, then run completionSteps and return.
            if (!document) {
                VERIFY_NOT_REACHED();

                completion_steps();
                return;
            }

            // 3. Set entry's document state's document to document.
            entry->document_state->set_document(document.ptr());

            // 4. Set entry's document state's origin to document's origin.
            entry->document_state->set_origin(document->origin());
        }

        // FIXME: 12. If entry's document state's request referrer is "client", then set it to request's referrer.

        // 13. If entry's document state's document is not null, then set entry's document state's ever populated to true.
        if (entry->document_state->document()) {
            entry->document_state->set_ever_populated(true);
        }

        // 14. Run completionSteps.
        completion_steps();
    });

    return {};
}

// To navigate a navigable navigable to a URL url using a Document sourceDocument,
// with an optional POST resource, string, or null documentResource (default null),
// an optional response-or-null response (default null), an optional boolean exceptionsEnabled (default false),
// an optional NavigationHistoryBehavior historyHandling (default "auto"),
// an optional serialized state-or-null navigationAPIState (default null),
// an optional entry list or null formDataEntryList (default null),
// an optional referrer policy referrerPolicy (default the empty string),
// and an optional user navigation involvement userInvolvement (default "none"):

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate
WebIDL::ExceptionOr<void> Navigable::navigate(
    AK::URL const& url,
    JS::NonnullGCPtr<DOM::Document> source_document,
    Variant<Empty, String, POSTResource> document_resource,
    JS::GCPtr<Fetch::Infrastructure::Response> response,
    bool exceptions_enabled,
    Bindings::NavigationHistoryBehavior history_handling,
    Optional<SerializationRecord> navigation_api_state,
    Optional<Vector<XHR::FormDataEntry>&> form_data_entry_list,
    ReferrerPolicy::ReferrerPolicy referrer_policy,
    UserNaviagationInvolvement user_involvement)
{
    auto& active_document = *this->active_document();
    auto& realm = active_document.realm();
    auto& vm = this->vm();

    // 1. Let cspNavigationType be "form-submission" if formDataEntryList is non-null; otherwise "other".
    auto csp_navigation_type = form_data_entry_list.has_value() ? CSPNavigationType::FormSubmission : CSPNavigationType::Other;

    // 2. Let sourceSnapshotParams be the result of snapshotting source snapshot params given sourceDocument.
    auto source_snapshot_params = source_document->snapshot_source_snapshot_params();

    // 3. Let initiatorOriginSnapshot be sourceDocument's origin.
    auto initiator_origin_snapshot = source_document->origin();

    // 4. Let initiatorBaseURLSnapshot be sourceDocument's document base URL.
    auto initiator_base_url_snapshot = source_document->base_url();

    // 5. If sourceDocument's node navigable is not allowed by sandboxing to navigate navigable given and sourceSnapshotParams, then:
    if (!source_document->navigable()->allowed_by_sandboxing_to_navigate(*this, source_snapshot_params)) {
        // 1. If exceptionsEnabled is true, then throw a "SecurityError" DOMException.
        if (exceptions_enabled) {
            return WebIDL::SecurityError::create(realm, "Source document's node navigable is not allowed to navigate"_fly_string);
        }

        // 2 Return.
        return {};
    }

    // 6. Let navigationId be the result of generating a random UUID.
    String navigation_id = TRY_OR_THROW_OOM(vm, Crypto::generate_random_uuid());

    // FIXME: 7. If the surrounding agent is equal to navigable's active document's relevant agent, then continue these steps.
    //           Otherwise, queue a global task on the navigation and traversal task source given navigable's active window to continue these steps.

    // 8. If navigable's active document's unload counter is greater than 0,
    //    then invoke WebDriver BiDi navigation failed with a WebDriver BiDi navigation status whose id is navigationId,
    //    status is "canceled", and url is url, and return.
    if (active_document.unload_counter() > 0) {
        // FIXME: invoke WebDriver BiDi navigation failed with a WebDriver BiDi navigation status whose id is navigationId,
        //        status is "canceled", and url is url
        return {};
    }

    // 9. If historyHandling is "auto", then:
    if (history_handling == Bindings::NavigationHistoryBehavior::Auto) {
        // FIXME: Fix spec typo targetNavigable --> navigable
        // 1. If url equals navigable's active document's URL,
        //     and initiatorOriginSnapshot is same origin with targetNavigable's active document's origin,
        //     then set historyHandling to "replace".
        if (url == active_document.url() && initiator_origin_snapshot.is_same_origin(active_document.origin()))
            history_handling = Bindings::NavigationHistoryBehavior::Replace;

        // 2. Otherwise, set historyHandling to "push".
        else
            history_handling = Bindings::NavigationHistoryBehavior::Push;
    }

    // 10. If the navigation must be a replace given url and navigable's active document, then set historyHandling to "replace".
    if (navigation_must_be_a_replace(url, active_document))
        history_handling = Bindings::NavigationHistoryBehavior::Replace;

    // 11. If all of the following are true:
    //    - documentResource is null;
    //    - response is null;
    //    - url equals navigable's active session history entry's URL with exclude fragments set to true; and
    //    - url's fragment is non-null
    if (document_resource.has<Empty>()
        && !response
        && url.equals(active_session_history_entry()->url, AK::URL::ExcludeFragment::Yes)
        && url.fragment().has_value()) {
        // 1. Navigate to a fragment given navigable, url, historyHandling, and navigationId.
        TRY(navigate_to_a_fragment(url, to_history_handling_behavior(history_handling), navigation_id));

        // 2. Return.
        return {};
    }

    // 12. If navigable's parent is non-null, then set navigable's is delaying load events to true.
    if (parent() != nullptr)
        set_delaying_load_events(true);

    // 13. Let targetBrowsingContext be navigable's active browsing context.
    [[maybe_unused]] auto target_browsing_context = active_browsing_context();

    // 14. Let targetSnapshotParams be the result of snapshotting target snapshot params given navigable.
    [[maybe_unused]] auto target_snapshot_params = snapshot_target_snapshot_params();

    // 15. Invoke WebDriver BiDi navigation started with targetBrowsingContext, and a new WebDriver BiDi navigation status whose id is navigationId, url is url, and status is "pending".

    // 16. If navigable's ongoing navigation is "traversal", then:
    if (ongoing_navigation().has<Traversal>()) {
        // FIXME: 1. Invoke WebDriver BiDi navigation failed with targetBrowsingContext and a new WebDriver BiDi navigation status whose id is navigationId, status is "canceled", and url is url.

        // 2. Return.
        return {};
    }

    // 17. Set navigable's ongoing navigation to navigationId.
    m_ongoing_navigation = navigation_id;

    // 18. If url's scheme is "javascript", then:
    if (url.scheme() == "javascript"sv) {
        // 1. Queue a global task on the navigation and traversal task source given navigable's active window to navigate to a javascript: URL given navigable, url, historyHandling, initiatorOriginSnapshot, and cspNavigationType.
        queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [this, url, history_handling, initiator_origin_snapshot, csp_navigation_type] {
            (void)navigate_to_a_javascript_url(url, to_history_handling_behavior(history_handling), initiator_origin_snapshot, csp_navigation_type);
        });

        // 2. Return.
        return {};
    }

    // 19. If all of the following are true:
    //     - userInvolvement is not "browser UI";
    //     - navigable's active document's origin is same origin-domain with sourceDocument's origin;
    //     - navigable's active document's is initial about:blank is false; and
    //     - url's scheme is a fetch scheme
    //     then:
    if (user_involvement != UserNaviagationInvolvement::BrowserUI && active_document.origin().is_same_origin_domain(source_document->origin()) && !active_document.is_initial_about_blank() && Fetch::Infrastructure::is_fetch_scheme(url.scheme())) {
        // 1. Let navigation be navigable's active window's navigation API.
        auto navigation = active_window()->navigation();

        // 2. Let entryListForFiring be formDataEntryList if documentResource is a POST resource; otherwise, null.
        auto entry_list_for_firing = [&]() -> Optional<Vector<XHR::FormDataEntry>&> {
            if (document_resource.has<POSTResource>())
                return form_data_entry_list;
            return {};
        }();

        // 3. Let navigationAPIStateForFiring be navigationAPIState if navigationAPIState is not null;
        //    otherwise, StructuredSerializeForStorage(undefined).
        auto navigation_api_state_for_firing = navigation_api_state.value_or(MUST(structured_serialize_for_storage(vm, JS::js_undefined())));

        // FIXME: 4. Let continue be the result of firing a push/replace/reload navigate event at navigation
        //           with navigationType set to historyHandling, isSameDocument set to false, userInvolvement set to userInvolvement,
        //           formDataEntryList set to entryListForFiring, destinationURL set to url, and navigationAPIState set to navigationAPIStateForFiring.
        (void)navigation;
        (void)entry_list_for_firing;
        (void)navigation_api_state_for_firing;

        // FIXME: 5. If continue is false, then return.
    }

    // 20. In parallel, run these steps:
    Platform::EventLoopPlugin::the().deferred_invoke([this, source_snapshot_params = move(source_snapshot_params), document_resource, url, navigation_id, referrer_policy, initiator_origin_snapshot, response, history_handling, initiator_base_url_snapshot] {
        // FIXME: 1. Let unloadPromptCanceled be the result of checking if unloading is user-canceled for navigable's active document's inclusive descendant navigables.

        // FIXME: 2. If unloadPromptCanceled is true, or navigable's ongoing navigation is no longer navigationId, then:
        if (!ongoing_navigation().has<String>() || ongoing_navigation().get<String>() != navigation_id) {
            // FIXME: 1. Invoke WebDriver BiDi navigation failed with targetBrowsingContext and a new WebDriver BiDi navigation status whose id is navigationId, status is "canceled", and url is url.

            // 2. Abort these steps.
            return;
        }

        // 3. Queue a global task on the navigation and traversal task source given navigable's active window to abort navigable's active document.
        queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [this] {
            VERIFY(this->active_document());
            this->active_document()->abort();
        });

        // 4. Let documentState be a new document state with
        //    request referrer policy: referrerPolicy
        //    initiator origin: initiatorOriginSnapshot
        //    resource: documentResource
        //    navigable target name: navigable's target name
        JS::NonnullGCPtr<DocumentState> document_state = *heap().allocate_without_realm<DocumentState>();
        document_state->set_request_referrer_policy(referrer_policy);
        document_state->set_initiator_origin(initiator_origin_snapshot);
        document_state->set_resource(document_resource);
        document_state->set_navigable_target_name(target_name());

        // 5. If url matches about:blank or is about:srcdoc, then set documentState's origin to documentState's initiator origin.
        //   FIXME: should this say "matches about:srcdoc"
        if (url_matches_about_blank(url) || url == "about:srcdoc"sv) {
            // 1. Set documentState's origin to initiatorOriginSnapshot.
            document_state->set_origin(document_state->initiator_origin());

            // 2. Set documentState's about base URL to initiatorBaseURLSnapshot.
            document_state->set_about_base_url(initiator_base_url_snapshot);
        }

        // 6. Let historyEntry be a new session history entry, with its URL set to url and its document state set to documentState.
        JS::NonnullGCPtr<SessionHistoryEntry> history_entry = *heap().allocate_without_realm<SessionHistoryEntry>();
        history_entry->url = url;
        history_entry->document_state = document_state;

        // 8. Let navigationParams be null.
        Optional<NavigationParams> navigation_params;

        // FIXME: 9. If response is non-null:
        if (response) {
        }

        // 10. Attempt to populate the history entry's document
        //     for historyEntry, given navigable, "navigate", sourceSnapshotParams,
        //     targetSnapshotParams, navigationId, navigationParams, cspNavigationType, with allowPOST
        //     set to true and completionSteps set to the following step:
        populate_session_history_entry_document(history_entry, navigation_params, navigation_id, source_snapshot_params, true, [this, history_entry, history_handling, navigation_id] {
            traversable_navigable()->append_session_history_traversal_steps([this, history_entry, history_handling] {
                finalize_a_cross_document_navigation(*this, to_history_handling_behavior(history_handling), history_entry);
            });
        }).release_value_but_fixme_should_propagate_errors();
    });

    return {};
}

WebIDL::ExceptionOr<void> Navigable::navigate_to_a_fragment(AK::URL const& url, HistoryHandlingBehavior history_handling, String navigation_id)
{
    (void)navigation_id;

    // FIXME: 1. Let navigation be navigable's active window's navigation API.
    // FIXME: 2. Let destinationNavigationAPIState be navigable's active session history entry's navigation API state.
    // FIXME: 3. If navigationAPIState is not null, then set destinationNavigationAPIState to navigationAPIState.
    // FIXME: 4. Let continue be the result of firing a push/replace/reload navigate event at navigation with navigationType set to historyHandling, isSameDocument set to true,
    //           userInvolvement set to userInvolvement, and destinationURL set to url, and navigationAPIState set to destinationNavigationAPIState.
    // FIXME: 5. If continue is false, then return.

    // 6. Let historyEntry be a new session history entry, with
    //      URL: url
    //      document state: navigable's active session history entry's document state
    //      navigation API state: destinationNavigationAPIState
    //      scroll restoration mode: navigable's active session history entry's scroll restoration mode
    JS::NonnullGCPtr<SessionHistoryEntry> history_entry = heap().allocate_without_realm<SessionHistoryEntry>();
    history_entry->url = url;
    history_entry->document_state = active_session_history_entry()->document_state;
    history_entry->scroll_restoration_mode = active_session_history_entry()->scroll_restoration_mode;

    // 7. Let entryToReplace be navigable's active session history entry if historyHandling is "replace", otherwise null.
    auto entry_to_replace = history_handling == HistoryHandlingBehavior::Replace ? active_session_history_entry() : nullptr;

    // FIXME: 8. Let history be navigable's active document's history object.

    // FIXME: 9. Let scriptHistoryIndex be history's index.

    // FIXME: 10. Let scriptHistoryIndex be history's index.

    // 11. If historyHandling is "push", then:
    if (history_handling == HistoryHandlingBehavior::Push) {
        // FIXME: 1. Set history's state to null.
        // FIXME: 2. Increment scriptHistoryIndex.
        // FIXME: 3. Set scriptHistoryLength to scriptHistoryIndex + 1.
    }

    // 12. Set navigable's active session history entry to historyEntry.
    m_active_session_history_entry = history_entry;

    // FIXME: 13. Update document for history step application given navigable's active document, historyEntry, true, scriptHistoryIndex, and scriptHistoryLength.

    // FIXME: 14. Update the navigation API entries for a same-document navigation given navigation, historyEntry, and historyHandling.

    // 15. Scroll to the fragment given navigable's active document.
    // FIXME: Specification doesn't say when document url needs to update during fragment navigation
    active_document()->set_url(url);
    active_document()->scroll_to_the_fragment();

    // 16. Let traversable be navigable's traversable navigable.
    auto traversable = traversable_navigable();

    // 17. Append the following session history synchronous navigation steps involving navigable to traversable:
    traversable->append_session_history_traversal_steps([&] {
        // 1. Finalize a same-document navigation given traversable, navigable, historyEntry, and entryToReplace.
        finalize_a_same_document_navigation(*traversable, *this, history_entry, entry_to_replace);

        // FIXME: 2. Invoke WebDriver BiDi fragment navigated with navigable's active browsing context and a new WebDriver BiDi
        //            navigation status whose id is navigationId, url is url, and status is "complete".
    });

    return {};
}

WebIDL::ExceptionOr<void> Navigable::navigate_to_a_javascript_url(AK::URL const&, HistoryHandlingBehavior, Origin const& initiator_origin, CSPNavigationType csp_navigation_type)
{
    (void)initiator_origin;
    (void)csp_navigation_type;
    TODO();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#reload
void Navigable::reload()
{
    // 1. Set navigable's active session history entry's document state's reload pending to true.
    active_session_history_entry()->document_state->set_reload_pending(true);

    // 2. Let traversable be navigable's traversable navigable.
    auto traversable = traversable_navigable();

    // 3. Append the following session history traversal steps to traversable:
    traversable->append_session_history_traversal_steps([traversable] {
        // 1. Apply pending history changes to traversable with true.
        traversable->apply_pending_history_changes();
    });
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#the-navigation-must-be-a-replace
bool navigation_must_be_a_replace(AK::URL const& url, DOM::Document const& document)
{
    return url.scheme() == "javascript"sv || document.is_initial_about_blank();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#allowed-to-navigate
bool Navigable::allowed_by_sandboxing_to_navigate(Navigable const& target, SourceSnapshotParams const& source_snapshot_params)
{
    auto& source = *this;

    auto is_ancestor_of = [](Navigable const& a, Navigable const& b) {
        for (auto parent = b.parent(); parent; parent = parent->parent()) {
            if (parent.ptr() == &a)
                return true;
        }
        return false;
    };

    // A navigable source is allowed by sandboxing to navigate a second navigable target,
    // given a source snapshot params sourceSnapshotParams, if the following steps return true:

    // 1. If source is target, then return true.
    if (&source == &target)
        return true;

    // 2. If source is an ancestor of target, then return true.
    if (is_ancestor_of(source, target))
        return true;

    // 3. If target is an ancestor of source, then:
    if (is_ancestor_of(target, source)) {

        // 1. If target is not a top-level traversable, then return true.
        if (!target.is_top_level_traversable())
            return true;

        // 2. If sourceSnapshotParams's has transient activation is true, and sourceSnapshotParams's sandboxing flags's
        //    sandboxed top-level navigation with user activation browsing context flag is set, then return false.
        if (source_snapshot_params.has_transient_activation && has_flag(source_snapshot_params.sandboxing_flags, SandboxingFlagSet::SandboxedTopLevelNavigationWithUserActivation))
            return false;

        // 3. If sourceSnapshotParams's has transient activation is false, and sourceSnapshotParams's sandboxing flags's
        //    sandboxed top-level navigation without user activation browsing context flag is set, then return false.
        if (!source_snapshot_params.has_transient_activation && has_flag(source_snapshot_params.sandboxing_flags, SandboxingFlagSet::SandboxedTopLevelNavigationWithoutUserActivation))
            return false;

        // 4. Return true.
        return true;
    }

    // 4. If target is a top-level traversable:
    if (target.is_top_level_traversable()) {
        // FIXME: 1. If source is the one permitted sandboxed navigator of target, then return true.

        // 2. If sourceSnapshotParams's sandboxing flags's sandboxed navigation browsing context flag is set, then return false.
        if (has_flag(source_snapshot_params.sandboxing_flags, SandboxingFlagSet::SandboxedNavigation))
            return false;

        // 3. Return true.
        return true;
    }

    // 5. If sourceSnapshotParams's sandboxing flags's sandboxed navigation browsing context flag is set, then return false.
    // 6. Return true.
    return !has_flag(source_snapshot_params.sandboxing_flags, SandboxingFlagSet::SandboxedNavigation);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#snapshotting-target-snapshot-params
TargetSnapshotParams Navigable::snapshot_target_snapshot_params()
{
    // To snapshot target snapshot params given a navigable targetNavigable, return a new target snapshot params
    // with sandboxing flags set to the result of determining the creation sandboxing flags given targetNavigable's
    // active browsing context and targetNavigable's container.

    return { determine_the_creation_sandboxing_flags(*active_browsing_context(), container()) };
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#finalize-a-cross-document-navigation
void finalize_a_cross_document_navigation(JS::NonnullGCPtr<Navigable> navigable, HistoryHandlingBehavior history_handling, JS::NonnullGCPtr<SessionHistoryEntry> history_entry)
{
    // 1. FIXME: Assert: this is running on navigable's traversable navigable's session history traversal queue.

    // 2. Set navigable's is delaying load events to false.
    navigable->set_delaying_load_events(false);

    // 3. If historyEntry's document is null, then return.
    if (!history_entry->document_state->document())
        return;

    // 4. If all of the following are true:
    //    - navigable's parent is null;
    //    - historyEntry's document's browsing context is not an auxiliary browsing context whose opener browsing context is non-null; and
    //    - historyEntry's document's origin is not navigable's active document's origin
    //    then set historyEntry's document state's navigable target name to the empty string.
    if (navigable->parent() == nullptr && history_entry->document_state->document()->browsing_context()->opener_browsing_context() != nullptr && history_entry->document_state->document()->origin() != navigable->active_document()->origin())
        history_entry->document_state->set_navigable_target_name(String {});

    // 5. Let entryToReplace be navigable's active session history entry if historyHandling is "replace", otherwise null.
    auto entry_to_replace = history_handling == HistoryHandlingBehavior::Replace ? navigable->active_session_history_entry() : nullptr;

    // 6. Let traversable be navigable's traversable navigable.
    auto traversable = navigable->traversable_navigable();

    // 7. Let targetStep be null.
    int target_step;

    // 8. Let targetEntries be the result of getting session history entries for navigable.
    auto& target_entries = navigable->get_session_history_entries();

    // 9. If entryToReplace is null, then:
    if (entry_to_replace == nullptr) {
        // 1. Clear the forward session history of traversable.
        traversable->clear_the_forward_session_history();

        // 2. Set targetStep to traversable's current session history step + 1.
        target_step = traversable->current_session_history_step() + 1;

        // 3. Set historyEntry's step to targetStep.
        history_entry->step = target_step;

        // 4. Append historyEntry to targetEntries.
        target_entries.append(history_entry);
    } else {
        // 1. Replace entryToReplace with historyEntry in targetEntries.
        *(target_entries.find(*entry_to_replace)) = history_entry;

        // 2. Set historyEntry's step to entryToReplace's step.
        history_entry->step = entry_to_replace->step;

        // 3. If historyEntry's document state's origin is same origin with entryToReplace's document state's origin,
        //    then set historyEntry's navigation API key to entryToReplace's navigation API key.
        if (history_entry->document_state->origin().has_value() && entry_to_replace->document_state->origin().has_value() && history_entry->document_state->origin()->is_same_origin(*entry_to_replace->document_state->origin())) {
            history_entry->navigation_api_key = entry_to_replace->navigation_api_key;
        }

        // 4. Set targetStep to traversable's current session history step.
        target_step = traversable->current_session_history_step();
    }

    // FIXME: 10. Apply the push/replace history step targetStep to traversable.
    traversable->apply_the_history_step(target_step);
}

}
