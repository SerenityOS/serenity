/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Platform {

class FontPluginSerenity final : public FontPlugin {
public:
    FontPluginSerenity();
    virtual ~FontPluginSerenity();

    virtual Gfx::Font& default_font() override;
    virtual Gfx::Font& default_fixed_width_font() override;
    virtual FlyString generic_font_name(GenericFont) override;
};

}
