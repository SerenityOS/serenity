set(SOURCES
    ColorScheme.cpp
)

serenity_lib(LibColorScheme colorscheme)
target_link_libraries(LibColorScheme PRIVATE LibConfig LibCore LibGfx)
