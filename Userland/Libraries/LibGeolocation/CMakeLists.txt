set(SOURCES
    Client.cpp
)

serenity_lib(LibGeolocation geolocation)
target_link_libraries(LibGeolocation PRIVATE LibConfig LibCore LibProtocol)
