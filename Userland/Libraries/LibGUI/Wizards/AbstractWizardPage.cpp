/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Wizards/AbstractWizardPage.h>

namespace GUI {

RefPtr<AbstractWizardPage> AbstractWizardPage::next_page()
{
    if (on_next_page)
        return on_next_page();
    return nullptr;
}

bool AbstractWizardPage::can_go_next()
{
    return !!on_next_page;
}

void AbstractWizardPage::page_enter()
{
    if (on_page_enter)
        return on_page_enter();
}

void AbstractWizardPage::page_leave()
{
    if (on_page_leave)
        return on_page_leave();
}

}
