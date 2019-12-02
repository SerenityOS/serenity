#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWindow.h>
#include <sys/utsname.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("About Serenity");
    Rect window_rect { 0, 0, 240, 180 };
    window_rect.center_within(GDesktop::the().rect());
    window->set_resizable(false);
    window->set_rect(window_rect);

    auto widget = GWidget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 0, 8, 0, 8 });
    widget->layout()->set_spacing(8);

    auto icon_label = GLabel::construct(widget);
    icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/serenity.png"));
    icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    icon_label->set_preferred_size(icon_label->icon()->size());

    auto label = GLabel::construct(widget);
    label->set_font(Font::default_bold_font());
    label->set_text("Serenity Operating System");
    label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label->set_preferred_size(0, 11);

    utsname uts;
    int rc = uname(&uts);
    ASSERT(rc == 0);

    auto version_label = GLabel::construct(widget);
    version_label->set_text(String::format("Version %s", uts.release));
    version_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    version_label->set_preferred_size(0, 11);

    auto git_info_label = GLabel::construct(widget);
    git_info_label->set_text(String::format("Built on %s@%s", GIT_BRANCH, GIT_COMMIT));
    git_info_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    git_info_label->set_preferred_size(0, 11);

    auto git_changes_label = GLabel::construct(widget);
    git_changes_label->set_text(String::format("Changes: %s", GIT_CHANGES));
    git_changes_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    git_changes_label->set_preferred_size(0, 11);

    auto quit_button = GButton::construct(widget);
    quit_button->set_text("Okay");
    quit_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    quit_button->set_preferred_size(100, 20);
    quit_button->on_click = [](GButton&) {
        GApplication::the().quit(0);
    };

    window->show();
    return app.exec();
}
