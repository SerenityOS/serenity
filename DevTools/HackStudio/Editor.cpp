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
#include "CppLexer.h"
#include "EditorWrapper.h"
#include <AK/FileSystemPath.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CFile.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GWindow.h>
#include <LibHTML/DOM/ElementFactory.h>
#include <LibHTML/DOM/HTMLHeadElement.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibMarkdown/MDDocument.h>

//#define EDITOR_DEBUG

Editor::Editor(GUI::Widget* parent)
    : TextEditor(GUI::TextEditor::MultiLine, parent)
{
    m_documentation_tooltip_window = GUI::Window::construct();
    m_documentation_tooltip_window->set_rect(0, 0, 500, 400);
    m_documentation_tooltip_window->set_window_type(GUI::WindowType::Tooltip);

    m_documentation_html_view = HtmlView::construct(nullptr);
    m_documentation_tooltip_window->set_main_widget(m_documentation_html_view);
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

void Editor::show_documentation_tooltip_if_available(const String& hovered_token, const Point& screen_location)
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

    auto html_document = parse_html_document(html_text);
    if (!html_document) {
        dbg() << "failed to parse HTML";
        return;
    }

    // FIXME: LibHTML needs a friendlier DOM manipulation API. Something like innerHTML :^)
    auto style_element = create_element(*html_document, "style");
    style_element->append_child(adopt(*new Text(*html_document, "body { background-color: #dac7b5; }")));

    // FIXME: This const_cast should not be necessary.
    auto* head_element = const_cast<HTMLHeadElement*>(html_document->head());
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
        GUI::Application::the().hide_tooltip();
        return;
    }

    for (auto& span : document().spans()) {
        if (span.range.contains(text_position)) {
            auto adjusted_range = span.range;
            adjusted_range.end().set_column(adjusted_range.end().column() + 1);
            auto hovered_span_text = document().text_in_range(adjusted_range);
#ifdef EDITOR_DEBUG
            dbg() << "Hovering: " << adjusted_range << " \"" << hovered_span_text << "\"";
#endif
            show_documentation_tooltip_if_available(hovered_span_text, event.position().translated(screen_relative_rect().location()));
            return;
        }
    }
    GUI::Application::the().hide_tooltip();
}

void Editor::highlight_matching_token_pair()
{
    enum class Direction {
        Forward,
        Backward,
    };

    auto find_span_of_type = [&](int i, CppToken::Type type, CppToken::Type not_type, Direction direction) {
        int nesting_level = 0;
        bool forward = direction == Direction::Forward;
        for (forward ? ++i : --i; forward ? (i < document().spans().size()) : (i >= 0); forward ? ++i : --i) {
            auto& span = document().spans().at(i);
            auto span_token_type = (CppToken::Type)((uintptr_t)span.data);
            if (span_token_type == not_type) {
                ++nesting_level;
            } else if (span_token_type == type) {
                if (nesting_level-- <= 0)
                    return i;
            }
        }
        return -1;
    };

    auto make_buddies = [&](int index0, int index1) {
        auto& buddy0 = const_cast<GUI::TextDocumentSpan&>(document().spans()[index0]);
        auto& buddy1 = const_cast<GUI::TextDocumentSpan&>(document().spans()[index1]);
        m_has_brace_buddies = true;
        m_brace_buddies[0].index = index0;
        m_brace_buddies[1].index = index1;
        m_brace_buddies[0].span_backup = buddy0;
        m_brace_buddies[1].span_backup = buddy1;
        buddy0.background_color = Color::DarkCyan;
        buddy1.background_color = Color::DarkCyan;
        buddy0.color = Color::White;
        buddy1.color = Color::White;
        update();
    };

    struct MatchingTokenPair {
        CppToken::Type open;
        CppToken::Type close;
    };

    MatchingTokenPair pairs[] = {
        { CppToken::Type::LeftCurly, CppToken::Type::RightCurly },
        { CppToken::Type::LeftParen, CppToken::Type::RightParen },
        { CppToken::Type::LeftBracket, CppToken::Type::RightBracket },
    };

    for (int i = 0; i < document().spans().size(); ++i) {
        auto& span = const_cast<GUI::TextDocumentSpan&>(document().spans().at(i));
        auto token_type = (CppToken::Type)((uintptr_t)span.data);

        for (auto& pair : pairs) {
            if (token_type == pair.open && span.range.start() == cursor()) {
                auto buddy = find_span_of_type(i, pair.close, pair.open, Direction::Forward);
                if (buddy != -1)
                    make_buddies(i, buddy);
                return;
            }
        }

        auto right_of_end = span.range.end();
        right_of_end.set_column(right_of_end.column() + 1);

        for (auto& pair : pairs) {
            if (token_type == pair.close && right_of_end == cursor()) {
                auto buddy = find_span_of_type(i, pair.open, pair.close, Direction::Backward);
                if (buddy != -1)
                    make_buddies(i, buddy);
                return;
            }
        }
    }
}

void Editor::cursor_did_change()
{
    if (m_has_brace_buddies) {
        if (m_brace_buddies[0].index >= 0 && m_brace_buddies[0].index < document().spans().size())
            document().set_span_at_index(m_brace_buddies[0].index, m_brace_buddies[0].span_backup);
        if (m_brace_buddies[1].index >= 0 && m_brace_buddies[1].index < document().spans().size())
            document().set_span_at_index(m_brace_buddies[1].index, m_brace_buddies[1].span_backup);
        m_has_brace_buddies = false;
        update();
    }
    highlight_matching_token_pair();
}

void Editor::notify_did_rehighlight()
{
    m_has_brace_buddies = false;
    highlight_matching_token_pair();
}
