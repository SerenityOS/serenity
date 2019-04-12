#include "VBWidgetRegistry.h"
#include "VBProperty.h"
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GGroupBox.h>

static String to_class_name(VBWidgetType type)
{
    switch (type) {
    case VBWidgetType::GWidget: return "GWidget";
    case VBWidgetType::GButton: return "GButton";
    case VBWidgetType::GLabel: return "GLabel";
    case VBWidgetType::GSpinBox: return "GSpinBox";
    case VBWidgetType::GTextEditor: return "GTextEditor";
    case VBWidgetType::GProgressBar: return "GProgressBar";
    case VBWidgetType::GCheckBox: return "GCheckBox";
    case VBWidgetType::GScrollBar: return "GScrollBar";
    case VBWidgetType::GGroupBox: return "GGroupBox";
    default: ASSERT_NOT_REACHED();
    }
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
        label->set_text("label_1");
        return label;
    }
    case VBWidgetType::GButton: {
        auto* button = new GButton(parent);
        button->set_caption("button_1");
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
    case VBWidgetType::GCheckBox: {
        auto* box = new GCheckBox(parent);
        box->set_caption("checkbox_1");
        return box;
    }
    default:
        ASSERT_NOT_REACHED();
        return nullptr;
    }
}

GWidget* VBWidgetRegistry::build_gwidget(VBWidgetType type, GWidget* parent, Vector<OwnPtr<VBProperty>>& properties)
{
    auto* gwidget = ::build_gwidget(type, parent);
    auto add_property = [&properties] (const String& name, const GVariant& value = { }, bool is_readonly = false) {
        auto property = make<VBProperty>(name, value);
        property->set_readonly(is_readonly);
        properties.append(move(property));
    };
    add_property("class", to_class_name(type), true);
    add_property("width");
    add_property("height");
    add_property("x");
    add_property("y");
    add_property("visible");
    add_property("enabled");
    add_property("tooltip");
    add_property("background_color");
    add_property("foreground_color");
    return gwidget;
}
