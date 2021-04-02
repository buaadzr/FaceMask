package com.example.permissiontest.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.example.permissiontest.R;

import java.util.ArrayList;
import java.util.List;

import static com.arcsoft.face.FaceEngine.ASF_FACE_DETECT;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_0_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;
import static com.arcsoft.face.enums.DetectMode.ASF_DETECT_MODE_IMAGE;

public class FaceDection {


    static  String app_id = "7PAwmWFMr7otA6c7N3hqxtX88jWt25pasdwcHNnRWV45";
    static  String sdk_key = "2G78jMeAnZRrjwwKiUaZi2bD2mTdcfKdVnQrQNevSWvq";
    static  FaceEngine mfaceengine  =  new FaceEngine();
    static  List<FaceInfo> faceInfoList  = new ArrayList<>();
    static  int detectFaceMaxNum = 32;

    private static final String TAG = "FaceDection";

    /**
     * 激活人脸引擎
     * @param context
     */
    public static void activeSdk(Context context){
        int code = FaceEngine.activeOnline(context, app_id, sdk_key);
        if(code == ErrorInfo.MOK) {
            Log.d(TAG, "activeOnline success");
        }else if(code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            Log.d(TAG, "already activated");
        }else{
            Log.d(TAG, "activeOnline failed, code is : " + code);
        }
    }
    /**
     * 初始化人脸引擎
     * @param context
     * @return
     */
    public static boolean initEngine(Context context){
        mfaceengine = new FaceEngine();
        int faceEngineCode = mfaceengine.init(context,
                ASF_DETECT_MODE_IMAGE,
                ASF_OP_ALL_OUT,
                32,
                detectFaceMaxNum,
                ASF_FACE_DETECT);
        if (faceEngineCode == ErrorInfo.MOK){
            Log.d(TAG, "人脸引擎初始化成功");
        }else{
            Log.d(TAG, "人脸引擎初始化失败" + faceEngineCode);
        }
        return faceEngineCode == ErrorInfo.MOK;
    }

    /**
     * 获取人脸信息
     * @param originalBitmap
     */
    public static void getFaceInfo(Bitmap originalBitmap){
        //获取宽高符合要求的图像
        Bitmap bitmap = ArcSoftImageUtil.getAlignedBitmap(originalBitmap, true);
        //为图像数据分配内存
        byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(),
                bitmap.getHeight(), ArcSoftImageFormat.BGR24);
        //图像格式转换
        int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
        if(transformCode != ArcSoftImageUtilError.CODE_SUCCESS){
            Log.d(TAG, "transform failed, code is :" + transformCode);
            return;
        }
        int code = mfaceengine.detectFaces(bgr24, bitmap.getWidth(), bitmap.getHeight(), FaceEngine.CP_PAF_BGR24, faceInfoList);
        if(code == ErrorInfo.MOK && faceInfoList.size() >= 1){
            Log.d(TAG, "detect face num is/are " + faceInfoList.size());
        }else {
            Log.d(TAG, "detect failed, failed code is " + code);
        }
    }
    public static Bitmap drawFaceRect(Bitmap bitmap){
        //绘制bitmap
        bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        for(int i = 0; i < faceInfoList.size(); i++){
            //绘制人脸框
            Rect face_rect = faceInfoList.get(i).getRect();
            paint.setStyle(Paint.Style.STROKE);
//            canvas.drawRect(face_rect, paint);
//            //绘制人脸编号
//            paint.setStyle(Paint.Style.FILL_AND_STROKE);
//            paint.setTextSize(faceInfoList.get(i).getRect().width() / 2);
//            canvas.drawText("" + i, faceInfoList.get(i).getRect().left, faceInfoList.get(i).getRect().top, paint);

            int currentPixel;
            int width = (int)Math.ceil(face_rect.width());
            int height = (int)Math.ceil(face_rect.height());
            int x = face_rect.left;
            int y = face_rect.top;

            for(int j=0;j<width;j++){
                for(int k=0;k<height;k++){
                    currentPixel = bitmap.getPixel(x + j, y + k);
                    paint.setColor(currentPixel);
                    Rect rRect = new Rect(x + j,y + k,x + j + 1,y + k + 1);
                    canvas.drawRect(rRect, paint);
                }
            }

        }
        return bitmap;
    }

}
