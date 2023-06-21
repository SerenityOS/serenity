/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Window.h>

class ProgressWindow final : public GUI::Window {
    C_OBJECT_ABSTRACT(ProgressWindow)
public:
    static ErrorOr<NonnullRefPtr<ProgressWindow>> try_create(StringView title, GUI::Window* parent = nullptr);
    virtual ~ProgressWindow() override;

    void update_progress_label(size_t files_encountered_count);

private:
    ProgressWindow(StringView title, GUI::Window* parent = nullptr);

    RefPtr<GUI::Label> m_progress_label;
};
