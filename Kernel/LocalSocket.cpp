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

bool LocalSocket::get_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size != sizeof(sockaddr_un))
        return false;
    memcpy(address, &m_address, sizeof(sockaddr_un));
    *address_size = sizeof(sockaddr_un);
    return true;
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

    m_file = VFS::the().open(safe_address, error, O_CREAT | O_EXCL, S_IFSOCK | 0666, *current->cwd_inode());
    if (!m_file) {
        if (error == -EEXIST)
            error = -EADDRINUSE;
        return error;
    }

    m_address = local_address;
    m_bound = true;
    return true;
}
