/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace TextEditor {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();
    ErrorOr<void> initialize();

    virtual ~MainWidget() override = default;
    ErrorOr<void> read_file(ByteString const& filename, Core::File&);
    void open_nonexistent_file(ByteString const& path);
    bool request_close();

    GUI::TextEditor& editor() { return *m_editor; }

    enum class PreviewMode {
        None,
        Markdown,
        HTML,
        Gemtext,
    };

    void set_preview_mode(PreviewMode);
    void set_auto_detect_preview_mode(bool value) { m_auto_detect_preview_mode = value; }

    void update_title();
    void update_statusbar();
    ErrorOr<void> initialize_menubar(GUI::Window&);

private:
    MainWidget() = default;
    void set_path(StringView);
    void update_preview();
    void update_markdown_preview();
    void update_html_preview();
    void update_gemtext_preview();

    WebView::OutOfProcessWebView& ensure_web_view();
    void set_web_view_visible(bool);

    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    enum class ShowMessageIfNoResults {
        Yes = 1,
        No = 0
    };
    void find_text(GUI::TextEditor::SearchDirection, ShowMessageIfNoResults);

    RefPtr<GUI::TextEditor> m_editor;
    ByteString m_path;
    ByteString m_name;
    ByteString m_extension;
    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_open_folder_action;
    RefPtr<GUI::Action> m_find_replace_action;
    RefPtr<GUI::Action> m_vim_emulation_setting_action;

    RefPtr<GUI::Action> m_find_next_action;
    RefPtr<GUI::Action> m_find_previous_action;
    RefPtr<GUI::Action> m_replace_action;
    RefPtr<GUI::Action> m_replace_all_action;

    RefPtr<GUI::Action> m_layout_toolbar_action;
    RefPtr<GUI::Action> m_layout_statusbar_action;
    RefPtr<GUI::Action> m_layout_ruler_action;

    GUI::ActionGroup m_preview_actions;
    RefPtr<GUI::Action> m_no_preview_action;
    RefPtr<GUI::Action> m_markdown_preview_action;
    RefPtr<GUI::Action> m_html_preview_action;
    RefPtr<GUI::Action> m_gemtext_preview_action;

    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::Menu> m_line_column_statusbar_menu;
    RefPtr<GUI::Menu> m_syntax_statusbar_menu;

    RefPtr<GUI::TextBox> m_find_textbox;
    RefPtr<GUI::TextBox> m_replace_textbox;
    RefPtr<GUI::Button> m_find_previous_button;
    RefPtr<GUI::Button> m_find_next_button;
    RefPtr<GUI::Button> m_replace_button;
    RefPtr<GUI::Button> m_replace_all_button;
    RefPtr<GUI::Widget> m_find_replace_widget;
    RefPtr<GUI::Widget> m_find_widget;
    RefPtr<GUI::Widget> m_replace_widget;
    RefPtr<GUI::CheckBox> m_regex_checkbox;
    RefPtr<GUI::CheckBox> m_match_case_checkbox;
    RefPtr<GUI::CheckBox> m_wrap_around_checkbox;

    GUI::ActionGroup m_wrapping_mode_actions;
    RefPtr<GUI::Action> m_no_wrapping_action;
    RefPtr<GUI::Action> m_wrap_anywhere_action;
    RefPtr<GUI::Action> m_wrap_at_words_action;

    RefPtr<GUI::Action> m_visualize_trailing_whitespace_action;
    RefPtr<GUI::Action> m_visualize_leading_whitespace_action;
    RefPtr<GUI::Action> m_cursor_line_highlighting_action;
    RefPtr<GUI::Action> m_relative_line_number_action;

    GUI::ActionGroup m_soft_tab_width_actions;
    RefPtr<GUI::Action> m_soft_tab_1_width_action;
    RefPtr<GUI::Action> m_soft_tab_2_width_action;
    RefPtr<GUI::Action> m_soft_tab_4_width_action;
    RefPtr<GUI::Action> m_soft_tab_8_width_action;
    RefPtr<GUI::Action> m_soft_tab_16_width_action;

    GUI::ActionGroup syntax_actions;
    RefPtr<GUI::Action> m_plain_text_highlight;
    RefPtr<GUI::Action> m_cmake_highlight;
    RefPtr<GUI::Action> m_cmakecache_highlight;
    RefPtr<GUI::Action> m_cpp_highlight;
    RefPtr<GUI::Action> m_css_highlight;
    RefPtr<GUI::Action> m_js_highlight;
    RefPtr<GUI::Action> m_html_highlight;
    RefPtr<GUI::Action> m_git_highlight;
    RefPtr<GUI::Action> m_gml_highlight;
    RefPtr<GUI::Action> m_ini_highlight;
    RefPtr<GUI::Action> m_markdown_highlight;
    RefPtr<GUI::Action> m_shell_highlight;
    RefPtr<GUI::Action> m_sql_highlight;

    RefPtr<WebView::OutOfProcessWebView> m_page_view;

    bool m_auto_detect_preview_mode { false };
    bool m_use_regex { false };
    bool m_match_case { true };
    bool m_should_wrap { true };

    PreviewMode m_preview_mode { PreviewMode::None };
};

}
