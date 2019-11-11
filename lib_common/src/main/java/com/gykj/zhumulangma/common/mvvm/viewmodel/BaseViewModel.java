package com.gykj.zhumulangma.common.mvvm.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.alibaba.android.arouter.launcher.ARouter;
import com.gykj.zhumulangma.common.event.SingleLiveEvent;
import com.gykj.zhumulangma.common.mvvm.model.BaseModel;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Author: Thomas.
 * <br/>Date: 2019/8/14 13:41
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:ViewModel基类
 */
public class BaseViewModel<M extends BaseModel> extends AndroidViewModel implements IBaseViewModel, Consumer<Disposable> {
    protected M mModel;
    //Disposable容器
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    protected ARouter mRouter =ARouter.getInstance();

    private SingleLiveEvent<Void> showInitLoadViewEvent;
    private SingleLiveEvent<String> showLoadingViewEvent;
    private SingleLiveEvent<Void> showEmptyViewEvent;
    private SingleLiveEvent<Void> showNetErrViewEvent;
    private SingleLiveEvent<Void> finishSelfEvent;
    private SingleLiveEvent<Void> clearStatusEvent;

    public BaseViewModel(@NonNull Application application, M model) {
        super(application);
        this.mModel = model;
    }

    /**
     * 初始化时loading视图
     *
     * @return
     */
    public SingleLiveEvent<Void> getShowInitViewEvent() {
        return showInitLoadViewEvent = createLiveData(showInitLoadViewEvent);
    }

    /**
     * 常规loading,null:隐藏,"":不带提示,"提示":带提示文本
     *
     * @return
     */
    public SingleLiveEvent<String> getShowLoadingViewEvent() {
        return showLoadingViewEvent = createLiveData(showLoadingViewEvent);
    }

    /**
     * 数据为空
     *
     * @return
     */
    public SingleLiveEvent<Void> getShowEmptyViewEvent() {
        return showEmptyViewEvent = createLiveData(showEmptyViewEvent);
    }

    /**
     * 网络异常
     *
     * @return
     */
    public SingleLiveEvent<Void> getShowErrorViewEvent() {
        return showNetErrViewEvent = createLiveData(showNetErrViewEvent);
    }

    /**
     * 结束宿主视图
     *
     * @return
     */
    public SingleLiveEvent<Void> getFinishSelfEvent() {
        return finishSelfEvent = createLiveData(finishSelfEvent);
    }

    /**
     * 清空所有状态
     *
     * @return
     */
    public SingleLiveEvent<Void> getClearStatusEvent() {
        return clearStatusEvent = createLiveData(clearStatusEvent);
    }

    protected <T> SingleLiveEvent<T> createLiveData(SingleLiveEvent<T> liveData) {
        if (liveData == null) {
            liveData = new SingleLiveEvent<>();
        }
        return liveData;
    }

    @Override
    public void onAny(LifecycleOwner owner, Lifecycle.Event event) {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void accept(Disposable disposable) throws Exception {
        mCompositeDisposable.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mCompositeDisposable.clear();
    }


}
