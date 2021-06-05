/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DOMException.h>
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
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/SVG/TagNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::DOM {

Document::Document(const URL& url)
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_resolver(make<CSS::StyleResolver>(*this))
    , m_style_sheets(CSS::StyleSheetList::create(*this))
    , m_url(url)
    , m_window(Window::create_with_document(*this))
    , m_implementation(DOMImplementation::create(*this))
{
    m_style_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_style();
    });

    m_forced_layout_timer = Core::Timer::create_single_shot(0, [this] {
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
    delete this;
}

Origin Document::origin() const
{
    if (!m_url.is_valid())
        return {};
    return { m_url.protocol(), m_url.host(), m_url.port() };
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

void Document::schedule_forced_layout()
{
    if (m_forced_layout_timer->is_active())
        return;
    m_forced_layout_timer->start();
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
        return downcast<HTML::HTMLHtmlElement>(html);
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
ExceptionOr<void> Document::set_body(HTML::HTMLElement& new_body)
{
    if (!is<HTML::HTMLBodyElement>(new_body) && !is<HTML::HTMLFrameSetElement>(new_body))
        return DOM::HierarchyRequestError::create("Invalid document body element, must be 'body' or 'frameset'");

    auto* existing_body = body();
    if (existing_body) {
        auto replace_result = existing_body->parent()->replace_child(new_body, *existing_body);
        if (replace_result.is_exception())
            return NonnullRefPtr<DOMException>(replace_result.exception());
        return {};
    }

    auto* document_element = this->document_element();
    if (!document_element)
        return DOM::HierarchyRequestError::create("Missing document element");

    auto append_result = document_element->append_child(new_body);
    if (append_result.is_exception())
        return NonnullRefPtr<DOMException>(append_result.exception());
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

void Document::attach_to_browsing_context(Badge<BrowsingContext>, BrowsingContext& browsing_context)
{
    m_browsing_context = browsing_context;
    update_layout();
}

void Document::detach_from_browsing_context(Badge<BrowsingContext>, BrowsingContext& browsing_context)
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

RefPtr<Gfx::Bitmap> Document::background_image() const
{
    auto* body_element = body();
    if (!body_element)
        return {};

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return {};

    auto background_image = body_layout_node->background_image();
    if (!background_image)
        return {};
    return background_image->bitmap();
}

CSS::Repeat Document::background_repeat_x() const
{
    auto* body_element = body();
    if (!body_element)
        return CSS::Repeat::Repeat;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return CSS::Repeat::Repeat;

    return body_layout_node->computed_values().background_repeat_x();
}

CSS::Repeat Document::background_repeat_y() const
{
    auto* body_element = body();
    if (!body_element)
        return CSS::Repeat::Repeat;

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return CSS::Repeat::Repeat;

    return body_layout_node->computed_values().background_repeat_y();
}

URL Document::complete_url(const String& string) const
{
    return m_url.complete_url(string);
}

void Document::invalidate_layout()
{
    tear_down_layout_tree();
}

void Document::force_layout()
{
    invalidate_layout();
    update_layout();
}

void Document::update_layout()
{
    if (!browsing_context())
        return;

    if (!m_layout_root) {
        Layout::TreeBuilder tree_builder;
        m_layout_root = static_ptr_cast<Layout::InitialContainingBlockBox>(tree_builder.build(*this));
    }

    Layout::BlockFormattingContext root_formatting_context(*m_layout_root, nullptr);
    root_formatting_context.run(*m_layout_root, Layout::LayoutMode::Default);

    m_layout_root->set_needs_display();

    if (browsing_context()->is_top_level()) {
        if (auto* page = this->page())
            page->client().page_did_layout();
    }
}

static void update_style_recursively(DOM::Node& node)
{
    node.for_each_child([&](auto& child) {
        if (child.needs_style_update()) {
            if (is<Element>(child))
                downcast<Element>(child).recompute_style();
            child.set_needs_style_update(false);
        }
        if (child.child_needs_style_update()) {
            update_style_recursively(child);
            child.set_child_needs_style_update(false);
        }
        return IterationDecision::Continue;
    });
}

void Document::update_style()
{
    update_style_recursively(*this);
    update_layout();
}

RefPtr<Layout::Node> Document::create_layout_node()
{
    return adopt_ref(*new Layout::InitialContainingBlockBox(*this, CSS::StyleProperties::create()));
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

const Layout::InitialContainingBlockBox* Document::layout_node() const
{
    return static_cast<const Layout::InitialContainingBlockBox*>(Node::layout_node());
}

Layout::InitialContainingBlockBox* Document::layout_node()
{
    return static_cast<Layout::InitialContainingBlockBox*>(Node::layout_node());
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

NonnullRefPtr<HTMLCollection> Document::get_elements_by_tag_name(FlyString const& tag_name)
{
    // FIXME: Support "*" for tag_name
    // https://dom.spec.whatwg.org/#concept-getelementsbytagname
    return HTMLCollection::create(*this, [tag_name](Element const& element) {
        if (element.namespace_() == Namespace::HTML)
            return element.local_name().to_lowercase() == tag_name.to_lowercase();
        return element.local_name() == tag_name;
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

JS::Interpreter& Document::interpreter()
{
    if (!m_interpreter) {
        auto& vm = Bindings::main_thread_vm();
        // TODO: Hook up vm.on_promise_unhandled_rejection and vm.on_promise_rejection_handled
        // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises#promise_rejection_events
        vm.on_call_stack_emptied = [this] {
            auto& vm = m_interpreter->vm();
            vm.run_queued_promise_jobs();
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
                    if (name.is_accessor() || name.is_native_property() || message.is_accessor() || message.is_native_property()) {
                        // The result is not going to be useful, let's just print the value. This affects DOMExceptions, for example.
                        dbgln("Unhandled JavaScript exception: {}", value);
                    } else {
                        dbgln("Unhandled JavaScript exception: [{}] {}", name, message);
                    }
                } else {
                    dbgln("Unhandled JavaScript exception: {}", value);
                }
                for (auto& traceback_frame : exception->traceback()) {
                    auto& function_name = traceback_frame.function_name;
                    auto& source_range = traceback_frame.source_range;
                    dbgln("  {} at {}:{}:{}", function_name, source_range.filename, source_range.start.line, source_range.start.column);
                }
            }
        };
        m_interpreter = JS::Interpreter::create<Bindings::WindowObject>(vm, *m_window);
    }
    return *m_interpreter;
}

JS::Value Document::run_javascript(const StringView& source, const StringView& filename)
{
    auto parser = JS::Parser(JS::Lexer(source, filename));
    auto program = parser.parse_program();
    if (parser.has_errors()) {
        parser.print_errors();
        return JS::js_undefined();
    }
    auto& interpreter = document().interpreter();
    auto& vm = interpreter.vm();
    interpreter.run(interpreter.global_object(), *program);
    if (vm.exception())
        vm.clear_exception();
    return vm.last_value();
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
        event = Event::create(""); // FIXME: Create CustomEvent
    } else if (interface_lowercase == "devicemotionevent") {
        event = Event::create(""); // FIXME: Create DeviceMotionEvent
    } else if (interface_lowercase == "deviceorientationevent") {
        event = Event::create(""); // FIXME: Create DeviceOrientationEvent
    } else if (interface_lowercase == "dragevent") {
        event = Event::create(""); // FIXME: Create DragEvent
    } else if (interface_lowercase.is_one_of("event", "events")) {
        event = Event::create("");
    } else if (interface_lowercase == "focusevent") {
        event = Event::create(""); // FIXME: Create FocusEvent
    } else if (interface_lowercase == "hashchangeevent") {
        event = Event::create(""); // FIXME: Create HashChangeEvent
    } else if (interface_lowercase == "htmlevents") {
        event = Event::create("");
    } else if (interface_lowercase == "keyboardevent") {
        event = Event::create(""); // FIXME: Create KeyboardEvent
    } else if (interface_lowercase == "messageevent") {
        event = Event::create(""); // FIXME: Create MessageEvent
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

NonnullRefPtr<HTML::HTMLScriptElement> Document::take_pending_parsing_blocking_script(Badge<HTML::HTMLDocumentParser>)
{
    return m_pending_parsing_blocking_script.release_nonnull();
}

void Document::add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_when_parsing_has_finished.append(script);
}

NonnullRefPtrVector<HTML::HTMLScriptElement> Document::take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLDocumentParser>)
{
    return move(m_scripts_to_execute_when_parsing_has_finished);
}

void Document::add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement& script)
{
    m_scripts_to_execute_as_soon_as_possible.append(script);
}

NonnullRefPtrVector<HTML::HTMLScriptElement> Document::take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLDocumentParser>)
{
    return move(m_scripts_to_execute_as_soon_as_possible);
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

    if (is<DocumentFragment>(*node) && downcast<DocumentFragment>(*node).host())
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

    m_focused_element = element;

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

void Document::set_ready_state(const String& ready_state)
{
    m_ready_state = ready_state;
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

void Document::completely_finish_loading()
{
    // FIXME: This needs to handle iframes.
    dispatch_event(DOM::Event::create(HTML::EventNames::load));
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

}
