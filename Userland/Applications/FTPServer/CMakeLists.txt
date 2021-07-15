serenity_component(
    FTPServer
    RECOMMENDED
    TARGETS FTPServer
)

set(SOURCES
    FTPServer.cpp
    FTPServerClient.cpp
    FTPServerTransferModel.cpp
    main.cpp
    Transports/RawTransport.cpp
    Transports/TLS/TLSTransport.cpp
    Transports/TLS/Extensions.cpp
)

serenity_app(FTPServer ICON app-ftp-server)
target_link_libraries(FTPServer LibWeb LibThreading)
