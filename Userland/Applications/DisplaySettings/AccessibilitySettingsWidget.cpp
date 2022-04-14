/*
* Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
* Copyright (c) 2021, Thomas Keppler <winfr34k@gmail.com>
* Copyright (c) 2022, the SerenityOS developers.
*
* SPDX-License-Identifier: BSD-2-Clause
*/

#include "AccessibilitySettingsWidget.h"
#include <Applications/DisplaySettings/AccessibilitySettingsGML.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/ConnectionToWindowServer.h>

namespace DisplaySettings {

//static void update_label_with_font(GUI::Label&, Gfx::Font const&);

AccessibilitySettingsWidget::AccessibilitySettingsWidget()
{
   load_from_gml(accessibility_settings_gml);

   m_filter_none = *find_descendant_of_type_named<GUI::RadioButton>("filter_none_radio_button");
   m_filter_protanopia = *find_descendant_of_type_named<GUI::RadioButton>("filter_protanopia_radio_button");
   m_filter_protanomaly = *find_descendant_of_type_named<GUI::RadioButton>("filter_protanomaly_radio_button");
   m_filter_deuteranopia = *find_descendant_of_type_named<GUI::RadioButton>("filter_deuteranopia_radio_button");
   m_filter_deuteranomaly = *find_descendant_of_type_named<GUI::RadioButton>("filter_deuteranomaly_radio_button");
   m_filter_tritanopia = *find_descendant_of_type_named<GUI::RadioButton>("filter_tritanopia_radio_button");
   m_filter_tritanomaly = *find_descendant_of_type_named<GUI::RadioButton>("filter_tritanomaly_radio_button");
   m_filter_achromatopsia = *find_descendant_of_type_named<GUI::RadioButton>("filter_achromatopsia_radio_button");
   m_filter_achromatomaly = *find_descendant_of_type_named<GUI::RadioButton>("filter_achromatomaly_radio_button");
   
//   auto color_wheel_bitmap = Gfx::Bitmap::try_load_from_file("/res/graphics/color-wheel.png").release_value_but_fixme_should_propagate_errors();
//
//   m_color_wheel = *find_descendant_of_type_named<GUI::ImageWidget>("color_wheel_image");
//   m_color_wheel->set_bitmap(color_wheel_bitmap);

   m_color_wheel = *find_descendant_of_type_named<GUI::ImageWidget>("color_wheel_image");
   m_color_wheel->load_from_file("/res/graphics/color-wheel.png");
}

void AccessibilitySettingsWidget::apply_settings()
{
    if (m_filter_none->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(0);
    } else if (m_filter_protanopia->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(1);
    } else if (m_filter_protanomaly->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(2);
    } else if (m_filter_deuteranopia->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(3);
    } else if (m_filter_deuteranomaly->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(4);
    } else if (m_filter_tritanopia->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(5);
    } else if (m_filter_tritanomaly->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(6);
    } else if (m_filter_achromatopsia->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(7);
    } else if (m_filter_achromatomaly->is_checked()) {
        GUI::ConnectionToWindowServer::the().async_set_screen_filter(8);
    }
}

}
