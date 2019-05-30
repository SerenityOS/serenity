#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#if defined(KERNEL)
#include <Kernel/LinearAddress.h>
#endif
#include <AK/ELF/ELFImage.h>

class ELFLoader {
public:
    explicit ELFLoader(const byte*);
    ~ELFLoader();

    bool load();
#if defined(KERNEL)
    Function<void*(LinearAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    Function<void*(LinearAddress, size_t, size_t, size_t, bool r, bool w, bool x, const String&)> map_section_hook;
    LinearAddress entry() const { return m_image.entry(); }
#endif
    char* symbol_ptr(const char* name);

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

    struct SortedSymbol {
        dword address;
        const char* name;
    };
    mutable Vector<SortedSymbol> m_sorted_symbols;
};

