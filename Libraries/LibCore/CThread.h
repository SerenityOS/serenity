#pragma once

#ifdef __serenity__

class CThread {
public:
    static CThread& main_thread();

    CThread(int (*entry)(void*), void* user_data);
    ~CThread();

    bool is_main_thread() const { return m_thread_id == 0; }
    int thread_id() const { return m_thread_id; }

private:
    enum MainThreadTag { MainThread };
    explicit CThread(MainThreadTag);

    int m_thread_id { -1 };
};

#endif

