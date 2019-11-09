#pragma once

#include <AK/Vector.h>
#include <LibAudio/AClientConnection.h>
#include <LibAudio/AWavLoader.h>
#include <LibCore/CTimer.h>

#define PLAYBACK_MANAGER_BUFFER_SIZE 64 * KB
#define PLAYBACK_MANAGER_RATE 44100

class PlaybackManager final {
public:
    PlaybackManager(NonnullRefPtr<AClientConnection>);
    ~PlaybackManager();

    void play();
    void stop();
    void pause();
    void seek(const int position);
    bool toggle_pause();
    void set_loader(OwnPtr<AWavLoader>&&);

    int last_seek() const { return m_last_seek; }
    bool is_paused() const { return m_paused; }
    float total_length() const { return m_total_length; }
    RefPtr<ABuffer> current_buffer() const { return m_current_buffer; }

    NonnullRefPtr<AClientConnection> connection() const { return m_connection; }

    Function<void()> on_update;

private:
    void next_buffer();
    void set_paused(bool);
    void load_next_buffer();
    void remove_dead_buffers();

    bool m_paused { true };
    int m_next_ptr { 0 };
    int m_last_seek { 0 };
    float m_total_length { 0 };
    OwnPtr<AWavLoader> m_loader { nullptr };
    NonnullRefPtr<AClientConnection> m_connection;
    RefPtr<ABuffer> m_next_buffer;
    RefPtr<ABuffer> m_current_buffer;
    Vector<RefPtr<ABuffer>> m_buffers;
    RefPtr<CTimer> m_timer;
};
