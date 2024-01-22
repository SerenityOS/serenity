/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ExportProgressWindow.h"
#include <AK/ByteString.h>
#include <Applications/Piano/ExportProgressWidget.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

ExportProgressWindow::ExportProgressWindow(GUI::Window& parent_window, Atomic<int>& wav_percent_written)
    : GUI::Dialog(&parent_window)
    , m_wav_percent_written(wav_percent_written)
{
}

ErrorOr<void> ExportProgressWindow::initialize()
{
    auto main_widget = set_main_widget<GUI::Widget>();
    TRY(main_widget->load_from_gml(export_progress_widget));

    set_resizable(false);
    set_closeable(false);
    set_title("Rendering Audio");
    set_icon(GUI::Icon::default_icon("app-piano"sv).bitmap_for_size(16));

    m_progress_bar = *main_widget->find_descendant_of_type_named<GUI::HorizontalProgressbar>("progress_bar");
    m_label = *main_widget->find_descendant_of_type_named<GUI::Label>("export_message");

    start_timer(250);

    return {};
}

void ExportProgressWindow::set_filename(StringView filename)
{
    m_label->set_text(String::formatted("Rendering audio to {}…", filename).release_value_but_fixme_should_propagate_errors());
    update();
}

void ExportProgressWindow::timer_event(Core::TimerEvent&)
{
    m_progress_bar->set_value(m_wav_percent_written.load());
    if (window_id() != 0)
        set_progress(m_wav_percent_written.load());

    if (m_wav_percent_written.load() == 100) {
        m_wav_percent_written.store(0);
        close();
    }
}
