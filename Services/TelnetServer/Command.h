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
            builder.append(String::format("UNKNOWN<%02x>", command));
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
            builder.append(String::format("UNKNOWN<%02x>"));
            break;
        }

        return builder.to_string();
    };
};
