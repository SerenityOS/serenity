/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/MediaQueryListEvent.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/CustomEvent.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/DOMImplementation.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/NodeIterator.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/TreeWalker.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLBaseElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/SVG/TagNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/FocusEvent.h>
#include <LibWeb/UIEvents/KeyboardEvent.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::DOM {

// https://html.spec.whatwg.org/multipage/origin.html#obtain-browsing-context-navigation
static NonnullRefPtr<HTML::BrowsingContext> obtain_a_browsing_context_to_use_for_a_navigation_response(
    HTML::BrowsingContext& browsing_context,
    HTML::SandboxingFlagSet sandbox_flags,
    HTML::CrossOriginOpenerPolicy navigation_coop,
    HTML::CrossOriginOpenerPolicyEnforcementResult coop_enforcement_result)
{
    // 1. If browsingContext is not a top-level browsing context, return browsingContext.
    if (!browsing_context.is_top_level())
        return browsing_context;

    // 2. If coopEnforcementResult's needs a browsing context group switch is false, then:
    if (!coop_enforcement_result.needs_a_browsing_context_group_switch) {
        // 1. If coopEnforcementResult's would need a browsing context group switch due to report-only is true,
        if (coop_enforcement_result.would_need_a_browsing_context_group_switch_due_to_report_only) {
            // FIXME: set browsing context's virtual browsing context group ID to a new unique identifier.
        }
        // 2. Return browsingContext.
        return browsing_context;
    }

    // 3. Let newBrowsingContext be the result of creating a new top-level browsing context.
    VERIFY(browsing_context.page());
    auto new_browsing_context = HTML::BrowsingContext::create_a_new_browsing_context(*browsing_context.page(), nullptr, nullptr);

    // FIXME: 4. If navigationCOOP's value is "same-origin-plus-COEP", then set newBrowsingContext's group's
    //           cross-origin isolation mode to either "logical" or "concrete". The choice of which is implementation-defined.

    // 5. If sandboxFlags is not empty, then:
    if (!sandbox_flags.is_empty()) {
        // 1. Assert navigationCOOP's value is "unsafe-none".
        VERIFY(navigation_coop.value == HTML::CrossOriginOpenerPolicyValue::UnsafeNone);

        // 2. Assert: newBrowsingContext's popup sandboxing flag set is empty.

        // 3. Set newBrowsingContext's popup sandboxing flag set to a clone of sandboxFlags.
    }

    // FIXME: 6. Discard browsingContext.

    // 7. Return newBrowsingContext.
    return new_browsing_context;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#initialise-the-document-object
NonnullRefPtr<Document> Document::create_and_initialize(Type type, String content_type, HTML::NavigationParams navigation_params)
{
    // 1. Let browsingContext be the result of the obtaining a browsing context to use for a navigation response
    //    given navigationParams's browsing context, navigationParams's final sandboxing flag set,
    //    navigationParams's cross-origin opener policy, and navigationParams's COOP enforcement result.
    auto browsing_context = obtain_a_browsing_context_to_use_for_a_navigation_response(
        navigation_params.browsing_context,
        navigation_params.final_sandboxing_flag_set,
        navigation_params.cross_origin_opener_policy,
        navigation_params.coop_enforcement_result);

    // FIXME: 2. Let permissionsPolicy be the result of creating a permissions policy from a response
    //           given browsingContext, navigationParams's origin, and navigationParams's response.

    // 3. Let creationURL be navigationParams's response's URL.
    auto creation_url = navigation_params.response->url();

    // 4. If navigationParams's request is non-null, then set creationURL to navigationParams's request's current URL.
    if (navigation_params.request) {
        creation_url = navigation_params.request->current_url();
    }

    RefPtr<HTML::Window> window;

    // 5. If browsingContext is still on its initial about:blank Document,
    //    and navigationParams's history handling is "replace",
    //    and browsingContext's active document's origin is same origin-domain with navigationParams's origin,
    //    then do nothing.
    if (browsing_context->still_on_its_initial_about_blank_document()
        && navigation_params.history_handling == HTML::HistoryHandlingBehavior::Replace
        && (browsing_context->active_document() && browsing_context->active_document()->origin().is_same_origin(navigation_params.origin))) {
        // Do nothing.
    }

    // 6. Otherwise:
    else {
        // FIXME: 1. Let oacHeader be the result of getting a structured field value given `Origin-Agent-Cluster` and "item" from response's header list.

        // FIXME: 2. Let requestsOAC be true if oacHeader is not null and oacHeader[0] is the boolean true; otherwise false.
        [[maybe_unused]] auto requests_oac = false;

        // FIXME: 3. If navigationParams's reserved environment is a non-secure context, then set requestsOAC to false.

        // FIXME: 4. Let agent be the result of obtaining a similar-origin window agent given navigationParams's origin, browsingContext's group, and requestsOAC.

        // 5. Let realm execution context be the result of creating a new JavaScript realm given agent and the following customizations:
        auto realm_execution_context = Bindings::create_a_new_javascript_realm(
            Bindings::main_thread_vm(),
            [&](JS::Realm& realm) -> JS::GlobalObject* {
                // - For the global object, create a new Window object.
                window = HTML::Window::create();
                auto* global_object = realm.heap().allocate_without_realm<Bindings::WindowObject>(realm, *window);
                VERIFY(window->wrapper() == global_object);
                return global_object;
            },
            [](JS::Realm&) -> JS::GlobalObject* {
                // FIXME: - For the global this binding, use browsingContext's WindowProxy object.
                return nullptr;
            });

        // 6. Let topLevelCreationURL be creationURL.
        auto top_level_creation_url = creation_url;

        // 7. Let topLevelOrigin be navigationParams's origin.
        auto top_level_origin = navigation_params.origin;

        // 8. If browsingContext is not a top-level browsing context, then:
        if (!browsing_context->is_top_level()) {
            // 1. Let parentEnvironment be browsingContext's container's relevant settings object.
            auto& parent_environment = browsing_context->container()->document().relevant_settings_object();

            // 2. Set topLevelCreationURL to parentEnvironment's top-level creation URL.
            top_level_creation_url = parent_environment.top_level_creation_url;

            // 3. Set topLevelOrigin to parentEnvironment's top-level origin.
            top_level_origin = parent_environment.top_level_origin;
        }

        // 9. Set up a window environment settings object with creationURL, realm execution context,
        //    navigationParams's reserved environment, topLevelCreationURL, and topLevelOrigin.

        // FIXME: Why do we assume `creation_url` is non-empty here? Is this a spec bug?
        // FIXME: Why do we assume `top_level_creation_url` is non-empty here? Is this a spec bug?
        HTML::WindowEnvironmentSettingsObject::setup(
            creation_url.value(),
            move(realm_execution_context),
            navigation_params.reserved_environment,
            top_level_creation_url.value(),
            top_level_origin);
    }

    // FIXME: 7. Let loadTimingInfo be a new document load timing info with its navigation start time set to response's timing info's start time.

    // 8. Let document be a new Document,
    //    whose type is type,
    //    content type is contentType,
    //    origin is navigationParams's origin,
    //    FIXME: policy container is navigationParams's policy container,
    //    FIXME: permissions policy is permissionsPolicy,
    //    FIXME: active sandboxing flag set is navigationParams's final sandboxing flag set,
    //    FIXME: and cross-origin opener policy is navigationParams's cross-origin opener policy,
    //    FIXME: load timing info is loadTimingInfo,
    //    FIXME: and navigation id is navigationParams's id.
    auto document = Document::create();
    document->m_type = type;
    document->m_content_type = content_type;
    document->set_origin(navigation_params.origin);

    document->m_window = window;
    window->set_associated_document(*document);

    // 9. Set document's URL to creationURL.
    document->m_url = creation_url.value();

    // 10. Set document's current document readiness to "loading".
    document->m_readiness = HTML::DocumentReadyState::Loading;

    // FIXME: 11. Run CSP initialization for a Document given document.

    // 12. If navigationParams's request is non-null, then:
    if (navigation_params.request) {
        // FIXME: 1. Set document's referrer to the empty string.
        // FIXME: 2. Let referrer be navigationParams's request's referrer.
        // FIXME: 3. If referrer is a URL record, then set document's referrer to the serialization of referrer.
    }

    // FIXME: 13. Let historyHandling be navigationParams's history handling.

    // FIXME: 14: Let navigationTimingType be the result of switching on navigationParams's history handling...

    // FIXME: 15. Let redirectCount be 0 if navigationParams's has cross-origin redirects is true;
    //            otherwise navigationParams's request's redirect count.

    // FIXME: 16. Create the navigation timing entry for document, with navigationParams's response's timing info,
    //            redirectCount, navigationTimingType, and navigationParams's response's service worker timing info.

    // FIXME: 17. If navigationParams's response has a `Refresh` header, then...

    // FIXME: 18. If navigationParams's commit early hints is not null, then call navigationParams's commit early hints with document.

    // FIXME: 19. Process link headers given document, navigationParams's response, and "pre-media".

    // 20. Return document.
    return document;
}

NonnullRefPtr<Document> Document::create_with_global_object(Bindings::WindowObject&)
{
    return Document::create();
}

NonnullRefPtr<Document> Document::create(AK::URL const& url)
{
    return adopt_ref(*new Document(url));
}

Document::Document(const AK::URL& url)
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_computer(make<CSS::StyleComputer>(*this))
    , m_url(url)
    , m_window(HTML::Window::create_with_document(*this))
    , m_history(HTML::History::create(*this))
{
    m_style_sheets = JS::make_handle(CSS::StyleSheetList::create(*this));

    HTML::main_thread_event_loop().register_document({}, *this);

    m_style_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_style();
    });

    m_layout_update_timer = Core::Timer::create_single_shot(0, [this] {
        force_layout();
    });
}

Document::~Document() = default;

void Document::removed_last_ref()
{
    VERIFY(!ref_count());
    VERIFY(!m_deletion_has_begun);

    if (m_referencing_node_count) {
        // The document has reached ref_count==0 but still has nodes keeping it alive.
        // At this point, sever all the node links we control.
        // If nodes remain elsewhere (e.g JS wrappers), they will keep the document alive.

        // NOTE: This makes sure we stay alive across for the duration of the cleanup below.
        increment_referencing_node_count();

        m_focused_element = nullptr;
        m_hovered_node = nullptr;
        m_pending_parsing_blocking_script = nullptr;
        m_inspected_node = nullptr;
        m_scripts_to_execute_when_parsing_has_finished.clear();
        m_scripts_to_execute_as_soon_as_possible.clear();
        m_associated_inert_template_document = nullptr;

        m_interpreter = nullptr;

        {
            // Gather up all the descendants of this document and prune them from the tree.
            // FIXME: This could definitely be more elegant.
            NonnullRefPtrVector<Node> descendants;
            for_each_in_inclusive_subtree([&](auto& node) {
                if (&node != this)
                    descendants.append(node);
                return IterationDecision::Continue;
            });

            for (auto& node : descendants) {
                VERIFY(&node.document() == this);
                VERIFY(!node.is_document());
                if (node.parent()) {
                    // We need to suppress mutation observers so that they don't try and queue a microtask for this Document which is in the process of dying,
                    // which will cause an `!m_in_removed_last_ref` assertion failure when it tries to ref this Document.
                    node.remove(true);
                }
            }
        }

        m_in_removed_last_ref = false;
        decrement_referencing_node_count();
        return;
    }

    m_in_removed_last_ref = false;
    m_deletion_has_begun = true;

    HTML::main_thread_event_loop().unregister_document({}, *this);

    delete this;
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-write
ExceptionOr<void> Document::write(Vector<String> const& strings)
{
    StringBuilder builder;
    builder.join(""sv, strings);

    return run_the_document_write_steps(builder.build());
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-writeln
ExceptionOr<void> Document::writeln(Vector<String> const& strings)
{
    StringBuilder builder;
    builder.join(""sv, strings);
    builder.append("\n"sv);

    return run_the_document_write_steps(builder.build());
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#document-write-steps
ExceptionOr<void> Document::run_the_document_write_steps(String input)
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException.
    if (m_type == Type::XML)
        return DOM::InvalidStateError::create("write() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return DOM::InvalidStateError::create("throw-on-dynamic-markup-insertion-counter greater than zero.");

    // 3. If document's active parser was aborted is true, then return.
    if (m_active_parser_was_aborted)
        return {};

    // 4. If the insertion point is undefined, then:
    if (!(m_parser && m_parser->tokenizer().is_insertion_point_defined())) {
        // 1. If document's unload counter is greater than 0 or document's ignore-destructive-writes counter is greater than 0, then return.
        if (m_unload_counter > 0 || m_ignore_destructive_writes_counter > 0)
            return {};

        // 2. Run the document open steps with document.
        open();
    }

    // 5. Insert input into the input stream just before the insertion point.
    m_parser->tokenizer().insert_input_at_insertion_point(input);

    // 6. If there is no pending parsing-blocking script, have the HTML parser process input, one code point at a time, processing resulting tokens as they are emitted, and stopping when the tokenizer reaches the insertion point or when the processing of the tokenizer is aborted by the tree construction stage (this can happen if a script end tag token is emitted by the tokenizer).
    if (!pending_parsing_blocking_script())
        m_parser->run();

    return {};
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-open
ExceptionOr<Document*> Document::open(String const&, String const&)
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException exception.
    if (m_type == Type::XML)
        return DOM::InvalidStateError::create("open() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return DOM::InvalidStateError::create("throw-on-dynamic-markup-insertion-counter greater than zero.");

    // FIXME: 3. Let entryDocument be the entry global object's associated Document.
    auto& entry_document = *this;

    // 4. If document's origin is not same origin to entryDocument's origin, then throw a "SecurityError" DOMException.
    if (origin() != entry_document.origin())
        return DOM::SecurityError::create("Document.origin() not the same as entryDocument's.");

    // 5. If document has an active parser whose script nesting level is greater than 0, then return document.
    if (m_parser && m_parser->script_nesting_level() > 0)
        return this;

    // 6. Similarly, if document's unload counter is greater than 0, then return document.
    if (m_unload_counter > 0)
        return this;

    // 7. If document's active parser was aborted is true, then return document.
    if (m_active_parser_was_aborted)
        return this;

    // FIXME: 8. If document's browsing context is non-null and there is an existing attempt to navigate document's browsing context, then stop document loading given document.

    // FIXME: 9. For each shadow-including inclusive descendant node of document, erase all event listeners and handlers given node.

    // FIXME 10. If document is the associated Document of document's relevant global object, then erase all event listeners and handlers given document's relevant global object.

    // 11. Replace all with null within document, without firing any mutation events.
    replace_all(nullptr);

    // 12. If document is fully active, then:
    if (is_fully_active()) {
        // 1. Let newURL be a copy of entryDocument's URL.
        auto new_url = entry_document.url();
        // 2. If entryDocument is not document, then set newURL's fragment to null.
        if (&entry_document != this)
            new_url.set_fragment("");

        // FIXME: 3. Run the URL and history update steps with document and newURL.
    }

    // 13. Set document's is initial about:blank to false.
    set_is_initial_about_blank(false);

    // FIXME: 14. If document's iframe load in progress flag is set, then set document's mute iframe load flag.

    // 15. Set document to no-quirks mode.
    set_quirks_mode(QuirksMode::No);

    // 16. Create a new HTML parser and associate it with document. This is a script-created parser (meaning that it can be closed by the document.open() and document.close() methods, and that the tokenizer will wait for an explicit call to document.close() before emitting an end-of-file token). The encoding confidence is irrelevant.
    m_parser = HTML::HTMLParser::create_for_scripting(*this);

    // 17. Set the insertion point to point at just before the end of the input stream (which at this point will be empty).
    m_parser->tokenizer().update_insertion_point();

    // 18. Update the current document readiness of document to "loading".
    update_readiness(HTML::DocumentReadyState::Loading);

    // 19. Return document.
    return this;
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#closing-the-input-stream
ExceptionOr<void> Document::close()
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException exception.
    if (m_type == Type::XML)
        return DOM::InvalidStateError::create("close() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return DOM::InvalidStateError::create("throw-on-dynamic-markup-insertion-counter greater than zero.");

    // 3. If there is no script-created parser associated with the document, then return.
    if (!m_parser)
        return {};

    // FIXME: 4. Insert an explicit "EOF" character at the end of the parser's input stream.
    m_parser->tokenizer().insert_eof();

    // 5. If there is a pending parsing-blocking script, then return.
    if (pending_parsing_blocking_script())
        return {};

    // FIXME: 6. Run the tokenizer, processing resulting tokens as they are emitted, and stopping when the tokenizer reaches the explicit "EOF" character or spins the event loop.
    m_parser->run();

    return {};
}

HTML::Origin Document::origin() const
{
    return m_origin;
}

void Document::set_origin(HTML::Origin const& origin)
{
    m_origin = origin;
}

void Document::schedule_style_update()
{
    if (m_style_update_timer->is_active())
        return;
    m_style_update_timer->start();
}

void Document::schedule_layout_update()
{
    if (m_layout_update_timer->is_active())
        return;
    m_layout_update_timer->start();
}

bool Document::is_child_allowed(Node const& node) const
{
    switch (node.type()) {
    case NodeType::DOCUMENT_NODE:
    case NodeType::TEXT_NODE:
        return false;
    case NodeType::COMMENT_NODE:
        return true;
    case NodeType::DOCUMENT_TYPE_NODE:
        return !first_child_of_type<DocumentType>();
    case NodeType::ELEMENT_NODE:
        return !first_child_of_type<Element>();
    default:
        return false;
    }
}

Element* Document::document_element()
{
    return first_child_of_type<Element>();
}

Element const* Document::document_element() const
{
    return first_child_of_type<Element>();
}

HTML::HTMLHtmlElement* Document::html_element()
{
    auto* html = document_element();
    if (is<HTML::HTMLHtmlElement>(html))
        return verify_cast<HTML::HTMLHtmlElement>(html);
    return nullptr;
}

HTML::HTMLHeadElement* Document::head()
{
    auto* html = html_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTML::HTMLHeadElement>();
}

HTML::HTMLElement* Document::body()
{
    auto* html = html_element();
    if (!html)
        return nullptr;
    auto* first_body = html->first_child_of_type<HTML::HTMLBodyElement>();
    if (first_body)
        return first_body;
    auto* first_frameset = html->first_child_of_type<HTML::HTMLFrameSetElement>();
    if (first_frameset)
        return first_frameset;
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-body
ExceptionOr<void> Document::set_body(HTML::HTMLElement* new_body)
{
    if (!is<HTML::HTMLBodyElement>(new_body) && !is<HTML::HTMLFrameSetElement>(new_body))
        return DOM::HierarchyRequestError::create("Invalid document body element, must be 'body' or 'frameset'");

    auto* existing_body = body();
    if (existing_body) {
        (void)TRY(existing_body->parent()->replace_child(*new_body, *existing_body));
        return {};
    }

    auto* document_element = this->document_element();
    if (!document_element)
        return DOM::HierarchyRequestError::create("Missing document element");

    (void)TRY(document_element->append_child(*new_body));
    return {};
}

String Document::title() const
{
    auto* head_element = head();
    if (!head_element)
        return {};

    auto* title_element = head_element->first_child_of_type<HTML::HTMLTitleElement>();
    if (!title_element)
        return {};

    auto raw_title = title_element->text_content();

    StringBuilder builder;
    bool last_was_space = false;
    for (auto code_point : Utf8View(raw_title)) {
        if (is_ascii_space(code_point)) {
            last_was_space = true;
        } else {
            if (last_was_space && !builder.is_empty())
                builder.append(' ');
            builder.append_code_point(code_point);
            last_was_space = false;
        }
    }
    return builder.to_string();
}

void Document::set_title(String const& title)
{
    auto* head_element = const_cast<HTML::HTMLHeadElement*>(head());
    if (!head_element)
        return;

    RefPtr<HTML::HTMLTitleElement> title_element = head_element->first_child_of_type<HTML::HTMLTitleElement>();
    if (!title_element) {
        title_element = static_ptr_cast<HTML::HTMLTitleElement>(create_element(HTML::TagNames::title).release_value());
        head_element->append_child(*title_element);
    }

    title_element->remove_all_children(true);
    title_element->append_child(adopt_ref(*new Text(*this, title)));

    if (auto* page = this->page()) {
        if (browsing_context() == &page->top_level_browsing_context())
            page->client().page_did_change_title(title);
    }
}

void Document::attach_to_browsing_context(Badge<HTML::BrowsingContext>, HTML::BrowsingContext& browsing_context)
{
    m_browsing_context = browsing_context;
}

void Document::detach_from_browsing_context(Badge<HTML::BrowsingContext>, HTML::BrowsingContext& browsing_context)
{
    VERIFY(&browsing_context == m_browsing_context);
    tear_down_layout_tree();
    m_browsing_context = nullptr;
}

void Document::tear_down_layout_tree()
{
    if (!m_layout_root)
        return;

    // Gather up all the layout nodes in a vector and detach them from parents
    // while the vector keeps them alive.

    NonnullRefPtrVector<Layout::Node> layout_nodes;

    m_layout_root->for_each_in_inclusive_subtree([&](auto& layout_node) {
        layout_nodes.append(layout_node);
        return IterationDecision::Continue;
    });

    for (auto& layout_node : layout_nodes) {
        if (layout_node.parent())
            layout_node.parent()->remove_child(layout_node);
    }

    m_layout_root = nullptr;
}

Color Document::background_color(Gfx::Palette const& palette) const
{
    // CSS2 says we should use the HTML element's background color unless it's transparent...
    if (auto* html_element = this->html_element(); html_element && html_element->layout_node()) {
        auto color = html_element->layout_node()->computed_values().background_color();
        if (color.alpha())
            return color;
    }

    // ...in which case we use the BODY element's background color.
    if (auto* body_element = body(); body_element && body_element->layout_node()) {
        auto color = body_element->layout_node()->computed_values().background_color();
        if (color.alpha())
            return color;
    }

    // If both HTML and BODY are transparent, we fall back to the system's "base" palette color.
    return palette.base();
}

Vector<CSS::BackgroundLayerData> const* Document::background_layers() const
{
    auto* body_element = body();
    if (!body_element)
        return {};

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return {};

    return &body_layout_node->background_layers();
}

RefPtr<HTML::HTMLBaseElement> Document::first_base_element_with_href_in_tree_order() const
{
    RefPtr<HTML::HTMLBaseElement> base_element;

    for_each_in_subtree_of_type<HTML::HTMLBaseElement>([&base_element](HTML::HTMLBaseElement const& base_element_in_tree) {
        if (base_element_in_tree.has_attribute(HTML::AttributeNames::href)) {
            base_element = base_element_in_tree;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return base_element;
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#fallback-base-url
AK::URL Document::fallback_base_url() const
{
    // FIXME: 1. If document is an iframe srcdoc document, then return the document base URL of document's browsing context's container document.
    // FIXME: 2. If document's URL is about:blank, and document's browsing context's creator base URL is non-null, then return that creator base URL.

    // 3. Return document's URL.
    return m_url;
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#document-base-url
AK::URL Document::base_url() const
{
    // 1. If there is no base element that has an href attribute in the Document, then return the Document's fallback base URL.
    auto base_element = first_base_element_with_href_in_tree_order();
    if (!base_element)
        return fallback_base_url();

    // 2. Otherwise, return the frozen base URL of the first base element in the Document that has an href attribute, in tree order.
    return base_element->frozen_base_url();
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#parse-a-url
AK::URL Document::parse_url(String const& url) const
{
    // FIXME: Pass in document's character encoding.
    return base_url().complete_url(url);
}

void Document::set_needs_layout()
{
    if (m_needs_layout)
        return;
    m_needs_layout = true;
    schedule_layout_update();
}

void Document::force_layout()
{
    tear_down_layout_tree();
    update_layout();
}

void Document::invalidate_layout()
{
    tear_down_layout_tree();
}

void Document::update_layout()
{
    // NOTE: If our parent document needs a relayout, we must do that *first*.
    //       This is necessary as the parent layout may cause our viewport to change.
    if (browsing_context() && browsing_context()->container())
        browsing_context()->container()->document().update_layout();

    update_style();

    if (!m_needs_layout && m_layout_root)
        return;

    if (!browsing_context())
        return;

    auto viewport_rect = browsing_context()->viewport_rect();

    if (!m_layout_root) {
        m_next_layout_node_serial_id = 0;
        Layout::TreeBuilder tree_builder;
        m_layout_root = static_ptr_cast<Layout::InitialContainingBlock>(tree_builder.build(*this));
    }

    Layout::LayoutState layout_state;
    layout_state.used_values_per_layout_node.resize(layout_node_count());

    {
        Layout::BlockFormattingContext root_formatting_context(layout_state, *m_layout_root, nullptr);

        auto& icb = static_cast<Layout::InitialContainingBlock&>(*m_layout_root);
        auto& icb_state = layout_state.get_mutable(icb);
        icb_state.set_has_definite_width(true);
        icb_state.set_has_definite_height(true);
        icb_state.set_content_width(viewport_rect.width());
        icb_state.set_content_height(viewport_rect.height());

        root_formatting_context.run(*m_layout_root, Layout::LayoutMode::Normal);
    }

    layout_state.commit();

    browsing_context()->set_needs_display();

    if (browsing_context()->is_top_level()) {
        if (auto* page = this->page())
            page->client().page_did_layout();
    }

    m_needs_layout = false;
    m_layout_update_timer->stop();
}

[[nodiscard]] static bool update_style_recursively(DOM::Node& node)
{
    bool const needs_full_style_update = node.document().needs_full_style_update();
    bool needs_relayout = false;

    if (is<Element>(node)) {
        needs_relayout |= static_cast<Element&>(node).recompute_style() == Element::NeedsRelayout::Yes;
    }
    node.set_needs_style_update(false);

    if (needs_full_style_update || node.child_needs_style_update()) {
        if (node.is_element()) {
            if (auto* shadow_root = static_cast<DOM::Element&>(node).shadow_root()) {
                if (needs_full_style_update || shadow_root->needs_style_update() || shadow_root->child_needs_style_update())
                    needs_relayout |= update_style_recursively(*shadow_root);
            }
        }
        node.for_each_child([&](auto& child) {
            if (needs_full_style_update || child.needs_style_update() || child.child_needs_style_update())
                needs_relayout |= update_style_recursively(child);
            return IterationDecision::Continue;
        });
    }

    node.set_child_needs_style_update(false);
    return needs_relayout;
}

void Document::update_style()
{
    if (!browsing_context())
        return;
    if (!needs_full_style_update() && !needs_style_update() && !child_needs_style_update())
        return;
    evaluate_media_rules();
    if (update_style_recursively(*this))
        invalidate_layout();
    m_needs_full_style_update = false;
    m_style_update_timer->stop();
}

void Document::set_link_color(Color color)
{
    m_link_color = color;
}

void Document::set_active_link_color(Color color)
{
    m_active_link_color = color;
}

void Document::set_visited_link_color(Color color)
{
    m_visited_link_color = color;
}

Layout::InitialContainingBlock const* Document::layout_node() const
{
    return static_cast<Layout::InitialContainingBlock const*>(Node::layout_node());
}

Layout::InitialContainingBlock* Document::layout_node()
{
    return static_cast<Layout::InitialContainingBlock*>(Node::layout_node());
}

void Document::set_inspected_node(Node* node)
{
    if (m_inspected_node == node)
        return;

    if (m_inspected_node && m_inspected_node->layout_node())
        m_inspected_node->layout_node()->set_needs_display();

    m_inspected_node = node;

    if (m_inspected_node && m_inspected_node->layout_node())
        m_inspected_node->layout_node()->set_needs_display();
}

static Node* find_common_ancestor(Node* a, Node* b)
{
    if (!a || !b)
        return nullptr;

    if (a == b)
        return a;

    HashTable<Node*> ancestors;
    for (auto* node = a; node; node = node->parent_or_shadow_host())
        ancestors.set(node);

    for (auto* node = b; node; node = node->parent_or_shadow_host()) {
        if (ancestors.contains(node))
            return node;
    }

    return nullptr;
}

void Document::set_hovered_node(Node* node)
{
    if (m_hovered_node == node)
        return;

    RefPtr<Node> old_hovered_node = move(m_hovered_node);
    m_hovered_node = node;

    if (auto* common_ancestor = find_common_ancestor(old_hovered_node, m_hovered_node))
        common_ancestor->invalidate_style();
    else
        invalidate_style();
}

NonnullRefPtr<HTMLCollection> Document::get_elements_by_name(String const& name)
{
    return HTMLCollection::create(*this, [name](Element const& element) {
        return element.name() == name;
    });
}

NonnullRefPtr<HTMLCollection> Document::get_elements_by_class_name(FlyString const& class_name)
{
    return HTMLCollection::create(*this, [class_name, quirks_mode = document().in_quirks_mode()](Element const& element) {
        return element.has_class(class_name, quirks_mode ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive);
    });
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-document-applets
NonnullRefPtr<HTMLCollection> Document::applets()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](auto&) { return false; });
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-document-anchors
NonnullRefPtr<HTMLCollection> Document::anchors()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return is<HTML::HTMLAnchorElement>(element) && element.has_attribute(HTML::AttributeNames::name);
    });
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-images
NonnullRefPtr<HTMLCollection> Document::images()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return is<HTML::HTMLImageElement>(element);
    });
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-embeds
NonnullRefPtr<HTMLCollection> Document::embeds()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return is<HTML::HTMLEmbedElement>(element);
    });
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-plugins
NonnullRefPtr<HTMLCollection> Document::plugins()
{
    return embeds();
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-links
NonnullRefPtr<HTMLCollection> Document::links()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return (is<HTML::HTMLAnchorElement>(element) || is<HTML::HTMLAreaElement>(element)) && element.has_attribute(HTML::AttributeNames::href);
    });
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-forms
NonnullRefPtr<HTMLCollection> Document::forms()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return is<HTML::HTMLFormElement>(element);
    });
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-scripts
NonnullRefPtr<HTMLCollection> Document::scripts()
{
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [](Element const& element) {
        return is<HTML::HTMLScriptElement>(element);
    });
}

Color Document::link_color() const
{
    if (m_link_color.has_value())
        return m_link_color.value();
    if (!page())
        return Color::Blue;
    return page()->palette().link();
}

Color Document::active_link_color() const
{
    if (m_active_link_color.has_value())
        return m_active_link_color.value();
    if (!page())
        return Color::Red;
    return page()->palette().active_link();
}

Color Document::visited_link_color() const
{
    if (m_visited_link_color.has_value())
        return m_visited_link_color.value();
    if (!page())
        return Color::Magenta;
    return page()->palette().visited_link();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#relevant-settings-object
HTML::EnvironmentSettingsObject& Document::relevant_settings_object()
{
    // Then, the relevant settings object for a platform object o is the environment settings object of the relevant Realm for o.
    return verify_cast<HTML::EnvironmentSettingsObject>(*realm().host_defined());
}

JS::Realm& Document::realm()
{
    VERIFY(m_window);
    VERIFY(m_window->wrapper());
    return m_window->wrapper()->shape().realm();
}

JS::Interpreter& Document::interpreter()
{
    if (!m_interpreter) {
        m_interpreter = JS::Interpreter::create_with_existing_realm(realm());
    }
    return *m_interpreter;
}

JS::Value Document::run_javascript(StringView source, StringView filename)
{
    // FIXME: The only user of this function now is javascript: URLs. Refactor them to follow the spec: https://html.spec.whatwg.org/multipage/browsing-the-web.html#javascript-protocol
    auto& interpreter = document().interpreter();
    auto script_or_error = JS::Script::parse(source, interpreter.realm(), filename);
    if (script_or_error.is_error()) {
        // FIXME: Add error logging back.
        return JS::js_undefined();
    }

    auto result = interpreter.run(script_or_error.value());

    if (result.is_error()) {
        // FIXME: I'm sure the spec could tell us something about error propagation here!
        HTML::report_exception(result);

        return {};
    }
    return result.value();
}

// https://dom.spec.whatwg.org/#dom-document-createelement
// FIXME: This only implements step 6 of the algorithm and does not take in options.
DOM::ExceptionOr<NonnullRefPtr<Element>> Document::create_element(String const& tag_name)
{
    if (!is_valid_name(tag_name))
        return DOM::InvalidCharacterError::create("Invalid character in tag name.");

    // FIXME: Let namespace be the HTML namespace, if this is an HTML document or this’s content type is "application/xhtml+xml", and null otherwise.
    return DOM::create_element(*this, tag_name, Namespace::HTML);
}

// https://dom.spec.whatwg.org/#dom-document-createelementns
// https://dom.spec.whatwg.org/#internal-createelementns-steps
// FIXME: This only implements step 4 of the algorithm and does not take in options.
DOM::ExceptionOr<NonnullRefPtr<Element>> Document::create_element_ns(String const& namespace_, String const& qualified_name)
{
    // 1. Let namespace, prefix, and localName be the result of passing namespace and qualifiedName to validate and extract.
    auto extracted_qualified_name = TRY(validate_and_extract(namespace_, qualified_name));

    // FIXME: 2. Let is be null.
    // FIXME: 3. If options is a dictionary and options["is"] exists, then set is to it.

    // 4. Return the result of creating an element given document, localName, namespace, prefix, is, and with the synchronous custom elements flag set.
    return DOM::create_element(*this, extracted_qualified_name.local_name(), extracted_qualified_name.namespace_(), extracted_qualified_name.prefix());
}

NonnullRefPtr<DocumentFragment> Document::create_document_fragment()
{
    return adopt_ref(*new DocumentFragment(*this));
}

NonnullRefPtr<Text> Document::create_text_node(String const& data)
{
    return adopt_ref(*new Text(*this, data));
}

NonnullRefPtr<Comment> Document::create_comment(String const& data)
{
    return adopt_ref(*new Comment(*this, data));
}

NonnullRefPtr<Range> Document::create_range()
{
    return Range::create(*this);
}

// https://dom.spec.whatwg.org/#dom-document-createevent
DOM::ExceptionOr<NonnullRefPtr<Event>> Document::create_event(String const& interface)
{
    // NOTE: This is named event here, since we do step 5 and 6 as soon as possible for each case.
    // 1. Let constructor be null.
    RefPtr<Event> event;

    // 2. If interface is an ASCII case-insensitive match for any of the strings in the first column in the following table,
    //      then set constructor to the interface in the second column on the same row as the matching string:
    auto interface_lowercase = interface.to_lowercase();
    if (interface_lowercase == "beforeunloadevent") {
        event = Event::create(""); // FIXME: Create BeforeUnloadEvent
    } else if (interface_lowercase == "compositionevent") {
        event = Event::create(""); // FIXME: Create CompositionEvent
    } else if (interface_lowercase == "customevent") {
        event = CustomEvent::create("");
    } else if (interface_lowercase == "devicemotionevent") {
        event = Event::create(""); // FIXME: Create DeviceMotionEvent
    } else if (interface_lowercase == "deviceorientationevent") {
        event = Event::create(""); // FIXME: Create DeviceOrientationEvent
    } else if (interface_lowercase == "dragevent") {
        event = Event::create(""); // FIXME: Create DragEvent
    } else if (interface_lowercase.is_one_of("event", "events")) {
        event = Event::create("");
    } else if (interface_lowercase == "focusevent") {
        event = UIEvents::FocusEvent::create("");
    } else if (interface_lowercase == "hashchangeevent") {
        event = Event::create(""); // FIXME: Create HashChangeEvent
    } else if (interface_lowercase == "htmlevents") {
        event = Event::create("");
    } else if (interface_lowercase == "keyboardevent") {
        event = UIEvents::KeyboardEvent::create("");
    } else if (interface_lowercase == "messageevent") {
        event = HTML::MessageEvent::create("");
    } else if (interface_lowercase.is_one_of("mouseevent", "mouseevents")) {
        event = UIEvents::MouseEvent::create("");
    } else if (interface_lowercase == "storageevent") {
        event = Event::create(""); // FIXME: Create StorageEvent
    } else if (interface_lowercase == "svgevents") {
        event = Event::create("");
    } else if (interface_lowercase == "textevent") {
        event = Event::create(""); // FIXME: Create CompositionEvent
    } else if (interface_lowercase == "touchevent") {
        event = Event::create(""); // FIXME: Create TouchEvent
    } else if (interface_lowercase.is_one_of("uievent", "uievents")) {
        event = UIEvents::UIEvent::create("");
    }

    // 3. If constructor is null, then throw a "NotSupportedError" DOMException.
    if (!event) {
        return DOM::NotSupportedError::create("No constructor for interface found");
    }

    // FIXME: 4. If the interface indicated by constructor is not exposed on the relevant global object of this, then throw a "NotSupportedError" DOMException.

    // NOTE: These are done in the if-chain above
    // 5. Let event be the result of creating an event given constructor.
    // 6. Initialize event’s type attribute to the empty string.
    // NOTE: This is handled by each constructor.

    // FIXME: 7. Initialize event’s timeStamp attribute to the result of calling current high resolution time with this’s relevant global object.

    // 8. Initialize event’s isTrusted attribute to false.
    event->set_is_trusted(false);

    // 9. Unset event’s initialized flag.
    event->set_initialized(false);

    // 10. Return event.
    return event.release_nonnull();
}

void Document::set_pending_parsing_blocking_script(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement* script)
{
    m_pending_parsing_blocking_script = script;
}

NonnullRefPtr<HTML::HTMLScriptElement> Document::take_pending_parsing_blocking_script(Badge<HTML::HTMLParser>)
{
    return m_pending_parsing_blocking_script.release_nonnull();
}

void Document::add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_when_parsing_has_finished.append(script);
}

NonnullRefPtrVector<HTML::HTMLScriptElement> Document::take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLParser>)
{
    return move(m_scripts_to_execute_when_parsing_has_finished);
}

void Document::add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_as_soon_as_possible.append(script);
}

NonnullRefPtrVector<HTML::HTMLScriptElement> Document::take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLParser>)
{
    return move(m_scripts_to_execute_as_soon_as_possible);
}

// https://dom.spec.whatwg.org/#dom-document-importnode
ExceptionOr<NonnullRefPtr<Node>> Document::import_node(NonnullRefPtr<Node> node, bool deep)
{
    // 1. If node is a document or shadow root, then throw a "NotSupportedError" DOMException.
    if (is<Document>(*node) || is<ShadowRoot>(*node))
        return DOM::NotSupportedError::create("Cannot import a document or shadow root.");

    // 2. Return a clone of node, with this and the clone children flag set if deep is true.
    return node->clone_node(this, deep);
}

// https://dom.spec.whatwg.org/#concept-node-adopt
void Document::adopt_node(Node& node)
{
    auto& old_document = node.document();
    if (node.parent())
        node.remove();

    if (&old_document != this) {
        node.for_each_shadow_including_descendant([&](auto& inclusive_descendant) {
            inclusive_descendant.set_document({}, *this);
            // FIXME: If inclusiveDescendant is an element, then set the node document of each attribute in inclusiveDescendant’s attribute list to document.
            return IterationDecision::Continue;
        });

        // FIXME: For each inclusiveDescendant in node’s shadow-including inclusive descendants that is custom,
        //        enqueue a custom element callback reaction with inclusiveDescendant, callback name "adoptedCallback",
        //        and an argument list containing oldDocument and document.

        node.for_each_shadow_including_descendant([&](auto& inclusive_descendant) {
            inclusive_descendant.adopted_from(old_document);
            return IterationDecision::Continue;
        });

        // Transfer NodeIterators rooted at `node` from old_document to this document.
        Vector<NodeIterator&> node_iterators_to_transfer;
        for (auto* node_iterator : old_document.m_node_iterators) {
            if (node_iterator->root() == &node)
                node_iterators_to_transfer.append(*node_iterator);
        }
        for (auto& node_iterator : node_iterators_to_transfer) {
            old_document.m_node_iterators.remove(&node_iterator);
            m_node_iterators.set(&node_iterator);
        }
    }
}

// https://dom.spec.whatwg.org/#dom-document-adoptnode
ExceptionOr<NonnullRefPtr<Node>> Document::adopt_node_binding(NonnullRefPtr<Node> node)
{
    if (is<Document>(*node))
        return DOM::NotSupportedError::create("Cannot adopt a document into a document");

    if (is<ShadowRoot>(*node))
        return DOM::HierarchyRequestError::create("Cannot adopt a shadow root into a document");

    if (is<DocumentFragment>(*node) && verify_cast<DocumentFragment>(*node).host())
        return node;

    adopt_node(*node);

    return node;
}

DocumentType const* Document::doctype() const
{
    return first_child_of_type<DocumentType>();
}

String const& Document::compat_mode() const
{
    static String back_compat = "BackCompat";
    static String css1_compat = "CSS1Compat";

    if (m_quirks_mode == QuirksMode::Yes)
        return back_compat;

    return css1_compat;
}

bool Document::is_editable() const
{
    return m_editable;
}

void Document::set_focused_element(Element* element)
{
    if (m_focused_element == element)
        return;

    if (m_focused_element) {
        m_focused_element->did_lose_focus();
        m_focused_element->set_needs_style_update(true);
    }

    m_focused_element = element;

    if (m_focused_element) {
        m_focused_element->did_receive_focus();
        m_focused_element->set_needs_style_update(true);
    }

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

void Document::set_active_element(Element* element)
{
    if (m_active_element == element)
        return;

    m_active_element = element;

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

String Document::ready_state() const
{
    switch (m_readiness) {
    case HTML::DocumentReadyState::Loading:
        return "loading"sv;
    case HTML::DocumentReadyState::Interactive:
        return "interactive"sv;
    case HTML::DocumentReadyState::Complete:
        return "complete"sv;
    }
    VERIFY_NOT_REACHED();
}

// https://html.spec.whatwg.org/#update-the-current-document-readiness
void Document::update_readiness(HTML::DocumentReadyState readiness_value)
{
    // 1. If document's current document readiness equals readinessValue, then return.
    if (m_readiness == readiness_value)
        return;

    // The spec doesn't actually mention updating the current readiness value.
    // FIXME: https://github.com/whatwg/html/issues/7120
    m_readiness = readiness_value;

    // FIXME: 2. If document is associated with an HTML parser, then:
    // FIXME:    1. If document is associated with an HTML parser, then:
    // FIXME:    2. If readinessValue is "complete", and document's load timing info's DOM complete time is 0, then set document's load timing info's DOM complete time to now.
    // FIXME:    3. Otherwise, if readinessValue is "interactive", and document's load timing info's DOM interactive time is 0, then set document's load timing info's DOM interactive time to now.

    // 3. Fire an event named readystatechange at document.
    dispatch_event(Event::create(HTML::EventNames::readystatechange));
}

Page* Document::page()
{
    return m_browsing_context ? m_browsing_context->page() : nullptr;
}

Page const* Document::page() const
{
    return m_browsing_context ? m_browsing_context->page() : nullptr;
}

EventTarget* Document::get_parent(Event const& event)
{
    if (event.type() == HTML::EventNames::load)
        return nullptr;

    return &window();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#completely-finish-loading
void Document::completely_finish_loading()
{
    // 1. Assert: document's browsing context is non-null.
    VERIFY(browsing_context());

    // FIXME: 2. Set document's completely loaded time to the current time.

    // 3. Let container be document's browsing context's container.
    auto* container = browsing_context()->container();

    // If container is an iframe element, then queue an element task on the DOM manipulation task source given container to run the iframe load event steps given container.
    if (container && is<HTML::HTMLIFrameElement>(*container)) {
        container->queue_an_element_task(HTML::Task::Source::DOMManipulation, [container]() mutable {
            run_iframe_load_event_steps(static_cast<HTML::HTMLIFrameElement&>(*container));
        });
    }
    // Otherwise, if container is non-null, then queue an element task on the DOM manipulation task source given container to fire an event named load at container.
    else if (container) {
        container->queue_an_element_task(HTML::Task::Source::DOMManipulation, [container]() mutable {
            container->dispatch_event(DOM::Event::create(HTML::EventNames::load));
        });
    }
}

String Document::cookie(Cookie::Source source)
{
    if (auto* page = this->page())
        return page->client().page_did_request_cookie(m_url, source);
    return {};
}

void Document::set_cookie(String const& cookie_string, Cookie::Source source)
{
    auto cookie = Cookie::parse_cookie(cookie_string);
    if (!cookie.has_value())
        return;

    if (auto* page = this->page())
        page->client().page_did_set_cookie(m_url, cookie.value(), source);
}

String Document::dump_dom_tree_as_json() const
{
    StringBuilder builder;
    auto json = MUST(JsonObjectSerializer<>::try_create(builder));
    serialize_tree_as_json(json);

    MUST(json.finish());
    return builder.to_string();
}

// https://html.spec.whatwg.org/multipage/semantics.html#has-a-style-sheet-that-is-blocking-scripts
bool Document::has_a_style_sheet_that_is_blocking_scripts() const
{
    // A Document has a style sheet that is blocking scripts if its script-blocking style sheet counter is greater than 0,
    if (m_script_blocking_style_sheet_counter > 0)
        return true;

    // ...or if that Document has a non-null browsing context whose container document is non-null and has a script-blocking style sheet counter greater than 0.
    if (!browsing_context() || !browsing_context()->container_document())
        return false;

    return browsing_context()->container_document()->m_script_blocking_style_sheet_counter > 0;
}

String Document::referrer() const
{
    return m_referrer;
}

void Document::set_referrer(String referrer)
{
    m_referrer = referrer;
}

// https://html.spec.whatwg.org/multipage/browsers.html#fully-active
bool Document::is_fully_active() const
{
    // A Document d is said to be fully active when d's browsing context is non-null, d's browsing context's active document is d,
    // and either d's browsing context is a top-level browsing context, or d's browsing context's container document is fully active.
    return browsing_context() && browsing_context()->active_document() == this && (browsing_context()->is_top_level() || browsing_context()->container_document()->is_fully_active());
}

// https://html.spec.whatwg.org/multipage/browsers.html#active-document
bool Document::is_active() const
{
    // A browsing context's active document is its active window's associated Document.
    return browsing_context() && browsing_context()->active_document() == this;
}

// https://html.spec.whatwg.org/multipage/history.html#dom-document-location
Bindings::LocationObject* Document::location()
{
    // The Document object's location attribute's getter must return this Document object's relevant global object's Location object,
    // if this Document object is fully active, and null otherwise.

    if (!is_fully_active())
        return nullptr;

    return window().wrapper()->location_object();
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-document-hidden
bool Document::hidden() const
{
    return false;
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-document-visibilitystate
String Document::visibility_state() const
{
    return hidden() ? "hidden" : "visible";
}

// https://drafts.csswg.org/cssom-view/#run-the-resize-steps
void Document::run_the_resize_steps()
{
    // 1. If doc’s viewport has had its width or height changed
    //    (e.g. as a result of the user resizing the browser window, or changing the page zoom scale factor,
    //    or an iframe element’s dimensions are changed) since the last time these steps were run,
    //    fire an event named resize at the Window object associated with doc.

    if (!browsing_context())
        return;

    auto viewport_size = browsing_context()->viewport_rect().size();
    if (m_last_viewport_size == viewport_size)
        return;
    m_last_viewport_size = viewport_size;

    window().dispatch_event(DOM::Event::create(UIEvents::EventNames::resize));

    update_layout();
}

void Document::add_media_query_list(NonnullRefPtr<CSS::MediaQueryList>& media_query_list)
{
    m_media_query_lists.append(media_query_list);
}

// https://drafts.csswg.org/cssom-view/#evaluate-media-queries-and-report-changes
void Document::evaluate_media_queries_and_report_changes()
{
    // NOTE: Not in the spec, but we take this opportunity to prune null WeakPtrs.
    m_media_query_lists.remove_all_matching([](auto& it) {
        return it.is_null();
    });

    // 1. For each MediaQueryList object target that has doc as its document,
    //    in the order they were created, oldest first, run these substeps:
    for (auto& media_query_list_ptr : m_media_query_lists) {
        // 1. If target’s matches state has changed since the last time these steps
        //    were run, fire an event at target using the MediaQueryListEvent constructor,
        //    with its type attribute initialized to change, its isTrusted attribute
        //    initialized to true, its media attribute initialized to target’s media,
        //    and its matches attribute initialized to target’s matches state.
        if (media_query_list_ptr.is_null())
            continue;
        auto media_query_list = media_query_list_ptr.strong_ref();
        bool did_match = media_query_list->matches();
        bool now_matches = media_query_list->evaluate();

        if (did_match != now_matches) {
            CSS::MediaQueryListEventInit init;
            init.media = media_query_list->media();
            init.matches = now_matches;
            auto event = CSS::MediaQueryListEvent::create(HTML::EventNames::change, init);
            event->set_is_trusted(true);
            media_query_list->dispatch_event(event);
        }
    }

    // Also not in the spec, but this is as good a place as any to evaluate @media rules!
    evaluate_media_rules();
}

void Document::evaluate_media_rules()
{
    bool any_media_queries_changed_match_state = false;
    for (auto& style_sheet : style_sheets().sheets()) {
        if (style_sheet.evaluate_media_queries(window()))
            any_media_queries_changed_match_state = true;
    }

    if (any_media_queries_changed_match_state) {
        style_computer().invalidate_rule_cache();
        invalidate_style();
    }
}

DOMImplementation* Document::implementation()
{
    if (!m_implementation.cell())
        m_implementation = JS::make_handle(*DOMImplementation::create(*this));
    return m_implementation.cell();
}

bool Document::has_focus() const
{
    // FIXME: Return whether we actually have focus.
    return true;
}

void Document::set_parser(Badge<HTML::HTMLParser>, HTML::HTMLParser& parser)
{
    m_parser = parser;
}

void Document::detach_parser(Badge<HTML::HTMLParser>)
{
    m_parser = nullptr;
}

// https://www.w3.org/TR/xml/#NT-NameStartChar
static bool is_valid_name_start_character(u32 code_point)
{
    return code_point == ':'
        || (code_point >= 'A' && code_point <= 'Z')
        || code_point == '_'
        || (code_point >= 'a' && code_point <= 'z')
        || (code_point >= 0xc0 && code_point <= 0xd6)
        || (code_point >= 0xd8 && code_point <= 0xf6)
        || (code_point >= 0xf8 && code_point <= 0x2ff)
        || (code_point >= 0x370 && code_point <= 0x37d)
        || (code_point >= 0x37f && code_point <= 0x1fff)
        || (code_point >= 0x200c && code_point <= 0x200d)
        || (code_point >= 0x2070 && code_point <= 0x218f)
        || (code_point >= 0x2c00 && code_point <= 0x2fef)
        || (code_point >= 0x3001 && code_point <= 0xD7ff)
        || (code_point >= 0xf900 && code_point <= 0xfdcf)
        || (code_point >= 0xfdf0 && code_point <= 0xfffd)
        || (code_point >= 0x10000 && code_point <= 0xeffff);
}

// https://www.w3.org/TR/xml/#NT-NameChar
static inline bool is_valid_name_character(u32 code_point)
{
    return is_valid_name_start_character(code_point)
        || code_point == '-'
        || code_point == '.'
        || (code_point >= '0' && code_point <= '9')
        || code_point == 0xb7
        || (code_point >= 0x300 && code_point <= 0x36f)
        || (code_point >= 0x203f && code_point <= 0x2040);
}

bool Document::is_valid_name(String const& name)
{
    if (name.is_empty())
        return false;

    if (!is_valid_name_start_character(name[0]))
        return false;

    for (size_t i = 1; i < name.length(); ++i) {
        if (!is_valid_name_character(name[i]))
            return false;
    }

    return true;
}

// https://dom.spec.whatwg.org/#validate
ExceptionOr<Document::PrefixAndTagName> Document::validate_qualified_name(String const& qualified_name)
{
    if (qualified_name.is_empty())
        return InvalidCharacterError::create("Empty string is not a valid qualified name.");

    Utf8View utf8view { qualified_name };
    if (!utf8view.validate())
        return InvalidCharacterError::create("Invalid qualified name.");

    Optional<size_t> colon_offset;

    bool at_start_of_name = true;

    for (auto it = utf8view.begin(); it != utf8view.end(); ++it) {
        auto code_point = *it;
        if (code_point == ':') {
            if (colon_offset.has_value())
                return InvalidCharacterError::create("More than one colon (:) in qualified name.");
            colon_offset = utf8view.byte_offset_of(it);
            at_start_of_name = true;
            continue;
        }
        if (at_start_of_name) {
            if (!is_valid_name_start_character(code_point))
                return InvalidCharacterError::create("Invalid start of qualified name.");
            at_start_of_name = false;
            continue;
        }
        if (!is_valid_name_character(code_point))
            return InvalidCharacterError::create("Invalid character in qualified name.");
    }

    if (!colon_offset.has_value())
        return Document::PrefixAndTagName {
            .prefix = {},
            .tag_name = qualified_name,
        };

    if (*colon_offset == 0)
        return InvalidCharacterError::create("Qualified name can't start with colon (:).");

    if (*colon_offset >= (qualified_name.length() - 1))
        return InvalidCharacterError::create("Qualified name can't end with colon (:).");

    return Document::PrefixAndTagName {
        .prefix = qualified_name.substring_view(0, *colon_offset),
        .tag_name = qualified_name.substring_view(*colon_offset + 1),
    };
}

// https://dom.spec.whatwg.org/#dom-document-createnodeiterator
NonnullRefPtr<NodeIterator> Document::create_node_iterator(Node& root, unsigned what_to_show, NodeFilter* filter)
{
    return NodeIterator::create(root, what_to_show, filter);
}

// https://dom.spec.whatwg.org/#dom-document-createtreewalker
NonnullRefPtr<TreeWalker> Document::create_tree_walker(Node& root, unsigned what_to_show, NodeFilter* filter)
{
    return TreeWalker::create(root, what_to_show, filter);
}

void Document::register_node_iterator(Badge<NodeIterator>, NodeIterator& node_iterator)
{
    auto result = m_node_iterators.set(&node_iterator);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void Document::unregister_node_iterator(Badge<NodeIterator>, NodeIterator& node_iterator)
{
    bool was_removed = m_node_iterators.remove(&node_iterator);
    VERIFY(was_removed);
}

void Document::increment_number_of_things_delaying_the_load_event(Badge<DocumentLoadEventDelayer>)
{
    ++m_number_of_things_delaying_the_load_event;

    if (auto* page = this->page())
        page->client().page_did_update_resource_count(m_number_of_things_delaying_the_load_event);
}

void Document::decrement_number_of_things_delaying_the_load_event(Badge<DocumentLoadEventDelayer>)
{
    VERIFY(m_number_of_things_delaying_the_load_event);
    --m_number_of_things_delaying_the_load_event;

    if (auto* page = this->page())
        page->client().page_did_update_resource_count(m_number_of_things_delaying_the_load_event);
}

void Document::invalidate_stacking_context_tree()
{
    if (auto* paint_box = this->paint_box())
        const_cast<Painting::PaintableBox*>(paint_box)->invalidate_stacking_context();
}

void Document::check_favicon_after_loading_link_resource()
{
    // https://html.spec.whatwg.org/multipage/links.html#rel-icon
    // NOTE: firefox also load favicons outside the head tag, which is against spec (see table 4.6.7)
    auto head_element = head();
    auto favicon_link_elements = HTMLCollection::create(*head_element, [](Element const& element) {
        if (!is<HTML::HTMLLinkElement>(element))
            return false;

        return static_cast<HTML::HTMLLinkElement const&>(element).has_loaded_icon();
    });

    if (favicon_link_elements->length() == 0) {
        dbgln_if(SPAM_DEBUG, "No favicon found to be used");
        return;
    }

    // 4.6.7.8 Link type "icon"
    //
    // If there are multiple equally appropriate icons, user agents must use the last one declared
    // in tree order at the time that the user agent collected the list of icons.
    //
    // If multiple icons are provided, the user agent must select the most appropriate icon
    // according to the type, media, and sizes attributes.
    //
    // FIXME: There is no selective behavior yet for favicons.
    for (auto i = favicon_link_elements->length(); i-- > 0;) {
        auto favicon_element = favicon_link_elements->item(i);

        if (favicon_element == m_active_element)
            return;

        // If the user agent tries to use an icon but that icon is determined, upon closer examination,
        // to in fact be inappropriate (...), then the user agent must try the next-most-appropriate icon
        // as determined by the attributes.
        if (static_cast<HTML::HTMLLinkElement*>(favicon_element)->load_favicon_and_use_if_window_is_active()) {
            m_active_favicon = favicon_element;
            return;
        }
    }

    dbgln_if(SPAM_DEBUG, "No favicon found to be used");
}

void Document::set_window(Badge<HTML::BrowsingContext>, HTML::Window& window)
{
    m_window = window;
}

Bindings::WindowObject& Document::preferred_window_object() const
{
    if (m_window && m_window->wrapper())
        return const_cast<Bindings::WindowObject&>(*m_window->wrapper());
    return Bindings::main_thread_internal_window_object();
}

}
