/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, the SerenityOS developers.
 * Copyright (c) 2023-2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Editor.h"
#include "Debugger/Debugger.h"
#include "EditorWrapper.h"
#include "HackStudio.h"
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <LibCMake/CMakeCache/SyntaxHighlighter.h>
#include <LibCMake/SyntaxHighlighter.h>
#include <LibConfig/Client.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Timer.h>
#include <LibCpp/SemanticSyntaxHighlighter.h>
#include <LibCpp/SyntaxHighlighter.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/GML/AutocompleteProvider.h>
#include <LibGUI/GML/SyntaxHighlighter.h>
#include <LibGUI/GitCommitSyntaxHighlighter.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/SyntaxHighlighter.h>
#include <LibSQL/AST/SyntaxHighlighter.h>
#include <LibShell/SyntaxHighlighter.h>
#include <LibSyntax/Language.h>
#include <LibWeb/CSS/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <fcntl.h>

namespace HackStudio {

enum class TooltipRole {
    Documentation,
    ParametersHint,
};

static RefPtr<GUI::Window> s_tooltip_window;
static RefPtr<WebView::OutOfProcessWebView> s_tooltip_page_view;
static Optional<TooltipRole> m_tooltip_role;

ErrorOr<NonnullRefPtr<Editor>> Editor::try_create()
{
    NonnullRefPtr<Editor> editor = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Editor()));
    TRY(initialize_tooltip_window());
    return editor;
}

Editor::Editor()
{
    create_tokens_info_timer();

    set_document(CodeDocument::create());
    m_move_execution_to_line_action = GUI::Action::create("Set Execution Point to Cursor Line", [this](auto&) {
        VERIFY(is_program_running());
        auto success = Debugger::the().set_execution_position(currently_open_file(), cursor().line());
        if (success) {
            set_execution_position(cursor().line());
        } else {
            GUI::MessageBox::show(window(), "Failed to set execution position"sv, "Error"sv, GUI::MessageBox::Type::Error);
        }
    });

    set_debug_mode(false);

    add_custom_context_menu_action(*m_move_execution_to_line_action);

    set_gutter_visible(true);
    on_gutter_click = [&](auto line, auto) {
        add_breakpoint(line).release_value_but_fixme_should_propagate_errors();
    };

    m_git_diff_indicator_id = register_gutter_indicator(
        [&](auto& painter, Gfx::IntRect rect, size_t line) {
            auto diff_type = code_document().line_difference(line);
            switch (diff_type) {
            case CodeDocument::DiffType::AddedLine:
                painter.draw_text(rect, "+"sv, font(), Gfx::TextAlignment::Center);
                break;
            case CodeDocument::DiffType::ModifiedLine:
                painter.draw_text(rect, "!"sv, font(), Gfx::TextAlignment::Center);
                break;
            case CodeDocument::DiffType::DeletedLinesBefore:
                painter.draw_text(rect, "-"sv, font(), Gfx::TextAlignment::Center);
                break;
            case CodeDocument::DiffType::None:
                VERIFY_NOT_REACHED();
            }
        }).release_value_but_fixme_should_propagate_errors();

    m_breakpoint_indicator_id = register_gutter_indicator(
        [&](auto& painter, Gfx::IntRect rect, size_t) {
            auto const& icon = breakpoint_icon_bitmap();
            painter.draw_scaled_bitmap(rect, icon, icon.rect());
        },
        [&](size_t line_index, auto) {
            remove_breakpoint(line_index);
        }).release_value_but_fixme_should_propagate_errors();

    m_execution_indicator_id = register_gutter_indicator(
        [&](auto& painter, Gfx::IntRect rect, size_t) {
            auto const& icon = current_position_icon_bitmap();
            painter.draw_scaled_bitmap(rect, icon, icon.rect());
        }).release_value_but_fixme_should_propagate_errors();

    if (Config::read_string("HackStudio"sv, "Global"sv, "DocumentationSearchPaths"sv).is_empty()) {
        Config::write_string("HackStudio"sv, "Global"sv, "DocumentationSearchPaths"sv, "[\"/usr/share/man/man2\", \"/usr/share/man/man3\"]"sv);
    }
}

ErrorOr<void> Editor::initialize_tooltip_window()
{
    if (s_tooltip_window.is_null()) {
        s_tooltip_window = GUI::Window::construct();
        s_tooltip_window->set_window_type(GUI::WindowType::Tooltip);
    }
    if (s_tooltip_page_view.is_null()) {
        s_tooltip_page_view = s_tooltip_window->set_main_widget<WebView::OutOfProcessWebView>();
    }
    return {};
}

EditorWrapper& Editor::wrapper()
{
    return static_cast<EditorWrapper&>(*parent());
}
EditorWrapper const& Editor::wrapper() const
{
    return static_cast<EditorWrapper const&>(*parent());
}

void Editor::paint_event(GUI::PaintEvent& event)
{
    GUI::TextEditor::paint_event(event);

    GUI::Painter painter(*this);
    if (is_focused()) {
        painter.add_clip_rect(event.rect());

        auto rect = frame_inner_rect();
        if (vertical_scrollbar().is_visible())
            rect.set_width(rect.width() - vertical_scrollbar().width());
        if (horizontal_scrollbar().is_visible())
            rect.set_height(rect.height() - horizontal_scrollbar().height());
        painter.draw_rect(rect, palette().selection());
    }
}

static HashMap<ByteString, ByteString>& man_paths()
{
    static HashMap<ByteString, ByteString> paths;
    if (paths.is_empty()) {
        auto json = Config::read_string("HackStudio"sv, "Global"sv, "DocumentationSearchPaths"sv);
        AK::JsonParser parser(json);

        auto value_or_error = parser.parse();
        if (value_or_error.is_error())
            return paths;

        auto value = value_or_error.release_value();
        if (!value.is_array())
            return paths;

        for (auto& json_value : value.as_array().values()) {
            if (!json_value.is_string())
                continue;

            Core::DirIterator it(json_value.as_string(), Core::DirIterator::Flags::SkipDots);
            while (it.has_next()) {
                auto path = it.next_full_path();
                auto title = LexicalPath::title(path);
                paths.set(title, path);
            }
        }
    }

    return paths;
}

void Editor::show_documentation_tooltip_if_available(ByteString const& hovered_token, Gfx::IntPoint screen_location)
{
    auto it = man_paths().find(hovered_token);
    if (it == man_paths().end()) {
        dbgln_if(EDITOR_DEBUG, "no man path for {}", hovered_token);
        if (m_tooltip_role == TooltipRole::Documentation) {
            s_tooltip_window->hide();
            m_tooltip_role.clear();
        }
        return;
    }

    if (s_tooltip_window->is_visible() && m_tooltip_role == TooltipRole::Documentation && hovered_token == m_last_parsed_token) {
        return;
    }

    dbgln_if(EDITOR_DEBUG, "opening {}", it->value);
    auto file_or_error = Core::File::open(it->value, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        dbgln("Failed to open {}, {}", it->value, file_or_error.error());
        return;
    }

    auto buffer_or_error = file_or_error.release_value()->read_until_eof();
    if (buffer_or_error.is_error()) {
        dbgln("Couldn't read file: {}", buffer_or_error.error());
        return;
    }

    auto man_document = Markdown::Document::parse(buffer_or_error.release_value());
    if (!man_document) {
        dbgln("failed to parse markdown");
        return;
    }

    s_tooltip_page_view->load_html(man_document->render_to_html("<style>body { background-color: #dac7b5; }</style>"sv));

    s_tooltip_window->set_rect(0, 0, 500, 400);
    s_tooltip_window->move_to(screen_location.translated(4, 4));
    m_tooltip_role = TooltipRole::Documentation;
    s_tooltip_window->show();

    m_last_parsed_token = hovered_token;
}

void Editor::mousemove_event(GUI::MouseEvent& event)
{
    GUI::TextEditor::mousemove_event(event);

    if (document().spans().is_empty())
        return;

    auto text_position = text_position_at(event.position());
    if (!text_position.is_valid() && m_tooltip_role == TooltipRole::Documentation) {
        s_tooltip_window->hide();
        m_tooltip_role.clear();
        return;
    }

    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter)
        return;

    bool hide_tooltip = (m_tooltip_role == TooltipRole::Documentation);
    bool is_over_clickable = false;

    if (m_hovering_editor && event.position().x() > fixed_elements_width())
        set_override_cursor(m_hovering_clickable && event.ctrl() ? Gfx::StandardCursor::Hand : Gfx::StandardCursor::IBeam);

    for (auto& span : document().spans()) {
        bool is_clickable = (highlighter->is_navigatable(span.data) || highlighter->is_identifier(span.data));
        if (span.range.contains(m_previous_text_position) && !span.range.contains(text_position)) {
            if (is_clickable && span.attributes.underline_style.has_value()) {
                span.attributes.underline_style.clear();
                wrapper().editor().update();
            }
        }

        if (span.range.contains(text_position)) {
            auto hovered_span_text = document().text_in_range(span.range);
            dbgln_if(EDITOR_DEBUG, "Hovering: {} \"{}\"", span.range, hovered_span_text);

            if (is_clickable) {
                is_over_clickable = true;
                bool was_underlined = span.attributes.underline_style.has_value();
                bool now_underlined = event.modifiers() & Mod_Ctrl;
                span.attributes.underline_style.clear();
                if (now_underlined)
                    span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Solid;
                if (now_underlined != was_underlined) {
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
    if (hide_tooltip) {
        s_tooltip_window->hide();
        m_tooltip_role.clear();
    }

    m_hovering_clickable = (is_over_clickable) && (event.modifiers() & Mod_Ctrl);
}

void Editor::mousedown_event(GUI::MouseEvent& event)
{
    if (m_tooltip_role == TooltipRole::ParametersHint) {
        s_tooltip_window->hide();
        m_tooltip_role.clear();
    }

    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    auto text_position = text_position_at(event.position());

    if (!(event.modifiers() & Mod_Ctrl)) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    if (!text_position.is_valid()) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    if (auto* span = document().span_at(text_position)) {
        if (highlighter->is_navigatable(span->data)) {
            on_navigatable_link_click(*span);
            return;
        }
        if (highlighter->is_identifier(span->data)) {
            on_identifier_click(*span);
            return;
        }
    }

    GUI::TextEditor::mousedown_event(event);
}

void Editor::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void Editor::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "HackStudio can only open one file at a time!"sv, "One at a time please!"sv, GUI::MessageBox::Type::Error);
            return;
        }
        set_current_editor_wrapper(static_cast<EditorWrapper*>(parent()));
        open_file(URL::percent_decode(urls.first().serialize_path()));
    }
}

void Editor::enter_event(Core::Event& event)
{
    m_hovering_editor = true;
    GUI::TextEditor::enter_event(event);
}

void Editor::leave_event(Core::Event& event)
{
    m_hovering_editor = false;
    GUI::TextEditor::leave_event(event);
}

static HashMap<ByteString, ByteString>& include_paths()
{
    static HashMap<ByteString, ByteString> paths;

    auto add_directory = [](ByteString base, Optional<ByteString> recursive, auto handle_directory) -> void {
        Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = it.next_full_path();
            if (!FileSystem::is_directory(path)) {
                auto key = path.substring(base.length() + 1, path.length() - base.length() - 1);
                dbgln_if(EDITOR_DEBUG, "Adding header \"{}\" in path \"{}\"", key, path);
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

void Editor::navigate_to_include_if_available(ByteString path)
{
    auto it = include_paths().find(path);
    if (it == include_paths().end()) {
        dbgln_if(EDITOR_DEBUG, "no header {} found.", path);
        return;
    }

    on_open(it->value);
}

void Editor::set_execution_position(size_t line_number)
{
    if (execution_position().has_value())
        remove_gutter_indicator(m_execution_indicator_id, execution_position().value());
    add_gutter_indicator(m_execution_indicator_id, line_number);
    code_document().set_execution_position(line_number);
    scroll_position_into_view({ line_number, 0 });
}

void Editor::clear_execution_position()
{
    if (!execution_position().has_value()) {
        return;
    }
    size_t previous_position = execution_position().value();
    code_document().clear_execution_position();
    remove_gutter_indicator(m_execution_indicator_id, previous_position);
}

Gfx::Bitmap const& Editor::breakpoint_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/breakpoint.png"sv).release_value_but_fixme_should_propagate_errors();
    return *bitmap;
}

Gfx::Bitmap const& Editor::current_position_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors();
    return *bitmap;
}

CodeDocument const& Editor::code_document() const
{
    return verify_cast<CodeDocument const>(document());
}

CodeDocument& Editor::code_document()
{
    return const_cast<CodeDocument&>(static_cast<Editor const&>(*this).code_document());
}

void Editor::set_document(GUI::TextDocument& doc)
{
    if (has_document() && &document() == &doc)
        return;

    VERIFY(is<CodeDocument>(doc));
    GUI::TextEditor::set_document(doc);

    auto& code_document = static_cast<CodeDocument&>(doc);

    set_language_client_for(code_document);
    set_syntax_highlighter_for(code_document);

    if (m_language_client) {
        set_autocomplete_provider(make<LanguageServerAidedAutocompleteProvider>(*m_language_client));
        // NOTE:
        // When a file is opened for the first time in HackStudio, its content is already synced with the filesystem.
        // Otherwise, if the file has already been opened before in some Editor instance, it should exist in the LanguageServer's
        // FileDB, and the LanguageServer should already have its up-to-date content.
        // So it's OK to just pass an fd here (rather than the TextDocument's content).
        auto maybe_fd = Core::System::open(code_document.file_path(), O_RDONLY | O_NOCTTY);
        if (maybe_fd.is_error()) {
            warnln("Failed to open `{}`: {}", code_document.file_path(), maybe_fd.release_error());
            return;
        }
        auto fd = maybe_fd.release_value();
        m_language_client->open_file(code_document.file_path(), fd);
        close(fd);
    } else {
        set_autocomplete_provider_for(code_document);
    }
}

Optional<Editor::AutoCompleteRequestData> Editor::get_autocomplete_request_data()
{
    if (!wrapper().editor().m_language_client)
        return {};

    return Editor::AutoCompleteRequestData { cursor() };
}

void Editor::LanguageServerAidedAutocompleteProvider::provide_completions(Function<void(Vector<CodeComprehension::AutocompleteResultEntry>)> callback)
{
    auto& editor = static_cast<Editor&>(*m_editor).wrapper().editor();
    auto data = editor.get_autocomplete_request_data();
    if (!data.has_value())
        callback({});

    m_language_client.on_autocomplete_suggestions = [callback = move(callback)](auto suggestions) {
        callback(suggestions);
    };

    m_language_client.request_autocomplete(
        editor.code_document().file_path(),
        data.value().position.line(),
        data.value().position.column());
}

void Editor::after_execute(GUI::TextDocumentUndoCommand const& command)
{
    if (!m_language_client)
        return;

    if (is<GUI::InsertTextCommand>(command)) {
        auto const& insert_command = static_cast<GUI::InsertTextCommand const&>(command);
        m_language_client->insert_text(
            code_document().file_path(),
            insert_command.text(),
            insert_command.range().start().line(),
            insert_command.range().start().column());
        return;
    }

    if (is<GUI::RemoveTextCommand>(command)) {
        auto const& remove_command = static_cast<GUI::RemoveTextCommand const&>(command);
        m_language_client->remove_text(
            code_document().file_path(),
            remove_command.range().start().line(),
            remove_command.range().start().column(),
            remove_command.range().end().line(),
            remove_command.range().end().column());
        return;
    }

    flush_file_content_to_langauge_server();
}

void Editor::undo()
{
    TextEditor::undo();
    flush_file_content_to_langauge_server();
}

void Editor::redo()
{
    TextEditor::redo();
    flush_file_content_to_langauge_server();
}

void Editor::flush_file_content_to_langauge_server()
{
    if (!m_language_client)
        return;

    m_language_client->set_file_content(
        code_document().file_path(),
        document().text());
}

void Editor::on_navigatable_link_click(const GUI::TextDocumentSpan& span)
{
    auto span_text = document().text_in_range(span.range);
    auto header_path = span_text.substring(1, span_text.length() - 2);
    dbgln_if(EDITOR_DEBUG, "Ctrl+click: {} \"{}\"", span.range, header_path);
    navigate_to_include_if_available(header_path);
}

void Editor::on_identifier_click(const GUI::TextDocumentSpan& span)
{
    if (!m_language_client)
        return;

    m_language_client->on_declaration_found = [](ByteString const& file, size_t line, size_t column) {
        HackStudio::open_file(file, line, column);
    };
    m_language_client->search_declaration(code_document().file_path(), span.range.start().line(), span.range.start().column());
}

void Editor::set_syntax_highlighter_for(CodeDocument const& document)
{
    if (!document.language().has_value()) {
        set_syntax_highlighter({});
        force_rehighlight();
        return;
    }

    switch (document.language().value()) {
    case Syntax::Language::Cpp:
        if (m_use_semantic_syntax_highlighting) {
            set_syntax_highlighter(make<Cpp::SemanticSyntaxHighlighter>());
            on_token_info_timer_tick();
            m_tokens_info_timer->restart();
        } else {
            set_syntax_highlighter(make<Cpp::SyntaxHighlighter>());
        }
        break;
    case Syntax::Language::CMake:
        set_syntax_highlighter(make<CMake::SyntaxHighlighter>());
        break;
    case Syntax::Language::CMakeCache:
        set_syntax_highlighter(make<CMake::Cache::SyntaxHighlighter>());
        break;
    case Syntax::Language::CSS:
        set_syntax_highlighter(make<Web::CSS::SyntaxHighlighter>());
        break;
    case Syntax::Language::GitCommit:
        set_syntax_highlighter(make<GUI::GitCommitSyntaxHighlighter>());
        break;
    case Syntax::Language::GML:
        set_syntax_highlighter(make<GUI::GML::SyntaxHighlighter>());
        break;
    case Syntax::Language::HTML:
        set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
        break;
    case Syntax::Language::INI:
        set_syntax_highlighter(make<GUI::IniSyntaxHighlighter>());
        break;
    case Syntax::Language::JavaScript:
        set_syntax_highlighter(make<JS::SyntaxHighlighter>());
        break;
    case Syntax::Language::Markdown:
        set_syntax_highlighter(make<Markdown::SyntaxHighlighter>());
        break;
    case Syntax::Language::Shell:
        set_syntax_highlighter(make<Shell::SyntaxHighlighter>());
        break;
    case Syntax::Language::SQL:
        set_syntax_highlighter(make<SQL::AST::SyntaxHighlighter>());
        break;
    default:
        set_syntax_highlighter({});
    }

    force_rehighlight();
}

void Editor::set_autocomplete_provider_for(CodeDocument const& document)
{
    if (document.language() == Syntax::Language::GML) {
        set_autocomplete_provider(make<GUI::GML::AutocompleteProvider>());
    } else {
        set_autocomplete_provider({});
    }
}

void Editor::set_language_client_for(CodeDocument const& document)
{
    if (m_language_client && m_language_client->language() == document.language())
        return;

    if (document.language() == Syntax::Language::Cpp)
        m_language_client = get_language_client<LanguageClients::Cpp::ConnectionToServer>(project().root_path());

    if (document.language() == Syntax::Language::Shell)
        m_language_client = get_language_client<LanguageClients::Shell::ConnectionToServer>(project().root_path());

    if (m_language_client) {
        m_language_client->on_tokens_info_result = [this](Vector<CodeComprehension::TokenInfo> const& tokens_info) {
            on_tokens_info_result(tokens_info);
        };
    }
}

void Editor::keydown_event(GUI::KeyEvent& event)
{
    TextEditor::keydown_event(event);

    if (m_tooltip_role == TooltipRole::ParametersHint) {
        s_tooltip_window->hide();
        m_tooltip_role.clear();
    }

    if (!event.shift() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_P) {
        handle_function_parameters_hint_request();
    }

    m_tokens_info_timer->restart();
}

void Editor::handle_function_parameters_hint_request()
{
    if (!m_language_client)
        return;

    m_language_client->on_function_parameters_hint_result = [this](Vector<ByteString> const& params, size_t argument_index) {
        dbgln("on_function_parameters_hint_result");

        StringBuilder html;
        for (size_t i = 0; i < params.size(); ++i) {
            if (i == argument_index)
                html.append("<b>"sv);

            html.appendff("{}", params[i]);

            if (i == argument_index)
                html.append("</b>"sv);

            if (i < params.size() - 1)
                html.append(", "sv);
        }
        html.append("<style>body { background-color: #dac7b5; }</style>"sv);

        s_tooltip_page_view->load_html(html.to_byte_string());

        auto cursor_rect = current_editor().cursor_content_rect().location().translated(screen_relative_rect().location());

        Gfx::Rect content(cursor_rect.x(), cursor_rect.y(), s_tooltip_page_view->children_clip_rect().width(), s_tooltip_page_view->children_clip_rect().height());

        m_tooltip_role = TooltipRole::ParametersHint;
        s_tooltip_window->set_rect(0, 0, 280, 35);
        s_tooltip_window->move_to(cursor_rect.x(), cursor_rect.y() - s_tooltip_window->height() - vertical_scrollbar().value());
        s_tooltip_window->show();
    };

    m_language_client->get_parameters_hint(
        code_document().file_path(),
        cursor().line(),
        cursor().column());
}

void Editor::set_debug_mode(bool enabled)
{
    m_move_execution_to_line_action->set_enabled(enabled);
}

void Editor::on_token_info_timer_tick()
{
    if (!semantic_syntax_highlighting_is_enabled())
        return;
    if (!m_language_client || !m_language_client->is_active_client())
        return;

    m_language_client->get_tokens_info(code_document().file_path());
}

void Editor::on_tokens_info_result(Vector<CodeComprehension::TokenInfo> const& tokens_info)
{
    auto highlighter = syntax_highlighter();
    if (highlighter && highlighter->is_cpp_semantic_highlighter()) {
        auto& semantic_cpp_highlighter = verify_cast<Cpp::SemanticSyntaxHighlighter>(*highlighter);
        semantic_cpp_highlighter.update_tokens_info(tokens_info);
        force_rehighlight();
    }
}

void Editor::create_tokens_info_timer()
{
    static constexpr size_t token_info_timer_interval_ms = 1000;
    m_tokens_info_timer = Core::Timer::create_repeating((int)token_info_timer_interval_ms, [this] {
        on_token_info_timer_tick();
        m_tokens_info_timer->stop();
    });
    m_tokens_info_timer->start();
}

void Editor::set_semantic_syntax_highlighting(bool value)
{
    m_use_semantic_syntax_highlighting = value;
    set_syntax_highlighter_for(code_document());
}

ErrorOr<void> Editor::add_breakpoint(size_t line_number)
{
    if (!breakpoint_lines().contains_slow(line_number)) {
        if (Debugger::the().change_breakpoint(wrapper().filename_title(), line_number, BreakpointChange::Added)) {
            add_gutter_indicator(m_breakpoint_indicator_id, line_number);
            TRY(breakpoint_lines().try_append(line_number));
        }
    }

    return {};
}

void Editor::remove_breakpoint(size_t line_number)
{
    if (Debugger::the().change_breakpoint(wrapper().filename_title(), line_number, BreakpointChange::Removed)) {
        remove_gutter_indicator(m_breakpoint_indicator_id, line_number);
        breakpoint_lines().remove_first_matching([&](size_t line) { return line == line_number; });
    }
}

ErrorOr<void> Editor::update_git_diff_indicators()
{
    clear_gutter_indicators(m_git_diff_indicator_id);

    if (!wrapper().git_repo())
        return {};

    Vector<CodeDocument::DiffType> line_differences;
    TRY(line_differences.try_ensure_capacity(document().line_count()));
    for (auto i = 0u; i < document().line_count(); ++i)
        line_differences.unchecked_append(CodeDocument::DiffType::None);

    for (auto const& hunk : wrapper().hunks()) {
        auto start_line = hunk.location.new_range.start_line;
        // Account for 1 indexed hunk location
        if (start_line != 0)
            start_line--;
        auto finish_line = start_line + hunk.location.new_range.number_of_lines;

        auto additions = hunk.location.new_range.number_of_lines;
        auto deletions = hunk.location.old_range.number_of_lines;

        for (size_t line_offset = 0; line_offset < additions; line_offset++) {
            auto line = start_line + line_offset;
            auto difference = (line_offset < deletions) ? CodeDocument::DiffType::ModifiedLine : CodeDocument::DiffType::AddedLine;
            line_differences[line] = difference;
            add_gutter_indicator(m_git_diff_indicator_id, line);
        }
        if (additions < deletions) {
            auto deletions_line = min(finish_line, line_count() - 1);
            line_differences[deletions_line] = CodeDocument::DiffType::DeletedLinesBefore;
            add_gutter_indicator(m_git_diff_indicator_id, deletions_line);
        }
    }
    code_document().set_line_differences({}, move(line_differences));
    update();

    return {};
}

}
