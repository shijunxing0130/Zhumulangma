package com.gykj.zhumulangma.home.fragment;

import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.gykj.zhumulangma.common.Constants;
import com.gykj.zhumulangma.common.databinding.CommonLayoutListBinding;
import com.gykj.zhumulangma.common.event.KeyCode;
import com.gykj.zhumulangma.common.mvvm.view.BaseRefreshMvvmFragment;
import com.gykj.zhumulangma.common.mvvm.view.status.ListSkeleton;
import com.gykj.zhumulangma.common.util.RouterUtil;
import com.gykj.zhumulangma.home.R;
import com.gykj.zhumulangma.home.adapter.AnnouncerAdapter;
import com.gykj.zhumulangma.home.mvvm.ViewModelFactory;
import com.gykj.zhumulangma.home.mvvm.viewmodel.AnnouncerListViewModel;
import com.kingja.loadsir.callback.Callback;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.ximalaya.ting.android.opensdk.model.album.Announcer;

/**
 * Author: Thomas.
 * <br/>Date: 2019/9/11 11:40
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:主播列表
 */

@Route(path = Constants.Router.Home.F_ANNOUNCER_LIST)
public class AnnouncerListFragment extends BaseRefreshMvvmFragment<CommonLayoutListBinding, AnnouncerListViewModel, Announcer>
        implements OnLoadMoreListener {

    @Autowired(name = KeyCode.Home.CATEGORY_ID)
    public long mCategoryId;
    @Autowired(name = KeyCode.Home.TITLE)
    public String mTitle;
    private AnnouncerAdapter mAnnouncerAdapter;

    @Override
    protected int onBindLayout() {
        return R.layout.common_layout_list;
    }

    @Override
    protected void initView() {
        mBinding.recyclerview.setLayoutManager(new LinearLayoutManager(mActivity));
        mBinding.recyclerview.setHasFixedSize(true);
        mAnnouncerAdapter = new AnnouncerAdapter(R.layout.home_item_announcer);
        mAnnouncerAdapter.bindToRecyclerView(mBinding.recyclerview);
        setTitle(new String[]{mTitle});
    }

    @Override
    public void initListener() {
        super.initListener();
        mAnnouncerAdapter.setOnItemClickListener((adapter, view, position) ->
                RouterUtil.navigateTo(mRouter.build(Constants.Router.Home.F_ANNOUNCER_DETAIL)
                .withLong(KeyCode.Home.ANNOUNCER_ID, mAnnouncerAdapter.getItem(position).getAnnouncerId())
                .withString(KeyCode.Home.ANNOUNCER_NAME, mAnnouncerAdapter.getItem(position).getNickname()), STANDARD));
    }

    @NonNull
    @Override
    protected WrapRefresh onBindWrapRefresh() {
        return new WrapRefresh(mBinding.refreshLayout, mAnnouncerAdapter);
    }


    @Override
    public void initData() {
        mViewModel.setCategoryId(mCategoryId);
        mViewModel.init();
    }


    @Override
    public void initViewObservable() {
        mViewModel.getInitAnnouncersEvent().observe(this, announcers -> mAnnouncerAdapter.setNewData(announcers));
    }


    @Override
    protected boolean enableLazy() {
        return false;
    }


    @Override
    public Class<AnnouncerListViewModel> onBindViewModel() {
        return AnnouncerListViewModel.class;
    }

    @Override
    public ViewModelProvider.Factory onBindViewModelFactory() {
        return ViewModelFactory.getInstance(mApplication);
    }

    @Override
    public Callback getInitStatus() {
        return new ListSkeleton();
    }
}
