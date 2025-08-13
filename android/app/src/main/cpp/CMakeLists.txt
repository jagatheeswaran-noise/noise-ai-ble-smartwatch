cmake_minimum_required(VERSION 3.10)
project(opusbridge)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

# Add libopus as imported shared library
add_library(opus SHARED IMPORTED)
set_target_properties(opus PROPERTIES
  IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopus.so
)

# Add opusbridge shared library
add_library(opusbridge SHARED
  opus_bridge_jni.cpp
  wav_writer.c
)

# Include directories
target_include_directories(opusbridge PRIVATE
  ${CMAKE_SOURCE_DIR}/third_party/opus/include
)

# Add SimpleNative shared library
add_library(simplenative SHARED
  simple_native.c
)

# Include directories for SimpleNative
target_include_directories(simplenative PRIVATE
  ${CMAKE_SOURCE_DIR}
)

# Find and link libraries
find_library(log-lib log)

# Link libraries
target_link_libraries(opusbridge
  opus
  ${log-lib}
)

target_link_libraries(simplenative 
  ${log-lib}
)
