#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <Kernel/VirtualFileSystem.h>
#include <LibC/errno_numbers.h>

RetainPtr<LocalSocket> LocalSocket::create(int type)
{
    return adopt(*new LocalSocket(type));
}

LocalSocket::LocalSocket(int type)
    : Socket(AF_LOCAL, type, 0)
{
    kprintf("%s(%u) LocalSocket{%p} created with type=%u\n", current->name().characters(), current->pid(), type);
}

LocalSocket::~LocalSocket()
{
}

bool LocalSocket::bind(const sockaddr* address, socklen_t address_size, int& error)
{
    if (address_size != sizeof(sockaddr_un)) {
        error = -EINVAL;
        return false;
    }

    if (address->sa_family != AF_LOCAL) {
        error = -EINVAL;
        return false;
    }

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1];
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

    kprintf("%s(%u) LocalSocket{%p} bind(%s)\n", current->name().characters(), current->pid(), safe_address);

    auto descriptor = VFS::the().open(safe_address, error, O_CREAT | O_EXCL, S_IFSOCK | 0666, *current->cwd_inode());
    if (!descriptor) {
        if (error == -EEXIST)
            error = -EADDRINUSE;
        return error;
    }
    return true;
}
