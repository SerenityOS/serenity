#include <AK/kmalloc.h>
#include <Kernel/Syscall.h>
#include <SharedBuffer.h>
#include <stdio.h>
#include <unistd.h>

RefPtr<SharedBuffer> SharedBuffer::create_with_size(int size)
{
    void* data;
    int shared_buffer_id = create_shared_buffer(size, &data);
    if (shared_buffer_id < 0) {
        perror("create_shared_buffer");
        return nullptr;
    }
    return adopt(*new SharedBuffer(shared_buffer_id, size, data));
}

bool SharedBuffer::share_with(pid_t peer)
{
    int ret = share_buffer_with(shared_buffer_id(), peer);
    if (ret < 0) {
        perror("share_buffer_with");
        return false;
    }
    return true;
}

bool SharedBuffer::share_globally()
{
    int ret = share_buffer_globally(shared_buffer_id());
    if (ret < 0) {
        perror("share_buffer_globally");
        return false;
    }
    return true;
}


RefPtr<SharedBuffer> SharedBuffer::create_from_shared_buffer_id(int shared_buffer_id)
{
    void* data = get_shared_buffer(shared_buffer_id);
    if (data == (void*)-1) {
        perror("get_shared_buffer");
        return nullptr;
    }
    int size = get_shared_buffer_size(shared_buffer_id);
    if (size < 0) {
        perror("get_shared_buffer_size");
        return nullptr;
    }
    return adopt(*new SharedBuffer(shared_buffer_id, size, data));
}

SharedBuffer::SharedBuffer(int shared_buffer_id, int size, void* data)
    : m_shared_buffer_id(shared_buffer_id)
    , m_size(size)
    , m_data(data)
{
}

SharedBuffer::~SharedBuffer()
{
    if (m_shared_buffer_id >= 0) {
        int rc = release_shared_buffer(m_shared_buffer_id);
        if (rc < 0) {
            perror("release_shared_buffer");
        }
    }
}

void SharedBuffer::seal()
{
    int rc = seal_shared_buffer(m_shared_buffer_id);
    if (rc < 0) {
        perror("seal_shared_buffer");
        ASSERT_NOT_REACHED();
    }
}

void SharedBuffer::set_volatile()
{
    u32 rc = syscall(SC_set_shared_buffer_volatile, m_shared_buffer_id, true);
    ASSERT(rc == 0);
}

bool SharedBuffer::set_nonvolatile()
{
    u32 rc = syscall(SC_set_shared_buffer_volatile, m_shared_buffer_id, false);
    if (rc == 0)
        return true;
    if (rc == 1)
        return false;
    ASSERT_NOT_REACHED();
}
