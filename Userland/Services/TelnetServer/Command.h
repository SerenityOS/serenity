/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
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

    ByteString to_byte_string() const
    {
        StringBuilder builder;

        switch (command) {
        case CMD_WILL:
            builder.append("WILL"sv);
            break;
        case CMD_WONT:
            builder.append("WONT"sv);
            break;
        case CMD_DO:
            builder.append("DO"sv);
            break;
        case CMD_DONT:
            builder.append("DONT"sv);
            break;
        default:
            builder.append(ByteString::formatted("UNKNOWN<{:02x}>", command));
            break;
        }

        builder.append(" "sv);

        switch (subcommand) {
        case SUB_ECHO:
            builder.append("ECHO"sv);
            break;
        case SUB_SUPPRESS_GO_AHEAD:
            builder.append("SUPPRESS_GO_AHEAD"sv);
            break;
        default:
            builder.append(ByteString::formatted("UNKNOWN<{:02x}>", subcommand));
            break;
        }

        return builder.to_byte_string();
    }
};
