#include "ELFLoader.h"
#include <AK/Demangle.h>
#include <AK/QuickSort.h>
#include <AK/kstdio.h>

#ifdef KERNEL
#include <Kernel/VM/MemoryManager.h>
#endif

//#define ELFLOADER_DEBUG

ELFLoader::ELFLoader(const u8* buffer)
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
        if (program_header.type() == PT_TLS) {
#ifdef KERNEL
            auto* tls_image = tls_section_hook(program_header.size_in_memory(), program_header.alignment());
            if (!tls_image) {
                failed = true;
                return;
            }
            memcpy(tls_image, program_header.raw_data(), program_header.size_in_image());
#endif
            return;
        }
        if (program_header.type() != PT_LOAD)
            return;
#ifdef ELFLOADER_DEBUG
        kprintf("PH: V%p %u r:%u w:%u\n", program_header.vaddr().get(), program_header.size_in_memory(), program_header.is_readable(), program_header.is_writable());
#endif
#ifdef KERNEL
        if (program_header.is_writable()) {
            auto* allocated_section = alloc_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.is_readable(),
                program_header.is_writable(),
                String::format("elf-alloc-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : ""));
            if (!allocated_section) {
                failed = true;
                return;
            }
            memcpy(program_header.vaddr().as_ptr(), program_header.raw_data(), program_header.size_in_image());
        } else {
            auto* mapped_section = map_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.offset(),
                program_header.is_readable(),
                program_header.is_writable(),
                program_header.is_executable(),
                String::format("elf-map-%s%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "", program_header.is_executable() ? "x" : ""));
            if (!mapped_section) {
                failed = true;
            }
        }
#endif
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
            found_ptr = (char*)(size_t)symbol.value();
        else
            ASSERT_NOT_REACHED();
        return IterationDecision::Break;
    });
    return found_ptr;
}

String ELFLoader::symbolicate(u32 address, u32* out_offset) const
{
    SortedSymbol* sorted_symbols = nullptr;
#ifdef KERNEL
    if (!m_sorted_symbols_region) {
        m_sorted_symbols_region = MM.allocate_kernel_region(PAGE_ROUND_UP(m_image.symbol_count() * sizeof(SortedSymbol)), "Sorted symbols", Region::Access::Read | Region::Access::Write);
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
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
            if (out_offset) {
                *out_offset = address - symbol.address;
                return demangle(symbol.name);
            }
            return String::format("%s +%u", demangle(symbol.name).characters(), address - symbol.address);
        }
    }
    return "??";
}
