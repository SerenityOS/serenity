set(SOURCES
    Download.cpp
    DownloadClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/DownloadServer/DownloadClientEndpoint.h
    ../../Services/DownloadServer/DownloadServerEndpoint.h
)

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol LibIPC)
