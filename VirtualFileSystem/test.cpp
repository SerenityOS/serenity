#include "Ext2FileSystem.h"
#include "FileBackedDiskDevice.h"
#include "VirtualFileSystem.h"
#include "FileDescriptor.h"
#include "SyntheticFileSystem.h"
#include "ZeroDevice.h"
#include "NullDevice.h"
#include "FullDevice.h"
#include "RandomDevice.h"
#include <cstring>
#include <AK/FileSystemPath.h>
#include <AK/SimpleMalloc.h>
#include <AK/StdLib.h>
#include <AK/kmalloc.h>
#include <AK/ktime.h>

static RetainPtr<FS> makeFileSystem(const char* imagePath);

int main(int c, char** v)
{
    const char* filename = "small.fs";
    if (c >= 2)
        filename = v[1];

    VFS::initialize_globals();

    VFS vfs;

    auto zero = make<ZeroDevice>();
    vfs.register_character_device(*zero);

    auto null = make<NullDevice>();
    vfs.register_character_device(*null);

    auto full = make<FullDevice>();
    vfs.register_character_device(*full);

    auto random = make<RandomDevice>();
    vfs.register_character_device(*random);

    if (!vfs.mount_root(makeFileSystem(filename))) {
        printf("Failed to mount root :(\n");
        return 1;
    }

#if 0
    auto newFile = vfs.create("/empty");
    printf("vfs.create: %p\n", newFile.ptr());
#endif
#if 1
    auto newDir = vfs.mkdir("/mydir");
    printf("vfs.mkdir: %p\n", newDir.ptr());
#endif
    //return 0;

    if (!strcmp(v[0], "./vcat")) {
        int error;
        auto descriptor = vfs.open(v[2], error);
        if (!descriptor) {
            printf("failed to open %s inside fs image\n", v[2]);
            return 1;
        }
        auto contents = descriptor->read_entire_file();

        FILE* fout = fopen(v[3], "w");
        if (!fout) {
            printf("failed to open %s for output\n", v[3]);
            return 1;
        }
        fwrite(contents.pointer(), sizeof(char), contents.size(), fout);
        fclose(fout);
        return 0;
    }

    auto synthfs = SynthFS::create();
    bool success = static_cast<FS&>(*synthfs).initialize();
    printf("synth->initialize(): returned %u\n", success);

    vfs.mount(std::move(synthfs), "/syn");

    vfs.listDirectory(".", vfs.root()->inode);
    printf("list /syn:\n");
    vfs.listDirectory("/syn", vfs.root()->inode);

#if 0
    auto descriptor = vfs.open("/home/andreas/../../home/./andreas/./file2");
    printf("descriptor = %p\n", handle.ptr());
    ASSERT(descriptor);

    auto contents = descriptor->readEntireFile();
    ASSERT(contents);

    printf("contents: '%s'\n", contents->pointer());
#endif

    String currentDirectory = "/";

    auto cwd = vfs.root()->inode;

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
            vfs.listDirectory(".", cwd);
            continue;
        }

        if (cmd == "lr") {
            vfs.listDirectoryRecursively(".", cwd);
            continue;
        }

        if (cmd == "cd" && parts.size() > 1) {
            char buf[4096];
            sprintf(buf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            FileSystemPath new_path(buf);
            if (new_path.string() == "/") {
                cwd = vfs.root()->inode;
                continue;
            }
            int error;
            auto new_cwd = vfs.open(new_path.string(), error, 0, cwd);
            if (new_cwd && new_cwd->is_directory()) {
                currentDirectory = new_path.string();
                cwd = new_cwd->metadata().inode;
            } else {
                printf("No such directory: %s\n", parts[1].characters());
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
            int error;
            auto descriptor = vfs.open(buf, error);
            if (!descriptor) {
                printf("Can't open '%s' :(\n", buf);
                continue;
            }
            Unix::stat st;
            int rc = descriptor->stat(&st);
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
            int error;
            auto descriptor = vfs.open(pathbuf, error);
            if (!descriptor) {
                printf("failed to open %s\n", pathbuf);
                continue;
            }
            auto contents = descriptor->read_entire_file();
            fwrite(contents.pointer(), sizeof(char), contents.size(), stdout);
            continue;
        }

        if (cmd == "kat" && parts.size() > 1) {
            char pathbuf[1024];
            sprintf(pathbuf, "%s/%s", currentDirectory.characters(), parts[1].characters());
            int error;
            auto descriptor = vfs.open(pathbuf, error);
            if (!descriptor) {
                printf("failed to open %s\n", pathbuf);
                continue;
            }
            ssize_t nread;
            byte buffer[512];
            for (;;) {
                nread = descriptor->read(buffer, sizeof(buffer));
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

RetainPtr<FS> makeFileSystem(const char* imagePath)
{
    auto fsImage = FileBackedDiskDevice::create(imagePath, 512);
    if (!fsImage->is_valid()) {
        fprintf(stderr, "Failed to open fs image file '%s'\n", imagePath);
        exit(1);
    }
    auto ext2 = Ext2FS::create(std::move(fsImage));

    bool success = static_cast<FS&>(*ext2).initialize();
    printf("ext2->initialize(): returned %u\n", success);
    return ext2;
}
