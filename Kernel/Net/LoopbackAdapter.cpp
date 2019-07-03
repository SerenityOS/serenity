#include <Kernel/Net/LoopbackAdapter.h>

LoopbackAdapter& LoopbackAdapter::the()
{
    static LoopbackAdapter* the;
    if (!the)
        the = new LoopbackAdapter;
    return *the;
}

LoopbackAdapter::LoopbackAdapter()
{
    set_interface_name("loop");
    set_ipv4_address({ 127, 0, 0, 1 });
}

LoopbackAdapter::~LoopbackAdapter()
{
}

void LoopbackAdapter::send_raw(const u8* data, int size)
{
    dbgprintf("LoopbackAdapter: Sending %d byte(s) to myself.\n", size);
    did_receive(data, size);
}
