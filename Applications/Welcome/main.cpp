#include <AK/String.h>
#include <AK/Vector.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GStackWidget.h>
#include <LibGUI/GWindow.h>

#include "TextWidget.h"

extern const u8 _binary_background_png_start[];
extern const u8 _binary_background_png_size;

struct ContentPage {
    String menu_name;
    String title;
    Vector<String> content;
};

int main(int argc, char** argv)
{
    Vector<ContentPage> pages = {
        {
            "Welcome",
            "Welcome",
            {
                "Welcome to the exciting new world of Serenity, where the year is 1998 and the leading OS vendor has decided to merge their flagship product with a Unix-like kernel.",
                "Sit back and relax as you take a brief tour of the options available on this screen.",
                "If you want to explore an option, just click it.",
            },
        },
        {
            "Register Now",
            "Register Now!",
            {
                "Registering your copy of Serenity opens the doors to full integration of Serenity into your life, your being, and your soul.",
                "By registering Serenity, you enter into the draw to win a lifetime supply of milk, delivered fresh each day by a mystical horse wearing a full tuxedo.",
                "To register, simply write your contact details on a piece of paper and hold it up to your monitor.",
            },
        },
        {
            "Connect to the Internet",
            "Connect to the Internet",
            {
                "On the Internet, you can correspond through electronic mail (e-mail), get the latest news and financial information, and visit Web sites around the world, most of which will make you really angry.",
                "Serenity includes several internet applications, such as an IRC (Internet relay chat) client, 4chan browser, telnet server, and basic utilities like ping.",
                "Come chat with us today! How bad can it be?",
            },
        },
        {
            "Have fun",
            "Play Some Games!",
            {
                "Serenity includes several games built right into the base system. These include the classic game Snake and the anti-productivity mainstay Minesweeper.",
                "With a little extra effort, you can even play the original id Software hit DOOM, albeit without sound. No sound just means you won't alert your boss, so it's more of a feature than a limitation.",
            },
        },
    };

    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("Welcome to Serenity");
    Rect window_rect { 0, 0, 640, 360 };
    window_rect.center_within(GDesktop::the().rect());
    window->set_resizable(true);
    window->set_rect(window_rect);

    auto background = GLabel::construct();
    window->set_main_widget(background);
    background->set_fill_with_background_color(true);
    background->set_layout(make<GBoxLayout>(Orientation::Vertical));
    background->layout()->set_margins({ 8, 8, 8, 8 });
    background->layout()->set_spacing(8);
    background->set_icon(load_png_from_memory((const u8*)&_binary_background_png_start, (size_t)&_binary_background_png_size));
    background->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    background->set_preferred_size(background->icon()->size());

    //
    // header
    //

    auto header = GLabel::construct(background);
    header->set_font(Font::default_bold_font());
    header->set_text("Welcome to Serenity");
    header->set_text_alignment(TextAlignment::CenterLeft);
    header->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    header->set_preferred_size(0, 30);

    //
    // main section
    //

    auto main_section = GWidget::construct(background);
    main_section->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    main_section->layout()->set_margins({ 0, 0, 0, 0 });
    main_section->layout()->set_spacing(8);
    main_section->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

    auto menu = GWidget::construct(main_section);
    menu->set_layout(make<GBoxLayout>(Orientation::Vertical));
    menu->layout()->set_margins({ 0, 0, 0, 0 });
    menu->layout()->set_spacing(8);
    menu->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    menu->set_preferred_size(200, 0);

    auto* stack = new GStackWidget(main_section);
    stack->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

    for (auto& page : pages) {
        auto content = GWidget::construct(stack);
        content->set_layout(make<GBoxLayout>(Orientation::Vertical));
        content->layout()->set_margins({ 0, 0, 0, 0 });
        content->layout()->set_spacing(8);
        content->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

        auto content_title = GLabel::construct(content);
        content_title->set_font(Font::default_bold_font());
        content_title->set_text(page.title);
        content_title->set_text_alignment(TextAlignment::CenterLeft);
        content_title->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        content_title->set_preferred_size(0, 10);

        for (auto& paragraph : page.content) {
            auto* content_text = new TextWidget(content);
            content_text->set_font(Font::default_font());
            content_text->set_text(paragraph);
            content_text->set_text_alignment(TextAlignment::TopLeft);
            content_text->set_line_height(12);
            content_text->wrap_and_set_height();
        }

        auto menu_option = GButton::construct(menu);
        menu_option->set_font(Font::default_font());
        menu_option->set_text(page.menu_name);
        menu_option->set_text_alignment(TextAlignment::CenterLeft);
        menu_option->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        menu_option->set_preferred_size(0, 20);
        menu_option->on_click = [&](auto&) {
            stack->set_active_widget(content);
            content->invalidate_layout();
        };
    }

    window->show();
    return app.exec();
}
