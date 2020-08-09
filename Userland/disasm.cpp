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

#include <AK/LogStream.h>
#include <AK/MappedFile.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/Loader.h>
#include <LibX86/Disassembler.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to i386 binary file", "path");
    args_parser.parse(argc, argv);

    MappedFile file(path);
    if (!file.is_valid()) {
        // Already printed some error message.
        return 1;
    }

    const u8* asm_data = (const u8*)file.data();
    size_t asm_size = file.size();
    size_t file_offset = 0;
    if (asm_size >= 4 && strncmp((const char*)asm_data, "\u007fELF", 4) == 0) {
        if (auto elf = ELF::Loader::create(asm_data, asm_size)) {
            elf->image().for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
                // FIXME: Disassemble all SHT_PROGBITS sections, not just .text.
                if (section.name() != ".text")
                    return IterationDecision::Continue;
                asm_data = (const u8*)section.raw_data();
                asm_size = section.size();
                file_offset = section.address();
                return IterationDecision::Break;
            });
        }
    }

    X86::SimpleInstructionStream stream(asm_data, asm_size);
    X86::Disassembler disassembler(stream);

    for (;;) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;
        out() << String::format("%08x", file_offset + offset) << "  " << insn.value().to_string(offset);
    }

    return 0;
}
