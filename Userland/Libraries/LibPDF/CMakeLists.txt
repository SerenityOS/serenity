set(SOURCES
    Object.cpp
    Document.cpp
    Object.cpp
    Parser.cpp
    Renderer.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF LibC LibCore LibIPC LibGfx)
