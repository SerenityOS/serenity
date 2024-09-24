/*
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include "HistogramWidget.h"
#include "IconBag.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include "LayerListWidget.h"
#include "LayerPropertiesWidget.h"
#include "PaletteWidget.h"
#include "ProjectLoader.h"
#include "ToolPropertiesWidget.h"
#include "ToolboxWidget.h"
#include "Tools/Tool.h"
#include "VectorscopeWidget.h"
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

extern IconBag g_icon_bag;

class MainWidget : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    virtual ~MainWidget() {};

    ErrorOr<void> initialize_menubar(GUI::Window&);

    void open_image(FileSystemAccessClient::File);
    ErrorOr<void> create_default_image();

    void refresh_crop_to_selection_menu_actions();
    bool request_close();

private:
    MainWidget();

    ImageEditor* current_image_editor();
    ImageEditor& create_new_editor(NonnullRefPtr<Image>);
    ErrorOr<void> create_image_from_clipboard();

    void image_editor_did_update_undo_stack();

    void set_actions_enabled(bool enabled);
    void set_mask_actions_for_layer(Layer* active_layer);

    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void update_window_modified();
    void update_status_bar(ByteString appended_text = ByteString::empty());

    ProjectLoader m_loader;

    RefPtr<ToolboxWidget> m_toolbox;
    RefPtr<PaletteWidget> m_palette_widget;
    RefPtr<HistogramWidget> m_histogram_widget;
    RefPtr<VectorscopeWidget> m_vectorscope_widget;
    RefPtr<LayerListWidget> m_layer_list_widget;
    RefPtr<LayerPropertiesWidget> m_layer_properties_widget;
    RefPtr<ToolPropertiesWidget> m_tool_properties_widget;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::ComboBox> m_zoom_combobox;

    RefPtr<GUI::Menu> m_export_submenu;
    RefPtr<GUI::Menu> m_edit_menu;
    RefPtr<GUI::Menu> m_view_menu;
    RefPtr<GUI::Menu> m_tool_menu;
    RefPtr<GUI::Menu> m_image_menu;
    RefPtr<GUI::Menu> m_layer_menu;
    RefPtr<GUI::Menu> m_filter_menu;

    RefPtr<GUI::Action> m_new_image_action;
    RefPtr<GUI::Action> m_new_image_from_clipboard_action;
    RefPtr<GUI::Action> m_open_image_action;
    RefPtr<GUI::Action> m_save_image_action;
    RefPtr<GUI::Action> m_save_image_as_action;
    RefPtr<GUI::Action> m_close_image_action;
    RefPtr<GUI::Action> m_levels_dialog_action;

    RefPtr<GUI::Action> m_cut_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_copy_merged_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_undo_action;
    RefPtr<GUI::Action> m_redo_action;

    RefPtr<GUI::Action> m_zoom_in_action;
    RefPtr<GUI::Action> m_zoom_out_action;
    RefPtr<GUI::Action> m_reset_zoom_action;
    RefPtr<GUI::Action> m_add_guide_action;
    RefPtr<GUI::Action> m_show_guides_action;
    RefPtr<GUI::Action> m_show_rulers_action;
    RefPtr<GUI::Action> m_show_active_layer_boundary_action;

    RefPtr<GUI::Action> m_layer_via_copy;
    RefPtr<GUI::Action> m_layer_via_cut;

    RefPtr<GUI::Action> m_add_mask_action;
    RefPtr<GUI::Action> m_delete_mask_action;
    RefPtr<GUI::Action> m_apply_mask_action;
    RefPtr<GUI::Action> m_add_editing_mask_action;
    RefPtr<GUI::Action> m_invert_mask_action;
    RefPtr<GUI::Action> m_clear_mask_action;
    RefPtr<GUI::Action> m_toggle_mask_visibility_action;
    RefPtr<GUI::Action> m_open_luminosity_masking_action;
    RefPtr<GUI::Action> m_open_color_masking_action;
    RefPtr<GUI::Action> m_crop_image_to_selection_action;
    RefPtr<GUI::Action> m_crop_layer_to_selection_action;

    Gfx::IntPoint m_last_image_editor_mouse_position;
};

}
