#include <assert.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <mman.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>

#include <AK/FileSystemPath.h>
#include <AK/HashMap.h>
#include <AK/RefPtr.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibELF/ELFDynamicObject.h>

// NOTE: The string here should never include a trailing newline (according to POSIX)
String g_dlerror_msg;

HashMap<String, RefPtr<ELFDynamicObject>> g_elf_objects;

extern "C" {

int dlclose(void*)
{
    g_dlerror_msg = "dlclose not implemented!";
    return -1;
}

char* dlerror()
{
    return const_cast<char*>(g_dlerror_msg.characters());
}

void* dlopen(const char* filename, int flags)
{
    // FIXME: Create a global mutex/semaphore/lock for dlopen/dlclose/dlsym and (?) dlerror
    // FIXME: refcount?

    if (!filename) {
        // FIXME: Return the handle for "the main executable"
        //     The Serenity Kernel will keep a mapping of the main elf binary resident in memory,
        //     But a future dynamic loader might have a different idea/way of letting us access this information
        ASSERT_NOT_REACHED();
    }

    FileSystemPath file_path(filename);

    auto existing_elf_object = g_elf_objects.get(file_path.basename());
    if (existing_elf_object.has_value()) {
        void* referenced_object = existing_elf_object.value().leak_ref();
        return referenced_object;
    }

    int fd = open(filename, O_RDONLY);
    if (!fd) {
        g_dlerror_msg = String::format("Unable to open file %s", filename);
        return nullptr;
    }

    ScopeGuard close_fd_guard([fd]() { close(fd); });

    struct stat file_stats{};

    int ret = fstat(fd, &file_stats);
    if (ret < 0) {
        g_dlerror_msg = String::format("Unable to stat file %s", filename);
        return nullptr;
    }

    auto image = ELFDynamicObject::construct(filename, fd, file_stats.st_size);

    if (!image->is_valid()) {
        g_dlerror_msg = String::format("%s is not a valid ELF dynamic shared object!", filename);
        return nullptr;
    }

    if (!image->load(flags)) {
        g_dlerror_msg = String::format("Failed to load ELF object %s", filename);
        return nullptr;
    }

    g_elf_objects.set(file_path.basename(), move(image));
    g_dlerror_msg = "Successfully loaded ELF object.";

    // we have one refcount already
    return g_elf_objects.get(file_path.basename()).value().ptr();
}

void* dlsym(void* handle, const char* symbol_name)
{
    // FIXME: When called with a NULL handle we're supposed to search every dso in the process... that'll get expensive
    ASSERT(handle);
    auto* dso = reinterpret_cast<ELFDynamicObject*>(handle);
    void* symbol = dso->symbol_for_name(symbol_name);
    if (!symbol) {
        g_dlerror_msg = "Symbol not found";
        return nullptr;
    }
    return symbol;
}

} // extern "C"
