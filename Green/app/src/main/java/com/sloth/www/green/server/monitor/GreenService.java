package com.sloth.www.green.server.monitor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sloth.www.green.server.GreenConfig;
import com.sloth.www.green.server.eventhandler.NotificationUtils;
import com.sloth.www.green.server.utils.BroadcastUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class GreenService extends BroadcastServerService implements TextToSpeech.OnInitListener, TaskHandlerCallback {

    //region 常量

    private static final String TAG = "PIG_PACK";
    private static final long SCHEDULE_DURATION = 20L;
    private final static TaskHandler handler = new TaskHandler();

    static final class TaskHandler extends Handler{
        private TaskHandlerCallback callback;

        public void setCallback(TaskHandlerCallback callback) {
            this.callback = callback;
        }

        public void destroy(){
            this.callback = null;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(this.callback != null){
                this.callback.tick();
            }
        }
    }

    //endregion 常量
    
    //region 变量

    private TextToSpeech tts;
    private boolean isOpening = true;
    private boolean voice = true;
    private String voiceConttent1 = null;
    private String voiceConttent2 = null;
    private String voiceConttent3 = null;
    private long userOpenDelay = 0;
    private boolean rollback = false;

    //是否在微信界面
    private boolean isInWeChat = false;

    private Timer timer;
    private TimerTask task;
    
    //endregion 变量

    //region 生命周期

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if(tts == null){
            tts = new TextToSpeech(this, this);
        }

        registerReceiver();

        refreshServiceState();

        initTimer();
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "结束", Toast.LENGTH_SHORT).show();
        destroyTimer();
    }

    private void initTimer() {
        handler.setCallback(this);
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };

        timer.schedule(task, 0, SCHEDULE_DURATION);
    }

    private void destroyTimer() {
        if(timer != null && task != null){
            timer.cancel();
            task.cancel();
            timer = null;
            task = null;
        }
        handler.destroy();
    }

    @Override
    public void onInit(int status) {
        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS){
            //默认设定语言为中文，原生的android貌似不支持中文。
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this, "手机不支持中文", Toast.LENGTH_SHORT).show();
            }else{
                //不支持中文就将语言设置为英文
                tts.setLanguage(Locale.US);
            }
        }
    }

    private void refreshServiceState() {
        sendToClient("opening", isOpening);

        new NotificationUtils(this).sendNotification("正在运行", isOpening ? "已开启" : "已暂停");
    }

    private void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        //将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        Log.d(TAG, "事件----> 打开通知栏消息 " + event);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    //endregion 生命周期

    //region 循环

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void tick() {
        if(!isOpening){
            return;
        }

        if(!isInWeChat){
            return;
        }

        final AccessibilityNodeInfo root = getWxWindow();

        if(root == null){
            return;
        }

        //抢红包页面 - 判断是否已抢
        List<AccessibilityNodeInfo> damn = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dam");
        if(damn != null && damn.size() > 0 && damn.get(0) != null && damn.get(0).getText() != null){
            String content = damn.get(0).getText().toString();
            if(content.contains("手慢了")){
                if(rollback){
                    closeRedDialog(root);
                }
                if(voice){
                    tts.speak(TextUtils.isEmpty(voiceConttent3) ? "手慢了" : voiceConttent3, TextToSpeech.QUEUE_FLUSH, null);
                }
            }else{
                final Handler op = new Handler();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(userOpenDelay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        op.post(new Runnable() {
                            @Override
                            public void run() {
                                List<AccessibilityNodeInfo> pickingButtons = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dan");
                                if(pickingButtons != null && pickingButtons.size() > 0){
                                    for (AccessibilityNodeInfo n : pickingButtons) {
                                        performBlurClickInner(n);
                                    }
                                }
                            }
                        });
                    }
                }).start();
            }
            return;
        }

        //聊天详情发生变化，点开红包
        List<AccessibilityNodeInfo> chatMsgLists = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/fdg");
        if(chatMsgLists != null && chatMsgLists.size() > 0){
            getPacket();
            return;
        }

        //聊天列表发生变化，找到发生变化的条目，并进入
        List<AccessibilityNodeInfo> chatPersonLists = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bah");
        if(chatPersonLists != null && chatPersonLists.size() > 0){
            chooseAndEnterPerson(chatPersonLists);
            return;
        }

    }

    private void closeRedDialog(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> close = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/d84");
        if(close != null && close.size() > 0){
            for (AccessibilityNodeInfo n : close) {
                performBlurClickInner(n);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private AccessibilityNodeInfo getWxWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if(windows == null || windows.size() == 0){
            return null;
        }

        for(AccessibilityWindowInfo window: windows){
            if(window == null || window.getTitle() == null){
                continue;
            }
            if("微信".equals(window.getTitle().toString())){
                return window.getRoot();
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AccessibilityNodeInfo getTopWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if(windows == null || windows.size() == 0 || windows.get(0) == null){
            return null;
        }

        return windows.get(0).getRoot();
    }

    //endregion 循环

    //region 屏幕事件

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        syncEvents(event);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private synchronized void syncEvents(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        //通知单独处理
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){

            if(!isOpening){
                return;
            }

            if(event.getText() == null){
                return;
            }

            if(event.getText().toString().contains("[微信红包]")){
                if(voice){
                    tts.speak(TextUtils.isEmpty(voiceConttent1) ? "快，有红包" : voiceConttent1, TextToSpeech.QUEUE_FLUSH, null);
                }

                openNotify(event);

            }

            return;
        }

        //屏幕变动 - 刷新变量状态
        if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){

            if(event.getPackageName() == null){
                return;
            }

            String packName = event.getPackageName().toString();
            if(packName.contains("system")){
                //系统变动，当作噪声忽略
            }else if(packName.contains("com.tencent.mm")){
                //微信
                isInWeChat = true;
                //微信中某些特殊事件需要处理
                handlerWeChat(event);
            }else{
                isInWeChat = false;
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handlerWeChat(AccessibilityEvent event) {

        if(event.getClassName() == null){
            return;
        }
        String className = event.getClassName().toString();

        AccessibilityNodeInfo root = getWxWindow();

        if(root == null){
            return;
        }

        if(className.contains("DetailUI")){
            //详情页 - 直接结束
            if(rollback){
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            if(voice){
                tts.speak(TextUtils.isEmpty(voiceConttent2) ? "抢到啦" : voiceConttent2, TextToSpeech.QUEUE_FLUSH, null);
            }
        }else{
            //其他由TICK完成
        }

    }

    private void performBlurClickOuter(AccessibilityNodeInfo node) {
        if(node == null){
            return;
        }
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        AccessibilityNodeInfo parent = node.getParent();
        while(parent != null){
            if(parent.isClickable()){
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            parent = parent.getParent();
        }

    }

    private void performBlurClickInner(AccessibilityNodeInfo node) {
        if(node == null){
            return;
        }

        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if(node.getChildCount() > 0){
            for(int i = 0; i < node.getChildCount(); i++){
                performBlurClickInner(node.getChild(i));
            }
        }
    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getWxWindow();

        if(rootNode == null){
            return;
        }

        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ag");
        if(list == null || list.size() == 0){
            return;
        }

        recycleList(list.get(0));
    }


    /**
     * 打印一个节点的结构
     */
    @SuppressLint("NewApi")
    public void recycleList(AccessibilityNodeInfo list) {
        for(int i = 0; i < list.getChildCount(); i++){
            AccessibilityNodeInfo[] clickable = new AccessibilityNodeInfo[1];
            getPackInitem(clickable, list.getChild(i));
            if(clickable[0] != null){
                AccessibilityNodeInfo[] disable = new AccessibilityNodeInfo[1];
                getPackDisableInitem(disable, list.getChild(i));
                if(disable[0] == null){

                    performBlurClickOuter(clickable[0]);

                    break;
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getWxWindow();
        if( nodeInfo == null){
            return;
        }

        List<AccessibilityNodeInfo> open = nodeInfo
                .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dan");
        for (AccessibilityNodeInfo n : open) {
            performBlurClickInner(n);
        }

    }

    private void getPackInitem(AccessibilityNodeInfo[] result, AccessibilityNodeInfo child) {
        if(child == null){
            return;
        }
        if (child.getChildCount() == 0) {
            if(child.getText() != null){
                if("微信红包".equals(child.getText().toString())){

                    result[0] = child;
                }
            }

        } else {
            for (int i = 0; i < child.getChildCount(); i++) {
                if(child.getChild(i)!=null){
                    getPackInitem(result, child.getChild(i));
                }
            }
        }
    }

    private void getPackDisableInitem(AccessibilityNodeInfo[] result, AccessibilityNodeInfo child) {
        if (child.getChildCount() == 0) {
            if(child.getText() != null){
                if("已领取".equals(child.getText().toString()) || "已被领完".equals(child.getText().toString())){
                    result[0] = child;
                }
            }

        } else {
            for (int i = 0; i < child.getChildCount(); i++) {
                if(child.getChild(i)!=null){
                    getPackDisableInitem(result, child.getChild(i));
                }
            }
        }
    }

    private void chooseAndEnterPerson(List<AccessibilityNodeInfo> chatMsgList) {
        for(int i = 0; i < chatMsgList.size(); i++){
            AccessibilityNodeInfo node = chatMsgList.get(i);

            //【红点+ 红包消息】 √
            List<AccessibilityNodeInfo> lines = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bal");
            List<AccessibilityNodeInfo> ops = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/op");
            if(lines != null && lines.size() > 0 && lines.get(0) != null && lines.get(0).getText() != null && lines.get(0).getText().toString().contains("[微信红包]") && ops != null && ops.size() > 0){
                performBlurClickInner(node);
                return;
            }
        }
    }

    //endregion 屏幕事件

    //region 广播

    @Override
    protected void achieveBroadcast(String key, Intent intent) {
        if(GreenConfig.FUNCTIONS.RUNNING.equals(key)){
            isOpening = intent.getBooleanExtra("value", false);
        }else if(GreenConfig.FUNCTIONS.VOICE.equals(key)){
            voice = intent.getBooleanExtra("value", false);
        }else if(GreenConfig.FUNCTIONS.VOICE_CONTENT.equals(key)){
            String ori = intent.getStringExtra("value");
            if(!TextUtils.isEmpty(ori)){
                Map<String, String> content = new Gson().fromJson(ori, Map.class);
                voiceConttent1 = content.get(GreenConfig.FUNCTIONS.VOICE_CONTENT_1);
                voiceConttent2 = content.get(GreenConfig.FUNCTIONS.VOICE_CONTENT_2);
                voiceConttent3 = content.get(GreenConfig.FUNCTIONS.VOICE_CONTENT_3);
            }

        }else if(GreenConfig.FUNCTIONS.DELAY.equals(key)){
            userOpenDelay = intent.getIntExtra("value", 0);
        }else if(GreenConfig.FUNCTIONS.ROLL_BACK.equals(key)){
            rollback = intent.getBooleanExtra("value", false);
        }else{  }
    }

    private void sendToClient(String key, @Nullable Object value) {
        BroadcastUtils.send(this, GreenConfig.ACTION_CLIENT, key , value);
    }

    //endregion 广播

}
