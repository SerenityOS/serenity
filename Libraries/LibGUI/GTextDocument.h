#pragma once

#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibDraw/Color.h>
#include <LibDraw/Font.h>
#include <LibGUI/GTextRange.h>

class GTextEditor;
class GTextDocumentLine;

struct GTextDocumentSpan {
    GTextRange range;
    Color color;
    bool is_skippable { false };
    const Font* font { nullptr };
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
        virtual void document_did_insert_line(int) = 0;
        virtual void document_did_remove_line(int) = 0;
        virtual void document_did_remove_all_lines() = 0;
        virtual void document_did_change() = 0;
    };

    static NonnullRefPtr<GTextDocument> create(Client* client = nullptr)
    {
        return adopt(*new GTextDocument(client));
    }

    int line_count() const { return m_lines.size(); }
    const GTextDocumentLine& line(int line_index) const { return m_lines[line_index]; }
    GTextDocumentLine& line(int line_index) { return m_lines[line_index]; }

    void set_spans(const Vector<GTextDocumentSpan>& spans) { m_spans = spans; }

    void set_text(const StringView&);

    const NonnullOwnPtrVector<GTextDocumentLine>& lines() const { return m_lines; }
    NonnullOwnPtrVector<GTextDocumentLine>& lines() { return m_lines; }

    bool has_spans() const { return !m_spans.is_empty(); }
    const Vector<GTextDocumentSpan>& spans() const { return m_spans; }

    void append_line(NonnullOwnPtr<GTextDocumentLine>);
    void remove_line(int line_index);
    void remove_all_lines();
    void insert_line(int line_index, NonnullOwnPtr<GTextDocumentLine>);

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

    Optional<GTextDocumentSpan> first_non_skippable_span_before(const GTextPosition&) const;
    Optional<GTextDocumentSpan> first_non_skippable_span_after(const GTextPosition&) const;

private:
    explicit GTextDocument(Client* client);

    NonnullOwnPtrVector<GTextDocumentLine> m_lines;
    Vector<GTextDocumentSpan> m_spans;

    HashTable<Client*> m_clients;
};

class GTextDocumentLine {
    friend class GTextEditor;
    friend class GTextDocument;

public:
    explicit GTextDocumentLine(GTextDocument&);
    explicit GTextDocumentLine(GTextDocument&, const StringView&);

    StringView view() const { return { characters(), length() }; }
    const char* characters() const { return m_text.data(); }
    int length() const { return m_text.size() - 1; }
    void set_text(GTextDocument&, const StringView&);
    void append(GTextDocument&, char);
    void prepend(GTextDocument&, char);
    void insert(GTextDocument&, int index, char);
    void remove(GTextDocument&, int index);
    void append(GTextDocument&, const char*, int);
    void truncate(GTextDocument&, int length);
    void clear(GTextDocument&);
    int first_non_whitespace_column() const;

private:
    // NOTE: This vector is null terminated.
    Vector<char> m_text;
};
