#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutNode.h>

Frame::Frame()
    : m_size(800, 600)
{
}

Frame::~Frame()
{
}

void Frame::set_document(Document* document)
{
    m_document = document;
}

void Frame::layout()
{
    if (!m_document)
        return;

    if (!m_document->layout_node())
        m_document->create_layout_node();

    ASSERT(m_document->layout_node());

    m_document->layout_node()->style().size().set_width(m_size.width());

    m_document->layout_node()->layout();
}
