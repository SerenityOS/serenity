#include "Ext2FileSystem.h"
#include "FileBackedBlockDevice.h"
#include "VirtualFileSystem.h"
#include "FileHandle.h"
#include "SyntheticFileSystem.h"
#include "ZeroDevice.h"
#include "NullDevice.h"
#include "FullDevice.h"
#include "RandomDevice.h"
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

    auto full = make<FullDevice>();
    vfs.registerCharacterDevice(1, 7, *full);

    auto random = make<RandomDevice>();
    vfs.registerCharacterDevice(1, 8, *random);

    if (!vfs.mountRoot(makeFileSystem(filename))) {
        printf("Failed to mount root :(\n");
        return 1;
    }

#if 1
    auto newFile = vfs.create("/empty");
    printf("vfs.create: %p\n", newFile.ptr());
#endif
    //return 0;

    if (!strcmp(v[0], "./vcat")) {
        auto handle = vfs.open(v[2]);
        if (!handle) {
            printf("failed to open %s inside fs image\n", v[2]);
            return 1;
        }
        auto contents = handle->readEntireFile();

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

    auto contents = handle->readEntireFile();
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

        if (cmd == "stat" && parts.size() > 1) {
            char buf[1024];
            sprintf(buf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            auto handle = vfs.open(buf);
            if (!handle) {
                printf("Can't open '%s' :(\n", buf);
                continue;
            }
            Unix::stat st;
            int rc = handle->stat(&st);
            if (rc < 0) {
                printf("stat failed: %d\n", rc);
                continue;
            }
            printf("st_dev:     %u\n", st.st_dev);
            printf("st_ino:     %u\n", st.st_ino);
            printf("st_mode:    %o\n", st.st_mode);
            printf("st_nlink:   %u\n", st.st_nlink);
            printf("st_uid:     %u\n", st.st_uid);
            printf("st_gid:     %u\n", st.st_gid);
            printf("st_rdev:    %u\n", st.st_rdev);
            printf("st_size:    %lld\n", st.st_size);
            printf("st_blksize: %u\n", st.st_blksize);
            printf("st_blocks:  %u\n", st.st_blocks);
            printf("st_atime:   %u - %s", st.st_atime, ctime(&st.st_atime));
            printf("st_mtime:   %u - %s", st.st_mtime, ctime(&st.st_mtime));
            printf("st_ctime:   %u - %s", st.st_ctime, ctime(&st.st_ctime));
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
            auto contents = handle->readEntireFile();
            fwrite(contents.pointer(), sizeof(char), contents.size(), stdout);
            continue;
        }

        if (cmd == "kat" && parts.size() > 1) {
            char pathbuf[1024];
            sprintf(pathbuf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            auto handle = vfs.open(pathbuf);
            if (!handle) {
                printf("failed to open %s\n", pathbuf);
                continue;
            }
            ssize_t nread;
            byte buffer[512];
            for (;;) {
                nread = handle->read(buffer, sizeof(buffer));
                if (nread <= 0)
                    break;
                fwrite(buffer, 1, nread, stdout);
            }
            if (nread < 0)
                printf("ERROR: %d\n", nread);
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
