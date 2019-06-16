#pragma once

#include <LibHTML/DOM/Document.h>
#include <SharedGraphics/Size.h>

class Frame {
public:
    Frame();
    ~Frame();

    Document* document() { return m_document.ptr(); }
    const Document* document() const { return m_document.ptr(); }

    void set_document(Document*);

    void layout();

private:
    RetainPtr<Document> m_document;
    Size m_size;
};
