/*
 * Copyright (c) 2024, Ninad Sachania <ninad.sachania@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static void execute(StringView const instructions)
{
    Array<u8, 30000> m_data;
    u32 data_pointer = 0;
    u32 instruction_pointer = 0;

    while (instruction_pointer < instructions.length()) {
        if (instructions[instruction_pointer] == '>') {
            data_pointer += 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '<') {
            data_pointer -= 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '+') {
            m_data[data_pointer] += 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '-') {
            m_data[data_pointer] -= 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '.') {
            putchar(m_data[data_pointer]);
            (void)fflush(stdout);

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == ',') {
            u8 input = getc(stdin);

            m_data[data_pointer] = input;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '[') {
            if (m_data[data_pointer] == 0x00) {
                int balance = 1;

                auto ip = instruction_pointer + 1;
                while (ip < instructions.length() && balance != 0) {
                    if (instructions[ip] == ']') {
                        balance -= 1;
                    } else if (instructions[ip] == '[') {
                        balance += 1;
                    }
                    ip += 1;
                }

                instruction_pointer = ip;
            } else {
                instruction_pointer += 1;
            }
        } else if (instructions[instruction_pointer] == ']') {
            if (m_data[data_pointer] > 0x00) {
                int balance = 1;

                auto ip = instruction_pointer - 1;
                while (balance != 0) {
                    if (instructions[ip] == '[') {
                        balance -= 1;
                    } else if (instructions[ip] == ']') {
                        balance += 1;
                    }

                    if (ip == 0)
                        break;
                    ip -= 1;
                }

                instruction_pointer = ip + 2;
            } else {
                instruction_pointer += 1;
            }
        } else {
            instruction_pointer += 1;
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("A Brainf**k interpreter.");
    args_parser.add_positional_argument(path, "Program path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file_or_error = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));

    auto const content = TRY(file_or_error->read_until_eof());
    StringView const content_view(content);

    execute(content_view);
    return 0;
}
