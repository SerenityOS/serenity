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
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SettingsWindow.h>
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
    if (event.key() == Key_Escape && window()->is_fullscreen()) {
        window()->set_fullscreen(false);
        event.accept();
        return;
    }

    // Alternate shortcuts for forward and backward
    switch (event.key()) {
    case Key_Down:
    case Key_PageDown:
    case Key_Space:
    case Key_N:
        m_next_slide_action->activate();
        event.accept();
        break;
    case Key_Return:
        if (!m_current_key_sequence.is_empty()) {
            go_to_slide_from_key_sequence();
            m_current_key_sequence.clear();
            update();
        } else {
            m_next_slide_action->activate();
        }
        event.accept();
        break;
    case Key_Up:
    case Key_Backspace:
    case Key_PageUp:
    case Key_P:
        m_previous_slide_action->activate();
        event.accept();
        break;
    case Key_0:
    case Key_1:
    case Key_2:
    case Key_3:
    case Key_4:
    case Key_5:
    case Key_6:
    case Key_7:
    case Key_8:
    case Key_9:
        m_current_key_sequence.append(event.key());
        event.accept();
        break;
    default:
        event.ignore();
        break;
    }
}

void PresenterWidget::go_to_slide_from_key_sequence()
{
    VERIFY(!m_current_key_sequence.is_empty());
    unsigned human_slide_number = 0;
    for (auto key : m_current_key_sequence) {
        unsigned value = 0;
        switch (key) {
        case Key_0:
            value = 0;
            break;
        case Key_1:
            value = 1;
            break;
        case Key_2:
            value = 2;
            break;
        case Key_3:
            value = 3;
            break;
        case Key_4:
            value = 4;
            break;
        case Key_5:
            value = 5;
            break;
        case Key_6:
            value = 6;
            break;
        case Key_7:
            value = 7;
            break;
        case Key_8:
            value = 8;
            break;
        case Key_9:
            value = 9;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        human_slide_number = value + human_slide_number * 10;
    }
    // We explicitly do not want to VERIFY, return error or show an error message to the user. We will just ignore invalid slide jump key sequences.
    if (human_slide_number == 0)
        return;
    unsigned slide_index = human_slide_number - 1;
    if (slide_index >= m_current_presentation->total_slide_count())
        return;

    m_current_presentation->go_to_slide(slide_index);
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
