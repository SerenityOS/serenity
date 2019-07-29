#pragma once

struct ASAPI_ServerMessage {
    enum class Type {
        Invalid,
        Greeting,
        PlayingBuffer,
        FinishedPlayingBuffer,
        EnqueueBufferResponse,
        DidGetMainMixVolume,
        DidSetMainMixVolume,
    };

    Type type { Type::Invalid };
    unsigned extra_size { 0 };
    bool success { true };
    int value { 0 };

    union {
        struct {
            int server_pid;
            int your_client_id;
        } greeting;
        struct {
            int buffer_id;
        } playing_buffer;
    };
};

struct ASAPI_ClientMessage {
    enum class Type {
        Invalid,
        Greeting,
        EnqueueBuffer,
        GetMainMixVolume,
        SetMainMixVolume,
    };

    Type type { Type::Invalid };
    unsigned extra_size { 0 };
    int value { 0 };

    union {
        struct {
            int client_pid;
        } greeting;
        struct {
            int buffer_id;
        } play_buffer;
    };
};
