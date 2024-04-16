/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DemoWizardDialog.h"
#include <Demos/WidgetGallery/DemoWizardPage1GML.h>
#include <Demos/WidgetGallery/DemoWizardPage2GML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Wizards/WizardDialog.h>

DemoWizardDialog::DemoWizardDialog(GUI::Window* parent_window)
    : GUI::WizardDialog(parent_window)
{
    set_title("Demo Wizard");
    build().release_value_but_fixme_should_propagate_errors();

    // Create the front cover
    m_front_page = GUI::CoverWizardPage::create(
        "Welcome to the SerenityOS demo wizard!"sv,
        "This wizard demonstrates the amazing wizardry\ncapabilities of LibGUI :^)"sv)
                       .release_value_but_fixme_should_propagate_errors();
    m_front_page->on_next_page = [&]() {
        return m_page_1;
    };

    // Create Page 1
    m_page_1 = GUI::WizardPage::create(
        "Installation location"sv,
        "Choose where Demo Application is installed on your computer."sv)
                   .release_value_but_fixme_should_propagate_errors();
    m_page_1->body_widget().load_from_gml(demo_wizard_page_1_gml).release_value_but_fixme_should_propagate_errors();
    m_page_1_location_text_box = m_page_1->body_widget().find_descendant_of_type_named<GUI::TextBox>("page_1_location_text_box");
    m_page_1->on_next_page = [&]() {
        return m_page_2;
    };

    // Create Page 2 with a progress bar :^)
    m_page_2 = GUI::WizardPage::create(
        "Installation in progress..."sv,
        "Please wait. Do not turn off your computer."sv)
                   .release_value_but_fixme_should_propagate_errors();
    m_page_2->body_widget().load_from_gml(demo_wizard_page_2_gml).release_value_but_fixme_should_propagate_errors();
    m_page_2_progressbar = m_page_2->body_widget().find_descendant_of_type_named<GUI::Progressbar>("page_2_progressbar");
    m_page_2_timer = Core::Timer::create_repeating(
        100, [&]() {
            if (m_page_2_progress_value < 100)
                m_page_2_progress_value++;
            m_page_2_progressbar->set_value(m_page_2_progress_value);

            // Go to final page on progress completion
            if (m_page_2_progress_value == 100) {
                m_page_2_progress_value = 0;
                replace_page(*m_back_page);
            }
        },
        this);
    m_page_2->on_page_enter = [&]() {
        m_page_2_progress_value = 0;
        m_page_2_timer->restart();
    };
    m_page_2->on_page_leave = [&]() {
        m_page_2_progress_value = 0;
        m_page_2_timer->stop();
    };
    // Don't set a on_next_page handler for page 2 as we automatically navigate to the final page on progress completion

    // Create the back cover
    m_back_page = GUI::CoverWizardPage::create(
        "Wizard complete."sv,
        "That concludes the SerenityOS demo wizard :^)"sv)
                      .release_value_but_fixme_should_propagate_errors();
    m_back_page->set_is_final_page(true);

    push_page(*m_front_page);
}
