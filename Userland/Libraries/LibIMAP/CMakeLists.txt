set(SOURCES
    Client.cpp
    Objects.cpp
    Parser.cpp
    QuotedPrintable.cpp
)

set(GENERATED_SOURCES)

serenity_lib(LibIMAP imap)
target_link_libraries(LibIMAP LibCore LibTLS)
