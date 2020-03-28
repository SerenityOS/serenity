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

#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/MessageBox.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLBodyElement.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/DOM/HTMLHtmlElement.h>
#include <LibWeb/DOM/HTMLTitleElement.h>
#include <LibWeb/Frame.h>
#include <LibWeb/HtmlView.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutTreeBuilder.h>
#include <stdio.h>

namespace Web {

Document::Document()
    : ParentNode(*this, NodeType::DOCUMENT_NODE)
    , m_style_resolver(make<StyleResolver>(*this))
{
    m_style_update_timer = Core::Timer::construct();
    m_style_update_timer->set_single_shot(true);
    m_style_update_timer->set_interval(0);
    m_style_update_timer->on_timeout = [this] {
        update_style();
    };
}

Document::~Document()
{
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

    if (is<HTMLHtmlElement>(first_child()->next_sibling()))
        return;

    auto body = create_element(*this, "body");
    auto html = create_element(*this, "html");
    html->append_child(body);
    this->donate_all_children_to(body);
    this->append_child(html);
}

const HTMLHtmlElement* Document::document_element() const
{
    return first_child_of_type<HTMLHtmlElement>();
}

const HTMLHeadElement* Document::head() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTMLHeadElement>();
}

const HTMLBodyElement* Document::body() const
{
    auto* html = document_element();
    if (!html)
        return nullptr;
    return html->first_child_of_type<HTMLBodyElement>();
}

String Document::title() const
{
    auto* head_element = head();
    if (!head_element)
        return {};

    auto* title_element = head_element->first_child_of_type<HTMLTitleElement>();
    if (!title_element)
        return {};

    return title_element->text_content();
}

void Document::attach_to_frame(Badge<Frame>, Frame& frame)
{
    m_frame = frame.make_weak_ptr();
    layout();
}

void Document::detach_from_frame(Badge<Frame>, Frame&)
{
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

    auto background_color = body_layout_node->style().property(CSS::PropertyID::BackgroundColor);
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

    auto background_image = body_layout_node->style().property(CSS::PropertyID::BackgroundImage);
    if (!background_image.has_value() || !background_image.value()->is_image())
        return {};

    auto& image_value = static_cast<const ImageStyleValue&>(*background_image.value());
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
        m_layout_root = tree_builder.build(*this);
    }
    m_layout_root->layout();
    m_layout_root->set_needs_display();
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
    if (on_layout_updated)
        on_layout_updated();
}

RefPtr<LayoutNode> Document::create_layout_node(const StyleProperties*) const
{
    return adopt(*new LayoutDocument(*this, StyleProperties::create()));
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

Vector<const Element*> Document::get_elements_by_name(const String& name) const
{
    Vector<const Element*> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.attribute("name") == name)
            elements.append(&element);
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
    return frame()->html_view()->palette().link();
}

Color Document::active_link_color() const
{
    if (m_active_link_color.has_value())
        return m_active_link_color.value();
    if (!frame())
        return Color::Red;
    return frame()->html_view()->palette().active_link();
}

Color Document::visited_link_color() const
{
    if (m_visited_link_color.has_value())
        return m_visited_link_color.value();
    if (!frame())
        return Color::Magenta;
    return frame()->html_view()->palette().visited_link();
}

JS::Interpreter& Document::interpreter()
{
    if (!m_interpreter) {
        m_interpreter = make<JS::Interpreter>();

        m_interpreter->global_object().put_native_function("alert", [](JS::Interpreter& interpreter) -> JS::Value {
            auto& arguments = interpreter.call_frame().arguments;
            if (arguments.size() < 1)
                return JS::js_undefined();
            GUI::MessageBox::show(arguments[0].to_string(), "Alert", GUI::MessageBox::Type::Information);
            return JS::js_undefined();
        });

        m_interpreter->global_object().put_native_function("setInterval", [this](JS::Interpreter& interpreter) -> JS::Value {
            auto& arguments = interpreter.call_frame().arguments;
            if (arguments.size() < 2)
                return JS::js_undefined();
            ASSERT(arguments[0].is_object());
            ASSERT(arguments[0].as_object()->is_function());
            auto callback = make_handle(const_cast<JS::Object*>(arguments[0].as_object()));

            // FIXME: This timer should not be leaked! It should also be removable with clearInterval()!
            (void)Core::Timer::construct(
                arguments[1].to_i32(), [this, callback] {
                    // FIXME: Perform the call through Interpreter so it can set up a call frame!
                    const_cast<JS::Function*>(static_cast<const JS::Function*>(callback.cell()))->call(*m_interpreter);
                })
                .leak_ref();

            return JS::js_undefined();
        });

        m_interpreter->global_object().put_native_function("requestAnimationFrame", [this](JS::Interpreter& interpreter) -> JS::Value {
            auto& arguments = interpreter.call_frame().arguments;
            if (arguments.size() < 1)
                return JS::js_undefined();
            ASSERT(arguments[0].is_object());
            ASSERT(arguments[0].as_object()->is_function());
            auto callback = make_handle(const_cast<JS::Object*>(arguments[0].as_object()));
            // FIXME: Don't hand out raw DisplayLink ID's to JavaScript!
            i32 link_id = GUI::DisplayLink::register_callback([this, callback](i32 link_id) {
                // FIXME: Perform the call through Interpreter so it can set up a call frame!
                const_cast<JS::Function*>(static_cast<const JS::Function*>(callback.cell()))->call(*m_interpreter);
                GUI::DisplayLink::unregister_callback(link_id);
            });
            return JS::Value(link_id);
        });

        m_interpreter->global_object().put_native_function("cancelAnimationFrame", [](JS::Interpreter& interpreter) -> JS::Value {
            auto& arguments = interpreter.call_frame().arguments;
            if (arguments.size() < 1)
                return JS::js_undefined();
            // FIXME: We should not be passing untrusted numbers to DisplayLink::unregistered_callback()!
            GUI::DisplayLink::unregister_callback(arguments[0].to_i32());
            return JS::js_undefined();
        });

        m_interpreter->global_object().put_native_property(
            "document",
            [this](JS::Object*) {
                return wrap(m_interpreter->heap(), *this);
            },
            nullptr);
    }
    return *m_interpreter;
}

}
