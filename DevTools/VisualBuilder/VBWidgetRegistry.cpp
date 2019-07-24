#include "VBWidgetRegistry.h"
#include "VBProperty.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GRadioButton.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextEditor.h>

String to_class_name(VBWidgetType type)
{
    switch (type) {
    case VBWidgetType::GWidget:
        return "GWidget";
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
    if (name == "GWidget")
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

static GWidget* build_gwidget(VBWidgetType type, GWidget* parent)
{
    switch (type) {
    case VBWidgetType::GWidget:
        return new GWidget(parent);
    case VBWidgetType::GScrollBar:
        return new GScrollBar(Orientation::Vertical, parent);
    case VBWidgetType::GGroupBox:
        return new GGroupBox("groupbox_1", parent);
    case VBWidgetType::GLabel: {
        auto* label = new GLabel(parent);
        label->set_fill_with_background_color(true);
        label->set_text("label_1");
        return label;
    }
    case VBWidgetType::GButton: {
        auto* button = new GButton(parent);
        button->set_text("button_1");
        return button;
    }
    case VBWidgetType::GSpinBox: {
        auto* box = new GSpinBox(parent);
        box->set_range(0, 100);
        box->set_value(0);
        return box;
    }
    case VBWidgetType::GTextEditor: {
        auto* editor = new GTextEditor(GTextEditor::Type::MultiLine, parent);
        editor->set_ruler_visible(false);
        return editor;
    }
    case VBWidgetType::GProgressBar: {
        auto* bar = new GProgressBar(parent);
        bar->set_format(GProgressBar::Format::NoText);
        bar->set_range(0, 100);
        bar->set_value(50);
        return bar;
    }
    case VBWidgetType::GSlider: {
        auto* slider = new GSlider(Orientation::Horizontal, parent);
        slider->set_range(0, 100);
        slider->set_value(50);
        return slider;
    }
    case VBWidgetType::GCheckBox: {
        auto* box = new GCheckBox(parent);
        box->set_text("checkbox_1");
        return box;
    }
    case VBWidgetType::GRadioButton:
        return new GRadioButton("radio_1", parent);
    default:
        ASSERT_NOT_REACHED();
        return nullptr;
    }
}

GWidget* VBWidgetRegistry::build_gwidget(VBWidget& widget, VBWidgetType type, GWidget* parent, NonnullOwnPtrVector<VBProperty>& properties)
{
    auto* gwidget = ::build_gwidget(type, parent);
    auto add_readonly_property = [&](const String& name, const GVariant& value) {
        auto property = make<VBProperty>(widget, name, value);
        property->set_readonly(true);
        properties.append(move(property));
    };
    add_readonly_property("class", to_class_name(type));
    return gwidget;
}
