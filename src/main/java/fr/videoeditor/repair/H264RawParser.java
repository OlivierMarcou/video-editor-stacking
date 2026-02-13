package fr.videoeditor.repair;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser H.264 brut pour récupération de vidéos endommagées
 * Analyse le flux binaire directement sans dépendre du container MP4
 * Similaire à l'approche de Digital Video Repair
 */
public class H264RawParser {
    
    // Start codes H.264
    private static final byte[] START_CODE_4 = {0x00, 0x00, 0x00, 0x01};
    private static final byte[] START_CODE_3 = {0x00, 0x00, 0x01};
    
    // Types de NAL units
    private static final int NAL_SLICE = 1;
    private static final int NAL_IDR = 5;
    private static final int NAL_SEI = 6;
    private static final int NAL_SPS = 7;
    private static final int NAL_PPS = 8;
    
    public static class VideoInfo {
        public int width;
        public int height;
        public double frameRate;
        public int profile;
        public int level;
        public boolean found = false;
        
        @Override
        public String toString() {
            return String.format("%dx%d @ %.2f fps (Profile: %d, Level: %d)", 
                width, height, frameRate, profile, level);
        }
    }
    
    public static class NALUnit {
        public int type;
        public long offset;
        public int size;
        public byte[] data;
        
        public boolean isFrame() {
            return type == NAL_SLICE || type == NAL_IDR;
        }
        
        public boolean isIDR() {
            return type == NAL_IDR;
        }
    }
    
    /**
     * Analyse le fichier et extrait les informations vidéo du SPS
     */
    public static VideoInfo analyzeFile(File file, ProgressCallback callback) throws IOException {
        VideoInfo info = new VideoInfo();
        
        if (callback != null) {
            callback.onProgress("Analyse du fichier binaire...");
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileSize = raf.length();
            byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer
            
            long position = 0;
            int spsFound = 0;
            
            while (position < fileSize && spsFound < 3) {
                // Lire un bloc
                raf.seek(position);
                int read = raf.read(buffer);
                if (read <= 0) break;
                
                // Chercher les start codes
                for (int i = 0; i < read - 4; i++) {
                    if (isStartCode(buffer, i)) {
                        int startCodeSize = getStartCodeSize(buffer, i);
                        int nalType = buffer[i + startCodeSize] & 0x1F;
                        
                        if (nalType == NAL_SPS) {
                            // Extraire le SPS
                            int spsEnd = findNextStartCode(buffer, i + startCodeSize, read);
                            if (spsEnd > 0) {
                                byte[] spsData = new byte[spsEnd - (i + startCodeSize)];
                                System.arraycopy(buffer, i + startCodeSize, spsData, 0, spsData.length);
                                
                                // Parser le SPS
                                VideoInfo parsedInfo = parseSPS(spsData);
                                if (parsedInfo.found) {
                                    info = parsedInfo;
                                    spsFound++;
                                    
                                    if (callback != null) {
                                        callback.onProgress("SPS trouvé #" + spsFound + ": " + info.toString());
                                    }
                                }
                            }
                        }
                    }
                }
                
                position += read - 1024; // Overlap pour ne pas manquer les start codes à la limite
                
                if (callback != null && position % (10 * 1024 * 1024) == 0) {
                    int percent = (int)((position * 100) / fileSize);
                    callback.onProgress("Analyse: " + percent + "%");
                }
            }
        }
        
        return info;
    }
    
    /**
     * Extrait toutes les NAL units du fichier
     */
    public static List<NALUnit> extractNALUnits(File file, ProgressCallback callback) throws IOException {
        List<NALUnit> nalUnits = new ArrayList<>();
        
        if (callback != null) {
            callback.onProgress("Extraction des NAL units...");
        }
        
        int frameCount = 0;
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileSize = raf.length();
            byte[] buffer = new byte[2 * 1024 * 1024]; // 2 MB buffer
            
            long position = 0;
            
            while (position < fileSize) {
                raf.seek(position);
                int read = raf.read(buffer);
                if (read <= 0) break;
                
                for (int i = 0; i < read - 4; i++) {
                    if (isStartCode(buffer, i)) {
                        int startCodeSize = getStartCodeSize(buffer, i);
                        int nalType = buffer[i + startCodeSize] & 0x1F;
                        
                        // Trouver la fin de cette NAL unit
                        int nalEnd = findNextStartCode(buffer, i + startCodeSize, read);
                        if (nalEnd < 0) nalEnd = read;
                        
                        int nalSize = nalEnd - (i + startCodeSize);
                        
                        NALUnit nal = new NALUnit();
                        nal.type = nalType;
                        nal.offset = position + i;
                        nal.size = nalSize;
                        nal.data = new byte[nalSize];
                        System.arraycopy(buffer, i + startCodeSize, nal.data, 0, nalSize);
                        
                        nalUnits.add(nal);
                        
                        if (nal.isFrame()) {
                            frameCount++;
                            if (frameCount % 100 == 0 && callback != null) {
                                callback.onProgress("Frames détectées: " + frameCount);
                            }
                        }
                        
                        i = nalEnd - 1;
                    }
                }
                
                position += read - 1024;
                
                if (callback != null && position % (10 * 1024 * 1024) == 0) {
                    int percent = (int)((position * 100) / fileSize);
                    callback.onProgress("Extraction: " + percent + "% - " + frameCount + " frames");
                }
            }
        }
        
        if (callback != null) {
            callback.onProgress("Extraction terminée: " + nalUnits.size() + " NAL units (" + frameCount + " frames)");
        }
        
        return nalUnits;
    }
    
    private static boolean isStartCode(byte[] buffer, int offset) {
        if (offset + 4 <= buffer.length) {
            if (buffer[offset] == 0x00 && buffer[offset + 1] == 0x00 &&
                buffer[offset + 2] == 0x00 && buffer[offset + 3] == 0x01) {
                return true;
            }
        }
        if (offset + 3 <= buffer.length) {
            if (buffer[offset] == 0x00 && buffer[offset + 1] == 0x00 &&
                buffer[offset + 2] == 0x01) {
                return true;
            }
        }
        return false;
    }
    
    private static int getStartCodeSize(byte[] buffer, int offset) {
        if (offset + 4 <= buffer.length) {
            if (buffer[offset] == 0x00 && buffer[offset + 1] == 0x00 &&
                buffer[offset + 2] == 0x00 && buffer[offset + 3] == 0x01) {
                return 4;
            }
        }
        return 3;
    }
    
    private static int findNextStartCode(byte[] buffer, int start, int end) {
        for (int i = start; i < end - 4; i++) {
            if (isStartCode(buffer, i)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Parse un SPS (Sequence Parameter Set) pour extraire résolution et framerate
     * Parsing simplifié - ne gère que les cas courants
     */
    private static VideoInfo parseSPS(byte[] sps) {
        VideoInfo info = new VideoInfo();
        
        try {
            // Skip NAL header
            BitReader reader = new BitReader(sps, 1);
            
            // profile_idc
            info.profile = reader.readBits(8);
            
            // constraint flags
            reader.skipBits(8);
            
            // level_idc
            info.level = reader.readBits(8);
            
            // seq_parameter_set_id
            reader.readUE();
            
            // Chroma format (pour certains profiles)
            if (info.profile == 100 || info.profile == 110 || info.profile == 122 || 
                info.profile == 244 || info.profile == 44 || info.profile == 83 || 
                info.profile == 86 || info.profile == 118 || info.profile == 128) {
                
                int chroma_format_idc = reader.readUE();
                if (chroma_format_idc == 3) {
                    reader.skipBits(1); // separate_colour_plane_flag
                }
                reader.readUE(); // bit_depth_luma_minus8
                reader.readUE(); // bit_depth_chroma_minus8
                reader.skipBits(1); // qpprime_y_zero_transform_bypass_flag
                
                boolean seq_scaling_matrix_present_flag = reader.readBit();
                if (seq_scaling_matrix_present_flag) {
                    // Skip scaling lists (complexe)
                    for (int i = 0; i < 8; i++) {
                        if (reader.readBit()) {
                            int size = i < 6 ? 16 : 64;
                            int lastScale = 8;
                            int nextScale = 8;
                            for (int j = 0; j < size; j++) {
                                if (nextScale != 0) {
                                    int delta_scale = reader.readSE();
                                    nextScale = (lastScale + delta_scale + 256) % 256;
                                }
                                lastScale = (nextScale == 0) ? lastScale : nextScale;
                            }
                        }
                    }
                }
            }
            
            // log2_max_frame_num_minus4
            reader.readUE();
            
            // pic_order_cnt_type
            int pic_order_cnt_type = reader.readUE();
            if (pic_order_cnt_type == 0) {
                reader.readUE(); // log2_max_pic_order_cnt_lsb_minus4
            } else if (pic_order_cnt_type == 1) {
                reader.skipBits(1); // delta_pic_order_always_zero_flag
                reader.readSE(); // offset_for_non_ref_pic
                reader.readSE(); // offset_for_top_to_bottom_field
                int num_ref_frames_in_pic_order_cnt_cycle = reader.readUE();
                for (int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++) {
                    reader.readSE(); // offset_for_ref_frame
                }
            }
            
            // max_num_ref_frames
            reader.readUE();
            
            // gaps_in_frame_num_value_allowed_flag
            reader.skipBits(1);
            
            // pic_width_in_mbs_minus1
            int pic_width_in_mbs_minus1 = reader.readUE();
            
            // pic_height_in_map_units_minus1
            int pic_height_in_map_units_minus1 = reader.readUE();
            
            // frame_mbs_only_flag
            boolean frame_mbs_only_flag = reader.readBit();
            
            if (!frame_mbs_only_flag) {
                reader.skipBits(1); // mb_adaptive_frame_field_flag
            }
            
            // direct_8x8_inference_flag
            reader.skipBits(1);
            
            // frame_cropping_flag
            boolean frame_cropping_flag = reader.readBit();
            int frame_crop_left_offset = 0;
            int frame_crop_right_offset = 0;
            int frame_crop_top_offset = 0;
            int frame_crop_bottom_offset = 0;
            
            if (frame_cropping_flag) {
                frame_crop_left_offset = reader.readUE();
                frame_crop_right_offset = reader.readUE();
                frame_crop_top_offset = reader.readUE();
                frame_crop_bottom_offset = reader.readUE();
            }
            
            // Calculer la résolution
            info.width = (pic_width_in_mbs_minus1 + 1) * 16;
            info.height = (pic_height_in_map_units_minus1 + 1) * 16 * (frame_mbs_only_flag ? 1 : 2);
            
            // Appliquer le cropping
            int cropUnitX = 2;
            int cropUnitY = 2 * (frame_mbs_only_flag ? 1 : 2);
            info.width -= (frame_crop_left_offset + frame_crop_right_offset) * cropUnitX;
            info.height -= (frame_crop_top_offset + frame_crop_bottom_offset) * cropUnitY;
            
            // VUI parameters pour framerate
            boolean vui_parameters_present_flag = reader.readBit();
            if (vui_parameters_present_flag) {
                boolean aspect_ratio_info_present_flag = reader.readBit();
                if (aspect_ratio_info_present_flag) {
                    int aspect_ratio_idc = reader.readBits(8);
                    if (aspect_ratio_idc == 255) { // Extended_SAR
                        reader.skipBits(16); // sar_width
                        reader.skipBits(16); // sar_height
                    }
                }
                
                boolean overscan_info_present_flag = reader.readBit();
                if (overscan_info_present_flag) {
                    reader.skipBits(1); // overscan_appropriate_flag
                }
                
                boolean video_signal_type_present_flag = reader.readBit();
                if (video_signal_type_present_flag) {
                    reader.skipBits(3); // video_format
                    reader.skipBits(1); // video_full_range_flag
                    boolean colour_description_present_flag = reader.readBit();
                    if (colour_description_present_flag) {
                        reader.skipBits(8); // colour_primaries
                        reader.skipBits(8); // transfer_characteristics
                        reader.skipBits(8); // matrix_coefficients
                    }
                }
                
                boolean chroma_loc_info_present_flag = reader.readBit();
                if (chroma_loc_info_present_flag) {
                    reader.readUE(); // chroma_sample_loc_type_top_field
                    reader.readUE(); // chroma_sample_loc_type_bottom_field
                }
                
                boolean timing_info_present_flag = reader.readBit();
                if (timing_info_present_flag) {
                    long num_units_in_tick = reader.readBits(32);
                    long time_scale = reader.readBits(32);
                    
                    if (num_units_in_tick > 0 && time_scale > 0) {
                        info.frameRate = (double)time_scale / (2.0 * num_units_in_tick);
                    } else {
                        info.frameRate = 30.0; // Default
                    }
                } else {
                    info.frameRate = 30.0; // Default
                }
            } else {
                info.frameRate = 30.0; // Default
            }
            
            info.found = true;
            
        } catch (Exception e) {
            // Parsing failed
            info.found = false;
        }
        
        return info;
    }
    
    /**
     * Helper class pour lire des bits
     */
    private static class BitReader {
        private byte[] data;
        private int bytePos;
        private int bitPos;
        
        public BitReader(byte[] data, int startByte) {
            this.data = data;
            this.bytePos = startByte;
            this.bitPos = 0;
        }
        
        public boolean readBit() {
            if (bytePos >= data.length) return false;
            boolean bit = ((data[bytePos] >> (7 - bitPos)) & 1) == 1;
            bitPos++;
            if (bitPos == 8) {
                bitPos = 0;
                bytePos++;
            }
            return bit;
        }
        
        public int readBits(int n) {
            int value = 0;
            for (int i = 0; i < n; i++) {
                value = (value << 1) | (readBit() ? 1 : 0);
            }
            return value;
        }
        
        public void skipBits(int n) {
            for (int i = 0; i < n; i++) {
                readBit();
            }
        }
        
        public int readUE() {
            int leadingZeros = 0;
            while (!readBit()) {
                leadingZeros++;
                if (leadingZeros > 32) return 0; // Erreur
            }
            if (leadingZeros == 0) return 0;
            return (1 << leadingZeros) - 1 + readBits(leadingZeros);
        }
        
        public int readSE() {
            int value = readUE();
            return (value & 1) == 0 ? -(value >> 1) : (value + 1) >> 1;
        }
    }
    
    public interface ProgressCallback {
        void onProgress(String message);
    }
}
