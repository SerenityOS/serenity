#pragma once

#include <LibHTML/DOM/Document.h>
#include <LibDraw/Size.h>

class Frame {
public:
    Frame();
    ~Frame();

    Document* document() { return m_document.ptr(); }
    const Document* document() const { return m_document.ptr(); }

    void set_document(Document*);

    void layout();

private:
    RefPtr<StyledNode> generate_style_tree();
    RefPtr<LayoutNode> generate_layout_tree(const StyledNode&);

    RefPtr<Document> m_document;
    Size m_size;
};
