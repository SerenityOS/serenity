set(SOURCES
    CommonNames.cpp
    Document.cpp
    Filter.cpp
    Object.cpp
    Parser.cpp
    Renderer.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF ${LIBRARY_LIBC} LibCore LibIPC LibGfx LibTextCodec)
