/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

// https://w3c.github.io/uievents/#event-modifier-initializers
struct EventModifierInit : public UIEventInit {
    bool ctrl_key { false };
    bool shift_key { false };
    bool alt_key { false };
    bool meta_key { false };
};

}
