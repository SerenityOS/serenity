#include "PTYMultiplexer.h"
#include "MasterPTY.h"
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

//#define PTMX_DEBUG

static const unsigned s_max_pty_pairs = 8;
static PTYMultiplexer* s_the;

PTYMultiplexer& PTYMultiplexer::the()
{
    ASSERT(s_the);
    return *s_the;
}

PTYMultiplexer::PTYMultiplexer()
    : CharacterDevice(5, 2)
{
    s_the = this;
    m_freelist.ensure_capacity(s_max_pty_pairs);
    for (int i = s_max_pty_pairs; i > 0; --i)
        m_freelist.unchecked_append(i - 1);
}

PTYMultiplexer::~PTYMultiplexer()
{
}

KResultOr<NonnullRefPtr<FileDescription>> PTYMultiplexer::open(int options)
{
    LOCKER(m_lock);
    if (m_freelist.is_empty())
        return KResult(-EBUSY);
    auto master_index = m_freelist.take_last();
    auto master = adopt(*new MasterPTY(master_index));
#ifdef PTMX_DEBUG
    dbgprintf("PTYMultiplexer::open: Vending master %u\n", master->index());
#endif
    auto description = FileDescription::create(move(master));
    description->set_rw_mode(options);
    return description;
}

void PTYMultiplexer::notify_master_destroyed(Badge<MasterPTY>, unsigned index)
{
    LOCKER(m_lock);
    m_freelist.append(index);
#ifdef PTMX_DEBUG
    dbgprintf("PTYMultiplexer: %u added to freelist\n", index);
#endif
}
