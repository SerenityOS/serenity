/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/AbstractWizardPage.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

ErrorOr<NonnullRefPtr<WizardDialog>> WizardDialog::create(Window* parent_window)
{
    auto dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WizardDialog(parent_window)));
    TRY(dialog->build());
    return dialog;
}

ErrorOr<void> WizardDialog::build()
{
    auto main_widget = set_main_widget<Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<VerticalBoxLayout>(Margins {}, 0);

    m_page_container_widget = main_widget->add<Widget>();
    m_page_container_widget->set_fixed_size(500, 315);
    m_page_container_widget->set_layout<VerticalBoxLayout>();

    auto& separator = main_widget->add<SeparatorWidget>(Gfx::Orientation::Horizontal);
    separator.set_fixed_height(2);

    auto& nav_container_widget = main_widget->add<Widget>();
    nav_container_widget.set_layout<HorizontalBoxLayout>(Margins { 0, 10 }, 0);
    nav_container_widget.set_fixed_height(42);
    nav_container_widget.add_spacer();

    m_back_button = nav_container_widget.add<DialogButton>("< Back"_string);
    m_back_button->on_click = [&](auto) {
        pop_page();
    };

    m_next_button = nav_container_widget.add<DialogButton>("Next >"_string);
    m_next_button->on_click = [&](auto) {
        VERIFY(has_pages());

        if (!current_page().can_go_next())
            return done(ExecResult::OK);

        auto next_page = current_page().next_page();
        if (!next_page)
            return done(ExecResult::OK);

        push_page(*next_page);
    };

    auto& button_spacer = nav_container_widget.add<Widget>();
    button_spacer.set_fixed_width(10);

    m_cancel_button = nav_container_widget.add<DialogButton>("Cancel"_string);
    m_cancel_button->on_click = [&](auto) {
        handle_cancel();
    };

    update_navigation();

    return {};
}

WizardDialog::WizardDialog(Window* parent_window)
    : Dialog(parent_window)
    , m_page_stack()
{
    resize(500, 360);
    set_resizable(false);
}

void WizardDialog::push_page(AbstractWizardPage& page)
{
    if (!m_page_stack.is_empty())
        m_page_stack.last()->page_leave();

    m_page_stack.append(page);
    m_page_container_widget->remove_all_children();
    m_page_container_widget->add_child(page);

    update_navigation();
    page.page_enter();
}

void WizardDialog::replace_page(AbstractWizardPage& page)
{
    if (!m_page_stack.is_empty())
        m_page_stack.take_last()->page_leave();

    m_page_stack.append(page);
    m_page_container_widget->remove_all_children();
    m_page_container_widget->add_child(page);

    update_navigation();
    page.page_enter();
}

void WizardDialog::pop_page()
{
    if (m_page_stack.size() <= 1)
        return;

    auto page = m_page_stack.take_last();
    page->page_leave();

    m_page_container_widget->remove_all_children();
    m_page_container_widget->add_child(m_page_stack.last());

    update_navigation();
    m_page_stack.last()->page_enter();
}

void WizardDialog::update_navigation()
{
    m_back_button->set_enabled(m_page_stack.size() > 1);
    if (has_pages()) {
        m_next_button->set_enabled(current_page().is_final_page() || current_page().can_go_next());
        if (current_page().is_final_page())
            m_next_button->set_text("Finish"_string);
        else
            m_next_button->set_text("Next >"_string);
    } else {
        m_next_button->set_text("Next >"_string);
        m_next_button->set_enabled(false);
    }
}

AbstractWizardPage& WizardDialog::current_page()
{
    VERIFY(has_pages());
    return m_page_stack.last();
}

void WizardDialog::handle_cancel()
{
    if (on_cancel)
        return on_cancel();

    done(ExecResult::Cancel);
}

}
