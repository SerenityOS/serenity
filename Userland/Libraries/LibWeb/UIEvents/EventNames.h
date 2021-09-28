/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::UIEvents::EventNames {

// FIXME: This is not all of the events

#define ENUMERATE_UI_EVENTS          \
    __ENUMERATE_UI_EVENT(click)      \
    __ENUMERATE_UI_EVENT(keydown)    \
    __ENUMERATE_UI_EVENT(keyup)      \
    __ENUMERATE_UI_EVENT(mousedown)  \
    __ENUMERATE_UI_EVENT(mouseenter) \
    __ENUMERATE_UI_EVENT(mouseleave) \
    __ENUMERATE_UI_EVENT(mousemove)  \
    __ENUMERATE_UI_EVENT(mouseout)   \
    __ENUMERATE_UI_EVENT(mouseover)  \
    __ENUMERATE_UI_EVENT(mouseup)    \
    __ENUMERATE_UI_EVENT(resize)

#define __ENUMERATE_UI_EVENT(name) extern FlyString name;
ENUMERATE_UI_EVENTS
#undef __ENUMERATE_UI_EVENT

}
