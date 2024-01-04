/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibMain/Main.h>

class MandelbrotSet {
public:
    MandelbrotSet()
    {
        set_view();
    }

    void reset()
    {
        set_view();
        correct_aspect();
    }

    void resize(Gfx::IntSize size)
    {
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, size).release_value_but_fixme_should_propagate_errors();
        correct_aspect();
    }

    void zoom(Gfx::IntRect const& rect)
    {
        set_view(
            rect.left() * (m_x_end - m_x_start) / m_bitmap->width() + m_x_start,
            (rect.right() - 1) * (m_x_end - m_x_start) / m_bitmap->width() + m_x_start,
            rect.top() * (m_y_end - m_y_start) / m_bitmap->height() + m_y_start,
            (rect.bottom() - 1) * (m_y_end - m_y_start) / m_bitmap->height() + m_y_start);
        correct_aspect();
    }

    void pan_by(Gfx::IntPoint delta)
    {
        auto relative_width_pixel = (m_x_end - m_x_start) / m_bitmap->width();
        auto relative_height_pixel = (m_y_end - m_y_start) / m_bitmap->height();

        set_view(
            m_x_start - delta.x() * relative_width_pixel,
            m_x_end - delta.x() * relative_width_pixel,
            m_y_start - delta.y() * relative_height_pixel,
            m_y_end - delta.y() * relative_height_pixel);

        Gfx::IntRect horizontal_missing, vertical_missing;
        if (delta.y() >= 0) {
            horizontal_missing = { 0, 0, m_bitmap->width(), delta.y() };
        } else {
            horizontal_missing = { 0, m_bitmap->height() + delta.y(), m_bitmap->width(), -delta.y() };
        }

        if (delta.x() >= 0) {
            vertical_missing = { 0, 0, delta.x(), m_bitmap->height() };
        } else {
            vertical_missing = { m_bitmap->width() + delta.x(), 0, -delta.x(), m_bitmap->height() };
        }

        move_contents_by(delta);
        calculate_rect(horizontal_missing);
        calculate_rect(vertical_missing);
    }

    double mandelbrot(double px, double py, i32 max_iterations)
    {
        // Based on https://en.wikipedia.org/wiki/Plotting_algorithms_for_the_Mandelbrot_set
        double const x0 = px * (m_x_end - m_x_start) / m_bitmap->width() + m_x_start;
        double const y0 = py * (m_y_end - m_y_start) / m_bitmap->height() + m_y_start;
        double x = 0;
        double y = 0;
        i32 iteration = 0;
        double x2 = 0;
        double y2 = 0;

        while (x2 + y2 <= 4 && iteration < max_iterations) {
            y = 2 * x * y + y0;
            x = x2 - y2 + x0;
            x2 = x * x;
            y2 = y * y;
            iteration++;
        }

        if (iteration == max_iterations)
            return iteration;

        // Renormalized fractional iteration count from https://linas.org/art-gallery/escape/escape.html
        // mu = n + 1 - log( log |Z(n)| ) / log(2)
        auto lz = log(sqrt(x2 + y2));
        auto mu = iteration + 1 - log(lz) / log(2);
        if (mu < 0)
            mu = 0;
        return mu;
    }

    static double linear_interpolate(double v0, double v1, double t)
    {
        return v0 + t * (v1 - v0);
    }

    void calculate_pixel(int px, int py, int max_iterations)
    {
        auto iterations = mandelbrot(px, py, max_iterations);
        auto whole_iterations = floor(iterations);
        auto partial_iterations = fmod(iterations, 1);
        double hue1 = whole_iterations * 360.0 / max_iterations;
        if (hue1 >= 360.0)
            hue1 = 0.0;
        double hue2 = (whole_iterations + 1) * 360.0 / max_iterations;
        if (hue2 >= 360.0)
            hue2 = 0.0;
        double hue = linear_interpolate(hue1, hue2, partial_iterations);
        double saturation = 1.0;
        double value = iterations < max_iterations ? 1.0 : 0;
        m_bitmap->set_pixel(px, py, Color::from_hsv(hue, saturation, value));
    }

    void calculate(int max_iterations = 100)
    {
        VERIFY(m_bitmap);

        calculate_rect(m_bitmap->rect(), max_iterations);
    }

    void calculate_rect(Gfx::IntRect const& rect, int max_iterations = 100)
    {
        VERIFY(m_bitmap);

        if (rect.is_empty())
            return;

        for (int py = rect.top(); py < rect.bottom(); py++)
            for (int px = rect.left(); px < rect.right(); px++)
                calculate_pixel(px, py, max_iterations);
    }

    Gfx::Bitmap const& bitmap() const
    {
        return *m_bitmap;
    }

private:
    double m_x_start { 0 };
    double m_x_end { 0 };
    double m_y_start { 0 };
    double m_y_end { 0 };
    RefPtr<Gfx::Bitmap> m_bitmap;

    void set_view(double x_start = -2.5, double x_end = 1.0, double y_start = -1.75, double y_end = 1.75)
    {
        m_x_start = x_start;
        m_x_end = x_end;
        m_y_start = y_start;
        m_y_end = y_end;
    }

    void correct_aspect()
    {
        auto y_mid = m_y_start + (m_y_end - m_y_start) / 2;
        auto aspect_corrected_y_length = (m_x_end - m_x_start) * m_bitmap->height() / m_bitmap->width();
        m_y_start = y_mid - aspect_corrected_y_length / 2;
        m_y_end = y_mid + aspect_corrected_y_length / 2;
    }

    void move_contents_by(Gfx::IntPoint delta)
    {
        // If we're moving down we paint upwards, else we paint downwards, to
        // avoid overwriting.
        if (delta.y() >= 0) {
            for (int row = m_bitmap->physical_height() - 1; row >= delta.y(); row--)
                move_row(row - delta.y(), row, delta.x());
        } else {
            for (int row = 0; row < m_bitmap->physical_height() + delta.y(); row++)
                move_row(row - delta.y(), row, delta.x());
        }
    }

    void move_row(int from, int to, int x_delta)
    {
        // If we're moving right we paint RTL, else we paint LTR, to avoid
        // overwriting.
        if (x_delta >= 0) {
            for (int column = m_bitmap->physical_width() - 1; column >= x_delta; column--) {
                m_bitmap->set_pixel(column, to, m_bitmap->get_pixel(column - x_delta, from));
            }
        } else {
            for (int column = 0; column < m_bitmap->physical_width() + x_delta; column++) {
                m_bitmap->set_pixel(column, to, m_bitmap->get_pixel(column - x_delta, from));
            }
        }
    }
};

enum class ImageType {
    BMP,
    PNG,
    QOI
};

class Mandelbrot : public GUI::Frame {
    C_OBJECT(Mandelbrot)

    ErrorOr<void> export_image(Core::File& export_path, ImageType image_type);

    enum class Zoom {
        In,
        Out,
    };
    void zoom(Zoom in_out, Gfx::IntPoint center);

    void reset();

private:
    Mandelbrot() = default;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent& event) override;
    virtual void keyup_event(GUI::KeyEvent& event) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;
    virtual void mousewheel_event(GUI::MouseEvent& event) override;
    virtual void resize_event(GUI::ResizeEvent& event) override;

    bool m_dragging { false };
    Gfx::IntPoint m_selection_start;
    Gfx::IntPoint m_selection_end;

    bool m_panning { false };
    Gfx::IntPoint m_last_pan_position;

    MandelbrotSet m_set;
};

void Mandelbrot::zoom(Zoom in_out, Gfx::IntPoint center)
{
    static constexpr double zoom_in_multiplier = 0.8;
    static constexpr double zoom_out_multiplier = 1.25;

    bool zooming_in = in_out == Zoom::In;
    double multiplier = zooming_in ? zoom_in_multiplier : zoom_out_multiplier;

    auto relative_rect = this->relative_rect();
    Gfx::IntRect zoomed_rect = relative_rect;

    zoomed_rect.set_width(zoomed_rect.width() * multiplier);
    zoomed_rect.set_height(zoomed_rect.height() * multiplier);

    auto leftover_width = abs(relative_rect.width() - zoomed_rect.width());
    auto leftover_height = abs(relative_rect.height() - zoomed_rect.height());

    double cursor_x_percentage = static_cast<double>(center.x()) / relative_rect.width();
    double cursor_y_percentage = static_cast<double>(center.y()) / relative_rect.height();

    zoomed_rect.set_x((zooming_in ? 1 : -1) * leftover_width * cursor_x_percentage);
    zoomed_rect.set_y((zooming_in ? 1 : -1) * leftover_height * cursor_y_percentage);

    m_set.zoom(zoomed_rect);
    update();
}

void Mandelbrot::reset()
{
    m_set.reset();
    update();
}

void Mandelbrot::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    if (!m_dragging && !m_panning)
        m_set.calculate();

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), m_set.bitmap(), m_set.bitmap().rect());

    if (m_dragging)
        painter.draw_rect(Gfx::IntRect::from_two_points(m_selection_start, m_selection_end), Color::Blue);
}

void Mandelbrot::keydown_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Left:
        m_set.pan_by(Gfx::IntPoint { 10, 0 });
        break;
    case KeyCode::Key_Right:
        m_set.pan_by(Gfx::IntPoint { -10, 0 });
        break;
    case KeyCode::Key_Up:
        m_set.pan_by(Gfx::IntPoint { 0, 10 });
        break;
    case KeyCode::Key_Down:
        m_set.pan_by(Gfx::IntPoint { 0, -10 });
        break;
    default:
        GUI::Widget::keydown_event(event);
        return;
    }

    m_panning = true;
    update();
}

void Mandelbrot::keyup_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Left:
    case KeyCode::Key_Right:
    case KeyCode::Key_Up:
    case KeyCode::Key_Down:
        m_panning = false;
        break;
    default:
        GUI::Widget::keydown_event(event);
    }
}

void Mandelbrot::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (!m_dragging) {
            m_selection_start = event.position();
            m_selection_end = event.position();
            m_dragging = true;
        }
    } else if (event.button() == GUI::MouseButton::Middle) {
        if (!m_panning) {
            m_last_pan_position = event.position();
            m_panning = true;
        }
    }

    return GUI::Widget::mousedown_event(event);
}

void Mandelbrot::mousemove_event(GUI::MouseEvent& event)
{
    if (m_dragging) {
        // Maintain aspect ratio
        int selection_width = event.position().x() - m_selection_start.x();
        int selection_height = event.position().y() - m_selection_start.y();
        int aspect_corrected_selection_width = selection_height * width() / height();
        int aspect_corrected_selection_height = selection_width * height() / width();
        if (selection_width * aspect_corrected_selection_height > aspect_corrected_selection_width * selection_height)
            m_selection_end = { event.position().x(), m_selection_start.y() + abs(aspect_corrected_selection_height) * ((selection_height < 0) ? -1 : 1) };
        else
            m_selection_end = { m_selection_start.x() + abs(aspect_corrected_selection_width) * ((selection_width < 0) ? -1 : 1), event.position().y() };
        update();
    }

    if (m_panning) {
        m_set.pan_by(event.position() - m_last_pan_position);
        m_last_pan_position = event.position();

        update();
    }

    return GUI::Widget::mousemove_event(event);
}

void Mandelbrot::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        auto selection = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
        if (selection.width() > 0 && selection.height() > 0) {
            m_set.zoom(selection);
            update();
        }
        m_dragging = false;
    } else if (event.button() == GUI::MouseButton::Middle) {
        m_panning = false;
        update();
    } else if (event.button() == GUI::MouseButton::Secondary) {
        reset();
    }

    return GUI::Widget::mouseup_event(event);
}

void Mandelbrot::mousewheel_event(GUI::MouseEvent& event)
{
    zoom(event.wheel_delta_y() < 0 ? Zoom::In : Zoom::Out, event.position());
}

void Mandelbrot::resize_event(GUI::ResizeEvent& event)
{
    m_set.resize(event.size());
}

ErrorOr<void> Mandelbrot::export_image(Core::File& export_file, ImageType image_type)
{
    m_set.resize(Gfx::IntSize { 1920, 1080 });
    m_set.calculate();
    ByteBuffer encoded_data;
    switch (image_type) {
    case ImageType::BMP:
        encoded_data = TRY(Gfx::BMPWriter::encode(m_set.bitmap()));
        break;
    case ImageType::PNG:
        encoded_data = TRY(Gfx::PNGWriter::encode(m_set.bitmap()));
        break;
    case ImageType::QOI:
        encoded_data = TRY(Gfx::QOIWriter::encode(m_set.bitmap()));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    m_set.resize(size());

    TRY(export_file.write_until_depleted(encoded_data));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath unix wpath cpath"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(false);
    window->set_title("Mandelbrot");
    window->set_obey_widget_min_size(false);
    window->set_minimum_size(320, 240);
    window->resize(window->minimum_size() * 2);
    auto mandelbrot = window->set_main_widget<Mandelbrot>();

    auto file_menu = window->add_menu("&File"_string);

    auto export_submenu = file_menu->add_submenu("&Export"_string);

    auto const save_image = [&](ImageType type, StringView extension) {
        auto const export_path = FileSystemAccessClient::Client::the().save_file(window, "mandelbrot"sv, extension);

        if (export_path.is_error())
            return;

        if (auto result = mandelbrot->export_image(export_path.value().stream(), type); result.is_error())
            GUI::MessageBox::show_error(window, ByteString::formatted("{}", result.error()));
    };

    export_submenu->add_action(GUI::Action::create("As &BMP...",
        [&](GUI::Action&) {
            save_image(ImageType::BMP, ".bmp"sv);
        }));
    export_submenu->add_action(GUI::Action::create("As &PNG...", { Mod_Ctrl | Mod_Shift, Key_S },
        [&](GUI::Action&) {
            save_image(ImageType::PNG, ".png"sv);
        }));
    export_submenu->add_action(GUI::Action::create("As &QOI...",
        [&](GUI::Action&) {
            save_image(ImageType::QOI, ".qoi"sv);
        }));

    export_submenu->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"sv)));

    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            mandelbrot->zoom(Mandelbrot::Zoom::In, mandelbrot->relative_rect().center());
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            // FIXME: Ideally, this would only reset zoom. Currently, it resets pan too.
            mandelbrot->reset();
        },
        window);

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            mandelbrot->zoom(Mandelbrot::Zoom::Out, mandelbrot->relative_rect().center());
        },
        window);

    auto app_icon = GUI::Icon::default_icon("app-mandelbrot"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(zoom_in_action);
    view_menu->add_action(reset_zoom_action);
    view_menu->add_action(zoom_out_action);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Mandelbrot Demo"_string, app_icon, window));

    window->show();
    window->set_cursor(Gfx::StandardCursor::Zoom);
    return app->exec();
}
