set(SOURCES
    DNSAnswer.cpp
    DNSName.cpp
    DNSPacket.cpp
)

serenity_lib(LibDNS dns)
target_link_libraries(LibDNS LibC)
