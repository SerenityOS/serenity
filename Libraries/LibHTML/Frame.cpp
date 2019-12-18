#include <LibHTML/DOM/Document.h>
#include <LibHTML/Frame.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutDocument.h>

Frame::Frame(HtmlView& html_view)
    : m_html_view(html_view.make_weak_ptr())
{
}

Frame::~Frame()
{
}

void Frame::set_document(Document* document)
{
    if (m_document == document)
        return;

    if (m_document)
        m_document->detach_from_frame({}, *this);

    m_document = document;

    if (m_document)
        m_document->attach_to_frame({}, *this);
}

void Frame::set_size(const Size& size)
{
    if (m_size == size)
        return;
    m_size = size;
}

void Frame::set_viewport_rect(const Rect& rect)
{
    if (m_viewport_rect == rect)
        return;
    m_viewport_rect = rect;

    if (m_document && m_document->layout_node())
        m_document->layout_node()->did_set_viewport_rect({}, rect);
}

void Frame::set_needs_display(const Rect& rect)
{
    if (!on_set_needs_display)
        return;
    on_set_needs_display(rect);
}
