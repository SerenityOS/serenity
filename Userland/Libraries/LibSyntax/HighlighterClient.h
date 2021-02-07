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

#pragma once

#include <LibGUI/Forward.h>

namespace Syntax {

class HighlighterClient {
public:
    virtual ~HighlighterClient() = default;

    virtual Vector<GUI::TextDocumentSpan>& spans() = 0;
    virtual const Vector<GUI::TextDocumentSpan>& spans() const = 0;
    virtual void set_span_at_index(size_t index, GUI::TextDocumentSpan span) = 0;

    virtual String highlighter_did_request_text() const = 0;
    virtual void highlighter_did_request_update() = 0;
    virtual GUI::TextDocument& highlighter_did_request_document() = 0;
    virtual GUI::TextPosition highlighter_did_request_cursor() const = 0;
    virtual void highlighter_did_set_spans(Vector<GUI::TextDocumentSpan>) = 0;

    void do_set_spans(Vector<GUI::TextDocumentSpan> spans) { highlighter_did_set_spans(move(spans)); }
    void do_update() { highlighter_did_request_update(); }

    String get_text() const { return highlighter_did_request_text(); }
    GUI::TextDocument& get_document() { return highlighter_did_request_document(); }
    GUI::TextPosition get_cursor() const { return highlighter_did_request_cursor(); }
};

}
