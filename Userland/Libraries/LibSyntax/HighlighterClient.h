/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/TextDocument.h>
#include <LibGUI/TextPosition.h>

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
