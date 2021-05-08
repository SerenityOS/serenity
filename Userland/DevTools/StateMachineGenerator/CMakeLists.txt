set(SOURCES
    main.cpp
)

add_executable(StateMachineGenerator ${SOURCES})
target_link_libraries(StateMachineGenerator LagomCore)
