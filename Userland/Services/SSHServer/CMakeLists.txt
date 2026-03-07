serenity_component(
    SSHServer
    TARGETS SSHServer
)

set(SOURCES
    main.cpp
    SSHClient.cpp
    TCPClient.cpp
)

serenity_bin(SSHServer)
target_link_libraries(SSHServer PRIVATE LibCore LibMain LibSSH)
