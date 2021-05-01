set(SOURCES
    Object.cpp
    Value.cpp
    )

serenity_lib(LibPDF pdf)
target_link_libraries(LibPDF LibC LibCore)
