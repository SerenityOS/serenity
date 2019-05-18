#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/LinearAddress.h>
#include <Kernel/ELF/ELFImage.h>

class ELFLoader {
public:
    explicit ELFLoader(const byte*);
    ~ELFLoader();

    bool load();
    Function<void*(LinearAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    Function<void*(LinearAddress, size_t, size_t, size_t, bool, bool, const String&)> map_section_hook;
    char* symbol_ptr(const char* name);
    LinearAddress entry() const { return m_image.entry(); }

    bool has_symbols() const { return m_image.symbol_count(); }

    String symbolicate(dword address) const;

private:
    bool layout();
    bool perform_relocations();
    void* lookup(const ELFImage::Symbol&);
    char* area_for_section(const ELFImage::Section&);
    char* area_for_section_name(const char*);

    struct PtrAndSize {
        PtrAndSize() { }
        PtrAndSize(char* p, unsigned s)
            : ptr(p)
            , size(s)
        {
        }

        char* ptr { nullptr };
        unsigned size { 0 };
    };
    ELFImage m_image;

    HashMap<String, char*> m_sections;

    struct SortedSymbol {
        dword address;
        const char* name;
    };
    mutable Vector<SortedSymbol> m_sorted_symbols;
};

