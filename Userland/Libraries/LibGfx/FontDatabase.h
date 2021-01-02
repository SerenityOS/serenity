/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Typeface.h>

namespace Gfx {

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
