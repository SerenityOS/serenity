/*
 * Copyright (c) 2024, Ninad Sachania <ninad.sachania@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static void execute(StringView const instructions, u32 const array_size)
{
    Vector<u8> data;
    data.resize(array_size);

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
            data[data_pointer] += 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '-') {
            data[data_pointer] -= 1;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '.') {
            putchar(data[data_pointer]);
            (void)fflush(stdout);

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == ',') {
            u8 input = getc(stdin);

            data[data_pointer] = input;

            instruction_pointer += 1;
        } else if (instructions[instruction_pointer] == '[') {
            if (data[data_pointer] == 0x00) {
                int balance = 1;

                auto ip = instruction_pointer + 1;
                while (ip < instructions.length() && balance != 0) {
                    if (instructions[ip] == ']')
                        balance -= 1;
                    else if (instructions[ip] == '[')
                        balance += 1;

                    ip += 1;
                }

                instruction_pointer = ip;
            } else {
                instruction_pointer += 1;
            }
        } else if (instructions[instruction_pointer] == ']') {
            if (data[data_pointer] > 0x00) {
                int balance = 1;

                auto ip = instruction_pointer - 1;
                while (balance != 0) {
                    if (instructions[ip] == '[')
                        balance -= 1;
                    else if (instructions[ip] == ']')
                        balance += 1;

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

    u32 array_size = 30000;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("A Brainf**k interpreter.");
    args_parser.add_positional_argument(path, "Program path", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(array_size, "Size of the program array (default 30000)", "size", 's', "number");
    args_parser.parse(arguments);

    if (array_size < 30000) {
        warnln("The array size must be at least 30000.");
        exit(1);
    }

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));

    auto const content = TRY(file->read_until_eof());

    execute(content, array_size);
    return 0;
}
