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

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Types.h>

#include "Parser.h"

void Parser::write(const StringView& data)
{
    for (size_t i = 0; i < data.length(); i++) {
        u8 ch = data[i];

        switch (m_state) {
        case State::Free:
            switch (ch) {
            case IAC:
                m_state = State::ReadCommand;
                break;
            case '\r':
                if (on_data)
                    on_data("\n");
                break;
            default:
                if (on_data)
                    on_data(StringView(&ch, 1));
                break;
            }
            break;
        case State::ReadCommand:
            switch (ch) {
            case IAC: {
                m_state = State::Free;
                if (on_data)
                    on_data("\xff");
                break;
            }
            case CMD_WILL:
            case CMD_WONT:
            case CMD_DO:
            case CMD_DONT:
                m_command = ch;
                m_state = State::ReadSubcommand;
                break;
            default:
                m_state = State::Error;
                if (on_error)
                    on_error();
                break;
            }
            break;
        case State::ReadSubcommand: {
            auto command = m_command;
            m_command = 0;
            m_state = State::Free;
            if (on_command)
                on_command({ command, ch });
            break;
        }
        case State::Error:
            // ignore everything
            break;
        }
    }
}
