#include "PTYMultiplexer.h"
#include "MasterPTY.h"
#include <LibC/errno_numbers.h>

PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(5, 2)
{
    m_freelist.ensureCapacity(4);
    for (int i = 4; i > 0; --i)
        m_freelist.unchecked_append(adopt(*new MasterPTY(i - 1)));
}

PTYMultiplexer::~PTYMultiplexer()
{
}

RetainPtr<FileDescriptor> PTYMultiplexer::open(int& error, int options)
{
    LOCKER(m_lock);
    if (m_freelist.is_empty()) {
        error = -EBUSY;
        return nullptr;
    }
    auto master = m_freelist.takeLast();
    dbgprintf("PTYMultiplexer::open: Vending master %u\n", master->index());
    return VFS::the().open(move(master), error, options);
}
