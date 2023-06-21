/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class AbstractWizardPage : public Widget {
    C_OBJECT_ABSTRACT(AbstractWizardPage);

public:
    virtual ~AbstractWizardPage() override = default;

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
    AbstractWizardPage() = default;

private:
    bool m_is_final_page { false };
};

}
