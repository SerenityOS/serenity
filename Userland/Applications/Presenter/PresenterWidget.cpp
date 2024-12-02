/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterWidget.h"
#include "Presentation.h"
#include <LibCore/MimeData.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>

PresenterWidget::PresenterWidget()
{
    set_min_size(200, 120);
    set_fill_with_background_color(true);
    m_web_view = add<WebView::OutOfProcessWebView>();
    m_web_view->set_frame_style(Gfx::FrameStyle::NoFrame);
    m_web_view->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_web_view->set_content_scales_to_viewport(true);
}

void PresenterWidget::resize_event(GUI::ResizeEvent& event)
{
    Widget::resize_event(event);

    if (!m_current_presentation)
        return;

    auto normative_size = m_current_presentation->normative_size().to_type<float>();
    float widget_ratio = static_cast<float>(event.size().width()) / static_cast<float>(event.size().height());
    float wh_ratio = normative_size.width() / normative_size.height();

    Gfx::IntRect rect;
    if (widget_ratio >= wh_ratio) {
        rect.set_width(static_cast<int>(ceilf(static_cast<float>(event.size().height()) * wh_ratio)));
        rect.set_height(event.size().height());
    } else {
        float hw_ratio = normative_size.height() / normative_size.width();
        rect.set_width(event.size().width());
        rect.set_height(static_cast<int>(ceilf(static_cast<float>(event.size().width()) * hw_ratio)));
    }
    m_web_view->set_relative_rect(rect.centered_within(this->rect()));
}

ErrorOr<void> PresenterWidget::initialize_menubar()
{
    auto* window = this->window();
    // Set up the menu bar.
    auto file_menu = window->add_menu("&File"_string);
    auto open_action = GUI::CommonActions::make_open_action([this](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .allowed_file_types = { { GUI::FileTypeFilter { "Presentation Files", { { "presenter" } } }, GUI::FileTypeFilter::all_files() } },
        };
        auto response = FileSystemAccessClient::Client::the().open_file(this->window(), options);
        if (response.is_error())
            return;
        this->set_file(response.value().filename());
    });
    file_menu->add_action(open_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto presentation_menu = window->add_menu("&Presentation"_string);
    m_next_slide_action = GUI::Action::create("&Next", { KeyCode::Key_Right }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)), [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->next_frame();
            update_web_view();
            update_slides_actions();
        }
    });
    m_previous_slide_action = GUI::Action::create("&Previous", { KeyCode::Key_Left }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv)), [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->previous_frame();
            update_web_view();
            update_slides_actions();
        }
    });
    m_present_from_first_slide_action = GUI::Action::create("Present from First &Slide", { KeyCode::Key_F5 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv)), [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->go_to_first_slide();
            update_web_view();
        }
        this->window()->set_fullscreen(true);
    });

    presentation_menu->add_action(*m_next_slide_action);
    presentation_menu->add_action(*m_previous_slide_action);
    presentation_menu->add_action(*m_present_from_first_slide_action);

    auto view_menu = window->add_menu("&View"_string);
    m_full_screen_action = GUI::Action::create("Toggle &Full Screen", { KeyModifier::Mod_Shift, KeyCode::Key_F5 }, { KeyCode::Key_F11 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/fullscreen.png"sv)), [this](auto&) {
        auto* window = this->window();
        window->set_fullscreen(!window->is_fullscreen());
    });
    m_resize_to_fit_content_action = GUI::Action::create("Resize to Fit &Content", { KeyModifier::Mod_Alt, KeyCode::Key_C }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/scale.png"sv)), [this](auto&) {
        if (m_current_presentation) {
            auto presentation_size = m_current_presentation->normative_size();
            auto* window = this->window();

            window->resize(window->size().match_aspect_ratio(presentation_size.aspect_ratio(), Orientation::Horizontal));
        }
    });

    view_menu->add_action(*m_full_screen_action);
    view_menu->add_action(*m_resize_to_fit_content_action);

    update_slides_actions();

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Presenter.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Presenter"_string, GUI::Icon::default_icon("app-presenter"sv)));

    return {};
}

void PresenterWidget::update_web_view()
{
    m_web_view->run_javascript(ByteString::formatted("goto({}, {})", m_current_presentation->current_slide_number(), m_current_presentation->current_frame_in_slide_number()));
}

void PresenterWidget::update_slides_actions()
{
    if (m_current_presentation) {
        m_next_slide_action->set_enabled(m_current_presentation->has_next_frame());
        m_previous_slide_action->set_enabled(m_current_presentation->has_previous_frame());
        m_full_screen_action->set_enabled(true);
        m_present_from_first_slide_action->set_enabled(true);
    } else {
        m_next_slide_action->set_enabled(false);
        m_previous_slide_action->set_enabled(false);
        m_full_screen_action->set_enabled(false);
        m_present_from_first_slide_action->set_enabled(false);
    }
}

void PresenterWidget::set_file(StringView file_name)
{
    auto presentation = Presentation::load_from_file(file_name);
    if (presentation.is_error()) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("The presentation \"{}\" could not be loaded.\n{}", file_name, presentation.error()));
    } else {
        m_current_presentation = presentation.release_value();
        window()->set_title(ByteString::formatted(title_template, m_current_presentation->title(), m_current_presentation->author()));
        set_min_size(m_current_presentation->normative_size());
        m_web_view->load_html(MUST(m_current_presentation->render()));
        update_slides_actions();
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

void PresenterWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), Gfx::Color::Black);
}

void PresenterWidget::second_paint_event(GUI::PaintEvent& event)
{
    if (!m_current_presentation)
        return;
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_text(m_web_view->relative_rect(), m_current_presentation->current_slide().title(), Gfx::TextAlignment::BottomCenter);
}

void PresenterWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void PresenterWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;

        window()->move_to_front();
        set_file(URL::percent_decode(urls.first().serialize_path()));
    }
}
