#pragma once

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
    const Font* font { nullptr };
};

class GTextDocument : public RefCounted<GTextDocument> {
public:
    class Client {
    public:
        virtual ~Client();
        virtual void document_did_append_line() = 0;
        virtual void document_did_insert_line(int) = 0;
        virtual void document_did_remove_line(int) = 0;
        virtual void document_did_remove_all_lines() = 0;
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
    explicit GTextDocumentLine();
    explicit GTextDocumentLine(const StringView&);

    StringView view() const { return { characters(), length() }; }
    const char* characters() const { return m_text.data(); }
    int length() const { return m_text.size() - 1; }
    void set_text(const StringView&);
    void append(char);
    void prepend(char);
    void insert(int index, char);
    void remove(int index);
    void append(const char*, int);
    void truncate(int length);
    void clear();
    int first_non_whitespace_column() const;

private:
    // NOTE: This vector is null terminated.
    Vector<char> m_text;
};
