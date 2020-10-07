/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/MessageBox.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutTreeBuilder.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/SVG/TagNames.h>
#include <stdio.h>

namespace Web::DOM {

Document::Document(const URL& url)
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_resolver(make<CSS::StyleResolver>(*this))
    , m_style_sheets(CSS::StyleSheetList::create(*this))
    , m_url(url)
    , m_window(Window::create_with_document(*this))
{
    m_style_update_timer = Core::Timer::create_single_shot(0, [this] {
        update_style();
    });
}

Document::~Document()
{
}

Origin Document::origin() const
{
    if (!m_url.is_valid())
        return {};
    return { m_url.protocol(), m_url.host(), m_url.port() };
}

void Document::schedule_style_update()
{
    if (m_style_update_timer->is_active())
        return;
    m_style_update_timer->start();
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

void Document::fixup()
{
    if (!first_child() || !is<DocumentType>(*first_child()))
        prepend_child(adopt(*new DocumentType(*this)));

    if (is<HTML::HTMLHtmlElement>(first_child()->next_sibling()))
        return;

    auto body = create_element("body");
    auto html = create_element("html");
    html->append_child(body);
    this->donate_all_children_to(body);
    this->append_child(html);
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

void Document::set_body(HTML::HTMLElement& new_body)
{
    if (!is<HTML::HTMLBodyElement>(new_body) && !is<HTML::HTMLFrameSetElement>(new_body)) {
        // FIXME: throw a "HierarchyRequestError" DOMException.
        return;
    }

    auto* existing_body = body();
    if (existing_body) {
        TODO();
        return;
    }

    auto* html = document_element();
    if (!html) {
        // FIXME: throw a "HierarchyRequestError" DOMException.
        return;
    }

    // FIXME: Implement this once there's a non-const first_child_of_type:
    //        "Otherwise, the body element is null, but there's a document element. Append the new value to the document element."
    TODO();
}

String Document::title() const
{
    auto* head_element = head();
    if (!head_element)
        return {};

    auto* title_element = head_element->first_child_of_type<HTML::HTMLTitleElement>();
    if (!title_element)
        return {};

    return title_element->text_content();
}

void Document::attach_to_frame(Badge<Frame>, Frame& frame)
{
    m_frame = frame.make_weak_ptr();
    for_each_in_subtree([&](auto& node) {
        node.document_did_attach_to_frame(frame);
        return IterationDecision::Continue;
    });
    layout();
}

void Document::detach_from_frame(Badge<Frame>, Frame& frame)
{
    for_each_in_subtree([&](auto& node) {
        node.document_will_detach_from_frame(frame);
        return IterationDecision::Continue;
    });
    m_layout_root = nullptr;
    m_frame = nullptr;
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

    auto background_color = body_layout_node->specified_style().property(CSS::PropertyID::BackgroundColor);
    if (!background_color.has_value() || !background_color.value()->is_color())
        return default_color;

    return background_color.value()->to_color(*this);
}

RefPtr<Gfx::Bitmap> Document::background_image() const
{
    auto* body_element = body();
    if (!body_element)
        return {};

    auto* body_layout_node = body_element->layout_node();
    if (!body_layout_node)
        return {};

    auto background_image = body_layout_node->specified_style().property(CSS::PropertyID::BackgroundImage);
    if (!background_image.has_value() || !background_image.value()->is_image())
        return {};

    auto& image_value = static_cast<const CSS::ImageStyleValue&>(*background_image.value());
    if (!image_value.bitmap())
        return {};

    return *image_value.bitmap();
}

URL Document::complete_url(const String& string) const
{
    return m_url.complete_url(string);
}

void Document::invalidate_layout()
{
    m_layout_root = nullptr;
}

void Document::force_layout()
{
    invalidate_layout();
    layout();
}

void Document::layout()
{
    if (!frame())
        return;

    if (!m_layout_root) {
        LayoutTreeBuilder tree_builder;
        m_layout_root = static_ptr_cast<LayoutDocument>(tree_builder.build(*this));
    }
    m_layout_root->layout();
    m_layout_root->set_needs_display();

    if (frame()->is_main_frame())
        frame()->page().client().page_did_layout();
}

void Document::update_style()
{
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.needs_style_update())
            element.recompute_style();
        return IterationDecision::Continue;
    });
    update_layout();
}

void Document::update_layout()
{
    if (!frame())
        return;

    layout();
}

RefPtr<LayoutNode> Document::create_layout_node(const CSS::StyleProperties*)
{
    return adopt(*new LayoutDocument(*this, CSS::StyleProperties::create()));
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

const LayoutDocument* Document::layout_node() const
{
    return static_cast<const LayoutDocument*>(Node::layout_node());
}

LayoutDocument* Document::layout_node()
{
    return static_cast<LayoutDocument*>(Node::layout_node());
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
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.local_name() == tag_name)
            elements.append(element);
        return IterationDecision::Continue;
    });
    return elements;
}

Color Document::link_color() const
{
    if (m_link_color.has_value())
        return m_link_color.value();
    if (!frame())
        return Color::Blue;
    return frame()->page().palette().link();
}

Color Document::active_link_color() const
{
    if (m_active_link_color.has_value())
        return m_active_link_color.value();
    if (!frame())
        return Color::Red;
    return frame()->page().palette().active_link();
}

Color Document::visited_link_color() const
{
    if (m_visited_link_color.has_value())
        return m_visited_link_color.value();
    if (!frame())
        return Color::Magenta;
    return frame()->page().palette().visited_link();
}

static JS::VM& main_thread_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm)
        vm = JS::VM::create();
    return *vm;
}

JS::Interpreter& Document::interpreter()
{
    if (!m_interpreter)
        m_interpreter = JS::Interpreter::create<Bindings::WindowObject>(main_thread_vm(), *m_window);
    return *m_interpreter;
}

JS::Value Document::run_javascript(const StringView& source)
{
    auto parser = JS::Parser(JS::Lexer(source));
    auto program = parser.parse_program();
    if (parser.has_errors()) {
        parser.print_errors();
        return JS::js_undefined();
    }
    auto& interpreter = document().interpreter();
    auto result = interpreter.run(interpreter.global_object(), *program);
    if (interpreter.exception())
        interpreter.vm().clear_exception();
    return result;
}

NonnullRefPtr<Element> Document::create_element(const String& tag_name)
{
    return DOM::create_element(*this, tag_name);
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

    if (element)
        m_focused_element = element->make_weak_ptr();
    else
        m_focused_element = nullptr;

    if (m_layout_root)
        m_layout_root->set_needs_display();
}

void Document::set_ready_state(const String& ready_state)
{
    m_ready_state = ready_state;
    dispatch_event(Event::create("readystatechange"));
}

}
