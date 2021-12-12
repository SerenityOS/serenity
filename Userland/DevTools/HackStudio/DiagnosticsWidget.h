/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Diagnostics.h"
#include "DiagnosticsData.h"
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class DiagnosticsWidget final : public GUI::Widget {
    C_OBJECT(DiagnosticsWidget)
public:
    virtual ~DiagnosticsWidget() override { }

    void refresh()
    {
        if (m_result_view && m_result_view->model())
            m_result_view->model()->invalidate();
    }

private:
    explicit DiagnosticsWidget();

    RefPtr<GUI::TableView> m_result_view;
};

}
