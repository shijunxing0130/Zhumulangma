package com.gykj.zhumulangma.common.net;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.gykj.zhumulangma.common.Constants;
import com.gykj.zhumulangma.common.db.DBManager;
import com.gykj.zhumulangma.common.net.service.CommonService;
import com.gykj.zhumulangma.common.net.service.UserService;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;

import io.rx_cache2.internal.RxCache;
import io.victoralbertos.jolyglot.GsonSpeaker;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Thomas.
 * <br/>Date: 2019/9/10 8:23
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:网络请求管理类
 */
public class NetManager {
    private static final String TAG = "NetManager";
    private static File mCacheFile;
    private static volatile NetManager instance;
    private CacheProvider mCacheProvider;
    private Retrofit mRetrofit;
    private int mNetStatus = Constans.NET_ONLINE;
    private CommonService mCommonService;
    private UserService mUserService;

    public static void init(File cacheFile) {
        if (!cacheFile.exists())
            cacheFile.mkdirs();
        mCacheFile = cacheFile;
    }

    public static NetManager getInstance() {
        if (instance == null) {
            synchronized (NetManager.class) {
                if (instance == null) {
                    instance = new NetManager();
                }
            }
        }
        return instance;
    }

    private NetManager() {
        //先异步获取token
        DBManager.getInstance()
                .getSPString(Constants.SP.TOKEN)
                .compose(RxAdapter.schedulersTransformer())
                .compose(RxAdapter.exceptionTransformer())
                .subscribe(token -> {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            //添加头部信息
                            .addInterceptor(chain -> {
                                Request.Builder requestBuilder = chain.request().newBuilder()
                                        .header(Constans.TOKEN_KEY, token);
                                return chain.proceed(requestBuilder.build());
                            })
                            //动态改变baseUrl拦截器
                            .addInterceptor(new UrlInterceptor())
                            //日志
                            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            //防抓包
                            .proxy(Proxy.NO_PROXY);

                    mRetrofit = new Retrofit.Builder()
                            .client(builder.build())
                            .baseUrl(Constans.ONLINE_HOST1)
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    mCacheProvider = new RxCache.Builder()
                            .persistence(mCacheFile, new GsonSpeaker())
                            .using(CacheProvider.class);
                });
    }

    /**
     * 获取缓存对象
     *
     * @return
     */
    public CacheProvider getCacheProvider() {
        return mCacheProvider;
    }

    public CommonService getCommonService() {
        if (mCommonService == null) {
            mCommonService = mRetrofit.create(CommonService.class);
        }
        return mCommonService;
    }

    public UserService getUserService() {
        if (mUserService == null) {
            mUserService = mRetrofit.create(UserService.class);
        }
        return mUserService;
    }

    /**
     * 切换网络环境(默认在线)
     *
     * @param netStatus Constans.NET_OFFLINE(离线)/Constans.NET_ONLINE(在线)
     */
    public void setNetStatus(int netStatus) {
        Log.d(TAG, "setNetStatus() called with: netStatus = [" + netStatus + "]");
        this.mNetStatus = netStatus;
    }

    /**
     * 动态更换主机
     */
    class UrlInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            //获取request
            Request request = chain.request();
            //从request中获取原有的HttpUrl实例oldHttpUrl
            HttpUrl oldUrl = request.url();
            //获取request的创建者builder
            Request.Builder builder = request.newBuilder();
            //从request中获取headers，通过给定的键url_name
            String hostValue = request.header(Constans.HOST_KEY);
            if (TextUtils.isEmpty(hostValue)) {
                return chain.proceed(request);
            }
            //如果有这个header，先将配置的header删除，因此header仅用作app和okhttp之间使用
            builder.removeHeader(Constans.HOST_KEY);
            HttpUrl newBaseUrl = getBaseUrl(hostValue);
            if (null == newBaseUrl) {
                return chain.proceed(request);
            }
            //重建新的HttpUrl，修改需要修改的url部分
            HttpUrl newUrl = oldUrl
                    .newBuilder()
                    .host(newBaseUrl.host())//更换主机名
                    .port(newBaseUrl.port())//更换端口
                    .build();
            Log.d(TAG, "Url重定向:" + newUrl.toString());
            return chain.proceed(builder.url(newUrl).build());
        }
    }

    /**
     * 根据header和netStatus组合baseUrl
     *
     * @param hostValue
     * @return
     */
    private HttpUrl getBaseUrl(String hostValue) {
        if (Constans.HOST1_VALUE.equals(hostValue) && mNetStatus == Constans.NET_OFFLINE) {
            return HttpUrl.parse(Constans.OFFLINE_HOST1);
        } else if (Constans.HOST1_VALUE.equals(hostValue) && mNetStatus == Constans.NET_ONLINE) {
            return HttpUrl.parse(Constans.ONLINE_HOST1);
        } else if (Constans.HOST2_VALUE.equals(hostValue) && mNetStatus == Constans.NET_OFFLINE) {
            return HttpUrl.parse(Constans.OFFLINE_HOST2);
        } else if (Constans.HOST2_VALUE.equals(hostValue) && mNetStatus == Constans.NET_ONLINE) {
            return HttpUrl.parse(Constans.ONLINE_HOST2);
        }
        return null;
    }
}