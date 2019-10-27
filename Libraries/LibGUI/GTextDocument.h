#pragma once

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
    static NonnullRefPtr<GTextDocument> create(GTextEditor& editor)
    {
        return adopt(*new GTextDocument(editor));
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

private:
    explicit GTextDocument(GTextEditor&);

    NonnullOwnPtrVector<GTextDocumentLine> m_lines;
    Vector<GTextDocumentSpan> m_spans;

    GTextEditor& m_editor;
};

class GTextDocumentLine {
    friend class GTextEditor;
    friend class GTextDocument;

public:
    explicit GTextDocumentLine(GTextEditor&);
    GTextDocumentLine(GTextEditor&, const StringView&);

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
    void recompute_visual_lines();
    int visual_line_containing(int column) const;
    int first_non_whitespace_column() const;

    template<typename Callback>
    void for_each_visual_line(Callback) const;

private:
    GTextEditor& m_editor;

    // NOTE: This vector is null terminated.
    Vector<char> m_text;

    Vector<int, 1> m_visual_line_breaks;
    Rect m_visual_rect;
};
