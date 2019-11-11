package com.gykj.zhumulangma.listen.fragment;

import android.arch.lifecycle.ViewModelProvider;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemDragListener;
import com.gykj.zhumulangma.common.Constants;
import com.gykj.zhumulangma.common.event.EventCode;
import com.gykj.zhumulangma.common.event.FragmentEvent;
import com.gykj.zhumulangma.common.event.KeyCode;
import com.gykj.zhumulangma.common.mvvm.view.BaseMvvmFragment;
import com.gykj.zhumulangma.common.util.ToastUtil;
import com.gykj.zhumulangma.listen.R;
import com.gykj.zhumulangma.listen.adapter.DownloadSortAdapter;
import com.gykj.zhumulangma.listen.databinding.ListenFragmentDownloadSortBinding;
import com.gykj.zhumulangma.listen.mvvm.ViewModelFactory;
import com.gykj.zhumulangma.listen.mvvm.viewmodel.DownloadSortViewModel;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.sdkdownloader.XmDownloadManager;
import com.ximalaya.ting.android.sdkdownloader.downloadutil.IDoSomethingProgress;
import com.ximalaya.ting.android.sdkdownloader.exception.BaseRuntimeException;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Thomas.
 * <br/>Date: 2019/10/10 14:51
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:下载排序
 */
@Route(path = Constants.Router.Listen.F_DOWNLOAD_SORT)
public class DownloadSortFragment extends BaseMvvmFragment<ListenFragmentDownloadSortBinding,DownloadSortViewModel> {

    @Autowired(name = KeyCode.Listen.ALBUMID)
    public long mAlbumId;

    private DownloadSortAdapter mSortAdapter;



    @Override
    protected int onBindLayout() {
        return R.layout.listen_fragment_download_sort;
    }

    @Override
    protected void initView() {

        mBinding.recyclerview.setLayoutManager(new LinearLayoutManager(mActivity));
        mBinding.recyclerview.setHasFixedSize(true);
        mSortAdapter = new DownloadSortAdapter(R.layout.listen_item_download_sort);
        mSortAdapter.bindToRecyclerView(mBinding.recyclerview);

        ItemDragAndSwipeCallback itemDragAndSwipeCallback = new ItemDragAndSwipeCallback(mSortAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemDragAndSwipeCallback);
        itemTouchHelper.attachToRecyclerView(mBinding.recyclerview);
        // 开启拖拽
        mSortAdapter.enableDragItem(itemTouchHelper, R.id.ll_sort, false);
    }

    @Override
    public void initListener() {
        super.initListener();
        mSortAdapter.setOnItemDragListener(onItemDragListener);
    }

    @Override
    public void initData() {
        mViewModel.getDownloadTracks(mAlbumId);
    }

    @Override
    protected void initViewObservable() {
        mViewModel.getTracksEvent().observe(this, tracks -> mSortAdapter.setNewData(tracks));
    }
    @Override
    public String[] onBindBarTitleText() {
        return new String[]{"手动排序"};
    }

    @Override
    public SimpleBarStyle onBindBarRightStyle() {
        return SimpleBarStyle.RIGHT_TEXT;
    }

    @Override
    public String[] onBindBarRightText() {
        return new String[]{"完成"};
    }

    @Override
    protected boolean enableLazy() {
        return false;
    }

    @Override
    public void onRight1Click(View v) {
        super.onRight1Click(v);
        List<Track> data = mSortAdapter.getData();
        Map<Long, Integer> map = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            map.put(data.get(i).getDataId(), i);
        }
        XmDownloadManager.getInstance().swapDownloadedPosition(map, new IDoSomethingProgress() {
            @Override
            public void begin() {
                showLoadingView(null);
            }

            @Override
            public void success() {
                clearStatus();
                EventBus.getDefault().post(new FragmentEvent(EventCode.Listen.DOWNLOAD_SORT));
                pop();
            }

            @Override
            public void fail(BaseRuntimeException e) {
                clearStatus();
                e.printStackTrace();
                ToastUtil.showToast(ToastUtil.LEVEL_E, e.getLocalizedMessage());
            }
        });

    }

    private OnItemDragListener onItemDragListener = new OnItemDragListener() {
        @Override
        public void onItemDragStart(RecyclerView.ViewHolder viewHolder, int pos) {
            viewHolder.itemView.setBackgroundColor(Color.WHITE);
        }

        @Override
        public void onItemDragMoving(RecyclerView.ViewHolder source, int from, RecyclerView.ViewHolder target, int to) {

        }

        @Override
        public void onItemDragEnd(RecyclerView.ViewHolder viewHolder, int pos) {
            //去除背景色,避免过渡绘制
            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    };

    @Override
    protected Class<DownloadSortViewModel> onBindViewModel() {
        return DownloadSortViewModel.class;
    }

    @Override
    protected ViewModelProvider.Factory onBindViewModelFactory() {
        return ViewModelFactory.getInstance(mApplication);
    }

}
