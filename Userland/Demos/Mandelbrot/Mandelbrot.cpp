/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/QOIWriter.h>
#include <LibMain/Main.h>
#include <unistd.h>

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
        calculate();
    }

    void resize(Gfx::IntSize const& size)
    {
        m_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, size).release_value_but_fixme_should_propagate_errors();
        correct_aspect();
        calculate();
    }

    void zoom(Gfx::IntRect const& rect)
    {
        set_view(
            rect.left() * (m_x_end - m_x_start) / m_bitmap->width() + m_x_start,
            rect.right() * (m_x_end - m_x_start) / m_bitmap->width() + m_x_start,
            rect.top() * (m_y_end - m_y_start) / m_bitmap->height() + m_y_start,
            rect.bottom() * (m_y_end - m_y_start) / m_bitmap->height() + m_y_start);
        correct_aspect();
        calculate();
    }

    void pan_by(Gfx::IntPoint const& delta)
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

        auto lz = sqrt(x * x + y * y) / 2;
        return 1 + iteration + log(lz / log(2)) / log(2);
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

        for (int py = rect.top(); py <= rect.bottom(); py++)
            for (int px = rect.left(); px <= rect.right(); px++)
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

    void move_contents_by(Gfx::IntPoint const& delta)
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

    void export_image(String const& export_path, ImageType image_type);

    enum class Zoom {
        In,
        Out,
    };
    void zoom(Zoom in_out, Gfx::IntPoint const& center);

    void reset();

private:
    Mandelbrot() = default;

    virtual void paint_event(GUI::PaintEvent&) override;
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

void Mandelbrot::zoom(Zoom in_out, Gfx::IntPoint const& center)
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

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), m_set.bitmap(), m_set.bitmap().rect());

    if (m_dragging)
        painter.draw_rect(Gfx::IntRect::from_two_points(m_selection_start, m_selection_end), Color::Blue);
}

void Mandelbrot::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (!m_dragging) {
            m_selection_start = event.position();
            m_selection_end = event.position();
            m_dragging = true;
            update();
        }
    } else if (event.button() == GUI::MouseButton::Middle) {
        if (!m_panning) {
            m_last_pan_position = event.position();
            m_panning = true;
            update();
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
        if (selection.width() > 0 && selection.height() > 0)
            m_set.zoom(selection);
        m_dragging = false;
        update();
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

void Mandelbrot::export_image(String const& export_path, ImageType image_type)
{
    m_set.resize(Gfx::IntSize { 1920, 1080 });
    ByteBuffer encoded_data;
    switch (image_type) {
    case ImageType::BMP: {
        Gfx::BMPWriter dumper;
        encoded_data = dumper.dump(m_set.bitmap());
        break;
    }
    case ImageType::PNG:
        encoded_data = Gfx::PNGWriter::encode(m_set.bitmap());
        break;
    case ImageType::QOI:
        encoded_data = Gfx::QOIWriter::encode(m_set.bitmap());
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    m_set.resize(size());
    auto file = fopen(export_path.characters(), "wb");
    if (!file) {
        GUI::MessageBox::show(window(), String::formatted("Could not open '{}' for writing.", export_path), "Mandelbrot", GUI::MessageBox::Type::Error);
        return;
    }
    fwrite(encoded_data.data(), 1, encoded_data.size(), file);
    fclose(file);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath wpath cpath"));

#if 0
    TRY(Core::System::unveil("/res", "r"));
    TRY(unveil(nullptr, nullptr));
#endif

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(false);
    window->set_title("Mandelbrot");
    window->set_minimum_size(320, 240);
    window->resize(window->minimum_size() * 2);
    auto mandelbrot = TRY(window->try_set_main_widget<Mandelbrot>());

    auto file_menu = TRY(window->try_add_menu("&File"));

    auto& export_submenu = file_menu->add_submenu("&Export");

    TRY(export_submenu.try_add_action(GUI::Action::create("As &BMP",
        [&](GUI::Action&) {
            Optional<String> export_path = GUI::FilePicker::get_save_filepath(window, "untitled", "bmp");
            if (!export_path.has_value())
                return;
            mandelbrot->export_image(export_path.value(), ImageType::BMP);
        })));
    TRY(export_submenu.try_add_action(GUI::Action::create("As &PNG", { Mod_Ctrl | Mod_Shift, Key_S },
        [&](GUI::Action&) {
            Optional<String> export_path = GUI::FilePicker::get_save_filepath(window, "untitled", "png");
            if (!export_path.has_value())
                return;
            mandelbrot->export_image(export_path.value(), ImageType::PNG);
        })));
    TRY(export_submenu.try_add_action(GUI::Action::create("As &QOI",
        [&](GUI::Action&) {
            Optional<String> export_path = GUI::FilePicker::get_save_filepath(window, "untitled", "qoi");
            if (!export_path.has_value())
                return;
            mandelbrot->export_image(export_path.value(), ImageType::QOI);
        })));

    export_submenu.set_icon(TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/save.png")));

    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

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

    auto app_icon = GUI::Icon::default_icon("app-mandelbrot");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(zoom_in_action));
    TRY(view_menu->try_add_action(reset_zoom_action));
    TRY(view_menu->try_add_action(zoom_out_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Mandelbrot Demo", app_icon, window)));

    window->show();
    window->set_cursor(Gfx::StandardCursor::Zoom);
    return app->exec();
}
