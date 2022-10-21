serenity_component(
    DHCPClient
    REQUIRED
    TARGETS DHCPClient
)

set(SOURCES
    DHCPv4Client.cpp
    DHCPv4.cpp
    main.cpp
)

serenity_bin(DHCPClient)
target_link_libraries(DHCPClient PRIVATE LibCore LibMain)
