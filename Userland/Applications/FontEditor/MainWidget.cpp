/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "GlyphEditorWidget.h"
#include "NewFontDialog.h"
#include <AK/Array.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <Applications/FontEditor/FontEditorWindowGML.h>
#include <Applications/FontEditor/FontPreviewWindowGML.h>
#include <LibConfig/Client.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GlyphMapWidget.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/Emoji.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/TextDirection.h>
#include <LibUnicode/CharacterTypes.h>

namespace FontEditor {

Resources g_resources;

static constexpr Array pangrams = {
    "quick fox jumps nightly above wizard"sv,
    "five quacking zephyrs jolt my wax bed"sv,
    "pack my box with five dozen liquor jugs"sv,
    "quick brown fox jumps over the lazy dog"sv,
    "waxy and quivering jocks fumble the pizza"sv,
    "~#:[@_1%]*{$2.3}/4^(5'6\")-&|7+8!=<9,0\\>?;"sv,
    "byxfjärmat föl gick på duvshowen"sv,
    "         "sv,
    "float Fox.quick(h){ is_brown && it_jumps_over(doges.lazy) }"sv,
    "<fox color=\"brown\" speed=\"quick\" jumps=\"over\">lazy dog</fox>"sv
};

ErrorOr<NonnullRefPtr<MainWidget>> MainWidget::try_create()
{
    auto main_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MainWidget()));
    TRY(main_widget->create_widgets());
    TRY(main_widget->create_actions());
    TRY(main_widget->create_models());
    TRY(main_widget->create_toolbars());
    TRY(main_widget->create_undo_stack());

    return main_widget;
}

ErrorOr<RefPtr<GUI::Window>> MainWidget::create_preview_window()
{
    auto window = GUI::Window::construct(this);
    window->set_window_mode(GUI::WindowMode::RenderAbove);
    window->set_title("Preview");
    window->resize(400, 150);
    window->center_within(*this->window());

    auto main_widget = window->set_main_widget<GUI::Widget>();
    TRY(main_widget->load_from_gml(font_preview_window_gml));

    m_preview_label = find_descendant_of_type_named<GUI::Label>("preview_label");
    m_preview_label->set_font(m_font);

    m_preview_textbox = find_descendant_of_type_named<GUI::TextBox>("preview_textbox");
    m_preview_textbox->on_change = [this] {
        auto maybe_preview = [](ByteString const& deprecated_text) -> ErrorOr<String> {
            auto text = TRY(String::from_byte_string(deprecated_text));
            return TRY(String::formatted("{}\n{}", text, TRY(text.to_uppercase())));
        }(m_preview_textbox->text());
        if (maybe_preview.is_error())
            return show_error(maybe_preview.release_error(), "Formatting preview text failed"sv);
        m_preview_label->set_text(maybe_preview.release_value());
    };
    m_preview_textbox->set_text(pangrams[0]);

    auto& reload_button = *find_descendant_of_type_named<GUI::Button>("reload_button");
    reload_button.on_click = [this](auto) {
        static size_t i = 1;
        if (i >= pangrams.size())
            i = 0;
        m_preview_textbox->set_text(pangrams[i]);
        i++;
    };

    return window;
}

ErrorOr<void> MainWidget::create_actions()
{
    m_new_action = GUI::Action::create("&New Font...", { Mod_Ctrl, Key_N }, g_resources.new_font, [this](auto&) {
        if (!request_close())
            return;
        auto maybe_wizard = NewFontDialog::create(window());
        if (maybe_wizard.is_error())
            return show_error(maybe_wizard.release_error(), "Creating font wizard failed"sv);
        auto wizard = maybe_wizard.release_value();
        if (wizard->exec() != GUI::Dialog::ExecResult::OK)
            return;
        auto maybe_font = wizard->create_font();
        if (maybe_font.is_error())
            return show_error(maybe_font.release_error(), "Creating new font failed"sv);
        if (auto result = initialize({}, move(maybe_font.value())); result.is_error())
            show_error(result.release_error(), "Initializing new font failed"sv);
    });
    m_new_action->set_status_tip("Create a new font"_string);

    m_open_action = GUI::CommonActions::make_open_action([this](auto&) {
        if (!request_close())
            return;
        FileSystemAccessClient::OpenFileOptions options {
            .window_title = "Open"sv,
            .path = "/res/fonts"sv,
            .allowed_file_types = { { GUI::FileTypeFilter { "Bitmap Font Files", { { "font" } } }, GUI::FileTypeFilter::all_files() } },
        };
        auto response = FileSystemAccessClient::Client::the().open_file(window(), options);
        if (response.is_error())
            return;
        auto file = response.release_value();
        if (auto result = open_file(file.filename(), file.release_stream()); result.is_error())
            show_error(result.release_error(), "Opening"sv, file.filename());
    });

    m_save_action = GUI::CommonActions::make_save_action([this](auto&) {
        if (m_path.is_empty())
            return m_save_as_action->activate();
        auto response = FileSystemAccessClient::Client::the().request_file(window(), m_path.to_byte_string(), Core::File::OpenMode::Truncate | Core::File::OpenMode::Write);
        if (response.is_error())
            return;
        auto file = response.release_value();
        if (auto result = save_file(m_path, file.release_stream()); result.is_error())
            show_error(result.release_error(), "Saving"sv, m_path);
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([this](auto&) {
        auto default_path = LexicalPath(m_path.is_empty() ? "Untitled.font"sv : m_path);
        auto response = FileSystemAccessClient::Client::the().save_file(window(), default_path.title(), default_path.extension());
        if (response.is_error())
            return;
        auto file = response.release_value();
        if (auto result = save_file(file.filename(), file.release_stream()); result.is_error())
            show_error(result.release_error(), "Saving"sv, file.filename());
        else
            GUI::Application::the()->set_most_recently_open_file(file.filename());
    });

    m_cut_action = GUI::CommonActions::make_cut_action([this](auto&) {
        if (auto result = cut_selected_glyphs(); result.is_error())
            show_error(result.release_error(), "Cutting selection failed"sv);
    });

    m_copy_action = GUI::CommonActions::make_copy_action([this](auto&) {
        if (auto result = copy_selected_glyphs(); result.is_error())
            show_error(result.release_error(), "Copying selection failed"sv);
    });

    m_paste_action = GUI::CommonActions::make_paste_action([this](auto&) {
        paste_glyphs();
    });
    m_paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "glyph/x-fonteditor");

    GUI::Clipboard::the().on_change = [this](ByteString const& data_type) {
        m_paste_action->set_enabled(data_type == "glyph/x-fonteditor");
    };

    m_delete_action = GUI::CommonActions::make_delete_action([this](auto&) {
        delete_selected_glyphs();
    });

    m_undo_action = GUI::CommonActions::make_undo_action([this](auto&) {
        undo();
    });
    m_undo_action->set_enabled(false);

    m_redo_action = GUI::CommonActions::make_redo_action([this](auto&) {
        redo();
    });
    m_redo_action->set_enabled(false);

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        m_glyph_map_widget->set_selection(m_range.first, m_range.last - m_range.first + 1);
        m_glyph_map_widget->update();
        auto selection = m_glyph_map_widget->selection().normalized();
        m_undo_selection->set_start(selection.start());
        m_undo_selection->set_size(selection.size());
        update_statusbar();
    });

    m_open_preview_action = GUI::Action::create("&Preview Font", { Mod_Ctrl, Key_P }, g_resources.preview_font, [this](auto&) {
        if (!m_font_preview_window) {
            if (auto maybe_window = create_preview_window(); maybe_window.is_error())
                show_error(maybe_window.release_error(), "Creating preview window failed"sv);
            else
                m_font_preview_window = maybe_window.release_value();
        }
        if (m_font_preview_window)
            m_font_preview_window->show();
    });
    m_open_preview_action->set_status_tip("Preview the current font"_string);

    bool show_metadata = Config::read_bool("FontEditor"sv, "Layout"sv, "ShowMetadata"sv, true);
    m_font_metadata_groupbox->set_visible(show_metadata);
    m_show_metadata_action = GUI::Action::create_checkable("Font &Metadata", { Mod_Ctrl, Key_M }, [this](auto& action) {
        m_font_metadata_groupbox->set_visible(action.is_checked());
        Config::write_bool("FontEditor"sv, "Layout"sv, "ShowMetadata"sv, action.is_checked());
    });
    m_show_metadata_action->set_checked(show_metadata);
    m_show_metadata_action->set_status_tip("Show or hide metadata about the current font"_string);

    bool show_unicode_blocks = Config::read_bool("FontEditor"sv, "Layout"sv, "ShowUnicodeBlocks"sv, true);
    m_unicode_block_container->set_visible(show_unicode_blocks);
    m_show_unicode_blocks_action = GUI::Action::create_checkable("&Unicode Blocks", { Mod_Ctrl, Key_U }, [this](auto& action) {
        m_unicode_block_container->set_visible(action.is_checked());
        if (action.is_checked())
            m_search_textbox->set_focus(m_initialized);
        else
            m_glyph_map_widget->set_focus(m_initialized);
        Config::write_bool("FontEditor"sv, "Layout"sv, "ShowUnicodeBlocks"sv, action.is_checked());
    });
    m_show_unicode_blocks_action->set_checked(show_unicode_blocks);
    m_show_unicode_blocks_action->set_status_tip("Show or hide the Unicode block list"_string);

    bool show_toolbar = Config::read_bool("FontEditor"sv, "Layout"sv, "ShowToolbar"sv, true);
    m_toolbar_container->set_visible(show_toolbar);
    m_show_toolbar_action = GUI::Action::create_checkable("&Toolbar", [this](auto& action) {
        m_toolbar_container->set_visible(action.is_checked());
        Config::write_bool("FontEditor"sv, "Layout"sv, "ShowToolbar"sv, action.is_checked());
    });
    m_show_toolbar_action->set_checked(show_toolbar);
    m_show_toolbar_action->set_status_tip("Show or hide the toolbar"_string);

    bool show_statusbar = Config::read_bool("FontEditor"sv, "Layout"sv, "ShowStatusbar"sv, true);
    m_statusbar->set_visible(show_statusbar);
    m_show_statusbar_action = GUI::Action::create_checkable("&Status Bar", [this](auto& action) {
        m_statusbar->set_visible(action.is_checked());
        update_statusbar();
        Config::write_bool("FontEditor"sv, "Layout"sv, "ShowStatusbar"sv, action.is_checked());
    });
    m_show_statusbar_action->set_checked(show_statusbar);
    m_show_statusbar_action->set_status_tip("Show or hide the status bar"_string);

    bool highlight_modifications = Config::read_bool("FontEditor"sv, "GlyphMap"sv, "HighlightModifications"sv, true);
    m_glyph_map_widget->set_highlight_modifications(highlight_modifications);
    m_highlight_modifications_action = GUI::Action::create_checkable("&Highlight Modifications", { Mod_Ctrl, Key_H }, [this](auto& action) {
        m_glyph_map_widget->set_highlight_modifications(action.is_checked());
        Config::write_bool("FontEditor"sv, "GlyphMap"sv, "HighlightModifications"sv, action.is_checked());
    });
    m_highlight_modifications_action->set_checked(highlight_modifications);
    m_highlight_modifications_action->set_status_tip("Show or hide highlights on modified glyphs"_string);

    bool show_system_emoji = Config::read_bool("FontEditor"sv, "GlyphMap"sv, "ShowSystemEmoji"sv, true);
    m_glyph_map_widget->set_show_system_emoji(show_system_emoji);
    m_show_system_emoji_action = GUI::Action::create_checkable("System &Emoji", { Mod_Ctrl, Key_E }, [this](auto& action) {
        m_glyph_map_widget->set_show_system_emoji(action.is_checked());
        Config::write_bool("FontEditor"sv, "GlyphMap"sv, "ShowSystemEmoji"sv, action.is_checked());
    });
    m_show_system_emoji_action->set_checked(show_system_emoji);
    m_show_system_emoji_action->set_status_tip("Show or hide system emoji"_string);

    m_go_to_glyph_action = GUI::Action::create("&Go to Glyph...", { Mod_Ctrl, Key_G }, g_resources.go_to_glyph, [this](auto&) {
        String input;
        auto result = GUI::InputBox::try_show(window(), input, {}, "Go to Glyph"sv, GUI::InputType::NonemptyText, "Hexadecimal"sv);
        if (!result.is_error() && result.value() == GUI::InputBox::ExecResult::OK) {
            auto maybe_code_point = AK::StringUtils::convert_to_uint_from_hex(input);
            if (!maybe_code_point.has_value())
                return;
            auto code_point = maybe_code_point.value();
            code_point = clamp(code_point, m_range.first, m_range.last);
            m_glyph_map_widget->set_focus(true);
            m_glyph_map_widget->set_active_glyph(code_point);
            m_glyph_map_widget->scroll_to_glyph(code_point);
        }
    });
    m_go_to_glyph_action->set_status_tip("Go to the specified code point"_string);

    m_previous_glyph_action = GUI::Action::create("Pre&vious Glyph", { Mod_Alt, Key_Left }, g_resources.previous_glyph, [this](auto&) {
        m_glyph_map_widget->select_previous_existing_glyph();
    });
    m_previous_glyph_action->set_status_tip("Seek the previous visible glyph"_string);

    m_next_glyph_action = GUI::Action::create("&Next Glyph", { Mod_Alt, Key_Right }, g_resources.next_glyph, [this](auto&) {
        m_glyph_map_widget->select_next_existing_glyph();
    });
    m_next_glyph_action->set_status_tip("Seek the next visible glyph"_string);

    i32 scale = Config::read_i32("FontEditor"sv, "GlyphEditor"sv, "Scale"sv, 10);
    m_glyph_editor_widget->set_scale(scale);
    m_scale_five_action = GUI::Action::create_checkable("500%", { Mod_Ctrl, Key_1 }, [this](auto&) {
        set_scale_and_save(5);
    });
    m_scale_five_action->set_checked(scale == 5);
    m_scale_five_action->set_status_tip("Scale the editor in proportion to the current font"_string);
    m_scale_ten_action = GUI::Action::create_checkable("1000%", { Mod_Ctrl, Key_2 }, [this](auto&) {
        set_scale_and_save(10);
    });
    m_scale_ten_action->set_checked(scale == 10);
    m_scale_ten_action->set_status_tip("Scale the editor in proportion to the current font"_string);
    m_scale_fifteen_action = GUI::Action::create_checkable("1500%", { Mod_Ctrl, Key_3 }, [this](auto&) {
        set_scale_and_save(15);
    });
    m_scale_fifteen_action->set_checked(scale == 15);
    m_scale_fifteen_action->set_status_tip("Scale the editor in proportion to the current font"_string);

    m_glyph_editor_scale_actions.add_action(*m_scale_five_action);
    m_glyph_editor_scale_actions.add_action(*m_scale_ten_action);
    m_glyph_editor_scale_actions.add_action(*m_scale_fifteen_action);
    m_glyph_editor_scale_actions.set_exclusive(true);

    m_paint_glyph_action = GUI::Action::create_checkable("Paint Glyph", { Mod_Ctrl, KeyCode::Key_J }, g_resources.paint_glyph, [this](auto&) {
        m_glyph_editor_widget->set_mode(GlyphEditorWidget::Paint);
    });
    m_paint_glyph_action->set_checked(true);

    m_move_glyph_action = GUI::Action::create_checkable("Move Glyph", { Mod_Ctrl, KeyCode::Key_K }, g_resources.move_glyph, [this](auto&) {
        m_glyph_editor_widget->set_mode(GlyphEditorWidget::Move);
    });

    m_glyph_tool_actions.add_action(*m_paint_glyph_action);
    m_glyph_tool_actions.add_action(*m_move_glyph_action);
    m_glyph_tool_actions.set_exclusive(true);

    m_rotate_counterclockwise_action = GUI::CommonActions::make_rotate_counterclockwise_action([this](auto&) {
        m_glyph_editor_widget->rotate_90(Gfx::RotationDirection::CounterClockwise);
    });

    m_rotate_clockwise_action = GUI::CommonActions::make_rotate_clockwise_action([this](auto&) {
        m_glyph_editor_widget->rotate_90(Gfx::RotationDirection::Clockwise);
    });

    m_flip_horizontal_action = GUI::Action::create("Flip Horizontally", { Mod_Ctrl | Mod_Shift, Key_Q }, g_resources.flip_horizontally, [this](auto&) {
        m_glyph_editor_widget->flip(Gfx::Orientation::Horizontal);
    });

    m_flip_vertical_action = GUI::Action::create("Flip Vertically", { Mod_Ctrl | Mod_Shift, Key_W }, g_resources.flip_vertically, [this](auto&) {
        m_glyph_editor_widget->flip(Gfx::Orientation::Vertical);
    });

    m_copy_text_action = GUI::Action::create("Copy as Te&xt", { Mod_Ctrl, Key_T }, g_resources.copy_as_text, [this](auto&) {
        StringBuilder builder;
        auto selection = m_glyph_map_widget->selection().normalized();
        for (auto code_point = selection.start(); code_point < selection.start() + selection.size(); ++code_point) {
            if (!m_glyph_map_widget->font().contains_glyph(code_point))
                continue;
            builder.append_code_point(code_point);
        }
        GUI::Clipboard::the().set_plain_text(builder.to_byte_string());
    });
    m_copy_text_action->set_status_tip("Copy to clipboard as text"_string);

    return {};
}

ErrorOr<void> MainWidget::create_toolbars()
{
    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_open_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_action(*m_delete_action);
    toolbar.add_separator();
    toolbar.add_action(*m_undo_action);
    toolbar.add_action(*m_redo_action);
    toolbar.add_separator();
    toolbar.add_action(*m_open_preview_action);
    toolbar.add_separator();
    toolbar.add_action(*m_previous_glyph_action);
    toolbar.add_action(*m_next_glyph_action);
    toolbar.add_action(*m_go_to_glyph_action);

    auto& glyph_transform_toolbar = *find_descendant_of_type_named<GUI::Toolbar>("glyph_transform_toolbar");
    glyph_transform_toolbar.add_action(*m_flip_horizontal_action);
    glyph_transform_toolbar.add_action(*m_flip_vertical_action);
    glyph_transform_toolbar.add_action(*m_rotate_counterclockwise_action);
    glyph_transform_toolbar.add_action(*m_rotate_clockwise_action);

    auto& glyph_mode_toolbar = *find_descendant_of_type_named<GUI::Toolbar>("glyph_mode_toolbar");
    glyph_mode_toolbar.add_action(*m_paint_glyph_action);
    glyph_mode_toolbar.add_action(*m_move_glyph_action);

    return {};
}

ErrorOr<void> MainWidget::create_models()
{
    TRY(m_font_slope_list.try_ensure_capacity(Gfx::font_slope_names.size()));
    for (auto& it : Gfx::font_slope_names)
        m_font_slope_list.unchecked_append(TRY(String::from_utf8(it.name)));
    m_slope_combobox->set_model(GUI::ItemListModel<String>::create(m_font_slope_list));

    TRY(m_font_weight_list.try_ensure_capacity(Gfx::font_weight_names.size()));
    for (auto& it : Gfx::font_weight_names)
        m_font_weight_list.unchecked_append(TRY(String::from_utf8(it.name)));
    m_weight_combobox->set_model(GUI::ItemListModel<String>::create(m_font_weight_list));

    auto unicode_blocks = Unicode::block_display_names();
    TRY(m_unicode_block_list.try_ensure_capacity(unicode_blocks.size() + 1));
    m_unicode_block_list.unchecked_append("Show All"_string);
    for (auto& block : unicode_blocks)
        m_unicode_block_list.unchecked_append(TRY(String::from_utf8(block.display_name)));

    m_unicode_block_model = GUI::ItemListModel<String>::create(m_unicode_block_list);
    m_filter_model = TRY(GUI::FilteringProxyModel::create(*m_unicode_block_model));
    m_filter_model->set_filter_term(""sv);

    m_unicode_block_listview = find_descendant_of_type_named<GUI::ListView>("unicode_block_listview");
    m_unicode_block_listview->on_selection_change = [this, unicode_blocks] {
        auto index = m_unicode_block_listview->selection().first();
        auto mapped_index = m_filter_model->map(index);
        if (mapped_index.row() > 0)
            m_range = unicode_blocks[mapped_index.row() - 1].code_point_range;
        else
            m_range = { 0x0000, 0x10FFFF };
        m_glyph_map_widget->set_active_range(m_range);
    };
    m_unicode_block_listview->set_model(m_filter_model);
    m_unicode_block_listview->set_activates_on_selection(true);
    m_unicode_block_listview->horizontal_scrollbar().set_visible(false);
    m_unicode_block_listview->set_cursor(m_unicode_block_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    m_unicode_block_listview->set_focus_proxy(m_search_textbox);

    return {};
}

ErrorOr<void> MainWidget::create_undo_stack()
{
    m_undo_stack = TRY(try_make<GUI::UndoStack>());
    m_undo_stack->on_state_change = [this] {
        m_undo_action->set_enabled(m_undo_stack->can_undo());
        m_redo_action->set_enabled(m_undo_stack->can_redo());
        update_action_text();
        if (m_undo_stack->is_current_modified())
            did_modify_font();
    };

    return {};
}

void MainWidget::update_action_text()
{
    auto text_or_error = [](auto prefix, auto suffix) -> ErrorOr<String> {
        StringBuilder builder;
        TRY(builder.try_append(prefix));
        if (suffix.has_value()) {
            TRY(builder.try_append(' '));
            TRY(builder.try_append(suffix.value()));
        }
        return builder.to_string();
    };

    if (auto maybe_text = text_or_error("&Undo"sv, m_undo_stack->undo_action_text()); !maybe_text.is_error())
        m_undo_action->set_text(maybe_text.release_value().to_byte_string());
    if (auto maybe_text = text_or_error("&Redo"sv, m_undo_stack->redo_action_text()); !maybe_text.is_error())
        m_redo_action->set_text(maybe_text.release_value().to_byte_string());
}

ErrorOr<void> MainWidget::create_widgets()
{
    TRY(load_from_gml(font_editor_window_gml));

    m_font_metadata_groupbox = find_descendant_of_type_named<GUI::GroupBox>("font_metadata_groupbox");
    m_unicode_block_container = find_descendant_of_type_named<GUI::Widget>("unicode_block_container");
    m_toolbar_container = find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    m_width_control_container = find_descendant_of_type_named<GUI::Widget>("width_control_container");

    m_glyph_map_widget = find_descendant_of_type_named<GUI::GlyphMapWidget>("glyph_map_widget");
    m_glyph_editor_widget = find_descendant_of_type_named<GlyphEditorWidget>("glyph_editor_widget");
    m_glyph_editor_widget->on_glyph_altered = [this](int glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_preview();
        did_modify_font();
    };

    m_glyph_editor_widget->on_undo_event = [this](auto action_text) {
        reset_selection();
        push_undo(action_text);
    };

    m_glyph_editor_width_spinbox = find_descendant_of_type_named<GUI::SpinBox>("glyph_editor_width_spinbox");
    m_glyph_editor_present_checkbox = find_descendant_of_type_named<GUI::CheckBox>("glyph_editor_present_checkbox");
    m_glyph_map_widget->on_active_glyph_changed = [this](int glyph) {
        if (m_undo_selection) {
            auto selection = m_glyph_map_widget->selection().normalized();
            m_undo_selection->set_start(selection.start());
            m_undo_selection->set_size(selection.size());
            m_undo_selection->set_active_glyph(glyph);
        }
        m_glyph_editor_widget->set_glyph(glyph);
        auto glyph_width = m_font->raw_glyph_width(glyph);
        if (m_font->is_fixed_width())
            m_glyph_editor_present_checkbox->set_checked(glyph_width > 0, GUI::AllowCallback::No);
        else
            m_glyph_editor_width_spinbox->set_value(glyph_width, GUI::AllowCallback::No);
        update_statusbar();
    };

    m_glyph_map_widget->on_context_menu_request = [this](auto& event) {
        m_context_menu->popup(event.screen_position());
    };

    m_glyph_map_widget->on_escape_pressed = [this]() {
        update_statusbar();
    };

    m_name_textbox = find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    m_name_textbox->on_change = [this] {
        m_font->set_name(MUST(String::from_byte_string(m_name_textbox->text())));
        did_modify_font();
    };

    m_family_textbox = find_descendant_of_type_named<GUI::TextBox>("family_textbox");
    m_family_textbox->on_change = [this] {
        m_font->set_family(MUST(String::from_byte_string(m_family_textbox->text())));
        did_modify_font();
    };

    m_fixed_width_checkbox = find_descendant_of_type_named<GUI::CheckBox>("fixed_width_checkbox");
    m_fixed_width_checkbox->on_checked = [this](bool checked) {
        m_font->set_fixed_width(checked);
        auto glyph_width = m_font->raw_glyph_width(m_glyph_map_widget->active_glyph());
        m_glyph_editor_width_spinbox->set_visible(!checked);
        m_glyph_editor_width_spinbox->set_value(glyph_width, GUI::AllowCallback::No);
        m_glyph_editor_present_checkbox->set_visible(checked);
        m_glyph_editor_present_checkbox->set_checked(glyph_width > 0, GUI::AllowCallback::No);
        m_glyph_editor_widget->update();
        update_preview();
        did_modify_font();
    };

    m_glyph_editor_width_spinbox->on_change = [this](int value) {
        reset_selection();
        push_undo("Resize Glyph"sv);
        m_font->set_glyph_width(m_glyph_map_widget->active_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->active_glyph());
        update_preview();
        update_statusbar();
        did_modify_font();
    };

    m_glyph_editor_present_checkbox->on_checked = [this](bool checked) {
        reset_selection();
        push_undo("Resize Glyph"sv);
        m_font->set_glyph_width(m_glyph_map_widget->active_glyph(), checked ? m_font->glyph_fixed_width() : 0);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->active_glyph());
        update_preview();
        update_statusbar();
        did_modify_font();
    };

    m_weight_combobox = find_descendant_of_type_named<GUI::ComboBox>("weight_combobox");
    m_weight_combobox->on_change = [this](auto&, auto&) {
        m_font->set_weight(Gfx::name_to_weight(m_weight_combobox->text()));
        did_modify_font();
    };

    m_slope_combobox = find_descendant_of_type_named<GUI::ComboBox>("slope_combobox");
    m_slope_combobox->on_change = [this](auto&, auto&) {
        m_font->set_slope(Gfx::name_to_slope(m_slope_combobox->text()));
        did_modify_font();
    };

    m_presentation_spinbox = find_descendant_of_type_named<GUI::SpinBox>("presentation_spinbox");
    m_presentation_spinbox->on_change = [this](int value) {
        m_font->set_presentation_size(value);
        update_preview();
        did_modify_font();
    };

    m_spacing_spinbox = find_descendant_of_type_named<GUI::SpinBox>("spacing_spinbox");
    m_spacing_spinbox->on_change = [this](int value) {
        m_font->set_glyph_spacing(value);
        update_preview();
        did_modify_font();
    };

    m_baseline_spinbox = find_descendant_of_type_named<GUI::SpinBox>("baseline_spinbox");
    m_baseline_spinbox->on_change = [this](int value) {
        m_font->set_baseline(value);
        m_glyph_editor_widget->update();
        update_preview();
        did_modify_font();
    };

    m_mean_line_spinbox = find_descendant_of_type_named<GUI::SpinBox>("mean_line_spinbox");
    m_mean_line_spinbox->on_change = [this](int value) {
        m_font->set_mean_line(value);
        m_glyph_editor_widget->update();
        update_preview();
        did_modify_font();
    };

    m_search_textbox = find_descendant_of_type_named<GUI::TextBox>("search_textbox");
    m_search_textbox->on_return_pressed = [this] {
        if (!m_unicode_block_listview->selection().is_empty())
            m_unicode_block_listview->activate_selected();
    };

    m_search_textbox->on_down_pressed = [this] {
        m_unicode_block_listview->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_search_textbox->on_up_pressed = [this] {
        m_unicode_block_listview->move_cursor(GUI::AbstractView::CursorMovement::Up, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_search_textbox->on_change = [this] {
        m_filter_model->set_filter_term(m_search_textbox->text());
        if (m_filter_model->row_count() != 0)
            m_unicode_block_listview->set_cursor(m_filter_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Auto);
    m_statusbar->segment(1).set_clickable(true);
    m_statusbar->segment(1).on_click = [this](auto) {
        m_show_unicode_blocks_action->activate();
    };

    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        m_statusbar->set_override_text(action.status_tip());
    };

    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };

    return {};
}

ErrorOr<void> MainWidget::initialize(StringView path, RefPtr<Gfx::BitmapFont>&& mutable_font)
{
    VERIFY(window());

    if (m_font == mutable_font)
        return {};

    ScopeGuard reset_on_error([&] {
        if (!m_initialized)
            reset();
    });

    m_initialized = false;
    m_path = TRY(String::from_utf8(path));
    m_font = move(mutable_font);

    TRY(m_glyph_map_widget->initialize(m_font));
    auto active_glyph = m_glyph_map_widget->active_glyph();
    m_glyph_map_widget->set_focus(true);
    m_glyph_map_widget->scroll_to_glyph(active_glyph);

    auto selection = m_glyph_map_widget->selection().normalized();
    m_undo_selection = TRY(try_make_ref_counted<UndoSelection>(selection.start(), selection.size(), active_glyph, *m_font, *m_glyph_map_widget));
    m_undo_stack->clear();

    if (m_preview_label)
        m_preview_label->set_font(*m_font);

    m_glyph_editor_widget->initialize(m_font);
    m_glyph_editor_widget->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
    m_glyph_editor_widget->set_glyph(active_glyph);

    m_glyph_editor_width_spinbox->set_visible(!m_font->is_fixed_width());
    m_glyph_editor_width_spinbox->set_max(m_font->max_glyph_width(), GUI::AllowCallback::No);
    m_glyph_editor_width_spinbox->set_value(m_font->raw_glyph_width(active_glyph), GUI::AllowCallback::No);

    m_glyph_editor_present_checkbox->set_visible(m_font->is_fixed_width());
    m_glyph_editor_present_checkbox->set_checked(m_font->contains_raw_glyph(active_glyph), GUI::AllowCallback::No);
    m_fixed_width_checkbox->set_checked(m_font->is_fixed_width(), GUI::AllowCallback::No);

    m_name_textbox->set_text(m_font->name(), GUI::AllowCallback::No);
    m_family_textbox->set_text(m_font->family(), GUI::AllowCallback::No);

    m_presentation_spinbox->set_value(m_font->presentation_size(), GUI::AllowCallback::No);
    m_spacing_spinbox->set_value(m_font->glyph_spacing(), GUI::AllowCallback::No);

    m_mean_line_spinbox->set_range(0, max(m_font->glyph_height() - 2, 0), GUI::AllowCallback::No);
    m_baseline_spinbox->set_range(0, max(m_font->glyph_height() - 2, 0), GUI::AllowCallback::No);
    m_mean_line_spinbox->set_value(m_font->mean_line(), GUI::AllowCallback::No);
    m_baseline_spinbox->set_value(m_font->baseline(), GUI::AllowCallback::No);

    for (size_t i = 0; i < Gfx::font_weight_names.size(); ++i) {
        if (Gfx::font_weight_names[i].style == m_font->weight()) {
            m_weight_combobox->set_selected_index(i, GUI::AllowCallback::No);
            break;
        }
    }
    for (size_t i = 0; i < Gfx::font_slope_names.size(); ++i) {
        if (Gfx::font_slope_names[i].style == m_font->slope()) {
            m_slope_combobox->set_selected_index(i, GUI::AllowCallback::No);
            break;
        }
    }

    window()->set_modified(false);
    update_title();
    update_statusbar();
    set_actions_enabled(true);
    set_widgets_enabled(true);
    m_initialized = true;

    return {};
}

ErrorOr<void> MainWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();
    file_menu->add_recent_files_list([this](auto& action) {
        if (!request_close())
            return;
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(this->window(), action.text());
        if (response.is_error())
            return;
        auto file = response.release_value();
        if (auto result = open_file(file.filename(), file.release_stream()); result.is_error())
            show_error(result.release_error(), "Opening"sv, file.filename());
    });
    file_menu->add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (!request_close())
            return;
        GUI::Application::the()->quit();
    }));

    auto edit_menu = window.add_menu("&Edit"_string);
    edit_menu->add_action(*m_undo_action);
    edit_menu->add_action(*m_redo_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_cut_action);
    edit_menu->add_action(*m_copy_action);
    edit_menu->add_action(*m_paste_action);
    edit_menu->add_action(*m_delete_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_select_all_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_copy_text_action);

    m_context_menu = edit_menu;

    auto go_menu = window.add_menu("&Go"_string);
    go_menu->add_action(*m_previous_glyph_action);
    go_menu->add_action(*m_next_glyph_action);
    go_menu->add_action(*m_go_to_glyph_action);

    auto view_menu = window.add_menu("&View"_string);
    auto layout_menu = view_menu->add_submenu("&Layout"_string);
    layout_menu->add_action(*m_show_toolbar_action);
    layout_menu->add_action(*m_show_statusbar_action);
    layout_menu->add_action(*m_show_metadata_action);
    layout_menu->add_action(*m_show_unicode_blocks_action);
    view_menu->add_separator();
    view_menu->add_action(*m_open_preview_action);
    view_menu->add_separator();
    view_menu->add_action(*m_highlight_modifications_action);
    view_menu->add_action(*m_show_system_emoji_action);
    view_menu->add_separator();
    auto scale_menu = view_menu->add_submenu("&Scale"_string);
    scale_menu->set_icon(g_resources.scale_editor);
    scale_menu->add_action(*m_scale_five_action);
    scale_menu->add_action(*m_scale_ten_action);
    scale_menu->add_action(*m_scale_fifteen_action);

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/FontEditor.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Font Editor"_string, TRY(GUI::Icon::try_create_default_icon("app-font-editor"sv)), &window));

    return {};
}

ErrorOr<void> MainWidget::save_file(StringView path, NonnullOwnPtr<Core::File> file)
{
    auto masked_font = TRY(m_font->masked_character_set());
    TRY(masked_font->write_to_file(move(file)));

    m_path = TRY(String::from_utf8(path));
    m_undo_stack->set_current_unmodified();
    window()->set_modified(false);
    update_title();
    return {};
}

ErrorOr<void> MainWidget::open_file(StringView path, NonnullOwnPtr<Core::File> file)
{
    auto mapped_file = TRY(Core::MappedFile::map_from_file(move(file), path));
    auto unmasked_font = TRY(TRY(Gfx::BitmapFont::try_load_from_mapped_file(move(mapped_file)))->unmasked_character_set());
    TRY(initialize(path, move(unmasked_font)));
    if (!path.is_empty())
        GUI::Application::the()->set_most_recently_open_file(path);
    return {};
}

void MainWidget::push_undo(StringView action_text)
{
    auto maybe_state = m_undo_selection->save_state();
    if (maybe_state.is_error())
        return show_error(maybe_state.release_error(), "Saving undo state failed"sv);
    auto maybe_text = String::from_utf8(action_text);
    if (maybe_text.is_error())
        return show_error(maybe_text.release_error(), "Creating action text failed"sv);
    auto maybe_command = try_make<SelectionUndoCommand>(*m_undo_selection, move(maybe_state.value()), move(maybe_text.value()));
    if (maybe_command.is_error())
        return show_error(maybe_command.release_error(), "Making undo command failed"sv);
    if (auto maybe_push = m_undo_stack->try_push(move(maybe_command.value())); maybe_push.is_error())
        show_error(maybe_push.release_error(), "Pushing undo stack failed"sv);
}

void MainWidget::reset_selection()
{
    auto selection = m_glyph_map_widget->selection().normalized();
    if (selection.size() == 1)
        return;
    auto start = m_glyph_map_widget->active_glyph();
    m_undo_selection->set_start(start);
    m_undo_selection->set_size(1);
    m_glyph_map_widget->set_selection(start, 1);
    m_glyph_map_widget->update();
}

void MainWidget::restore_state()
{
    auto glyph = m_undo_selection->restored_active_glyph();
    auto glyph_width = m_font->raw_glyph_width(glyph);
    if (glyph < m_range.first || glyph > m_range.last)
        m_search_textbox->set_text(""sv);

    auto start = m_undo_selection->restored_start();
    auto size = m_undo_selection->restored_size();
    m_glyph_map_widget->restore_selection(start, size, glyph);
    m_glyph_map_widget->scroll_to_glyph(glyph);
    m_glyph_map_widget->set_focus(true);

    if (m_font->is_fixed_width())
        m_glyph_editor_present_checkbox->set_checked(glyph_width > 0, GUI::AllowCallback::No);
    else
        m_glyph_editor_width_spinbox->set_value(glyph_width, GUI::AllowCallback::No);

    m_glyph_editor_widget->update();
    m_glyph_map_widget->update();
    update_preview();
    update_statusbar();
}

void MainWidget::undo()
{
    if (!m_undo_stack->can_undo())
        return;
    m_undo_stack->undo();
    restore_state();
}

void MainWidget::redo()
{
    if (!m_undo_stack->can_redo())
        return;
    m_undo_stack->redo();
    restore_state();
}

bool MainWidget::request_close()
{
    if (!window()->is_modified())
        return true;
    auto result = GUI::MessageBox::try_ask_about_unsaved_changes(window(), m_path, m_undo_stack->last_unmodified_timestamp());
    if (result.is_error())
        return false;
    if (result.value() == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        if (!window()->is_modified())
            return true;
    }
    if (result.value() == GUI::MessageBox::ExecResult::No)
        return true;
    return false;
}

void MainWidget::update_title()
{
    StringBuilder title;
    if (m_path.is_empty())
        title.append("Untitled"sv);
    else
        title.append(m_path);
    title.append("[*] - Font Editor"sv);
    window()->set_title(title.to_byte_string());
}

void MainWidget::did_modify_font()
{
    if (!window() || window()->is_modified())
        return;
    window()->set_modified(true);
    update_title();
}

void MainWidget::update_statusbar()
{
    if (!m_font)
        return;

    if (!m_statusbar->is_visible())
        return;

    auto format_statusbar = [this]() -> ErrorOr<void> {
        auto glyph = m_glyph_map_widget->active_glyph();
        StringBuilder builder;

        TRY(builder.try_appendff("U+{:04X} (", glyph));
        if (auto abbreviation = Unicode::code_point_abbreviation(glyph); abbreviation.has_value())
            TRY(builder.try_append(*abbreviation));
        // FIXME: Bidirectional text cannot currently be isolated; for now, replace RTL glyphs with U+FFFD.
        else if (Gfx::get_char_bidi_class(glyph) == Gfx::BidirectionalClass::STRONG_RTL)
            TRY(builder.try_append_code_point(0xFFFD));
        else
            TRY(builder.try_append_code_point(glyph));

        builder.append(')');

        auto glyph_name = Unicode::code_point_display_name(glyph);
        if (glyph_name.has_value())
            TRY(builder.try_appendff(" {}", glyph_name.value()));

        if (m_font->contains_raw_glyph(glyph))
            TRY(builder.try_appendff(" [{}x{}]", m_font->raw_glyph_width(glyph), m_font->glyph_height()));
        else if (Gfx::Emoji::emoji_for_code_point(glyph))
            TRY(builder.try_appendff(" [emoji]"));

        m_statusbar->set_text(0, TRY(builder.to_string()));

        builder.clear();

        auto selection = m_glyph_map_widget->selection().normalized();
        if (selection.size() > 1)
            TRY(builder.try_appendff("{} glyphs selected", selection.size()));
        else
            TRY(builder.try_appendff("U+{:04X}-U+{:04X}", m_range.first, m_range.last));
        m_statusbar->set_text(1, TRY(builder.to_string()));

        return {};
    }();

    if (format_statusbar.is_error())
        warnln("Formatting status bar failed");
}

void MainWidget::update_preview()
{
    if (m_font_preview_window)
        m_font_preview_window->update();
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void MainWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;

        window()->move_to_front();
        if (!request_close())
            return;

        auto file_path = URL::percent_decode(urls.first().serialize_path());
        auto result = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), file_path);
        if (result.is_error())
            return;

        auto file = result.release_value();
        if (auto result = open_file(file.filename(), file.release_stream()); result.is_error())
            show_error(result.release_error(), "Opening"sv, file.filename());
    }
}

void MainWidget::set_scale_and_save(i32 scale)
{
    Config::write_i32("FontEditor"sv, "GlyphEditor"sv, "Scale"sv, scale);
    m_glyph_editor_widget->set_scale(scale);
    m_glyph_editor_widget->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
}

ErrorOr<void> MainWidget::copy_selected_glyphs()
{
    size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * m_font->glyph_height();
    auto selection = m_glyph_map_widget->selection().normalized();
    auto rows = m_font->rows().slice(selection.start() * bytes_per_glyph, selection.size() * bytes_per_glyph);
    auto widths = m_font->widths().slice(selection.start(), selection.size());

    ByteBuffer buffer;
    TRY(buffer.try_append(rows));
    TRY(buffer.try_append(widths));

    HashMap<ByteString, ByteString> metadata;
    metadata.set("start", ByteString::number(selection.start()));
    metadata.set("count", ByteString::number(selection.size()));
    metadata.set("width", ByteString::number(m_font->max_glyph_width()));
    metadata.set("height", ByteString::number(m_font->glyph_height()));
    GUI::Clipboard::the().set_data(buffer.bytes(), "glyph/x-fonteditor", metadata);

    return {};
}

ErrorOr<void> MainWidget::cut_selected_glyphs()
{
    TRY(copy_selected_glyphs());
    delete_selected_glyphs();
    return {};
}

void MainWidget::paste_glyphs()
{
    auto [data, mime_type, metadata] = GUI::Clipboard::the().fetch_data_and_type();
    if (!mime_type.starts_with("glyph/x-fonteditor"sv))
        return;

    auto glyph_count = metadata.get("count").value().to_number<unsigned>().value_or(0);
    if (!glyph_count)
        return;

    auto height = metadata.get("height").value().to_number<unsigned>().value_or(0);
    if (!height)
        return;

    auto selection = m_glyph_map_widget->selection().normalized();
    auto range_bound_glyph_count = min(glyph_count, 1 + m_range.last - selection.start());
    m_undo_selection->set_size(range_bound_glyph_count);
    auto action_text = range_bound_glyph_count == 1 ? "Paste Glyph"sv : "Paste Glyphs"sv;
    push_undo(action_text);

    size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * m_font->glyph_height();
    size_t bytes_per_copied_glyph = Gfx::GlyphBitmap::bytes_per_row() * height;
    size_t copyable_bytes_per_glyph = min(bytes_per_glyph, bytes_per_copied_glyph);
    auto rows = m_font->rows().slice(selection.start() * bytes_per_glyph);
    auto widths = m_font->widths().slice(selection.start());

    for (size_t i = 0; i < range_bound_glyph_count; ++i) {
        auto copyable_width = m_font->is_fixed_width()
            ? data[bytes_per_copied_glyph * glyph_count + i] ? m_font->glyph_fixed_width() : 0
            : min(m_font->max_glyph_width(), data[bytes_per_copied_glyph * glyph_count + i]);
        memcpy(&rows[i * bytes_per_glyph], &data[i * bytes_per_copied_glyph], copyable_bytes_per_glyph);
        memset(&widths[i], copyable_width, sizeof(u8));
        m_glyph_map_widget->set_glyph_modified(selection.start() + i, true);
    }

    m_glyph_map_widget->set_selection(selection.start() + range_bound_glyph_count - 1, -range_bound_glyph_count + 1);

    if (m_font->is_fixed_width())
        m_glyph_editor_present_checkbox->set_checked(m_font->contains_raw_glyph(m_glyph_map_widget->active_glyph()), GUI::AllowCallback::No);
    else
        m_glyph_editor_width_spinbox->set_value(m_font->raw_glyph_width(m_glyph_map_widget->active_glyph()), GUI::AllowCallback::No);

    m_glyph_editor_widget->update();
    m_glyph_map_widget->update();
    update_preview();
    update_statusbar();
}

void MainWidget::delete_selected_glyphs()
{
    auto selection = m_glyph_map_widget->selection().normalized();
    auto action_text = selection.size() == 1 ? "Delete Glyph"sv : "Delete Glyphs"sv;
    push_undo(action_text);

    size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * m_font->glyph_height();
    auto rows = m_font->rows().slice(selection.start() * bytes_per_glyph, selection.size() * bytes_per_glyph);
    auto widths = m_font->widths().slice(selection.start(), selection.size());
    memset(rows.data(), 0, bytes_per_glyph * selection.size());
    memset(widths.data(), 0, selection.size());

    if (m_font->is_fixed_width())
        m_glyph_editor_present_checkbox->set_checked(false, GUI::AllowCallback::No);
    else
        m_glyph_editor_width_spinbox->set_value(0, GUI::AllowCallback::No);

    m_glyph_editor_widget->update();
    m_glyph_map_widget->update();
    update_preview();
    update_statusbar();
}

void MainWidget::show_error(Error error, StringView action, StringView filename)
{
    auto format = filename.is_null() ? "{}{}: {}"sv : "{} \"{}\" failed: {}"sv;
    auto file = filename.is_null() ? StringView {} : filename;
    warnln(format, action, file, error);

    auto maybe_message = String::formatted(format, action, file, error);
    if (!maybe_message.is_error())
        (void)GUI::MessageBox::try_show_error(window(), maybe_message.release_value());
}

void MainWidget::reset()
{
    VERIFY(window());

    m_initialized = false;
    m_font = nullptr;
    m_path = {};
    m_undo_selection = nullptr;
    m_undo_stack->clear();

    (void)m_glyph_map_widget->initialize(nullptr);
    m_glyph_editor_widget->initialize(nullptr);

    if (m_font_preview_window)
        m_font_preview_window->close();
    if (m_preview_label)
        m_preview_label->set_font(nullptr);

    m_name_textbox->set_text({}, GUI::AllowCallback::No);
    m_family_textbox->set_text({}, GUI::AllowCallback::No);
    m_slope_combobox->set_text({}, GUI::AllowCallback::No);
    m_weight_combobox->set_text({}, GUI::AllowCallback::No);
    m_presentation_spinbox->set_text({}, GUI::AllowCallback::No);
    m_baseline_spinbox->set_text({}, GUI::AllowCallback::No);
    m_mean_line_spinbox->set_text({}, GUI::AllowCallback::No);
    m_spacing_spinbox->set_text({}, GUI::AllowCallback::No);
    m_fixed_width_checkbox->set_checked(false, GUI::AllowCallback::No);
    m_statusbar->set_text(0, {});
    m_statusbar->set_text(1, {});

    window()->set_modified(false);
    window()->set_title("Font Editor");
    set_actions_enabled(false);
    set_widgets_enabled(false);
    set_focus(true);
}

void MainWidget::set_actions_enabled(bool enabled)
{
    m_save_action->set_enabled(enabled);
    m_save_as_action->set_enabled(enabled);

    m_cut_action->set_enabled(enabled);
    m_copy_action->set_enabled(enabled);
    m_paste_action->set_enabled(enabled && GUI::Clipboard::the().fetch_mime_type() == "glyph/x-fonteditor");
    m_delete_action->set_enabled(enabled);

    m_copy_text_action->set_enabled(enabled);
    m_select_all_action->set_enabled(enabled);

    m_go_to_glyph_action->set_enabled(enabled);
    m_previous_glyph_action->set_enabled(enabled);
    m_next_glyph_action->set_enabled(enabled);

    m_move_glyph_action->set_enabled(enabled);
    m_paint_glyph_action->set_enabled(enabled);

    m_flip_horizontal_action->set_enabled(enabled);
    m_flip_vertical_action->set_enabled(enabled);
    m_rotate_clockwise_action->set_enabled(enabled);
    m_rotate_counterclockwise_action->set_enabled(enabled);

    m_open_preview_action->set_enabled(enabled);
    m_highlight_modifications_action->set_enabled(enabled);
    m_show_system_emoji_action->set_enabled(enabled);

    m_scale_five_action->set_enabled(enabled);
    m_scale_ten_action->set_enabled(enabled);
    m_scale_fifteen_action->set_enabled(enabled);
}

void MainWidget::set_widgets_enabled(bool enabled)
{
    m_font_metadata_groupbox->set_enabled(enabled);
    m_unicode_block_container->set_enabled(enabled);
    m_width_control_container->set_enabled(enabled);
    m_width_control_container->set_visible(enabled);

    m_glyph_map_widget->set_enabled(enabled);
    m_glyph_editor_widget->set_enabled(enabled);
    m_glyph_editor_widget->set_visible(enabled);
    m_statusbar->segment(1).set_visible(enabled);
}

}
