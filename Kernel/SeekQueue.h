#pragma once

#include <AK/SinglyLinkedList.h>

class Thread;

class SeekQueueEntry {

public:
    SeekQueueEntry(u32, u32, u16, u32, u32, u32, u16, u8*, Thread&);
    ~SeekQueueEntry();

    u32 get_entry_id();
    u32 get_namespace_id();
    u32 get_priority();
    u32 get_lba_low();
    u32 get_lba_high();
    u32 get_block_count();
    u16 get_flags();
    u8* get_io_buffer();
    Thread& get_thread();

private:
    u32 m_entry_id;
    u32 m_namespace_id;
    u16 m_priority;
    u32 m_lbal;
    u32 m_lbah;
    u32 m_block_count;
    u16 m_flags;
    u8* m_buf;
    Thread& m_blocked_thread;
};

class SeekQueue {
public:
    SeekQueue();
    ~SeekQueue();

    void insert_request(Thread&, u32 namespace_id, u16 priority, u32 lbal, u32 lbah, u32 block_count, u16 flags, u8* buf);
    void process_request();
    void wake_one();
    void wake_all();
    void clear_all();

private:
    u32 entry_counter;
    SinglyLinkedList<SeekQueueEntry*> m_seek_requests;
};
