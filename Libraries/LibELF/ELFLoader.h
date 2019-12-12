#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibELF/ELFImage.h>

#ifdef KERNEL
#include <Kernel/VM/VirtualAddress.h>
class Region;
#endif

class ELFLoader {
public:
    explicit ELFLoader(const u8*);
    ~ELFLoader();

    bool load();
#if defined(KERNEL)
    Function<void*(VirtualAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    Function<void*(size_t, size_t)> tls_section_hook;
    Function<void*(VirtualAddress, size_t, size_t, size_t, bool r, bool w, bool x, const String&)> map_section_hook;
    VirtualAddress entry() const { return m_image.entry(); }
#endif
    char* symbol_ptr(const char* name);

    bool has_symbols() const { return m_image.symbol_count(); }

    String symbolicate(u32 address, u32* offset = nullptr) const;

private:
    bool layout();
    bool perform_relocations();
    void* lookup(const ELFImage::Symbol&);
    char* area_for_section(const ELFImage::Section&);
    char* area_for_section_name(const char*);

    struct PtrAndSize {
        PtrAndSize() {}
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
        u32 address;
        const char* name;
    };
#ifdef KERNEL
    mutable OwnPtr<Region> m_sorted_symbols_region;
#else
    mutable Vector<SortedSymbol> m_sorted_symbols;
#endif
};
