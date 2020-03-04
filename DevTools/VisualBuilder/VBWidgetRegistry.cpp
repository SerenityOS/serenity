/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "VBWidgetRegistry.h"
#include "VBProperty.h"
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextEditor.h>

String to_class_name(VBWidgetType type)
{
    switch (type) {
    case VBWidgetType::GWidget:
        return "GUI::Widget";
    case VBWidgetType::GButton:
        return "GButton";
    case VBWidgetType::GLabel:
        return "GLabel";
    case VBWidgetType::GSpinBox:
        return "GSpinBox";
    case VBWidgetType::GTextEditor:
        return "GTextEditor";
    case VBWidgetType::GProgressBar:
        return "GProgressBar";
    case VBWidgetType::GCheckBox:
        return "GCheckBox";
    case VBWidgetType::GRadioButton:
        return "GRadioButton";
    case VBWidgetType::GScrollBar:
        return "GScrollBar";
    case VBWidgetType::GGroupBox:
        return "GGroupBox";
    case VBWidgetType::GSlider:
        return "GSlider";
    default:
        ASSERT_NOT_REACHED();
    }
}

VBWidgetType widget_type_from_class_name(const StringView& name)
{
    if (name == "GUI::Widget")
        return VBWidgetType::GWidget;
    if (name == "GButton")
        return VBWidgetType::GButton;
    if (name == "GLabel")
        return VBWidgetType::GLabel;
    if (name == "GSpinBox")
        return VBWidgetType::GSpinBox;
    if (name == "GTextEditor")
        return VBWidgetType::GTextEditor;
    if (name == "GProgressBar")
        return VBWidgetType::GProgressBar;
    if (name == "GCheckBox")
        return VBWidgetType::GCheckBox;
    if (name == "GRadioButton")
        return VBWidgetType::GRadioButton;
    if (name == "GScrollBar")
        return VBWidgetType::GScrollBar;
    if (name == "GGroupBox")
        return VBWidgetType::GGroupBox;
    if (name == "GSlider")
        return VBWidgetType::GSlider;
    ASSERT_NOT_REACHED();
}

static RefPtr<GUI::Widget> build_gwidget(VBWidgetType type, GUI::Widget* parent)
{
    switch (type) {
    case VBWidgetType::GWidget:
        return parent->add<GUI::Widget>();
    case VBWidgetType::GScrollBar:
        return parent->add<GUI::ScrollBar>(Orientation::Vertical);
    case VBWidgetType::GGroupBox:
        return parent->add<GUI::GroupBox>("groupbox_1");
    case VBWidgetType::GLabel: {
        auto& label = parent->add<GUI::Label>();
        label.set_fill_with_background_color(true);
        label.set_text("label_1");
        return label;
    }
    case VBWidgetType::GButton: {
        auto& button = parent->add<GUI::Button>();
        button.set_text("button_1");
        return button;
    }
    case VBWidgetType::GSpinBox: {
        auto& box = parent->add<GUI::SpinBox>();
        box.set_range(0, 100);
        box.set_value(0);
        return box;
    }
    case VBWidgetType::GTextEditor: {
        auto& editor = parent->add<GUI::TextEditor>();
        editor.set_ruler_visible(false);
        return editor;
    }
    case VBWidgetType::GProgressBar: {
        auto& bar = parent->add<GUI::ProgressBar>();
        bar.set_format(GUI::ProgressBar::Format::NoText);
        bar.set_range(0, 100);
        bar.set_value(50);
        return bar;
    }
    case VBWidgetType::GSlider: {
        auto& slider = parent->add<GUI::HorizontalSlider>();
        slider.set_range(0, 100);
        slider.set_value(50);
        return slider;
    }
    case VBWidgetType::GCheckBox: {
        auto& box = parent->add<GUI::CheckBox>();
        box.set_text("checkbox_1");
        return box;
    }
    case VBWidgetType::GRadioButton:
        return parent->add<GUI::RadioButton>("radio_1");
    default:
        ASSERT_NOT_REACHED();
        return nullptr;
    }
}

RefPtr<GUI::Widget> VBWidgetRegistry::build_gwidget(VBWidget& widget, VBWidgetType type, GUI::Widget* parent, NonnullOwnPtrVector<VBProperty>& properties)
{
    auto gwidget = ::build_gwidget(type, parent);
    auto add_readonly_property = [&](const String& name, const GUI::Variant& value) {
        auto property = make<VBProperty>(widget, name, value);
        property->set_readonly(true);
        properties.append(move(property));
    };
    add_readonly_property("class", to_class_name(type));
    return gwidget;
}
