#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <LibDraw/Rect.h>
#include <LibDraw/Size.h>
#include <LibHTML/TreeNode.h>

class Document;

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create() { return adopt(*new Frame); }
    ~Frame();

    const Document* document() const { return m_document; }
    Document* document() { return m_document; }

    void set_document(Document*);

    const Size& size() const { return m_size; }
    void set_size(const Size&);

    void set_needs_display(const Rect&);
    Function<void(const Rect&)> on_set_needs_display;

private:
    Frame();

    RefPtr<Document> m_document;
    Size m_size;
};
