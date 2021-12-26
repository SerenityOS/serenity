#pragma once

#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibCore/CTimer.h>
#include <LibDraw/Color.h>
#include <LibDraw/Font.h>
#include <LibGUI/GTextRange.h>
#include <LibGUI/GUndoStack.h>

class GTextEditor;
class GTextDocument;
class GTextDocumentLine;

struct GTextDocumentSpan {
    GTextRange range;
    Color color;
    Optional<Color> background_color;
    bool is_skippable { false };
    const Font* font { nullptr };
    void* data { nullptr };
};

class GTextDocumentUndoCommand : public GCommand {
public:
    GTextDocumentUndoCommand(GTextDocument&);
    virtual ~GTextDocumentUndoCommand();

protected:
    GTextDocument& m_document;
};

class InsertTextCommand : public GTextDocumentUndoCommand {
public:
    InsertTextCommand(GTextDocument&, const String&, const GTextPosition&);
    virtual void undo() override;
    virtual void redo() override;

private:
    String m_text;
    GTextRange m_range;
};

class RemoveTextCommand : public GTextDocumentUndoCommand {
public:
    RemoveTextCommand(GTextDocument&, const String&, const GTextRange&);
    virtual void undo() override;
    virtual void redo() override;

private:
    String m_text;
    GTextRange m_range;
};

class GTextDocument : public RefCounted<GTextDocument> {
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
        virtual void document_did_set_cursor(const GTextPosition&) = 0;
    };

    static NonnullRefPtr<GTextDocument> create(Client* client = nullptr)
    {
        return adopt(*new GTextDocument(client));
    }

    size_t line_count() const { return (size_t)m_lines.size(); }
    const GTextDocumentLine& line(size_t line_index) const { return m_lines[(int)line_index]; }
    GTextDocumentLine& line(size_t line_index) { return m_lines[(int)line_index]; }

    void set_spans(const Vector<GTextDocumentSpan>& spans) { m_spans = spans; }

    void set_text(const StringView&);

    const NonnullOwnPtrVector<GTextDocumentLine>& lines() const { return m_lines; }
    NonnullOwnPtrVector<GTextDocumentLine>& lines() { return m_lines; }

    bool has_spans() const { return !m_spans.is_empty(); }
    const Vector<GTextDocumentSpan>& spans() const { return m_spans; }
    void set_span_at_index(size_t index, GTextDocumentSpan span) { m_spans[(int)index] = move(span); }

    void append_line(NonnullOwnPtr<GTextDocumentLine>);
    void remove_line(size_t line_index);
    void remove_all_lines();
    void insert_line(size_t line_index, NonnullOwnPtr<GTextDocumentLine>);

    void register_client(Client&);
    void unregister_client(Client&);

    void update_views(Badge<GTextDocumentLine>);

    String text_in_range(const GTextRange&) const;

    Vector<GTextRange> find_all(const StringView& needle) const;

    GTextRange find_next(const StringView&, const GTextPosition& start = {}, SearchShouldWrap = SearchShouldWrap::Yes) const;
    GTextRange find_previous(const StringView&, const GTextPosition& start = {}, SearchShouldWrap = SearchShouldWrap::Yes) const;

    GTextPosition next_position_after(const GTextPosition&, SearchShouldWrap = SearchShouldWrap::Yes) const;
    GTextPosition previous_position_before(const GTextPosition&, SearchShouldWrap = SearchShouldWrap::Yes) const;

    char character_at(const GTextPosition&) const;

    GTextRange range_for_entire_line(size_t line_index) const;

    Optional<GTextDocumentSpan> first_non_skippable_span_before(const GTextPosition&) const;
    Optional<GTextDocumentSpan> first_non_skippable_span_after(const GTextPosition&) const;

    void add_to_undo_stack(NonnullOwnPtr<GTextDocumentUndoCommand>);

    bool can_undo() const { return m_undo_stack.can_undo(); }
    bool can_redo() const { return m_undo_stack.can_redo(); }
    void undo();
    void redo();

    void notify_did_change();
    void set_all_cursors(const GTextPosition&);

    GTextPosition insert_at(const GTextPosition&, char);
    GTextPosition insert_at(const GTextPosition&, const StringView&);
    void remove(const GTextRange&);

private:
    explicit GTextDocument(Client* client);

    void update_undo_timer();

    NonnullOwnPtrVector<GTextDocumentLine> m_lines;
    Vector<GTextDocumentSpan> m_spans;

    HashTable<Client*> m_clients;
    bool m_client_notifications_enabled { true };

    GUndoStack m_undo_stack;
    RefPtr<CTimer> m_undo_timer;
};

class GTextDocumentLine {
    friend class GTextEditor;
    friend class GTextDocument;

public:
    explicit GTextDocumentLine(GTextDocument&);
    explicit GTextDocumentLine(GTextDocument&, const StringView&);

    StringView view() const { return { characters(), (size_t)length() }; }
    const char* characters() const { return m_text.data(); }
    size_t length() const { return (size_t)m_text.size() - 1; }
    void set_text(GTextDocument&, const StringView&);
    void append(GTextDocument&, char);
    void prepend(GTextDocument&, char);
    void insert(GTextDocument&, size_t index, char);
    void remove(GTextDocument&, size_t index);
    void append(GTextDocument&, const char*, size_t);
    void truncate(GTextDocument&, size_t length);
    void clear(GTextDocument&);
    size_t first_non_whitespace_column() const;

private:
    // NOTE: This vector is null terminated.
    Vector<char> m_text;
};
