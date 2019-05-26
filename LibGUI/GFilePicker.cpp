#include <LibGUI/GFilePicker.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GDirectoryModel.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GSortingProxyModel.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GMessageBox.h>
#include <AK/FileSystemPath.h>

GFilePicker::GFilePicker(const String& path, CObject* parent)
    : GDialog(parent)
    , m_model(GDirectoryModel::create())
{
    set_title("GFilePicker");
    set_rect(200, 200, 400, 300);
    set_main_widget(new GWidget);
    main_widget()->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget()->layout()->set_margins({ 4, 4, 4, 4 });
    main_widget()->layout()->set_spacing(4);
    main_widget()->set_fill_with_background_color(true);
    main_widget()->set_background_color(Color::LightGray);

    auto* upper_container = new GWidget(main_widget());
    upper_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    upper_container->layout()->set_spacing(4);
    upper_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    upper_container->set_preferred_size({ 0, 26 });

    auto* toolbar = new GToolBar(upper_container);
    toolbar->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    toolbar->set_preferred_size({ 60, 0 });
    toolbar->set_has_frame(false);

    auto* location_textbox = new GTextBox(upper_container);
    location_textbox->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    location_textbox->set_preferred_size({ 0, 20 });

    m_view = new GTableView(main_widget());
    m_view->set_model(GSortingProxyModel::create(*m_model));
    m_view->set_column_hidden(GDirectoryModel::Column::Owner, true);
    m_view->set_column_hidden(GDirectoryModel::Column::Group, true);
    m_view->set_column_hidden(GDirectoryModel::Column::Permissions, true);
    m_view->set_column_hidden(GDirectoryModel::Column::Inode, true);
    m_model->open(path);

    location_textbox->on_return_pressed = [&] {
        m_model->open(location_textbox->text());
    };

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [this] (const GAction&) {
        m_model->open(String::format("%s/..", m_model->path().characters()));
    });
    toolbar->add_action(*open_parent_directory_action);

    auto mkdir_action = GAction::create("New directory...", GraphicsBitmap::load_from_file("/res/icons/16x16/mkdir.png"), [this] (const GAction&) {
        GInputBox input_box("Enter name:", "New directory", this);
        if (input_box.exec() == GInputBox::ExecOK && !input_box.text_value().is_empty()) {
            auto new_dir_path = FileSystemPath(String::format("%s/%s",
                m_model->path().characters(),
                input_box.text_value().characters()
            )).string();
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                GMessageBox::show(String::format("mkdir(\"%s\") failed: %s", new_dir_path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, this);
            } else {
                m_model->update();
            }
        }
    });
    toolbar->add_action(*mkdir_action);

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

    m_view->on_activation = [this, filename_textbox] (auto& index) {
        auto& filter_model = (GSortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_target(index);
        const GDirectoryModel::Entry& entry = m_model->entry(local_index.row());

        FileSystemPath path(String::format("%s/%s", m_model->path().characters(), entry.name.characters()));

        if (entry.is_directory()) {
            m_model->open(path.string());
            // NOTE: 'entry' is invalid from here on
        } else {
            filename_textbox->set_text(entry.name);
        }
    };

    auto* button_container = new GWidget(lower_container);
    button_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container->set_preferred_size({ 0, 20 });
    button_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container->layout()->set_spacing(4);
    button_container->layout()->add_spacer();

    auto* cancel_button = new GButton(button_container);
    cancel_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    cancel_button->set_preferred_size({ 80, 0 });
    cancel_button->set_text("Cancel");
    cancel_button->on_click = [this] (auto&) {
        done(ExecCancel);
    };

    auto* ok_button = new GButton(button_container);
    ok_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    ok_button->set_preferred_size({ 80, 0 });
    ok_button->set_text("OK");
    ok_button->on_click = [this, filename_textbox] (auto&) {
        FileSystemPath path(String::format("%s/%s", m_model->path().characters(), filename_textbox->text().characters()));
        m_selected_file = path;
        done(ExecOK);
    };
}

GFilePicker::~GFilePicker()
{
}
