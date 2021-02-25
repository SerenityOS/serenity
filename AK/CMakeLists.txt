include(${CMAKE_SOURCE_DIR}/Meta/CMake/utils.cmake)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wunused -Wold-style-cast")
serenity_install_headers(AK)
serenity_install_sources(AK)
