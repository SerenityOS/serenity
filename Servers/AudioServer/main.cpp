#include <LibCore/CFile.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibAudio/AWavLoader.h>
#include <LibAudio/AWavFile.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>

class ASEventLoop
{
public:
    ASEventLoop();
    int exec() { return m_event_loop.exec(); }
private:
    CEventLoop m_event_loop;
    CLocalSocket m_server_sock;
    OwnPtr<CNotifier> m_server_notifier;

    void drain_server();
};

void read_and_play_wav()
{
    CFile audio("/dev/audio");
    if (!audio.open(CIODevice::WriteOnly)) {
        dbgprintf("Can't open audio device: %s\n", audio.error_string());
        return;
    }

    AWavLoader loader;
    const auto& file = loader.load_wav("/home/anon/tmp.wav");
    if (!file) {
        dbgprintf("Can't parse WAV: %s\n", loader.error_string());
        return;
    }

    dbgprintf("Read WAV of format %d with num_channels %d sample rate %d, bits per sample %d\n", (u8)file->format(), file->channel_count(), file->sample_rate_per_second(), file->bits_per_sample());

    auto contents = file->sample_data();
    const int chunk_size = 4096;
    int i = 0;
    while (i < contents.size()) {
        const auto chunk = contents.slice(i, chunk_size);
        audio.write(chunk);
        i += chunk_size;
    }
}

ASEventLoop::ASEventLoop()
{
    read_and_play_wav();

    unlink("/tmp/asportal");

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/asportal");
    int rc = bind(m_server_sock.fd(), (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = listen(m_server_sock.fd(), 5);
    ASSERT(rc == 0);

    m_server_notifier = make<CNotifier>(m_server_sock.fd(), CNotifier::Read);
    m_server_notifier->on_ready_to_read = [this] { drain_server(); };
}

void ASEventLoop::drain_server()
{
    sockaddr_un address;
    socklen_t address_size = sizeof(address);
    int client_fd = accept(m_server_sock.fd(), (sockaddr*)&address, &address_size);
    if (client_fd < 0) {
        dbgprintf("WindowServer: accept() failed: %s\n", strerror(errno));
    } else {
        dbgprintf("AudioServer: accept()ed client %d\n", client_fd);
        String s("hello, client!\n");
        write(client_fd, s.characters(), s.length());
        close(client_fd);
    }
}

int main(int, char**)
{
    ASEventLoop event_loop;
    return event_loop.exec();
}
