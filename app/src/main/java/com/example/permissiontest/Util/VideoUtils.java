package com.example.permissiontest.Util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static com.example.permissiontest.Util.FaceDection.drawFaceRect;
import static com.example.permissiontest.Util.FaceDection.getFaceInfo;
import static com.example.permissiontest.Util.FileUtils.Save_Video;
import static com.example.permissiontest.Util.FileUtils.deleteFile;

public class VideoUtils {
    static private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private static final boolean VERBOSE = false;
    private static final long DEFAULT_TIMEOUT_US = 10000;

    /**
     * 得到这一秒的视频帧
     * @param extractor
     * @param mediaFormat
     * @param decoder
     * @param sec //获取帧图像时间
     * @return  Bitmap
     * @throws IOException
     */
    private static Bitmap getBitmapBySec(MediaExtractor extractor, MediaFormat mediaFormat, MediaCodec decoder, long sec) throws IOException {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Bitmap bitmap = null;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        boolean stopDecode = false;
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.i("getBitmapBySec", "w: " + width);
        long presentationTimeUs = -1;
        int outputBufferId;
        Image image = null;

        extractor.seekTo(sec, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                //Log.i("getBitmapBySec", "sawInputEOS: " + sawInputEOS);
                int inputBufferId = decoder.dequeueInputBuffer(-1);
                //Log.i("getBitmapBySec", "inputBufferId: " + inputBufferId);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        //Log.i("getBitmapBySec", "sampleSize<0 ");
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                        //Log.i("getBitmapBySec", "presentationTimeUs: " + presentationTimeUs);
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 | presentationTimeUs >= sec) {
                    Log.i("getBitmapBySec", "sec: " + sec);
                    sawOutputEOS = true;
                    boolean doRender = (info.size != 0);
                    if (doRender) {
                        Log.i("getBitmapBySec", "deal bitmap which at " + presentationTimeUs);
                        image = decoder.getOutputImage(outputBufferId);
                        YuvImage yuvImage = new YuvImage(YUV_420_888toNV21(image), ImageFormat.NV21, width, height, null);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                        bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        stream.close();
                        image.close();
                    }
                }
                decoder.releaseOutputBuffer(outputBufferId, true);
            }
        }

        return bitmap;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if (VERBOSE) Log.v("YUV_420_888toNV21", "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;

                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;

                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v("YUV_420_888toNV21", "pixelStride " + pixelStride);
                Log.v("YUV_420_888toNV21", "rowStride " + rowStride);
                Log.v("YUV_420_888toNV21", "width " + width);
                Log.v("YUV_420_888toNV21", "height " + height);
                Log.v("YUV_420_888toNV21", "buffer size " + buffer.remaining());
            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v("", "Finished reading data from plane " + i);
        }
        return data;
    }

    private static long getValidSampleTime(long time, MediaExtractor extractor, MediaFormat format) {
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        int count = 0;
        int maxRange = 5;
        long sampleTime = extractor.getSampleTime();
        while (count < maxRange) {
            extractor.advance();
            long s = extractor.getSampleTime();
            if (s != -1L) {
                count++;
                // 选取和目标时间差值最小的那个
                sampleTime = getMinDiffTime(time, sampleTime, s);
                if (Math.abs(sampleTime - time) <= format.getInteger(MediaFormat.KEY_FRAME_RATE)) {
                    //如果这个差值在 一帧间隔 内，即为成功
                    return sampleTime;
                }
            } else {
                count = maxRange;
            }
        }
        return sampleTime;
    }

    private static long getMinDiffTime(long time, long value1, long value2) {
        long diff1 = value1 - time;
        long diff2 = value2 - time;
        diff1 = diff1 > 0 ? diff1 : -diff1;
        diff2 = diff2 > 0 ? diff2 : -diff2;
        return diff1 < diff2 ? value1 : value2;
    }


    static class VideoInfo {
        long time;
        int width;
        int height;
    }

    public static VideoInfo getVideoInfo(Context context, Uri uri) {
//        File file = new File(path);
        MediaPlayer mediaPlayer = getVideoMediaPlayer(context, uri);
        VideoInfo vi = new VideoInfo();
        vi.time = mediaPlayer == null ? 0 : mediaPlayer.getDuration();
        vi.height = mediaPlayer == null ? 0 : mediaPlayer.getVideoHeight();
        vi.width = mediaPlayer == null ? 0 : mediaPlayer.getVideoWidth();
        mediaPlayer.release();
        return vi;
    }

    /**
     * 获取视频帧
     * @param context
     * @param audioPath
     * @param uri
     * @param width
     * @param height
     * @param duration
     * @return
     */
    static public boolean initFrameFromVideoBySecond(Context context, String audioPath, Uri
            uri, int width, int height, long duration) {
//        File file = new File(videoPath);
        MediaExtractor extractor = null;
        MediaFormat mediaFormat = null;
        MediaCodec decoder = null;

        boolean res = false;
        try {
            long totalSec = duration * 1000;

            extractor = initMediaExtractor(context, uri);
            extractAudio(context, extractor, uri, audioPath);

            mediaFormat = initMediaFormat(uri, extractor, 0);
            decoder = initMediaCodec(mediaFormat);
            decoder.configure(mediaFormat, null, null, 0);
            decoder.start();

            Log.i("totalSec", "totalSec:" + totalSec);
            for (long time = 0; time < totalSec; time += 40000) { //获取帧时间
                //获取这一帧图片

                Bitmap bitmap = getBitmapBySec(extractor, mediaFormat, decoder, time);
                if (bitmap == null) continue;

                Log.i("initFrameFromVideoBySecond", "w: " + bitmap.getWidth());

                float xScale = (float) 100 / bitmap.getWidth();

                Log.i("initFrameFromVideoBySecond", "xScale: " + xScale);

                //压缩图片大小
//                bitmap = BitmapUtils.compressBitmap(bitmap, xScale, xScale);
                //图像灰度化
//                bitmap = BitmapUtils.array2Bitmap(BitmapUtils.getBitmap2GaryArray(bitmap), bitmap.getWidth(), bitmap.getHeight());
                //识别人脸，并画框
                getFaceInfo(bitmap);
                bitmap = drawFaceRect(bitmap);

                BitmapUtils.addGraphToGallery(context, bitmap, "FunVideo_CachePic_Source", false);
                bitmap.recycle();
            }

            decoder.stop();
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
            if (extractor != null) {
                extractor.release();
            }
            res = true;
        } catch (IOException ex) {
            Log.i("init error", ex.getMessage());
            ex.printStackTrace();
        } finally {

        }
        return res;
    }

    public static void extractAudio(Context context, MediaExtractor extractor, Uri uri, String audiopath){
        MediaFormat mediaFormat = null;

        mediaFormat = initMediaFormat(uri, extractor, 1);

        int trackIndex = selectTrack(extractor, 1);


        try {
            muxerAudio(extractor, mediaFormat, audiopath, trackIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     *
     * @param extractor 源数据
     * @param format    需要分离的音视频或者视频
     * @param path      视频或者音频输出路径
     * @param index     轨道下标
     * @throws IOException
     */
    static public void muxerAudio(MediaExtractor extractor, MediaFormat format, String path, int index) throws IOException {
        MediaFormat trackFormat = extractor.getTrackFormat(index);
        MediaMuxer mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int trackIndex = mediaMuxer.addTrack(trackFormat);
        extractor.selectTrack(index);
        mediaMuxer.start();

        int maxVideoBufferCount = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxVideoBufferCount);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        //时长：这里需要区分音频或者视频，两个是根据不同的计算方法，否则会导致分离的音频或者视频时长不一致或者崩溃
        long videoSampleTime = 0;
        try {
            videoSampleTime = getSampleTime(trackFormat);
        } catch (Exception e) {
            videoSampleTime = getSampleTime(extractor, byteBuffer);
        }
        while (true) {
            int readSampleDataSize = extractor.readSampleData(byteBuffer, 0);
            if (readSampleDataSize < 0) {
                break;
            }
            bufferInfo.size = readSampleDataSize;
            bufferInfo.offset = 0;
            bufferInfo.flags = extractor.getSampleFlags();
            bufferInfo.presentationTimeUs += videoSampleTime;
            mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            //该方法放在前面会导致首帧录屏
            extractor.advance();
        }
        //释放音轨
        extractor.unselectTrack(index);
        mediaMuxer.stop();
        //内部也会执行stop，所以可以不用执行stop
        mediaMuxer.release();
    }
    /**
     * 通过帧率来计算：适用于视频
     */
    static public long getSampleTime(MediaFormat mediaFormat) {
        //每秒多少帧
        int frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
        //得出平均每一帧间隔多少微妙
        return 1000 * 1000 / frameRate;
    }

    /**
     * 通过设置PTS的办法：适用于音频，该方法使得视频播放变慢
     */
    static public long getSampleTime(MediaExtractor audioExtractor, ByteBuffer buffer) {
        long videoSampleTime;
        audioExtractor.readSampleData(buffer, 0);
        //skip first I frame
        if (audioExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC)
            audioExtractor.advance();
        audioExtractor.readSampleData(buffer, 0);
        long firstVideoPTS = audioExtractor.getSampleTime();
        audioExtractor.advance();
        audioExtractor.readSampleData(buffer, 0);
        long SecondVideoPTS = audioExtractor.getSampleTime();
        videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
        return videoSampleTime;
    }
    private static int selectTrack(MediaExtractor extractor, int requestCode) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (requestCode == 0 && mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d("selectTrack", "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
            if (requestCode == 1 && mime.startsWith("audio/")){
                return  i;
            }
        }
        return -1;
    }

    static public MediaCodec initMediaCodec(MediaFormat mediaFormat) throws IOException {
        MediaCodec decoder = null;
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
        if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
            Log.i("initMediaCodec", "set decode color format to type " + decodeColorFormat);
        } else {
            Log.i("initMediaCodec", "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
        }
        return decoder;
    }

    static private MediaFormat initMediaFormat(Uri uri, MediaExtractor extractor, int requestCode) {
        int trackIndex = selectTrack(extractor, requestCode);
        if (trackIndex < 0) {
            throw new RuntimeException("No video or audio track found in " + uri.toString());
        }
        extractor.selectTrack(trackIndex);
        MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
        return mediaFormat;
    }

    static private MediaExtractor initMediaExtractor(Context context, Uri uri) throws IOException {
        MediaExtractor extractor = null;
        extractor = new MediaExtractor();
        extractor.setDataSource(context, uri, null);
        return extractor;
    }

    static private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
    }

    static private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.
            CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param context
     * @param picsDri
     * @return
     */
    static public String convertVideoBySourcePics(Context context, String picsDri, String audiopath) {

        SeekableByteChannel out = null;
        File dirpath = context.getExternalFilesDir("");
        String fileString = dirpath + File.separator;
        File file = new File(fileString);
//        File destDir = new File(Environment.getExternalStorageDirectory() + "/FunVideo_Video");
        if (!file.exists()) {
            file.mkdirs();
        }
        File file_old = new File(file.getPath() + "/funvideo_" + System.currentTimeMillis() + ".mp4");
        try {
//            file.createNewFile();
            // for Android use: AndroidSequenceEncoder
            File _piscDri = new File(picsDri);
            AndroidSequenceEncoder encoder = AndroidSequenceEncoder.createSequenceEncoder(file_old, 25);
//            File[] s = _piscDri.listFiles();
            for (File childFile : _piscDri.listFiles()) {
                Bitmap bitmap = BitmapUtils.getBitmapByUri(context, Uri.fromFile(childFile));
                encoder.encodeImage(bitmap);
                bitmap.recycle();
            }
            encoder.finish();

            File file_out = new File(file.getPath() + "/funvideo_" + System.currentTimeMillis() + ".mp4");
            startComposeTrack(file_old.getPath(), audiopath, file_out.getPath());


        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(out);
        }

        return file_old.getPath();
    }

    /**
     * @param videoPath  源视频路径
     * @param audioPath  源音频路径
     * @param outPath    输出路径
     */
    static public void startComposeTrack(String videoPath, String audioPath, String outPath) {
        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoPath);
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioPath);

            MediaMuxer muxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            int videoTrack = muxer.addTrack(videoFormat);
            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
            int audioTrack = muxer.addTrack(audioFormat);

            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            muxer.start();
            while (!sawEOS) {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS = true;
                    videoBufferInfo.size = 0;
                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    //noinspection WrongConstant
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();
                    frameCount++;
                }
            }

            boolean sawEOS2 = false;
            int frameCount2 = 0;
            while (!sawEOS2) {
                frameCount2++;
                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS2 = true;
                    audioBufferInfo.size = 0;
                } else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();

                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                    audioExtractor.advance();
                }
            }
            muxer.stop();
            muxer.release();
            audioExtractor.release();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MediaPlayer getVideoMediaPlayer(Context context, Uri uri) {
        try {
            return MediaPlayer.create(context, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
