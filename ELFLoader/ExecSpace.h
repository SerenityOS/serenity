#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class ELFLoader;

class ExecSpace {
public:
    struct Area {
        Area(String&& n, dword o, char* m, unsigned s, LinearAddress l)
            : name(move(n))
            , offset(o)
            , memory(m)
            , size(s)
            , laddr(l)
        {
        }

        String name;
        dword offset { 0 };
        char* memory { 0 };
        unsigned size { 0 };
        LinearAddress laddr;
    };

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

    Function<void*(const String&, size_t)> hookableAlloc;

#ifdef SERENITY
    bool loadELF(ByteBuffer&&);
#else
    bool loadELF(MappedFile&&);
#endif

    char* symbolPtr(const char* name);

    char* allocateArea(String&& name, unsigned size, dword offset, LinearAddress);
    void addSymbol(String&& name, char* ptr, unsigned size);

    void allocateUniverse(size_t);

    void forEachArea(Function<void(const String& name, dword offset, size_t size, LinearAddress)>);

private:
    void initializeBuiltins();

    Vector<OwnPtr<Area>> m_areas;
    HashMap<String, PtrAndSize> m_symbols;
    char* m_universe { nullptr };
};

