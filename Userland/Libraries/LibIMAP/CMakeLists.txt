set(SOURCES Objects.cpp Client.cpp  Parser.cpp)

set(GENERATED_SOURCES)

serenity_lib(LibIMAP imap)
target_link_libraries(LibIMAP LibCore LibTLS)
