if(NOT EMSCRIPTEN)
  add_compile_options(-DAK_DONT_REPLACE_STD)
  find_package(SDL2 REQUIRED)

  include_directories(${SDL2_INCLUDE_DIRS})
  add_executable(VideoPlayerSDL
      main.cpp
  )

  target_include_directories(VideoPlayerSDL PRIVATE ${SDL2_INCLUDE_DIRS})
  target_link_libraries(VideoPlayerSDL PRIVATE LibMain LibCore LibGfx LibMedia SDL2::SDL2)
endif()
