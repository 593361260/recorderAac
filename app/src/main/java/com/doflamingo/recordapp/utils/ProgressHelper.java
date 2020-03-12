package com.doflamingo.recordapp.utils;

import android.os.Handler;
import android.os.Message;


/**
 * 进度条的刷新任务
 * <p>
 * Created by yzm on 2018/3/26.
 */

public class ProgressHelper extends Handler implements Runnable {
    private long delay = 500;

    public ProgressHelper(long delay) {
        super();
        this.delay = delay;
    }

    public ProgressHelper() {
        super();
    }

    /**
     * 开始执行扫描
     */
    public void start() {
        stop();
        postDelayed(this, delay);
    }

    @Override
    public void handleMessage(Message msg) {

    }

    /**
     * 停止任务
     */
    public void stop() {
        removeCallbacks(this);
    }

    @Override
    public void run() {
        doBackground();
        sendEmptyMessage(0);
        postDelayed(this, delay);
    }

    /**
     * 后台处理
     */
    protected void doBackground() {

    }
}
