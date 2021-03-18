set(SOURCES
    TarStream.cpp
)

serenity_lib(LibArchive archive)
target_link_libraries(LibArchive LibCore)
