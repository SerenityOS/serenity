#pragma once

#include <LibDraw/Size.h>
#include <LibHTML/DOM/Document.h>

class Frame {
public:
    Frame();
    ~Frame();

    Document* document() { return m_document.ptr(); }
    const Document* document() const { return m_document.ptr(); }

    void set_document(Document*);

    void layout();

private:
    RefPtr<LayoutNode> generate_layout_tree();

    RefPtr<Document> m_document;
    Size m_size;
};
