/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

class PCSpeaker {
public:
    static void tone_on(int frequency);
    static void tone_off();
};
