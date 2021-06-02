/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJIT/InstructionBuffer.h>
#include <LibJIT/X86Assembler.h>
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
    JIT::X86Assembler assembler(m_buffer);
    m_program = ::parse_source_file(file);
    m_program_start = m_buffer.get_current_offset();
    auto const memory_register = X86::RegisterEBX;
    auto const putchar_register = X86::RegisterEDI;
    auto const getchar_register = X86::RegisterEDX;
    auto const scratch_register8 = X86::RegisterCL;
    auto const scratch_register32 = X86::RegisterECX;

    // Initialize the data array
    m_data.resize(30000);
    for (auto& c : m_data)
        c = 0;
    void* start_of_memory = &m_data[0];

    // Function prelude
    assembler.prelude();
    assembler.move<32>(JIT::RegisterIndex(memory_register), JIT::Immediate(bit_cast<u32>(start_of_memory)));
    assembler.move<32>(JIT::RegisterIndex(putchar_register), JIT::Immediate(bit_cast<u32>(&platform_putchar)));
    assembler.move<32>(JIT::RegisterIndex(getchar_register), JIT::Immediate(bit_cast<u32>(&platform_getchar)));

    Vector<JIT::JITLabel> loop_starts;
    Vector<JIT::JITPatchLocation> loop_start_patch_locations;
    for (size_t i = 0; i < m_program.size(); ++i) {
        auto instruction = m_program[i];
        switch (instruction) {
        case Instruction::IncrementPointer: {
            assembler.inc_register32(memory_register);
            break;
        }
        case Instruction::DecrementPointer: {
            assembler.dec_register32(memory_register);
            break;
        }
        case Instruction::IncrementCell: {
            assembler.move<8>(JIT::RegisterIndex(scratch_register8), JIT::DereferencedRegisterIndex(memory_register));
            assembler.inc_register8(scratch_register8);
            assembler.move<8>(JIT::DereferencedRegisterIndex(memory_register), JIT::RegisterIndex(scratch_register8));
            break;
        }
        case Instruction::DecrementCell: {
            assembler.move<8>(JIT::RegisterIndex(scratch_register8), JIT::DereferencedRegisterIndex(memory_register));
            assembler.dec_register8(scratch_register8);
            assembler.move<8>(JIT::DereferencedRegisterIndex(memory_register), JIT::RegisterIndex(scratch_register8));
            break;
        }
        case Instruction::PutChar: {
            assembler.push_register32(getchar_register);
            assembler.move<8>(JIT::RegisterIndex(scratch_register8), JIT::DereferencedRegisterIndex(memory_register));
            assembler.push_register32(scratch_register32);
            assembler.call(putchar_register);
            assembler.add_register32_imm32(X86::RegisterESP, 4);
            assembler.pop_register32(getchar_register);
            break;
        }
        case Instruction::GetChar: {
            assembler.push_register32(getchar_register);
            assembler.call(getchar_register);
            assembler.move<8>(JIT::DereferencedRegisterIndex(memory_register), JIT::RegisterIndex(X86::RegisterAL));
            assembler.pop_register32(getchar_register);
            break;
        }
        case Instruction::BeginLoop: {
            // 1. Load cell
            // 2. Check if cell is 0
            // 3. If it is, jump to 0x00000000 (patched later in the code generator to the end of this loop)
            loop_starts.append(m_buffer.get_current_offset());
            assembler.move<8>(JIT::RegisterIndex(scratch_register8), JIT::DereferencedRegisterIndex(memory_register));
            assembler.test<8>(JIT::RegisterIndex(scratch_register8), JIT::RegisterIndex(scratch_register8));
            auto address_patch_location = assembler.jump_relative_on_condition(JIT::EqualityCondition::Equal, 0);
            loop_start_patch_locations.append(address_patch_location);
            break;
        }
        case Instruction::EndLoop: {
            // Determine start of loop
            // Generate unconditional backwards jump
            // Patch branch instruction at start of loop to point past the end of it
            if (loop_starts.is_empty()) {
                outln("Error: Unmatched ]");
                exit(1);
            }
            auto const loop_start = loop_starts.take_last();
            auto const forward_jump_loc = loop_start_patch_locations.take_last();
            auto const backwards_jump_start_address = m_buffer.get_current_offset();
            assembler.jump_relative((loop_start - backwards_jump_start_address).value());
            auto const end_of_loop = m_buffer.get_current_offset();
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
    assembler.epilogue();
    assembler.ret();
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
