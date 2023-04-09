/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/StringView.h>
#include <AK/Types.h>

#include "Command.h"

#define IAC 0xff

class Parser {
public:
    Function<void(Command const&)> on_command;
    Function<void(StringView)> on_data;
    Function<void()> on_error;

    void write(StringView);

protected:
    enum State {
        Free,
        ReadCommand,
        ReadSubcommand,
        Error,
    };

private:
    State m_state { State::Free };
    u8 m_command { 0 };
};
