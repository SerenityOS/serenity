#include "Ext2FileSystem.h"
#include "FileBackedBlockDevice.h"
#include "VirtualFileSystem.h"
#include "FileHandle.h"
#include "SyntheticFileSystem.h"
#include "ZeroDevice.h"
#include "NullDevice.h"
#include <cstring>
#include <AK/SimpleMalloc.h>
#include <AK/kmalloc.h>

static RetainPtr<FileSystem> makeFileSystem(const char* imagePath);

int main(int c, char** v)
{
    const char* filename = "small.fs";
    if (c >= 2)
        filename = v[1];

    VirtualFileSystem vfs;

    auto zero = make<ZeroDevice>();
    vfs.registerCharacterDevice(1, 5, *zero);

    auto null = make<NullDevice>();
    vfs.registerCharacterDevice(1, 3, *null);

    if (!vfs.mountRoot(makeFileSystem(filename))) {
        printf("Failed to mount root :(\n");
        return 1;
    }

    //auto newFile = vfs.create("/empty");
    //printf("vfs.create: %p\n", newFile.ptr());
    //return 0;

    if (!strcmp(v[0], "./vcat")) {
        auto handle = vfs.open(v[2]);
        if (!handle) {
            printf("failed to open %s inside fs image\n", v[2]);
            return 1;
        }
        auto contents = handle->read();

        FILE* fout = fopen(v[3], "w");
        if (!fout) {
            printf("failed to open %s for output\n", v[3]);
            return 1;
        }
        fwrite(contents.pointer(), sizeof(char), contents.size(), fout);
        fclose(fout);
        return 0;
    }

    auto synthfs = SyntheticFileSystem::create();
    bool success = static_cast<FileSystem&>(*synthfs).initialize();
    printf("synth->initialize(): returned %u\n", success);

    vfs.mount(std::move(synthfs), "/syn");

    vfs.listDirectory("/");
    printf("list /syn:\n");
    vfs.listDirectory("/syn");

#if 0
    auto handle = vfs.open("/home/andreas/../../home/./andreas/./file2");
    printf("handle = %p\n", handle.ptr());
    ASSERT(handle);

    auto contents = handle->read();
    ASSERT(contents);

    printf("contents: '%s'\n", contents->pointer());
#endif

    String currentDirectory = "/";

    while (true) {
        char cmdbuf[256];
        printf("::>");
        fflush(stdout);
        fgets(cmdbuf, sizeof(cmdbuf), stdin);

        if (cmdbuf[strlen(cmdbuf) - 1] == '\n')
            cmdbuf[strlen(cmdbuf) - 1] = '\0';

        String command = cmdbuf;
        auto parts = command.split(' ');

        if (parts.isEmpty())
            continue;

        String cmd = parts[0];

        if (cmd == "q")
            break;

        if (cmd == "pwd") {
            printf("%s\n", currentDirectory.characters());
            continue;
        }

        if (cmd == "ls") {
            vfs.listDirectory(currentDirectory);
            continue;
        }

        if (cmd == "lr") {
            vfs.listDirectoryRecursively(currentDirectory);
            continue;
        }

        if (cmd == "cd" && parts.size() > 1) {
            char buf[1024];
            sprintf(buf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            if (vfs.isDirectory(buf)) {
                currentDirectory = buf;
            } else {
                printf("No such directory: %s\n", buf);
            }
            continue;
        }

        if (cmd == "mt" && parts.size() > 1) {
            char buf[1024];
            sprintf(buf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            vfs.touch(buf);
            continue;
        }


        if (cmd == "cat" && parts.size() > 1) {
            char pathbuf[1024];
            sprintf(pathbuf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            auto handle = vfs.open(pathbuf);
            if (!handle) {
                printf("failed to open %s\n", pathbuf);
                continue;
            }
            auto contents = handle->read();
            fwrite(contents.pointer(), sizeof(char), contents.size(), stdout);
            continue;
        }

        if (cmd == "ma") {
            SimpleMalloc::dump();
            continue;
        }
    }

    return 0;
}

RetainPtr<FileSystem> makeFileSystem(const char* imagePath)
{
    auto fsImage = FileBackedBlockDevice::create(imagePath, 512);
    if (!fsImage->isValid()) {
        fprintf(stderr, "Failed to open fs image file '%s'\n", imagePath);
        exit(1);
    }
    auto ext2 = Ext2FileSystem::create(std::move(fsImage));

    bool success = static_cast<FileSystem&>(*ext2).initialize();
    printf("ext2->initialize(): returned %u\n", success);
    return ext2;
}
