#ifndef WAV_WRITER_H
#define WAV_WRITER_H

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// Write WAV header to file
int write_wav_header(FILE* file, uint32_t sample_rate, uint16_t num_channels, 
                     uint16_t bits_per_sample, uint32_t num_samples);

// Update WAV header with final data size
int update_wav_header(FILE* file, uint32_t num_samples, uint16_t num_channels, 
                      uint16_t bits_per_sample);

#ifdef __cplusplus
}
#endif

#endif // WAV_WRITER_H
