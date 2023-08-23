/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/CSS/MediaQueryListEvent.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/VisualViewport.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/CustomEvent.h>
#include <LibWeb/DOM/DOMImplementation.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentObserver.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/NodeIterator.h>
#include <LibWeb/DOM/ProcessingInstruction.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/TreeWalker.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/CustomElements/CustomElementDefinition.h>
#include <LibWeb/HTML/CustomElements/CustomElementReactionNames.h>
#include <LibWeb/HTML/CustomElements/CustomElementRegistry.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLBaseElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLDocument.h>
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
#include <LibWeb/HTML/ListOfAvailableImages.h>
#include <LibWeb/HTML/Location.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/SVG/SVGElement.h>
#include <LibWeb/SVG/SVGTitleElement.h>
#include <LibWeb/SVG/TagNames.h>
#include <LibWeb/Selection/Selection.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/FocusEvent.h>
#include <LibWeb/UIEvents/KeyboardEvent.h>
#include <LibWeb/UIEvents/MouseEvent.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

// https://html.spec.whatwg.org/multipage/origin.html#obtain-browsing-context-navigation
static JS::NonnullGCPtr<HTML::BrowsingContext> obtain_a_browsing_context_to_use_for_a_navigation_response(
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
    auto new_browsing_context = HTML::BrowsingContext::create_a_new_top_level_browsing_context(*browsing_context.page());

    // FIXME: 4. If navigationCOOP's value is "same-origin-plurs-COEP", then set newBrowsingContext's group's
    //           cross-origin isolation mode to either "logical" or "concrete". The choice of which is implementation-defined.

    // 5. If sandboxFlags is not empty, then:
    if (!sandbox_flags.is_empty()) {
        // 1. Assert navigationCOOP's value is "unsafe-none".
        VERIFY(navigation_coop.value == HTML::CrossOriginOpenerPolicyValue::UnsafeNone);

        // 2. Assert: newBrowsingContext's popup sandboxing flag set is empty.

        // 3. Set newBrowsingContext's popup sandboxing flag set to a clone of sandboxFlags.
    }

    // 6. Discard browsingContext.
    browsing_context.discard();

    // 7. Return newBrowsingContext.
    return new_browsing_context;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#initialise-the-document-object
WebIDL::ExceptionOr<JS::NonnullGCPtr<Document>> Document::create_and_initialize(Type type, DeprecatedString content_type, HTML::NavigationParams navigation_params)
{
    // 1. Let browsingContext be the result of the obtaining a browsing context to use for a navigation response
    //    given navigationParams's browsing context, navigationParams's final sandboxing flag set,
    //    navigationParams's cross-origin opener policy, and navigationParams's COOP enforcement result.
    auto browsing_context = obtain_a_browsing_context_to_use_for_a_navigation_response(
        *navigation_params.browsing_context,
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

    JS::GCPtr<HTML::Window> window;

    // 5. If browsingContext is still on its initial about:blank Document,
    //    and navigationParams's history handling is "replace",
    //    and browsingContext's active document's origin is same origin-domain with navigationParams's origin,
    //    then do nothing.
    if (browsing_context->still_on_its_initial_about_blank_document()
        && navigation_params.history_handling == HTML::HistoryHandlingBehavior::Replace
        && (browsing_context->active_document() && browsing_context->active_document()->origin().is_same_origin(navigation_params.origin))) {
        // Do nothing.
        // NOTE: This means that both the initial about:blank Document, and the new Document that is about to be created, will share the same Window object.
        window = browsing_context->active_window();
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
            [&](JS::Realm& realm) -> JS::Object* {
                // - For the global object, create a new Window object.
                window = HTML::Window::create(realm);
                return window;
            },
            [&](JS::Realm&) -> JS::Object* {
                // - For the global this binding, use browsingContext's WindowProxy object.
                return browsing_context->window_proxy();
            });

        // 6. Let topLevelCreationURL be creationURL.
        auto top_level_creation_url = creation_url;

        // 7. Let topLevelOrigin be navigationParams's origin.
        auto top_level_origin = navigation_params.origin;

        // 8. If browsingContext is not a top-level browsing context, then:
        if (!browsing_context->is_top_level()) {
            // 1. Let parentEnvironment be browsingContext's container's relevant settings object.
            VERIFY(browsing_context->container());
            auto& parent_environment = HTML::relevant_settings_object(*browsing_context->container());

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
    //    policy container is navigationParams's policy container,
    //    FIXME: permissions policy is permissionsPolicy,
    //    active sandboxing flag set is navigationParams's final sandboxing flag set,
    //    FIXME: and cross-origin opener policy is navigationParams's cross-origin opener policy,
    //    FIXME: load timing info is loadTimingInfo,
    //    and navigation id is navigationParams's id.
    auto document = HTML::HTMLDocument::create(window->realm());
    document->m_type = type;
    document->m_content_type = move(content_type);
    document->set_origin(navigation_params.origin);
    document->m_policy_container = navigation_params.policy_container;
    document->m_active_sandboxing_flag_set = navigation_params.final_sandboxing_flag_set;
    document->m_navigation_id = navigation_params.id;

    document->m_window = window;
    window->set_associated_document(*document);

    // 9. Set document's URL to creationURL.
    document->m_url = creation_url.value();

    // 10. Set document's current document readiness to "loading".
    document->m_readiness = HTML::DocumentReadyState::Loading;

    // FIXME: 11. Run CSP initialization for a Document given document.

    // 12. If navigationParams's request is non-null, then:
    if (navigation_params.request) {
        // 1. Set document's referrer to the empty string.
        document->m_referrer = DeprecatedString::empty();

        // 2. Let referrer be navigationParams's request's referrer.
        auto& referrer = navigation_params.request->referrer();

        // 3. If referrer is a URL record, then set document's referrer to the serialization of referrer.
        if (referrer.has<AK::URL>()) {
            document->m_referrer = referrer.get<AK::URL>().serialize();
        }
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

WebIDL::ExceptionOr<JS::NonnullGCPtr<Document>> Document::construct_impl(JS::Realm& realm)
{
    return Document::create(realm);
}

JS::NonnullGCPtr<Document> Document::create(JS::Realm& realm, AK::URL const& url)
{
    return realm.heap().allocate<Document>(realm, realm, url);
}

Document::Document(JS::Realm& realm, const AK::URL& url)
    : ParentNode(realm, *this, NodeType::DOCUMENT_NODE)
    , m_style_computer(make<CSS::StyleComputer>(*this))
    , m_url(url)
{
    HTML::main_thread_event_loop().register_document({}, *this);

    m_style_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_style();
    }).release_value_but_fixme_should_propagate_errors();

    m_layout_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_layout();
    }).release_value_but_fixme_should_propagate_errors();
}

Document::~Document()
{
    HTML::main_thread_event_loop().unregister_document({}, *this);
}

void Document::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DocumentPrototype>(realm, "Document"));

    m_selection = heap().allocate<Selection::Selection>(realm, realm, *this);

    m_list_of_available_images = make<HTML::ListOfAvailableImages>();
}

void Document::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window);
    visitor.visit(m_layout_root);
    visitor.visit(m_style_sheets);
    visitor.visit(m_hovered_node);
    visitor.visit(m_inspected_node);
    visitor.visit(m_active_favicon);
    visitor.visit(m_focused_element);
    visitor.visit(m_active_element);
    visitor.visit(m_implementation);
    visitor.visit(m_current_script);
    visitor.visit(m_associated_inert_template_document);
    visitor.visit(m_appropriate_template_contents_owner_document);
    visitor.visit(m_pending_parsing_blocking_script);
    visitor.visit(m_history);

    visitor.visit(m_browsing_context);

    visitor.visit(m_applets);
    visitor.visit(m_anchors);
    visitor.visit(m_images);
    visitor.visit(m_embeds);
    visitor.visit(m_links);
    visitor.visit(m_forms);
    visitor.visit(m_scripts);
    visitor.visit(m_all);
    visitor.visit(m_selection);
    visitor.visit(m_first_base_element_with_href_in_tree_order);
    visitor.visit(m_parser);
    visitor.visit(m_lazy_load_intersection_observer);

    for (auto& script : m_scripts_to_execute_when_parsing_has_finished)
        visitor.visit(script);
    for (auto& script : m_scripts_to_execute_as_soon_as_possible)
        visitor.visit(script);

    for (auto& node_iterator : m_node_iterators)
        visitor.visit(node_iterator);

    for (auto& document_observer : m_document_observers)
        visitor.visit(document_observer);

    for (auto& target : m_pending_scroll_event_targets)
        visitor.visit(target);
    for (auto& target : m_pending_scrollend_event_targets)
        visitor.visit(target);
}

// https://w3c.github.io/selection-api/#dom-document-getselection
JS::GCPtr<Selection::Selection> Document::get_selection() const
{
    // The method must return the selection associated with this if this has an associated browsing context,
    // and it must return null otherwise.
    if (!browsing_context())
        return {};
    return m_selection;
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-write
WebIDL::ExceptionOr<void> Document::write(Vector<DeprecatedString> const& strings)
{
    StringBuilder builder;
    builder.join(""sv, strings);

    return run_the_document_write_steps(builder.to_deprecated_string());
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-writeln
WebIDL::ExceptionOr<void> Document::writeln(Vector<DeprecatedString> const& strings)
{
    StringBuilder builder;
    builder.join(""sv, strings);
    builder.append("\n"sv);

    return run_the_document_write_steps(builder.to_deprecated_string());
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#document-write-steps
WebIDL::ExceptionOr<void> Document::run_the_document_write_steps(DeprecatedString input)
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException.
    if (m_type == Type::XML)
        return WebIDL::InvalidStateError::create(realm(), "write() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return WebIDL::InvalidStateError::create(realm(), "throw-on-dynamic-markup-insertion-counter greater than zero.");

    // 3. If document's active parser was aborted is true, then return.
    if (m_active_parser_was_aborted)
        return {};

    // 4. If the insertion point is undefined, then:
    if (!(m_parser && m_parser->tokenizer().is_insertion_point_defined())) {
        // 1. If document's unload counter is greater than 0 or document's ignore-destructive-writes counter is greater than 0, then return.
        if (m_unload_counter > 0 || m_ignore_destructive_writes_counter > 0)
            return {};

        // 2. Run the document open steps with document.
        TRY(open());
    }

    // 5. Insert input into the input stream just before the insertion point.
    m_parser->tokenizer().insert_input_at_insertion_point(input);

    // 6. If there is no pending parsing-blocking script, have the HTML parser process input, one code point at a time, processing resulting tokens as they are emitted, and stopping when the tokenizer reaches the insertion point or when the processing of the tokenizer is aborted by the tree construction stage (this can happen if a script end tag token is emitted by the tokenizer).
    if (!pending_parsing_blocking_script())
        m_parser->run();

    return {};
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-open
WebIDL::ExceptionOr<Document*> Document::open(DeprecatedString const&, DeprecatedString const&)
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException exception.
    if (m_type == Type::XML)
        return WebIDL::InvalidStateError::create(realm(), "open() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return WebIDL::InvalidStateError::create(realm(), "throw-on-dynamic-markup-insertion-counter greater than zero.");

    // FIXME: 3. Let entryDocument be the entry global object's associated Document.
    auto& entry_document = *this;

    // 4. If document's origin is not same origin to entryDocument's origin, then throw a "SecurityError" DOMException.
    if (origin() != entry_document.origin())
        return WebIDL::SecurityError::create(realm(), "Document.origin() not the same as entryDocument's.");

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
            new_url.set_fragment({});

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

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-document-open-window
WebIDL::ExceptionOr<JS::GCPtr<HTML::WindowProxy>> Document::open(DeprecatedString const& url, DeprecatedString const& name, DeprecatedString const& features)
{
    // 1. If this is not fully active, then throw an "InvalidAccessError" DOMException exception.
    if (!is_fully_active())
        return WebIDL::InvalidAccessError::create(realm(), "Cannot perform open on a document that isn't fully active."sv);

    // 2. Return the result of running the window open steps with url, name, and features.
    return window().open_impl(url, name, features);
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#closing-the-input-stream
WebIDL::ExceptionOr<void> Document::close()
{
    // 1. If document is an XML document, then throw an "InvalidStateError" DOMException exception.
    if (m_type == Type::XML)
        return WebIDL::InvalidStateError::create(realm(), "close() called on XML document.");

    // 2. If document's throw-on-dynamic-markup-insertion counter is greater than 0, then throw an "InvalidStateError" DOMException.
    if (m_throw_on_dynamic_markup_insertion_counter > 0)
        return WebIDL::InvalidStateError::create(realm(), "throw-on-dynamic-markup-insertion-counter greater than zero.");

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

// https://html.spec.whatwg.org/multipage/dom.html#the-html-element-2
HTML::HTMLHtmlElement* Document::html_element()
{
    // The html element of a document is its document element, if it's an html element, and null otherwise.
    auto* html = document_element();
    if (is<HTML::HTMLHtmlElement>(html))
        return verify_cast<HTML::HTMLHtmlElement>(html);
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/dom.html#the-head-element-2
HTML::HTMLHeadElement* Document::head()
{
    // The head element of a document is the first head element that is a child of the html element, if there is one,
    // or null otherwise.
    auto* html = html_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTML::HTMLHeadElement>();
}

// https://html.spec.whatwg.org/multipage/dom.html#the-title-element-2
JS::GCPtr<HTML::HTMLTitleElement> Document::title_element()
{
    // The title element of a document is the first title element in the document (in tree order), if there is one, or
    // null otherwise.
    JS::GCPtr<HTML::HTMLTitleElement> title_element = nullptr;

    for_each_in_subtree_of_type<HTML::HTMLTitleElement>([&](auto& title_element_in_tree) {
        title_element = title_element_in_tree;
        return IterationDecision::Break;
    });

    return title_element;
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
WebIDL::ExceptionOr<void> Document::set_body(HTML::HTMLElement* new_body)
{
    if (!is<HTML::HTMLBodyElement>(new_body) && !is<HTML::HTMLFrameSetElement>(new_body))
        return WebIDL::HierarchyRequestError::create(realm(), "Invalid document body element, must be 'body' or 'frameset'");

    auto* existing_body = body();
    if (existing_body) {
        (void)TRY(existing_body->parent()->replace_child(*new_body, *existing_body));
        return {};
    }

    auto* document_element = this->document_element();
    if (!document_element)
        return WebIDL::HierarchyRequestError::create(realm(), "Missing document element");

    (void)TRY(document_element->append_child(*new_body));
    return {};
}

// https://html.spec.whatwg.org/multipage/dom.html#document.title
DeprecatedString Document::title() const
{
    auto value = DeprecatedString::empty();

    // 1. If the document element is an SVG svg element, then let value be the child text content of the first SVG title
    //    element that is a child of the document element.
    if (auto const* document_element = this->document_element(); is<SVG::SVGElement>(document_element)) {
        if (auto const* title_element = document_element->first_child_of_type<SVG::SVGTitleElement>())
            value = title_element->child_text_content();
    }

    // 2. Otherwise, let value be the child text content of the title element, or the empty string if the title element
    //    is null.
    else if (auto title_element = this->title_element()) {
        value = title_element->text_content();
    }

    // 3. Strip and collapse ASCII whitespace in value.
    auto title = Infra::strip_and_collapse_whitespace(value).release_value_but_fixme_should_propagate_errors();

    // 4. Return value.
    return title.to_deprecated_string();
}

// https://html.spec.whatwg.org/multipage/dom.html#document.title
WebIDL::ExceptionOr<void> Document::set_title(DeprecatedString const& title)
{
    auto* document_element = this->document_element();

    // -> If the document element is an SVG svg element
    if (is<SVG::SVGElement>(document_element)) {
        JS::GCPtr<Element> element;

        // 1. If there is an SVG title element that is a child of the document element, let element be the first such
        //    element.
        if (auto* title_element = document_element->first_child_of_type<SVG::SVGTitleElement>()) {
            element = title_element;
        }
        // 2. Otherwise:
        else {
            // 1. Let element be the result of creating an element given the document element's node document, title,
            //    and the SVG namespace.
            element = TRY(DOM::create_element(*this, HTML::TagNames::title, Namespace::SVG));

            // 2. Insert element as the first child of the document element.
            document_element->insert_before(*element, nullptr);
        }

        // 3. String replace all with the given value within element.
        element->string_replace_all(title);
    }

    // -> If the document element is in the HTML namespace
    else if (document_element && document_element->namespace_() == Namespace::HTML) {
        auto title_element = this->title_element();
        auto* head_element = this->head();

        // 1. If the title element is null and the head element is null, then return.
        if (title_element == nullptr && head_element == nullptr)
            return {};

        JS::GCPtr<Element> element;

        // 2. If the title element is non-null, let element be the title element.
        if (title_element) {
            element = title_element;
        }
        // 3. Otherwise:
        else {
            // 1. Let element be the result of creating an element given the document element's node document, title,
            //    and the HTML namespace.
            element = TRY(DOM::create_element(*this, HTML::TagNames::title, Namespace::HTML));

            // 2. Append element to the head element.
            TRY(head_element->append_child(*element));
        }

        // 4. String replace all with the given value within element.
        element->string_replace_all(title);
    }

    // -> Otherwise
    else {
        // Do nothing.
        return {};
    }

    if (auto* page = this->page()) {
        if (browsing_context() == &page->top_level_browsing_context())
            page->client().page_did_change_title(title);
    }

    return {};
}

void Document::tear_down_layout_tree()
{
    if (!m_layout_root)
        return;

    // Gather up all the layout nodes in a vector and detach them from parents
    // while the vector keeps them alive.

    Vector<JS::Handle<Layout::Node>> layout_nodes;

    m_layout_root->for_each_in_inclusive_subtree([&](auto& layout_node) {
        layout_nodes.append(layout_node);
        return IterationDecision::Continue;
    });

    for (auto& layout_node : layout_nodes) {
        if (layout_node->parent())
            layout_node->parent()->remove_child(*layout_node);
    }

    m_layout_root = nullptr;
}

Color Document::background_color() const
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
        return color;
    }

    // By default, the document is transparent.
    // The outermost canvas is colored by the PageHost.
    return Color::Transparent;
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

void Document::update_base_element(Badge<HTML::HTMLBaseElement>)
{
    JS::GCPtr<HTML::HTMLBaseElement const> base_element;

    for_each_in_subtree_of_type<HTML::HTMLBaseElement>([&base_element](HTML::HTMLBaseElement const& base_element_in_tree) {
        if (base_element_in_tree.has_attribute(HTML::AttributeNames::href)) {
            base_element = &base_element_in_tree;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    m_first_base_element_with_href_in_tree_order = base_element;
}

JS::GCPtr<HTML::HTMLBaseElement const> Document::first_base_element_with_href_in_tree_order() const
{
    return m_first_base_element_with_href_in_tree_order;
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#fallback-base-url
AK::URL Document::fallback_base_url() const
{
    // FIXME: 1. If document is an iframe srcdoc document, then return the document base URL of document's browsing context's container document.

    // 2. If document's URL is about:blank, and document's browsing context's creator base URL is non-null, then return that creator base URL.
    if (m_url == "about:blank"sv && browsing_context() && browsing_context()->creator_url().has_value())
        return browsing_context()->creator_url().value();

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
AK::URL Document::parse_url(StringView url) const
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
    schedule_layout_update();
}

static void propagate_overflow_to_viewport(Element& root_element, Layout::Viewport& viewport)
{
    // https://drafts.csswg.org/css-overflow-3/#overflow-propagation
    // UAs must apply the overflow-* values set on the root element to the viewport
    // when the root element’s display value is not none.
    auto* overflow_origin_node = root_element.layout_node();
    auto& viewport_computed_values = const_cast<CSS::MutableComputedValues&>(static_cast<CSS::MutableComputedValues const&>(static_cast<CSS::ComputedValues const&>(viewport.computed_values())));

    // However, when the root element is an [HTML] html element (including XML syntax for HTML)
    // whose overflow value is visible (in both axes), and that element has as a child
    // a body element whose display value is also not none,
    // user agents must instead apply the overflow-* values of the first such child element to the viewport.
    if (root_element.is_html_html_element()) {
        auto* root_element_layout_node = root_element.layout_node();
        auto& root_element_computed_values = const_cast<CSS::MutableComputedValues&>(static_cast<CSS::MutableComputedValues const&>(static_cast<CSS::ComputedValues const&>(root_element_layout_node->computed_values())));
        if (root_element_computed_values.overflow_x() == CSS::Overflow::Visible && root_element_computed_values.overflow_y() == CSS::Overflow::Visible) {
            auto* body_element = root_element.first_child_of_type<HTML::HTMLBodyElement>();
            if (body_element && body_element->layout_node())
                overflow_origin_node = body_element->layout_node();
        }
    }

    // NOTE: This is where we assign the chosen overflow values to the viewport.
    auto& overflow_origin_computed_values = const_cast<CSS::MutableComputedValues&>(static_cast<CSS::MutableComputedValues const&>(static_cast<CSS::ComputedValues const&>(overflow_origin_node->computed_values())));
    viewport_computed_values.set_overflow_x(overflow_origin_computed_values.overflow_x());
    viewport_computed_values.set_overflow_y(overflow_origin_computed_values.overflow_y());

    // The element from which the value is propagated must then have a used overflow value of visible.
    overflow_origin_computed_values.set_overflow_x(CSS::Overflow::Visible);
    overflow_origin_computed_values.set_overflow_y(CSS::Overflow::Visible);
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

    // NOTE: If this is a document hosting <template> contents, layout is unnecessary.
    if (m_created_for_appropriate_template_contents)
        return;

    if (!browsing_context())
        return;

    auto viewport_rect = this->viewport_rect();

    if (!m_layout_root) {
        Layout::TreeBuilder tree_builder;
        m_layout_root = verify_cast<Layout::Viewport>(*tree_builder.build(*this));
    }

    if (auto* document_element = this->document_element()) {
        propagate_overflow_to_viewport(*document_element, *m_layout_root);
    }

    Layout::LayoutState layout_state;

    {
        Layout::BlockFormattingContext root_formatting_context(layout_state, *m_layout_root, nullptr);

        auto& viewport = static_cast<Layout::Viewport&>(*m_layout_root);
        auto& viewport_state = layout_state.get_mutable(viewport);
        viewport_state.set_content_width(viewport_rect.width());
        viewport_state.set_content_height(viewport_rect.height());

        if (auto* document_element = this->document_element()) {
            VERIFY(document_element->layout_node());
            auto& icb_state = layout_state.get_mutable(verify_cast<Layout::NodeWithStyleAndBoxModelMetrics>(*document_element->layout_node()));
            icb_state.set_content_width(viewport_rect.width());
            icb_state.set_content_height(viewport_rect.height());
        }

        root_formatting_context.run(
            *m_layout_root,
            Layout::LayoutMode::Normal,
            Layout::AvailableSpace(
                Layout::AvailableSize::make_definite(viewport_rect.width()),
                Layout::AvailableSize::make_definite(viewport_rect.height())));
    }

    layout_state.commit(*m_layout_root);

    // Broadcast the current viewport rect to any new paintables, so they know whether they're visible or not.
    inform_all_viewport_clients_about_the_current_viewport_rect();

    browsing_context()->set_needs_display();

    if (browsing_context()->is_top_level() && browsing_context()->active_document() == this) {
        if (auto* page = this->page())
            page->client().page_did_layout();
    }

    m_layout_root->recompute_selection_states();

    m_needs_layout = false;
    m_layout_update_timer->stop();
}

[[nodiscard]] static Element::RequiredInvalidationAfterStyleChange update_style_recursively(DOM::Node& node)
{
    bool const needs_full_style_update = node.document().needs_full_style_update();
    Element::RequiredInvalidationAfterStyleChange invalidation;

    if (is<Element>(node)) {
        invalidation |= static_cast<Element&>(node).recompute_style();
    }
    node.set_needs_style_update(false);

    if (needs_full_style_update || node.child_needs_style_update()) {
        if (node.is_element()) {
            if (auto* shadow_root = static_cast<DOM::Element&>(node).shadow_root_internal()) {
                if (needs_full_style_update || shadow_root->needs_style_update() || shadow_root->child_needs_style_update())
                    invalidation |= update_style_recursively(*shadow_root);
            }
        }
        node.for_each_child([&](auto& child) {
            if (needs_full_style_update || child.needs_style_update() || child.child_needs_style_update())
                invalidation |= update_style_recursively(child);
            return IterationDecision::Continue;
        });
    }

    node.set_child_needs_style_update(false);
    return invalidation;
}

void Document::update_style()
{
    if (!browsing_context())
        return;
    if (!needs_full_style_update() && !needs_style_update() && !child_needs_style_update())
        return;

    // NOTE: If this is a document hosting <template> contents, style update is unnecessary.
    if (m_created_for_appropriate_template_contents)
        return;

    evaluate_media_rules();

    auto invalidation = update_style_recursively(*this);
    if (invalidation.rebuild_layout_tree) {
        invalidate_layout();
    } else {
        if (invalidation.relayout)
            set_needs_layout();
        if (invalidation.rebuild_stacking_context_tree)
            invalidate_stacking_context_tree();
    }
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

Layout::Viewport const* Document::layout_node() const
{
    return static_cast<Layout::Viewport const*>(Node::layout_node());
}

Layout::Viewport* Document::layout_node()
{
    return static_cast<Layout::Viewport*>(Node::layout_node());
}

void Document::set_inspected_node(Node* node, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    if (m_inspected_node.ptr() == node && m_inspected_pseudo_element == pseudo_element)
        return;

    if (auto layout_node = inspected_layout_node())
        layout_node->set_needs_display();

    m_inspected_node = node;
    m_inspected_pseudo_element = pseudo_element;

    if (auto layout_node = inspected_layout_node())
        layout_node->set_needs_display();
}

Layout::Node* Document::inspected_layout_node()
{
    if (!m_inspected_node)
        return nullptr;
    if (!m_inspected_pseudo_element.has_value() || !m_inspected_node->is_element())
        return m_inspected_node->layout_node();
    auto& element = static_cast<Element&>(*m_inspected_node);
    return element.get_pseudo_element_node(m_inspected_pseudo_element.value());
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
    if (m_hovered_node.ptr() == node)
        return;

    JS::GCPtr<Node> old_hovered_node = move(m_hovered_node);
    m_hovered_node = node;

    auto* common_ancestor = find_common_ancestor(old_hovered_node, m_hovered_node);
    if (common_ancestor)
        common_ancestor->invalidate_style();
    else
        invalidate_style();

    // https://w3c.github.io/uievents/#mouseleave
    if (old_hovered_node && (!m_hovered_node || !m_hovered_node->is_descendant_of(*old_hovered_node))) {
        // FIXME: Check if we need to dispatch these events in a specific order.
        for (auto target = old_hovered_node; target && target.ptr() != common_ancestor; target = target->parent()) {
            // FIXME: Populate the event with mouse coordinates, etc.
            target->dispatch_event(UIEvents::MouseEvent::create(realm(), UIEvents::EventNames::mouseleave));
        }
    }

    // https://w3c.github.io/uievents/#mouseenter
    if (m_hovered_node && (!old_hovered_node || !m_hovered_node->is_ancestor_of(*old_hovered_node))) {
        // FIXME: Check if we need to dispatch these events in a specific order.
        for (auto target = m_hovered_node; target && target.ptr() != common_ancestor; target = target->parent()) {
            // FIXME: Populate the event with mouse coordinates, etc.
            target->dispatch_event(UIEvents::MouseEvent::create(realm(), UIEvents::EventNames::mouseenter));
        }
    }
}

JS::NonnullGCPtr<HTMLCollection> Document::get_elements_by_name(DeprecatedString const& name)
{
    return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [name](Element const& element) {
        return element.name() == name;
    });
}

JS::NonnullGCPtr<HTMLCollection> Document::get_elements_by_class_name(DeprecatedFlyString const& class_names)
{
    Vector<FlyString> list_of_class_names;
    for (auto& name : class_names.view().split_view(' ')) {
        list_of_class_names.append(FlyString::from_utf8(name).release_value_but_fixme_should_propagate_errors());
    }
    return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [list_of_class_names = move(list_of_class_names), quirks_mode = document().in_quirks_mode()](Element const& element) {
        for (auto& name : list_of_class_names) {
            if (!element.has_class(name, quirks_mode ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive))
                return false;
        }
        return true;
    });
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-document-applets
JS::NonnullGCPtr<HTMLCollection> Document::applets()
{
    if (!m_applets)
        m_applets = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](auto&) { return false; });
    return *m_applets;
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-document-anchors
JS::NonnullGCPtr<HTMLCollection> Document::anchors()
{
    if (!m_anchors) {
        m_anchors = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is<HTML::HTMLAnchorElement>(element) && element.has_attribute(HTML::AttributeNames::name);
        });
    }
    return *m_anchors;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-images
JS::NonnullGCPtr<HTMLCollection> Document::images()
{
    if (!m_images) {
        m_images = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is<HTML::HTMLImageElement>(element);
        });
    }
    return *m_images;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-embeds
JS::NonnullGCPtr<HTMLCollection> Document::embeds()
{
    if (!m_embeds) {
        m_embeds = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is<HTML::HTMLEmbedElement>(element);
        });
    }
    return *m_embeds;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-plugins
JS::NonnullGCPtr<HTMLCollection> Document::plugins()
{
    return embeds();
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-links
JS::NonnullGCPtr<HTMLCollection> Document::links()
{
    if (!m_links) {
        m_links = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return (is<HTML::HTMLAnchorElement>(element) || is<HTML::HTMLAreaElement>(element)) && element.has_attribute(HTML::AttributeNames::href);
        });
    }
    return *m_links;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-forms
JS::NonnullGCPtr<HTMLCollection> Document::forms()
{
    if (!m_forms) {
        m_forms = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is<HTML::HTMLFormElement>(element);
        });
    }
    return *m_forms;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-scripts
JS::NonnullGCPtr<HTMLCollection> Document::scripts()
{
    if (!m_scripts) {
        m_scripts = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is<HTML::HTMLScriptElement>(element);
        });
    }
    return *m_scripts;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-document-all
JS::NonnullGCPtr<HTMLCollection> Document::all()
{
    if (!m_all) {
        m_all = HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const&) {
            return true;
        });
    }
    return *m_all;
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
    return Bindings::host_defined_environment_settings_object(realm());
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate-to-a-javascript:-url
void Document::navigate_to_javascript_url(StringView url)
{
    // FIXME: Implement the rest of steps from the spec

    // 6. Let newDocument be the result of evaluating a javascript: URL given targetNavigable, url, and initiatorOrigin.
    evaluate_javascript_url(url);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#evaluate-a-javascript:-url
void Document::evaluate_javascript_url(StringView url)
{
    // NOTE: This is done by EventHandler::handle_mouseup
    // 1. Let urlString be the result of running the URL serializer on url.

    // 2. Let encodedScriptSource be the result of removing the leading "javascript:" from urlString.
    auto encoded_script_source = url.substring_view(11, url.length() - 11);

    // FIXME: 3. Let scriptSource be the UTF-8 decoding of the percent-decoding of encodedScriptSource.

    // 4. Let settings be targetNavigable's active document's relevant settings object.
    auto& settings = relevant_settings_object();

    // 5. Let baseURL be settings's API base URL.
    auto base_url = settings.api_base_url();

    // 6. Let script be the result of creating a classic script given scriptSource, settings, baseURL, and the default classic script fetch options.
    auto script = HTML::ClassicScript::create("(javascript url)", encoded_script_source, settings, base_url);

    // 7. Let evaluationStatus be the result of running the classic script script.
    static_cast<void>(script->run());

    // FIXME: Implement the rest of the steps from the spec
}

// https://dom.spec.whatwg.org/#dom-document-createelement
WebIDL::ExceptionOr<JS::NonnullGCPtr<Element>> Document::create_element(DeprecatedString const& a_local_name, Variant<DeprecatedString, ElementCreationOptions> const& options)
{
    auto& vm = this->vm();

    auto local_name = a_local_name;

    // 1. If localName does not match the Name production, then throw an "InvalidCharacterError" DOMException.
    if (!is_valid_name(local_name))
        return WebIDL::InvalidCharacterError::create(realm(), "Invalid character in tag name.");

    // 2. If this is an HTML document, then set localName to localName in ASCII lowercase.
    if (document_type() == Type::HTML)
        local_name = local_name.to_lowercase();

    // 3. Let is be null.
    Optional<String> is_value;

    // 4. If options is a dictionary and options["is"] exists, then set is to it.
    if (options.has<ElementCreationOptions>()) {
        auto const& element_creation_options = options.get<ElementCreationOptions>();
        if (!element_creation_options.is.is_null())
            is_value = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(element_creation_options.is));
    }

    // 5. Let namespace be the HTML namespace, if this is an HTML document or this’s content type is "application/xhtml+xml"; otherwise null.
    DeprecatedFlyString namespace_;
    if (document_type() == Type::HTML || content_type() == "application/xhtml+xml"sv)
        namespace_ = Namespace::HTML;

    // 6. Return the result of creating an element given this, localName, namespace, null, is, and with the synchronous custom elements flag set.
    return TRY(DOM::create_element(*this, local_name, namespace_, {}, move(is_value), true));
}

// https://dom.spec.whatwg.org/#dom-document-createelementns
// https://dom.spec.whatwg.org/#internal-createelementns-steps
WebIDL::ExceptionOr<JS::NonnullGCPtr<Element>> Document::create_element_ns(DeprecatedString const& namespace_, DeprecatedString const& qualified_name, Variant<DeprecatedString, ElementCreationOptions> const& options)
{
    auto& vm = this->vm();

    // 1. Let namespace, prefix, and localName be the result of passing namespace and qualifiedName to validate and extract.
    auto extracted_qualified_name = TRY(validate_and_extract(realm(), namespace_, qualified_name));

    // 2. Let is be null.
    Optional<String> is_value;

    // 3. If options is a dictionary and options["is"] exists, then set is to it.
    if (options.has<ElementCreationOptions>()) {
        auto const& element_creation_options = options.get<ElementCreationOptions>();
        if (!element_creation_options.is.is_null())
            is_value = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(element_creation_options.is));
    }

    // 4. Return the result of creating an element given document, localName, namespace, prefix, is, and with the synchronous custom elements flag set.
    return TRY(DOM::create_element(*this, extracted_qualified_name.local_name(), extracted_qualified_name.namespace_(), extracted_qualified_name.prefix(), move(is_value), true));
}

JS::NonnullGCPtr<DocumentFragment> Document::create_document_fragment()
{
    return heap().allocate<DocumentFragment>(realm(), *this);
}

JS::NonnullGCPtr<Text> Document::create_text_node(DeprecatedString const& data)
{
    return heap().allocate<Text>(realm(), *this, data);
}

JS::NonnullGCPtr<Comment> Document::create_comment(DeprecatedString const& data)
{
    return heap().allocate<Comment>(realm(), *this, data);
}

// https://dom.spec.whatwg.org/#dom-document-createprocessinginstruction
WebIDL::ExceptionOr<JS::NonnullGCPtr<ProcessingInstruction>> Document::create_processing_instruction(DeprecatedString const& target, DeprecatedString const& data)
{
    // FIXME: 1. If target does not match the Name production, then throw an "InvalidCharacterError" DOMException.

    // FIXME: 2. If data contains the string "?>", then throw an "InvalidCharacterError" DOMException.

    // 3. Return a new ProcessingInstruction node, with target set to target, data set to data, and node document set to this.
    return heap().allocate<ProcessingInstruction>(realm(), *this, data, target);
}

JS::NonnullGCPtr<Range> Document::create_range()
{
    return Range::create(*this);
}

// https://dom.spec.whatwg.org/#dom-document-createevent
WebIDL::ExceptionOr<JS::NonnullGCPtr<Event>> Document::create_event(DeprecatedString const& interface)
{
    auto& realm = this->realm();

    // NOTE: This is named event here, since we do step 5 and 6 as soon as possible for each case.
    // 1. Let constructor be null.
    JS::GCPtr<Event> event;

    // 2. If interface is an ASCII case-insensitive match for any of the strings in the first column in the following table,
    //      then set constructor to the interface in the second column on the same row as the matching string:
    if (Infra::is_ascii_case_insensitive_match(interface, "beforeunloadevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create BeforeUnloadEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "compositionevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create CompositionEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "customevent"sv)) {
        event = CustomEvent::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "devicemotionevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create DeviceMotionEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "deviceorientationevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create DeviceOrientationEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "dragevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create DragEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "event"sv)
        || Infra::is_ascii_case_insensitive_match(interface, "events"sv)) {
        event = Event::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "focusevent"sv)) {
        event = UIEvents::FocusEvent::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "hashchangeevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create HashChangeEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "htmlevents"sv)) {
        event = Event::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "keyboardevent"sv)) {
        event = UIEvents::KeyboardEvent::create(realm, String {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "messageevent"sv)) {
        event = HTML::MessageEvent::create(realm, String {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "mouseevent"sv)
        || Infra::is_ascii_case_insensitive_match(interface, "mouseevents"sv)) {
        event = UIEvents::MouseEvent::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "storageevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create StorageEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "svgevents"sv)) {
        event = Event::create(realm, FlyString {});
    } else if (Infra::is_ascii_case_insensitive_match(interface, "textevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create CompositionEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "touchevent"sv)) {
        event = Event::create(realm, FlyString {}); // FIXME: Create TouchEvent
    } else if (Infra::is_ascii_case_insensitive_match(interface, "uievent"sv)
        || Infra::is_ascii_case_insensitive_match(interface, "uievents"sv)) {
        event = UIEvents::UIEvent::create(realm, FlyString {});
    }

    // 3. If constructor is null, then throw a "NotSupportedError" DOMException.
    if (!event) {
        return WebIDL::NotSupportedError::create(realm, "No constructor for interface found");
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
    return JS::NonnullGCPtr(*event);
}

void Document::set_pending_parsing_blocking_script(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement* script)
{
    m_pending_parsing_blocking_script = script;
}

JS::NonnullGCPtr<HTML::HTMLScriptElement> Document::take_pending_parsing_blocking_script(Badge<HTML::HTMLParser>)
{
    VERIFY(m_pending_parsing_blocking_script);
    auto script = m_pending_parsing_blocking_script;
    m_pending_parsing_blocking_script = nullptr;
    return *script;
}

void Document::add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_when_parsing_has_finished.append(JS::make_handle(script));
}

Vector<JS::Handle<HTML::HTMLScriptElement>> Document::take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLParser>)
{
    return move(m_scripts_to_execute_when_parsing_has_finished);
}

void Document::add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_as_soon_as_possible.append(JS::make_handle(script));
}

Vector<JS::Handle<HTML::HTMLScriptElement>> Document::take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLParser>)
{
    return move(m_scripts_to_execute_as_soon_as_possible);
}

void Document::add_script_to_execute_in_order_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_in_order_as_soon_as_possible.append(JS::make_handle(script));
}

Vector<JS::Handle<HTML::HTMLScriptElement>> Document::take_scripts_to_execute_in_order_as_soon_as_possible(Badge<HTML::HTMLParser>)
{
    return move(m_scripts_to_execute_in_order_as_soon_as_possible);
}

// https://dom.spec.whatwg.org/#dom-document-importnode
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Document::import_node(JS::NonnullGCPtr<Node> node, bool deep)
{
    // 1. If node is a document or shadow root, then throw a "NotSupportedError" DOMException.
    if (is<Document>(*node) || is<ShadowRoot>(*node))
        return WebIDL::NotSupportedError::create(realm(), "Cannot import a document or shadow root.");

    // 2. Return a clone of node, with this and the clone children flag set if deep is true.
    return node->clone_node(this, deep);
}

// https://dom.spec.whatwg.org/#concept-node-adopt
void Document::adopt_node(Node& node)
{
    // 1. Let oldDocument be node’s node document.
    auto& old_document = node.document();

    // 2. If node’s parent is non-null, then remove node.
    if (node.parent())
        node.remove();

    // 3. If document is not oldDocument, then:
    if (&old_document != this) {
        // 1. For each inclusiveDescendant in node’s shadow-including inclusive descendants:
        node.for_each_shadow_including_inclusive_descendant([&](DOM::Node& inclusive_descendant) {
            // 1. Set inclusiveDescendant’s node document to document.
            inclusive_descendant.set_document({}, *this);

            // FIXME: 2. If inclusiveDescendant is an element, then set the node document of each attribute in inclusiveDescendant’s
            //           attribute list to document.
            return IterationDecision::Continue;
        });

        // 2. For each inclusiveDescendant in node’s shadow-including inclusive descendants that is custom,
        //    enqueue a custom element callback reaction with inclusiveDescendant, callback name "adoptedCallback",
        //    and an argument list containing oldDocument and document.
        node.for_each_shadow_including_inclusive_descendant([&](DOM::Node& inclusive_descendant) {
            if (!is<DOM::Element>(inclusive_descendant))
                return IterationDecision::Continue;

            auto& element = static_cast<DOM::Element&>(inclusive_descendant);
            if (element.is_custom()) {
                auto& vm = this->vm();

                JS::MarkedVector<JS::Value> arguments { vm.heap() };
                arguments.append(&old_document);
                arguments.append(this);

                element.enqueue_a_custom_element_callback_reaction(HTML::CustomElementReactionNames::adoptedCallback, move(arguments));
            }

            return IterationDecision::Continue;
        });

        // 3. For each inclusiveDescendant in node’s shadow-including inclusive descendants, in shadow-including tree order,
        //    run the adopting steps with inclusiveDescendant and oldDocument.
        node.for_each_shadow_including_inclusive_descendant([&](auto& inclusive_descendant) {
            inclusive_descendant.adopted_from(old_document);
            return IterationDecision::Continue;
        });

        // Transfer NodeIterators rooted at `node` from old_document to this document.
        Vector<NodeIterator&> node_iterators_to_transfer;
        for (auto node_iterator : old_document.m_node_iterators) {
            if (node_iterator->root().ptr() == &node)
                node_iterators_to_transfer.append(*node_iterator);
        }

        for (auto& node_iterator : node_iterators_to_transfer) {
            old_document.m_node_iterators.remove(&node_iterator);
            m_node_iterators.set(&node_iterator);
        }
    }
}

// https://dom.spec.whatwg.org/#dom-document-adoptnode
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Document::adopt_node_binding(JS::NonnullGCPtr<Node> node)
{
    if (is<Document>(*node))
        return WebIDL::NotSupportedError::create(realm(), "Cannot adopt a document into a document");

    if (is<ShadowRoot>(*node))
        return WebIDL::HierarchyRequestError::create(realm(), "Cannot adopt a shadow root into a document");

    if (is<DocumentFragment>(*node) && verify_cast<DocumentFragment>(*node).host())
        return node;

    adopt_node(*node);

    return node;
}

DocumentType const* Document::doctype() const
{
    return first_child_of_type<DocumentType>();
}

DeprecatedString const& Document::compat_mode() const
{
    static DeprecatedString back_compat = "BackCompat";
    static DeprecatedString css1_compat = "CSS1Compat";

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
    if (m_focused_element.ptr() == element)
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

    // Scroll the viewport if necessary to make the newly focused element visible.
    if (m_focused_element)
        (void)m_focused_element->scroll_into_view();
}

void Document::set_active_element(Element* element)
{
    if (m_active_element.ptr() == element)
        return;

    m_active_element = element;

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

void Document::set_target_element(Element* element)
{
    if (m_target_element.ptr() == element)
        return;

    if (m_target_element)
        m_target_element->set_needs_style_update(true);

    m_target_element = element;

    if (m_target_element)
        m_target_element->set_needs_style_update(true);

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#the-indicated-part-of-the-document
Document::IndicatedPart Document::determine_the_indicated_part() const
{
    // For an HTML document document, the following processing model must be followed to determine its indicated part:

    // 1. Let fragment be document's URL's fragment.
    VERIFY(url().fragment().has_value());

    auto fragment = url().fragment().value();

    // 2. If fragment is the empty string, then return the special value top of the document.
    if (fragment.is_empty())
        return Document::TopOfTheDocument {};

    // 3. Let potentialIndicatedElement be the result of finding a potential indicated element given document and fragment.
    auto* potential_indicated_element = find_a_potential_indicated_element(fragment.to_deprecated_string());

    // 4. If potentialIndicatedElement is not null, then return potentialIndicatedElement.
    if (potential_indicated_element)
        return potential_indicated_element;

    // 5. Let fragmentBytes be the result of percent-decoding fragment.
    // 6. Let decodedFragment be the result of running UTF-8 decode without BOM on fragmentBytes.
    auto decoded_fragment = AK::URL::percent_decode(fragment);

    // 7. Set potentialIndicatedElement to the result of finding a potential indicated element given document and decodedFragment.
    potential_indicated_element = find_a_potential_indicated_element(decoded_fragment);

    // 8. If potentialIndicatedElement is not null, then return potentialIndicatedElement.
    if (potential_indicated_element)
        return potential_indicated_element;

    // 9. If decodedFragment is an ASCII case-insensitive match for the string top, then return the top of the document.
    if (Infra::is_ascii_case_insensitive_match(decoded_fragment, "top"sv))
        return Document::TopOfTheDocument {};

    // 10. Return null.
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#find-a-potential-indicated-element
Element* Document::find_a_potential_indicated_element(DeprecatedString fragment) const
{
    // To find a potential indicated element given a Document document and a string fragment, run these steps:

    // 1. If there is an element in the document tree whose root is document and that has an ID equal to
    //    fragment, then return the first such element in tree order.
    if (auto element = get_element_by_id(fragment))
        return const_cast<Element*>(element.ptr());

    // 2. If there is an a element in the document tree whose root is document that has a name attribute
    //    whose value is equal to fragment, then return the first such element in tree order.
    Element* element_with_name;
    root().for_each_in_subtree_of_type<Element>([&](Element const& element) {
        if (element.name() == fragment) {
            element_with_name = const_cast<Element*>(&element);
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (element_with_name)
        return element_with_name;

    // 3. Return null.
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#scroll-to-the-fragment-identifier
void Document::scroll_to_the_fragment()
{
    // To scroll to the fragment given a Document document:

    // 1. If document's indicated part is null, then set document's target element to null.
    auto indicated_part = determine_the_indicated_part();
    if (indicated_part.has<Element*>() && indicated_part.get<Element*>() == nullptr) {
        set_target_element(nullptr);
    }

    // 2. Otherwise, if document's indicated part is top of the document, then:
    else if (indicated_part.has<TopOfTheDocument>()) {
        // 1. Set document's target element to null.
        set_target_element(nullptr);

        // 2. Scroll to the beginning of the document for document. [CSSOMVIEW]
        scroll_to_the_beginning_of_the_document();

        // 3. Return.
        return;
    }

    // 3. Otherwise:
    else {
        // 1. Assert: document's indicated part is an element.
        VERIFY(indicated_part.has<Element*>());

        // 2. Let target be document's indicated part.
        auto target = indicated_part.get<Element*>();

        // 3. Set document's target element to target.
        set_target_element(target);

        // FIXME: 4. Run the ancestor details revealing algorithm on target.

        // FIXME: 5. Run the ancestor hidden-until-found revealing algorithm on target.

        // 6. Scroll target into view, with behavior set to "auto", block set to "start", and inline set to "nearest". [CSSOMVIEW]
        // FIXME: Do this properly!
        (void)target->scroll_into_view();

        // 7. Run the focusing steps for target, with the Document's viewport as the fallback target.
        // FIXME: Pass the Document's viewport somehow.
        HTML::run_focusing_steps(target);

        // FIXME: 8. Move the sequential focus navigation starting point to target.
    }
}

// https://drafts.csswg.org/cssom-view-1/#scroll-to-the-beginning-of-the-document
void Document::scroll_to_the_beginning_of_the_document()
{
    // FIXME: Actually implement this algorithm
    if (auto browsing_context = this->browsing_context())
        browsing_context->scroll_to({ 0, 0 });
}

DeprecatedString Document::ready_state() const
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

// https://html.spec.whatwg.org/multipage/dom.html#update-the-current-document-readiness
void Document::update_readiness(HTML::DocumentReadyState readiness_value)
{
    // 1. If document's current document readiness equals readinessValue, then return.
    if (m_readiness == readiness_value)
        return;

    // 2. Set document's current document readiness to readinessValue.
    m_readiness = readiness_value;

    // 3. If document is associated with an HTML parser, then:
    if (m_parser) {
        // 1. Let now be the current high resolution time given document's relevant global object.
        auto now = HighResolutionTime::unsafe_shared_current_time();

        // 2. If readinessValue is "complete", and document's load timing info's DOM complete time is 0,
        //    then set document's load timing info's DOM complete time to now.
        if (readiness_value == HTML::DocumentReadyState::Complete && m_load_timing_info.dom_complete_time == 0) {
            m_load_timing_info.dom_complete_time = now;
        }
        // 3. Otherwise, if readinessValue is "interactive", and document's load timing info's DOM interactive time is 0,
        //    then set document's load timing info's DOM interactive time to now.
        else if (readiness_value == HTML::DocumentReadyState::Interactive && m_load_timing_info.dom_interactive_time == 0) {
            m_load_timing_info.dom_interactive_time = now;
        }
    }

    // 4. Fire an event named readystatechange at document.
    dispatch_event(Event::create(realm(), HTML::EventNames::readystatechange));
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

    return m_window;
}

// https://html.spec.whatwg.org/#completely-loaded
bool Document::is_completely_loaded() const
{
    return m_completely_loaded_time.has_value();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#completely-finish-loading
void Document::completely_finish_loading()
{
    // 1. Assert: document's browsing context is non-null.
    VERIFY(browsing_context());

    // 2. Set document's completely loaded time to the current time.
    m_completely_loaded_time = AK::UnixDateTime::now();

    // NOTE: See the end of shared_declarative_refresh_steps.
    if (m_active_refresh_timer)
        m_active_refresh_timer->start();

    // 3. Let container be document's browsing context's container.
    auto container = JS::make_handle(browsing_context()->container());

    // 4. If container is an iframe element, then queue an element task on the DOM manipulation task source given container to run the iframe load event steps given container.
    if (container && is<HTML::HTMLIFrameElement>(*container)) {
        container->queue_an_element_task(HTML::Task::Source::DOMManipulation, [container] {
            run_iframe_load_event_steps(static_cast<HTML::HTMLIFrameElement&>(*container));
        });
    }
    // 5. Otherwise, if container is non-null, then queue an element task on the DOM manipulation task source given container to fire an event named load at container.
    else if (container) {
        container->queue_an_element_task(HTML::Task::Source::DOMManipulation, [container] {
            container->dispatch_event(DOM::Event::create(container->realm(), HTML::EventNames::load));
        });
    }

    auto observers_to_notify = m_document_observers.values();
    for (auto& document_observer : observers_to_notify) {
        if (document_observer->document_completely_loaded)
            document_observer->document_completely_loaded();
    }
}

DeprecatedString Document::cookie(Cookie::Source source)
{
    if (auto* page = this->page())
        return page->client().page_did_request_cookie(m_url, source);
    return {};
}

void Document::set_cookie(DeprecatedString const& cookie_string, Cookie::Source source)
{
    auto cookie = Cookie::parse_cookie(cookie_string);
    if (!cookie.has_value())
        return;

    if (auto* page = this->page())
        page->client().page_did_set_cookie(m_url, cookie.value(), source);
}

DeprecatedString Document::dump_dom_tree_as_json() const
{
    StringBuilder builder;
    auto json = MUST(JsonObjectSerializer<>::try_create(builder));
    serialize_tree_as_json(json);

    MUST(json.finish());
    return builder.to_deprecated_string();
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

DeprecatedString Document::referrer() const
{
    return m_referrer;
}

void Document::set_referrer(DeprecatedString referrer)
{
    m_referrer = referrer;
}

// https://html.spec.whatwg.org/multipage/browsers.html#fully-active
bool Document::is_fully_active() const
{
    // A Document d is said to be fully active when d's browsing context is non-null, d's browsing context's active document is d,
    // and either d's browsing context is a top-level browsing context, or d's browsing context's container document is fully active.
    auto* browsing_context = this->browsing_context();
    if (!browsing_context)
        return false;
    if (browsing_context->active_document() != this)
        return false;
    if (browsing_context->is_top_level())
        return true;
    if (auto* navigable_container_document = browsing_context->container_document()) {
        if (navigable_container_document->is_fully_active())
            return true;
    }
    return false;
}

// https://html.spec.whatwg.org/multipage/browsers.html#active-document
bool Document::is_active() const
{
    // A browsing context's active document is its active window's associated Document.
    return browsing_context() && browsing_context()->active_document() == this;
}

// https://html.spec.whatwg.org/multipage/history.html#dom-document-location
JS::GCPtr<HTML::Location> Document::location()
{
    // The Document object's location attribute's getter must return this Document object's relevant global object's Location object,
    // if this Document object is fully active, and null otherwise.

    if (!is_fully_active())
        return nullptr;

    return window().location();
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-document-hidden
bool Document::hidden() const
{
    return visibility_state() == "hidden";
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-document-visibilitystate
DeprecatedString Document::visibility_state() const
{
    switch (m_visibility_state) {
    case HTML::VisibilityState::Hidden:
        return "hidden"sv;
    case HTML::VisibilityState::Visible:
        return "visible"sv;
    }
    VERIFY_NOT_REACHED();
}

void Document::set_visibility_state(Badge<HTML::BrowsingContext>, HTML::VisibilityState visibility_state)
{
    m_visibility_state = visibility_state;
}

// https://html.spec.whatwg.org/multipage/interaction.html#update-the-visibility-state
void Document::update_the_visibility_state(HTML::VisibilityState visibility_state)
{
    // 1. If document's visibility state equals visibilityState, then return.
    if (m_visibility_state == visibility_state)
        return;

    // 2. Set document's visibility state to visibilityState.
    m_visibility_state = visibility_state;

    // FIXME: 3. Run any page visibility change steps which may be defined in other specifications, with visibility state and document.

    // 4. Fire an event named visibilitychange at document, with its bubbles attribute initialized to true.
    auto event = DOM::Event::create(realm(), HTML::EventNames::visibilitychange);
    event->set_bubbles(true);
    dispatch_event(event);
}

// https://drafts.csswg.org/cssom-view/#run-the-resize-steps
void Document::run_the_resize_steps()
{
    // 1. If doc’s viewport has had its width or height changed
    //    (e.g. as a result of the user resizing the browser window, or changing the page zoom scale factor,
    //    or an iframe element’s dimensions are changed) since the last time these steps were run,
    //    fire an event named resize at the Window object associated with doc.

    auto viewport_size = viewport_rect().size().to_type<int>();
    if (m_last_viewport_size == viewport_size)
        return;
    m_last_viewport_size = viewport_size;

    window().dispatch_event(DOM::Event::create(realm(), UIEvents::EventNames::resize));

    schedule_layout_update();
}

// https://w3c.github.io/csswg-drafts/cssom-view-1/#document-run-the-scroll-steps
void Document::run_the_scroll_steps()
{
    // 1. For each item target in doc’s pending scroll event targets, in the order they were added to the list, run these substeps:
    for (auto& target : m_pending_scroll_event_targets) {
        // 1. If target is a Document, fire an event named scroll that bubbles at target and fire an event named scroll at the VisualViewport that is associated with target.
        if (is<Document>(*target)) {
            auto event = DOM::Event::create(realm(), HTML::EventNames::scroll);
            event->set_bubbles(true);
            target->dispatch_event(event);
            // FIXME: Fire at the associated VisualViewport
        }
        // 2. Otherwise, fire an event named scroll at target.
        else {
            auto event = DOM::Event::create(realm(), HTML::EventNames::scroll);
            target->dispatch_event(event);
        }
    }

    // 2. Empty doc’s pending scroll event targets.
    m_pending_scroll_event_targets.clear();
}

void Document::add_media_query_list(JS::NonnullGCPtr<CSS::MediaQueryList> media_query_list)
{
    m_media_query_lists.append(*media_query_list);
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
        JS::GCPtr<CSS::MediaQueryList> media_query_list = media_query_list_ptr.ptr();
        bool did_match = media_query_list->matches();
        bool now_matches = media_query_list->evaluate();

        if (did_match != now_matches) {
            CSS::MediaQueryListEventInit init;
            init.media = String::from_deprecated_string(media_query_list->media()).release_value_but_fixme_should_propagate_errors();
            init.matches = now_matches;
            auto event = CSS::MediaQueryListEvent::create(realm(), HTML::EventNames::change, init);
            event->set_is_trusted(true);
            media_query_list->dispatch_event(*event);
        }
    }

    // Also not in the spec, but this is as good a place as any to evaluate @media rules!
    evaluate_media_rules();
}

void Document::evaluate_media_rules()
{
    bool any_media_queries_changed_match_state = false;
    for (auto& style_sheet : style_sheets().sheets()) {
        if (style_sheet->evaluate_media_queries(window()))
            any_media_queries_changed_match_state = true;
    }

    if (any_media_queries_changed_match_state) {
        style_computer().invalidate_rule_cache();
        invalidate_style();
    }
}

DOMImplementation* Document::implementation()
{
    if (!m_implementation)
        m_implementation = DOMImplementation::create(*this);
    return m_implementation;
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

bool Document::is_valid_name(DeprecatedString const& name)
{
    auto code_points = Utf8View { name };
    auto it = code_points.begin();
    if (code_points.is_empty())
        return false;

    if (!is_valid_name_start_character(*it))
        return false;
    ++it;

    for (; it != code_points.end(); ++it) {
        if (!is_valid_name_character(*it))
            return false;
    }

    return true;
}

// https://dom.spec.whatwg.org/#validate
WebIDL::ExceptionOr<Document::PrefixAndTagName> Document::validate_qualified_name(JS::Realm& realm, DeprecatedString const& qualified_name)
{
    if (qualified_name.is_empty())
        return WebIDL::InvalidCharacterError::create(realm, "Empty string is not a valid qualified name.");

    Utf8View utf8view { qualified_name };
    if (!utf8view.validate())
        return WebIDL::InvalidCharacterError::create(realm, "Invalid qualified name.");

    Optional<size_t> colon_offset;

    bool at_start_of_name = true;

    for (auto it = utf8view.begin(); it != utf8view.end(); ++it) {
        auto code_point = *it;
        if (code_point == ':') {
            if (colon_offset.has_value())
                return WebIDL::InvalidCharacterError::create(realm, "More than one colon (:) in qualified name.");
            colon_offset = utf8view.byte_offset_of(it);
            at_start_of_name = true;
            continue;
        }
        if (at_start_of_name) {
            if (!is_valid_name_start_character(code_point))
                return WebIDL::InvalidCharacterError::create(realm, "Invalid start of qualified name.");
            at_start_of_name = false;
            continue;
        }
        if (!is_valid_name_character(code_point))
            return WebIDL::InvalidCharacterError::create(realm, "Invalid character in qualified name.");
    }

    if (!colon_offset.has_value())
        return Document::PrefixAndTagName {
            .prefix = {},
            .tag_name = qualified_name,
        };

    if (*colon_offset == 0)
        return WebIDL::InvalidCharacterError::create(realm, "Qualified name can't start with colon (:).");

    if (*colon_offset >= (qualified_name.length() - 1))
        return WebIDL::InvalidCharacterError::create(realm, "Qualified name can't end with colon (:).");

    return Document::PrefixAndTagName {
        .prefix = qualified_name.substring_view(0, *colon_offset),
        .tag_name = qualified_name.substring_view(*colon_offset + 1),
    };
}

// https://dom.spec.whatwg.org/#dom-document-createnodeiterator
JS::NonnullGCPtr<NodeIterator> Document::create_node_iterator(Node& root, unsigned what_to_show, JS::GCPtr<NodeFilter> filter)
{
    return NodeIterator::create(root, what_to_show, filter).release_value_but_fixme_should_propagate_errors();
}

// https://dom.spec.whatwg.org/#dom-document-createtreewalker
JS::NonnullGCPtr<TreeWalker> Document::create_tree_walker(Node& root, unsigned what_to_show, JS::GCPtr<NodeFilter> filter)
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

void Document::register_document_observer(Badge<DocumentObserver>, DocumentObserver& document_observer)
{
    auto result = m_document_observers.set(document_observer);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void Document::unregister_document_observer(Badge<DocumentObserver>, DocumentObserver& document_observer)
{
    bool was_removed = m_document_observers.remove(document_observer);
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
    if (auto* paintable_box = this->paintable_box())
        paintable_box->invalidate_stacking_context();
}

void Document::check_favicon_after_loading_link_resource()
{
    // https://html.spec.whatwg.org/multipage/links.html#rel-icon
    // NOTE: firefox also load favicons outside the head tag, which is against spec (see table 4.6.7)
    auto* head_element = head();
    if (!head_element)
        return;

    auto favicon_link_elements = HTMLCollection::create(*head_element, HTMLCollection::Scope::Descendants, [](Element const& element) {
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

        if (favicon_element == m_active_element.ptr())
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

void Document::set_window(HTML::Window& window)
{
    m_window = &window;
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#look-up-a-custom-element-definition
JS::GCPtr<HTML::CustomElementDefinition> Document::lookup_custom_element_definition(DeprecatedFlyString const& namespace_, DeprecatedFlyString const& local_name, Optional<String> const& is) const
{
    // 1. If namespace is not the HTML namespace, return null.
    if (namespace_ != Namespace::HTML)
        return nullptr;

    // 2. If document's browsing context is null, return null.
    if (!browsing_context())
        return nullptr;

    // 3. Let registry be document's relevant global object's CustomElementRegistry object.
    auto registry = window().custom_elements();

    // 4. If there is custom element definition in registry with name and local name both equal to localName, return that custom element definition.
    auto converted_local_name = String::from_deprecated_string(local_name).release_value_but_fixme_should_propagate_errors();
    auto maybe_definition = registry->get_definition_with_name_and_local_name(converted_local_name, converted_local_name);
    if (maybe_definition)
        return maybe_definition;

    // 5. If there is a custom element definition in registry with name equal to is and local name equal to localName, return that custom element definition.
    // 6. Return null.

    // NOTE: If `is` has no value, it can never match as custom element definitions always have a name and localName (i.e. not stored as Optional<String>)
    if (!is.has_value())
        return nullptr;

    return registry->get_definition_with_name_and_local_name(is.value(), converted_local_name);
}

CSS::StyleSheetList& Document::style_sheets()
{
    if (!m_style_sheets)
        m_style_sheets = CSS::StyleSheetList::create(*this);
    return *m_style_sheets;
}

CSS::StyleSheetList const& Document::style_sheets() const
{
    return const_cast<Document*>(this)->style_sheets();
}

JS::NonnullGCPtr<HTML::History> Document::history()
{
    if (!m_history)
        m_history = HTML::History::create(realm(), *this);
    return *m_history;
}

JS::NonnullGCPtr<HTML::History> Document::history() const
{
    return const_cast<Document*>(this)->history();
}

// https://html.spec.whatwg.org/multipage/origin.html#dom-document-domain
DeprecatedString Document::domain() const
{
    // 1. Let effectiveDomain be this's origin's effective domain.
    auto effective_domain = origin().effective_domain();

    // 2. If effectiveDomain is null, then return the empty string.
    if (!effective_domain.has_value())
        return DeprecatedString::empty();

    // 3. Return effectiveDomain, serialized.
    return URLParser::serialize_host(effective_domain.release_value()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

void Document::set_domain(DeprecatedString const& domain)
{
    dbgln("(STUBBED) Document::set_domain(domain='{}')", domain);
}

void Document::set_navigation_id(Optional<String> navigation_id)
{
    m_navigation_id = move(navigation_id);
}

Optional<String> Document::navigation_id() const
{
    return m_navigation_id;
}

HTML::SandboxingFlagSet Document::active_sandboxing_flag_set() const
{
    return m_active_sandboxing_flag_set;
}

HTML::PolicyContainer Document::policy_container() const
{
    return m_policy_container;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#descendant-navigables
Vector<JS::Handle<HTML::Navigable>> Document::descendant_navigables()
{
    // 1. Let navigables be new list.
    Vector<JS::Handle<HTML::Navigable>> navigables;

    // 2. Let navigableContainers be a list of all shadow-including descendants of document that are navigable containers, in shadow-including tree order.
    // 3. For each navigableContainer of navigableContainers:
    for_each_shadow_including_descendant([&](DOM::Node& node) {
        if (is<HTML::NavigableContainer>(node)) {
            auto& navigable_container = static_cast<HTML::NavigableContainer&>(node);
            // 1. If navigableContainer's content navigable is null, then continue.
            if (!navigable_container.content_navigable())
                return IterationDecision::Continue;

            // 2. Extend navigables with navigableContainer's content navigable's active document's inclusive descendant navigables.
            navigables.extend(navigable_container.content_navigable()->active_document()->inclusive_descendant_navigables());
        }
        return IterationDecision::Continue;
    });

    // 4. Return navigables.
    return navigables;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#inclusive-descendant-navigables
Vector<JS::Handle<HTML::Navigable>> Document::inclusive_descendant_navigables()
{
    // 1. Let navigables be « document's node navigable ».
    Vector<JS::Handle<HTML::Navigable>> navigables;
    navigables.append(*navigable());

    // 2. Extend navigables with document's descendant navigables.
    navigables.extend(descendant_navigables());

    // 3. Return navigables.
    return navigables;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#ancestor-navigables
Vector<JS::Handle<HTML::Navigable>> Document::ancestor_navigables()
{
    // 1. Let navigable be document's node navigable's parent.
    VERIFY(navigable());
    auto navigable = this->navigable()->parent();

    // 2. Let ancestors be an empty list.
    Vector<JS::Handle<HTML::Navigable>> ancestors;

    // 3. While navigable is not null:
    while (navigable) {
        // 1. Prepend navigable to ancestors.
        ancestors.prepend(*navigable);

        // 2. Set navigable to navigable's parent.
        navigable = navigable->parent();
    }

    // 4. Return ancestors.
    return ancestors;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#inclusive-ancestor-navigables
Vector<JS::Handle<HTML::Navigable>> Document::inclusive_ancestor_navigables()
{
    // 1. Let navigables be document's ancestor navigables.
    auto navigables = ancestor_navigables();

    // 2. Append document's node navigable to navigables.
    navigables.append(*navigable());

    // 3. Return navigables.
    return navigables;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#document-tree-child-navigables
Vector<JS::Handle<HTML::Navigable>> Document::document_tree_child_navigables()
{
    // 1. If document's node navigable is null, then return the empty list.
    if (!navigable())
        return {};

    // 2. Let navigables be new list.
    Vector<JS::Handle<HTML::Navigable>> navigables;

    // 3. Let navigableContainers be a list of all descendants of document that are navigable containers, in tree order.
    // 4. For each navigableContainer of navigableContainers:
    for_each_in_subtree_of_type<HTML::NavigableContainer>([&](HTML::NavigableContainer& navigable_container) {
        // 1. If navigableContainer's content navigable is null, then continue.
        if (!navigable_container.content_navigable())
            return IterationDecision::Continue;
        // 2. Append navigableContainer's content navigable to navigables.
        navigables.append(*navigable_container.content_navigable());
        return IterationDecision::Continue;
    });

    // 5. Return navigables.
    return navigables;
}

// https://html.spec.whatwg.org/multipage/browsers.html#list-of-the-descendant-browsing-contexts
Vector<JS::Handle<HTML::BrowsingContext>> Document::list_of_descendant_browsing_contexts() const
{
    // 1. Let list be an empty list.
    Vector<JS::Handle<HTML::BrowsingContext>> list;

    // 2. For each browsing context container container,
    //    whose nested browsing context is non-null and whose shadow-including root is d, in shadow-including tree order:

    // NOTE: We already store our browsing contexts in a tree structure, so we can simply collect all the descendants
    //       of this document's browsing context.
    if (browsing_context()) {
        browsing_context()->for_each_in_subtree([&](auto& context) {
            list.append(JS::make_handle(context));
            return IterationDecision::Continue;
        });
    }

    return list;
}

// https://html.spec.whatwg.org/multipage/window-object.html#discard-a-document
void Document::discard()
{
    // 1. Set document's salvageable state to false.
    m_salvageable = false;

    // FIXME: 2. Run any unloading document cleanup steps for document that are defined by this specification and other applicable specifications.

    // 3. Abort document.
    abort();

    // 4. Remove any tasks associated with document in any task source, without running those tasks.
    HTML::main_thread_event_loop().task_queue().remove_tasks_matching([this](auto& task) {
        return task.document() == this;
    });

    // 5. Discard all the child browsing contexts of document.
    if (browsing_context()) {
        browsing_context()->for_each_child([](HTML::BrowsingContext& child_browsing_context) {
            child_browsing_context.discard();
        });
    }

    // FIXME: 6. For each session history entry entry whose document is equal to document, set entry's document to null.

    // 7. Set document's browsing context to null.
    tear_down_layout_tree();
    m_browsing_context = nullptr;

    // FIXME: 8. Remove document from the owner set of each WorkerGlobalScope object whose set contains document.

    // FIXME: 9. For each workletGlobalScope in document's worklet global scopes, terminate workletGlobalScope.
}

// https://html.spec.whatwg.org/multipage/document-lifecycle.html#destroy-a-document
void Document::destroy()
{
    // 1. Destroy the active documents of each of document's descendant navigables.
    for (auto navigable : descendant_navigables()) {
        if (auto document = navigable->active_document())
            document->destroy();
    }

    // 2. Set document's salvageable state to false.
    m_salvageable = false;

    // FIXME: 3. Run any unloading document cleanup steps for document that are defined by this specification and other applicable specifications.

    // 4. Abort document.
    abort();

    // 5. Remove any tasks whose document is document from any task queue (without running those tasks).
    HTML::main_thread_event_loop().task_queue().remove_tasks_matching([this](auto& task) {
        return task.document() == this;
    });

    // 6. Set document's browsing context to null.
    m_browsing_context = nullptr;

    // 7. Set document's node navigable's active session history entry's document state's document to null.
    navigable()->active_session_history_entry()->document_state->set_document(nullptr);

    // FIXME: 8. Remove document from the owner set of each WorkerGlobalScope object whose set contains document.

    // FIXME: 9. For each workletGlobalScope in document's worklet global scopes, terminate workletGlobalScope.
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#abort-a-document
void Document::abort()
{
    // 1. Abort the active documents of every child browsing context.
    //    If this results in any of those Document objects having their salvageable state set to false,
    //    then set document's salvageable state to false also.
    if (browsing_context()) {
        browsing_context()->for_each_child([this](HTML::BrowsingContext& child_browsing_context) {
            if (auto* child_document = child_browsing_context.active_document()) {
                child_document->abort();
                if (!child_document->m_salvageable)
                    m_salvageable = false;
            }
        });
    }

    // FIXME: 2. Cancel any instances of the fetch algorithm in the context of document,
    //           discarding any tasks queued for them, and discarding any further data received from the network for them.
    //           If this resulted in any instances of the fetch algorithm being canceled
    //           or any queued tasks or any network data getting discarded,
    //           then set document's salvageable state to false.

    // 3. If document's navigation id is non-null, then:
    if (m_navigation_id.has_value()) {
        // 1. FIXME: Invoke WebDriver BiDi navigation aborted with document's browsing context,
        //           and new WebDriver BiDi navigation status whose whose id is document's navigation id,
        //           status is "canceled", and url is document's URL.

        // 2. Set document's navigation id to null.
        m_navigation_id = {};
    }

    // 4. If document has an active parser, then:
    if (auto parser = active_parser()) {
        // 1. Set document's active parser was aborted to true.
        m_active_parser_was_aborted = true;

        // 2. Abort that parser.
        parser->abort();

        // 3. Set document's salvageable state to false.
        m_salvageable = false;
    }
}

// https://html.spec.whatwg.org/multipage/dom.html#active-parser
JS::GCPtr<HTML::HTMLParser> Document::active_parser()
{
    if (!m_parser)
        return nullptr;

    if (m_parser->aborted() || m_parser->stopped())
        return nullptr;

    return m_parser;
}

void Document::set_browsing_context(HTML::BrowsingContext* browsing_context)
{
    m_browsing_context = browsing_context;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#unload-a-document
void Document::unload(bool recursive_flag, Optional<DocumentUnloadTimingInfo> unload_timing_info)
{
    // 1. Increase the event loop's termination nesting level by one.
    HTML::main_thread_event_loop().increment_termination_nesting_level();

    // 2. Increase document's unload counter by 1.
    m_unload_counter += 1;

    // 3. If the user agent does not intend to keep document alive in a session history entry
    //    (such that it can be reused later on history traversal), set document's salvageable state to false.
    // FIXME: If we want to implement fast back/forward cache, this has to change.
    m_salvageable = false;

    // 4. If document's page showing flag is true:
    if (m_page_showing) {
        // 1. Set document's page showing flag to false.
        m_page_showing = false;

        // 2. Fire a page transition event named pagehide at document's relevant global object with document's salvageable state.
        global_object().fire_a_page_transition_event(HTML::EventNames::pagehide, m_salvageable);

        // 3. Update the visibility state of newDocument to "hidden".
        update_the_visibility_state(HTML::VisibilityState::Hidden);
    }

    // 5. If unloadTimingInfo is not null,
    if (unload_timing_info.has_value()) {
        // then set unloadTimingInfo's unload event start time to the current high resolution time given newGlobal,
        // coarsened given document's relevant settings object's cross-origin isolated capability.
        unload_timing_info->unload_event_start_time = HighResolutionTime::coarsen_time(
            HighResolutionTime::unsafe_shared_current_time(),
            relevant_settings_object().cross_origin_isolated_capability() == HTML::CanUseCrossOriginIsolatedAPIs::Yes);
    }

    // 6. If document's salvageable state is false,
    if (!m_salvageable) {
        // then fire an event named unload at document's relevant global object, with legacy target override flag set.
        // FIXME: The legacy target override flag is currently set by a virtual override of dispatch_event()
        //        We should reorganize this so that the flag appears explicitly here instead.
        auto event = DOM::Event::create(realm(), HTML::EventNames::unload);
        global_object().dispatch_event(event);
    }

    // 7. If unloadTimingInfo is not null,
    if (unload_timing_info.has_value()) {
        // then set unloadTimingInfo's unload event end time to the current high resolution time given newGlobal,
        // coarsened given document's relevant settings object's cross-origin isolated capability.
        unload_timing_info->unload_event_end_time = HighResolutionTime::coarsen_time(
            HighResolutionTime::unsafe_shared_current_time(),
            relevant_settings_object().cross_origin_isolated_capability() == HTML::CanUseCrossOriginIsolatedAPIs::Yes);
    }

    // 8. Decrease the event loop's termination nesting level by one.
    HTML::main_thread_event_loop().decrement_termination_nesting_level();

    // FIXME: 9. Set document's suspension time to the current high resolution time given document's relevant global object.

    // FIXME: 10. Set document's suspended timer handles to the result of getting the keys for the map of active timers.

    // FIXME: 11. Run any unloading document cleanup steps for document that are defined by this specification and other applicable specifications.

    // 12. If the recursiveFlag is not set, then:
    if (!recursive_flag) {
        // 1. Let descendants be the list of the descendant browsing contexts of document.
        auto descendants = list_of_descendant_browsing_contexts();

        // 2. For each browsingContext in descendants:
        for (auto browsing_context : descendants) {
            JS::GCPtr<Document> active_document = browsing_context->active_document();
            if (!active_document)
                continue;

            // 1. Unload the active document of browsingContext with the recursiveFlag set.
            active_document->unload(true);

            // 2. If the salvageable state of the active document of browsingContext is false,
            //    then set the salvageable state of document to false also.
            if (!active_document->m_salvageable)
                m_salvageable = false;
        }

        // 3. If document's salvageable state is false, then discard document.
        if (!m_salvageable)
            discard();
    }

    // 13. Decrease document's unload counter by 1.
    m_unload_counter -= 1;
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#allowed-to-use
bool Document::is_allowed_to_use_feature(PolicyControlledFeature feature) const
{
    // 1. If document's browsing context is null, then return false.
    if (browsing_context() == nullptr)
        return false;

    // 2. If document is not fully active, then return false.
    if (!is_fully_active())
        return false;

    // 3. If the result of running is feature enabled in document for origin on feature, document, and document's origin
    //    is "Enabled", then return true.
    // FIXME: This is ad-hoc. Implement the Permissions Policy specification.
    switch (feature) {
    case PolicyControlledFeature::Autoplay:
        if (PermissionsPolicy::AutoplayAllowlist::the().is_allowed_for_origin(*this, origin()) == PermissionsPolicy::Decision::Enabled)
            return true;
        break;
    }

    // 4. Return false.
    return false;
}

void Document::did_stop_being_active_document_in_browsing_context(Badge<HTML::BrowsingContext>)
{
    tear_down_layout_tree();

    auto observers_to_notify = m_document_observers.values();
    for (auto& document_observer : observers_to_notify) {
        if (document_observer->document_became_inactive)
            document_observer->document_became_inactive();
    }
}

// https://w3c.github.io/editing/docs/execCommand/#querycommandsupported()
bool Document::query_command_supported(DeprecatedString const& command) const
{
    dbgln("(STUBBED) Document::query_command_supported(command='{}')", command);
    return false;
}

void Document::increment_throw_on_dynamic_markup_insertion_counter(Badge<HTML::HTMLParser>)
{
    ++m_throw_on_dynamic_markup_insertion_counter;
}

void Document::decrement_throw_on_dynamic_markup_insertion_counter(Badge<HTML::HTMLParser>)
{
    VERIFY(m_throw_on_dynamic_markup_insertion_counter);
    --m_throw_on_dynamic_markup_insertion_counter;
}

// https://html.spec.whatwg.org/multipage/scripting.html#appropriate-template-contents-owner-document
JS::NonnullGCPtr<DOM::Document> Document::appropriate_template_contents_owner_document()
{
    // 1. If doc is not a Document created by this algorithm, then:
    if (!created_for_appropriate_template_contents()) {
        // 1. If doc does not yet have an associated inert template document, then:
        if (!m_associated_inert_template_document) {
            // 1. Let new doc be a new Document (whose browsing context is null). This is "a Document created by this algorithm" for the purposes of the step above.
            auto new_document = HTML::HTMLDocument::create(realm());
            new_document->m_created_for_appropriate_template_contents = true;

            // 2. If doc is an HTML document, mark new doc as an HTML document also.
            if (document_type() == Type::HTML)
                new_document->set_document_type(Type::HTML);

            // 3. Let doc's associated inert template document be new doc.
            m_associated_inert_template_document = new_document;
        }
        // 2. Set doc to doc's associated inert template document.
        return *m_associated_inert_template_document;
    }
    // 2. Return doc.
    return *this;
}

DeprecatedString Document::dump_accessibility_tree_as_json()
{
    StringBuilder builder;
    auto accessibility_tree = AccessibilityTreeNode::create(this, nullptr);
    build_accessibility_tree(*&accessibility_tree);
    auto json = MUST(JsonObjectSerializer<>::try_create(builder));

    // Empty document
    if (!accessibility_tree->value()) {
        MUST(json.add("type"sv, "element"sv));
        MUST(json.add("role"sv, "document"sv));
    } else {
        accessibility_tree->serialize_tree_as_json(json, *this);
    }

    MUST(json.finish());
    return builder.to_deprecated_string();
}

// https://dom.spec.whatwg.org/#dom-document-createattribute
WebIDL::ExceptionOr<JS::NonnullGCPtr<Attr>> Document::create_attribute(DeprecatedString const& local_name)
{
    // 1. If localName does not match the Name production in XML, then throw an "InvalidCharacterError" DOMException.
    if (!is_valid_name(local_name))
        return WebIDL::InvalidCharacterError::create(realm(), "Invalid character in attribute name.");

    // 2. If this is an HTML document, then set localName to localName in ASCII lowercase.
    // 3. Return a new attribute whose local name is localName and node document is this.
    return Attr::create(*this, is_html_document() ? local_name.to_lowercase() : local_name);
}

// https://dom.spec.whatwg.org/#dom-document-createattributens
WebIDL::ExceptionOr<JS::NonnullGCPtr<Attr>> Document::create_attribute_ns(DeprecatedString const& namespace_, DeprecatedString const& qualified_name)
{
    // 1. Let namespace, prefix, and localName be the result of passing namespace and qualifiedName to validate and extract.
    auto extracted_qualified_name = TRY(validate_and_extract(realm(), namespace_, qualified_name));

    // 2. Return a new attribute whose namespace is namespace, namespace prefix is prefix, local name is localName, and node document is this.

    return Attr::create(*this, extracted_qualified_name);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#make-active
void Document::make_active()
{
    // 1. Let window be document's relevant global object.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));

    // 2. Set document's browsing context's WindowProxy's [[Window]] internal slot value to window.
    m_browsing_context->window_proxy()->set_window(window);

    // 3. Set document's visibility state to document's node navigable's traversable navigable's system visibility state.
    if (navigable()) {
        m_visibility_state = navigable()->traversable_navigable()->system_visibility_state();
    }

    // 4. Set window's relevant settings object's execution ready flag.
    HTML::relevant_settings_object(window).execution_ready = true;
}

HTML::ListOfAvailableImages& Document::list_of_available_images()
{
    return *m_list_of_available_images;
}

HTML::ListOfAvailableImages const& Document::list_of_available_images() const
{
    return *m_list_of_available_images;
}

CSSPixelRect Document::viewport_rect() const
{
    if (auto* browsing_context = this->browsing_context())
        return browsing_context->viewport_rect();
    return CSSPixelRect {};
}

JS::NonnullGCPtr<CSS::VisualViewport> Document::visual_viewport()
{
    if (!m_visual_viewport)
        m_visual_viewport = CSS::VisualViewport::create(*this);
    return *m_visual_viewport;
}

void Document::register_viewport_client(ViewportClient& client)
{
    auto result = m_viewport_clients.set(&client);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void Document::unregister_viewport_client(ViewportClient& client)
{
    bool was_removed = m_viewport_clients.remove(&client);
    VERIFY(was_removed);
}

void Document::inform_all_viewport_clients_about_the_current_viewport_rect()
{
    for (auto* client : m_viewport_clients)
        client->did_set_viewport_rect(viewport_rect());
}

void Document::register_intersection_observer(Badge<IntersectionObserver::IntersectionObserver>, IntersectionObserver::IntersectionObserver& observer)
{
    auto result = m_intersection_observers.set(observer);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void Document::unregister_intersection_observer(Badge<IntersectionObserver::IntersectionObserver>, IntersectionObserver::IntersectionObserver& observer)
{
    bool was_removed = m_intersection_observers.remove(observer);
    VERIFY(was_removed);
}

// https://www.w3.org/TR/intersection-observer/#queue-an-intersection-observer-task
void Document::queue_intersection_observer_task()
{
    // 1. If document’s IntersectionObserverTaskQueued flag is set to true, return.
    if (m_intersection_observer_task_queued)
        return;

    // 2. Set document’s IntersectionObserverTaskQueued flag to true.
    m_intersection_observer_task_queued = true;

    // 3. Queue a task on the IntersectionObserver task source associated with the document's event loop to notify intersection observers.
    HTML::queue_global_task(HTML::Task::Source::IntersectionObserver, window(), [this]() {
        auto& realm = this->realm();

        // https://www.w3.org/TR/intersection-observer/#notify-intersection-observers
        // 1. Set document’s IntersectionObserverTaskQueued flag to false.
        m_intersection_observer_task_queued = false;

        // 2. Let notify list be a list of all IntersectionObservers whose root is in the DOM tree of document.
        Vector<JS::Handle<IntersectionObserver::IntersectionObserver>> notify_list;
        notify_list.try_ensure_capacity(m_intersection_observers.size()).release_value_but_fixme_should_propagate_errors();
        for (auto& observer : m_intersection_observers) {
            notify_list.append(JS::make_handle(observer));
        }

        // 3. For each IntersectionObserver object observer in notify list, run these steps:
        for (auto& observer : notify_list) {
            // 2. Let queue be a copy of observer’s internal [[QueuedEntries]] slot.
            // 3. Clear observer’s internal [[QueuedEntries]] slot.
            auto queue = observer->take_records();

            // 1. If observer’s internal [[QueuedEntries]] slot is empty, continue.
            if (queue.is_empty())
                continue;

            auto wrapped_queue = MUST(JS::Array::create(realm, 0));
            for (size_t i = 0; i < queue.size(); ++i) {
                auto& record = queue.at(i);
                auto property_index = JS::PropertyKey { i };
                MUST(wrapped_queue->create_data_property(property_index, record.ptr()));
            }

            // 4. Let callback be the value of observer’s internal [[callback]] slot.
            auto& callback = observer->callback();

            // 5. Invoke callback with queue as the first argument, observer as the second argument, and observer as the callback this value. If this throws an exception, report the exception.
            auto completion = WebIDL::invoke_callback(callback, observer.ptr(), wrapped_queue, observer.ptr());
            if (completion.is_abrupt())
                HTML::report_exception(completion, realm);
        }
    });
}

// https://www.w3.org/TR/intersection-observer/#queue-an-intersectionobserverentry
void Document::queue_an_intersection_observer_entry(IntersectionObserver::IntersectionObserver& observer, HighResolutionTime::DOMHighResTimeStamp time, JS::NonnullGCPtr<Geometry::DOMRectReadOnly> root_bounds, JS::NonnullGCPtr<Geometry::DOMRectReadOnly> bounding_client_rect, JS::NonnullGCPtr<Geometry::DOMRectReadOnly> intersection_rect, bool is_intersecting, double intersection_ratio, JS::NonnullGCPtr<Element> target)
{
    auto& realm = this->realm();

    // 1. Construct an IntersectionObserverEntry, passing in time, rootBounds, boundingClientRect, intersectionRect, isIntersecting, and target.
    auto entry = realm.heap().allocate<IntersectionObserver::IntersectionObserverEntry>(realm, realm, time, root_bounds, bounding_client_rect, intersection_rect, is_intersecting, intersection_ratio, target);

    // 2. Append it to observer’s internal [[QueuedEntries]] slot.
    observer.queue_entry({}, entry);

    // 3. Queue an intersection observer task for document.
    queue_intersection_observer_task();
}

// https://www.w3.org/TR/intersection-observer/#compute-the-intersection
static JS::NonnullGCPtr<Geometry::DOMRectReadOnly> compute_intersection(JS::NonnullGCPtr<Element> target, IntersectionObserver::IntersectionObserver const& observer)
{
    // 1. Let intersectionRect be the result of getting the bounding box for target.
    auto intersection_rect = target->get_bounding_client_rect();

    // FIXME: 2. Let container be the containing block of target.
    // FIXME: 3. While container is not root:
    // FIXME:   1. If container is the document of a nested browsing context, update intersectionRect by clipping to
    //             the viewport of the document, and update container to be the browsing context container of container.
    // FIXME:   2. Map intersectionRect to the coordinate space of container.
    // FIXME:   3. If container has a content clip or a css clip-path property, update intersectionRect by applying
    //             container’s clip.
    // FIXME:   4. If container is the root element of a browsing context, update container to be the browsing context’s
    //             document; otherwise, update container to be the containing block of container.
    // FIXME: 4. Map intersectionRect to the coordinate space of root.

    // 5. Update intersectionRect by intersecting it with the root intersection rectangle.
    // FIXME: Pass in target so we can properly apply rootMargin.
    auto root_intersection_rectangle = observer.root_intersection_rectangle();
    CSSPixelRect intersection_rect_as_pixel_rect(intersection_rect->x(), intersection_rect->y(), intersection_rect->width(), intersection_rect->height());
    intersection_rect_as_pixel_rect.intersect(root_intersection_rectangle);
    intersection_rect->set_x(static_cast<double>(intersection_rect_as_pixel_rect.x()));
    intersection_rect->set_y(static_cast<double>(intersection_rect_as_pixel_rect.y()));
    intersection_rect->set_width(static_cast<double>(intersection_rect_as_pixel_rect.width()));
    intersection_rect->set_height(static_cast<double>(intersection_rect_as_pixel_rect.height()));

    // FIXME: 6. Map intersectionRect to the coordinate space of the viewport of the document containing target.

    // 7. Return intersectionRect.
    return intersection_rect;
}

// https://www.w3.org/TR/intersection-observer/#run-the-update-intersection-observations-steps
void Document::run_the_update_intersection_observations_steps(HighResolutionTime::DOMHighResTimeStamp time)
{
    auto& realm = this->realm();

    // 1. Let observer list be a list of all IntersectionObservers whose root is in the DOM tree of document.
    //    For the top-level browsing context, this includes implicit root observers.
    // 2. For each observer in observer list:
    for (auto& observer : m_intersection_observers) {
        // 1. Let rootBounds be observer’s root intersection rectangle.
        auto root_bounds = observer->root_intersection_rectangle();

        // 2. For each target in observer’s internal [[ObservationTargets]] slot, processed in the same order that
        //    observe() was called on each target:
        for (auto& target : observer->observation_targets()) {
            // 1. Let:
            // thresholdIndex be 0.
            size_t threshold_index = 0;

            // isIntersecting be false.
            bool is_intersecting = false;

            // targetRect be a DOMRectReadOnly with x, y, width, and height set to 0.
            auto target_rect = Geometry::DOMRectReadOnly::construct_impl(realm, 0, 0, 0, 0).release_value_but_fixme_should_propagate_errors();

            // intersectionRect be a DOMRectReadOnly with x, y, width, and height set to 0.
            auto intersection_rect = Geometry::DOMRectReadOnly::construct_impl(realm, 0, 0, 0, 0).release_value_but_fixme_should_propagate_errors();

            // SPEC ISSUE: It doesn't pass in intersection ratio to "queue an IntersectionObserverEntry" despite needing it.
            //             This is default 0, as isIntersecting is default false, see step 9.
            double intersection_ratio = 0.0;

            // 2. If the intersection root is not the implicit root, and target is not in the same document as the intersection root, skip to step 11.
            // 3. If the intersection root is an Element, and target is not a descendant of the intersection root in the containing block chain, skip to step 11.
            // FIXME: Actually use the containing block chain.
            auto intersection_root = observer->intersection_root();
            auto intersection_root_document = intersection_root.visit([](auto& node) -> JS::NonnullGCPtr<Document> {
                return node->document();
            });
            if (!(observer->root().has<Empty>() && &target->document() == intersection_root_document.ptr())
                || !(intersection_root.has<JS::Handle<DOM::Element>>() && !target->is_descendant_of(*intersection_root.get<JS::Handle<DOM::Element>>()))) {
                // 4. Set targetRect to the DOMRectReadOnly obtained by getting the bounding box for target.
                target_rect = target->get_bounding_client_rect();

                // 5. Let intersectionRect be the result of running the compute the intersection algorithm on target and
                //    observer’s intersection root.
                intersection_rect = compute_intersection(target, observer);

                // 6. Let targetArea be targetRect’s area.
                auto target_area = target_rect->width() * target_rect->height();

                // 7. Let intersectionArea be intersectionRect’s area.
                auto intersection_area = intersection_rect->width() * intersection_rect->height();

                // 8. Let isIntersecting be true if targetRect and rootBounds intersect or are edge-adjacent, even if the
                //    intersection has zero area (because rootBounds or targetRect have zero area).
                CSSPixelRect target_rect_as_pixel_rect(target_rect->x(), target_rect->y(), target_rect->width(), target_rect->height());
                is_intersecting = target_rect_as_pixel_rect.intersects(root_bounds);

                // 9. If targetArea is non-zero, let intersectionRatio be intersectionArea divided by targetArea.
                //    Otherwise, let intersectionRatio be 1 if isIntersecting is true, or 0 if isIntersecting is false.
                if (target_area != 0.0)
                    intersection_ratio = intersection_area / target_area;
                else
                    intersection_ratio = is_intersecting ? 1.0 : 0.0;

                // 10. Set thresholdIndex to the index of the first entry in observer.thresholds whose value is greater
                //     than intersectionRatio, or the length of observer.thresholds if intersectionRatio is greater than
                //     or equal to the last entry in observer.thresholds.
                threshold_index = observer->thresholds().find_first_index_if([&intersection_ratio](double threshold_value) {
                                                            return threshold_value > intersection_ratio;
                                                        })
                                      .value_or(observer->thresholds().size());
            }

            // 11. Let intersectionObserverRegistration be the IntersectionObserverRegistration record in target’s
            //     internal [[RegisteredIntersectionObservers]] slot whose observer property is equal to observer.
            auto& intersection_observer_registration = target->get_intersection_observer_registration({}, observer);

            // 12. Let previousThresholdIndex be the intersectionObserverRegistration’s previousThresholdIndex property.
            auto previous_threshold_index = intersection_observer_registration.previous_threshold_index;

            // 13. Let previousIsIntersecting be the intersectionObserverRegistration’s previousIsIntersecting property.
            auto previous_is_intersecting = intersection_observer_registration.previous_is_intersecting;

            // 14. If thresholdIndex does not equal previousThresholdIndex or if isIntersecting does not equal
            //     previousIsIntersecting, queue an IntersectionObserverEntry, passing in observer, time,
            //     rootBounds, targetRect, intersectionRect, isIntersecting, and target.
            if (threshold_index != previous_threshold_index || is_intersecting != previous_is_intersecting) {
                auto root_bounds_as_dom_rect = Geometry::DOMRectReadOnly::construct_impl(realm, static_cast<double>(root_bounds.x()), static_cast<double>(root_bounds.y()), static_cast<double>(root_bounds.width()), static_cast<double>(root_bounds.height())).release_value_but_fixme_should_propagate_errors();

                // SPEC ISSUE: It doesn't pass in intersectionRatio, but it's required.
                queue_an_intersection_observer_entry(observer, time, root_bounds_as_dom_rect, target_rect, intersection_rect, is_intersecting, intersection_ratio, target);
            }

            // 15. Assign thresholdIndex to intersectionObserverRegistration’s previousThresholdIndex property.
            intersection_observer_registration.previous_threshold_index = threshold_index;

            // 16. Assign isIntersecting to intersectionObserverRegistration’s previousIsIntersecting property.
            intersection_observer_registration.previous_is_intersecting = is_intersecting;
        }
    }
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#start-intersection-observing-a-lazy-loading-element
void Document::start_intersection_observing_a_lazy_loading_element(Element& element)
{
    auto& realm = this->realm();

    // 1. Let doc be element's node document.
    VERIFY(&element.document() == this);

    // 2. If doc's lazy load intersection observer is null, set it to a new IntersectionObserver instance, initialized as follows:
    if (!m_lazy_load_intersection_observer) {
        // - The callback is these steps, with arguments entries and observer:
        auto callback = JS::NativeFunction::create(realm, "", [this](JS::VM& vm) -> JS::ThrowCompletionOr<JS::Value> {
            // For each entry in entries using a method of iteration which does not trigger developer-modifiable array accessors or iteration hooks:
            auto& entries = verify_cast<JS::Array>(vm.argument(0).as_object());
            auto entries_length = MUST(MUST(entries.get(vm.names.length)).to_length(vm));

            for (size_t i = 0; i < entries_length; ++i) {
                auto property_key = JS::PropertyKey { i };
                auto& entry = verify_cast<IntersectionObserver::IntersectionObserverEntry>(entries.get_without_side_effects(property_key).as_object());

                // 1. Let resumptionSteps be null.
                JS::SafeFunction<void()> resumption_steps;

                // 2. If entry.isIntersecting is true, then set resumptionSteps to entry.target's lazy load resumption steps.
                if (entry.is_intersecting()) {
                    // 5. Set entry.target's lazy load resumption steps to null.
                    resumption_steps = verify_cast<HTML::HTMLImageElement>(*entry.target()).take_lazy_load_resumption_steps({});
                }

                // 3. If resumptionSteps is null, then return.
                if (!resumption_steps)
                    return JS::js_undefined();

                // 4. Stop intersection-observing a lazy loading element for entry.target.
                // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#stop-intersection-observing-a-lazy-loading-element
                // 1. Let doc be element's node document.
                // NOTE: It's `this`.

                // 2. Assert: doc's lazy load intersection observer is not null.
                VERIFY(m_lazy_load_intersection_observer);

                // 3. Call doc's lazy load intersection observer unobserve method with element as the argument.
                m_lazy_load_intersection_observer->unobserve(entry.target());

                // 6. Invoke resumptionSteps.
                resumption_steps();
            }

            return JS::js_undefined();
        });

        // FIXME: The options is an IntersectionObserverInit dictionary with the following dictionary members: «[ "rootMargin" → lazy load root margin ]»
        // Spec Note: This allows for fetching the image during scrolling, when it does not yet — but is about to — intersect the viewport.
        auto options = IntersectionObserver::IntersectionObserverInit {};

        auto wrapped_callback = realm.heap().allocate_without_realm<WebIDL::CallbackType>(callback, Bindings::host_defined_environment_settings_object(realm));
        m_lazy_load_intersection_observer = IntersectionObserver::IntersectionObserver::construct_impl(realm, wrapped_callback, options).release_value_but_fixme_should_propagate_errors();
    }

    // 3. Call doc's lazy load intersection observer's observe method with element as the argument.
    VERIFY(m_lazy_load_intersection_observer);
    m_lazy_load_intersection_observer->observe(element);
}

// https://html.spec.whatwg.org/multipage/semantics.html#shared-declarative-refresh-steps
void Document::shared_declarative_refresh_steps(StringView input, JS::GCPtr<HTML::HTMLMetaElement const> meta_element)
{
    // 1. If document's will declaratively refresh is true, then return.
    if (m_will_declaratively_refresh)
        return;

    // 2. Let position point at the first code point of input.
    GenericLexer lexer(input);

    // 3. Skip ASCII whitespace within input given position.
    lexer.ignore_while(Infra::is_ascii_whitespace);

    // 4. Let time be 0.
    u32 time = 0;

    // 5. Collect a sequence of code points that are ASCII digits from input given position, and let the result be timeString.
    auto time_string = lexer.consume_while(is_ascii_digit);

    // 6. If timeString is the empty string, then:
    if (time_string.is_empty()) {
        // 1. If the code point in input pointed to by position is not U+002E (.), then return.
        if (lexer.peek() != '.')
            return;
    }

    // 7. Otherwise, set time to the result of parsing timeString using the rules for parsing non-negative integers.
    // FIXME: Not sure if this exactly matches the spec's "rules for parsing non-negative integers".
    auto maybe_time = time_string.to_uint<u32>();

    // FIXME: Since we only collected ASCII digits, this can only fail because of overflow. What do we do when that happens? For now, default to 0.
    if (maybe_time.has_value() && maybe_time.value() < NumericLimits<int>::max() && !Checked<int>::multiplication_would_overflow(static_cast<int>(maybe_time.value()), 1000)) {
        time = maybe_time.value();
    }

    // 8. Collect a sequence of code points that are ASCII digits and U+002E FULL STOP characters (.) from input given
    //    position. Ignore any collected characters.
    lexer.ignore_while([](auto c) {
        return is_ascii_digit(c) || c == '.';
    });

    // 9. Let urlRecord be document's URL.
    auto url_record = url();

    // 10. If position is not past the end of input, then:
    if (!lexer.is_eof()) {
        // 1. If the code point in input pointed to by position is not U+003B (;), U+002C (,), or ASCII whitespace, then return.
        if (lexer.peek() != ';' && lexer.peek() != ',' && !Infra::is_ascii_whitespace(lexer.peek()))
            return;

        // 2. Skip ASCII whitespace within input given position.
        lexer.ignore_while(Infra::is_ascii_whitespace);

        // 3. If the code point in input pointed to by position is U+003B (;) or U+002C (,), then advance position to the next code point.
        if (lexer.peek() == ';' || lexer.peek() == ',')
            lexer.ignore(1);

        // 4. Skip ASCII whitespace within input given position.
        lexer.ignore_while(Infra::is_ascii_whitespace);
    }

    // 11. If position is not past the end of input, then:
    if (!lexer.is_eof()) {
        // 1. Let urlString be the substring of input from the code point at position to the end of the string.
        auto url_string = lexer.remaining();

        // 2. If the code point in input pointed to by position is U+0055 (U) or U+0075 (u), then advance position to the next code point. Otherwise, jump to the step labeled skip quotes.
        if (lexer.peek() == 'U' || lexer.peek() == 'u')
            lexer.ignore(1);
        else
            goto skip_quotes;

        // 3. If the code point in input pointed to by position is U+0052 (R) or U+0072 (r), then advance position to the next code point. Otherwise, jump to the step labeled parse.
        if (lexer.peek() == 'R' || lexer.peek() == 'r')
            lexer.ignore(1);
        else
            goto parse;

        // 4. If the code point in input pointed to by position is U+004C (L) or U+006C (l), then advance position to the next code point. Otherwise, jump to the step labeled parse.
        if (lexer.peek() == 'L' || lexer.peek() == 'l')
            lexer.ignore(1);
        else
            goto parse;

        // 5. Skip ASCII whitespace within input given position.
        lexer.ignore_while(Infra::is_ascii_whitespace);

        // 6. If the code point in input pointed to by position is U+003D (=), then advance position to the next code point. Otherwise, jump to the step labeled parse.
        if (lexer.peek() == '=')
            lexer.ignore(1);
        else
            goto parse;

        // 7. Skip ASCII whitespace within input given position.
        lexer.ignore_while(Infra::is_ascii_whitespace);

    skip_quotes : {
        // 8. Skip quotes: If the code point in input pointed to by position is U+0027 (') or U+0022 ("), then let
        //    quote be that code point, and advance position to the next code point. Otherwise, let quote be the empty
        //    string.
        Optional<char> quote;
        if (lexer.peek() == '\'' || lexer.peek() == '"')
            quote = lexer.consume();

        // 9. Set urlString to the substring of input from the code point at position to the end of the string.
        // 10. If quote is not the empty string, and there is a code point in urlString equal to quote, then truncate
        //     urlString at that code point, so that it and all subsequent code points are removed.
        url_string = lexer.consume_while([&quote](auto c) {
            return !quote.has_value() || c != quote.value();
        });
    }

    parse:
        // 11. Parse: Parse urlString relative to document. If that fails, return. Otherwise, set urlRecord to the
        //     resulting URL record.
        auto maybe_url_record = parse_url(url_string);
        if (!maybe_url_record.is_valid())
            return;

        url_record = maybe_url_record;
    }

    // 12. Set document's will declaratively refresh to true.
    m_will_declaratively_refresh = true;

    // 13. Perform one or more of the following steps:
    // - After the refresh has come due (as defined below), if the user has not canceled the redirect and, if meta is
    //   given, document's active sandboxing flag set does not have the sandboxed automatic features browsing context
    //   flag set, then navigate document's node navigable to urlRecord using document, with historyHandling set to
    //   "replace".
    m_active_refresh_timer = Core::Timer::create_single_shot(time * 1000, [this, has_meta_element = !!meta_element, url_record = move(url_record)]() {
        if (has_meta_element && active_sandboxing_flag_set().flags & HTML::SandboxingFlagSet::SandboxedAutomaticFeatures)
            return;

        // FIXME: Use navigables when they're used for all navigation (otherwise, navigable() would be null in some cases)
        VERIFY(browsing_context());
        auto request = Fetch::Infrastructure::Request::create(vm());
        request->set_url(url_record);
        MUST(browsing_context()->navigate(request, *browsing_context(), false, HTML::HistoryHandlingBehavior::Replace));
    }).release_value_but_fixme_should_propagate_errors();

    // For the purposes of the previous paragraph, a refresh is said to have come due as soon as the later of the
    // following two conditions occurs:

    // - At least time seconds have elapsed since document's completely loaded time, adjusted to take into
    //   account user or user agent preferences.
    // m_active_refresh_timer is started in completely_finished_loading after setting the completely loaded time.

    // - If meta is given, at least time seconds have elapsed since meta was inserted into the document document,
    // adjusted to take into account user or user agent preferences.
    // NOTE: This is only done if completely loaded time has a value because shared_declarative_refresh_steps is called
    // by HTMLMetaElement::inserted and if the document hasn't finished loading when the meta element was inserted,
    // then the document completely finishing loading will _always_ come after inserting the meta element.
    if (meta_element && m_completely_loaded_time.has_value()) {
        m_active_refresh_timer->start();
    }
}

Painting::ViewportPaintable const* Document::paintable() const
{
    return static_cast<Painting::ViewportPaintable const*>(Node::paintable());
}

Painting::ViewportPaintable* Document::paintable()
{
    return static_cast<Painting::ViewportPaintable*>(Node::paintable());
}

}
