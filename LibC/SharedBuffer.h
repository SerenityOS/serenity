#pragma once

#include <AK/RetainPtr.h>
#include <AK/Retainable.h>

class SharedBuffer : public Retainable<SharedBuffer> {
public:
    static RetainPtr<SharedBuffer> create(pid_t peer, int);
    static RetainPtr<SharedBuffer> create_from_shared_buffer_id(int);
    ~SharedBuffer();

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
