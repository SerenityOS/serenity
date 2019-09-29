#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/HTMLAnchorElement.h>
#include <LibHTML/Dump.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/RenderingContext.h>
#include <stdio.h>

HtmlView::HtmlView(GWidget* parent)
    : GScrollableWidget(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
    set_should_hide_unnecessary_scrollbars(true);
    set_background_color(Color::White);
}

void HtmlView::set_document(Document* document)
{
    if (document == m_document)
        return;
    m_document = document;

    if (document == nullptr)
        m_layout_root = nullptr;
    else
        m_layout_root = document->create_layout_tree(document->style_resolver(), nullptr);

#ifdef HTML_DEBUG
    if (document != nullptr) {
        printf("\033[33;1mLayout tree before layout:\033[0m\n");
        ::dump_tree(*m_layout_root);
    }
#endif

    layout_and_sync_size();
    update();
}

void HtmlView::layout_and_sync_size()
{
    if (!m_layout_root)
        return;

    m_layout_root->style().size().set_width(available_size().width());
    m_layout_root->layout();
    set_content_size(m_layout_root->rect().size());

#ifdef HTML_DEBUG
    printf("\033[33;1mLayout tree after layout:\033[0m\n");
    ::dump_tree(*m_layout_root);
#endif
}

void HtmlView::resize_event(GResizeEvent& event)
{
    GScrollableWidget::resize_event(event);
    layout_and_sync_size();
}

void HtmlView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), background_color());

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    if (!m_layout_root)
        return;

    RenderingContext context { painter };
    m_layout_root->render(context);
}

void HtmlView::mousemove_event(GMouseEvent& event)
{
    if (!m_layout_root)
        return GScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    auto result = m_layout_root->hit_test(event.position());
    if (result.layout_node) {
        auto* node = result.layout_node->node();
        hovered_node_changed = node != m_document->hovered_node();
        m_document->set_hovered_node(const_cast<Node*>(node));
#ifdef HTML_DEBUG
        if (node) {
            dbg() << "HtmlView: mousemove: " << node->tag_name() << "{" << node << "}";
            if (auto* link = node->enclosing_link_element()) {
                dbg() << "HtmlView: hovering over a link to " << link->href();
            }
        }
#endif
    }
    if (hovered_node_changed) {
        update();
        auto* hovered_html_element = m_document->hovered_node() ? m_document->hovered_node()->enclosing_html_element() : nullptr;
        if (hovered_html_element && !hovered_html_element->title().is_null()) {
            auto screen_position = screen_relative_rect().location().translated(event.position());
            GApplication::the().show_tooltip(hovered_html_element->title(), screen_position.translated(4, 4));
        } else {
            GApplication::the().hide_tooltip();
        }
    }
    event.accept();
}

void HtmlView::mousedown_event(GMouseEvent& event)
{
    if (!m_layout_root)
        return GScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    auto result = m_layout_root->hit_test(event.position());
    if (result.layout_node) {
        auto* node = result.layout_node->node();
        hovered_node_changed = node != m_document->hovered_node();
        m_document->set_hovered_node(const_cast<Node*>(node));
        if (node) {
#ifdef HTML_DEBUG
            dbg() << "HtmlView: mousedown: " << node->tag_name() << "{" << node << "}";
#endif
            if (auto* link = node->enclosing_link_element()) {
                dbg() << "HtmlView: clicking on a link to " << link->href();
                if (on_link_click)
                    on_link_click(link->href());
            }
        }
    }
    if (hovered_node_changed)
        update();
    event.accept();
}
