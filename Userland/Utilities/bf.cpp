/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <unistd.h>

enum class Instruction : u8 {
    IncrementPointer,
    DecrementPointer,
    IncrementCell,
    DecrementCell,
    GetChar,
    PutChar,
    BeginLoop,
    EndLoop,
};

class BrainFuckInterpreter {
public:
    void parse_source_file(NonnullRefPtr<Core::File> file);
    void exec();
private:
    Vector<Instruction> m_program {};
    HashMap<u64, u64> m_loop_pairs {};
    Vector<u8> m_data {};
};

void BrainFuckInterpreter::parse_source_file(NonnullRefPtr<Core::File> file)
{
    Vector<size_t> loop_pairs;
    while (!file->eof()) {
        auto program_chunk = file->read(1024);
        if (file->has_error()) {
            outln("Error: Failed to read {}: {}", file, file->error_string());
            exit(1);
        }
        for (auto c : program_chunk.bytes()) {
            switch (c) {
            case '+':
                m_program.append(Instruction::IncrementCell);
                break;
            case '-':
                m_program.append(Instruction::DecrementCell);
                break;
            case '<':
                m_program.append(Instruction::DecrementPointer);
                break;
            case '>':
                m_program.append(Instruction::IncrementPointer);
                break;
            case '.':
                m_program.append(Instruction::PutChar);
                break;
            case ',':
                m_program.append(Instruction::GetChar);
                break;
            case '[':
                loop_pairs.append(m_program.size());
                m_program.append(Instruction::BeginLoop);
                break;
            case ']': {
                if (loop_pairs.is_empty()) {
                    outln("Error: Unmatched ]");
                    exit(1);
                }
                auto corresponding_start = loop_pairs.take_last();
                m_loop_pairs.set(corresponding_start, m_program.size());
                m_loop_pairs.set(m_program.size(), corresponding_start);
                m_program.append(Instruction::EndLoop);
                break;
            }
            default:
                break;
            }
        }
    }
    if (!loop_pairs.is_empty()) {
        outln("Error: Unmatched [");
        exit(1);
    }
    file->close();
}

void BrainFuckInterpreter::exec()
{
    Vector<u8> data;
    data.resize(30000);
    for (auto& c : data)
        c = 0;
    size_t instruction_pointer = 0, data_pointer = 0;
    while (instruction_pointer < m_program.size()) {
        auto ins = m_program[instruction_pointer];
        auto next_instruction = instruction_pointer + 1;
        switch (ins) {
        case Instruction::IncrementCell:
            ++data[data_pointer];
            break;
        case Instruction::DecrementCell:
            --data[data_pointer];
            break;
        case Instruction::IncrementPointer:
            ++data_pointer;
            break;
        case Instruction::DecrementPointer:
            --data_pointer;
            break;
        case Instruction::GetChar:
            data[data_pointer] = getchar();
            break;
        case Instruction::PutChar:
            putchar(data[data_pointer]);
            break;
        case Instruction::BeginLoop:
            if (!data[data_pointer])
                next_instruction = m_loop_pairs.get(instruction_pointer).value() + 1;
            break;
        case Instruction::EndLoop:
            next_instruction = m_loop_pairs.get(instruction_pointer).value();
            break;
        }
        instruction_pointer = next_instruction;
    }
}

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String path;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Brainfuck Interpreter");
    args_parser.add_positional_argument(path, "Program path", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    auto result = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (result.is_error()) {
        perror("open");
        return 1;
    }
    BrainFuckInterpreter interpreter;
    interpreter.parse_source_file(result.value());

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    interpreter.exec();

    return 0;
}
