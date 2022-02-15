/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "EditGuideDialog.h"
#include "FilterGallery.h"
#include "FilterParams.h"
#include <Applications/PixelPaint/PixelPaintWindowGML.h>
#include <LibConfig/Client.h>
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
    m_tab_widget->set_container_margins({ 4, 5, 5, 4 });
    m_tab_widget->set_reorder_allowed(true);
    m_tab_widget->set_close_button_enabled(true);

    m_palette_widget = *find_descendant_of_type_named<PixelPaint::PaletteWidget>("palette_widget");

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
        m_layer_list_widget->set_image(&image_editor.image());
        m_layer_properties_widget->set_layer(image_editor.active_layer());
        if (auto* active_tool = m_toolbox->active_tool())
            image_editor.set_active_tool(active_tool);
        m_show_guides_action->set_checked(image_editor.guide_visibility());
        m_show_rulers_action->set_checked(image_editor.ruler_visibility());
        image_editor.on_scale_change(image_editor.scale());
    };
}

// Note: Update these together! v
static const Vector<String> s_suggested_zoom_levels { "25%", "50%", "100%", "200%", "300%", "400%", "800%", "1600%", "Fit to width", "Fit to height", "Fit entire image" };
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
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto image = PixelPaint::Image::try_create_with_size(dialog->image_size()).release_value_but_fixme_should_propagate_errors();
                auto bg_layer = PixelPaint::Layer::try_create_with_size(*image, image->size(), "Background").release_value_but_fixme_should_propagate_errors();
                image->add_layer(*bg_layer);
                bg_layer->bitmap().fill(Color::White);

                auto& editor = create_new_editor(*image);
                auto image_title = dialog->image_name().trim_whitespace();
                editor.set_title(image_title.is_empty() ? "Untitled" : image_title);
                editor.undo_stack().set_current_unmodified();

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
                auto response = FileSystemAccessClient::Client::the().try_save_file(&window, "untitled", "bmp");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_bmp_to_file(response.value(), preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, String::formatted("Export to BMP failed: {}", result.error()));
            }));

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &PNG", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                // TODO: fix bmp on line below?
                auto response = FileSystemAccessClient::Client::the().try_save_file(&window, "untitled", "png");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_png_to_file(response.value(), preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, String::formatted("Export to PNG failed: {}", result.error()));
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

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (!editor->active_layer()) {
            dbgln("Cannot copy with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->try_copy_bitmap(editor->selection());
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

            auto bitmap = editor->image().try_copy_bitmap(editor->selection());
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
        editor->selection().clear();
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

    m_edit_menu->add_action(*m_copy_action);
    m_edit_menu->add_action(*m_copy_merged_action);
    m_edit_menu->add_action(*m_paste_action);
    m_edit_menu->add_action(*m_undo_action);
    m_edit_menu->add_action(*m_redo_action);
    m_edit_menu->add_separator();

    m_edit_menu->add_action(GUI::CommonActions::make_select_all_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (!editor->active_layer())
            return;
        editor->selection().merge(editor->active_layer()->relative_rect(), PixelPaint::Selection::MergeMode::Set);
    }));
    m_edit_menu->add_action(GUI::Action::create(
        "Clear &Selection", g_icon_bag.clear_selection, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->selection().clear();
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
                GUI::MessageBox::show_error(&window, String::formatted("Loading color palette failed: {}", result.error()));
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
                GUI::MessageBox::show_error(&window, String::formatted("Writing color palette failed: {}", result.error()));
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
            if (dialog->exec() != GUI::Dialog::ExecOK)
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
            Config::write_bool("PixelPaint", "Guides", "Show", action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_guide_visibility(action.is_checked());
        });
    m_show_guides_action->set_checked(Config::read_bool("PixelPaint", "Guides", "Show", true));

    m_view_menu->add_action(*m_zoom_in_action);
    m_view_menu->add_action(*m_zoom_out_action);
    m_view_menu->add_action(*m_reset_zoom_action);
    m_view_menu->add_action(GUI::Action::create(
        "Fit Image To &View", [&](auto&) {
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
            Config::write_bool("PixelPaint", "PixelGrid", "Show", action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_pixel_grid_visibility(action.is_checked());
        });
    show_pixel_grid_action->set_checked(Config::read_bool("PixelPaint", "PixelGrid", "Show", true));
    m_view_menu->add_action(*show_pixel_grid_action);

    m_show_rulers_action = GUI::Action::create_checkable(
        "Show R&ulers", { Mod_Ctrl, Key_R }, [&](auto& action) {
            Config::write_bool("PixelPaint", "Rulers", "Show", action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_ruler_visibility(action.is_checked());
        });
    m_show_rulers_action->set_checked(Config::read_bool("PixelPaint", "Rulers", "Show", true));
    m_view_menu->add_action(*m_show_rulers_action);

    m_show_active_layer_boundary_action = GUI::Action::create_checkable(
        "Show Active Layer &Boundary", [&](auto& action) {
            Config::write_bool("PixelPaint", "ImageEditor", "ShowActiveLayerBoundary", action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_show_active_layer_boundary(action.is_checked());
        });
    m_show_active_layer_boundary_action->set_checked(Config::read_bool("PixelPaint", "ImageEditor", "ShowActiveLayerBoundary", true));
    m_view_menu->add_action(*m_show_active_layer_boundary_action);

    m_tool_menu = window.add_menu("&Tool");
    m_toolbox->for_each_tool([&](auto& tool) {
        if (tool.action())
            m_tool_menu->add_action(*tool.action());
        return IterationDecision::Continue;
    });

    m_image_menu = window.add_menu("&Image");
    m_image_menu->add_action(GUI::Action::create(
        "Flip &Vertically", g_icon_bag.edit_flip_vertical, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flip(Gfx::Orientation::Vertical);
        }));
    m_image_menu->add_action(GUI::Action::create(
        "Flip &Horizontally", g_icon_bag.edit_flip_horizontal, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flip(Gfx::Orientation::Horizontal);
        }));
    m_image_menu->add_separator();

    m_image_menu->add_action(GUI::CommonActions::make_rotate_counterclockwise_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().rotate(Gfx::RotationDirection::CounterClockwise);
        }));

    m_image_menu->add_action(GUI::CommonActions::make_rotate_clockwise_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().rotate(Gfx::RotationDirection::Clockwise);
        }));
    m_image_menu->add_separator();
    m_image_menu->add_action(GUI::Action::create(
        "&Crop To Selection", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            // FIXME: disable this action if there is no selection
            if (editor->selection().is_empty())
                return;
            auto crop_rect = editor->image().rect().intersected(editor->selection().bounding_rect());
            editor->image().crop(crop_rect);
            editor->selection().clear();
        }));

    m_layer_menu = window.add_menu("&Layer");
    m_layer_menu->add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, g_icon_bag.new_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::CreateNewLayerDialog::construct(editor->image().size(), &window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto layer_or_error = PixelPaint::Layer::try_create_with_size(editor->image(), dialog->layer_size(), dialog->layer_name());
                if (layer_or_error.is_error()) {
                    GUI::MessageBox::show_error(&window, String::formatted("Unable to create layer with size {}", dialog->size()));
                    return;
                }
                editor->image().add_layer(layer_or_error.release_value());
                editor->layers_did_change();
                m_layer_list_widget->select_top_layer();
            }
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
        "Fl&atten Image", { Mod_Ctrl, Key_F }, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().flatten_all_layers();
            editor->did_complete_action();
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "&Merge Visible", { Mod_Ctrl, Key_M }, g_icon_bag.merge_visible, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().merge_visible_layers();
            editor->did_complete_action();
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "Merge &Active Layer Up", g_icon_bag.merge_active_layer_up, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_up(*active_layer);
            editor->did_complete_action();
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "M&erge Active Layer Down", { Mod_Ctrl, Key_E }, g_icon_bag.merge_active_layer_down, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_down(*active_layer);
            editor->did_complete_action();
        }));

    m_filter_menu = window.add_menu("&Filter");

    m_filter_menu->add_action(GUI::Action::create("Filter &Gallery", g_icon_bag.filter, [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        auto dialog = PixelPaint::FilterGallery::construct(&window, editor);
        if (dialog->exec() != GUI::Dialog::ExecOK)
            return;
    }));

    m_filter_menu->add_separator();
    m_filter_menu->add_action(GUI::Action::create("Generic 5x5 &Convolution", [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (auto* layer = editor->active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(&window)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Pixel Paint", GUI::Icon::default_icon("app-pixel-paint"), &window));

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(*m_new_image_action);
    toolbar.add_action(*m_open_image_action);
    toolbar.add_action(*m_save_image_action);
    toolbar.add_separator();
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
    m_zoom_combobox->set_model(*GUI::ItemListModel<String>::create(s_suggested_zoom_levels));
    m_zoom_combobox->on_change = [this](String const& value, GUI::ModelIndex const& index) {
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
        GUI::MessageBox::show_error(window(), String::formatted("Unable to open file: {}, {}", file.filename(), try_load.error()));
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
    bg_layer->bitmap().fill(Color::White);

    m_layer_list_widget->set_image(image);

    auto& editor = create_new_editor(*image);
    editor.set_active_layer(bg_layer);
    editor.undo_stack().set_current_unmodified();
}

void MainWidget::create_image_from_clipboard()
{
    auto bitmap = GUI::Clipboard::the().fetch_data_and_type().as_bitmap();
    if (!bitmap) {
        GUI::MessageBox::show(window(), "There is no image in a clipboard to paste.", "PixelPaint", GUI::MessageBox::Type::Warning);
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
            m_statusbar->set_override_text(mouse_position.to_string());
        } else {
            m_statusbar->set_override_text({});
        }
    };

    image_editor.on_leave = [&]() {
        m_statusbar->set_override_text({});
    };

    image_editor.on_set_guide_visibility = [&](bool show_guides) {
        m_show_guides_action->set_checked(show_guides);
    };

    image_editor.on_set_ruler_visibility = [&](bool show_rulers) {
        m_show_rulers_action->set_checked(show_rulers);
    };

    image_editor.on_scale_change = [this](float scale) {
        m_zoom_combobox->set_text(String::formatted("{}%", roundf(scale * 100)));
    };

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

void MainWidget::drop_event(GUI::DropEvent& event)
{
    if (!event.mime_data().has_urls())
        return;

    event.accept();

    if (event.mime_data().urls().is_empty())
        return;

    for (auto& url : event.mime_data().urls()) {
        if (url.protocol() != "file")
            continue;

        auto response = FileSystemAccessClient::Client::the().try_request_file(window(), url.path(), Core::OpenMode::ReadOnly);
        if (response.is_error())
            return;
        open_image(response.value());
    }
}
}
