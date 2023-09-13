/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/Version.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class AboutDialog final : public Dialog {
    C_OBJECT_ABSTRACT(AboutDialog)
public:
    [[nodiscard]] static NonnullRefPtr<AboutDialog> create(String const& name, String version, RefPtr<Gfx::Bitmap const> icon = nullptr, Window* parent_window = nullptr);
    virtual ~AboutDialog() override = default;

    static void show(String name, String version, RefPtr<Gfx::Bitmap const> icon = nullptr, Window* parent_window = nullptr, RefPtr<Gfx::Bitmap const> window_icon = nullptr);

private:
    AboutDialog(String const& name, String version, RefPtr<Gfx::Bitmap const> icon = nullptr, Window* parent_window = nullptr);

    String m_name;
    String m_version_string;
    RefPtr<Gfx::Bitmap const> m_icon;
};
}
