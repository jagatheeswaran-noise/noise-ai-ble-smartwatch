#ifndef OPUS_H
#define OPUS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

// Type definitions
typedef int32_t opus_int32;
typedef int16_t opus_int16;

// Opus error codes
#define OPUS_OK                0
#define OPUS_BAD_ARG          -1
#define OPUS_BUFFER_TOO_SMALL -2
#define OPUS_INTERNAL_ERROR   -3
#define OPUS_INVALID_PACKET  -4
#define OPUS_UNIMPLEMENTED   -5
#define OPUS_INVALID_STATE   -6
#define OPUS_ALLOC_FAIL      -7

// Opus decoder
typedef struct OpusDecoder OpusDecoder;

// Create Opus decoder
OpusDecoder* opus_decoder_create(
    opus_int32 Fs,
    int channels,
    int *error
);

// Destroy Opus decoder
void opus_decoder_destroy(OpusDecoder *st);

// Decode Opus packet
int opus_decode(
    OpusDecoder *st,
    const unsigned char *data,
    opus_int32 len,
    opus_int16 *pcm,
    int frame_size,
    int decode_fec
);

// Get error string
const char* opus_strerror(int error);

#ifdef __cplusplus
}
#endif

#endif // OPUS_H
