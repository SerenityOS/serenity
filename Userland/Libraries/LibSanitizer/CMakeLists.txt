set(SOURCES
    UndefinedSanitizer.cpp
)

serenity_lib(LibSanitizer sanitizer)
target_link_libraries(LibSanitizer LibC)
