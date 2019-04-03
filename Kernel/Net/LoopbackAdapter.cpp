#include <Kernel/Net/LoopbackAdapter.h>
#include <AK/Eternal.h>

LoopbackAdapter& LoopbackAdapter::the()
{
    static Eternal<LoopbackAdapter> the;
    return the;
}

LoopbackAdapter::LoopbackAdapter()
{
    set_ipv4_address({ 127, 0, 0, 1 });
}

LoopbackAdapter::~LoopbackAdapter()
{
}

void LoopbackAdapter::send_raw(const byte* data, int size)
{
    dbgprintf("LoopbackAdapter: Sending %d byte(s) to myself.\n", size);
    did_receive(data, size);
}
