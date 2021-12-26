#include "ELFLoader.h"
#include <AK/QuickSort.h>
#include <AK/kstdio.h>

#ifdef KERNEL
#include <Kernel/VM/MemoryManager.h>
#endif

//#define ELFLOADER_DEBUG

ELFLoader::ELFLoader(const byte* buffer)
    : m_image(buffer)
{
}

ELFLoader::~ELFLoader()
{
}

bool ELFLoader::load()
{
#ifdef ELFLOADER_DEBUG
    m_image.dump();
#endif
    if (!m_image.is_valid())
        return false;

    if (!layout())
        return false;

    return true;
}

bool ELFLoader::layout()
{
    bool failed = false;
    m_image.for_each_program_header([&](const ELFImage::ProgramHeader& program_header) {
        if (program_header.type() != PT_LOAD)
            return;
#ifdef ELFLOADER_DEBUG
        kprintf("PH: L%x %u r:%u w:%u\n", program_header.vaddr().get(), program_header.size_in_memory(), program_header.is_readable(), program_header.is_writable());
#endif
        if (program_header.is_writable()) {
            alloc_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.is_readable(),
                program_header.is_writable(),
                String::format("elf-alloc-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : ""));
            memcpy(program_header.vaddr().as_ptr(), program_header.raw_data(), program_header.size_in_image());
        } else {
            map_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.offset(),
                program_header.is_readable(),
                program_header.is_writable(),
                program_header.is_executable(),
                String::format("elf-map-%s%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "", program_header.is_executable() ? "x" : ""));
        }
    });
    return !failed;
}

char* ELFLoader::symbol_ptr(const char* name)
{
    char* found_ptr = nullptr;
    m_image.for_each_symbol([&](const ELFImage::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        if (strcmp(symbol.name(), name))
            return IterationDecision::Continue;
        if (m_image.is_executable())
            found_ptr = (char*)symbol.value();
        else
            ASSERT_NOT_REACHED();
        return IterationDecision::Break;
    });
    return found_ptr;
}

String ELFLoader::symbolicate(dword address) const
{
    SortedSymbol* sorted_symbols = nullptr;
#ifdef KERNEL
    if (!m_sorted_symbols_region) {
        m_sorted_symbols_region = MM.allocate_kernel_region(PAGE_ROUND_UP(m_image.symbol_count() * sizeof(SortedSymbol)), "Sorted symbols");
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
        dbgprintf("sorted_symbols: %p\n", sorted_symbols);
        size_t index = 0;
        m_image.for_each_symbol([&](auto& symbol) {
            sorted_symbols[index++] = { symbol.value(), symbol.name() };
            return IterationDecision::Continue;
        });
        quick_sort(sorted_symbols, sorted_symbols + m_image.symbol_count(), [](auto& a, auto& b) {
            return a.address < b.address;
        });
    } else {
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
    }
#else
    if (m_sorted_symbols.is_empty()) {
        m_sorted_symbols.ensure_capacity(m_image.symbol_count());
        m_image.for_each_symbol([this](auto& symbol) {
            m_sorted_symbols.append({ symbol.value(), symbol.name() });
            return IterationDecision::Continue;
        });
        quick_sort(m_sorted_symbols.begin(), m_sorted_symbols.end(), [](auto& a, auto& b) {
            return a.address < b.address;
        });
    }
    sorted_symbols = m_sorted_symbols.data();
#endif

    for (size_t i = 0; i < m_image.symbol_count(); ++i) {
        if (sorted_symbols[i].address > address) {
            if (i == 0)
                return "!!";
            auto& symbol = sorted_symbols[i - 1];
            return String::format("%s +%u", symbol.name, address - symbol.address);
        }
    }
    return "??";
}
