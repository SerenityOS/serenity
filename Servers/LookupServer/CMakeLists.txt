set(SOURCES
    main.cpp
    LookupServer.cpp
    DNSAnswer.cpp
    DNSRequest.cpp
    DNSResponse.cpp
)

serenity_app(LookupServer)
target_link_libraries(LookupServer LibCore)
