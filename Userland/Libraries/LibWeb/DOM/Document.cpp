/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/SVG/TagNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/FocusEvent.h>
#include <LibWeb/UIEvents/KeyboardEvent.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::DOM {

Document::Document(const AK::URL& url)
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_computer(make<CSS::StyleComputer>(*this))
    , m_style_sheets(CSS::StyleSheetList::create(*this))
    , m_url(url)
    , m_window(Window::create_with_document(*this))
    , m_implementation(DOMImplementation::create({}, *this))
    , m_history(HTML::History::create(*this))
{
    HTML::main_thread_event_loop().register_document({}, *this);

    m_style_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_style();
    });

    m_layout_update_timer = Core::Timer::create_single_shot(0, [this] {
        force_layout();
    });
}

Document::~Document()
{
}

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
                if (node.parent())
                    node.remove();
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

Origin Document::origin() const
{
    if (!m_url.is_valid())
        return {};
    return { m_url.protocol(), m_url.host(), m_url.port_or_default() };
}

void Document::set_origin(const Origin& origin)
{
    m_url.set_protocol(origin.protocol());
    m_url.set_host(origin.host());
    m_url.set_port(origin.port());
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

bool Document::is_child_allowed(const Node& node) const
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

const Element* Document::document_element() const
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
        auto replace_result = existing_body->parent()->replace_child(*new_body, *existing_body);
        if (replace_result.is_exception())
            return replace_result.exception();
        return {};
    }

    auto* document_element = this->document_element();
    if (!document_element)
        return DOM::HierarchyRequestError::create("Missing document element");

    auto append_result = document_element->append_child(*new_body);
    if (append_result.is_exception())
        return append_result.exception();
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

void Document::set_title(const String& title)
{
    auto* head_element = const_cast<HTML::HTMLHeadElement*>(head());
    if (!head_element)
        return;

    RefPtr<HTML::HTMLTitleElement> title_element = head_element->first_child_of_type<HTML::HTMLTitleElement>();
    if (!title_element) {
        title_element = static_ptr_cast<HTML::HTMLTitleElement>(create_element(HTML::TagNames::title));
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
    update_layout();
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

Color Document::background_color(const Palette& palette) const
{
    auto default_color = palette.base();
    auto* body_element = body();
    if (!body_element)
        return default_color;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return default_color;

    auto color = body_layout_node->computed_values().background_color();
    if (!color.alpha())
        return default_color;
    return color;
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

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#parse-a-url
AK::URL Document::parse_url(String const& url) const
{
    // FIXME: Make sure we do this according to spec.
    return m_url.complete_url(url);
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

void Document::ensure_layout()
{
    if (m_needs_layout || !m_layout_root)
        update_layout();
}

void Document::update_layout()
{
    if (!m_needs_layout && m_layout_root)
        return;

    if (!browsing_context())
        return;

    update_style();

    if (!m_layout_root) {
        Layout::TreeBuilder tree_builder;
        m_layout_root = static_ptr_cast<Layout::InitialContainingBlock>(tree_builder.build(*this));
    }

    Layout::BlockFormattingContext root_formatting_context(*m_layout_root, nullptr);
    root_formatting_context.run(*m_layout_root, Layout::LayoutMode::Default);

    m_layout_root->set_needs_display();

    if (browsing_context()->is_top_level()) {
        if (auto* page = this->page())
            page->client().page_did_layout();
    }

    m_needs_layout = false;
    m_layout_update_timer->stop();
}

static void update_style_recursively(DOM::Node& node)
{
    if (is<Element>(node))
        static_cast<Element&>(node).recompute_style();
    node.set_needs_style_update(false);

    if (node.child_needs_style_update()) {
        node.for_each_child([&](auto& child) {
            if (child.needs_style_update() || child.child_needs_style_update())
                update_style_recursively(child);
            return IterationDecision::Continue;
        });
    }

    node.set_child_needs_style_update(false);
}

void Document::update_style()
{
    if (!browsing_context())
        return;
    if (!needs_style_update() && !child_needs_style_update())
        return;
    update_style_recursively(*this);
    m_style_update_timer->stop();
    set_needs_layout();
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

const Layout::InitialContainingBlock* Document::layout_node() const
{
    return static_cast<const Layout::InitialContainingBlock*>(Node::layout_node());
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

void Document::set_hovered_node(Node* node)
{
    if (m_hovered_node == node)
        return;

    RefPtr<Node> old_hovered_node = move(m_hovered_node);
    m_hovered_node = node;

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

JS::Realm& Document::realm()
{
    return interpreter().realm();
}

JS::Interpreter& Document::interpreter()
{
    if (!m_interpreter) {
        auto& vm = Bindings::main_thread_vm();
        m_interpreter = JS::Interpreter::create<Bindings::WindowObject>(vm, *m_window);

        // NOTE: We must hook `on_call_stack_emptied` after the interpreter was created, as the initialization of the
        // WindowsObject can invoke some internal calls, which will eventually lead to this hook being called without
        // `m_interpreter` being fully initialized yet.
        // TODO: Hook up vm.on_promise_unhandled_rejection and vm.on_promise_rejection_handled
        // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises#promise_rejection_events
        vm.on_call_stack_emptied = [this] {
            auto& vm = m_interpreter->vm();
            vm.run_queued_promise_jobs();
            vm.run_queued_finalization_registry_cleanup_jobs();

            // FIXME: This isn't exactly the right place for this.
            HTML::main_thread_event_loop().perform_a_microtask_checkpoint();

            // Note: This is not an exception check for the promise jobs, they will just leave any
            // exception that already exists intact and never throw a new one (without cleaning it
            // up, that is). Taking care of any previous unhandled exception just happens to be the
            // very last thing we want to do, even after running promise jobs.
            if (auto* exception = vm.exception()) {
                auto value = exception->value();
                if (value.is_object()) {
                    auto& object = value.as_object();
                    auto name = object.get_without_side_effects(vm.names.name).value_or(JS::js_undefined());
                    auto message = object.get_without_side_effects(vm.names.message).value_or(JS::js_undefined());
                    if (name.is_accessor() || message.is_accessor()) {
                        // The result is not going to be useful, let's just print the value. This affects DOMExceptions, for example.
                        dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m {}", value);
                    } else {
                        dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m [{}] {}", name, message);
                    }
                } else {
                    dbgln("\033[31;1mUnhandled JavaScript exception:\033[0m {}", value);
                }
                for (auto& traceback_frame : exception->traceback()) {
                    auto& function_name = traceback_frame.function_name;
                    auto& source_range = traceback_frame.source_range;
                    dbgln("  {} at {}:{}:{}", function_name, source_range.filename, source_range.start.line, source_range.start.column);
                }
            }

            vm.finish_execution_generation();
        };
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

    auto& vm = interpreter.vm();
    if (result.is_error()) {
        // FIXME: I'm sure the spec could tell us something about error propagation here!
        vm.clear_exception();
        return {};
    }
    return result.value();
}

// https://dom.spec.whatwg.org/#dom-document-createelement
// FIXME: This only implements step 6 of the algorithm and does not take in options.
NonnullRefPtr<Element> Document::create_element(const String& tag_name)
{
    // FIXME: Let namespace be the HTML namespace, if this is an HTML document or this’s content type is "application/xhtml+xml", and null otherwise.
    return DOM::create_element(*this, tag_name, Namespace::HTML);
}

// https://dom.spec.whatwg.org/#internal-createelementns-steps
// FIXME: This only implements step 4 of the algorithm and does not take in options.
NonnullRefPtr<Element> Document::create_element_ns(const String& namespace_, const String& qualified_name)
{
    return DOM::create_element(*this, qualified_name, namespace_);
}

NonnullRefPtr<DocumentFragment> Document::create_document_fragment()
{
    return adopt_ref(*new DocumentFragment(*this));
}

NonnullRefPtr<Text> Document::create_text_node(const String& data)
{
    return adopt_ref(*new Text(*this, data));
}

NonnullRefPtr<Comment> Document::create_comment(const String& data)
{
    return adopt_ref(*new Comment(*this, data));
}

NonnullRefPtr<Range> Document::create_range()
{
    return Range::create(*this);
}

// https://dom.spec.whatwg.org/#dom-document-createevent
NonnullRefPtr<Event> Document::create_event(const String& interface)
{
    auto interface_lowercase = interface.to_lowercase();
    RefPtr<Event> event;
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
        event = UIEvents::MouseEvent::create("", 0, 0, 0, 0);
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
    } else {
        // FIXME:
        // 3. If constructor is null, then throw a "NotSupportedError" DOMException.
        // 4. If the interface indicated by constructor is not exposed on the relevant global object of this, then throw a "NotSupportedError" DOMException.
        TODO();
    }
    // Setting type to empty string is handled by each constructor.
    // FIXME:
    // 7. Initialize event’s timeStamp attribute to a DOMHighResTimeStamp representing the high resolution time from the time origin to now.
    event->set_is_trusted(false);
    event->set_initialized(false);
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
        // FIXME: This should be shadow-including.
        node.for_each_in_inclusive_subtree([&](auto& inclusive_descendant) {
            inclusive_descendant.set_document({}, *this);
            // FIXME: If inclusiveDescendant is an element, then set the node document of each attribute in inclusiveDescendant’s attribute list to document.
            return IterationDecision::Continue;
        });

        // FIXME: For each inclusiveDescendant in node’s shadow-including inclusive descendants that is custom,
        //        enqueue a custom element callback reaction with inclusiveDescendant, callback name "adoptedCallback",
        //        and an argument list containing oldDocument and document.

        // FIXME: This should be shadow-including.
        node.for_each_in_inclusive_subtree([&](auto& inclusive_descendant) {
            inclusive_descendant.adopted_from(old_document);
            return IterationDecision::Continue;
        });
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

const DocumentType* Document::doctype() const
{
    return first_child_of_type<DocumentType>();
}

const String& Document::compat_mode() const
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

    if (m_focused_element)
        m_focused_element->did_lose_focus();

    m_focused_element = element;

    if (m_focused_element)
        m_focused_element->did_receive_focus();

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

const Page* Document::page() const
{
    return m_browsing_context ? m_browsing_context->page() : nullptr;
}

EventTarget* Document::get_parent(const Event& event)
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

void Document::set_cookie(String cookie_string, Cookie::Source source)
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
    JsonObjectSerializer json(builder);
    serialize_tree_as_json(json);

    json.finish();
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
    // FIXME: Return the document's actual referrer.
    return "";
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

    dispatch_event(DOM::Event::create(UIEvents::EventNames::resize));

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
        // 1.1. If target’s matches state has changed since the last time these steps
        //      were run, fire an event at target using the MediaQueryListEvent constructor,
        //      with its type attribute initialized to change, its isTrusted attribute
        //      initialized to true, its media attribute initialized to target’s media,
        //      and its matches attribute initialized to target’s matches state.
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
    for (auto& style_sheet : style_sheets().sheets()) {
        style_sheet.evaluate_media_queries(window());
    }
}

NonnullRefPtr<DOMImplementation> Document::implementation() const
{
    return *m_implementation;
}

bool Document::has_focus() const
{
    // FIXME: Return whether we actually have focus.
    return true;
}

}
