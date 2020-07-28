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

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutWidget.h>
#include <LibWeb/PageView.h>

namespace Web {

Frame::Frame(DOM::Element& host_element, Frame& main_frame)
    : m_page(main_frame.page())
    , m_main_frame(main_frame)
    , m_loader(*this)
    , m_event_handler({}, *this)
    , m_host_element(host_element.make_weak_ptr())
{
}

Frame::Frame(Page& page)
    : m_page(page)
    , m_main_frame(*this)
    , m_loader(*this)
    , m_event_handler({}, *this)
{
}

Frame::~Frame()
{
}

void Frame::set_document(DOM::Document* document)
{
    if (m_document == document)
        return;

    if (m_document)
        m_document->detach_from_frame({}, *this);

    m_document = document;

    if (m_document)
        m_document->attach_to_frame({}, *this);

    page().client().page_did_set_document_in_main_frame(m_document);
}

void Frame::set_size(const Gfx::IntSize& size)
{
    if (m_size == size)
        return;
    m_size = size;
    if (m_document)
        m_document->layout();
}

void Frame::set_viewport_rect(const Gfx::IntRect& rect)
{
    if (m_viewport_rect == rect)
        return;
    m_viewport_rect = rect;

    if (m_document && m_document->layout_node())
        m_document->layout_node()->did_set_viewport_rect({}, rect);
}

void Frame::set_needs_display(const Gfx::IntRect& rect)
{
    if (!m_viewport_rect.intersects(rect))
        return;

    if (is_main_frame()) {
        page().client().page_did_invalidate(to_main_frame_rect(rect));
        return;
    }

    if (host_element() && host_element()->layout_node())
        host_element()->layout_node()->set_needs_display();
}

void Frame::did_scroll(Badge<PageView>)
{
    if (!m_document)
        return;
    if (!m_document->layout_node())
        return;
    m_document->layout_node()->for_each_in_subtree_of_type<LayoutWidget>([&](auto& layout_widget) {
        layout_widget.update_widget();
        return IterationDecision::Continue;
    });
}

void Frame::scroll_to_anchor(const String& fragment)
{
    if (!document())
        return;

    const auto* element = document()->get_element_by_id(fragment);
    if (!element) {
        auto candidates = document()->get_elements_by_name(fragment);
        for (auto* candidate : candidates) {
            if (is<HTML::HTMLAnchorElement>(*candidate)) {
                element = downcast<HTML::HTMLAnchorElement>(candidate);
                break;
            }
        }
    }

    if (!element || !element->layout_node())
        return;

    auto& layout_node = *element->layout_node();

    Gfx::FloatRect float_rect { layout_node.box_type_agnostic_position(), { (float)viewport_rect().width(), (float)viewport_rect().height() } };
    if (is<LayoutBox>(layout_node)) {
        auto& layout_box = downcast<LayoutBox>(layout_node);
        auto padding_box = layout_box.box_model().padding_box(layout_box);
        float_rect.move_by(-padding_box.left, -padding_box.top);
    }

    page().client().page_did_request_scroll_into_view(enclosing_int_rect(float_rect));
}

Gfx::IntRect Frame::to_main_frame_rect(const Gfx::IntRect& a_rect)
{
    auto rect = a_rect;
    rect.set_location(to_main_frame_position(a_rect.location()));
    return rect;
}

Gfx::IntPoint Frame::to_main_frame_position(const Gfx::IntPoint& a_position)
{
    auto position = a_position;
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_main_frame())
            break;
        if (!ancestor->host_element())
            return {};
        if (!ancestor->host_element()->layout_node())
            return {};
        position.move_by(ancestor->host_element()->layout_node()->box_type_agnostic_position().to_type<int>());
    }
    return position;
}

}
