#include <AK/LogStream.h>
#include <AK/MappedFile.h>
#include <LibX86/Disassembler.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc == 1) {
        fprintf(stderr, "usage: %s <binary>\n", argv[0]);
        return 1;
    }

    MappedFile file(argv[1]);

    X86::SimpleInstructionStream stream((const u8*)file.data(), file.size());
    X86::Disassembler disassembler(stream);

    for (;;) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;
        out() << String::format("%08x", offset) << "  " << insn.value().to_string(offset);
    }

    return 0;
}
