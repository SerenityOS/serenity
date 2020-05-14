set(SOURCES
    GeminiJob.cpp
    GeminiRequest.cpp
    GeminiResponse.cpp
    Job.cpp
)

serenity_lib(LibGemini gemini)
target_link_libraries(LibGemini LibCore LibTLS)
