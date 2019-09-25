#include <AK/Function.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <stdio.h>

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

    auto layout_root = m_document->create_layout_tree(m_document->style_resolver(), nullptr);

    layout_root->style().size().set_width(m_size.width());

    printf("\033[33;1mLayout tree before layout:\033[0m\n");
    dump_tree(*layout_root);

    layout_root->layout();

    printf("\033[33;1mLayout tree after layout:\033[0m\n");
    dump_tree(*layout_root);
}
