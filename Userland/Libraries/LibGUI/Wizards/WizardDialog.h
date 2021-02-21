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

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Wizards/AbstractWizardPage.h>

namespace GUI {

class WizardDialog : public Dialog {
    C_OBJECT(WizardDialog)
public:
    virtual ~WizardDialog() override;

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
