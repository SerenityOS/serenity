/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Optional.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/Process.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <unistd.h>

class SelectableLayover final : public GUI::Widget {
    C_OBJECT(SelectableLayover)
public:
    virtual ~SelectableLayover() override {};

    Gfx::IntRect region() const
    {
        return m_region;
    }

private:
    SelectableLayover(GUI::Window* window)
        : m_window(window)
        , m_background_color(palette().threed_highlight().with_alpha(128))
    {
        set_override_cursor(Gfx::StandardCursor::Crosshair);
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Primary)
            m_anchor_point = event.position();
    }

    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        if (m_anchor_point.has_value()) {
            m_region = Gfx::IntRect::from_two_points(*m_anchor_point, event.position());
        }

        m_mouse_y = event.y();
        m_mouse_x = event.x();
        update();
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Primary)
            m_window->close();
    }

    virtual void paint_event(GUI::PaintEvent&) override
    {
        GUI::Painter painter(*this);
        painter.clear_rect(m_window->rect(), Gfx::Color::Transparent);
        painter.fill_rect(m_window->rect(), m_background_color);

        painter.draw_line(Gfx::IntPoint(0, m_mouse_y), Gfx::IntPoint(m_window->width(), m_mouse_y), Gfx::Color::Green);
        painter.draw_line(Gfx::IntPoint(m_mouse_x, 0), Gfx::IntPoint(m_mouse_x, m_window->height()), Gfx::Color::Green);
        if (m_region.is_empty())
            return;

        painter.clear_rect(m_region, Gfx::Color::Transparent);
    }

    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        if (event.key() == Key_Escape) {
            m_region = Gfx::IntRect();
            m_window->close();
        }
    }

    Optional<Gfx::IntPoint> m_anchor_point;
    Gfx::IntRect m_region;
    GUI::Window* m_window = nullptr;
    Gfx::Color const m_background_color;

    int m_mouse_y = 0;
    int m_mouse_x = 0;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    ByteString output_path;
    bool output_to_clipboard = false;
    unsigned delay = 0;
    bool select_region = false;
    bool edit_image = false;
    int screen = -1;

    args_parser.add_positional_argument(output_path, "Output filename", "output", Core::ArgsParser::Required::No);
    args_parser.add_option(output_to_clipboard, "Output to clipboard", "clipboard", 'c');
    args_parser.add_option(delay, "Seconds to wait before taking a screenshot", "delay", 'd', "seconds");
    args_parser.add_option(screen, "The index of the screen (default: -1 for all screens)", "screen", 's', "index");
    args_parser.add_option(select_region, "Select a region to capture", "region", 'r');
    args_parser.add_option(edit_image, "Open in PixelPaint", "edit", 'e');

    args_parser.parse(arguments);

    if (output_path.is_empty()) {
        output_path = Core::DateTime::now().to_byte_string("screenshot-%Y-%m-%d-%H-%M-%S.png"sv);
    }

    auto app = TRY(GUI::Application::create(arguments));
    Optional<Gfx::IntRect> crop_region;
    if (select_region) {
        auto window = GUI::Window::construct();
        auto container = window->set_main_widget<SelectableLayover>(window);

        window->set_title("shot");
        window->set_window_type(GUI::WindowType::Popup);
        window->set_has_alpha_channel(true);
        window->set_fullscreen(true);
        window->show();
        app->exec();

        crop_region = container->region();
        if (crop_region.value().is_empty()) {
            dbgln("cancelled...");
            return 0;
        }
    }

    sleep(delay);
    Optional<u32> screen_index;
    if (screen >= 0)
        screen_index = (u32)screen;
    dbgln("getting screenshot...");
    auto shared_bitmap = GUI::ConnectionToWindowServer::the().get_screen_bitmap(crop_region, screen_index);
    dbgln("got screenshot");

    RefPtr<Gfx::Bitmap> bitmap = shared_bitmap.bitmap();
    if (!bitmap) {
        warnln("Failed to grab screenshot");
        return 1;
    }

    if (output_to_clipboard) {
        GUI::Clipboard::the().set_bitmap(*bitmap);
        return 0;
    }

    auto encoded_bitmap_or_error = Gfx::PNGWriter::encode(*bitmap);
    if (encoded_bitmap_or_error.is_error()) {
        warnln("Failed to encode PNG");
        return 1;
    }
    auto encoded_bitmap = encoded_bitmap_or_error.release_value();

    if (edit_image)
        output_path = Core::DateTime::now().to_byte_string("/tmp/screenshot-%Y-%m-%d-%H-%M-%S.png"sv);

    auto file_or_error = Core::File::open(output_path, Core::File::OpenMode::Write);
    if (file_or_error.is_error()) {
        warnln("Could not open '{}' for writing: {}", output_path, file_or_error.error());
        return 1;
    }

    auto& file = *file_or_error.value();
    TRY(file.write_until_depleted(encoded_bitmap.bytes()));

    if (edit_image)
        TRY(Core::Process::spawn("/bin/PixelPaint"sv, Array { output_path }));

    bool printed_hyperlink = false;
    if (isatty(STDOUT_FILENO)) {
        auto full_path_or_error = FileSystem::real_path(output_path);
        if (!full_path_or_error.is_error()) {
            char hostname[HOST_NAME_MAX];
            VERIFY(gethostname(hostname, sizeof(hostname)) == 0);

            auto url = URL::create_with_file_scheme(full_path_or_error.value(), {}, hostname);
            out("\033]8;;{}\033\\", url.serialize());
            printed_hyperlink = true;
        }
    }

    out("{}", output_path);

    if (printed_hyperlink) {
        out("\033]8;;\033\\");
    }

    outln("");
    return 0;
}
