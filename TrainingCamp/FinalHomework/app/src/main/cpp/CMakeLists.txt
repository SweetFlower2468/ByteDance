cmake_minimum_required(VERSION 3.10.2)
project("llama-android")

# Define libraries to link
find_library(log-lib log)

# Set flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")

# Add llama.cpp as a subdirectory
# The user must clone llama.cpp into app/src/main/cpp/llama.cpp
# We check if it exists
if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    # llama.cpp's CMakeLists.txt builds 'llama' target
    set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
    set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    
    # Try to enable Vulkan for GPU acceleration
    # Note: Requires NDK 24+ and valid environment. If build fails, revert to OFF.
    option(GGML_VULKAN "llama: use Vulkan" OFF)
    
    add_subdirectory(llama.cpp)
    
    # Define our JNI library
    add_library(llama-android SHARED llama-jni.cpp)

    # Link against llama and common (if available from common dir)
    # Note: Recent llama.cpp might put common in a separate target or just object files.
    # We might need to include headers.
    
    target_include_directories(llama-android PRIVATE 
        llama.cpp 
        llama.cpp/include
        llama.cpp/common
        llama.cpp/ggml/include
    )
    
    # Depend on llama target
    target_link_libraries(llama-android llama ${log-lib})

else()
    message(WARNING "llama.cpp submodule not found! Please clone it into app/src/main/cpp/llama.cpp")
    # Define a dummy library so gradle sync doesn't fail completely before cloning
    add_library(llama-android SHARED llama-jni.cpp)
endif()

