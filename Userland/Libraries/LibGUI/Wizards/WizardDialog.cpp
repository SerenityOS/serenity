/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
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

WizardDialog::WizardDialog(Window* parent_window)
    : Dialog(parent_window)
    , m_page_stack()
{
    resize(500, 360);
    set_title(String::formatted("Sample wizard"));
    set_resizable(false);

    if (parent_window)
        set_icon(parent_window->icon());

    auto& main_widget = set_main_widget<Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<VerticalBoxLayout>();
    main_widget.layout()->set_spacing(0);

    m_page_container_widget = main_widget.add<Widget>();
    m_page_container_widget->set_fixed_size(500, 315);
    m_page_container_widget->set_layout<VerticalBoxLayout>();

    auto& separator = main_widget.add<SeparatorWidget>(Gfx::Orientation::Horizontal);
    separator.set_fixed_height(2);

    auto& nav_container_widget = main_widget.add<Widget>();
    nav_container_widget.set_layout<HorizontalBoxLayout>();
    nav_container_widget.set_fixed_height(42);
    nav_container_widget.layout()->set_margins({ 0, 10 });
    nav_container_widget.layout()->set_spacing(0);
    nav_container_widget.layout()->add_spacer();

    m_back_button = nav_container_widget.add<Button>("< Back");
    m_back_button->set_fixed_width(75);
    m_back_button->on_click = [&](auto) {
        pop_page();
    };

    m_next_button = nav_container_widget.add<Button>("Next >");
    m_next_button->set_fixed_width(75);
    m_next_button->on_click = [&](auto) {
        VERIFY(has_pages());

        if (!current_page().can_go_next())
            return done(ExecOK);

        auto next_page = current_page().next_page();
        if (!next_page)
            return done(ExecOK);

        push_page(*next_page);
    };

    auto& button_spacer = nav_container_widget.add<Widget>();
    button_spacer.set_fixed_width(10);

    m_cancel_button = nav_container_widget.add<Button>("Cancel");
    m_cancel_button->set_fixed_width(75);
    m_cancel_button->on_click = [&](auto) {
        handle_cancel();
    };

    update_navigation();
}

WizardDialog::~WizardDialog()
{
}

void WizardDialog::push_page(AbstractWizardPage& page)
{
    if (!m_page_stack.is_empty())
        m_page_stack.last().page_leave();

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
    m_page_stack.last().page_enter();
}

void WizardDialog::update_navigation()
{
    m_back_button->set_enabled(m_page_stack.size() > 1);
    if (has_pages()) {
        m_next_button->set_enabled(current_page().is_final_page() || current_page().can_go_next());
        m_next_button->set_text(current_page().is_final_page() ? "Finish" : "Next >");
    } else {
        m_next_button->set_text("Next >");
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

    done(ExecCancel);
}

}
