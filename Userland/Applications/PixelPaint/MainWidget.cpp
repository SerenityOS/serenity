/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "EditGuideDialog.h"
#include "FilterGallery.h"
#include "FilterParams.h"
#include "LevelsDialog.h"
#include "ResizeImageDialog.h"
#include <Applications/PixelPaint/PixelPaintWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/Debounce.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

IconBag g_icon_bag;

MainWidget::MainWidget()
    : Widget()
{
    load_from_gml(pixel_paint_window_gml);

    m_toolbox = find_descendant_of_type_named<PixelPaint::ToolboxWidget>("toolbox");
    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    m_palette_widget = *find_descendant_of_type_named<PixelPaint::PaletteWidget>("palette_widget");

    m_histogram_widget = *find_descendant_of_type_named<PixelPaint::HistogramWidget>("histogram_widget");
    m_vectorscope_widget = *find_descendant_of_type_named<PixelPaint::VectorscopeWidget>("vectorscope_widget");
    m_layer_list_widget = *find_descendant_of_type_named<PixelPaint::LayerListWidget>("layer_list_widget");
    m_layer_list_widget->on_layer_select = [&](auto* layer) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->set_active_layer(layer);
    };

    m_layer_properties_widget = *find_descendant_of_type_named<PixelPaint::LayerPropertiesWidget>("layer_properties_widget");
    m_tool_properties_widget = *find_descendant_of_type_named<PixelPaint::ToolPropertiesWidget>("tool_properties_widget");

    m_toolbox->on_tool_selection = [&](auto* tool) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->set_active_tool(tool);
        m_tool_properties_widget->set_active_tool(tool);
    };

    m_tab_widget->on_middle_click = [&](auto& widget) {
        m_tab_widget->on_tab_close_click(widget);
    };

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        if (image_editor.request_close()) {
            m_tab_widget->deferred_invoke([&] {
                m_tab_widget->remove_tab(image_editor);
                if (m_tab_widget->children().size() == 0) {
                    m_histogram_widget->set_image(nullptr);
                    m_vectorscope_widget->set_image(nullptr);
                    m_layer_list_widget->set_image(nullptr);
                    m_layer_properties_widget->set_layer(nullptr);
                    m_palette_widget->set_image_editor(nullptr);
                    m_tool_properties_widget->set_enabled(false);
                    set_actions_enabled(false);
                }
            });
        }
    };

    m_tab_widget->on_change = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        m_palette_widget->set_image_editor(&image_editor);
        m_histogram_widget->set_image(&image_editor.image());
        m_vectorscope_widget->set_image(&image_editor.image());
        m_layer_list_widget->set_image(&image_editor.image());
        m_layer_properties_widget->set_layer(image_editor.active_layer());
        window()->set_modified(image_editor.is_modified());
        image_editor.on_modified_change = [this](bool modified) {
            window()->set_modified(modified);
            m_histogram_widget->image_changed();
            m_vectorscope_widget->image_changed();
        };
        if (auto* active_tool = m_toolbox->active_tool())
            image_editor.set_active_tool(active_tool);
        m_show_guides_action->set_checked(image_editor.guide_visibility());
        m_show_rulers_action->set_checked(image_editor.ruler_visibility());
        image_editor.on_scale_change(image_editor.scale());
        image_editor.undo_stack().on_state_change = [this] {
            image_editor_did_update_undo_stack();
        };
        // Ensure that our undo/redo actions are in sync with the current editor.
        image_editor_did_update_undo_stack();
    };
}

void MainWidget::image_editor_did_update_undo_stack()
{
    auto* image_editor = current_image_editor();
    if (!image_editor) {
        m_undo_action->set_enabled(false);
        m_redo_action->set_enabled(false);
        return;
    }

    auto make_action_text = [](auto prefix, auto suffix) {
        StringBuilder builder;
        builder.append(prefix);
        if (suffix.has_value()) {
            builder.append(' ');
            builder.append(suffix.value());
        }
        return builder.to_deprecated_string();
    };

    auto& undo_stack = image_editor->undo_stack();
    m_undo_action->set_enabled(undo_stack.can_undo());
    m_redo_action->set_enabled(undo_stack.can_redo());

    m_undo_action->set_text(make_action_text("&Undo"sv, undo_stack.undo_action_text()));
    m_redo_action->set_text(make_action_text("&Redo"sv, undo_stack.redo_action_text()));
}

// Note: Update these together! v
static Vector<DeprecatedString> const s_suggested_zoom_levels { "25%", "50%", "100%", "200%", "300%", "400%", "800%", "1600%", "Fit to width", "Fit to height", "Fit entire image" };
static constexpr int s_zoom_level_fit_width = 8;
static constexpr int s_zoom_level_fit_height = 9;
static constexpr int s_zoom_level_fit_image = 10;
// Note: Update these together! ^

void MainWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");

    m_new_image_action = GUI::Action::create(
        "&New Image...", { Mod_Ctrl, Key_N }, g_icon_bag.filetype_pixelpaint, [&](auto&) {
            auto dialog = PixelPaint::CreateNewImageDialog::construct(&window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                auto image = PixelPaint::Image::try_create_with_size(dialog->image_size()).release_value_but_fixme_should_propagate_errors();
                auto bg_layer = PixelPaint::Layer::try_create_with_size(*image, image->size(), "Background").release_value_but_fixme_should_propagate_errors();
                image->add_layer(*bg_layer);
                bg_layer->content_bitmap().fill(Color::White);

                auto& editor = create_new_editor(*image);
                auto image_title = dialog->image_name().trim_whitespace();
                editor.set_title(image_title.is_empty() ? "Untitled" : image_title);
                editor.undo_stack().set_current_unmodified();

                m_histogram_widget->set_image(image);
                m_vectorscope_widget->set_image(image);
                m_layer_list_widget->set_image(image);
                m_layer_list_widget->set_selected_layer(bg_layer);
            }
        });

    m_new_image_from_clipboard_action = GUI::Action::create(
        "&New Image from Clipboard", { Mod_Ctrl | Mod_Shift, Key_V }, g_icon_bag.new_clipboard, [&](auto&) {
            create_image_from_clipboard();
        });

    m_open_image_action = GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(&window);
        if (response.is_error())
            return;
        open_image(response.value());
    });

    m_save_image_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->save_project_as();
    });

    m_save_image_action = GUI::CommonActions::make_save_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->save_project();
    });

    file_menu.add_action(*m_new_image_action);
    file_menu.add_action(*m_new_image_from_clipboard_action);
    file_menu.add_action(*m_open_image_action);
    file_menu.add_action(*m_save_image_action);
    file_menu.add_action(*m_save_image_as_action);

    m_export_submenu = file_menu.add_submenu("&Export");

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &BMP", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                auto response = FileSystemAccessClient::Client::the().try_save_file(&window, editor->title(), "bmp");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?"sv, "Preserve transparency?"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_bmp_to_file(response.value(), preserve_alpha_channel == GUI::MessageBox::ExecResult::Yes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Export to BMP failed: {}", result.error()));
            }));

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &PNG", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                // TODO: fix bmp on line below?
                auto response = FileSystemAccessClient::Client::the().try_save_file(&window, editor->title(), "png");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?"sv, "Preserve transparency?"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_png_to_file(response.value(), preserve_alpha_channel == GUI::MessageBox::ExecResult::Yes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Export to PNG failed: {}", result.error()));
            }));

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &QOI", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                auto response = FileSystemAccessClient::Client::the().try_save_file(&window, editor->title(), "qoi");
                if (response.is_error())
                    return;
                auto result = editor->image().export_qoi_to_file(response.value());
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Export to QOI failed: {}", result.error()));
            }));

    m_export_submenu->set_icon(g_icon_bag.file_export);

    file_menu.add_separator();

    m_close_image_action = GUI::Action::create("&Close Image", { Mod_Ctrl, Key_W }, g_icon_bag.close_image, [&](auto&) {
        auto* active_widget = m_tab_widget->active_widget();
        VERIFY(active_widget);
        m_tab_widget->on_tab_close_click(*active_widget);
    });

    file_menu.add_action(*m_close_image_action);

    file_menu.add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (request_close())
            GUI::Application::the()->quit();
    }));

    m_edit_menu = window.add_menu("&Edit");

    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (!editor->active_layer()) {
            dbgln("Cannot cut with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->try_copy_bitmap(editor->image().selection());
        if (!bitmap) {
            dbgln("try_copy_bitmap() from Layer failed");
            return;
        }
        GUI::Clipboard::the().set_bitmap(*bitmap);
        editor->active_layer()->erase_selection(editor->image().selection());
    });

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (!editor->active_layer()) {
            dbgln("Cannot copy with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->try_copy_bitmap(editor->image().selection());
        if (!bitmap) {
            dbgln("try_copy_bitmap() from Layer failed");
            return;
        }
        GUI::Clipboard::the().set_bitmap(*bitmap);
    });

    m_copy_merged_action = GUI::Action::create(
        "Copy &Merged", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.edit_copy, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);

            auto bitmap = editor->image().try_copy_bitmap(editor->image().selection());
            if (!bitmap) {
                dbgln("try_copy_bitmap() from Image failed");
                return;
            }
            GUI::Clipboard::the().set_bitmap(*bitmap);
        });

    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor) {
            create_image_from_clipboard();
            return;
        }

        auto bitmap = GUI::Clipboard::the().fetch_data_and_type().as_bitmap();
        if (!bitmap)
            return;

        auto layer = PixelPaint::Layer::try_create_with_bitmap(editor->image(), *bitmap, "Pasted layer").release_value_but_fixme_should_propagate_errors();
        editor->image().add_layer(*layer);
        editor->set_active_layer(layer);
        editor->image().selection().clear();
    });
    GUI::Clipboard::the().on_change = [&](auto& mime_type) {
        m_paste_action->set_enabled(mime_type == "image/x-serenityos");
    };
    m_paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "image/x-serenityos");

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        if (auto* editor = current_image_editor())
            editor->undo();
    });

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->redo();
    });

    m_edit_menu->add_action(*m_undo_action);
    m_edit_menu->add_action(*m_redo_action);
    m_edit_menu->add_separator();
    m_edit_menu->add_action(*m_cut_action);
    m_edit_menu->add_action(*m_copy_action);
    m_edit_menu->add_action(*m_copy_merged_action);
    m_edit_menu->add_action(*m_paste_action);
    m_edit_menu->add_separator();

    m_edit_menu->add_action(GUI::CommonActions::make_select_all_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (!editor->active_layer())
            return;
        editor->image().selection().merge(editor->active_layer()->relative_rect(), PixelPaint::Selection::MergeMode::Set);
        editor->did_complete_action("Select All"sv);
    }));
    m_edit_menu->add_action(GUI::Action::create(
        "Clear &Selection", g_icon_bag.clear_selection, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().selection().clear();
            editor->did_complete_action("Clear Selection"sv);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Invert Selection", g_icon_bag.invert_selection, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().selection().invert();
            editor->did_complete_action("Invert Selection"sv);
        }));

    m_edit_menu->add_separator();
    m_edit_menu->add_action(GUI::Action::create(
        "S&wap Colors", { Mod_None, Key_X }, g_icon_bag.swap_colors, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto old_primary_color = editor->primary_color();
            editor->set_primary_color(editor->secondary_color());
            editor->set_secondary_color(old_primary_color);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Default Colors", { Mod_None, Key_D }, g_icon_bag.default_colors, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_primary_color(Color::Black);
            editor->set_secondary_color(Color::White);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Load Color Palette", g_icon_bag.load_color_palette, [&](auto&) {
            auto response = FileSystemAccessClient::Client::the().try_open_file(&window, "Load Color Palette");
            if (response.is_error())
                return;

            auto result = PixelPaint::PaletteWidget::load_palette_file(*response.value());
            if (result.is_error()) {
                GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Loading color palette failed: {}", result.error()));
                return;
            }

            m_palette_widget->display_color_list(result.value());
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "Sa&ve Color Palette", g_icon_bag.save_color_palette, [&](auto&) {
            auto response = FileSystemAccessClient::Client::the().try_save_file(&window, "untitled", "palette");
            if (response.is_error())
                return;

            auto result = PixelPaint::PaletteWidget::save_palette_file(m_palette_widget->colors(), *response.value());
            if (result.is_error())
                GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Writing color palette failed: {}", result.error()));
        }));

    m_view_menu = window.add_menu("&View");

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->scale_by(0.1f);
        });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->scale_by(-0.1f);
        });

    m_reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->reset_view();
        });

    m_add_guide_action = GUI::Action::create(
        "&Add Guide", g_icon_bag.add_guide, [&](auto&) {
            auto dialog = PixelPaint::EditGuideDialog::construct(&window);
            if (dialog->exec() != GUI::Dialog::ExecResult::OK)
                return;
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto offset = dialog->offset_as_pixel(*editor);
            if (!offset.has_value())
                return;
            editor->add_guide(PixelPaint::Guide::construct(dialog->orientation(), offset.value()));
        });

    // Save this so other methods can use it
    m_show_guides_action = GUI::Action::create_checkable(
        "Show &Guides", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "Guides"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_guide_visibility(action.is_checked());
        });
    m_show_guides_action->set_checked(Config::read_bool("PixelPaint"sv, "Guides"sv, "Show"sv, true));

    m_view_menu->add_action(*m_zoom_in_action);
    m_view_menu->add_action(*m_zoom_out_action);
    m_view_menu->add_action(*m_reset_zoom_action);
    m_view_menu->add_action(GUI::Action::create(
        "Fit Image To &View", g_icon_bag.fit_image_to_view, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->fit_image_to_view();
        }));
    m_view_menu->add_separator();
    m_view_menu->add_action(*m_add_guide_action);
    m_view_menu->add_action(*m_show_guides_action);

    m_view_menu->add_action(GUI::Action::create(
        "&Clear Guides", g_icon_bag.clear_guides, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->clear_guides();
        }));
    m_view_menu->add_separator();

    auto show_pixel_grid_action = GUI::Action::create_checkable(
        "Show &Pixel Grid", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "PixelGrid"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_pixel_grid_visibility(action.is_checked());
        });
    show_pixel_grid_action->set_checked(Config::read_bool("PixelPaint"sv, "PixelGrid"sv, "Show"sv, true));
    m_view_menu->add_action(*show_pixel_grid_action);

    m_show_rulers_action = GUI::Action::create_checkable(
        "Show R&ulers", { Mod_Ctrl, Key_R }, [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "Rulers"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_ruler_visibility(action.is_checked());
        });
    m_show_rulers_action->set_checked(Config::read_bool("PixelPaint"sv, "Rulers"sv, "Show"sv, true));
    m_view_menu->add_action(*m_show_rulers_action);

    m_show_active_layer_boundary_action = GUI::Action::create_checkable(
        "Show Active Layer &Boundary", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "ImageEditor"sv, "ShowActiveLayerBoundary"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_show_active_layer_boundary(action.is_checked());
        });
    m_show_active_layer_boundary_action->set_checked(Config::read_bool("PixelPaint"sv, "ImageEditor"sv, "ShowActiveLayerBoundary"sv, true));
    m_view_menu->add_action(*m_show_active_layer_boundary_action);

    m_view_menu->add_separator();

    auto histogram_action = GUI::Action::create_checkable("&Histogram", [&](auto& action) {
        Config::write_bool("PixelPaint"sv, "Scopes"sv, "ShowHistogram"sv, action.is_checked());
        m_histogram_widget->parent_widget()->set_visible(action.is_checked());
    });
    histogram_action->set_checked(Config::read_bool("PixelPaint"sv, "Scopes"sv, "ShowHistogram"sv, false));
    m_histogram_widget->parent_widget()->set_visible(histogram_action->is_checked());

    auto vectorscope_action = GUI::Action::create_checkable("&Vectorscope", [&](auto& action) {
        Config::write_bool("PixelPaint"sv, "Scopes"sv, "ShowVectorscope"sv, action.is_checked());
        m_vectorscope_widget->parent_widget()->set_visible(action.is_checked());
    });
    vectorscope_action->set_checked(Config::read_bool("PixelPaint"sv, "Scopes"sv, "ShowVectorscope"sv, false));
    m_vectorscope_widget->parent_widget()->set_visible(vectorscope_action->is_checked());

    auto& scopes_menu = m_view_menu->add_submenu("&Scopes");
    scopes_menu.add_action(histogram_action);
    scopes_menu.add_action(vectorscope_action);

    m_tool_menu = window.add_menu("&Tool");
    m_toolbox->for_each_tool([&](auto& tool) {
        if (tool.action())
            m_tool_menu->add_action(*tool.action());
        return IterationDecision::Continue;
    });

    m_image_menu = window.add_menu("&Image");
    m_image_menu->add_action(GUI::Action::create(
        "Flip Image &Vertically", g_icon_bag.edit_flip_vertical, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flip(Gfx::Orientation::Vertical);
            editor->did_complete_action("Flip Image Vertically"sv);
        }));
    m_image_menu->add_action(GUI::Action::create(
        "Flip Image &Horizontally", g_icon_bag.edit_flip_horizontal, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flip(Gfx::Orientation::Horizontal);
            editor->did_complete_action("Flip Image Horizontally"sv);
        }));
    m_image_menu->add_separator();

    m_image_menu->add_action(GUI::Action::create("Rotate Image &Counterclockwise", { Mod_Ctrl | Mod_Shift, Key_LessThan }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-ccw.png"sv).release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().rotate(Gfx::RotationDirection::CounterClockwise);
            editor->did_complete_action("Rotate Image Counterclockwise"sv);
        }));

    m_image_menu->add_action(GUI::Action::create("Rotate Image Clock&wise", { Mod_Ctrl | Mod_Shift, Key_GreaterThan }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-cw.png"sv).release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().rotate(Gfx::RotationDirection::Clockwise);
            editor->did_complete_action("Rotate Image Clockwise"sv);
        }));
    m_image_menu->add_separator();
    m_image_menu->add_action(GUI::Action::create(
        "&Resize Image...", { Mod_Ctrl | Mod_Shift, Key_R }, g_icon_bag.resize_image, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::ResizeImageDialog::construct(editor->image().size(), &window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                editor->image().resize(dialog->desired_size(), dialog->scaling_mode());
                editor->did_complete_action("Resize Image"sv);
            }
        }));
    m_image_menu->add_action(GUI::Action::create(
        "&Crop Image to Selection", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            // FIXME: disable this action if there is no selection
            if (editor->image().selection().is_empty())
                return;
            auto crop_rect = editor->image().rect().intersected(editor->image().selection().bounding_rect());
            editor->image().crop(crop_rect);
            editor->image().selection().clear();
            editor->did_complete_action("Crop Image to Selection"sv);
        }));

    m_image_menu->add_action(GUI::Action::create(
        "&Crop Image to Content", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);

            auto content_bounding_rect = editor->image().nonempty_content_bounding_rect();
            if (!content_bounding_rect.has_value())
                return;

            editor->image().crop(content_bounding_rect.value());
            editor->did_complete_action("Crop Image to Content"sv);
        }));

    m_layer_menu = window.add_menu("&Layer");

    m_layer_menu->on_visibility_change = [this](bool visible) {
        if (!visible)
            return;

        bool image_has_selection = !current_image_editor()->active_layer()->image().selection().is_empty();

        m_layer_via_copy->set_enabled(image_has_selection);
        m_layer_via_cut->set_enabled(image_has_selection);
    };

    m_layer_menu->add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, g_icon_bag.new_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::CreateNewLayerDialog::construct(editor->image().size(), &window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                auto layer_or_error = PixelPaint::Layer::try_create_with_size(editor->image(), dialog->layer_size(), dialog->layer_name());
                if (layer_or_error.is_error()) {
                    GUI::MessageBox::show_error(&window, DeprecatedString::formatted("Unable to create layer with size {}", dialog->size()));
                    return;
                }
                editor->image().add_layer(layer_or_error.release_value());
                editor->layers_did_change();
                editor->did_complete_action("New Layer"sv);
                m_layer_list_widget->select_top_layer();
            }
        }));

    m_layer_via_copy = GUI::Action::create(
        "Layer via Copy", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.new_layer, [&](auto&) {
            auto add_layer_success = current_image_editor()->add_new_layer_from_selection();
            if (add_layer_success.is_error()) {
                GUI::MessageBox::show_error(&window, add_layer_success.release_error().string_literal());
                return;
            }
            current_image_editor()->did_complete_action("New Layer via Copy"sv);
            m_layer_list_widget->select_top_layer();
        });
    m_layer_menu->add_action(*m_layer_via_copy);

    m_layer_via_cut = GUI::Action::create(
        "Layer via Cut", { Mod_Ctrl | Mod_Shift, Key_X }, g_icon_bag.new_layer, [&](auto&) {
            auto add_layer_success = current_image_editor()->add_new_layer_from_selection();
            if (add_layer_success.is_error()) {
                GUI::MessageBox::show_error(&window, add_layer_success.release_error().string_literal());
                return;
            }
            current_image_editor()->active_layer()->erase_selection(current_image_editor()->image().selection());
            current_image_editor()->did_complete_action("New Layer via Cut"sv);
            m_layer_list_widget->select_top_layer();
        });
    m_layer_menu->add_action(*m_layer_via_cut);

    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Add M&ask", { Mod_Ctrl | Mod_Shift, Key_M }, g_icon_bag.add_mask, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            active_layer->create_mask();
            editor->update();
            m_layer_list_widget->repaint();
        }));

    m_layer_menu->add_separator();

    m_layer_menu->add_action(GUI::Action::create(
        "Select &Previous Layer", { 0, Key_PageUp }, g_icon_bag.previous_layer, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(1);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select &Next Layer", { 0, Key_PageDown }, g_icon_bag.next_layer, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(-1);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select &Top Layer", { 0, Key_Home }, g_icon_bag.top_layer, [&](auto&) {
            m_layer_list_widget->select_top_layer();
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select B&ottom Layer", { 0, Key_End }, g_icon_bag.bottom_layer, [&](auto&) {
            m_layer_list_widget->select_bottom_layer();
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::CommonActions::make_move_to_front_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_front(*active_layer);
            editor->layers_did_change();
        }));
    m_layer_menu->add_action(GUI::CommonActions::make_move_to_back_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_back(*active_layer);
            editor->layers_did_change();
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Move Active Layer &Up", { Mod_Ctrl, Key_PageUp }, g_icon_bag.active_layer_up, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_up(*active_layer);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Move Active Layer &Down", { Mod_Ctrl, Key_PageDown }, g_icon_bag.active_layer_down, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_down(*active_layer);
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "&Remove Active Layer", { Mod_Ctrl, Key_D }, g_icon_bag.delete_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;

            auto active_layer_index = editor->image().index_of(*active_layer);
            editor->image().remove_layer(*active_layer);

            if (editor->image().layer_count()) {
                auto& next_active_layer = editor->image().layer(active_layer_index > 0 ? active_layer_index - 1 : 0);
                editor->set_active_layer(&next_active_layer);
            } else {
                auto layer = PixelPaint::Layer::try_create_with_size(editor->image(), editor->image().size(), "Background").release_value_but_fixme_should_propagate_errors();
                editor->image().add_layer(move(layer));
                editor->layers_did_change();
                m_layer_list_widget->select_top_layer();
            }
        }));

    m_layer_list_widget->on_context_menu_request = [&](auto& event) {
        m_layer_menu->popup(event.screen_position());
    };
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Fl&atten Image", { Mod_Ctrl, Key_F }, g_icon_bag.flatten_image, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flatten_all_layers();
            editor->did_complete_action("Flatten Image"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "&Merge Visible", { Mod_Ctrl, Key_M }, g_icon_bag.merge_visible, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().merge_visible_layers();
            editor->did_complete_action("Merge Visible"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "Merge &Active Layer Up", g_icon_bag.merge_active_layer_up, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_up(*active_layer);
            editor->did_complete_action("Merge Active Layer Up"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "M&erge Active Layer Down", { Mod_Ctrl, Key_E }, g_icon_bag.merge_active_layer_down, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_down(*active_layer);
            editor->did_complete_action("Merge Active Layer Down"sv);
        }));

    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Flip Layer &Vertically", g_icon_bag.edit_flip_vertical, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            active_layer->flip(Gfx::Orientation::Vertical);
            editor->did_complete_action("Flip Layer Vertically"sv);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Flip Layer &Horizontally", g_icon_bag.edit_flip_horizontal, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            active_layer->flip(Gfx::Orientation::Horizontal);
            editor->did_complete_action("Flip Layer Horizontally"sv);
        }));
    m_layer_menu->add_separator();

    m_layer_menu->add_action(GUI::Action::create("Rotate Layer &Counterclockwise", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-ccw.png"sv).release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            active_layer->rotate(Gfx::RotationDirection::CounterClockwise);
            editor->did_complete_action("Rotate Layer Counterclockwise"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create("Rotate Layer Clock&wise", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-cw.png"sv).release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            active_layer->rotate(Gfx::RotationDirection::Clockwise);
            editor->did_complete_action("Rotate Layer Clockwise"sv);
        }));

    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "&Crop Layer to Selection", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            // FIXME: disable this action if there is no selection
            auto active_layer = editor->active_layer();
            if (!active_layer || editor->image().selection().is_empty())
                return;
            auto intersection = editor->image().rect().intersected(editor->image().selection().bounding_rect());
            auto crop_rect = intersection.translated(-active_layer->location());
            active_layer->crop(crop_rect);
            active_layer->set_location(intersection.location());
            editor->image().selection().clear();
            editor->did_complete_action("Crop Layer to Selection"sv);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "&Crop Layer to Content", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto content_bounding_rect = active_layer->nonempty_content_bounding_rect();
            if (!content_bounding_rect.has_value())
                return;
            active_layer->crop(content_bounding_rect.value());
            active_layer->set_location(content_bounding_rect->location());
            editor->did_complete_action("Crop Layer to Content"sv);
        }));

    m_filter_menu = window.add_menu("&Filter");

    m_filter_menu->add_action(GUI::Action::create("Filter &Gallery", g_icon_bag.filter, [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        auto dialog = PixelPaint::FilterGallery::construct(&window, editor);
        if (dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;
    }));

    m_filter_menu->add_separator();
    m_filter_menu->add_action(GUI::Action::create("Generic 5x5 &Convolution", g_icon_bag.generic_5x5_convolution, [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (auto* layer = editor->active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(&window)) {
                filter.apply(layer->content_bitmap(), layer->rect(), layer->content_bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action("Generic 5x5 Convolution"sv);
            }
        }
    }));

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu.add_action(GUI::CommonActions::make_about_action("Pixel Paint", GUI::Icon::default_icon("app-pixel-paint"sv), &window));

    m_levels_dialog_action = GUI::Action::create(
        "Change &Levels...", { Mod_Ctrl, Key_L }, g_icon_bag.levels, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::LevelsDialog::construct(&window, editor);
            if (dialog->exec() != GUI::Dialog::ExecResult::OK)
                dialog->revert_possible_changes();
        });

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(*m_new_image_action);
    toolbar.add_action(*m_open_image_action);
    toolbar.add_action(*m_save_image_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_action(*m_undo_action);
    toolbar.add_action(*m_redo_action);
    toolbar.add_separator();
    toolbar.add_action(*m_zoom_in_action);
    toolbar.add_action(*m_zoom_out_action);
    toolbar.add_action(*m_reset_zoom_action);

    m_zoom_combobox = toolbar.add<GUI::ComboBox>();
    m_zoom_combobox->set_max_width(75);
    m_zoom_combobox->set_model(*GUI::ItemListModel<DeprecatedString>::create(s_suggested_zoom_levels));
    m_zoom_combobox->on_change = [this](DeprecatedString const& value, GUI::ModelIndex const& index) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (index.is_valid()) {
            switch (index.row()) {
            case s_zoom_level_fit_width:
                editor->fit_image_to_view(ImageEditor::FitType::Width);
                return;
            case s_zoom_level_fit_height:
                editor->fit_image_to_view(ImageEditor::FitType::Height);
                return;
            case s_zoom_level_fit_image:
                editor->fit_image_to_view(ImageEditor::FitType::Both);
                return;
            }
        }

        auto zoom_level_optional = value.view().trim("%"sv, TrimMode::Right).to_int();
        if (!zoom_level_optional.has_value()) {
            // Indicate that a parse-error occurred by resetting the text to the current state.
            editor->on_scale_change(editor->scale());
            return;
        }

        editor->set_scale(zoom_level_optional.value() * 1.0f / 100);
        // If the selected zoom level got clamped, or a "fit to …" level was selected,
        // there is a chance that the new scale is identical to the old scale.
        // In these cases, we need to manually reset the text:
        editor->on_scale_change(editor->scale());
    };
    m_zoom_combobox->on_return_pressed = [this]() {
        m_zoom_combobox->on_change(m_zoom_combobox->text(), GUI::ModelIndex());
    };

    toolbar.add_separator();
    toolbar.add_action(*m_levels_dialog_action);
}

void MainWidget::set_actions_enabled(bool enabled)
{
    m_save_image_action->set_enabled(enabled);
    m_save_image_as_action->set_enabled(enabled);
    m_close_image_action->set_enabled(enabled);

    m_export_submenu->set_children_actions_enabled(enabled);

    m_edit_menu->set_children_actions_enabled(enabled);
    m_paste_action->set_enabled(true);

    m_view_menu->set_children_actions_enabled(enabled);
    m_layer_menu->set_children_actions_enabled(enabled);
    m_image_menu->set_children_actions_enabled(enabled);
    m_tool_menu->set_children_actions_enabled(enabled);
    m_filter_menu->set_children_actions_enabled(enabled);

    m_zoom_combobox->set_enabled(enabled);
}

void MainWidget::open_image(Core::File& file)
{
    auto try_load = m_loader.try_load_from_file(file);

    if (try_load.is_error()) {
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Unable to open file: {}, {}", file.filename(), try_load.error()));
        return;
    }

    auto& image = *m_loader.release_image();
    auto& editor = create_new_editor(image);
    editor.set_loaded_from_image(m_loader.is_raw_image());
    editor.set_path(file.filename());
    editor.undo_stack().set_current_unmodified();
    m_layer_list_widget->set_image(&image);
}

void MainWidget::create_default_image()
{
    auto image = Image::try_create_with_size({ 510, 356 }).release_value_but_fixme_should_propagate_errors();

    auto bg_layer = Layer::try_create_with_size(*image, image->size(), "Background").release_value_but_fixme_should_propagate_errors();
    image->add_layer(*bg_layer);
    bg_layer->content_bitmap().fill(Color::Transparent);

    m_layer_list_widget->set_image(image);

    auto& editor = create_new_editor(*image);
    editor.set_title("Untitled");
    editor.set_active_layer(bg_layer);
    editor.undo_stack().set_current_unmodified();
}

void MainWidget::create_image_from_clipboard()
{
    auto bitmap = GUI::Clipboard::the().fetch_data_and_type().as_bitmap();
    if (!bitmap) {
        GUI::MessageBox::show(window(), "There is no image in a clipboard to paste."sv, "PixelPaint"sv, GUI::MessageBox::Type::Warning);
        return;
    }

    auto image = PixelPaint::Image::try_create_with_size(bitmap->size()).release_value_but_fixme_should_propagate_errors();
    auto layer = PixelPaint::Layer::try_create_with_bitmap(image, *bitmap, "Pasted layer").release_value_but_fixme_should_propagate_errors();
    image->add_layer(*layer);

    auto& editor = create_new_editor(*image);
    editor.set_title("Untitled");

    m_layer_list_widget->set_image(image);
    m_layer_list_widget->set_selected_layer(layer);
}

bool MainWidget::request_close()
{
    while (!m_tab_widget->children().is_empty()) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (!editor->request_close())
            return false;
        m_tab_widget->remove_tab(*m_tab_widget->active_widget());
    }
    return true;
}

ImageEditor* MainWidget::current_image_editor()
{
    if (!m_tab_widget->active_widget())
        return nullptr;
    return verify_cast<PixelPaint::ImageEditor>(m_tab_widget->active_widget());
}

ImageEditor& MainWidget::create_new_editor(NonnullRefPtr<Image> image)
{
    auto& image_editor = m_tab_widget->add_tab<PixelPaint::ImageEditor>("Untitled", image);

    image_editor.on_active_layer_change = [&](auto* layer) {
        if (current_image_editor() != &image_editor)
            return;
        m_layer_list_widget->set_selected_layer(layer);
        m_layer_properties_widget->set_layer(layer);
    };

    image_editor.on_title_change = [&](auto const& title) {
        m_tab_widget->set_tab_title(image_editor, title);
    };

    image_editor.on_image_mouse_position_change = [&](auto const& mouse_position) {
        auto const& image_size = current_image_editor()->image().size();
        auto image_rectangle = Gfx::IntRect { 0, 0, image_size.width(), image_size.height() };
        if (image_rectangle.contains(mouse_position)) {
            m_statusbar->set_override_text(mouse_position.to_deprecated_string());
            m_histogram_widget->set_color_at_mouseposition(current_image_editor()->image().color_at(mouse_position));
            m_vectorscope_widget->set_color_at_mouseposition(current_image_editor()->image().color_at(mouse_position));
        } else {
            m_statusbar->set_override_text({});
            m_histogram_widget->set_color_at_mouseposition(Color::Transparent);
            m_vectorscope_widget->set_color_at_mouseposition(Color::Transparent);
        }
    };

    image_editor.on_leave = [&]() {
        m_statusbar->set_override_text({});
        m_histogram_widget->set_color_at_mouseposition(Color::Transparent);
        m_vectorscope_widget->set_color_at_mouseposition(Color::Transparent);
    };

    image_editor.on_set_guide_visibility = [&](bool show_guides) {
        m_show_guides_action->set_checked(show_guides);
    };

    image_editor.on_set_ruler_visibility = [&](bool show_rulers) {
        m_show_rulers_action->set_checked(show_rulers);
    };

    image_editor.on_scale_change = Core::debounce([this](float scale) {
        m_zoom_combobox->set_text(DeprecatedString::formatted("{}%", roundf(scale * 100)));
        current_image_editor()->update_tool_cursor();
    },
        100);

    if (image->layer_count())
        image_editor.set_active_layer(&image->layer(0));

    if (!m_loader.is_raw_image()) {
        m_loader.json_metadata().for_each([&](JsonValue const& value) {
            if (!value.is_object())
                return;
            auto& json_object = value.as_object();
            auto orientation_value = json_object.get("orientation"sv);
            if (!orientation_value.is_string())
                return;

            auto offset_value = json_object.get("offset"sv);
            if (!offset_value.is_number())
                return;

            auto orientation_string = orientation_value.as_string();
            PixelPaint::Guide::Orientation orientation;
            if (orientation_string == "horizontal"sv)
                orientation = PixelPaint::Guide::Orientation::Horizontal;
            else if (orientation_string == "vertical"sv)
                orientation = PixelPaint::Guide::Orientation::Vertical;
            else
                return;

            image_editor.add_guide(PixelPaint::Guide::construct(orientation, offset_value.to_number<float>()));
        });
    }

    m_tab_widget->set_active_widget(&image_editor);
    image_editor.set_focus(true);
    image_editor.fit_image_to_view();
    m_tool_properties_widget->set_enabled(true);
    set_actions_enabled(true);

    return image_editor;
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    auto const& mime_types = event.mime_types();
    if (mime_types.contains_slow("text/uri-list"))
        event.accept();
}

void MainWidget::drop_event(GUI::DropEvent& event)
{
    if (!event.mime_data().has_urls())
        return;

    event.accept();

    if (event.mime_data().urls().is_empty())
        return;

    for (auto& url : event.mime_data().urls()) {
        if (url.scheme() != "file")
            continue;

        auto response = FileSystemAccessClient::Client::the().try_request_file(window(), url.path(), Core::OpenMode::ReadOnly);
        if (response.is_error())
            return;
        open_image(response.value());
    }
}
}
