serenity_component(
    GeolocationServer
    REQUIRED
    TARGETS GeolocationServer
)

compile_ipc(GeolocationServer.ipc GeolocationServerEndpoint.h)
compile_ipc(GeolocationClient.ipc GeolocationClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    main.cpp
)

set(GENERATED_SOURCES
    GeolocationServerEndpoint.h
    GeolocationClientEndpoint.h
)

serenity_bin(GeolocationServer)
target_link_libraries(GeolocationServer PRIVATE LibCore LibIPC LibMain)
