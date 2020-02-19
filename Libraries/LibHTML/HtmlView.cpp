/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FileSystemPath.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/PNGLoader.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/ElementFactory.h>
#include <LibHTML/DOM/HTMLAnchorElement.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibHTML/RenderingContext.h>
#include <LibHTML/ResourceLoader.h>
#include <stdio.h>

HtmlView::HtmlView(GUI::Widget* parent)
    : GUI::ScrollableWidget(parent)
    , m_main_frame(::Frame::create(*this))
{
    main_frame().on_set_needs_display = [this](auto& content_rect) {
        if (content_rect.is_empty()) {
            update();
            return;
        }
        Gfx::Rect adjusted_rect = content_rect;
        adjusted_rect.set_location(to_widget_position(content_rect.location()));
        update(adjusted_rect);
    };

    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(2);
    set_should_hide_unnecessary_scrollbars(true);
    set_background_role(ColorRole::Base);
}

HtmlView::~HtmlView()
{
}

void HtmlView::set_document(Document* new_document)
{
    RefPtr<Document> old_document = document();

    if (new_document == old_document)
        return;

    if (old_document)
        old_document->on_layout_updated = nullptr;

    main_frame().set_document(new_document);

    if (new_document) {
        new_document->on_layout_updated = [this] {
            layout_and_sync_size();
            update();
        };
    }

#ifdef HTML_DEBUG
    if (document != nullptr) {
        dbgprintf("\033[33;1mLayout tree before layout:\033[0m\n");
        ::dump_tree(*layout_root());
    }
#endif

    layout_and_sync_size();
    update();
}

void HtmlView::layout_and_sync_size()
{
    if (!document())
        return;

    bool had_vertical_scrollbar = vertical_scrollbar().is_visible();
    bool had_horizontal_scrollbar = horizontal_scrollbar().is_visible();

    main_frame().set_size(available_size());
    document()->layout();
    set_content_size(enclosing_int_rect(layout_root()->rect()).size());

    // NOTE: If layout caused us to gain or lose scrollbars, we have to lay out again
    //       since the scrollbars now take up some of the available space.
    if (had_vertical_scrollbar != vertical_scrollbar().is_visible() || had_horizontal_scrollbar != horizontal_scrollbar().is_visible()) {
        main_frame().set_size(available_size());
        document()->layout();
        set_content_size(enclosing_int_rect(layout_root()->rect()).size());
    }

    main_frame().set_viewport_rect(visible_content_rect());

#ifdef HTML_DEBUG
    dbgprintf("\033[33;1mLayout tree after layout:\033[0m\n");
    ::dump_tree(*layout_root());
#endif
}

void HtmlView::resize_event(GUI::ResizeEvent& event)
{
    GUI::ScrollableWidget::resize_event(event);
    layout_and_sync_size();
}

void HtmlView::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    if (!layout_root()) {
        painter.fill_rect(event.rect(), palette().color(background_role()));
        return;
    }

    painter.fill_rect(event.rect(), document()->background_color(palette()));

    if (auto background_bitmap = document()->background_image()) {
        painter.draw_tiled_bitmap(event.rect(), *background_bitmap);
    }

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    RenderingContext context(painter, palette());
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(visible_content_rect());
    layout_root()->render(context);
}

void HtmlView::mousemove_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    bool is_hovering_link = false;
    bool was_hovering_link = document()->hovered_node() && document()->hovered_node()->is_link();
    auto result = layout_root()->hit_test(to_content_position(event.position()));
    const HTMLAnchorElement* hovered_link_element = nullptr;
    if (result.layout_node) {
        auto* node = result.layout_node->node();
        hovered_node_changed = node != document()->hovered_node();
        document()->set_hovered_node(const_cast<Node*>(node));
        if (node) {
            hovered_link_element = node->enclosing_link_element();
            if (hovered_link_element) {
#ifdef HTML_DEBUG
                dbg() << "HtmlView: hovering over a link to " << hovered_link_element->href();
#endif
                is_hovering_link = true;
            }
        }
        if (m_in_mouse_selection) {
            layout_root()->selection().set_end({ result.layout_node, result.index_in_node });
            dump_selection("MouseMove");
            update();
        }
    }
    if (window())
        window()->set_override_cursor(is_hovering_link ? GUI::StandardCursor::Hand : GUI::StandardCursor::None);
    if (hovered_node_changed) {
        update();
        auto* hovered_html_element = document()->hovered_node() ? document()->hovered_node()->enclosing_html_element() : nullptr;
        if (hovered_html_element && !hovered_html_element->title().is_null()) {
            auto screen_position = screen_relative_rect().location().translated(event.position());
            GUI::Application::the().show_tooltip(hovered_html_element->title(), screen_position.translated(4, 4));
        } else {
            GUI::Application::the().hide_tooltip();
        }
    }
    if (is_hovering_link != was_hovering_link) {
        if (on_link_hover) {
            on_link_hover(hovered_link_element ? document()->complete_url(hovered_link_element->href()).to_string() : String());
        }
    }
    event.accept();
}

void HtmlView::mousedown_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mousemove_event(event);

    bool hovered_node_changed = false;
    auto result = layout_root()->hit_test(to_content_position(event.position()));
    if (result.layout_node) {
        auto* node = result.layout_node->node();
        hovered_node_changed = node != document()->hovered_node();
        document()->set_hovered_node(const_cast<Node*>(node));
        if (node) {
            if (auto* link = node->enclosing_link_element()) {
                dbg() << "HtmlView: clicking on a link to " << link->href();
                if (on_link_click)
                    on_link_click(link->href());
            } else {
                if (event.button() == GUI::MouseButton::Left) {
                    layout_root()->selection().set({ result.layout_node, result.index_in_node }, {});
                    dump_selection("MouseDown");
                    m_in_mouse_selection = true;
                }
            }
        }
    }
    if (hovered_node_changed)
        update();
    event.accept();
}

void HtmlView::mouseup_event(GUI::MouseEvent& event)
{
    if (!layout_root())
        return GUI::ScrollableWidget::mouseup_event(event);

    if (event.button() == GUI::MouseButton::Left) {
        dump_selection("MouseUp");
        m_in_mouse_selection = false;
    }
}

void HtmlView::keydown_event(GUI::KeyEvent& event)
{
    if (event.modifiers() == 0) {
        switch (event.key()) {
        case Key_Home:
            vertical_scrollbar().set_value(0);
            break;
        case Key_End:
            vertical_scrollbar().set_value(vertical_scrollbar().max());
            break;
        case Key_Down:
            vertical_scrollbar().set_value(vertical_scrollbar().value() + vertical_scrollbar().step());
            break;
        case Key_Up:
            vertical_scrollbar().set_value(vertical_scrollbar().value() - vertical_scrollbar().step());
            break;
        case Key_Left:
            horizontal_scrollbar().set_value(horizontal_scrollbar().value() + horizontal_scrollbar().step());
            break;
        case Key_Right:
            horizontal_scrollbar().set_value(horizontal_scrollbar().value() - horizontal_scrollbar().step());
            break;
        case Key_PageDown:
            vertical_scrollbar().set_value(vertical_scrollbar().value() + frame_inner_rect().height());
            break;
        case Key_PageUp:
            vertical_scrollbar().set_value(vertical_scrollbar().value() - frame_inner_rect().height());
            break;
        }
    }

    event.accept();
}

void HtmlView::reload()
{
    load(main_frame().document()->url());
}

static RefPtr<Document> create_image_document(const ByteBuffer& data, const URL& url)
{
    auto document = adopt(*new Document);
    document->set_url(url);

    auto bitmap = Gfx::load_png_from_memory(data.data(), data.size());
    ASSERT(bitmap);

    auto html_element = create_element(document, "html");
    document->append_child(html_element);

    auto head_element = create_element(document, "head");
    html_element->append_child(head_element);
    auto title_element = create_element(document, "title");
    head_element->append_child(title_element);

    auto basename = FileSystemPath(url.path()).basename();
    auto title_text = adopt(*new Text(document, String::format("%s [%dx%d]", basename.characters(), bitmap->width(), bitmap->height())));
    title_element->append_child(title_text);

    auto body_element = create_element(document, "body");
    html_element->append_child(body_element);

    auto image_element = create_element(document, "img");
    image_element->set_attribute("src", url.to_string());
    body_element->append_child(image_element);

    return document;
}

void HtmlView::load(const URL& url)
{
    dbg() << "HtmlView::load: " << url;

    if (window())
        window()->set_override_cursor(GUI::StandardCursor::None);

    if (on_load_start)
        on_load_start(url);

    ResourceLoader::the().load(url, [this, url](auto data) {
        if (data.is_null()) {
            dbg() << "Load failed!";
            ASSERT_NOT_REACHED();
        }

        RefPtr<Document> document;
        if (url.path().ends_with(".png")) {
            document = create_image_document(data, url);
        } else {
            document = parse_html_document(data, url);
        }
        ASSERT(document);
        set_document(document);
        if (on_title_change)
            on_title_change(document->title());
    });
}

const LayoutDocument* HtmlView::layout_root() const
{
    return document() ? document()->layout_node() : nullptr;
}

LayoutDocument* HtmlView::layout_root()
{
    if (!document())
        return nullptr;
    return const_cast<LayoutDocument*>(document()->layout_node());
}

void HtmlView::scroll_to_anchor(const StringView& name)
{
    if (!document())
        return;

    auto* element = document()->get_element_by_id(name);
    if (!element) {
        auto candidates = document()->get_elements_by_name(name);
        for (auto* candidate : candidates) {
            if (is<HTMLAnchorElement>(*candidate)) {
                element = to<HTMLAnchorElement>(candidate);
                break;
            }
        }
    }

    if (!element) {
        dbg() << "HtmlView::scroll_to_anchor(): Anchor not found: '" << name << "'";
        return;
    }
    if (!element->layout_node()) {
        dbg() << "HtmlView::scroll_to_anchor(): Anchor found but without layout node: '" << name << "'";
        return;
    }
    auto& layout_node = *element->layout_node();
    Gfx::FloatRect float_rect { layout_node.box_type_agnostic_position(), { (float)visible_content_rect().width(), (float)visible_content_rect().height() } };
    scroll_into_view(enclosing_int_rect(float_rect), true, true);
    window()->set_override_cursor(GUI::StandardCursor::None);
}

Document* HtmlView::document()
{
    return main_frame().document();
}

const Document* HtmlView::document() const
{
    return main_frame().document();
}

void HtmlView::dump_selection(const char* event_name)
{
    dbg() << event_name << " selection start: "
          << layout_root()->selection().start().layout_node << ":" << layout_root()->selection().start().index_in_node << ", end: "
          << layout_root()->selection().end().layout_node << ":" << layout_root()->selection().end().index_in_node;
}

void HtmlView::did_scroll()
{
    main_frame().set_viewport_rect(visible_content_rect());
}
