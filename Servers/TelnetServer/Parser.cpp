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
