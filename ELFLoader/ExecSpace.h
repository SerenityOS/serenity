#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/String.h>
#include "types.h"

class ELFLoader;

class ExecSpace {
public:
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

    ExecSpace();
    ~ExecSpace();

    Function<void*(LinearAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;

    bool loadELF(ByteBuffer&&);

    char* symbolPtr(const char* name);

    void addSymbol(String&& name, char* ptr, unsigned size);

    bool allocate_section(LinearAddress, size_t, size_t alignment, bool is_readable, bool is_writable);

private:
    Vector<char*> m_allocated_regions;
    HashMap<String, PtrAndSize> m_symbols;
};

