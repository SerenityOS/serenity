/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <Ladybird/Utilities.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

#import <System/Cocoa.h>
#import <UI/Palette.h>

namespace Ladybird {

bool is_using_dark_system_theme()
{
    auto* appearance = [NSApp effectiveAppearance];

    auto* matched_appearance = [appearance bestMatchFromAppearancesWithNames:@[
        NSAppearanceNameAqua,
        NSAppearanceNameDarkAqua,
    ]];

    return [matched_appearance isEqualToString:NSAppearanceNameDarkAqua];
}

Core::AnonymousBuffer create_system_palette()
{
    auto is_dark = is_using_dark_system_theme();

    auto theme_file = is_dark ? "Default"sv : "Dark"sv;
    auto theme_path = DeprecatedString::formatted("{}/res/themes/{}.ini", s_serenity_resource_root, theme_file);

    auto theme = MUST(Gfx::load_system_theme(theme_path));
    auto palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);
    auto palette = Gfx::Palette(move(palette_impl));

    palette.set_flag(Gfx::FlagRole::IsDark, is_dark);

    return theme;
}

}
