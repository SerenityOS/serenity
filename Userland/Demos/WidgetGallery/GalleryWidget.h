/*
 * Copyright (c) 2021, the SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>

class GalleryWidget final : public GUI::Widget {
    C_OBJECT(GalleryWidget)
public:
    virtual ~GalleryWidget() override;

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

    RefPtr<GUI::VerticalProgressBar> m_vertical_progressbar_left;
    RefPtr<GUI::VerticalProgressBar> m_vertical_progressbar_right;
    RefPtr<GUI::HorizontalProgressBar> m_horizontal_progressbar;

    RefPtr<GUI::ScrollBar> m_enabled_scrollbar;
    RefPtr<GUI::ScrollBar> m_disabled_scrollbar;

    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::TextEditor> m_wizard_output;

    RefPtr<GUI::Frame> m_label_frame;
    RefPtr<GUI::Label> m_enabled_label;
    RefPtr<GUI::SpinBox> m_thickness_spinbox;
    RefPtr<GUI::ColorInput> m_font_colorinput;
    RefPtr<GUI::TableView> m_icons_tableview;
    RefPtr<GUI::TableView> m_cursors_tableview;
    RefPtr<GUI::OpacitySlider> m_opacity_slider;
    RefPtr<GUI::ImageWidget> m_opacity_imagewidget;

    Vector<String> m_frame_shapes;
    Vector<String> m_msgbox_icons;
    Vector<String> m_msgbox_buttons;
    Vector<RefPtr<Gfx::Bitmap>> m_button_icons;

    GUI::MessageBox::Type m_msgbox_type;
    GUI::MessageBox::InputType m_msgbox_input_type;
};
