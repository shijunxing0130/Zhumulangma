package com.gykj.zhumulangma.home.fragment;

import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.gykj.zhumulangma.common.AppConstants;
import com.gykj.zhumulangma.common.event.KeyCode;
import com.gykj.zhumulangma.common.mvvm.view.BaseRefreshMvvmFragment;
import com.gykj.zhumulangma.common.mvvm.view.status.ListCallback;
import com.gykj.zhumulangma.home.R;
import com.gykj.zhumulangma.home.adapter.AnnouncerTrackAdapter;
import com.gykj.zhumulangma.home.mvvm.ViewModelFactory;
import com.gykj.zhumulangma.home.mvvm.viewmodel.TrackListViewModel;
import com.kingja.loadsir.callback.Callback;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.ximalaya.ting.android.opensdk.model.track.Track;

/**
 * Author: Thomas.
 * <br/>Date: 2019/9/11 11:40
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:声音列表
 */

@Route(path = AppConstants.Router.Home.F_TRACK_LIST)
public class TrackListFragment extends BaseRefreshMvvmFragment<TrackListViewModel,Track> implements
        BaseQuickAdapter.OnItemClickListener {

    @Autowired(name = KeyCode.Home.ANNOUNCER_ID)
    public long mAnnouncerId;
    @Autowired(name = KeyCode.Home.TITLE)
    public String mTitle;
    private SmartRefreshLayout refreshLayout;
    private AnnouncerTrackAdapter mAnnouncerTrackAdapter;

    @Override
    protected int onBindLayout() {
        return R.layout.common_layout_refresh_loadmore;
    }

    @Override
    protected void initView(View view) {
        RecyclerView recyclerView = fd(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        recyclerView.setHasFixedSize(true);
        mAnnouncerTrackAdapter = new AnnouncerTrackAdapter(R.layout.home_item_announcer_track);
        mAnnouncerTrackAdapter.bindToRecyclerView(recyclerView);
        setTitle(new String[]{mTitle});
        refreshLayout = view.findViewById(R.id.refreshLayout);
    }

    @Override
    public void initListener() {
        super.initListener();
        mAnnouncerTrackAdapter.setOnItemClickListener(this);
        refreshLayout.setOnLoadMoreListener(this);
    }

    @NonNull
    @Override
    protected WrapRefresh onBindWrapRefresh() {
        return new WrapRefresh(refreshLayout,mAnnouncerTrackAdapter);
    }

    @Override
    public void initData() {
        mViewModel.setAnnouncerId(mAnnouncerId);
        mViewModel.init();
    }
    @Override
    public void initViewObservable() {
        mViewModel.getInitTrackListEvent().observe(this, tracks -> mAnnouncerTrackAdapter.setNewData(tracks));
    }
    @Override
    protected boolean enableLazy() {
        return false;
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        Track track = mAnnouncerTrackAdapter.getItem(position);
        mViewModel.play(track.getAlbum().getAlbumId(),track.getDataId());
    }

    @Override
    public Class<TrackListViewModel> onBindViewModel() {
        return TrackListViewModel.class;
    }

    @Override
    public ViewModelProvider.Factory onBindViewModelFactory() {
        return ViewModelFactory.getInstance(mApplication);
    }
     @Override
     public Callback getInitCallBack() {
        return new ListCallback();
    }
}
