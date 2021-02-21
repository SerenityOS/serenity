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
#include <LibGUI/Widget.h>

namespace GUI {

class AbstractWizardPage : public Widget {
    C_OBJECT_ABSTRACT(AbstractWizardPage);

public:
    virtual ~AbstractWizardPage() override;

    Function<RefPtr<AbstractWizardPage>()> on_next_page;
    virtual RefPtr<AbstractWizardPage> next_page();
    virtual bool can_go_next();

    Function<void()> on_page_enter;
    virtual void page_enter();

    Function<void()> on_page_leave;
    virtual void page_leave();

    bool is_final_page() const { return m_is_final_page; }
    void set_is_final_page(bool val) { m_is_final_page = val; }

protected:
    AbstractWizardPage();

private:
    bool m_is_final_page { false };
};

}
