/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Typeface.h>

namespace Gfx {

namespace FontWeight {
enum {
    Thin = 100,
    ExtraLight = 200,
    Light = 300,
    Regular = 400,
    Medium = 500,
    SemiBold = 600,
    Bold = 700,
    ExtraBold = 800,
    Black = 900,
    ExtraBlack = 950
};
}

class FontDatabase {
public:
    static FontDatabase& the();

    static Font& default_font();
    static Font& default_bold_font();

    static Font& default_fixed_width_font();
    static Font& default_bold_fixed_width_font();

    RefPtr<Gfx::Font> get(const String& family, unsigned size, unsigned weight);
    RefPtr<Gfx::Font> get(const String& family, const String& variant, unsigned size);
    RefPtr<Gfx::Font> get_by_name(const StringView&);
    void for_each_font(Function<void(const Gfx::Font&)>);
    void for_each_fixed_width_font(Function<void(const Gfx::Font&)>);

    void for_each_typeface(Function<void(const Typeface&)>);

private:
    FontDatabase();
    ~FontDatabase();

    RefPtr<Typeface> get_or_create_typeface(const String& family, const String& variant);

    struct Private;
    OwnPtr<Private> m_private;
};

}
