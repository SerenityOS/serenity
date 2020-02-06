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
#include <LibDraw/Rect.h>
#include <LibDraw/Size.h>
#include <LibHTML/TreeNode.h>

class Document;
class HtmlView;

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create(HtmlView& html_view) { return adopt(*new Frame(html_view)); }
    ~Frame();

    const Document* document() const { return m_document; }
    Document* document() { return m_document; }

    void set_document(Document*);

    HtmlView* html_view() { return m_html_view; }
    const HtmlView* html_view() const { return m_html_view; }

    const Gfx::Size& size() const { return m_size; }
    void set_size(const Gfx::Size&);

    void set_needs_display(const Gfx::Rect&);
    Function<void(const Gfx::Rect&)> on_set_needs_display;

    void set_viewport_rect(const Gfx::Rect&);
    Rect viewport_rect() const { return m_viewport_rect; }

private:
    explicit Frame(HtmlView&);

    WeakPtr<HtmlView> m_html_view;
    RefPtr<Document> m_document;
    Gfx::Size m_size;
    Gfx::Rect m_viewport_rect;
};
