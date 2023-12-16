/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/Time.h>
#include <AK/Utf32View.h>
#include <LibCore/Forward.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGUI/TextPosition.h>
#include <LibGUI/TextRange.h>
#include <LibGUI/UndoStack.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>
#include <LibGfx/TextAttributes.h>
#include <LibRegex/Regex.h>
#include <LibSyntax/Document.h>

namespace GUI {

using TextDocumentSpan = Syntax::TextDocumentSpan;
using TextDocumentFoldingRegion = Syntax::TextDocumentFoldingRegion;
using TextDocumentLine = Syntax::TextDocumentLine;

constexpr Duration COMMAND_COMMIT_TIME = Duration::from_milliseconds(400);

class TextDocument : public Syntax::Document {
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
    virtual TextDocumentLine const& line(size_t line_index) const override { return *m_lines[line_index]; }
    virtual TextDocumentLine& line(size_t line_index) override { return *m_lines[line_index]; }

    enum class IsNewDocument {
        No,
        Yes,
    };
    bool set_text(StringView, AllowCallback = AllowCallback::Yes, IsNewDocument = IsNewDocument::Yes);

    Vector<NonnullOwnPtr<TextDocumentLine>> const& lines() const { return m_lines; }
    Vector<NonnullOwnPtr<TextDocumentLine>>& lines() { return m_lines; }

    void append_line(NonnullOwnPtr<TextDocumentLine>);
    NonnullOwnPtr<TextDocumentLine> take_line(size_t line_index);
    void remove_line(size_t line_index);
    void remove_all_lines();
    void insert_line(size_t line_index, NonnullOwnPtr<TextDocumentLine>);

    void register_client(Client&);
    void unregister_client(Client&);

    virtual void update_views(Badge<TextDocumentLine>) override;

    ByteString text() const;
    ByteString text_in_range(TextRange const&) const;

    Vector<TextRange> find_all(StringView needle, bool regmatch = false, bool match_case = true);

    void update_regex_matches(StringView);
    TextRange find_next(StringView, TextPosition const& start = {}, SearchShouldWrap = SearchShouldWrap::Yes, bool regmatch = false, bool match_case = true);
    TextRange find_previous(StringView, TextPosition const& start = {}, SearchShouldWrap = SearchShouldWrap::Yes, bool regmatch = false, bool match_case = true);

    TextPosition next_position_after(TextPosition const&, SearchShouldWrap = SearchShouldWrap::Yes) const;
    TextPosition previous_position_before(TextPosition const&, SearchShouldWrap = SearchShouldWrap::Yes) const;

    size_t get_next_grapheme_cluster_boundary(TextPosition const& cursor) const;
    size_t get_previous_grapheme_cluster_boundary(TextPosition const& cursor) const;

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

    bool is_empty() const;
    bool is_modified() const { return m_undo_stack.is_current_modified(); }
    void set_unmodified();

protected:
    explicit TextDocument(Client* client);

private:
    Vector<NonnullOwnPtr<TextDocumentLine>> m_lines;

    HashTable<Client*> m_clients;
    bool m_client_notifications_enabled { true };

    UndoStack m_undo_stack;

    RegexResult m_regex_result;
    size_t m_regex_result_match_index { 0 };
    size_t m_regex_result_match_capture_group_index { 0 };

    bool m_regex_needs_update { true };
    ByteString m_regex_needle;
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
    bool commit_time_expired() const { return MonotonicTime::now() - m_timestamp >= COMMAND_COMMIT_TIME; }

    MonotonicTime m_timestamp = MonotonicTime::now();
    TextDocument& m_document;
    TextDocument::Client const* m_client { nullptr };
};

class InsertTextCommand : public TextDocumentUndoCommand {
public:
    InsertTextCommand(TextDocument&, ByteString const&, TextPosition const&);
    virtual ~InsertTextCommand() = default;
    virtual void perform_formatting(TextDocument::Client const&) override;
    virtual void undo() override;
    virtual void redo() override;
    virtual bool merge_with(GUI::Command const&) override;
    virtual ByteString action_text() const override;
    ByteString const& text() const { return m_text; }
    TextRange const& range() const { return m_range; }

private:
    ByteString m_text;
    TextRange m_range;
};

class RemoveTextCommand : public TextDocumentUndoCommand {
public:
    RemoveTextCommand(TextDocument&, ByteString const&, TextRange const&, TextPosition const&);
    virtual ~RemoveTextCommand() = default;
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }
    virtual bool merge_with(GUI::Command const&) override;
    virtual ByteString action_text() const override;

private:
    ByteString m_text;
    TextRange m_range;
    TextPosition m_original_cursor_position;
};

class InsertLineCommand : public TextDocumentUndoCommand {
public:
    enum class InsertPosition {
        Above,
        Below,
    };

    InsertLineCommand(TextDocument&, TextPosition, ByteString&&, InsertPosition);
    virtual ~InsertLineCommand() = default;
    virtual void undo() override;
    virtual void redo() override;
    virtual ByteString action_text() const override;

private:
    size_t compute_line_number() const;

    TextPosition m_cursor;
    ByteString m_text;
    InsertPosition m_pos;
};

class ReplaceAllTextCommand final : public GUI::TextDocumentUndoCommand {

public:
    ReplaceAllTextCommand(GUI::TextDocument& document, ByteString const& new_text, ByteString const& action_text);
    virtual ~ReplaceAllTextCommand() = default;
    void redo() override;
    void undo() override;
    bool merge_with(GUI::Command const&) override;
    ByteString action_text() const override;
    ByteString const& text() const { return m_new_text; }

private:
    ByteString m_original_text;
    ByteString m_new_text;
    ByteString m_action_text;
};

class IndentSelection : public TextDocumentUndoCommand {
public:
    IndentSelection(TextDocument&, size_t tab_width, TextRange const&);
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }

private:
    size_t m_tab_width { 0 };
    TextRange m_range;
};

class UnindentSelection : public TextDocumentUndoCommand {
public:
    UnindentSelection(TextDocument&, size_t tab_width, TextRange const&);
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }

private:
    size_t m_tab_width { 0 };
    TextRange m_range;
};

class CommentSelection : public TextDocumentUndoCommand {
public:
    CommentSelection(TextDocument&, StringView, StringView, TextRange const&);
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }

private:
    StringView m_prefix;
    StringView m_suffix;
    TextRange m_range;
};

class UncommentSelection : public TextDocumentUndoCommand {
public:
    UncommentSelection(TextDocument&, StringView, StringView, TextRange const&);
    virtual void undo() override;
    virtual void redo() override;
    TextRange const& range() const { return m_range; }

private:
    StringView m_prefix;
    StringView m_suffix;
    TextRange m_range;
};

}
