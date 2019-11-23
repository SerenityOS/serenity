#pragma once

#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>

class PSClientConnection;

class Download : public RefCounted<Download> {
public:
    virtual ~Download();

    static Download* find_by_id(i32);

    i32 id() const { return m_id; }
    URL url() const { return m_url; }

    size_t total_size() const { return m_total_size; }
    size_t downloaded_size() const { return m_downloaded_size; }
    const ByteBuffer& payload() const { return m_payload; }

    void stop();

protected:
    explicit Download(PSClientConnection&);

    void did_finish(bool success);
    void did_progress(size_t total_size, size_t downloaded_size);
    void set_payload(const ByteBuffer&);

private:
    i32 m_id;
    URL m_url;
    size_t m_total_size { 0 };
    size_t m_downloaded_size { 0 };
    ByteBuffer m_payload;
    WeakPtr<PSClientConnection> m_client;
};
