/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/Time.h>
#include <AK/Utf32View.h>
#include <LibCore/Forward.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGUI/TextRange.h>
#include <LibGUI/UndoStack.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>
#include <LibGfx/TextAttributes.h>
#include <LibRegex/Regex.h>

namespace GUI {

constexpr Time COMMAND_COMMIT_TIME = Time::from_milliseconds(400);

struct TextDocumentSpan {
    TextRange range;
    Gfx::TextAttributes attributes;
    u64 data { 0 };
    bool is_skippable { false };
};

class TextDocument : public RefCounted<TextDocument> {
public:
    enum class SearchShouldWrap {
        No = 0,
        Yes
    };

    class Client {
    public:
        virtual ~Client() = default;
        virtual void document_did_append_line() = 0;
        virtual void document_did_insert_line(size_t) = 0;
        virtual void document_did_remove_line(size_t) = 0;
        virtual void document_did_remove_all_lines() = 0;
        virtual void document_did_change(AllowCallback = AllowCallback::Yes) = 0;
        virtual void document_did_set_text(AllowCallback = AllowCallback::Yes) = 0;
        virtual void document_did_set_cursor(TextPosition const&) = 0;
        virtual void document_did_update_undo_stack() = 0;

        virtual bool is_automatic_indentation_enabled() const = 0;
        virtual int soft_tab_width() const = 0;
    };

    static NonnullRefPtr<TextDocument> create(Client* client = nullptr);
    virtual ~TextDocument() = default;

    size_t line_count() const { return m_lines.size(); }
    TextDocumentLine const& line(size_t line_index) const { return m_lines[line_index]; }
    TextDocumentLine& line(size_t line_index) { return m_lines[line_index]; }

    void set_spans(u32 span_collection_index, Vector<TextDocumentSpan> spans);

    bool set_text(StringView, AllowCallback = AllowCallback::Yes);

    NonnullOwnPtrVector<TextDocumentLine> const& lines() const { return m_lines; }
    NonnullOwnPtrVector<TextDocumentLine>& lines() { return m_lines; }

    bool has_spans() const { return !m_spans.is_empty(); }
    Vector<TextDocumentSpan>& spans() { return m_spans; }
    Vector<TextDocumentSpan> const& spans() const { return m_spans; }
    void set_span_at_index(size_t index, TextDocumentSpan span) { m_spans[index] = move(span); }

    TextDocumentSpan const* span_at(TextPosition const&) const;

    void append_line(NonnullOwnPtr<TextDocumentLine>);
    void remove_line(size_t line_index);
    void remove_all_lines();
    void insert_line(size_t line_index, NonnullOwnPtr<TextDocumentLine>);

    void register_client(Client&);
    void unregister_client(Client&);

    void update_views(Badge<TextDocumentLine>);

    String text() const;
    String text_in_range(TextRange const&) const;

    Vector<TextRange> find_all(StringView needle, bool regmatch = false, bool match_case = true);

    void update_regex_matches(StringView);
    TextRange find_next(StringView, TextPosition const& start = {}, SearchShouldWrap = SearchShouldWrap::Yes, bool regmatch = false, bool match_case = true);
    TextRange find_previous(StringView, TextPosition const& start = {}, SearchShouldWrap = SearchShouldWrap::Yes, bool regmatch = false, bool match_case = true);

    TextPosition next_position_after(TextPosition const&, SearchShouldWrap = SearchShouldWrap::Yes) const;
    TextPosition previous_position_before(TextPosition const&, SearchShouldWrap = SearchShouldWrap::Yes) const;

    u32 code_point_at(TextPosition const&) const;

    TextRange range_for_entire_line(size_t line_index) const;

    Optional<TextDocumentSpan> first_non_skippable_span_before(TextPosition const&) const;
    Optional<TextDocumentSpan> first_non_skippable_span_after(TextPosition const&) const;

    TextPosition first_word_break_before(TextPosition const&, bool start_at_column_before) const;
    TextPosition first_word_break_after(TextPosition const&) const;

    TextPosition first_word_before(TextPosition const&, bool start_at_column_before) const;

    void add_to_undo_stack(NonnullOwnPtr<TextDocumentUndoCommand>);

    bool can_undo() const { return m_undo_stack.can_undo(); }
    bool can_redo() const { return m_undo_stack.can_redo(); }
    void undo();
    void redo();

    UndoStack const& undo_stack() const { return m_undo_stack; }

    void notify_did_change();
    void set_all_cursors(TextPosition const&);

    TextPosition insert_at(TextPosition const&, u32, Client const* = nullptr);
    TextPosition insert_at(TextPosition const&, StringView, Client const* = nullptr);
    void remove(TextRange const&);

    virtual bool is_code_document() const { return false; }

    bool is_empty() const;
    bool is_modified() const { return m_undo_stack.is_current_modified(); }
    void set_unmodified();

protected:
    explicit TextDocument(Client* client);

private:
    void merge_span_collections();

    NonnullOwnPtrVector<TextDocumentLine> m_lines;
    HashMap<u32, Vector<TextDocumentSpan>> m_span_collections;
    Vector<TextDocumentSpan> m_spans;

    HashTable<Client*> m_clients;
    bool m_client_notifications_enabled { true };

    UndoStack m_undo_stack;

    RegexResult m_regex_result;
    size_t m_regex_result_match_index { 0 };
    size_t m_regex_result_match_capture_group_index { 0 };

    bool m_regex_needs_update { true };
    String m_regex_needle;
};

class TextDocumentLine {
public:
    explicit TextDocumentLine(TextDocument&);
    explicit TextDocumentLine(TextDocument&, StringView);

    String to_utf8() const;

    Utf32View view() const { return { code_points(), length() }; }
    u32 const* code_points() const { return m_text.data(); }
    size_t length() const { return m_text.size(); }
    bool set_text(TextDocument&, StringView);
    void set_text(TextDocument&, Vector<u32>);
    void append(TextDocument&, u32);
    void prepend(TextDocument&, u32);
    void insert(TextDocument&, size_t index, u32);
    void remove(TextDocument&, size_t index);
    void append(TextDocument&, u32 const*, size_t);
    void truncate(TextDocument&, size_t length);
    void clear(TextDocument&);
    void remove_range(TextDocument&, size_t start, size_t length);

    size_t first_non_whitespace_column() const;
    Optional<size_t> last_non_whitespace_column() const;
    bool ends_in_whitespace() const;
    bool can_select() const;
    bool is_empty() const { return length() == 0; }
    size_t leading_spaces() const;

private:
    // NOTE: This vector is null terminated.
    Vector<u32> m_text;
};

class TextDocumentUndoCommand : public Command {
public:
    TextDocumentUndoCommand(TextDocument&);
    virtual ~TextDocumentUndoCommand() = default;
    virtual void perform_formatting(TextDocument::Client const&) { }

    void execute_from(TextDocument::Client const& client)
    {
        m_client = &client;
        redo();
        m_client = nullptr;
    }

protected:
    bool commit_time_expired() const { return Time::now_monotonic() - m_timestamp >= COMMAND_COMMIT_TIME; }

    Time m_timestamp = Time::now_monotonic();
    TextDocument& m_document;
    TextDocument::Client const* m_client { nullptr };
};

class InsertTextCommand : public TextDocumentUndoCommand {
public:
    InsertTextCommand(TextDocument&, String const&, TextPosition const&);
    virtual void perform_formatting(TextDocument::Client const&) override;
    virtual void undo() override;
    virtual void redo() override;
    virtual bool merge_with(GUI::Command const&) override;
    virtual String action_text() const override;
    String const& text() const { return m_text; }
    TextRange const& range() const { return m_range; }

private:
    String m_text;
    TextRange m_range;
};

class RemoveTextCommand : public TextDocumentUndoCommand {
public:
    RemoveTextCommand(TextDocument&, String const&, TextRange const&);
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }
    virtual bool merge_with(GUI::Command const&) override;
    virtual String action_text() const override;

private:
    String m_text;
    TextRange m_range;
};

class ReplaceAllTextCommand final : public GUI::TextDocumentUndoCommand {

public:
    ReplaceAllTextCommand(GUI::TextDocument& document, String const& text, GUI::TextRange const& range, String const& action_text);
    void redo() override;
    void undo() override;
    bool merge_with(GUI::Command const&) override;
    String action_text() const override;
    String const& text() const { return m_text; }
    TextRange const& range() const { return m_range; }

private:
    String m_text;
    GUI::TextRange m_range;
    String m_action_text;
};

}
