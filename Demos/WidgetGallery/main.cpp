/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Variant.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

template<typename T>
class ListViewModel final : public GUI::Model {
public:
    static NonnullRefPtr<ListViewModel> create(Vector<T>& model_items) { return adopt(*new ListViewModel(model_items)); }
    virtual ~ListViewModel() override { }
    virtual int row_count(const GUI::ModelIndex&) const override { return m_model_items.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        ASSERT(index.is_valid());
        ASSERT(index.column() == 0);
        if (role == GUI::ModelRole::Display)
            return m_model_items.at(index.row());
        return {};
    }
    virtual void update() override { did_update(); }

private:
    explicit ListViewModel(Vector<String>& model_items)
        : m_model_items(model_items)
    {
    }
    Vector<T>& m_model_items;
};

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-widget-gallery");

    auto window = GUI::Window::construct();
    window->resize(430, 480);
    window->set_title("Widget Gallery");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Widget Gallery");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Widget Gallery", app_icon.bitmap_for_size(32), window);
    }));

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_fill_with_background_color(true);
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_margins({ 4, 4, 4, 4 });

    auto& tab_widget = root_widget.add<GUI::TabWidget>();

    auto& tab_basic = tab_widget.add_tab<GUI::Widget>("Basic");
    tab_basic.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_basic.set_layout<GUI::VerticalBoxLayout>();
    tab_basic.layout()->set_margins({ 8, 8, 8, 8 });
    tab_basic.layout()->set_spacing(8);

    auto& radio_group_box = tab_basic.add<GUI::GroupBox>();
    radio_group_box.set_layout<GUI::HorizontalBoxLayout>();
    radio_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    radio_group_box.layout()->set_margins({ 4, 4, 4, 4 });

    auto& radio_button_vert_container = radio_group_box.add<GUI::Widget>();
    radio_button_vert_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    radio_button_vert_container.set_layout<GUI::VerticalBoxLayout>();
    radio_button_vert_container.layout()->set_margins({ 4, 9, 4, 4 });

    auto& radio_button_container = radio_button_vert_container.add<GUI::Widget>();
    radio_button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    radio_button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& radio1 = radio_button_container.add<GUI::RadioButton>("RadioButton 1");
    radio1.set_checked(true);
    auto& radio2 = radio_button_container.add<GUI::RadioButton>("RadioButton 2");
    (void)radio2;
    auto& radio3 = radio_button_container.add<GUI::RadioButton>("RadioButton 3");
    radio3.set_enabled(false);

    auto& checklabelspin_container = tab_basic.add<GUI::Widget>();
    checklabelspin_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    checklabelspin_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& check_group_box = checklabelspin_container.add<GUI::GroupBox>();
    check_group_box.set_layout<GUI::HorizontalBoxLayout>();
    check_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    check_group_box.layout()->set_margins({ 4, 12, 4, 4 });

    auto& checkbox_container = check_group_box.add<GUI::Widget>();
    checkbox_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    checkbox_container.set_layout<GUI::VerticalBoxLayout>();
    checkbox_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto& label_container = check_group_box.add<GUI::Widget>();
    label_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    label_container.set_layout<GUI::VerticalBoxLayout>();
    label_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto& spin_group_box = checklabelspin_container.add<GUI::GroupBox>();
    spin_group_box.set_layout<GUI::HorizontalBoxLayout>();
    spin_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    spin_group_box.layout()->set_margins({ 4, 4, 4, 4 });
    spin_group_box.set_title("Spin boxes");

    auto& spin_container = spin_group_box.add<GUI::Widget>();
    spin_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    spin_container.set_layout<GUI::VerticalBoxLayout>();
    spin_container.layout()->set_margins({ 4, 12, 4, 4 });

    auto& checkbox1 = checkbox_container.add<GUI::CheckBox>("CheckBox 1");
    checkbox1.set_checked(true);
    auto& checkbox2 = checkbox_container.add<GUI::CheckBox>("CheckBox 2");
    checkbox2.set_enabled(false);

    auto& label1 = label_container.add<GUI::Label>("Label 1");
    (void)label1;
    auto& label2 = label_container.add<GUI::Label>("Label 2");
    label2.set_enabled(false);

    auto& spinbox1 = spin_container.add<GUI::SpinBox>();
    (void)spinbox1;
    auto& spinbox2 = spin_container.add<GUI::SpinBox>();
    spinbox2.set_enabled(false);

    auto& button_container = tab_basic.add<GUI::Widget>();
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    button_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& button_vert1_container = button_container.add<GUI::Widget>();
    button_vert1_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    button_vert1_container.set_layout<GUI::VerticalBoxLayout>();

    auto& button_vert2_container = button_container.add<GUI::Widget>();
    button_vert2_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    button_vert2_container.set_layout<GUI::VerticalBoxLayout>();

    auto& button1 = button_vert1_container.add<GUI::Button>("Button 1");
    button1.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/kill.png"));
    auto& button2 = button_vert1_container.add<GUI::Button>("Button 2");
    button2.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/kill.png"));
    button2.set_enabled(false);
    auto& button3 = button_vert2_container.add<GUI::Button>("\xF0\x9F\x98\x88 Button 3");
    (void)button3;
    auto& button4 = button_vert2_container.add<GUI::Button>("\xF0\x9F\x8D\x86 Button 4");
    button4.set_enabled(false);

    auto& text_group_box = tab_basic.add<GUI::GroupBox>();
    text_group_box.set_layout<GUI::HorizontalBoxLayout>();
    text_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    text_group_box.set_title("Text boxes");
    text_group_box.layout()->set_margins({ 8, 4, 8, 4 });

    auto& textbox_vert1_container = text_group_box.add<GUI::Widget>();
    textbox_vert1_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    textbox_vert1_container.set_layout<GUI::VerticalBoxLayout>();
    textbox_vert1_container.layout()->set_margins({ 1, 12, 1, 4 });

    auto& textbox_vert2_container = text_group_box.add<GUI::Widget>();
    textbox_vert2_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    textbox_vert2_container.set_layout<GUI::VerticalBoxLayout>();
    textbox_vert2_container.layout()->set_margins({ 1, 12, 1, 4 });

    auto& textbox1 = textbox_vert1_container.add<GUI::TextBox>();
    textbox1.set_placeholder("Editable");
    auto& textbox2 = textbox_vert1_container.add<GUI::TextBox>();
    textbox2.set_text("Disabled");
    textbox2.set_enabled(false);
    auto& textbox3 = textbox_vert2_container.add<GUI::TextBox>();
    textbox3.set_text("Read only");
    textbox3.set_mode(GUI::TextEditor::ReadOnly);
    auto& textbox4 = textbox_vert2_container.add<GUI::TextBox>();
    textbox4.set_text("Display only");
    textbox4.set_mode(GUI::TextEditor::DisplayOnly);

    auto& combocolor_container = tab_basic.add<GUI::Widget>();
    combocolor_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    combocolor_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& combo_group_box = combocolor_container.add<GUI::GroupBox>();
    combo_group_box.set_layout<GUI::HorizontalBoxLayout>();
    combo_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    combo_group_box.layout()->set_margins({ 4, 4, 4, 4 });
    combo_group_box.set_title("Combo boxes");

    auto& color_group_box = combocolor_container.add<GUI::GroupBox>();
    color_group_box.set_layout<GUI::HorizontalBoxLayout>();
    color_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_group_box.layout()->set_margins({ 4, 4, 4, 4 });
    color_group_box.set_title("Color pickers");

    auto& combo_container = combo_group_box.add<GUI::Widget>();
    combo_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    combo_container.set_layout<GUI::VerticalBoxLayout>();
    combo_container.layout()->set_margins({ 4, 12, 4, 4 });

    auto& color_container = color_group_box.add<GUI::Widget>();
    color_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_container.set_layout<GUI::VerticalBoxLayout>();
    color_container.layout()->set_margins({ 4, 12, 4, 4 });

    Vector<String> model_items;
    model_items.append("Yes");
    model_items.append("No");
    model_items.append("Maybe");
    model_items.append("I don't know");
    model_items.append("Can you repeat the question?");

    auto& combobox1 = combo_container.add<GUI::ComboBox>();
    combobox1.set_only_allow_values_from_model(true);
    combobox1.set_model(*ListViewModel<AK::String>::create(model_items));

    auto& combobox2 = combo_container.add<GUI::ComboBox>();
    combobox2.set_enabled(false);

    auto& color_input_enabled = color_container.add<GUI::ColorInput>();
    color_input_enabled.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_input_enabled.set_color(Color::from_string("#961605ff").value());
    color_input_enabled.set_color_picker_title("Select color for desktop");

    auto& color_input_disabled = color_container.add<GUI::ColorInput>();
    color_input_disabled.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_input_disabled.set_color(Color::from_string("#961605ff").value());
    color_input_disabled.set_enabled(false);

    auto& tab_others = tab_widget.add_tab<GUI::Widget>("Sliders");
    tab_others.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_others.set_layout<GUI::VerticalBoxLayout>();
    tab_others.layout()->set_margins({ 8, 8, 8, 8 });
    tab_others.layout()->set_spacing(8);

    auto& vert_slider_group_box = tab_others.add<GUI::GroupBox>();
    vert_slider_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    vert_slider_group_box.set_layout<GUI::HorizontalBoxLayout>();
    vert_slider_group_box.layout()->set_margins({ 4, 28, 4, 4 });
    vert_slider_group_box.set_title("Vertical sliders");

    auto& vslider1 = vert_slider_group_box.add<GUI::VerticalSlider>();
    vslider1.set_tooltip("Fixed");
    auto& vslider2 = vert_slider_group_box.add<GUI::VerticalSlider>();
    vslider2.set_enabled(false);
    vslider2.set_tooltip("Disabled");
    auto& vslider3 = vert_slider_group_box.add<GUI::VerticalSlider>();
    vslider3.set_max(5);
    vslider3.set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);
    vslider3.set_tooltip("Proportional");

    auto& horizontal_slider_group_box = tab_others.add<GUI::GroupBox>();
    horizontal_slider_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    horizontal_slider_group_box.set_layout<GUI::VerticalBoxLayout>();
    horizontal_slider_group_box.layout()->set_margins({ 4, 12, 4, 4 });
    horizontal_slider_group_box.set_title("Horizontal sliders");

    auto& horizontal_slider_container = horizontal_slider_group_box.add<GUI::Widget>();
    horizontal_slider_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    horizontal_slider_container.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_slider_container.layout()->set_margins({ 4, 4, 4, 4 });

    auto& horizontal_slider_container2 = horizontal_slider_group_box.add<GUI::Widget>();
    horizontal_slider_container2.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    horizontal_slider_container2.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_slider_container2.layout()->set_margins({ 4, 4, 4, 4 });

    auto& slider1 = horizontal_slider_container.add<GUI::HorizontalSlider>();
    (void)slider1;
    auto& slider2 = horizontal_slider_container.add<GUI::HorizontalSlider>();
    slider2.set_enabled(false);
    slider2.set_value(50);
    auto& slider3 = horizontal_slider_container.add<GUI::HorizontalSlider>();
    slider3.set_max(5);
    slider3.set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);

    auto& progress1 = horizontal_slider_container2.add<GUI::ProgressBar>();
    progress1.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    progress1.set_preferred_size(0, 28);

    slider1.on_value_changed = [&](int value) {
        progress1.set_value(value);
        if (!(value % (100 / slider3.max())))
            slider3.set_value(value / (100 / slider3.max()));
    };

    slider3.on_value_changed = [&](int value) {
        progress1.set_value((value * 100) / slider3.max());
        slider1.set_value((value * 100) / slider3.max());
    };

    auto& scroll_group_box = tab_others.add<GUI::GroupBox>();
    scroll_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    scroll_group_box.set_layout<GUI::VerticalBoxLayout>();
    scroll_group_box.layout()->set_margins({ 12, 12, 12, 12 });
    scroll_group_box.set_title("Scrollbars");

    scroll_group_box.layout()->add_spacer();

    auto& scrollbar1 = scroll_group_box.add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar1.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar1.set_preferred_size(0, 16);
    scrollbar1.set_min(0);
    scrollbar1.set_max(100);
    scrollbar1.set_value(50);

    scroll_group_box.layout()->add_spacer();

    auto& scrollbar2 = scroll_group_box.add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar2.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar2.set_preferred_size(0, 16);
    scrollbar2.set_enabled(false);

    scroll_group_box.layout()->add_spacer();

    auto& tab_modals = tab_widget.add_tab<GUI::Widget>("Modals");
    tab_modals.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_modals.set_layout<GUI::VerticalBoxLayout>();
    tab_modals.layout()->set_margins({ 8, 8, 8, 8 });
    tab_modals.layout()->set_spacing(8);

    GUI::MessageBox::Type msg_box_type = GUI::MessageBox::Type::Error;

    auto& msgbox_group_container = tab_modals.add<GUI::GroupBox>("Message boxes");
    msgbox_group_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    msgbox_group_container.set_layout<GUI::VerticalBoxLayout>();
    msgbox_group_container.layout()->set_margins({ 4, 12, 4, 2 });

    auto& msgbox_radio_container = msgbox_group_container.add<GUI::Widget>();
    msgbox_radio_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    msgbox_radio_container.set_layout<GUI::HorizontalBoxLayout>();
    msgbox_radio_container.layout()->set_margins({ 4, 12, 4, 4 });

    auto& icon_group_box = msgbox_radio_container.add<GUI::GroupBox>("Icon");
    icon_group_box.set_layout<GUI::VerticalBoxLayout>();
    icon_group_box.layout()->set_margins({ 4, 16, 4, 4 });
    icon_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    auto& radio_none = icon_group_box.add<GUI::RadioButton>("None");
    radio_none.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::None;
    };
    auto& radio_information = icon_group_box.add<GUI::RadioButton>("\xE2\x84\xB9 Information");
    radio_information.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Information;
    };
    auto& question_information = icon_group_box.add<GUI::RadioButton>("\xF0\x9F\xA4\x94 Question");
    question_information.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Question;
    };
    auto& radio_warning = icon_group_box.add<GUI::RadioButton>("\xE2\x9A\xA0 Warning");
    radio_warning.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Warning;
    };
    auto& radio_error = icon_group_box.add<GUI::RadioButton>("\xE2\x9D\x8C Error");
    radio_error.set_checked(true);
    radio_error.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Error;
    };

    auto& button_group_box = msgbox_radio_container.add<GUI::GroupBox>("Buttons");
    button_group_box.set_layout<GUI::VerticalBoxLayout>();
    button_group_box.layout()->set_margins({ 4, 16, 4, 4 });
    button_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    GUI::MessageBox::InputType msg_box_input_type = GUI::MessageBox::InputType::OKCancel;

    auto& radio_ok = button_group_box.add<GUI::RadioButton>("OK");
    radio_ok.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::OK;
    };
    auto& radio_ok_cancel = button_group_box.add<GUI::RadioButton>("OK & Cancel");
    radio_ok_cancel.set_checked(true);
    radio_ok_cancel.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::OKCancel;
    };
    auto& radio_yes_no = button_group_box.add<GUI::RadioButton>("Yes & No");
    radio_yes_no.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::YesNo;
    };
    auto& radio_yes_no_cancel = button_group_box.add<GUI::RadioButton>("Yes & No & Cancel");
    radio_yes_no_cancel.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::YesNoCancel;
    };

    auto& msgbox_text_container = msgbox_group_container.add<GUI::Widget>();
    msgbox_text_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    msgbox_text_container.set_layout<GUI::VerticalBoxLayout>();
    msgbox_text_container.set_preferred_size(0, 100);
    msgbox_text_container.layout()->set_margins({ 4, 8, 4, 8 });

    auto& title_textbox = msgbox_text_container.add<GUI::TextBox>();
    title_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    title_textbox.set_preferred_size(0, 24);
    title_textbox.set_text("Demo Title");

    auto& content_textbox = msgbox_text_container.add<GUI::TextBox>();
    content_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    content_textbox.set_preferred_size(0, 24);
    content_textbox.set_text("Demo text for message box.");

    auto& msgbox_button = msgbox_text_container.add<GUI::Button>("Create");
    msgbox_button.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    msgbox_button.set_preferred_size(0, 30);
    msgbox_button.on_click = [&](auto) {
        GUI::MessageBox::show(window, content_textbox.text(), title_textbox.text(), msg_box_type, msg_box_input_type);
    };

    auto& input_group_box = tab_modals.add<GUI::GroupBox>("Input boxes");
    input_group_box.set_layout<GUI::VerticalBoxLayout>();
    input_group_box.layout()->set_margins({ 4, 12, 4, 4 });
    input_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    input_group_box.set_preferred_size(0, 160);

    input_group_box.layout()->add_spacer();

    auto& input_label = input_group_box.add<GUI::Label>("Valued user input goes here.");
    input_label.set_font(Gfx::Font::default_bold_font());

    input_group_box.layout()->add_spacer();

    auto& input_button_container = input_group_box.add<GUI::Widget>();
    input_button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    input_button_container.set_layout<GUI::VerticalBoxLayout>();
    input_button_container.layout()->set_margins({ 4, 0, 4, 0 });

    auto& input_button = input_button_container.add<GUI::Button>("Input...");
    input_button.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    input_button.set_preferred_size(0, 30);
    String value;
    input_button.on_click = [&](auto) {
        if (GUI::InputBox::show(value, window, "Enter input:", "Input Box") == GUI::InputBox::ExecOK && !value.is_empty())
            input_label.set_text(value);
    };

    auto& tab_image = tab_widget.add_tab<GUI::Widget>("Images");
    tab_image.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_image.set_layout<GUI::VerticalBoxLayout>();
    tab_image.layout()->set_margins({ 8, 8, 8, 8 });
    tab_image.layout()->set_spacing(8);

    auto& banner_image = tab_image.add<GUI::ImageWidget>();
    banner_image.set_frame_thickness(2);
    banner_image.load_from_file("/res/graphics/brand-banner.png");

    auto& gif_animation_image = tab_image.add<GUI::ImageWidget>();
    gif_animation_image.load_from_file("/res/graphics/download-animation.gif");

    auto& tab_cursors = tab_widget.add_tab<GUI::Widget>("Cursors");
    tab_cursors.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_cursors.set_layout<GUI::VerticalBoxLayout>();
    tab_cursors.layout()->set_margins({ 8, 8, 8, 8 });
    tab_cursors.layout()->set_spacing(8);

    auto& cursor_group_box = tab_cursors.add<GUI::GroupBox>("Cursor");
    cursor_group_box.set_layout<GUI::VerticalBoxLayout>();
    cursor_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    cursor_group_box.layout()->set_margins({ 4, 12, 4, 4 });

    auto& radio_cursor_none = cursor_group_box.add<GUI::RadioButton>("None");
    radio_cursor_none.set_checked(true);
    radio_cursor_none.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::None);
    };
    auto& radio_cursor_arrow = cursor_group_box.add<GUI::RadioButton>("Arrow");
    radio_cursor_arrow.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Arrow);
    };
    auto& radio_cursor_i_beam = cursor_group_box.add<GUI::RadioButton>("IBeam");
    radio_cursor_i_beam.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::IBeam);
    };
    auto& radio_cursor_resize_horizontal = cursor_group_box.add<GUI::RadioButton>("ResizeHorizontal");
    radio_cursor_resize_horizontal.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeHorizontal);
    };
    auto& radio_cursor_resize_vertical = cursor_group_box.add<GUI::RadioButton>("ResizeVertical");
    radio_cursor_resize_vertical.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeVertical);
    };
    auto& radio_cursor_resize_diagonal_tlbr = cursor_group_box.add<GUI::RadioButton>("ResizeDiagonalTLBR");
    radio_cursor_resize_diagonal_tlbr.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeDiagonalTLBR);
    };
    auto& radio_cursor_resize_diagonal_bltr = cursor_group_box.add<GUI::RadioButton>("ResizeDiagonalBLTR");
    radio_cursor_resize_diagonal_bltr.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeDiagonalBLTR);
    };
    auto& radio_cursor_resize_column = cursor_group_box.add<GUI::RadioButton>("ResizeColumn");
    radio_cursor_resize_column.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeColumn);
    };
    auto& radio_cursor_resize_row = cursor_group_box.add<GUI::RadioButton>("ResizeRow");
    radio_cursor_resize_row.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::ResizeRow);
    };
    auto& radio_cursor_hand = cursor_group_box.add<GUI::RadioButton>("Hand");
    radio_cursor_hand.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Hand);
    };
    auto& radio_cursor_help = cursor_group_box.add<GUI::RadioButton>("Help");
    radio_cursor_help.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Help);
    };
    auto& radio_cursor_drag = cursor_group_box.add<GUI::RadioButton>("Drag");
    radio_cursor_drag.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Drag);
    };
    auto& radio_cursor_move = cursor_group_box.add<GUI::RadioButton>("Move");
    radio_cursor_move.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Move);
    };
    auto& radio_cursor_wait = cursor_group_box.add<GUI::RadioButton>("Wait");
    radio_cursor_wait.on_checked = [&](bool) {
        window->set_cursor(Gfx::StandardCursor::Wait);
    };

    app->set_menubar(move(menubar));

    window->show();

    return app->exec();
}
