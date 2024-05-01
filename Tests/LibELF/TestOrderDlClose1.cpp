/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/File.h>
#include <dlfcn.h>
#include <unistd.h>

[[gnu::destructor]] static void fini()
{
    outln("TestOrderDlClose1.cpp:fini");
}

int main()
{
    {
        outln("===== simple =====");
        outln("main:1");
        void* lib2 = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
        VERIFY(lib2 != nullptr);
        outln("main:2");
        auto f = reinterpret_cast<char const* (*)()>(dlsym(lib2, "_Z1fv"));
        VERIFY(f != nullptr);
        outln("f() = {}", f());
        outln("main:3");
        dlclose(lib2);
        outln("main:4");
    }

#ifdef AK_OS_SERENITY
    auto file = MUST(Core::File::open("/proc/self/vm"sv, Core::File::OpenMode::Read));
    ByteBuffer map = MUST(file->read_until_eof());
    StringView string_contents { map.bytes() };

    VERIFY(!string_contents.contains("TestOrderLib1"sv));
    VERIFY(!string_contents.contains("TestOrderLib2"sv));
#endif

    {
        outln("===== dlopen refcounts =====");
        outln("main:1");
        void* lib2 = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
        VERIFY(lib2 != nullptr);
        outln("main:2");
        void* lib1 = dlopen("libTestOrderLib1.so", RTLD_LAZY | RTLD_GLOBAL);
        VERIFY(lib1 != nullptr);
        outln("main:3");
        void* lib2_again = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
        VERIFY(lib2_again != nullptr);
        VERIFY(lib2 == lib2_again);
        outln("main:4");
        dlclose(lib2);
        outln("main:5");
        dlclose(lib2_again);
        outln("main:6");
        dlclose(lib1);
        outln("main:7");
    }
}
