/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Font/FontWeight.h>
#include <LibGfx/Font/Typeface.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class FontDatabase {
public:
    static FontDatabase& the();

    static Font& default_font();
    static Font& default_fixed_width_font();
    static Font& window_title_font();

    static ByteString default_font_query();
    static ByteString window_title_font_query();
    static ByteString fixed_width_font_query();

    static ByteString default_fonts_lookup_path();
    static void set_default_font_query(ByteString);
    static void set_window_title_font_query(ByteString);
    static void set_fixed_width_font_query(ByteString);

    RefPtr<Gfx::Font> get(FlyString const& family, float point_size, unsigned weight, unsigned width, unsigned slope, Font::AllowInexactSizeMatch = Font::AllowInexactSizeMatch::No);
    RefPtr<Gfx::Font> get(FlyString const& family, FlyString const& variant, float point_size, Font::AllowInexactSizeMatch = Font::AllowInexactSizeMatch::No);
    RefPtr<Gfx::Font> get_by_name(StringView);
    void for_each_font(Function<void(Gfx::Font const&)>);
    void for_each_fixed_width_font(Function<void(Gfx::Font const&)>);

    void for_each_typeface(Function<void(Typeface const&)>);
    void for_each_typeface_with_family_name(FlyString const& family_name, Function<void(Typeface const&)>);

    void load_all_fonts_from_uri(StringView);

private:
    FontDatabase();
    ~FontDatabase() = default;

    RefPtr<Typeface> get_or_create_typeface(FlyString const& family, FlyString const& variant);

    struct Private;
    OwnPtr<Private> m_private;
};

}
