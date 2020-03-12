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

#include "Editor.h"
#include "EditorWrapper.h"
#include <AK/ByteBuffer.h>
#include <AK/FileSystemPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/SyntaxHighlighter.h>
#include <LibGUI/Window.h>
#include <LibMarkdown/MDDocument.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HtmlView.h>
#include <LibWeb/Parser/HTMLParser.h>

// #define EDITOR_DEBUG

Editor::Editor()
{
    m_documentation_tooltip_window = GUI::Window::construct();
    m_documentation_tooltip_window->set_rect(0, 0, 500, 400);
    m_documentation_tooltip_window->set_window_type(GUI::WindowType::Tooltip);
    m_documentation_html_view = m_documentation_tooltip_window->set_main_widget<Web::HtmlView>();
}

Editor::~Editor()
{
}

EditorWrapper& Editor::wrapper()
{
    return static_cast<EditorWrapper&>(*parent());
}
const EditorWrapper& Editor::wrapper() const
{
    return static_cast<const EditorWrapper&>(*parent());
}

void Editor::focusin_event(Core::Event& event)
{
    wrapper().set_editor_has_focus({}, true);
    if (on_focus)
        on_focus();
    GUI::TextEditor::focusin_event(event);
}

void Editor::focusout_event(Core::Event& event)
{
    wrapper().set_editor_has_focus({}, false);
    GUI::TextEditor::focusout_event(event);
}

void Editor::paint_event(GUI::PaintEvent& event)
{
    GUI::TextEditor::paint_event(event);

    if (is_focused()) {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());

        auto rect = frame_inner_rect();
        if (vertical_scrollbar().is_visible())
            rect.set_width(rect.width() - vertical_scrollbar().width());
        if (horizontal_scrollbar().is_visible())
            rect.set_height(rect.height() - horizontal_scrollbar().height());
        painter.draw_rect(rect, palette().selection());

        window()->set_override_cursor(m_hovering_link && m_holding_ctrl ? GUI::StandardCursor::Hand : GUI::StandardCursor::IBeam);
    }
}

static HashMap<String, String>& man_paths()
{
    static HashMap<String, String> paths;
    if (paths.is_empty()) {
        // FIXME: This should also search man3, possibly other places..
        Core::DirIterator it("/usr/share/man/man2", Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = String::format("/usr/share/man/man2/%s", it.next_path().characters());
            auto title = FileSystemPath(path).title();
            paths.set(title, path);
        }
    }

    return paths;
}

void Editor::show_documentation_tooltip_if_available(const String& hovered_token, const Gfx::Point& screen_location)
{
    auto it = man_paths().find(hovered_token);
    if (it == man_paths().end()) {
#ifdef EDITOR_DEBUG
        dbg() << "no man path for " << hovered_token;
#endif
        m_documentation_tooltip_window->hide();
        return;
    }

    if (m_documentation_tooltip_window->is_visible() && hovered_token == m_last_parsed_token) {
        return;
    }

#ifdef EDITOR_DEBUG
    dbg() << "opening " << it->value;
#endif
    auto file = Core::File::construct(it->value);
    if (!file->open(Core::File::ReadOnly)) {
        dbg() << "failed to open " << it->value << " " << file->error_string();
        return;
    }

    MDDocument man_document;
    bool success = man_document.parse(file->read_all());

    if (!success) {
        dbg() << "failed to parse markdown";
        return;
    }

    auto html_text = man_document.render_to_html();

    auto html_document = Web::parse_html_document(html_text);
    if (!html_document) {
        dbg() << "failed to parse HTML";
        return;
    }

    // FIXME: LibWeb needs a friendlier DOM manipulation API. Something like innerHTML :^)
    auto style_element = create_element(*html_document, "style");
    style_element->append_child(adopt(*new Web::Text(*html_document, "body { background-color: #dac7b5; }")));

    // FIXME: This const_cast should not be necessary.
    auto* head_element = const_cast<Web::HTMLHeadElement*>(html_document->head());
    ASSERT(head_element);
    head_element->append_child(style_element);

    m_documentation_html_view->set_document(html_document);
    m_documentation_tooltip_window->move_to(screen_location.translated(4, 4));
    m_documentation_tooltip_window->show();

    m_last_parsed_token = hovered_token;
}

void Editor::mousemove_event(GUI::MouseEvent& event)
{
    GUI::TextEditor::mousemove_event(event);

    if (document().spans().is_empty())
        return;

    auto text_position = text_position_at(event.position());
    if (!text_position.is_valid()) {
        m_documentation_tooltip_window->hide();
        return;
    }

    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter)
        return;

    bool hide_tooltip = true;
    bool is_over_link = false;

    for (auto& span : document().spans()) {
        if (span.range.contains(m_previous_text_position) && !span.range.contains(text_position)) {
            if (highlighter->is_navigatable(span.data) && span.is_underlined) {
                span.is_underlined = false;
                wrapper().editor().update();
            }
        }

        if (span.range.contains(text_position)) {
            auto adjusted_range = span.range;
            adjusted_range.end().set_column(adjusted_range.end().column() + 1);
            auto hovered_span_text = document().text_in_range(adjusted_range);
#ifdef EDITOR_DEBUG
            dbg() << "Hovering: " << adjusted_range << " \"" << hovered_span_text << "\"";
#endif

            if (highlighter->is_navigatable(span.data)) {
                is_over_link = true;
                bool was_underlined = span.is_underlined;
                span.is_underlined = event.modifiers() & Mod_Ctrl;
                if (span.is_underlined != was_underlined) {
                    wrapper().editor().update();
                }
            }
            if (highlighter->is_identifier(span.data)) {
                show_documentation_tooltip_if_available(hovered_span_text, event.position().translated(screen_relative_rect().location()));
                hide_tooltip = false;
            }
        }
    }

    m_previous_text_position = text_position;
    if (hide_tooltip)
        m_documentation_tooltip_window->hide();

    m_hovering_link = is_over_link && (event.modifiers() & Mod_Ctrl);
}

void Editor::mousedown_event(GUI::MouseEvent& event)
{
    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    if (!(event.modifiers() & Mod_Ctrl)) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    auto text_position = text_position_at(event.position());
    if (!text_position.is_valid()) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    for (auto& span : document().spans()) {
        if (span.range.contains(text_position)) {
            if (!highlighter->is_navigatable(span.data)) {
                GUI::TextEditor::mousedown_event(event);
                return;
            }

            auto adjusted_range = span.range;
            adjusted_range.end().set_column(adjusted_range.end().column() + 1);
            auto span_text = document().text_in_range(adjusted_range);
            auto header_path = span_text.substring(1, span_text.length() - 2);
#ifdef EDITOR_DEBUG
            dbg() << "Ctrl+click: " << adjusted_range << " \"" << header_path << "\"";
#endif
            navigate_to_include_if_available(header_path);
            return;
        }
    }

    GUI::TextEditor::mousedown_event(event);
}

void Editor::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Control)
        m_holding_ctrl = true;
    GUI::TextEditor::keydown_event(event);
}

void Editor::keyup_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Control)
        m_holding_ctrl = false;
    GUI::TextEditor::keyup_event(event);
}

static HashMap<String, String>& include_paths()
{
    static HashMap<String, String> paths;

    auto add_directory = [](String base, Optional<String> recursive, auto handle_directory) -> void {
        Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = it.next_full_path();
            if (!Core::File::is_directory(path)) {
                auto key = path.substring(base.length() + 1, path.length() - base.length() - 1);
#ifdef EDITOR_DEBUG
                dbg() << "Adding header \"" << key << "\" in path \"" << path << "\"";
#endif
                paths.set(key, path);
            } else {
                handle_directory(base, path, handle_directory);
            }
        }
    };

    if (paths.is_empty()) {
        add_directory(".", {}, add_directory);
        add_directory("/usr/local/include", {}, add_directory);
        add_directory("/usr/local/include/c++/9.2.0", {}, add_directory);
        add_directory("/usr/include", {}, add_directory);
    }

    return paths;
}

void Editor::navigate_to_include_if_available(String path)
{
    auto it = include_paths().find(path);
    if (it == include_paths().end()) {
#ifdef EDITOR_DEBUG
        dbg() << "no header " << path << " found.";
#endif
        return;
    }

    on_open(it->value);
}
