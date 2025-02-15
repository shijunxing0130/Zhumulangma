package com.gykj.zhumulangma.listen.mvvm.viewmodel;

import android.app.Application;
import android.support.annotation.NonNull;

import com.blankj.utilcode.util.CollectionUtils;
import com.gykj.zhumulangma.common.bean.PlayHistoryBean;
import com.gykj.zhumulangma.common.bean.SubscribeBean;
import com.gykj.zhumulangma.common.db.PlayHistoryBeanDao;
import com.gykj.zhumulangma.common.db.SubscribeBeanDao;
import com.gykj.zhumulangma.common.event.SingleLiveEvent;
import com.gykj.zhumulangma.common.extra.RxField;
import com.gykj.zhumulangma.common.mvvm.model.ZhumulangmaModel;
import com.gykj.zhumulangma.common.mvvm.viewmodel.BaseRefreshViewModel;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.GussLikeAlbumList;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Author: Thomas.
 * <br/>Date: 2019/9/20 10:01
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:
 */
public class SubscribeViewModel extends BaseRefreshViewModel<ZhumulangmaModel, SubscribeBean> {
    private SingleLiveEvent<List<SubscribeBean>> mInitSubscribesEvent;
    private SingleLiveEvent<List<Album>> mLikesEvent;
    private SingleLiveEvent<Album> mSubscribeEvent;
    private SingleLiveEvent<Album> mUnSubscribeEvent;
    private static final int PAGESIZE = 20;
    private int curPage = 1;

    public SubscribeViewModel(@NonNull Application application, ZhumulangmaModel model) {
        super(application, model);
    }

    public void unsubscribe(Album album) {
        mModel.remove(SubscribeBean.class, album.getId()).subscribe(aBoolean ->
                getUnSubscribeEvent().setValue(album), e -> e.printStackTrace());

    }

    public void subscribe(Album album) {
        mModel.insert(new SubscribeBean(album.getId(), album, System.currentTimeMillis()))
                .subscribe(subscribeBean -> getSubscribeEvent().setValue(album), e -> e.printStackTrace());
    }

    public void getSubscribes() {
        mModel.listDesc(SubscribeBean.class, curPage, PAGESIZE, SubscribeBeanDao.Properties.Datetime)
                .doFinally(() -> super.onViewRefresh())
                .subscribe(subscribeBeans ->
                {
                    if (CollectionUtils.isEmpty(subscribeBeans)) {
                        getShowEmptyViewEvent().call();
                        return;
                    }
                    getClearStatusEvent().call();
                    curPage++;
                    getInitSubscribesEvent().setValue(subscribeBeans);
                }, e -> {
                    getShowErrorViewEvent().call();
                    e.printStackTrace();
                });
    }

    private void getMoreSubscribes() {
        mModel.listDesc(SubscribeBean.class, curPage, PAGESIZE, SubscribeBeanDao.Properties.Datetime)
                .subscribe(subscribeBeans -> {
                    if (!CollectionUtils.isEmpty(subscribeBeans)) {
                        curPage++;
                    }
                    getFinishLoadmoreEvent().setValue(subscribeBeans);
                }, e -> {
                    getFinishLoadmoreEvent().call();
                    e.printStackTrace();
                });
    }

    @Override
    public void onViewRefresh() {
        curPage = 1;
        getSubscribes();
    }

    @Override
    public void onViewLoadmore() {
        getMoreSubscribes();
    }

    private long trackId = -1;

    public void play(String albumId) {
        trackId = -1;
        mModel.listDesc(PlayHistoryBean.class, 1, 1, PlayHistoryBeanDao.Properties.Datatime
                , PlayHistoryBeanDao.Properties.GroupId.eq(albumId),
                PlayHistoryBeanDao.Properties.Kind.eq(PlayableModel.KIND_TRACK))
                .doOnNext(playHistoryBeans -> {
                    if (!CollectionUtils.isEmpty(playHistoryBeans))
                        trackId = playHistoryBeans.get(0).getTrack().getDataId();
                })
                .flatMap((Function<List<PlayHistoryBean>, ObservableSource<TrackList>>) playHistoryBeans -> {
                    if (-1 == trackId) {
                        Map<String, String> map = new HashMap<>();
                        map.put(DTransferConstants.ALBUM_ID, albumId);
                        map.put(DTransferConstants.SORT, "time_desc");
                        map.put(DTransferConstants.PAGE, String.valueOf(1));
                        return mModel.getTracks(map);
                    } else {
                        Map<String, String> map = new HashMap<>();
                        map.put(DTransferConstants.ALBUM_ID, albumId);
                        map.put(DTransferConstants.SORT, "time_desc");
                        map.put(DTransferConstants.TRACK_ID, String.valueOf(trackId));
                        return mModel.getLastPlayTracks(map)
                                .map(lastPlayTrackList -> {
                                    TrackList trackList = new TrackList();
                                    trackList.cloneCommonTrackList(lastPlayTrackList);
                                    return trackList;
                                });
                    }
                })
                .subscribe(trackList -> {
                    int playIndex = 0;
                    if (trackId != -1) {
                        for (int i = 0; i < trackList.getTracks().size(); i++) {
                            if (trackList.getTracks().get(i).getDataId() == trackId) {
                                playIndex = i;
                                break;
                            }
                        }
                    }
                    XmPlayerManager.getInstance(getApplication()).playList(trackList, playIndex);
                }, Throwable::printStackTrace);
    }

    public void getGuessLikeAlbum() {
        RxField<List<Album>> albums = new RxField<>();
        Map<String, String> map = new HashMap<String, String>();
        map.put(DTransferConstants.LIKE_COUNT, "50");
        map.put(DTransferConstants.PAGE, String.valueOf(1));
        Completable.fromObservable(
                mModel.getGuessLikeAlbum(map)
                        .flatMapIterable((Function<GussLikeAlbumList, Iterable<Album>>) gussLikeAlbumList -> {
                            albums.set(gussLikeAlbumList.getAlbumList());
                            return gussLikeAlbumList.getAlbumList();
                        })
                        //利用CanDownload字段保存是否已订阅
                        .flatMap((Function<Album, ObservableSource<List<SubscribeBean>>>) album -> {
                            album.setCanDownload(false);
                            return mModel.list(SubscribeBean.class, SubscribeBeanDao.Properties.AlbumId.eq(album.getId()));
                        })
                        .observeOn(Schedulers.io())
                        .doOnNext(subscribeBeans -> {
                            if (!CollectionUtils.isEmpty(subscribeBeans)) {
                                CollectionUtils.find(albums.get(), item ->
                                        item.getId() == subscribeBeans.get(0).getAlbumId()).setCanDownload(true);
                            }
                        }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    getClearStatusEvent().call();
                    getLikesEvent().setValue(albums.get());
                },e-> {
                    getShowErrorViewEvent().call();
                    e.printStackTrace();
                });
    }


    public SingleLiveEvent<List<SubscribeBean>> getInitSubscribesEvent() {
        return mInitSubscribesEvent = createLiveData(mInitSubscribesEvent);
    }

    public SingleLiveEvent<List<Album>> getLikesEvent() {
        return mLikesEvent = createLiveData(mLikesEvent);
    }

    public SingleLiveEvent<Album> getSubscribeEvent() {
        return mSubscribeEvent = createLiveData(mSubscribeEvent);
    }

    public SingleLiveEvent<Album> getUnSubscribeEvent() {
        return mUnSubscribeEvent = createLiveData(mUnSubscribeEvent);
    }
}
