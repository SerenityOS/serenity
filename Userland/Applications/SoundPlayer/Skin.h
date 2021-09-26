/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>

class Skin {
public:
    void load_from_file(const String&);

    RefPtr<Gfx::Bitmap> main() const { return m_main; }
    RefPtr<Gfx::Bitmap> cbuttons() const { return m_cbuttons; }
    RefPtr<Gfx::Bitmap> numbers() const { return m_numbers; }
    RefPtr<Gfx::Bitmap> playpaus() const { return m_playpaus; }
    RefPtr<Gfx::Bitmap> posbar() const { return m_posbar; }
    RefPtr<Gfx::Bitmap> shufrep() const { return m_shufrep; }
    RefPtr<Gfx::Bitmap> titlebar() const { return m_titlebar; }
    RefPtr<Gfx::Bitmap> volume() const { return m_volume; }
    RefPtr<Gfx::Bitmap> balance() const { return m_balance; }
    RefPtr<Gfx::Bitmap> monoster() const { return m_monoster; }
    RefPtr<Gfx::Bitmap> text() const { return m_text; }
    Color viscolor(int index) const
    {
        return (index >= 0 && index <= 23) ? m_viscolor[index] : Gfx::Color(Gfx::Color::NamedColor::Black);
    }

private:
    RefPtr<Gfx::Bitmap> m_main;
    RefPtr<Gfx::Bitmap> m_cbuttons;
    RefPtr<Gfx::Bitmap> m_numbers;
    RefPtr<Gfx::Bitmap> m_playpaus;
    RefPtr<Gfx::Bitmap> m_posbar;
    RefPtr<Gfx::Bitmap> m_shufrep;
    RefPtr<Gfx::Bitmap> m_titlebar;
    RefPtr<Gfx::Bitmap> m_volume;
    RefPtr<Gfx::Bitmap> m_balance;
    RefPtr<Gfx::Bitmap> m_monoster;
    RefPtr<Gfx::Bitmap> m_text;

    Gfx::Color m_viscolor[24];
};
