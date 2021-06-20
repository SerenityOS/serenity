set(SOURCES
    main.cpp
)

add_executable(ConfigureComponents ${SOURCES})
target_link_libraries(ConfigureComponents LagomCore)
