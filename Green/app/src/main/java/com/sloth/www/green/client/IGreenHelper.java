package com.sloth.www.green.client;

import android.content.Context;

public interface IGreenHelper {

    /**
     * 提醒用户开启或关闭服务
     * @author nigal
     * @date 2020/1/19
     */
    void openAndCloseService(Context context);

    /**
     * 服务唤醒
     * @author nigal
     * @date 2020/1/19
     */
    void resume(Context context);
    
    /**
     * 服务睡眠 
     * @author nigal
     * @date 2020/1/19
     */
    void pause(Context context);
    
    /**
     * 是否开启语音提醒
     * @author nigal
     * @date 2020/1/19
     */
    void voiceAlarm(Context context, boolean open);

    /**
     * 语音提醒文本
     * @author nigal
     * @date 2020/1/19
     */
    void voiceAlarm(Context context, String attentionMsg, String successMsg, String failedMsg);

    /**
     * 设置抢红包延时 
     * @author nigal
     * @date 2020/1/19
     */
    void delay(Context context, long delay);

    /**
     * 是否自动返回聊天列表页面
     * @author nigal
     * @date 2020/1/19
     */
    void autoRollBackToChatList(Context context, boolean auto);

    /**
     * 判断服务是否开启
     * @author nigal
     * @date 2020/1/19
     */
    boolean isServiceRunning(Context context);

    /**
     * 注册管道
     * @author nigal
     * @date 2020/1/19
     */
    void register(Context context);

    /**
     * 取消注册管道
     * @author nigal
     * @date 2020/1/19
     */
    void unRegister(Context context);
}
