/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    // Create the front cover
    m_front_page = GUI::CoverWizardPage::construct();
    m_front_page->set_header_text("Welcome to the SerenityOS demo wizard!");
    m_front_page->set_body_text("This wizard demonstrates the amazing wizardry\ncapabilities of LibGUI :^)");
    m_front_page->on_next_page = [&]() {
        return m_page_1;
    };

    // Create Page 1
    m_page_1 = GUI::WizardPage::construct(
        "Installation location",
        "Choose where Demo Application is installed on your computer.");
    m_page_1->body_widget().load_from_gml(demo_wizard_page_1_gml);
    m_page_1_location_text_box = m_page_1->body_widget().find_descendant_of_type_named<GUI::TextBox>("page_1_location_text_box");
    m_page_1->on_next_page = [&]() {
        return m_page_2;
    };

    // Create Page 2 with a progress bar :^)
    m_page_2 = GUI::WizardPage::construct(
        "Installation in progress...",
        "Please wait. Do not turn off your computer.");
    m_page_2->body_widget().load_from_gml(demo_wizard_page_2_gml);
    m_page_2_progress_bar = m_page_2->body_widget().find_descendant_of_type_named<GUI::ProgressBar>("page_2_progress_bar");
    m_page_2_timer = Core::Timer::construct(this);
    m_page_2->on_page_enter = [&]() {
        m_page_2_progress_value = 0;
        m_page_2_timer->restart(100);
    };
    m_page_2->on_page_leave = [&]() {
        m_page_2_progress_value = 0;
        m_page_2_timer->stop();
    };
    m_page_2_timer->on_timeout = [&]() {
        if (m_page_2_progress_value < 100)
            m_page_2_progress_value++;
        m_page_2_progress_bar->set_value(m_page_2_progress_value);

        // Go to final page on progress completion
        if (m_page_2_progress_value == 100) {
            m_page_2_progress_value = 0;
            replace_page(*m_back_page);
        }
    };
    // Don't set a on_next_page handler for page 2 as we automatically navigate to the final page on progress completion

    // Create the back cover
    m_back_page = GUI::CoverWizardPage::construct();
    m_back_page->set_header_text("Wizard complete.");
    m_back_page->set_body_text("That concludes the SerenityOS demo wizard :^)");
    m_back_page->set_is_final_page(true);

    push_page(*m_front_page);
}
