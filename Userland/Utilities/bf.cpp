/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJIT/InstructionBuffer.h>
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

Vector<Instruction> parse_source_file(NonnullRefPtr<Core::File> file);

Vector<Instruction> parse_source_file(NonnullRefPtr<Core::File> file)
{
    Vector<Instruction> instructions;
    while (!file->eof()) {
        auto program_chunk = file->read(1024);
        if (file->has_error()) {
            outln("Error: Failed to read {}: {}", file, file->error_string());
            exit(1);
        }
        for (auto c : program_chunk.bytes()) {
            switch (c) {
            case '+':
                instructions.append(Instruction::IncrementCell);
                break;
            case '-':
                instructions.append(Instruction::DecrementCell);
                break;
            case '<':
                instructions.append(Instruction::DecrementPointer);
                break;
            case '>':
                instructions.append(Instruction::IncrementPointer);
                break;
            case '.':
                instructions.append(Instruction::PutChar);
                break;
            case ',':
                instructions.append(Instruction::GetChar);
                break;
            case '[':
                instructions.append(Instruction::BeginLoop);
                break;
            case ']': {
                instructions.append(Instruction::EndLoop);
                break;
            }
            default:
                break;
            }
        }
    }
    file->close();
    return instructions;
}

class BrainFuckExecutor {
public:
    virtual ~BrainFuckExecutor() { }
    virtual void parse_source_file(NonnullRefPtr<Core::File> file) = 0;
    virtual void exec() = 0;
};

class BrainFuckInterpreter : public BrainFuckExecutor {
public:
    virtual ~BrainFuckInterpreter() override { }
    void parse_source_file(NonnullRefPtr<Core::File> file) override;
    void exec() override;

private:
    Vector<Instruction> m_program {};
    HashMap<u64, u64> m_loop_pairs {};
    Vector<u8> m_data {};
};

void BrainFuckInterpreter::parse_source_file(NonnullRefPtr<Core::File> file)
{
    m_program = ::parse_source_file(file);
    Vector<size_t> loop_pairs;
    for (size_t i = 0; i < m_program.size(); ++i) {
        auto instruction = m_program[i];
        if (instruction == Instruction::BeginLoop) {
            loop_pairs.append(i);
        } else if (instruction == Instruction::EndLoop) {
            if (loop_pairs.is_empty()) {
                outln("Error: Unmatched ]");
                exit(1);
            }
            auto corresponding_start = loop_pairs.take_last();
            m_loop_pairs.set(corresponding_start, i);
            m_loop_pairs.set(i, corresponding_start);
        }
    }
    if (!loop_pairs.is_empty()) {
        outln("Error: Unmatched [");
        exit(1);
    }
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

class BrainFuckJIT : public BrainFuckExecutor {
public:
    virtual ~BrainFuckJIT() override { }
    void parse_source_file(NonnullRefPtr<Core::File> file) override;
    void exec() override;

private:
    Vector<Instruction> m_program {};
    JIT::InstructionBuffer m_buffer { "Brainfuck JIT Region" };
    JIT::JITLabel m_program_start {};
    Vector<u8> m_data {};
};

static void platform_putchar(i32 c)
{
    putchar(c);
}

static char platform_getchar()
{
    return getchar();
}

// Register model
// ebx = current data pointer
// cl is where we usually load the value of the current cell
// edx = &getchar
// edi = &putchar
void BrainFuckJIT::parse_source_file(NonnullRefPtr<Core::File> file)
{
    m_program = ::parse_source_file(file);
    m_program_start = m_buffer.get_current_offset();
    // Initialize the data array
    m_data.resize(30000);
    for (auto& c : m_data)
        c = 0;

    void* start_of_memory = &m_data[0];

    // Function prelude
    m_buffer.append_bytes({
        // push %ebp
        0x55,
        // mov %ebp, %esp
        0x89,
        0xe5,
        // push %ebx
        0x53,
        // push %edi
        0x57,
        // mov %ebx, start_of_memory
        0xbb,
    });
    m_buffer.append_le(bit_cast<u32>(start_of_memory));
    m_buffer.append_bytes({
        // mov %edi, putchar
        0xbf,
    });
    m_buffer.append_le(bit_cast<u32>(&platform_putchar));
    m_buffer.append_bytes({
        // mov %edx, getchar
        0xba,
    });
    m_buffer.append_le(bit_cast<u32>(&platform_getchar));

    Vector<JIT::JITLabel> loop_starts;
    Vector<JIT::JITPatchLocation> loop_start_patch_locations;
    for (size_t i = 0; i < m_program.size(); ++i) {
        auto instruction = m_program[i];
        switch (instruction) {
        case Instruction::IncrementPointer: {
            // inc %ebx
            m_buffer.append_bytes({ 0x43 });
            break;
        }
        case Instruction::DecrementPointer: {
            // dec %ebx
            m_buffer.append_bytes({ 0x4b });
            break;
        }
        case Instruction::IncrementCell: {
            m_buffer.append_bytes({
                // mov %cl, [%ebx]
                0x8a,
                0x0b,
                // inc %cl
                0xfe,
                0xc1,
                // mov [%ebx], %cl
                0x88,
                0x0b,
            });
            break;
        }
        case Instruction::DecrementCell: {
            m_buffer.append_bytes({
                // mov %cl, [%ebx]
                0x8a,
                0x0b,
                // dec %cl
                0xfe,
                0xc9,
                // mov [%ebx], %cl
                0x88,
                0x0b,
            });
            break;
        }
        case Instruction::PutChar: {
            m_buffer.append_bytes({
                // push %edx
                0x52,
                // mov %cl, [%ebx]
                0x8a,
                0x0b,
                // push %ecx
                0x51,
                // call %edi
                0xff,
                0xd7,
                // add %esp, 4
                0x83,
                0xc4,
                0x04,
                // pop %edx
                0x5a,
            });
            break;
        }
        case Instruction::GetChar: {
            m_buffer.append_bytes({
                // push %edx
                0x52,
                // call %edx
                0xff,
                0xd2,
                // mov [%ebx], %al
                0x88,
                0x03,
                // pop %edx
                0x5a,
            });
            break;
        }
        case Instruction::BeginLoop: {
            // What we do here is:
            // Store the current offset in loop_starts

            // load cell
            // check if cell is 0
            // if it is, jump to 0x00000000 (patched later in the code generator)
            loop_starts.append(m_buffer.get_current_offset());
            m_buffer.append_bytes({
                // mov %cl, [%ebx]
                0x8a,
                0x0b,
                // test cl, cl
                0x84,
                0xc9,
            });
            loop_start_patch_locations.append(m_buffer.get_relative_patch_location(2));
            m_buffer.append_bytes({
                // jz 0x00000000
                0x0f,
                0x84,
            });
            m_buffer.append_bytes({ 0x00, 0x00, 0x00, 0x00 });
            break;
        }
        case Instruction::EndLoop: {
            if (loop_starts.is_empty()) {
                outln("Error: Unmatched ]");
                exit(1);
            }
            auto loop_start = loop_starts.take_last();
            auto forward_jump_loc = loop_start_patch_locations.take_last();
            outln("Got from stack: loop_start = {:x}, forward_jump_loc = {:x}", loop_start.value(), forward_jump_loc.value());
            auto backwards_jump_start_address = m_buffer.get_current_offset();
            m_buffer.append_bytes({
                // jmp (start of loop)
                0xe9,
            });
            m_buffer.append_le((u32)(loop_start - backwards_jump_start_address - 5).value());
            auto end_of_loop = m_buffer.get_current_offset();
            m_buffer.write_le(forward_jump_loc, (u32)(end_of_loop.value() - forward_jump_loc.value() - 4));
            break;
        }
        }
    }
    if (!loop_starts.is_empty()) {
        outln("Error: Unmatched [");
        exit(1);
    }
    // Function epilogue
    m_buffer.append_bytes({
        // mov %esp, %ebp
        0x89,
        0xec,
        // pop ebp
        0x5d,
        // pop %ebi
        0x5f,
        // pop %ebx
        0x5b,
        // ret
        0xc3,
    });
}

void BrainFuckJIT::exec()
{
    m_buffer.dump_encoded_instructions();
    m_buffer.finalize();
    m_buffer.enter_at_offset(m_program_start);
}

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath prot_exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool use_jit = false;
    String path;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Brainfuck Interpreter");
    args_parser.add_positional_argument(path, "Program path", "path", Core::ArgsParser::Required::Yes);
    args_parser.add_option(use_jit, "Enable the jit", "use-jit", 'j');
    args_parser.parse(argc, argv);

    auto result = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (result.is_error()) {
        perror("open");
        return 1;
    }
    BrainFuckExecutor* executor;
    if (use_jit) {
        executor = new BrainFuckJIT();
    } else {
        executor = new BrainFuckInterpreter();
    }
    executor->parse_source_file(result.value());

    if (pledge("stdio prot_exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    executor->exec();

    return 0;
}
