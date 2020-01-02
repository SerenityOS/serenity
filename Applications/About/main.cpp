#include <AK/FileSystemPath.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CFile.h>
#include <LibCore/CIODevice.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GWindow.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibMarkdown/MDDocument.h>
#include <sys/utsname.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("About SerenityOS");
    Rect window_rect { 0, 0, 570, 500 };
    window_rect.center_within(GDesktop::the().rect());
    window->set_rect(window_rect);

    auto widget = GWidget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 0, 8, 0, 8 });
    widget->layout()->set_spacing(8);

    auto splitter = GSplitter::construct(Orientation::Horizontal, widget);
    auto html_view = HtmlView::construct(splitter);
    html_view->set_scrollbars_enabled(true);

    auto icon_label = GLabel::construct(widget);
    icon_label->set_icon(GraphicsBitmap::load_from_file("/res/icons/serenity.png"));
    icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    icon_label->set_preferred_size(icon_label->icon()->size());

    auto label = GLabel::construct(widget);
    label->set_font(Font::default_bold_font());
    label->set_text("SerenityOS");
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

    auto open_changelog = [&](const String& path) {
        if (path.is_null()) {
            html_view->set_document(nullptr);
            return;
        }

        dbg() << "Opening changelog at " << path;
        auto file = CFile::construct();
        file->set_filename(path);

        if (!file->open(CIODevice::OpenMode::ReadOnly)) {
            int saved_errno = errno;
            GMessageBox::show(strerror(saved_errno), "Failed to open changelog", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            return;
        }
        auto buffer = file->read_all();
        StringView source { (const char*)buffer.data(), (size_t)buffer.size() };

        MDDocument md_document;
        bool success = md_document.parse(source);
        ASSERT(success);

        String html = md_document.render_to_html();
        auto html_document = parse_html_document(html);
        html_view->set_document(html_document);
    };

    auto it = CDirIterator("/res/changelogs/", CDirIterator::Flags::SkipDots);
    if (it.has_error())
        ASSERT_NOT_REACHED();

    bool found = false;
    while (it.has_next()) {
        auto filename = it.next_path();
        if (filename.ends_with("current.md")) {
            auto path = String::format("%s/%s", "/res/changelogs", filename.characters());
            found = true;
            open_changelog(path);
            break;
        }
    }
    ASSERT(found);

    window->set_main_widget(widget);
    window->show();
    return app.exec();
}
