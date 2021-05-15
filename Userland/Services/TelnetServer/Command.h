/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>

#define CMD_WILL 0xfb
#define CMD_WONT 0xfc
#define CMD_DO 0xfd
#define CMD_DONT 0xfe
#define SUB_ECHO 0x01
#define SUB_SUPPRESS_GO_AHEAD 0x03

struct Command {
    u8 command;
    u8 subcommand;

    String to_string() const
    {
        StringBuilder builder;

        switch (command) {
        case CMD_WILL:
            builder.append("WILL");
            break;
        case CMD_WONT:
            builder.append("WONT");
            break;
        case CMD_DO:
            builder.append("DO");
            break;
        case CMD_DONT:
            builder.append("DONT");
            break;
        default:
            builder.append(String::formatted("UNKNOWN<{:02x}>", command));
            break;
        }

        builder.append(" ");

        switch (subcommand) {
        case SUB_ECHO:
            builder.append("ECHO");
            break;
        case SUB_SUPPRESS_GO_AHEAD:
            builder.append("SUPPRESS_GO_AHEAD");
            break;
        default:
            builder.append(String::formatted("UNKNOWN<{:02x}>", subcommand));
            break;
        }

        return builder.to_string();
    };
};
