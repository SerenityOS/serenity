/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontEditor.h"
#include "GlyphEditorWidget.h"
#include "GlyphMapWidget.h"
#include "NewFontDialog.h"
#include <AK/StringBuilder.h>
#include <AK/UnicodeUtils.h>
#include <Applications/FontEditor/FontEditorWindowGML.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontPickerWeightModel.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextDirection.h>
#include <stdlib.h>

static constexpr int s_pangram_count = 7;
static const char* pangrams[s_pangram_count] = {
    "quick fox jumps nightly above wizard",
    "five quacking zephyrs jolt my wax bed",
    "pack my box with five dozen liquor jugs",
    "quick brown fox jumps over the lazy dog",
    "waxy and quivering jocks fumble the pizza",
    "~#:[@_1%]*{$2.3}/4^(5'6\")-&|7+8!=<9,0\\>?;",
    "byxfjärmat föl gick på duvshowen"
};

static RefPtr<GUI::Window> create_font_preview_window(FontEditorWidget& editor)
{
    auto window = GUI::Window::construct();
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Font preview");
    window->resize(400, 150);
    window->set_minimum_size(200, 100);
    window->center_within(*editor.window());

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins({ 2, 2, 2, 2 });
    main_widget.layout()->set_spacing(4);

    auto& preview_box = main_widget.add<GUI::GroupBox>();
    preview_box.set_layout<GUI::VerticalBoxLayout>();
    preview_box.layout()->set_margins({ 8, 8, 8, 8 });

    auto& preview_label = preview_box.add<GUI::Label>();
    preview_label.set_font(editor.edited_font());

    editor.on_initialize = [&] {
        preview_label.set_font(editor.edited_font());
    };

    auto& textbox_button_container = main_widget.add<GUI::Widget>();
    textbox_button_container.set_layout<GUI::HorizontalBoxLayout>();
    textbox_button_container.set_fixed_height(22);

    auto& preview_textbox = textbox_button_container.add<GUI::TextBox>();
    preview_textbox.set_text(pangrams[0]);
    preview_textbox.set_placeholder("Preview text");

    preview_textbox.on_change = [&] {
        auto preview = String::formatted("{}\n{}",
            preview_textbox.text(),
            preview_textbox.text().to_uppercase());
        preview_label.set_text(preview);
    };

    auto& reload_button = textbox_button_container.add<GUI::Button>();
    reload_button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"));
    reload_button.set_fixed_width(22);
    reload_button.on_click = [&](auto) {
        static int i = 1;
        if (i >= s_pangram_count)
            i = 0;
        preview_textbox.set_text(pangrams[i]);
        i++;
    };

    return window;
}

FontEditorWidget::FontEditorWidget(const String& path, RefPtr<Gfx::BitmapFont>&& edited_font)
{
    load_from_gml(font_editor_window_gml);

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    auto& statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    auto& glyph_map_container = *find_descendant_of_type_named<GUI::Widget>("glyph_map_container");
    auto& move_glyph_button = *find_descendant_of_type_named<GUI::Button>("move_glyph_button");
    m_glyph_editor_container = *find_descendant_of_type_named<GUI::Widget>("glyph_editor_container");
    m_left_column_container = *find_descendant_of_type_named<GUI::Widget>("left_column_container");
    m_glyph_editor_width_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("glyph_editor_width_spinbox");
    m_glyph_editor_present_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("glyph_editor_present_checkbox");
    m_name_textbox = *find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    m_family_textbox = *find_descendant_of_type_named<GUI::TextBox>("family_textbox");
    m_presentation_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("presentation_spinbox");
    m_weight_combobox = *find_descendant_of_type_named<GUI::ComboBox>("weight_combobox");
    m_type_combobox = *find_descendant_of_type_named<GUI::ComboBox>("type_combobox");
    m_spacing_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("spacing_spinbox");
    m_mean_line_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("mean_line_spinbox");
    m_baseline_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("baseline_spinbox");
    m_fixed_width_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("fixed_width_checkbox");
    m_font_metadata_groupbox = *find_descendant_of_type_named<GUI::GroupBox>("font_metadata_groupbox");

    m_glyph_editor_widget = m_glyph_editor_container->add<GlyphEditorWidget>();
    m_glyph_map_widget = glyph_map_container.add<GlyphMapWidget>();

    auto update_statusbar = [&] {
        auto glyph = m_glyph_map_widget->selected_glyph();
        StringBuilder builder;
        builder.appendff("U+{:04X} (", glyph);

        if (AK::UnicodeUtils::is_unicode_control_code_point(glyph)) {
            builder.append(AK::UnicodeUtils::get_unicode_control_code_point_alias(glyph).value());
        } else if (Gfx::get_char_bidi_class(glyph) == Gfx::BidirectionalClass::STRONG_RTL) {
            // FIXME: This is a necessary hack, as RTL text will mess up the painting of the statusbar text.
            // For now, replace RTL glyphs with U+FFFD, the replacement character.
            builder.append_code_point(0xFFFD);
        } else {
            builder.append_code_point(glyph);
        }

        builder.append(")");
        if (m_edited_font->raw_glyph_width(glyph) > 0)
            builder.appendff(" [{}x{}]", m_edited_font->raw_glyph_width(glyph), m_edited_font->glyph_height());
        statusbar.set_text(builder.to_string());
    };

    auto update_demo = [&] {
        if (m_font_preview_window)
            m_font_preview_window->update();
    };

    m_new_action = GUI::Action::create("&New Font...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-font.png"), [&](auto&) {
        if (m_font_modified) {
            auto result = GUI::MessageBox::show(window(), "Save changes to the current font?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
            if (result == GUI::Dialog::ExecResult::ExecYes) {
                m_save_action->activate();
                if (m_font_modified)
                    return;
            }
            if (result == GUI::Dialog::ExecResult::ExecCancel)
                return;
        }
        auto new_font_wizard = NewFontDialog::construct(window());
        if (new_font_wizard->exec() == GUI::Dialog::ExecOK) {
            auto metadata = new_font_wizard->new_font_metadata();

            String name = metadata.name;
            auto parts = name.split(' ');
            if (parts.size() > 1)
                name = parts[0];
            parts.clear();

            String weight = GUI::weight_to_name(metadata.weight).to_string();
            parts = weight.split(' ');
            if (parts.size() > 1)
                weight = String::formatted("{}{}", parts[0], parts[1]);

            RefPtr<Gfx::BitmapFont> new_font = Gfx::BitmapFont::create(metadata.glyph_height, metadata.glyph_width, metadata.is_fixed_width, metadata.type);
            String path = String::formatted("{}{}{}.font", name, weight, metadata.presentation_size);
            if (!new_font) {
                String message = String::formatted("Failed to create new font: {}\n", path);
                GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
                return;
            }

            new_font->set_name(metadata.name);
            new_font->set_family(metadata.family);
            new_font->set_presentation_size(metadata.presentation_size);
            new_font->set_weight(metadata.weight);
            new_font->set_baseline(metadata.baseline);
            new_font->set_mean_line(metadata.mean_line);
            m_font_modified = true;
            initialize(path, move(new_font));
        }
    });
    m_new_action->set_status_tip("Create a new font");
    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        if (m_font_modified) {
            auto result = GUI::MessageBox::show(window(), "Save changes to the current font?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
            if (result == GUI::Dialog::ExecResult::ExecYes) {
                m_save_action->activate();
                if (m_font_modified)
                    return;
            }
            if (result == GUI::Dialog::ExecResult::ExecCancel)
                return;
        }
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window(), {}, "/res/fonts/");
        if (!open_path.has_value())
            return;

        auto bitmap_font = Gfx::BitmapFont::load_from_file(open_path.value());
        if (!bitmap_font) {
            String message = String::formatted("Couldn't load font: {}\n", open_path.value());
            GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
            return;
        }
        RefPtr<Gfx::BitmapFont> new_font = static_ptr_cast<Gfx::BitmapFont>(bitmap_font->clone());
        if (!new_font) {
            String message = String::formatted("Couldn't load font: {}\n", open_path.value());
            GUI::MessageBox::show(window(), message, "Font Editor", GUI::MessageBox::Type::Error);
            return;
        }
        m_font_newly_opened = true;
        initialize(open_path.value(), move(new_font));
    });
    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        LexicalPath lexical_path(m_path);
        if (!lexical_path.dirname().is_empty())
            save_as(m_path);
        else
            m_save_as_action->activate();
    });
    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        LexicalPath lexical_path(m_path);
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(), lexical_path.title(), lexical_path.extension());
        if (!save_path.has_value())
            return;
        save_as(save_path.value());
    });
    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        m_glyph_editor_widget->cut_glyph();
    });
    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        m_glyph_editor_widget->copy_glyph();
    });
    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        m_glyph_editor_widget->paste_glyph();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
    });
    m_paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "glyph/x-fonteditor");
    m_delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), m_edited_font->max_glyph_width());
        m_glyph_editor_widget->delete_glyph();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        auto glyph_width = m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph());
        m_glyph_editor_width_spinbox->set_value(glyph_width);
        m_glyph_editor_present_checkbox->set_checked(glyph_width > 0);
    });
    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        undo();
    });
    m_undo_action->set_enabled(false);
    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        redo();
    });
    m_redo_action->set_enabled(false);
    m_open_preview_action = GUI::Action::create("&Preview Font", { Mod_Ctrl, Key_P }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"), [&](auto&) {
        if (!m_font_preview_window)
            m_font_preview_window = create_font_preview_window(*this);
        m_font_preview_window->show();
        m_font_preview_window->move_to_front();
    });
    m_open_preview_action->set_checked(false);
    m_open_preview_action->set_status_tip("Preview the current font");
    m_show_metadata_action = GUI::Action::create_checkable("Font &Metadata", { Mod_Ctrl, Key_M }, [&](auto& action) {
        set_show_font_metadata(action.is_checked());
    });
    m_show_metadata_action->set_checked(true);
    m_show_metadata_action->set_status_tip("Show or hide metadata about the current font");

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

    m_scale_five_action = GUI::Action::create_checkable("500%", { Mod_Ctrl, Key_1 }, [&](auto&) {
        m_glyph_editor_widget->set_scale(5);
        m_glyph_editor_container->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
        m_left_column_container->set_fixed_width(m_glyph_editor_widget->preferred_width());
    });
    m_scale_five_action->set_checked(false);
    m_scale_five_action->set_status_tip("Scale the editor in proportion to the current font");
    m_scale_ten_action = GUI::Action::create_checkable("1000%", { Mod_Ctrl, Key_2 }, [&](auto&) {
        m_glyph_editor_widget->set_scale(10);
        m_glyph_editor_container->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
        m_left_column_container->set_fixed_width(m_glyph_editor_widget->preferred_width());
    });
    m_scale_ten_action->set_checked(true);
    m_scale_ten_action->set_status_tip("Scale the editor in proportion to the current font");
    m_scale_fifteen_action = GUI::Action::create_checkable("1500%", { Mod_Ctrl, Key_3 }, [&](auto&) {
        m_glyph_editor_widget->set_scale(15);
        m_glyph_editor_container->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
        m_left_column_container->set_fixed_width(m_glyph_editor_widget->preferred_width());
    });
    m_scale_fifteen_action->set_checked(false);
    m_scale_fifteen_action->set_status_tip("Scale the editor in proportion to the current font");

    m_glyph_editor_scale_actions.add_action(*m_scale_five_action);
    m_glyph_editor_scale_actions.add_action(*m_scale_ten_action);
    m_glyph_editor_scale_actions.add_action(*m_scale_fifteen_action);
    m_glyph_editor_scale_actions.set_exclusive(true);

    move_glyph_button.on_click = [&](auto) {
        if (move_glyph_button.is_checked())
            m_glyph_editor_widget->set_mode(GlyphEditorWidget::Move);
        else
            m_glyph_editor_widget->set_mode(GlyphEditorWidget::Paint);
    };
    move_glyph_button.set_checkable(true);
    move_glyph_button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/selection-move.png"));

    GUI::Clipboard::the().on_change = [&](const String& data_type) {
        m_paste_action->set_enabled(data_type == "glyph/x-fonteditor");
    };

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo](int glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_demo();
        did_modify_font();
    };

    m_glyph_editor_widget->on_undo_event = [this](bool) {
        // FIXME: UndoStack no longer has finalization concept, so this needs some fixing.
        m_undo_stack->push(make<GlyphUndoCommand>(*m_undo_glyph));
        did_change_undo_stack();
    };

    m_glyph_map_widget->on_glyph_selected = [&, update_statusbar](int glyph) {
        if (m_undo_glyph)
            m_undo_glyph->set_code_point(glyph);
        m_glyph_editor_widget->set_glyph(glyph);
        auto glyph_width = m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph());
        m_glyph_editor_width_spinbox->set_value(glyph_width);
        m_glyph_editor_present_checkbox->set_checked(glyph_width > 0);
        update_statusbar();
    };

    m_name_textbox->on_change = [&] {
        m_edited_font->set_name(m_name_textbox->text());
        did_modify_font();
    };

    m_family_textbox->on_change = [&] {
        m_edited_font->set_family(m_family_textbox->text());
        did_modify_font();
    };

    m_fixed_width_checkbox->on_checked = [&, update_demo](bool checked) {
        m_edited_font->set_fixed_width(checked);
        auto glyph_width = m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph());
        m_glyph_editor_width_spinbox->set_visible(!checked);
        m_glyph_editor_width_spinbox->set_value(glyph_width);
        m_glyph_editor_present_checkbox->set_visible(checked);
        m_glyph_editor_present_checkbox->set_checked(glyph_width > 0);
        m_glyph_editor_widget->update();
        update_demo();
        did_modify_font();
    };

    m_glyph_editor_width_spinbox->on_change = [this, update_demo, update_statusbar](int value) {
        if (m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph()) == value)
            return;
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
        update_statusbar();
        did_modify_font();
    };

    m_glyph_editor_present_checkbox->on_checked = [this, update_demo, update_statusbar](bool checked) {
        if (!m_edited_font->is_fixed_width()
            || m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph()) == checked
            || (m_edited_font->raw_glyph_width(m_glyph_map_widget->selected_glyph()) && checked))
            return;
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), checked ? m_edited_font->glyph_fixed_width() : 0);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
        update_statusbar();
        did_modify_font();
    };

    m_weight_combobox->on_change = [this](auto&, auto&) {
        m_edited_font->set_weight(GUI::name_to_weight(m_weight_combobox->text()));
        did_modify_font();
    };

    m_type_combobox->on_change = [this](auto&, const auto& index) {
        m_edited_font->set_type(static_cast<Gfx::FontTypes>(index.row()));
        m_glyph_map_widget->reprobe_font();
        did_modify_font();
    };

    m_presentation_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_presentation_size(value);
        update_demo();
        did_modify_font();
    };

    m_spacing_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_spacing(value);
        update_demo();
        did_modify_font();
    };

    m_baseline_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_baseline(value);
        m_glyph_editor_widget->update();
        update_demo();
        did_modify_font();
    };

    m_mean_line_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_mean_line(value);
        m_glyph_editor_widget->update();
        update_demo();
        did_modify_font();
    };

    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    initialize(path, move(edited_font));
}

FontEditorWidget::~FontEditorWidget()
{
}

void FontEditorWidget::initialize(const String& path, RefPtr<Gfx::BitmapFont>&& edited_font)
{
    if (m_edited_font == edited_font)
        return;
    m_path = path;
    m_edited_font = edited_font;

    m_glyph_map_widget->initialize(*m_edited_font);
    m_glyph_editor_widget->initialize(*m_edited_font);

    m_glyph_editor_container->set_fixed_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());
    m_left_column_container->set_fixed_width(m_glyph_editor_widget->preferred_width());
    m_glyph_editor_width_spinbox->set_visible(!m_edited_font->is_fixed_width());
    m_glyph_editor_width_spinbox->set_max(m_edited_font->max_glyph_width());
    m_glyph_editor_present_checkbox->set_visible(m_edited_font->is_fixed_width());

    m_name_textbox->set_text(m_edited_font->name());
    m_family_textbox->set_text(m_edited_font->family());

    m_presentation_spinbox->set_value(m_edited_font->presentation_size());
    m_spacing_spinbox->set_value(m_edited_font->glyph_spacing());

    m_mean_line_spinbox->set_range(0, max(m_edited_font->glyph_height() - 2, 0), false);
    m_baseline_spinbox->set_range(0, max(m_edited_font->glyph_height() - 2, 0), false);
    m_mean_line_spinbox->set_value(m_edited_font->mean_line());
    m_baseline_spinbox->set_value(m_edited_font->baseline());

    m_font_weight_list.clear();
    for (auto& it : GUI::font_weight_names)
        m_font_weight_list.append(it.name);
    m_weight_combobox->set_model(*GUI::ItemListModel<String>::create(m_font_weight_list));

    int i = 0;
    for (auto it : GUI::font_weight_names) {
        if (it.weight == m_edited_font->weight()) {
            m_weight_combobox->set_selected_index(i);
            break;
        }
        i++;
    }

    m_font_type_list.clear();
    StringBuilder type_count;
    for (int i = 0; i < Gfx::FontTypes::__Count; i++) {
        type_count.appendff("{}", Gfx::BitmapFont::type_name_by_type(static_cast<Gfx::FontTypes>(i)));
        m_font_type_list.append(type_count.to_string());
        type_count.clear();
    }
    m_type_combobox->set_model(*GUI::ItemListModel<String>::create(m_font_type_list));
    m_type_combobox->set_selected_index(m_edited_font->type());

    m_fixed_width_checkbox->set_checked(m_edited_font->is_fixed_width());

    m_glyph_map_widget->set_selected_glyph('A');
    deferred_invoke([this](auto&) {
        m_glyph_map_widget->set_focus(true);
        m_glyph_map_widget->scroll_to_glyph(m_glyph_map_widget->selected_glyph());
        if (m_font_newly_opened)
            m_font_modified = false;
        update_title();
        m_font_newly_opened = false;
    });

    m_undo_stack = make<GUI::UndoStack>();
    m_undo_glyph = adopt_ref(*new UndoGlyph(m_glyph_map_widget->selected_glyph(), *m_edited_font));
    did_change_undo_stack();

    if (on_initialize)
        on_initialize();
}

void FontEditorWidget::initialize_menubar(GUI::Menubar& menubar)
{
    auto& file_menu = menubar.add_menu("&File");
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

    auto& edit_menu = menubar.add_menu("&Edit");
    edit_menu.add_action(*m_undo_action);
    edit_menu.add_action(*m_redo_action);
    edit_menu.add_separator();
    edit_menu.add_action(*m_cut_action);
    edit_menu.add_action(*m_copy_action);
    edit_menu.add_action(*m_paste_action);
    edit_menu.add_action(*m_delete_action);

    auto& view_menu = menubar.add_menu("&View");
    view_menu.add_action(*m_open_preview_action);
    view_menu.add_separator();
    view_menu.add_action(*m_show_metadata_action);
    view_menu.add_separator();
    auto& scale_menu = view_menu.add_submenu("&Scale");
    scale_menu.add_action(*m_scale_five_action);
    scale_menu.add_action(*m_scale_ten_action);
    scale_menu.add_action(*m_scale_fifteen_action);

    auto& help_menu = menubar.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/FontEditor.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Font Editor", GUI::Icon::default_icon("app-font-editor"), window()));
}

bool FontEditorWidget::save_as(const String& path)
{
    auto ret_val = m_edited_font->write_to_file(path);
    if (!ret_val) {
        GUI::MessageBox::show(window(), "The font file could not be saved.", "Save failed", GUI::MessageBox::Type::Error);
        return false;
    }
    m_path = path;
    m_font_modified = false;
    update_title();
    return true;
}

void FontEditorWidget::set_show_font_metadata(bool show)
{
    if (m_font_metadata == show)
        return;
    m_font_metadata = show;
    m_font_metadata_groupbox->set_visible(m_font_metadata);
}

void FontEditorWidget::undo()
{
    if (!m_undo_stack->can_undo())
        return;
    m_undo_stack->undo();
    did_change_undo_stack();

    m_glyph_editor_widget->update();
    m_glyph_map_widget->update();
    if (m_font_preview_window)
        m_font_preview_window->update();
}

void FontEditorWidget::redo()
{
    if (!m_undo_stack->can_redo())
        return;
    m_undo_stack->redo();
    did_change_undo_stack();

    m_glyph_editor_widget->update();
    m_glyph_map_widget->update();
    if (m_font_preview_window)
        m_font_preview_window->update();
}

void FontEditorWidget::did_change_undo_stack()
{
    m_undo_action->set_enabled(m_undo_stack->can_undo());
    m_redo_action->set_enabled(m_undo_stack->can_redo());
    did_modify_font();
}

bool FontEditorWidget::request_close()
{
    if (!m_font_modified)
        return true;
    auto result = GUI::MessageBox::show(window(), "Save changes to the current font?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
    if (result == GUI::MessageBox::ExecYes) {
        m_save_action->activate();
        if (!m_font_modified)
            return true;
    }
    if (result == GUI::MessageBox::ExecNo)
        return true;
    return false;
}

void FontEditorWidget::update_title()
{
    StringBuilder title;
    title.append(m_path);
    if (m_font_modified && !m_font_newly_opened)
        title.append(" (*)");
    title.append(" - Font Editor");
    if (window())
        window()->set_title(title.to_string());
}

void FontEditorWidget::did_modify_font()
{
    if (m_font_modified)
        return;
    m_font_modified = true;
    update_title();
}
