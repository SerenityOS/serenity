/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterWidget.h"
#include "Presentation.h"
#include "PresenterSettings.h"
#include <AK/Format.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Painter.h>

PresenterWidget::PresenterWidget()
{
    set_min_size(100, 100);
}

ErrorOr<void> PresenterWidget::initialize_menubar()
{
    auto* window = this->window();
    // Set up the menu bar.
    auto& file_menu = window->add_menu("&File");
    auto open_action = GUI::CommonActions::make_open_action([this](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(this->window());
        if (response.is_error())
            return;
        this->set_file(response.value()->filename());
    });

    m_settings_window = TRY(GUI::SettingsWindow::create("Presenter Settings"));
    (void)TRY(m_settings_window->add_tab<PresenterSettingsFooterWidget>("Footer", "footer"sv));
    auto settings_action = GUI::Action::create("&Settings...", [this](auto&) {
        m_settings_window->show();
    });
    auto about_action = GUI::CommonActions::make_about_action("Presenter", GUI::Icon::default_icon("app-display-settings"sv));

    TRY(file_menu.try_add_action(open_action));
    TRY(file_menu.try_add_action(settings_action));
    TRY(file_menu.try_add_action(about_action));

    auto& presentation_menu = window->add_menu("&Presentation");
    auto next_slide_action = GUI::Action::create("&Next", { KeyCode::Key_Right }, [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->next_frame();
            outln("Switched forward to slide {} frame {}", m_current_presentation->current_slide_number(), m_current_presentation->current_frame_in_slide_number());
            update();
        }
    });
    auto previous_slide_action = GUI::Action::create("&Previous", { KeyCode::Key_Left }, [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->previous_frame();
            outln("Switched backward to slide {} frame {}", m_current_presentation->current_slide_number(), m_current_presentation->current_frame_in_slide_number());
            update();
        }
    });
    TRY(presentation_menu.try_add_action(next_slide_action));
    TRY(presentation_menu.try_add_action(previous_slide_action));
    m_next_slide_action = next_slide_action;
    m_previous_slide_action = previous_slide_action;

    auto export_slides_action = GUI::Action::create("&Export Slides...", { KeyModifier::Mod_Ctrl, KeyCode::Key_E }, [this](auto&) { this->on_export_slides_action(); });
    TRY(presentation_menu.try_add_action(export_slides_action));

    TRY(presentation_menu.try_add_action(GUI::Action::create("&Full Screen", { KeyModifier::Mod_Shift, KeyCode::Key_F5 }, { KeyCode::Key_F11 }, [this](auto&) {
        this->window()->set_fullscreen(true);
    })));
    TRY(presentation_menu.try_add_action(GUI::Action::create("Present From First &Slide", { KeyCode::Key_F5 }, [this](auto&) {
        if (m_current_presentation)
            m_current_presentation->go_to_first_slide();
        this->window()->set_fullscreen(true);
    })));

    return {};
}

void PresenterWidget::set_file(StringView file_name)
{
    auto presentation = Presentation::load_from_file(file_name, *window());
    if (presentation.is_error()) {
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("The presentation \"{}\" could not be loaded.\n{}", file_name, presentation.error()));
    } else {
        m_current_presentation = presentation.release_value();
        window()->set_title(DeprecatedString::formatted(title_template, m_current_presentation->title(), m_current_presentation->author()));
        set_min_size(m_current_presentation->normative_size());
        // This will apply the new minimum size.
        update();
    }
}

void PresenterWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && window()->is_fullscreen())
        window()->set_fullscreen(false);

    // Alternate shortcuts for forward and backward
    switch (event.key()) {
    case Key_Down:
    case Key_PageDown:
    case Key_Space:
    case Key_N:
    case Key_Return:
        m_next_slide_action->activate();
        event.accept();
        break;
    case Key_Up:
    case Key_Backspace:
    case Key_PageUp:
    case Key_P:
        m_previous_slide_action->activate();
        event.accept();
        break;
    default:
        event.ignore();
        break;
    }
}

void PresenterWidget::paint_event([[maybe_unused]] GUI::PaintEvent& event)
{
    if (!m_current_presentation)
        return;
    auto normative_size = m_current_presentation->normative_size();
    // Choose an aspect-correct size which doesn't exceed actual widget dimensions.
    auto width_corresponding_to_height = height() * normative_size.aspect_ratio();
    auto dimension_to_preserve = (width_corresponding_to_height > width()) ? Orientation::Horizontal : Orientation::Vertical;
    auto display_size = size().match_aspect_ratio(normative_size.aspect_ratio(), dimension_to_preserve);

    GUI::Painter painter { *this };
    auto clip_rect = Gfx::IntRect::centered_at({ width() / 2, height() / 2 }, display_size);
    painter.clear_clip_rect();
    // FIXME: This currently leaves a black border when the window aspect ratio doesn't match.
    // Figure out a way to apply the background color here as well.
    painter.add_clip_rect(clip_rect);

    m_current_presentation->paint(painter);
}

void PresenterWidget::on_export_slides_action()
{
    if (!m_current_presentation)
        return;
    auto maybe_path = GUI::FilePicker::get_save_filepath(this->window(), DeprecatedString::formatted("{}-export", m_current_presentation->title()), "png");
    if (!maybe_path.has_value())
        return;
    auto path = LexicalPath { maybe_path.release_value() };
    auto prefix = LexicalPath::join(path.dirname(), path.title());
    auto slides = m_current_presentation->slides();

    // FIXME: Allow the user to change the export size; we just make sure it's above 1000px wide and high.
    auto size = m_current_presentation->normative_size();
    VERIFY(size.width() > 0 && size.height() > 0);
    auto display_scale = 1;
    while (size.height() < 1000 || size.width() < 1000) {
        display_scale *= 2;
        size.scale_by(2);
    }

    auto maybe_slide_image = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size);
    if (maybe_slide_image.is_error())
        return;
    auto slide_image = maybe_slide_image.release_value();

    Core::EventLoop progress_event_loop;

    m_export_done.store(false);
    m_exporter = Threading::BackgroundAction<int>::construct([=, this](auto&) {
        auto painter = Gfx::Painter { slide_image };
        for (size_t i = 0; i < slides.size(); ++i) {
            m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Starting }; m_export_state_updated.broadcast(); });
            auto const& slide = slides[i];
            for (size_t frame = 0; frame < slide.frame_count(); ++frame) {
                m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Starting }; m_export_state_updated.broadcast(); });
                // FIXME: Should not be necessary, the slide should paint a background instead.
                painter.clear_rect({ { 0, 0 }, size }, Gfx::Color::White);
                painter.clear_clip_rect();

                m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Rendering }; m_export_state_updated.broadcast(); });
                slide.paint(painter, frame, { display_scale, display_scale });

                m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Writing }; m_export_state_updated.broadcast(); });
                auto path = DeprecatedString::formatted("{}-{:03}-{:02}.png", prefix, i, frame);
                auto png = Gfx::PNGWriter::encode(slide_image);
                if (png.is_error()) {
                    // Do not store "done" here so that the progress window stays visible and displays the error until the user closes it.
                    m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Error }; m_export_state_updated.broadcast(); });
                    return 0;
                }

                auto file = Core::Stream::File::open(path, Core::Stream::OpenMode::Truncate | Core::Stream::OpenMode::Write);
                if (file.is_error()) {
                    dbgln("failed to write to path {}: {}", path, file.error());
                    m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Error }; m_export_state_updated.broadcast(); });
                    return 0;
                }

                auto result = file.value()->write_entire_buffer(png.value());                    
                if (result.is_error()) {
                    m_export_state.with_locked([&](auto& state) { state = { static_cast<unsigned>(i), SlideProgress::Error }; m_export_state_updated.broadcast(); });
                    return 0;
                }
            }
        }

        m_export_state_updated.broadcast();
        m_export_done.store(true);
        return 0; },
        nullptr);

    auto maybe_progress_window = GUI::Window::try_create(this->window());
    // The exporter will still run, we just won't get feedback if we're OOMing here.
    if (maybe_progress_window.is_error())
        return;
    m_progress_window = maybe_progress_window.release_value();
    m_progress_window->set_title("Exporting Presentation");
    m_progress_window->set_minimizable(false);
    auto maybe_progress_bar = m_progress_window->try_set_main_widget<GUI::Progressbar>();
    if (maybe_progress_bar.is_error())
        return;
    m_progress_bar = maybe_progress_bar.release_value();
    m_progress_bar->set_format(GUI::Progressbar::Format::ValueSlashMax);
    m_progress_bar->set_min(1);
    m_progress_bar->set_max(static_cast<int>(slides.size()));
    m_progress_bar->set_min_size({ 300, 100 });
    m_progress_window->show();

    progress_event_loop.deferred_invoke([this] { update_export_status(); });

    progress_event_loop.spin_until([&] { return !m_progress_window->is_visible(); });
    m_export_done.store(true);
    update_export_status();
}

void PresenterWidget::update_export_status()
{
    if (!m_progress_window || !m_current_presentation)
        return;

    if (m_export_done.load()) {
        m_progress_window->hide();
        this->window()->set_progress({});
        return;
    }

    auto const status = m_export_state.with_locked([&](auto status) {
        // It is okay to return on spurious wakeups here, then we just update too often.
        m_export_state_updated.wait();
        return status;
    });
    m_progress_bar->set_value(status.current_slide + 1);
    this->window()->set_progress((status.current_slide * 100) / m_current_presentation->slides().size());

    if (status.progress == SlideProgress::Error) {
        // Don't hide the window so the user sees the error.
        m_progress_bar->set_text(DeprecatedString::formatted("Error while writing slide {}!", status.current_slide + 1));
        return;
    }

    auto const status_text = status.progress == SlideProgress::Starting ? "Starting "sv
        : status.progress == SlideProgress::Rendering                   ? "Rendering "sv
        : status.progress == SlideProgress::Writing                     ? "Writing "sv
                                                                        : ""sv;
    m_progress_bar->set_text(status_text);
    m_progress_bar->update();

    Core::EventLoop::current().deferred_invoke([&] { update_export_status(); });
}
