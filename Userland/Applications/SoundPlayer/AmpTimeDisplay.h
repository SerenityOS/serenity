/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Skin.h"
#include <LibGUI/Widget.h>

class AmpTimeDisplay final : public GUI::Widget {
    C_OBJECT(AmpTimeDisplay)

public:
    void set_minutes(int);
    int minutes() const { return m_minutes; }

    void set_seconds(int);
    int seconds() const { return m_seconds; }

    void set_time(int minutes, int seconds)
    {
        set_minutes(minutes);
        set_seconds(seconds);
    }

    void set_digits_visible(bool);
    bool digits_visible() const { return m_digits_visible; }

protected:
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    explicit AmpTimeDisplay(const Skin&);

    const Skin& m_skin;
    int m_minutes;
    int m_seconds;
    bool m_digits_visible;
};
