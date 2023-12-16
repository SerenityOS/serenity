/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/HorizontalSlider.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>

class GalleryWidget final : public GUI::Widget {
    C_OBJECT(GalleryWidget)
public:
    virtual ~GalleryWidget() override = default;

private:
    GalleryWidget();

    RefPtr<GUI::Button> m_font_button;
    RefPtr<GUI::Button> m_file_button;
    RefPtr<GUI::Button> m_icon_button;
    RefPtr<GUI::Button> m_input_button;
    RefPtr<GUI::Button> m_wizard_button;
    RefPtr<GUI::Button> m_msgbox_button;
    RefPtr<GUI::Button> m_disabled_icon_button;

    RefPtr<GUI::ComboBox> m_frame_shape_combobox;
    RefPtr<GUI::ComboBox> m_msgbox_icon_combobox;
    RefPtr<GUI::ComboBox> m_msgbox_buttons_combobox;

    RefPtr<GUI::VerticalSlider> m_vertical_slider_left;
    RefPtr<GUI::VerticalSlider> m_vertical_slider_right;
    RefPtr<GUI::HorizontalSlider> m_horizontal_slider_left;
    RefPtr<GUI::HorizontalSlider> m_horizontal_slider_right;

    RefPtr<GUI::VerticalProgressbar> m_vertical_progressbar_left;
    RefPtr<GUI::VerticalProgressbar> m_vertical_progressbar_right;
    RefPtr<GUI::HorizontalProgressbar> m_horizontal_progressbar;

    RefPtr<GUI::Scrollbar> m_enabled_scrollbar;
    RefPtr<GUI::Scrollbar> m_disabled_scrollbar;

    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::TextEditor> m_wizard_output;

    RefPtr<GUI::Frame> m_label_frame;
    RefPtr<GUI::Label> m_enabled_label;
    RefPtr<GUI::ColorInput> m_font_colorinput;
    RefPtr<GUI::TableView> m_icons_tableview;
    RefPtr<GUI::TableView> m_cursors_tableview;
    RefPtr<GUI::OpacitySlider> m_opacity_slider;
    RefPtr<GUI::ValueSlider> m_opacity_value_slider;
    RefPtr<GUI::ImageWidget> m_opacity_imagewidget;

    Vector<ByteString> m_frame_shapes;
    Vector<ByteString> m_msgbox_icons;
    Vector<ByteString> m_msgbox_buttons;
    Vector<RefPtr<Gfx::Bitmap>> m_button_icons;

    GUI::MessageBox::Type m_msgbox_type;
    GUI::MessageBox::InputType m_msgbox_input_type;
};
