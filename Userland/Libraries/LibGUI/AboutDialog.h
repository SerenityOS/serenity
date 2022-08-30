/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Version.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class AboutDialog final : public Dialog {
    C_OBJECT(AboutDialog)
public:
    virtual ~AboutDialog() override = default;

    static void show(StringView name, StringView version, Gfx::Bitmap const* icon = nullptr, Window* parent_window = nullptr, Gfx::Bitmap const* window_icon = nullptr)
    {
        auto dialog = AboutDialog::construct(name, version, icon, parent_window);
        if (window_icon)
            dialog->set_icon(window_icon);
        dialog->exec();
    }

private:
    AboutDialog(StringView name, StringView version, Gfx::Bitmap const* icon = nullptr, Window* parent_window = nullptr);

    String m_name;
    RefPtr<Gfx::Bitmap> m_icon;
    String m_version_string;
};
}
