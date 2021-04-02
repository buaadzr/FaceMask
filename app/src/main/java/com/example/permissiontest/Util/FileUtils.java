package com.example.permissiontest.Util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaExtractor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.util.Log;

import org.jcodec.common.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class FileUtils {
    /**
     * 根据Uri加载Bitmap，并自动转正
     * @param uri
     * @return 转正后的bitmap
     */
    public static  Bitmap loadBitmap(Context context, Uri uri, boolean adjustOritation) {
        try{
            ContentResolver cr = context.getContentResolver();
            Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
            if (!adjustOritation) {
                return bitmap;
            } else {
                int digree = 0;
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(cr.openInputStream(uri));
                } catch (IOException e) {
                    e.printStackTrace();
                    exif = null;
                }
                if (exif != null) {
                    // 读取图片中相机方向信息
                    int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
                    // 计算旋转角度
                    switch (ori) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            digree = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            digree = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            digree = 270;
                            break;
                        default:
                            digree = 0;
                            break;
                    }
                }
                if (digree != 0) {
                    // 旋转图片
                    Matrix m = new Matrix();
                    m.postRotate(digree);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), m, true);
                }
                return bitmap;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  保存图片至相册
     * @param context
     * @param bitmap
     */
    public static void Save_Pic(Context context, Bitmap bitmap){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        //插入数据，返回所插入数据对应的Uri
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//        //加载应用程序res下的图片bitmap
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shuangta);
        OutputStream outputStream = null;
        try {
            //获取刚插入的数据的Uri对应的输出流
            outputStream = context.getContentResolver().openOutputStream(uri);
            //将bitmap图片保存到Uri对应的数据节点中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            //图片会保存到sd卡的pcitures目录下1487231905572.jpg
            outputStream.close();
            Log.d("GsonUtils", "outputStream=");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("GsonUtils", "Exception=" + e.toString());
        }
    }

    public static String getPathByUri(Context context, Uri uri) {
        String path;
        //使用第三方应用打开
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        //4.4以后
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            path = getPathFromNewSdk(context, uri);
            //4.4以下下系统调用方法
        } else {
            path = getPathFromOldSdk(context, uri);
        }
        return path;
    }

    /**
     * 删除缓存图片
     * @param file
     */
    public static void deleteFile(File file) {

        if (file.exists()) { // 判断文件是否存在
            if (file.isFile()) { // 判断是否是文件

                file.delete(); // delete()方法 你应该知道 是删除的意思;
            } else if (file.isDirectory()) { // 否则如果它是一个目录
                File files[] = file.listFiles(); // 声明目录下所有的文件 files[];
                for (int i = 0; i < files.length; i++) { // 遍历目录下所有的文件
                    deleteFile(files[i]); // 把每个文件 用这个方法进行迭代
                }
            }
            file.delete();
        } else {
        }
    }

    /**
     * 保存视频
     * @param context
     * @param file
     */
    public static void Save_Video(Context context, File file) {

        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        //是否添加到相册
        ContentResolver localContentResolver = context.getContentResolver();
        ContentValues localContentValues = getVideoContentValues(context, file);
        Uri localUri = localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri));

        // 扫描本地mp4文件并添加到本地视频库
//        MediaScannerConnection mMediaScanner = new MediaScannerConnection(context, null);
//        mMediaScanner.connect();
//        if (mMediaScanner !=null && mMediaScanner.isConnected()) {
//            mMediaScanner.scanFile(file.getPath(), "video/*");
//        }
    }

    public static ContentValues getVideoContentValues(Context paramContext, File paramFile) {

        long paramLong = System.currentTimeMillis();

        ContentValues localContentValues = new ContentValues();
        localContentValues.put(MediaStore.Video.Media.TITLE, paramFile.getName());
        localContentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        localContentValues.put(MediaStore.Video.Media.DATA, paramFile.getAbsolutePath());
        localContentValues.put(MediaStore.Video.Media.DISPLAY_NAME, paramFile.getName());
        localContentValues.put(MediaStore.Video.Media.DATE_TAKEN, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.DATE_MODIFIED, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.DATE_ADDED, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.SIZE, Long.valueOf(paramFile.length()));
        return localContentValues;
    }

    public static String createDri(Context context, String path) {
        File dirpath = context.getExternalFilesDir("");
        String fileString = dirpath + File.separator;
        File file = new File(fileString);
        File destDir = new File(file + "/" + path);
        if (destDir.exists()) {
            deleteFile(destDir);
        }
        destDir.mkdirs();
        return destDir.getPath();
    }

    private static String getPathFromOldSdk(Context context, Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    private static String getPathFromNewSdk(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String sFileExtensions;

    // Audio
    public static final int FILE_TYPE_MP3     = 1;
    public static final int FILE_TYPE_M4A     = 2;
    public static final int FILE_TYPE_WAV     = 3;
    public static final int FILE_TYPE_AMR     = 4;
    public static final int FILE_TYPE_AWB     = 5;
    public static final int FILE_TYPE_WMA     = 6;
    public static final int FILE_TYPE_OGG     = 7;
    private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
    private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_OGG;

    // MIDI
    public static final int FILE_TYPE_MID     = 11;
    public static final int FILE_TYPE_SMF     = 12;
    public static final int FILE_TYPE_IMY     = 13;
    private static final int FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID;
    private static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;

    // Video
    public static final int FILE_TYPE_MP4     = 21;
    public static final int FILE_TYPE_M4V     = 22;
    public static final int FILE_TYPE_3GPP    = 23;
    public static final int FILE_TYPE_3GPP2   = 24;
    public static final int FILE_TYPE_WMV     = 25;
    private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4;
    private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_WMV;

    // Image
    public static final int FILE_TYPE_JPEG    = 31;
    public static final int FILE_TYPE_GIF     = 32;
    public static final int FILE_TYPE_PNG     = 33;
    public static final int FILE_TYPE_BMP     = 34;
    public static final int FILE_TYPE_WBMP    = 35;
    private static final int FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG;
    private static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WBMP;

    // Playlist
    public static final int FILE_TYPE_M3U     = 41;
    public static final int FILE_TYPE_PLS     = 42;
    public static final int FILE_TYPE_WPL     = 43;
    private static final int FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U;
    private static final int LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_WPL;

    //静态内部类
    static class MediaFileType {

        int fileType;
        String mimeType;

        MediaFileType(int fileType, String mimeType) {
            this.fileType = fileType;
            this.mimeType = mimeType;
        }
    }

    private static HashMap<String, MediaFileType> sFileTypeMap
            = new HashMap<String, MediaFileType>();
    private static HashMap<String, Integer> sMimeTypeMap
            = new HashMap<String, Integer>();
    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
        sMimeTypeMap.put(mimeType, new Integer(fileType));
    }
    static {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg");
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4");
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav");
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
        addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma");
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg");

        addFileType("MID", FILE_TYPE_MID, "audio/midi");
        addFileType("XMF", FILE_TYPE_MID, "audio/midi");
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");

        addFileType("MP4", FILE_TYPE_MP4, "video/mp4");
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4");
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp");
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2");
        addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv");

        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg");
        addFileType("GIF", FILE_TYPE_GIF, "image/gif");
        addFileType("PNG", FILE_TYPE_PNG, "image/png");
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp");
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");

        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl");
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls");
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl");

        // compute file extensions list for native Media Scanner
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = sFileTypeMap.keySet().iterator();

        while (iterator.hasNext()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(iterator.next());
        }
        sFileExtensions = builder.toString();
    }

    public static final String UNKNOWN_STRING = "<unknown>";

    public static boolean isAudioFileType(int fileType) {
        return ((fileType >= FIRST_AUDIO_FILE_TYPE &&
                fileType <= LAST_AUDIO_FILE_TYPE) ||
                (fileType >= FIRST_MIDI_FILE_TYPE &&
                        fileType <= LAST_MIDI_FILE_TYPE));
    }

    public static boolean isVideoFileType(int fileType) {
        return (fileType >= FIRST_VIDEO_FILE_TYPE &&
                fileType <= LAST_VIDEO_FILE_TYPE);
    }

    public static boolean isImageFileType(int fileType) {
        return (fileType >= FIRST_IMAGE_FILE_TYPE &&
                fileType <= LAST_IMAGE_FILE_TYPE);
    }

    public static boolean isPlayListFileType(int fileType) {
        return (fileType >= FIRST_PLAYLIST_FILE_TYPE &&
                fileType <= LAST_PLAYLIST_FILE_TYPE);
    }

    public static MediaFileType getFileType(String path) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0)
            return null;
        return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
    }

    //根据视频文件路径判断文件类型
    public static boolean isVideoFileType(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isVideoFileType(type.fileType);
        }
        return false;
    }

    //根据音频文件路径判断文件类型
    public static boolean isAudioFileType(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isAudioFileType(type.fileType);
        }
        return false;
    }

    //根据mime类型查看文件类型
    public static int getFileTypeForMimeType(String mimeType) {
        Integer value = sMimeTypeMap.get(mimeType);
        return (value == null ? 0 : value.intValue());
    }

    //根据图片文件路径判断文件类型
    public static boolean isImageFileType(String path) {
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isImageFileType(type.fileType);
        }
        return false;
    }
}
