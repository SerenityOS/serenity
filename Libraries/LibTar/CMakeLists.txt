set(SOURCES
    TarStream.cpp
)

serenity_lib(LibTar tar)
target_link_libraries(LibTar LibCore)
