package com.example.permissiontest.Util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;


import java.io.File;

public class temp {
    public static void startConvert(final Context context, final Uri uri){
        VideoUtils.VideoInfo vi = VideoUtils.getVideoInfo(context, uri);
        Log.i("startConvertVideo", "width:" + vi.width + " height:" + vi.height + " time:" + vi.time);

//        emitter.onNext(10);
        String framePath = FileUtils.createDri(context, "FunVideo_CachePic_Source");
        String Path = FileUtils.createDri(context, "FunVideo_CacheAudio_Source");
        String audioPath = Path + File.separator + System.currentTimeMillis() + ".mp3";
//        emitter.onNext(30);


        if (VideoUtils.initFrameFromVideoBySecond(context, audioPath, uri, vi.width, vi.height, vi.time)) {
//            emitter.onNext(50);
            String old_viode_path = VideoUtils.convertVideoBySourcePics(context, framePath, audioPath);
//            emitter.onComplete();
            FileUtils.deleteFile(new File(framePath));
            FileUtils.deleteFile(new File(audioPath));
            FileUtils.deleteFile(new File(old_viode_path));
        } else {

        }
    }
}
