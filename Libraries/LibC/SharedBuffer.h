#pragma once

#include <AK/RefPtr.h>
#include <AK/RefCounted.h>

class SharedBuffer : public RefCounted<SharedBuffer> {
public:
    static RefPtr<SharedBuffer> create_with_size(int);
    static RefPtr<SharedBuffer> create_from_shared_buffer_id(int);
    ~SharedBuffer();

    bool share_with(pid_t);
    int shared_buffer_id() const { return m_shared_buffer_id; }
    void seal();
    int size() const { return m_size; }
    void* data() { return m_data; }
    const void* data() const { return m_data; }

private:
    SharedBuffer(int shared_buffer_id, int size, void*);

    int m_shared_buffer_id { -1 };
    int m_size { 0 };
    void* m_data;
};
