add_compile_options(-DAK_DONT_REPLACE_STD)

add_executable(VideoPlayerSDL
    main.cpp
)

target_link_libraries(VideoPlayerSDL PRIVATE LibMain LibCore LibGfx LibVideo SDL2::SDL2)
