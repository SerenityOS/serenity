#include <LibC/SharedBuffer.h>
#include <unistd.h>
#include <stdio.h>
#include <AK/kmalloc.h>

RetainPtr<SharedBuffer> SharedBuffer::create(pid_t peer, int size)
{
    void* data;
    int shared_buffer_id = create_shared_buffer(peer, size, &data);
    if (shared_buffer_id < 0) {
        perror("create_shared_buffer");
        return nullptr;
    }
    return adopt(*new SharedBuffer(shared_buffer_id, size, data));
}

RetainPtr<SharedBuffer> SharedBuffer::create_from_shared_buffer_id(int shared_buffer_id)
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
