#pragma once

#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <LibDraw/Size.h>
#include <LibHTML/TreeNode.h>

class Document;

class Frame
    : public TreeNode<Frame>
    , public Weakable<Frame> {
public:
    static NonnullRefPtr<Frame> create() { return adopt(*new Frame); }
    ~Frame();

    const Document* document() const { return m_document; }
    Document* document() { return m_document; }

    void set_document(Document*);

    const Size& size() const { return m_size; }
    void set_size(const Size&);

private:
    Frame();

    RefPtr<Document> m_document;
    Size m_size;
};
