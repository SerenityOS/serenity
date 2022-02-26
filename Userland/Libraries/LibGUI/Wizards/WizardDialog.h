/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Wizards/AbstractWizardPage.h>

namespace GUI {

class WizardDialog : public Dialog {
    C_OBJECT(WizardDialog)
public:
    virtual ~WizardDialog() override = default;

    static void show(AbstractWizardPage& first_page, Window* parent_window = nullptr)
    {
        auto dialog = WizardDialog::construct(parent_window);
        dialog->push_page(first_page);
        dialog->exec();
    }

    Function<void()> on_cancel;

    /// Push a page onto the page stack and display it, preserving the previous page on the stack.
    void push_page(AbstractWizardPage& page);
    /// Replace the current page on the stack with a new page, preventing the user from returning to the current page.
    void replace_page(AbstractWizardPage& page);
    void pop_page();
    AbstractWizardPage& current_page();

    inline bool has_pages() const { return !m_page_stack.is_empty(); }

protected:
    WizardDialog(Window* parent_window);

    virtual void handle_cancel();

private:
    void update_navigation();

    RefPtr<Widget> m_page_container_widget;
    RefPtr<Button> m_back_button;
    RefPtr<Button> m_next_button;
    RefPtr<Button> m_cancel_button;

    NonnullRefPtrVector<AbstractWizardPage> m_page_stack;
};
}
