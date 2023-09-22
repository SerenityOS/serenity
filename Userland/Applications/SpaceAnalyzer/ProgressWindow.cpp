/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProgressWindow.h"
#include <LibCore/EventLoop.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>

ErrorOr<NonnullRefPtr<ProgressWindow>> ProgressWindow::try_create(StringView title, Window* parent)
{
    auto window = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProgressWindow(title, parent)));

    auto main_widget = window->set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& label = main_widget->add<GUI::Label>("Analyzing storage space..."_string);
    label.set_fixed_height(22);

    window->m_progress_label = main_widget->add<GUI::Label>();
    window->m_progress_label->set_fixed_height(22);

    window->update_progress_label(0);
    return window;
}

ProgressWindow::ProgressWindow(StringView title, GUI::Window* parent)
    : GUI::Window(parent)
{
    set_title(title);
    set_resizable(false);
    set_closeable(false);
    resize(240, 50);
    center_on_screen();
}

ProgressWindow::~ProgressWindow() = default;

void ProgressWindow::update_progress_label(size_t files_encountered_count)
{
    m_progress_label->set_text(String::formatted("{} files...", files_encountered_count).release_value_but_fixme_should_propagate_errors());
    // FIXME: Why is this necessary to make the window repaint?
    Core::EventLoop::current().pump(Core::EventLoop::WaitMode::PollForEvents);
}
