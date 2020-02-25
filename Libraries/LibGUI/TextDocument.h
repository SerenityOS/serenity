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

#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <LibCore/Forward.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGUI/TextRange.h>
#include <LibGUI/UndoStack.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>

namespace GUI {

struct TextDocumentSpan {
    TextRange range;
    Color color;
    Optional<Color> background_color;
    bool is_skippable { false };
    const Gfx::Font* font { nullptr };
    void* data { nullptr };
};

class TextDocument : public RefCounted<TextDocument> {
public:
    enum class SearchShouldWrap {
        No = 0,
        Yes
    };

    class Client {
    public:
        virtual ~Client();
        virtual void document_did_append_line() = 0;
        virtual void document_did_insert_line(size_t) = 0;
        virtual void document_did_remove_line(size_t) = 0;
        virtual void document_did_remove_all_lines() = 0;
        virtual void document_did_change() = 0;
        virtual void document_did_set_text() = 0;
        virtual void document_did_set_cursor(const TextPosition&) = 0;

        virtual bool is_automatic_indentation_enabled() const = 0;
        virtual int soft_tab_width() const = 0;
    };

    static NonnullRefPtr<TextDocument> create(Client* client = nullptr);
    ~TextDocument();

    size_t line_count() const { return (size_t)m_lines.size(); }
    const TextDocumentLine& line(size_t line_index) const { return m_lines[(int)line_index]; }
    TextDocumentLine& line(size_t line_index) { return m_lines[(int)line_index]; }

    void set_spans(const Vector<TextDocumentSpan>& spans) { m_spans = spans; }

    void set_text(const StringView&);

    const NonnullOwnPtrVector<TextDocumentLine>& lines() const { return m_lines; }
    NonnullOwnPtrVector<TextDocumentLine>& lines() { return m_lines; }

    bool has_spans() const { return !m_spans.is_empty(); }
    const Vector<TextDocumentSpan>& spans() const { return m_spans; }
    void set_span_at_index(size_t index, TextDocumentSpan span) { m_spans[(int)index] = move(span); }

    void append_line(NonnullOwnPtr<TextDocumentLine>);
    void remove_line(size_t line_index);
    void remove_all_lines();
    void insert_line(size_t line_index, NonnullOwnPtr<TextDocumentLine>);

    void register_client(Client&);
    void unregister_client(Client&);

    void update_views(Badge<TextDocumentLine>);

    String text_in_range(const TextRange&) const;

    Vector<TextRange> find_all(const StringView& needle) const;

    TextRange find_next(const StringView&, const TextPosition& start = {}, SearchShouldWrap = SearchShouldWrap::Yes) const;
    TextRange find_previous(const StringView&, const TextPosition& start = {}, SearchShouldWrap = SearchShouldWrap::Yes) const;

    TextPosition next_position_after(const TextPosition&, SearchShouldWrap = SearchShouldWrap::Yes) const;
    TextPosition previous_position_before(const TextPosition&, SearchShouldWrap = SearchShouldWrap::Yes) const;

    char character_at(const TextPosition&) const;

    TextRange range_for_entire_line(size_t line_index) const;

    Optional<TextDocumentSpan> first_non_skippable_span_before(const TextPosition&) const;
    Optional<TextDocumentSpan> first_non_skippable_span_after(const TextPosition&) const;

    void add_to_undo_stack(NonnullOwnPtr<TextDocumentUndoCommand>);

    bool can_undo() const { return m_undo_stack.can_undo(); }
    bool can_redo() const { return m_undo_stack.can_redo(); }
    void undo();
    void redo();

    void notify_did_change();
    void set_all_cursors(const TextPosition&);

    TextPosition insert_at(const TextPosition&, char, const Client* = nullptr);
    TextPosition insert_at(const TextPosition&, const StringView&, const Client* = nullptr);
    void remove(const TextRange&);

private:
    explicit TextDocument(Client* client);

    void update_undo_timer();

    NonnullOwnPtrVector<TextDocumentLine> m_lines;
    Vector<TextDocumentSpan> m_spans;

    HashTable<Client*> m_clients;
    bool m_client_notifications_enabled { true };

    UndoStack m_undo_stack;
    RefPtr<Core::Timer> m_undo_timer;
};

class TextDocumentLine {
    friend class GTextEditor;
    friend class TextDocument;

public:
    explicit TextDocumentLine(TextDocument&);
    explicit TextDocumentLine(TextDocument&, const StringView&);

    StringView view() const { return { characters(), (size_t)length() }; }
    const char* characters() const { return m_text.data(); }
    size_t length() const { return (size_t)m_text.size() - 1; }
    void set_text(TextDocument&, const StringView&);
    void append(TextDocument&, char);
    void prepend(TextDocument&, char);
    void insert(TextDocument&, size_t index, char);
    void remove(TextDocument&, size_t index);
    void append(TextDocument&, const char*, size_t);
    void truncate(TextDocument&, size_t length);
    void clear(TextDocument&);
    size_t first_non_whitespace_column() const;

private:
    // NOTE: This vector is null terminated.
    Vector<char> m_text;
};

class TextDocumentUndoCommand : public Command {
public:
    TextDocumentUndoCommand(TextDocument&);
    virtual ~TextDocumentUndoCommand();

    void execute_from(const TextDocument::Client& client)
    {
        m_client = &client;
        redo();
        m_client = nullptr;
    }

protected:
    TextDocument& m_document;
    const TextDocument::Client* m_client { nullptr };
};

class InsertTextCommand : public TextDocumentUndoCommand {
public:
    InsertTextCommand(TextDocument&, const String&, const TextPosition&);
    virtual void undo() override;
    virtual void redo() override;

private:
    String m_text;
    TextRange m_range;
};

class RemoveTextCommand : public TextDocumentUndoCommand {
public:
    RemoveTextCommand(TextDocument&, const String&, const TextRange&);
    virtual void undo() override;
    virtual void redo() override;

private:
    String m_text;
    TextRange m_range;
};

}
