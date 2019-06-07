#include "ELFLoader.h"
#include <AK/kstdio.h>
#include <AK/QuickSort.h>

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
    m_image.for_each_program_header([&] (const ELFImage::ProgramHeader& program_header) {
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
                String::format("elf-alloc-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "")
            );
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
                String::format("elf-map-%s%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "", program_header.is_executable() ? "x" : "")
            );
        }
    });
    return !failed;
}

char* ELFLoader::symbol_ptr(const char* name)
{
    char* found_ptr = nullptr;
    m_image.for_each_symbol([&] (const ELFImage::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        if (strcmp(symbol.name(), name))
            return IterationDecision::Continue;
        if (m_image.is_executable())
            found_ptr = (char*)symbol.value();
        else
            ASSERT_NOT_REACHED();
        return IterationDecision::Abort;
    });
    return found_ptr;
}

String ELFLoader::symbolicate(dword address) const
{
    if (m_sorted_symbols.is_empty()) {
        m_sorted_symbols.ensure_capacity(m_image.symbol_count());
        m_image.for_each_symbol([this] (auto& symbol) {
            m_sorted_symbols.append({ symbol.value(), symbol.name() });
            return IterationDecision::Continue;
        });
        quick_sort(m_sorted_symbols.begin(), m_sorted_symbols.end(), [] (auto& a, auto& b) {
            return a.address < b.address;
        });
    }

    for (int i = 0; i < m_sorted_symbols.size(); ++i) {
        if (m_sorted_symbols[i].address > address) {
            if (i == 0)
                return "!!";
            auto& symbol = m_sorted_symbols[i - 1];
            return String::format("%s +%u", symbol.name, address - symbol.address);
        }
    }
    return "??";
}
