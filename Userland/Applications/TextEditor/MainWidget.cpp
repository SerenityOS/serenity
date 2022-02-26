/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <Applications/TextEditor/TextEditorWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>
#include <LibCpp/SyntaxHighlighter.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/GML/SyntaxHighlighter.h>
#include <LibGUI/GitCommitSyntaxHighlighter.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/VimEditingEngine.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibMarkdown/Document.h>
#include <LibSQL/AST/SyntaxHighlighter.h>
#include <LibWeb/CSS/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <Shell/SyntaxHighlighter.h>

namespace TextEditor {

MainWidget::MainWidget()
{
    load_from_gml(text_editor_window_gml);

    m_toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    m_toolbar_container = *find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");

    m_editor = *find_descendant_of_type_named<GUI::TextEditor>("editor");
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);
    if (m_editor->editing_engine()->is_regular())
        m_editor->set_editing_engine(make<GUI::RegularEditingEngine>());
    else if (m_editor->editing_engine()->is_vim())
        m_editor->set_editing_engine(make<GUI::VimEditingEngine>());
    else
        VERIFY_NOT_REACHED();

    m_editor->on_change = [this] {
        update_preview();
    };

    m_editor->on_modified_change = [this](bool modified) {
        window()->set_modified(modified);
    };

    m_find_replace_widget = *find_descendant_of_type_named<GUI::GroupBox>("find_replace_widget");
    m_find_widget = *find_descendant_of_type_named<GUI::Widget>("find_widget");
    m_replace_widget = *find_descendant_of_type_named<GUI::Widget>("replace_widget");

    m_find_textbox = *find_descendant_of_type_named<GUI::TextBox>("find_textbox");
    m_find_textbox->set_placeholder("Find");

    m_replace_textbox = *find_descendant_of_type_named<GUI::TextBox>("replace_textbox");
    m_replace_textbox->set_placeholder("Replace");

    m_match_case_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("match_case_checkbox");
    m_match_case_checkbox->on_checked = [this](auto is_checked) {
        m_match_case = is_checked;
    };
    m_match_case_checkbox->set_checked(true);

    m_regex_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("regex_checkbox");
    m_regex_checkbox->on_checked = [this](auto is_checked) {
        m_use_regex = is_checked;
    };
    m_regex_checkbox->set_checked(false);

    m_wrap_around_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("wrap_around_checkbox");
    m_wrap_around_checkbox->on_checked = [this](auto is_checked) {
        m_should_wrap = is_checked;
    };
    m_wrap_around_checkbox->set_checked(true);

    m_find_next_action = GUI::Action::create("Find &Next", { Mod_Ctrl, Key_G }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find-next.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto needle = m_find_textbox->text();
        if (needle.is_empty())
            return;
        if (m_use_regex)
            m_editor->document().update_regex_matches(needle);

        auto found_range = m_editor->document().find_next(needle, m_editor->normalized_selection().end(), m_should_wrap ? GUI::TextDocument::SearchShouldWrap::Yes : GUI::TextDocument::SearchShouldWrap::No, m_use_regex, m_match_case);
        dbgln("find_next('{}') returned {}", needle, found_range);
        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
        } else {
            GUI::MessageBox::show(window(),
                String::formatted("Not found: \"{}\"", needle),
                "Not found",
                GUI::MessageBox::Type::Information);
        }
    });

    m_find_previous_action = GUI::Action::create("Find Pr&evious", { Mod_Ctrl | Mod_Shift, Key_G }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find-previous.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        auto needle = m_find_textbox->text();
        if (needle.is_empty())
            return;
        if (m_use_regex)
            m_editor->document().update_regex_matches(needle);

        auto selection_start = m_editor->normalized_selection().start();
        if (!selection_start.is_valid())
            selection_start = m_editor->normalized_selection().end();

        auto found_range = m_editor->document().find_previous(needle, selection_start, m_should_wrap ? GUI::TextDocument::SearchShouldWrap::Yes : GUI::TextDocument::SearchShouldWrap::No, m_use_regex, m_match_case);
        dbgln("find_prev(\"{}\") returned {}", needle, found_range);
        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
        } else {
            GUI::MessageBox::show(window(),
                String::formatted("Not found: \"{}\"", needle),
                "Not found",
                GUI::MessageBox::Type::Information);
        }
    });

    m_replace_action = GUI::Action::create("Rep&lace", { Mod_Ctrl, Key_F1 }, [&](auto&) {
        auto needle = m_find_textbox->text();
        auto substitute = m_replace_textbox->text();
        if (needle.is_empty())
            return;
        if (m_use_regex)
            m_editor->document().update_regex_matches(needle);

        auto found_range = m_editor->document().find_next(needle, m_editor->normalized_selection().start(), m_should_wrap ? GUI::TextDocument::SearchShouldWrap::Yes : GUI::TextDocument::SearchShouldWrap::No, m_use_regex, m_match_case);
        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
            m_editor->insert_at_cursor_or_replace_selection(substitute);
        } else {
            GUI::MessageBox::show(window(),
                String::formatted("Not found: \"{}\"", needle),
                "Not found",
                GUI::MessageBox::Type::Information);
        }
    });

    m_replace_all_action = GUI::Action::create("Replace &All", { Mod_Ctrl, Key_F2 }, [&](auto&) {
        auto needle = m_find_textbox->text();
        auto substitute = m_replace_textbox->text();
        auto length_delta = substitute.length() - needle.length();
        if (needle.is_empty())
            return;
        if (m_use_regex)
            m_editor->document().update_regex_matches(needle);

        auto found_range = m_editor->document().find_next(needle, {}, GUI::TextDocument::SearchShouldWrap::No, m_use_regex, m_match_case);
        while (found_range.is_valid()) {
            m_editor->set_selection(found_range);
            m_editor->insert_at_cursor_or_replace_selection(substitute);
            auto next_start = GUI::TextPosition(found_range.end().line(), found_range.end().column() + length_delta);
            found_range = m_editor->document().find_next(needle, next_start, GUI::TextDocument::SearchShouldWrap::No, m_use_regex, m_match_case);
        }
    });

    m_find_previous_button = *find_descendant_of_type_named<GUI::Button>("find_previous_button");
    m_find_previous_button->set_action(*m_find_previous_action);
    m_find_previous_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find-previous.png").release_value_but_fixme_should_propagate_errors());

    m_find_next_button = *find_descendant_of_type_named<GUI::Button>("find_next_button");
    m_find_next_button->set_action(*m_find_next_action);
    m_find_next_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find-next.png").release_value_but_fixme_should_propagate_errors());

    m_find_textbox->on_return_pressed = [this] {
        m_find_next_button->click();
    };

    m_find_textbox->on_escape_pressed = [this] {
        m_find_replace_widget->set_visible(false);
        m_editor->set_focus(true);
    };

    m_replace_button = *find_descendant_of_type_named<GUI::Button>("replace_button");
    m_replace_button->set_action(*m_replace_action);

    m_replace_all_button = *find_descendant_of_type_named<GUI::Button>("replace_all_button");
    m_replace_all_button->set_action(*m_replace_all_action);

    m_replace_textbox->on_return_pressed = [this] {
        m_replace_button->click();
    };

    m_replace_textbox->on_escape_pressed = [this] {
        m_find_replace_widget->set_visible(false);
        m_editor->set_focus(true);
    };

    m_vim_emulation_setting_action = GUI::Action::create_checkable("&Vim Emulation", { Mod_Ctrl | Mod_Shift | Mod_Alt, Key_V }, [&](auto& action) {
        if (action.is_checked())
            m_editor->set_editing_engine(make<GUI::VimEditingEngine>());
        else
            m_editor->set_editing_engine(make<GUI::RegularEditingEngine>());
    });
    m_vim_emulation_setting_action->set_checked(false);

    m_find_replace_action = GUI::Action::create("&Find/Replace...", { Mod_Ctrl, Key_F }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        m_find_replace_widget->set_visible(true);
        m_find_widget->set_visible(true);
        m_replace_widget->set_visible(true);
        m_find_textbox->set_focus(true);

        if (m_editor->has_selection()) {
            auto selected_text = m_editor->document().text_in_range(m_editor->normalized_selection());
            m_find_textbox->set_text(selected_text);
        }
        m_find_textbox->select_all();
    });

    m_editor->add_custom_context_menu_action(*m_find_replace_action);
    m_editor->add_custom_context_menu_action(*m_find_next_action);
    m_editor->add_custom_context_menu_action(*m_find_previous_action);

    m_line_column_statusbar_menu = GUI::Menu::construct();
    m_syntax_statusbar_menu = GUI::Menu::construct();

    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Auto);
    m_statusbar->segment(1).set_clickable(true);
    m_statusbar->segment(1).set_menu(m_syntax_statusbar_menu);
    m_statusbar->segment(2).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    auto width = font().width("Ln 0000, Col 000") + font().max_glyph_width();
    m_statusbar->segment(2).set_fixed_width(width);
    m_statusbar->segment(2).set_clickable(true);
    m_statusbar->segment(2).set_menu(m_line_column_statusbar_menu);

    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        m_statusbar->set_override_text(move(text));
    };

    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };

    m_editor->on_cursor_change = [this] { update_statusbar(); };
    m_editor->on_selection_change = [this] { update_statusbar(); };
    m_editor->on_highlighter_change = [this] { update_statusbar(); };

    m_new_action = GUI::Action::create("&New", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png").release_value_but_fixme_should_propagate_errors(), [this](GUI::Action const&) {
        if (editor().document().is_modified()) {
            auto save_document_first_result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path, editor().document().undo_stack().last_unmodified_timestamp());
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                m_save_action->activate();
            if (save_document_first_result != GUI::Dialog::ExecResult::ExecNo && editor().document().is_modified())
                return;
        }

        m_editor->set_text(StringView());
        set_path({});
        update_title();
    });

    m_open_action = GUI::CommonActions::make_open_action([this](auto&) {
        if (editor().document().is_modified()) {
            auto save_document_first_result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path, editor().document().undo_stack().last_unmodified_timestamp());
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                m_save_action->activate();
            if (save_document_first_result != GUI::Dialog::ExecResult::ExecNo && editor().document().is_modified())
                return;
        }

        auto response = FileSystemAccessClient::Client::the().try_open_file(window());
        if (response.is_error())
            return;

        read_file(*response.value());
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_save_file(window(), m_name, m_extension);
        if (response.is_error())
            return;

        auto file = response.release_value();
        if (!m_editor->write_to_file(*file)) {
            GUI::MessageBox::show(window(), "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
            return;
        }

        set_path(file->filename());
        dbgln("Wrote document to {}", file->filename());
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (m_path.is_empty()) {
            m_save_as_action->activate();
            return;
        }
        auto response = FileSystemAccessClient::Client::the().try_request_file(window(), m_path, Core::OpenMode::Truncate | Core::OpenMode::WriteOnly);
        if (response.is_error())
            return;

        if (!m_editor->write_to_file(*response.value())) {
            GUI::MessageBox::show(window(), "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
        }
    });

    m_toolbar->add_action(*m_new_action);
    m_toolbar->add_action(*m_open_action);
    m_toolbar->add_action(*m_save_action);

    m_toolbar->add_separator();

    m_toolbar->add_action(m_editor->cut_action());
    m_toolbar->add_action(m_editor->copy_action());
    m_toolbar->add_action(m_editor->paste_action());

    m_toolbar->add_separator();

    m_toolbar->add_action(m_editor->undo_action());
    m_toolbar->add_action(m_editor->redo_action());
}

Web::OutOfProcessWebView& MainWidget::ensure_web_view()
{
    if (!m_page_view) {
        auto& web_view_container = *find_descendant_of_type_named<GUI::Widget>("web_view_container");
        m_page_view = web_view_container.add<Web::OutOfProcessWebView>();
        m_page_view->on_link_hover = [this](auto& url) {
            if (url.is_valid())
                m_statusbar->set_text(url.to_string());
            else
                update_statusbar();
        };
        m_page_view->on_link_click = [&](auto& url, auto&, unsigned) {
            if (!Desktop::Launcher::open(url)) {
                GUI::MessageBox::show(
                    window(),
                    String::formatted("The link to '{}' could not be opened.", url),
                    "Failed to open link",
                    GUI::MessageBox::Type::Error);
            }
        };
    }
    return *m_page_view;
}

void MainWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(*m_new_action);
    file_menu.add_action(*m_open_action);
    file_menu.add_action(*m_save_action);
    file_menu.add_action(*m_save_as_action);
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (!request_close())
            return;
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = window.add_menu("&Edit");
    edit_menu.add_action(m_editor->undo_action());
    edit_menu.add_action(m_editor->redo_action());
    edit_menu.add_separator();
    edit_menu.add_action(m_editor->cut_action());
    edit_menu.add_action(m_editor->copy_action());
    edit_menu.add_action(m_editor->paste_action());
    edit_menu.add_separator();
    edit_menu.add_action(*m_vim_emulation_setting_action);
    edit_menu.add_separator();
    edit_menu.add_action(*m_find_replace_action);
    edit_menu.add_action(*m_find_next_action);
    edit_menu.add_action(*m_find_previous_action);
    edit_menu.add_action(*m_replace_action);
    edit_menu.add_action(*m_replace_all_action);

    m_no_preview_action = GUI::Action::create_checkable(
        "&No Preview", [this](auto&) {
            set_preview_mode(PreviewMode::None);
        });

    m_markdown_preview_action = GUI::Action::create_checkable(
        "&Markdown Preview", [this](auto&) {
            set_preview_mode(PreviewMode::Markdown);
        },
        this);

    m_html_preview_action = GUI::Action::create_checkable(
        "&HTML Preview", [this](auto&) {
            set_preview_mode(PreviewMode::HTML);
        },
        this);

    m_preview_actions.add_action(*m_no_preview_action);
    m_preview_actions.add_action(*m_markdown_preview_action);
    m_preview_actions.add_action(*m_html_preview_action);
    m_preview_actions.set_exclusive(true);

    m_layout_toolbar_action = GUI::Action::create_checkable("&Toolbar", [&](auto& action) {
        action.is_checked() ? m_toolbar_container->set_visible(true) : m_toolbar_container->set_visible(false);
        Config::write_bool("TextEditor", "Layout", "ShowToolbar", action.is_checked());
    });
    auto show_toolbar = Config::read_bool("TextEditor", "Layout", "ShowToolbar", true);
    m_layout_toolbar_action->set_checked(show_toolbar);
    m_toolbar_container->set_visible(show_toolbar);

    m_layout_statusbar_action = GUI::Action::create_checkable("&Status Bar", [&](auto& action) {
        action.is_checked() ? m_statusbar->set_visible(true) : m_statusbar->set_visible(false);
        Config::write_bool("TextEditor", "Layout", "ShowStatusbar", action.is_checked());
        update_statusbar();
    });
    auto show_statusbar = Config::read_bool("TextEditor", "Layout", "ShowStatusbar", true);
    m_layout_statusbar_action->set_checked(show_statusbar);
    m_statusbar->set_visible(show_statusbar);

    m_layout_ruler_action = GUI::Action::create_checkable("&Ruler", [&](auto& action) {
        action.is_checked() ? m_editor->set_ruler_visible(true) : m_editor->set_ruler_visible(false);
        Config::write_bool("TextEditor", "Layout", "ShowRuler", action.is_checked());
    });
    auto show_ruler = Config::read_bool("TextEditor", "Layout", "ShowRuler", true);
    m_layout_ruler_action->set_checked(show_ruler);
    m_editor->set_ruler_visible(show_ruler);

    auto& view_menu = window.add_menu("&View");
    auto& layout_menu = view_menu.add_submenu("&Layout");
    layout_menu.add_action(*m_layout_toolbar_action);
    layout_menu.add_action(*m_layout_statusbar_action);
    layout_menu.add_action(*m_layout_ruler_action);

    view_menu.add_separator();

    view_menu.add_action(GUI::Action::create("Editor &Font...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-font-editor.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto picker = GUI::FontPicker::construct(&window, &m_editor->font(), false);
            if (picker->exec() == GUI::Dialog::ExecOK) {
                dbgln("setting font {}", picker->font()->qualified_name());
                m_editor->set_font(picker->font());
            }
        }));

    view_menu.add_separator();

    m_wrapping_mode_actions.set_exclusive(true);
    auto& wrapping_mode_menu = view_menu.add_submenu("&Wrapping Mode");
    m_no_wrapping_action = GUI::Action::create_checkable("&No Wrapping", [&](auto&) {
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
        Config::write_string("TextEditor", "View", "WrappingMode", "None");
    });
    m_wrap_anywhere_action = GUI::Action::create_checkable("Wrap &Anywhere", [&](auto&) {
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAnywhere);
        Config::write_string("TextEditor", "View", "WrappingMode", "Anywhere");
    });
    m_wrap_at_words_action = GUI::Action::create_checkable("Wrap at &Words", [&](auto&) {
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAtWords);
        Config::write_string("TextEditor", "View", "WrappingMode", "Words");
    });

    m_wrapping_mode_actions.add_action(*m_no_wrapping_action);
    m_wrapping_mode_actions.add_action(*m_wrap_anywhere_action);
    m_wrapping_mode_actions.add_action(*m_wrap_at_words_action);

    wrapping_mode_menu.add_action(*m_no_wrapping_action);
    wrapping_mode_menu.add_action(*m_wrap_anywhere_action);
    wrapping_mode_menu.add_action(*m_wrap_at_words_action);

    auto word_wrap = Config::read_string("TextEditor", "View", "WrappingMode", "Words");
    if (word_wrap == "None") {
        m_no_wrapping_action->set_checked(true);
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    } else if (word_wrap == "Anywhere") {
        m_wrap_anywhere_action->set_checked(true);
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAnywhere);
    } else {
        m_wrap_at_words_action->set_checked(true);
        m_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAtWords);
    }

    m_soft_tab_width_actions.set_exclusive(true);
    auto& soft_tab_width_menu = view_menu.add_submenu("&Tab Width");
    m_soft_tab_1_width_action = GUI::Action::create_checkable("1", [&](auto&) {
        m_editor->set_soft_tab_width(1);
    });
    m_soft_tab_2_width_action = GUI::Action::create_checkable("2", [&](auto&) {
        m_editor->set_soft_tab_width(2);
    });
    m_soft_tab_4_width_action = GUI::Action::create_checkable("4", [&](auto&) {
        m_editor->set_soft_tab_width(4);
    });
    m_soft_tab_8_width_action = GUI::Action::create_checkable("8", [&](auto&) {
        m_editor->set_soft_tab_width(8);
    });
    m_soft_tab_16_width_action = GUI::Action::create_checkable("16", [&](auto&) {
        m_editor->set_soft_tab_width(16);
    });

    m_soft_tab_width_actions.add_action(*m_soft_tab_1_width_action);
    m_soft_tab_width_actions.add_action(*m_soft_tab_2_width_action);
    m_soft_tab_width_actions.add_action(*m_soft_tab_4_width_action);
    m_soft_tab_width_actions.add_action(*m_soft_tab_8_width_action);
    m_soft_tab_width_actions.add_action(*m_soft_tab_16_width_action);

    soft_tab_width_menu.add_action(*m_soft_tab_1_width_action);
    soft_tab_width_menu.add_action(*m_soft_tab_2_width_action);
    soft_tab_width_menu.add_action(*m_soft_tab_4_width_action);
    soft_tab_width_menu.add_action(*m_soft_tab_8_width_action);
    soft_tab_width_menu.add_action(*m_soft_tab_16_width_action);

    m_soft_tab_4_width_action->set_checked(true);

    view_menu.add_separator();

    m_visualize_trailing_whitespace_action = GUI::Action::create_checkable("T&railing Whitespace", [&](auto&) {
        m_editor->set_visualize_trailing_whitespace(m_visualize_trailing_whitespace_action->is_checked());
    });
    m_visualize_leading_whitespace_action = GUI::Action::create_checkable("L&eading Whitespace", [&](auto&) {
        m_editor->set_visualize_leading_whitespace(m_visualize_leading_whitespace_action->is_checked());
    });

    m_visualize_trailing_whitespace_action->set_checked(true);
    m_visualize_trailing_whitespace_action->set_status_tip("Visualize trailing whitespace");
    m_visualize_leading_whitespace_action->set_status_tip("Visualize leading whitespace");

    view_menu.add_action(*m_visualize_trailing_whitespace_action);
    view_menu.add_action(*m_visualize_leading_whitespace_action);

    m_cursor_line_highlighting_action = GUI::Action::create_checkable("L&ine Highlighting", [&](auto&) {
        m_editor->set_cursor_line_highlighting(m_cursor_line_highlighting_action->is_checked());
    });

    m_cursor_line_highlighting_action->set_checked(true);
    m_cursor_line_highlighting_action->set_status_tip("Highlight the current line");

    view_menu.add_action(*m_cursor_line_highlighting_action);

    view_menu.add_separator();
    view_menu.add_action(*m_no_preview_action);
    view_menu.add_action(*m_markdown_preview_action);
    view_menu.add_action(*m_html_preview_action);
    m_no_preview_action->set_checked(true);
    view_menu.add_separator();

    syntax_actions.set_exclusive(true);

    auto& syntax_menu = view_menu.add_submenu("&Syntax");
    m_plain_text_highlight = GUI::Action::create_checkable("&Plain Text", [&](auto&) {
        m_statusbar->set_text(1, "Plain Text");
        m_editor->set_syntax_highlighter({});
        m_editor->update();
    });
    m_plain_text_highlight->set_checked(true);
    m_statusbar->set_text(1, "Plain Text");
    syntax_actions.add_action(*m_plain_text_highlight);
    syntax_menu.add_action(*m_plain_text_highlight);

    m_cpp_highlight = GUI::Action::create_checkable("&C++", [&](auto&) {
        m_editor->set_syntax_highlighter(make<Cpp::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_cpp_highlight);
    syntax_menu.add_action(*m_cpp_highlight);

    m_js_highlight = GUI::Action::create_checkable("&JavaScript", [&](auto&) {
        m_editor->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_js_highlight);
    syntax_menu.add_action(*m_js_highlight);

    m_css_highlight = GUI::Action::create_checkable("C&SS", [&](auto&) {
        m_editor->set_syntax_highlighter(make<Web::CSS::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_css_highlight);
    syntax_menu.add_action(*m_css_highlight);

    m_html_highlight = GUI::Action::create_checkable("&HTML File", [&](auto&) {
        m_editor->set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_html_highlight);
    syntax_menu.add_action(*m_html_highlight);

    m_git_highlight = GUI::Action::create_checkable("Gi&t Commit", [&](auto&) {
        m_editor->set_syntax_highlighter(make<GUI::GitCommitSyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_git_highlight);
    syntax_menu.add_action(*m_git_highlight);

    m_gml_highlight = GUI::Action::create_checkable("&GML", [&](auto&) {
        m_editor->set_syntax_highlighter(make<GUI::GML::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_gml_highlight);
    syntax_menu.add_action(*m_gml_highlight);

    m_ini_highlight = GUI::Action::create_checkable("&INI File", [&](auto&) {
        m_editor->set_syntax_highlighter(make<GUI::IniSyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_ini_highlight);
    syntax_menu.add_action(*m_ini_highlight);

    m_shell_highlight = GUI::Action::create_checkable("Sh&ell File", [&](auto&) {
        m_editor->set_syntax_highlighter(make<Shell::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_shell_highlight);
    syntax_menu.add_action(*m_shell_highlight);

    m_sql_highlight = GUI::Action::create_checkable("S&QL File", [&](auto&) {
        m_editor->set_syntax_highlighter(make<SQL::AST::SyntaxHighlighter>());
        m_editor->update();
    });
    syntax_actions.add_action(*m_sql_highlight);
    syntax_menu.add_action(*m_sql_highlight);

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/TextEditor.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Text Editor", GUI::Icon::default_icon("app-text-editor"), &window));

    auto& wrapping_statusbar_menu = m_line_column_statusbar_menu->add_submenu("&Wrapping Mode");
    wrapping_statusbar_menu.add_action(*m_no_wrapping_action);
    wrapping_statusbar_menu.add_action(*m_wrap_anywhere_action);
    wrapping_statusbar_menu.add_action(*m_wrap_at_words_action);

    auto& tab_width_statusbar_menu = m_line_column_statusbar_menu->add_submenu("&Tab Width");
    tab_width_statusbar_menu.add_action(*m_soft_tab_1_width_action);
    tab_width_statusbar_menu.add_action(*m_soft_tab_2_width_action);
    tab_width_statusbar_menu.add_action(*m_soft_tab_4_width_action);
    tab_width_statusbar_menu.add_action(*m_soft_tab_8_width_action);
    tab_width_statusbar_menu.add_action(*m_soft_tab_16_width_action);

    m_line_column_statusbar_menu->add_separator();
    m_line_column_statusbar_menu->add_action(*m_cursor_line_highlighting_action);

    m_syntax_statusbar_menu->add_action(*m_plain_text_highlight);
    m_syntax_statusbar_menu->add_action(*m_cpp_highlight);
    m_syntax_statusbar_menu->add_action(*m_css_highlight);
    m_syntax_statusbar_menu->add_action(*m_git_highlight);
    m_syntax_statusbar_menu->add_action(*m_gml_highlight);
    m_syntax_statusbar_menu->add_action(*m_html_highlight);
    m_syntax_statusbar_menu->add_action(*m_ini_highlight);
    m_syntax_statusbar_menu->add_action(*m_js_highlight);
    m_syntax_statusbar_menu->add_action(*m_shell_highlight);
    m_syntax_statusbar_menu->add_action(*m_sql_highlight);
}

void MainWidget::set_path(StringView path)
{
    if (path.is_empty()) {
        m_path = {};
        m_name = {};
        m_extension = {};
    } else {
        auto lexical_path = LexicalPath(path);
        m_path = lexical_path.string();
        m_name = lexical_path.title();
        m_extension = lexical_path.extension();
    }

    if (m_extension == "c" || m_extension == "cc" || m_extension == "cxx" || m_extension == "cpp" || m_extension == "c++"
        || m_extension == "h" || m_extension == "hh" || m_extension == "hxx" || m_extension == "hpp" || m_extension == "h++") {
        m_cpp_highlight->activate();
    } else if (m_extension == "js" || m_extension == "mjs" || m_extension == "json") {
        m_js_highlight->activate();
    } else if (m_name == "COMMIT_EDITMSG") {
        m_git_highlight->activate();
    } else if (m_extension == "gml") {
        m_gml_highlight->activate();
    } else if (m_extension == "ini" || m_extension == "af") {
        m_ini_highlight->activate();
    } else if (m_extension == "sh" || m_extension == "bash") {
        m_shell_highlight->activate();
    } else if (m_extension == "sql") {
        m_sql_highlight->activate();
    } else if (m_extension == "html" || m_extension == "htm") {
        m_html_highlight->activate();
    } else if (m_extension == "css") {
        m_css_highlight->activate();
    } else {
        m_plain_text_highlight->activate();
    }

    if (m_auto_detect_preview_mode) {
        if (m_extension == "md")
            set_preview_mode(PreviewMode::Markdown);
        else if (m_extension == "html" || m_extension == "htm")
            set_preview_mode(PreviewMode::HTML);
        else
            set_preview_mode(PreviewMode::None);
    }

    update_title();
}

void MainWidget::update_title()
{
    StringBuilder builder;
    if (m_path.is_empty())
        builder.append("Untitled");
    else
        builder.append(m_path);
    builder.append("[*] - Text Editor");
    window()->set_title(builder.to_string());
}

bool MainWidget::read_file(Core::File& file)
{
    m_editor->set_text(file.read_all());
    set_path(file.filename());
    m_editor->set_focus(true);
    return true;
}

void MainWidget::open_nonexistent_file(String const& path)
{
    m_editor->set_text({});
    set_path(path);
    m_editor->set_focus(true);
}

bool MainWidget::request_close()
{
    if (!editor().document().is_modified())
        return true;
    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_path, editor().document().undo_stack().last_unmodified_timestamp());

    if (result == GUI::MessageBox::ExecYes) {
        m_save_action->activate();
        if (editor().document().is_modified())
            return false;
        return true;
    }

    if (result == GUI::MessageBox::ExecNo)
        return true;

    return false;
}

void MainWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "TextEditor can only open one file at a time!", "One at a time please!", GUI::MessageBox::Type::Error);
            return;
        }

        // TODO: A drop event should be considered user consent for opening a file
        auto response = FileSystemAccessClient::Client::the().try_request_file(window(), urls.first().path(), Core::OpenMode::ReadOnly);
        if (response.is_error())
            return;
        read_file(*response.value());
    }
}

void MainWidget::set_web_view_visible(bool visible)
{
    if (!visible && !m_page_view)
        return;
    ensure_web_view();
    auto& web_view_container = *find_descendant_of_type_named<GUI::Widget>("web_view_container");
    web_view_container.set_visible(visible);
}

void MainWidget::set_preview_mode(PreviewMode mode)
{
    if (m_preview_mode == mode)
        return;
    m_preview_mode = mode;

    if (m_preview_mode == PreviewMode::HTML) {
        m_html_preview_action->set_checked(true);
        set_web_view_visible(true);
        update_html_preview();
    } else if (m_preview_mode == PreviewMode::Markdown) {
        m_markdown_preview_action->set_checked(true);
        set_web_view_visible(true);
        update_markdown_preview();
    } else {
        m_no_preview_action->set_checked(true);
        set_web_view_visible(false);
    }
}

void MainWidget::update_preview()
{
    switch (m_preview_mode) {
    case PreviewMode::Markdown:
        update_markdown_preview();
        break;
    case PreviewMode::HTML:
        update_html_preview();
        break;
    default:
        break;
    }
}

void MainWidget::update_markdown_preview()
{
    auto document = Markdown::Document::parse(m_editor->text());
    if (document) {
        auto html = document->render_to_html();
        auto current_scroll_pos = m_page_view->visible_content_rect();
        m_page_view->load_html(html, URL::create_with_file_protocol(m_path));
        m_page_view->scroll_into_view(current_scroll_pos, true, true);
    }
}

void MainWidget::update_html_preview()
{
    auto current_scroll_pos = m_page_view->visible_content_rect();
    m_page_view->load_html(m_editor->text(), URL::create_with_file_protocol(m_path));
    m_page_view->scroll_into_view(current_scroll_pos, true, true);
}

void MainWidget::update_statusbar()
{
    if (!m_statusbar->is_visible())
        return;

    StringBuilder builder;
    if (m_editor->has_selection()) {
        String selected_text = m_editor->selected_text();
        auto word_count = m_editor->number_of_selected_words();
        builder.appendff("{} {} ({} {}) selected", selected_text.length(), selected_text.length() == 1 ? "character" : "characters", word_count, word_count != 1 ? "words" : "word");
    } else {
        String text = m_editor->text();
        auto word_count = m_editor->number_of_words();
        builder.appendff("{} {} ({} {})", text.length(), text.length() == 1 ? "character" : "characters", word_count, word_count != 1 ? "words" : "word");
    }
    m_statusbar->set_text(0, builder.to_string());

    if (m_editor && m_editor->syntax_highlighter()) {
        auto language = m_editor->syntax_highlighter()->language();
        m_statusbar->set_text(1, m_editor->syntax_highlighter()->language_string(language));
    }
    m_statusbar->set_text(2, String::formatted("Ln {}, Col {}", m_editor->cursor().line() + 1, m_editor->cursor().column()));
}

}
