/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Ladybird {

class FontPluginQt final : public Web::Platform::FontPlugin {
public:
    FontPluginQt();
    virtual ~FontPluginQt();

    virtual String generic_font_name(Web::Platform::GenericFont) override;

    void update_generic_fonts();

private:
    Vector<String> m_generic_font_names;
};

}
