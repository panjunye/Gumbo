package io.junye.gumbo.lib;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import io.junye.gumbo.lib.util.ApkUtils;

/**
 * Created by Junye on 2017/3/23 0023.
 *
 */

public class Gumbo {
    
    public static final String TAG = Gumbo.class.getSimpleName();

    public static final String DEBUG_TAG = "Gumbo";

    public static final String SP_DOWNLOAD_APK_ID = "io.junye.gumbo.lib.sp.DownloadApkId";

    public static final String SP_UPDATE_INFO = "io.junye.gumbo.lib.sp.UpdateInfo";

    private static String UPDATE_URL;

    private static String APP_KEY;

    private String appName;

    private boolean allowDelta;


    public static void setUpdateUrl(String updateUrl){
        if(TextUtils.isEmpty(updateUrl)){
            throw new IllegalArgumentException("UpdateUrl must not be null");
        }

        if(!updateUrl.endsWith("/")){
            throw new IllegalArgumentException("UpdateUrl must ends with / ");
        }
        UPDATE_URL = updateUrl;
    }

    public static void setAppKey(String appKey){
        if(TextUtils.isEmpty(appKey)){
            throw new IllegalArgumentException("AppKey must not be null");
        }
        APP_KEY = appKey;
    }

    private Context mContext;

    private UpdateListener mListener;

    private UpdateInfo mUpdateInfo;

    private Gumbo(Context context) {
        this.mContext = context;
    }

    public static class Builder{

        private Context context;

        private String appName;

        private boolean allowDelta;

        public Builder(Context context) {

            this.context = context;

        }

        public Gumbo build(){
            if(null == UPDATE_URL || null == APP_KEY){
                throw new RuntimeException("必须设置UPDATE_URL和APP_KEY");
            }

            Gumbo gumbo = new Gumbo(context);

            if(appName == null){
                appName = "新版本";
            }

            gumbo.setAppName(appName);

            return gumbo;

        }

        public String getAppName() {
            return appName;
        }

        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public boolean isAllowDelta() {
            return allowDelta;
        }

        public Builder setAllowDelta(boolean allowDelta) {
            this.allowDelta = allowDelta;
            return this;
        }
    }

    public void checkUpdate() {
        // 通知正在获取更新信息
        notifyOnLoading();
        CheckUpdateTask task = new CheckUpdateTask(buildUpdateUrl());
        task.setResponseListener(new BaseAsyncTask.ResponseListener<UpdateInfo>() {
            @Override
            public void onFinish(UpdateInfo info) {
                Log.d(TAG, "onFinish: 检测更新完成");
                if (info != null) {
                    if (info.isUpdate()) {
                        Log.d(TAG, "onFinish: 需要更新");
                        mUpdateInfo = info;
                        if(ApkUtils.getDownloadedApk(mContext,mUpdateInfo) != null){
                            info.setDownloaded(true);
                        }
                        info.setTitle(buildTitle(info));
                        notifyOnUpdate(info);
                    }else{
                        Log.d(TAG, "onFinish: ");
                        notifyOnLatest();
                    }
                } else {
                    notifyOnFailed();
                }
            }
        });
        task.execute();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isAllowDelta() {
        return allowDelta;
    }

    public void setAllowDelta(boolean allowDelta) {
        this.allowDelta = allowDelta;
    }

    private String buildTitle(UpdateInfo info){

        StringBuilder titleBuilder = new StringBuilder();

        titleBuilder
                .append(getAppName())
                .append(":")
                .append(info.getVersionName())
                .append("(");

        if(info.isDownloaded()){
            titleBuilder.append("已下载)");
        }else{
            if(info.isDelta()){
                //增量更新
                titleBuilder.append("增量包").append(getMbSize(info.getPatchSize()));
            }else{
                // 全量更新
                titleBuilder.append("安装包").append(getMbSize(info.getTargetSize()));
            }
            titleBuilder.append("MB)");
        }

        return titleBuilder.toString();
    }

    private static double getMbSize(double size){
        return Math.round(size / 1024.0 / 1024.0 * 100) / 100.0;
    }


    private String buildUpdateUrl(){
        return Gumbo.UPDATE_URL + "?appKey=" + Gumbo.APP_KEY + "&versionCode=" + ApkUtils.getVersionCode(mContext);
    }

    /**
     * 用户决定下载APK
     */
    public void install(){

        if(mUpdateInfo == null){
            return;
        }

        // 判断安装文件是否存在
        Uri uri = ApkUtils.getDownloadedApk(mContext,mUpdateInfo);

        if(uri != null){
            Log.d(DEBUG_TAG,"APK已下载，直接安装");
            ApkUtils.installApk(mContext,uri);
        }else{
            Log.d(DEBUG_TAG,"APK还没有安装,准备下载");
            ApkUtils.download(mContext,mUpdateInfo,allowDelta, appName + mUpdateInfo.getVersionName());
        }

    }

    private void notifyOnUpdate(UpdateInfo info) {
        if(mListener != null){
            mListener.onUpdate(info);
        }
    }

    private void notifyOnLatest() {
        if(mListener != null){
            mListener.onLatest();
        }
    }

    private void notifyOnFailed() {
        if(mListener != null){
            mListener.onFailed();
        }
    }

    private void notifyOnLoading() {
        if(mListener != null){
            mListener.onLoading();
        }
    }

    public UpdateListener getListener() {
        return mListener;
    }

    public void setListener(UpdateListener listener) {
        this.mListener = listener;
    }
}
