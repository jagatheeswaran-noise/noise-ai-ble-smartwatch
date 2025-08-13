import { NativeModules } from 'react-native';

type DecodeOpts = { bytesPerPacket?: number };

const { OpusBridge } = NativeModules;

export default {
  decodeBinToWav: (inPath: string, outPath: string, opts?: DecodeOpts) =>
    OpusBridge.decodeBinToWav(inPath, outPath, opts || {})
};
