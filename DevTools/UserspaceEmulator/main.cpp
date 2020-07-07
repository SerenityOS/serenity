/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "Emulator.h"
#include "SoftCPU.h"
#include <AK/LogStream.h>
#include <AK/MappedFile.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/Loader.h>
#include <LibX86/Instruction.h>

int main(int argc, char** argv)
{
    const char* executable_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(executable_path, "Executable path", "executable");
    args_parser.parse(argc, argv);

    MappedFile mapped_file(executable_path);
    if (!mapped_file.is_valid()) {
        warn() << "Unable to map " << executable_path;
        return 1;
    }

    auto elf = ELF::Loader::create((const u8*)mapped_file.data(), mapped_file.size());

    auto main_symbol = elf->find_demangled_function("main");
    if (!main_symbol.has_value()) {
        warn() << "Could not find 'main' symbol in executable";
        return 1;
    }

    auto main_code = main_symbol.value().raw_data();
    X86::SimpleInstructionStream stream((const u8*)main_code.characters_without_null_termination(), main_code.length());

    UserspaceEmulator::Emulator emulator;
    return emulator.exec(stream, main_symbol.value().value());
}
