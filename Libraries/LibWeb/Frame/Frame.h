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

#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibWeb/Frame/EventHandler.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/TreeNode.h>

namespace Web {

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create_subframe(Element& host_element, Frame& main_frame) { return adopt(*new Frame(host_element, main_frame)); }
    static NonnullRefPtr<Frame> create(PageView& page_view) { return adopt(*new Frame(page_view)); }
    ~Frame();

    bool is_main_frame() const { return this == &m_main_frame; }

    const Document* document() const { return m_document; }
    Document* document() { return m_document; }

    void set_document(Document*);

    PageView* page_view() { return is_main_frame() ? m_page_view : main_frame().m_page_view; }
    const PageView* page_view() const { return is_main_frame() ? m_page_view : main_frame().m_page_view; }

    const Gfx::Size& size() const { return m_size; }
    void set_size(const Gfx::Size&);

    void set_needs_display(const Gfx::Rect&);

    Function<void(const String&)> on_title_change;
    Function<void(const URL&)> on_load_start;
    Function<void(const Gfx::Bitmap&)> on_favicon_change;
    Function<void(Document*)> on_set_document;

    void set_viewport_rect(const Gfx::Rect&);
    Gfx::Rect viewport_rect() const { return m_viewport_rect; }

    void did_scroll(Badge<PageView>);

    FrameLoader& loader() { return m_loader; }
    const FrameLoader& loader() const { return m_loader; }

    EventHandler& event_handler() { return m_event_handler; }
    const EventHandler& event_handler() const { return m_event_handler; }

    void scroll_to_anchor(const String&);

    Frame& main_frame() { return m_main_frame; }
    const Frame& main_frame() const { return m_main_frame; }

    Element* host_element() { return m_host_element; }
    const Element* host_element() const { return m_host_element; }

private:
    explicit Frame(Element& host_element, Frame& main_frame);
    explicit Frame(PageView&);

    Frame& m_main_frame;

    FrameLoader m_loader;
    EventHandler m_event_handler;

    WeakPtr<Element> m_host_element;
    WeakPtr<PageView> m_page_view;
    RefPtr<Document> m_document;
    Gfx::Size m_size;
    Gfx::Rect m_viewport_rect;
};

}
