#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>

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

