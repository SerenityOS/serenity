/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "EditGuideDialog.h"
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
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

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
        if (auto* editor = current_image_editor())
            editor->set_active_layer(layer);
    };

    m_layer_properties_widget = *find_descendant_of_type_named<PixelPaint::LayerPropertiesWidget>("layer_properties_widget");
    m_tool_properties_widget = *find_descendant_of_type_named<PixelPaint::ToolPropertiesWidget>("tool_properties_widget");

    m_toolbox->on_tool_selection = [&](auto* tool) {
        if (auto* editor = current_image_editor())
            editor->set_active_tool(tool);
        m_tool_properties_widget->set_active_tool(tool);
    };

    m_tab_widget->on_middle_click = [&](auto& widget) {
        m_tab_widget->on_tab_close_click(widget);
    };

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        if (request_close()) {
            auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
            m_tab_widget->deferred_invoke([&] {
                m_tab_widget->remove_tab(image_editor);
                if (m_tab_widget->children().size() == 1) {
                    m_layer_list_widget->set_image(nullptr);
                    m_layer_properties_widget->set_layer(nullptr);
                }
            });
        }
    };

    m_tab_widget->on_change = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        m_palette_widget->set_image_editor(image_editor);
        m_layer_list_widget->set_image(&image_editor.image());
        m_layer_properties_widget->set_layer(image_editor.active_layer());
        if (auto* active_tool = m_toolbox->active_tool())
            image_editor.set_active_tool(active_tool);
        m_show_guides_action->set_checked(image_editor.guide_visibility());
        m_show_rulers_action->set_checked(image_editor.ruler_visibility());
        image_editor.on_scale_changed(image_editor.scale());
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
        "&New Image...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            auto dialog = PixelPaint::CreateNewImageDialog::construct(&window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto image = PixelPaint::Image::try_create_with_size(dialog->image_size()).release_value_but_fixme_should_propagate_errors();
                auto bg_layer = PixelPaint::Layer::try_create_with_size(*image, image->size(), "Background").release_value_but_fixme_should_propagate_errors();
                image->add_layer(*bg_layer);
                bg_layer->bitmap().fill(Color::White);
                auto image_title = dialog->image_name().trim_whitespace();
                image->set_title(image_title.is_empty() ? "Untitled" : image_title);

                create_new_editor(*image);
                m_layer_list_widget->set_image(image);
                m_layer_list_widget->set_selected_layer(bg_layer);
            }
        });

    m_open_image_action = GUI::CommonActions::make_open_action([&](auto&) {
        auto result = FileSystemAccessClient::Client::the().open_file(window.window_id());
        if (result.error != 0)
            return;

        open_image_fd(*result.fd, *result.chosen_file);
    });

    m_save_image_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        auto save_result = FileSystemAccessClient::Client::the().save_file(window.window_id(), "untitled", "pp");
        if (save_result.error != 0)
            return;
        auto result = editor->save_project_to_fd_and_close(*save_result.fd);
        if (result.is_error()) {
            GUI::MessageBox::show_error(&window, String::formatted("Could not save {}: {}", *save_result.chosen_file, result.error()));
            return;
        }
        editor->image().set_path(*save_result.chosen_file);
    });

    file_menu.add_action(*m_new_image_action);
    file_menu.add_action(*m_open_image_action);
    file_menu.add_action(*m_save_image_as_action);

    auto& export_submenu = file_menu.add_submenu("&Export");

    export_submenu.add_action(
        GUI::Action::create(
            "As &BMP", [&](auto&) {
                auto* editor = current_image_editor();
                if (!editor)
                    return;
                auto save_result = FileSystemAccessClient::Client::the().save_file(window.window_id(), "untitled", "bmp");
                if (save_result.error != 0)
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_bmp_to_fd_and_close(*save_result.fd, preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, String::formatted("Export to BMP failed: {}", result.error()));
            }));

    export_submenu.add_action(
        GUI::Action::create(
            "As &PNG", [&](auto&) {
                auto* editor = current_image_editor();
                auto save_result = FileSystemAccessClient::Client::the().save_file(window.window_id(), "untitled", "png");
                if (save_result.error != 0)
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_png_to_fd_and_close(*save_result.fd, preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, String::formatted("Export to PNG failed: {}", result.error()));
            }));

    file_menu.add_separator();
    file_menu.add_action(
        GUI::Action::create(
            "&Close Image", { Mod_Ctrl, Key_W }, [&](auto&) {
                auto* active_widget = m_tab_widget->active_widget();
                if (!active_widget)
                    return;
                m_tab_widget->on_tab_close_click(*active_widget);
            }));
    file_menu.add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (request_close())
            GUI::Application::the()->quit();
    }));

    auto& edit_menu = window.add_menu("&Edit");

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
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
        "Copy &Merged", { Mod_Ctrl | Mod_Shift, Key_C }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto bitmap = editor->image().try_copy_bitmap(editor->selection());
            if (!bitmap) {
                dbgln("try_copy_bitmap() from Image failed");
                return;
            }
            GUI::Clipboard::the().set_bitmap(*bitmap);
        });

    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
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
        if (auto* editor = current_image_editor())
            editor->redo();
    });

    edit_menu.add_action(*m_copy_action);
    edit_menu.add_action(*m_copy_merged_action);
    edit_menu.add_action(*m_paste_action);

    edit_menu.add_action(*m_undo_action);
    edit_menu.add_action(*m_redo_action);

    edit_menu.add_separator();
    edit_menu.add_action(GUI::CommonActions::make_select_all_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor->active_layer())
            return;
        editor->selection().merge(editor->active_layer()->relative_rect(), PixelPaint::Selection::MergeMode::Set);
    }));
    edit_menu.add_action(GUI::Action::create(
        "Clear &Selection", { Mod_Ctrl | Mod_Shift, Key_A }, [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->selection().clear();
        }));

    edit_menu.add_separator();
    edit_menu.add_action(GUI::Action::create(
        "S&wap Colors", { Mod_None, Key_X }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto old_primary_color = editor->primary_color();
            editor->set_primary_color(editor->secondary_color());
            editor->set_secondary_color(old_primary_color);
        }));
    edit_menu.add_action(GUI::Action::create(
        "&Default Colors", { Mod_None, Key_D }, [&](auto&) {
            if (auto* editor = current_image_editor()) {
                editor->set_primary_color(Color::Black);
                editor->set_secondary_color(Color::White);
            }
        }));
    edit_menu.add_action(GUI::Action::create(
        "&Load Color Palette", [&](auto&) {
            auto open_result = FileSystemAccessClient::Client::the().open_file(window.window_id(), "Load Color Palette");
            if (open_result.error != 0)
                return;

            auto result = PixelPaint::PaletteWidget::load_palette_fd_and_close(*open_result.fd);
            if (result.is_error()) {
                GUI::MessageBox::show_error(&window, String::formatted("Loading color palette failed: {}", result.error()));
                return;
            }

            m_palette_widget->display_color_list(result.value());
        }));
    edit_menu.add_action(GUI::Action::create(
        "Sa&ve Color Palette", [&](auto&) {
            auto save_result = FileSystemAccessClient::Client::the().save_file(window.window_id(), "untitled", "palette");
            if (save_result.error != 0)
                return;

            auto result = PixelPaint::PaletteWidget::save_palette_fd_and_close(m_palette_widget->colors(), *save_result.fd);
            if (result.is_error())
                GUI::MessageBox::show_error(&window, String::formatted("Writing color palette failed: {}", result.error()));
        }));

    auto& view_menu = window.add_menu("&View");

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->scale_by(0.1f);
        });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->scale_by(-0.1f);
        });

    m_reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->reset_scale_and_position();
        });

    m_add_guide_action = GUI::Action::create(
        "&Add Guide", [&](auto&) {
            auto dialog = PixelPaint::EditGuideDialog::construct(&window);
            if (dialog->exec() != GUI::Dialog::ExecOK)
                return;
            if (auto* editor = current_image_editor()) {
                auto offset = dialog->offset_as_pixel(*editor);
                if (!offset.has_value())
                    return;
                editor->add_guide(PixelPaint::Guide::construct(dialog->orientation(), offset.value()));
            }
        });

    // Save this so other methods can use it
    m_show_guides_action = GUI::Action::create_checkable(
        "Show &Guides", [&](auto& action) {
            Config::write_bool("PixelPaint", "Guides", "Show", action.is_checked());
            if (auto* editor = current_image_editor()) {
                editor->set_guide_visibility(action.is_checked());
            }
        });
    m_show_guides_action->set_checked(Config::read_bool("PixelPaint", "Guides", "Show", true));

    view_menu.add_action(*m_zoom_in_action);
    view_menu.add_action(*m_zoom_out_action);
    view_menu.add_action(*m_reset_zoom_action);
    view_menu.add_action(GUI::Action::create(
        "&Fit Image To View", [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->fit_image_to_view();
        }));
    view_menu.add_separator();
    view_menu.add_action(*m_add_guide_action);
    view_menu.add_action(*m_show_guides_action);

    view_menu.add_action(GUI::Action::create(
        "&Clear Guides", [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->clear_guides();
        }));
    view_menu.add_separator();

    auto show_pixel_grid_action = GUI::Action::create_checkable(
        "Show &Pixel Grid", [&](auto& action) {
            Config::write_bool("PixelPaint", "PixelGrid", "Show", action.is_checked());
            if (auto* editor = current_image_editor())
                editor->set_pixel_grid_visibility(action.is_checked());
        });
    show_pixel_grid_action->set_checked(Config::read_bool("PixelPaint", "PixelGrid", "Show", true));
    view_menu.add_action(*show_pixel_grid_action);

    m_show_rulers_action = GUI::Action::create_checkable(
        "Show R&ulers", { Mod_Ctrl, Key_R }, [&](auto& action) {
            Config::write_bool("PixelPaint", "Rulers", "Show", action.is_checked());
            if (auto* editor = current_image_editor()) {
                editor->set_ruler_visibility(action.is_checked());
            }
        });
    m_show_rulers_action->set_checked(Config::read_bool("PixelPaint", "Rulers", "Show", true));
    view_menu.add_action(*m_show_rulers_action);

    m_show_active_layer_boundary_action = GUI::Action::create_checkable(
        "Show Active Layer &Boundary", [&](auto& action) {
            Config::write_bool("PixelPaint", "ImageEditor", "ShowActiveLayerBoundary", action.is_checked());
            if (auto* editor = current_image_editor())
                editor->set_show_active_layer_boundary(action.is_checked());
        });
    m_show_active_layer_boundary_action->set_checked(Config::read_bool("PixelPaint", "ImageEditor", "ShowActiveLayerBoundary", true));
    view_menu.add_action(*m_show_active_layer_boundary_action);

    auto& tool_menu = window.add_menu("&Tool");
    m_toolbox->for_each_tool([&](auto& tool) {
        if (tool.action())
            tool_menu.add_action(*tool.action());
        return IterationDecision::Continue;
    });

    auto& image_menu = window.add_menu("&Image");
    image_menu.add_action(GUI::Action::create(
        "Flip &Vertically", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flip(Gfx::Orientation::Vertical);
        }));
    image_menu.add_action(GUI::Action::create(
        "Flip &Horizontally", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flip(Gfx::Orientation::Horizontal);
        }));
    image_menu.add_separator();
    image_menu.add_action(GUI::Action::create(
        "Rotate &Left", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().rotate(Gfx::RotationDirection::CounterClockwise);
        }));
    image_menu.add_action(GUI::Action::create(
        "Rotate &Right", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().rotate(Gfx::RotationDirection::Clockwise);
        }));
    image_menu.add_separator();
    image_menu.add_action(GUI::Action::create(
        "&Crop To Selection", [&](auto&) {
            auto* editor = current_image_editor();
            // FIXME: disable this action if there is no selection
            if (!editor || editor->selection().is_empty())
                return;
            auto crop_rect = editor->selection().bounding_rect();
            editor->image().crop(crop_rect);
            editor->selection().clear();
        }));

    auto& layer_menu = window.add_menu("&Layer");
    layer_menu.add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-layer.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
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

    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Select &Previous Layer", { 0, Key_PageUp }, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(1);
        }));
    layer_menu.add_action(GUI::Action::create(
        "Select &Next Layer", { 0, Key_PageDown }, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(-1);
        }));
    layer_menu.add_action(GUI::Action::create(
        "Select &Top Layer", { 0, Key_Home }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/top-layer.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            m_layer_list_widget->select_top_layer();
        }));
    layer_menu.add_action(GUI::Action::create(
        "Select B&ottom Layer", { 0, Key_End }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bottom-layer.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            m_layer_list_widget->select_bottom_layer();
        }));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::CommonActions::make_move_to_front_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_front(*active_layer);
            editor->layers_did_change();
        }));
    layer_menu.add_action(GUI::CommonActions::make_move_to_back_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_back(*active_layer);
            editor->layers_did_change();
        }));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Up", { Mod_Ctrl, Key_PageUp }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_up(*active_layer);
        }));
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Down", { Mod_Ctrl, Key_PageDown }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_down(*active_layer);
        }));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "&Remove Active Layer", { Mod_Ctrl, Key_D }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
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
        layer_menu.popup(event.screen_position());
    };
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Fl&atten Image", { Mod_Ctrl, Key_F }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flatten_all_layers();
            editor->did_complete_action();
        }));

    layer_menu.add_action(GUI::Action::create(
        "&Merge Visible", { Mod_Ctrl, Key_M }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().merge_visible_layers();
            editor->did_complete_action();
        }));

    layer_menu.add_action(GUI::Action::create(
        "M&erge Active Layer Down", { Mod_Ctrl, Key_E }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_down(*active_layer);
            editor->did_complete_action();
        }));

    auto& filter_menu = window.add_menu("&Filter");
    auto& spatial_filters_menu = filter_menu.add_submenu("&Spatial");

    auto& edge_detect_submenu = spatial_filters_menu.add_submenu("&Edge Detect");
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Cardinal)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Diagonal)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    auto& blur_submenu = spatial_filters_menu.add_submenu("&Blur and Sharpen");
    blur_submenu.add_action(GUI::Action::create("&Gaussian Blur (3x3)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SpatialGaussianBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("G&aussian Blur (5x5)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SpatialGaussianBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Box Blur (3x3)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::BoxBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("B&ox Blur (5x5)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::BoxBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Sharpen", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SharpenFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SharpenFilter>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));

    spatial_filters_menu.add_separator();
    spatial_filters_menu.add_action(GUI::Action::create("Generic 5x5 &Convolution", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(&window)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        }
    }));

    auto& color_filters_menu = filter_menu.add_submenu("&Color");
    color_filters_menu.add_action(GUI::Action::create("Grayscale", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::GrayscaleFilter filter;
            filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
            layer->did_modify_bitmap(layer->rect());
            editor->did_complete_action();
        }
    }));
    color_filters_menu.add_action(GUI::Action::create("Invert", { Mod_Ctrl, Key_I }, [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::InvertFilter filter;
            filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
            layer->did_modify_bitmap(layer->rect());
            editor->did_complete_action();
        }
    }));

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Pixel Paint", GUI::Icon::default_icon("app-pixel-paint"), &window));

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(*m_new_image_action);
    toolbar.add_action(*m_open_image_action);
    toolbar.add_action(*m_save_image_as_action);
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
        if (editor == nullptr)
            return;

        if (index.is_valid()) {
            switch (index.row()) {
            case s_zoom_level_fit_width:
                editor->fit_image_to_view(ImageEditor::FitType::Width);
                return;
            case s_zoom_level_fit_height:
                editor->fit_image_to_view(ImageEditor::FitType::Height);
                return;
            case s_zoom_level_fit_image:
                editor->fit_image_to_view(ImageEditor::FitType::Image);
                return;
            }
        }

        auto zoom_level_optional = value.view().trim("%"sv, TrimMode::Right).to_int();
        if (!zoom_level_optional.has_value()) {
            // Indicate that a parse-error occurred by resetting the text to the current state.
            editor->on_scale_changed(editor->scale());
            return;
        }

        editor->set_absolute_scale(zoom_level_optional.value() * 1.0f / 100);
        // If the selected zoom level got clamped, or a "fit to …" level was selected,
        // there is a chance that the new scale is identical to the old scale.
        // In these cases, we need to manually reset the text:
        editor->on_scale_changed(editor->scale());
    };
    m_zoom_combobox->on_return_pressed = [this]() {
        m_zoom_combobox->on_change(m_zoom_combobox->text(), GUI::ModelIndex());
    };
}

void MainWidget::open_image_fd(int fd, String const& path)
{
    auto try_load = m_loader.try_load_from_fd_and_close(fd, path);

    if (try_load.is_error()) {
        GUI::MessageBox::show_error(window(), String::formatted("Unable to open file: {}, {}", path, try_load.error()));
        return;
    }

    auto& image = *m_loader.release_image();
    create_new_editor(image);
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
}

bool MainWidget::request_close()
{
    if (m_tab_widget->children().is_empty())
        return true;

    auto result = GUI::MessageBox::show(window(), "Save before closing?", "Save changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);

    if (result == GUI::MessageBox::ExecYes) {
        m_save_image_as_action->activate();
        return true;
    }

    if (result == GUI::MessageBox::ExecNo)
        return true;

    return false;
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

    image_editor.on_image_title_change = [&](auto const& title) {
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

    // NOTE: We invoke the above hook directly here to make sure the tab title is set up.
    image_editor.on_image_title_change(image->title());

    image_editor.on_scale_changed = [this](float scale) {
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

        auto result = FileSystemAccessClient::Client::the().request_file(window()->window_id(), url.path(), Core::OpenMode::ReadOnly);
        if (result.error != 0)
            continue;

        open_image_fd(*result.fd, *result.chosen_file);
    }
}
}
