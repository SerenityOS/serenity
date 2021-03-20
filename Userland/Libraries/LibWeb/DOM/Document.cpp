/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/SVG/TagNames.h>
#include <ctype.h>

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
            for_each_in_subtree([&](auto& node) {
                if (&node != this)
                    descendants.append(node);
                return IterationDecision::Continue;
            });

            for (auto& node : descendants) {
                VERIFY(&node.document() == this);
                VERIFY(!node.is_document());
                if (node.parent())
                    node.parent()->remove_child(node);
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

const HTML::HTMLHtmlElement* Document::html_element() const
{
    auto* html = document_element();
    if (is<HTML::HTMLHtmlElement>(html))
        return downcast<HTML::HTMLHtmlElement>(html);
    return nullptr;
}

const HTML::HTMLHeadElement* Document::head() const
{
    auto* html = html_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTML::HTMLHeadElement>();
}

const HTML::HTMLElement* Document::body() const
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
        TODO();
        return {};
    }

    auto* document_element = this->document_element();
    if (!document_element)
        return DOM::HierarchyRequestError::create("Missing document element");

    document_element->append_child(new_body);
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
        if (isspace(code_point)) {
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

    while (RefPtr<Node> child = title_element->first_child())
        title_element->remove_child(child.release_nonnull());

    title_element->append_child(adopt(*new Text(*this, title)));

    if (auto* page = this->page()) {
        if (frame() == &page->main_frame())
            page->client().page_did_change_title(title);
    }
}

void Document::attach_to_frame(Badge<Frame>, Frame& frame)
{
    m_frame = frame;
    update_layout();
}

void Document::detach_from_frame(Badge<Frame>, Frame& frame)
{
    VERIFY(&frame == m_frame);
    tear_down_layout_tree();
    m_frame = nullptr;
}

void Document::tear_down_layout_tree()
{
    if (!m_layout_root)
        return;

    // Gather up all the layout nodes in a vector and detach them from parents
    // while the vector keeps them alive.

    NonnullRefPtrVector<Layout::Node> layout_nodes;

    m_layout_root->for_each_in_subtree([&](auto& layout_node) {
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
    if (!frame())
        return;

    if (!m_layout_root) {
        Layout::TreeBuilder tree_builder;
        m_layout_root = static_ptr_cast<Layout::InitialContainingBlockBox>(tree_builder.build(*this));
    }

    Layout::BlockFormattingContext root_formatting_context(*m_layout_root, nullptr);
    root_formatting_context.run(*m_layout_root, Layout::LayoutMode::Default);

    m_layout_root->set_needs_display();

    if (frame()->is_main_frame()) {
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
    return adopt(*new Layout::InitialContainingBlockBox(*this, CSS::StyleProperties::create()));
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

NonnullRefPtrVector<Element> Document::get_elements_by_name(const String& name) const
{
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.attribute(HTML::AttributeNames::name) == name)
            elements.append(element);
        return IterationDecision::Continue;
    });
    return elements;
}

NonnullRefPtrVector<Element> Document::get_elements_by_tag_name(const FlyString& tag_name) const
{
    // FIXME: Support "*" for tag_name
    // https://dom.spec.whatwg.org/#concept-getelementsbytagname
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.namespace_() == Namespace::HTML
                ? element.local_name().to_lowercase() == tag_name.to_lowercase()
                : element.local_name() == tag_name) {
            elements.append(element);
        }
        return IterationDecision::Continue;
    });
    return elements;
}

NonnullRefPtrVector<Element> Document::get_elements_by_class_name(const FlyString& class_name) const
{
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.has_class(class_name, in_quirks_mode() ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive))
            elements.append(element);
        return IterationDecision::Continue;
    });
    return elements;
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
    if (!m_interpreter)
        m_interpreter = JS::Interpreter::create<Bindings::WindowObject>(Bindings::main_thread_vm(), *m_window);
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
    return adopt(*new DocumentFragment(*this));
}

NonnullRefPtr<Text> Document::create_text_node(const String& data)
{
    return adopt(*new Text(*this, data));
}

NonnullRefPtr<Comment> Document::create_comment(const String& data)
{
    return adopt(*new Comment(*this, data));
}

NonnullRefPtr<Range> Document::create_range()
{
    return Range::create(*this);
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

void Document::adopt_node(Node& subtree_root)
{
    subtree_root.for_each_in_subtree([&](auto& node) {
        node.set_document({}, *this);
        return IterationDecision::Continue;
    });
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
    return m_frame ? m_frame->page() : nullptr;
}

const Page* Document::page() const
{
    return m_frame ? m_frame->page() : nullptr;
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

String Document::cookie() const
{
    // FIXME: Support cookies!
    return {};
}

void Document::set_cookie(String)
{
    // FIXME: Support cookies!
}

}
