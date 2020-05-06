set(SOURCES
    DNSAnswer.cpp
    DNSRequest.cpp
    DNSResponse.cpp
    LookupServer.cpp
    main.cpp
)

serenity_bin(LookupServer)
target_link_libraries(LookupServer LibCore)
