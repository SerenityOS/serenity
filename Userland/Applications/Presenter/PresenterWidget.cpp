/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterWidget.h"
#include "LibGUI/MessageBox.h"
#include "Presentation.h"
#include <AK/Format.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>

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
    auto about_action = GUI::CommonActions::make_about_action("Presenter", GUI::Icon::default_icon("app-display-settings"sv));

    TRY(file_menu.try_add_action(open_action));
    TRY(file_menu.try_add_action(about_action));

    auto& presentation_menu = window->add_menu("&Presentation");
    auto next_slide_action = GUI::Action::create("&Next", { KeyCode::Key_Right }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv)), [this](auto&) {
        if (m_current_presentation) {
            m_current_presentation->next_frame();
            outln("Switched forward to slide {} frame {}", m_current_presentation->current_slide_number(), m_current_presentation->current_frame_in_slide_number());
            update();
        }
    });
    auto previous_slide_action = GUI::Action::create("&Previous", { KeyCode::Key_Left }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png"sv)), [this](auto&) {
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

    TRY(presentation_menu.try_add_action(GUI::Action::create("&Full Screen", { KeyModifier::Mod_Shift, KeyCode::Key_F5 }, { KeyCode::Key_F11 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/fullscreen.png"sv)), [this](auto&) {
        this->window()->set_fullscreen(true);
    })));
    TRY(presentation_menu.try_add_action(GUI::Action::create("Present From First &Slide", { KeyCode::Key_F5 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv)), [this](auto&) {
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

void PresenterWidget::drag_enter_event(GUI::DragEvent& event)
{
    auto const& mime_types = event.mime_types();
    if (mime_types.contains_slow("text/uri-list"))
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
        set_file(urls.first().path());
    }
}
