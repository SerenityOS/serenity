#include <AK/Demangle.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <LibELF/ELFLoader.h>

namespace Profiling {

static KBufferImpl* s_profiling_buffer;
static size_t s_slot_count;
static size_t s_next_slot_index;
static Process* s_process;

void start(Process& process)
{
    s_process = &process;

    if (!s_profiling_buffer) {
        s_profiling_buffer = RefPtr<KBufferImpl>(KBuffer::create_with_size(8 * MB).impl()).leak_ref();
        s_slot_count = s_profiling_buffer->size() / sizeof(Sample);
    }

    s_next_slot_index = 0;
}

static Sample& sample_slot(size_t index)
{
    return ((Sample*)s_profiling_buffer->data())[index];
}

Sample& next_sample_slot()
{
    auto& slot = sample_slot(s_next_slot_index++);
    if (s_next_slot_index >= s_slot_count)
        s_next_slot_index = 0;
    return slot;
}

static void symbolicate(Sample& stack)
{
    auto& process = *s_process;
    ProcessPagingScope paging_scope(process);
    struct RecognizedSymbol {
        u32 address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol, max_stack_frame_count> recognized_symbols;
    for (size_t i = 1; i < max_stack_frame_count; ++i) {
        if (stack.frames[i] == 0)
            break;
        recognized_symbols.append({ stack.frames[i], ksymbolicate(stack.frames[i]) });
    }

    size_t i = 1;
    for (auto& symbol : recognized_symbols) {
        if (!symbol.address)
            break;
        auto& symbol_string_slot = stack.symbolicated_frames[i++];
        if (!symbol.ksym) {
            if (!Scheduler::is_active() && process.elf_loader() && process.elf_loader()->has_symbols())
                symbol_string_slot = String::format("%s", process.elf_loader()->symbolicate(symbol.address).characters());
            else
                symbol_string_slot = String::empty();
            continue;
        }
        unsigned offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096)
            symbol_string_slot = String::empty();
        else
            symbol_string_slot = String::format("%s +%u", demangle(symbol.ksym->name).characters(), offset);
    }
}

void stop()
{
    for (size_t i = 0; i < s_next_slot_index; ++i) {
        auto& stack = sample_slot(i);
        symbolicate(stack);
    }

    s_process = nullptr;
}

void for_each_sample(Function<void(Sample&)> callback)
{
    for (size_t i = 0; i < s_next_slot_index; ++i) {
        auto& sample = sample_slot(i);
        callback(sample);
    }
}

}
