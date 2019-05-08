#include <LibGUI/GFilePicker.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GDirectoryModel.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>

GFilePicker::GFilePicker(const String& path, CObject* parent)
    : GDialog(parent)
    , m_model(GDirectoryModel::create())
{
    set_title("GFilePicker");
    set_rect(200, 200, 400, 300);
    set_main_widget(new GWidget);
    main_widget()->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget()->layout()->set_margins({ 4, 0, 4, 0 });
    main_widget()->layout()->set_spacing(4);
    main_widget()->set_fill_with_background_color(true);
    main_widget()->set_background_color(Color::LightGray);
    m_view = new GTableView(main_widget());
    m_view->set_model(*m_model);
    model().open("/");

    auto* lower_container = new GWidget(main_widget());
    lower_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    lower_container->layout()->set_spacing(4);
    lower_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    lower_container->set_preferred_size({ 0, 60 });

    auto* filename_container = new GWidget(lower_container);
    filename_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    filename_container->set_preferred_size({ 0, 20 });
    filename_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto* filename_label = new GLabel("File name:", filename_container);
    filename_label->set_text_alignment(TextAlignment::CenterLeft);
    filename_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    filename_label->set_preferred_size({ 60, 0 });
    auto* filename_textbox = new GTextBox(filename_container);

    auto* button_container = new GWidget(lower_container);
    button_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container->set_preferred_size({ 0, 20 });
    button_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container->layout()->set_spacing(4);

    auto* cancel_button = new GButton(button_container);
    cancel_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    cancel_button->set_preferred_size({ 80, 0 });
    cancel_button->set_caption("Cancel");
    cancel_button->on_click = [this] (auto&) {
        done(ExecCancel);
    };

    auto* ok_button = new GButton(button_container);
    ok_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    ok_button->set_preferred_size({ 80, 0 });
    ok_button->set_caption("OK");
    ok_button->on_click = [this] (auto&) {
        done(ExecOK);
    };
}

GFilePicker::~GFilePicker()
{
}
