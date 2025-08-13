#include <stdio.h>
#include <stdint.h>

void wav_write_header(FILE* f, uint32_t sr, uint16_t ch, uint16_t bps, uint32_t nsamples){
  uint32_t byteRate = sr * ch * (bps/8);
  uint16_t align = ch * (bps/8);
  uint32_t dataSize = nsamples * align;
  uint32_t riffSize = 36 + dataSize;

  fwrite("RIFF",1,4,f); fwrite(&riffSize,4,1,f); fwrite("WAVE",1,4,f);
  fwrite("fmt ",1,4,f); uint32_t fmtSize=16; fwrite(&fmtSize,4,1,f);
  uint16_t fmt=1; fwrite(&fmt,2,1,f); fwrite(&ch,2,1,f); fwrite(&sr,4,1,f);
  fwrite(&byteRate,4,1,f); fwrite(&align,2,1,f); fwrite(&bps,2,1,f);
  fwrite("data",1,4,f); fwrite(&dataSize,4,1,f);
}
