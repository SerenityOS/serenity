#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include "ELFImage.h"

class ELFLoader {
public:
    ELFLoader(ByteBuffer&&);
    ~ELFLoader();

    bool load();
    Function<void*(LinearAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    char* symbol_ptr(const char* name);
    void add_symbol(String&& name, char* ptr, unsigned size);
    bool allocate_section(LinearAddress, size_t, size_t alignment, bool is_readable, bool is_writable);

private:
    bool layout();
    bool performRelocations();
    void exportSymbols();
    void* lookup(const ELFImage::Symbol&);
    char* areaForSection(const ELFImage::Section&);
    char* areaForSectionName(const char*);

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
    HashMap<String, PtrAndSize> m_symbols;
    HashMap<String, char*> m_sections;
    OwnPtr<ELFImage> m_image;
};

