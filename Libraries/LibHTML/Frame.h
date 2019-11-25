#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <LibDraw/Rect.h>
#include <LibDraw/Size.h>
#include <LibHTML/TreeNode.h>

class Document;
class HtmlView;

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create(HtmlView& html_view) { return adopt(*new Frame(html_view)); }
    ~Frame();

    const Document* document() const { return m_document; }
    Document* document() { return m_document; }

    void set_document(Document*);

    HtmlView* html_view() { return m_html_view; }
    const HtmlView* html_view() const { return m_html_view; }

    const Size& size() const { return m_size; }
    void set_size(const Size&);

    void set_needs_display(const Rect&);
    Function<void(const Rect&)> on_set_needs_display;

private:
    explicit Frame(HtmlView&);

    WeakPtr<HtmlView> m_html_view;
    RefPtr<Document> m_document;
    Size m_size;
};
