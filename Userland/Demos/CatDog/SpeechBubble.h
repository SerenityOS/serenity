/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CatDog.h"
#include <AK/NonnullRefPtr.h>
#include <LibGUI/Widget.h>

class SpeechBubble final : public GUI::Widget {
    C_OBJECT(SpeechBubble);

public:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    Function<void()> on_dismiss;
    NonnullRefPtr<CatDog> m_cat_dog;

private:
    SpeechBubble(NonnullRefPtr<CatDog> cat_dog)
        : m_cat_dog(move(cat_dog))
    {
    }
};
