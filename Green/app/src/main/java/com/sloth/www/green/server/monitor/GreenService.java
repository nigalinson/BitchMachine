package com.sloth.www.green.server.monitor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sloth.www.green.server.GreenConfig;
import com.sloth.www.green.server.eventhandler.NotificationUtils;
import com.sloth.www.green.server.utils.BroadcastUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;

public class GreenService extends BroadcastServerService implements TextToSpeech.OnInitListener{

    private static final String TAG = "PIG_PACK";
    private TextToSpeech tts;
    private final long FIXED_OPEN_DELAY = 100;

    private Handler operate = new Handler();

    private boolean isOpening = true;
    private boolean voice = true;
    private String voiceConttent1 = null;
    private String voiceConttent2 = null;
    private String voiceConttent3 = null;
    private long userOpenDelay = 0;
    private boolean rollback = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if(tts == null){
            tts = new TextToSpeech(this, this);
        }

        registerReceiver();

        refreshServiceState();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if(!isOpening){
            return;
        }

        syncEvents(event);
    }

    private synchronized void syncEvents(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        //通知单独处理
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){

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

        //屏幕变动 - 判断是否微信内的变动
        if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){

            if(event.getPackageName() == null){
                return;
            }

            if(!event.getPackageName().toString().contains("com.tencent.mm")){
                return;
            }

            handlerWeChat(event);
        }
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

    private void handlerWeChat(AccessibilityEvent event) {

        if(event.getClassName() == null){
            return;
        }
        String className = event.getClassName().toString();

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if(root == null){
            return;
        }

        if (className.contains("com.tencent.mm.ui.LauncherUI")) {
            //开始抢红包
            getPacket();
        } else if (className.contains("ReceiveUI")) {
            //开始打开红包
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(FIXED_OPEN_DELAY + userOpenDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    operate.post(new Runnable() {
                        @Override
                        public void run() {
                            openPacket();
                        }
                    });
                }
            }).start();
        }else if(className.contains("DetailUI")){
            if(rollback){
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            if(voice){
                tts.speak(TextUtils.isEmpty(voiceConttent2) ? "抢到啦" : voiceConttent1, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        //聊天详情发生变化，直接抢
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
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if( nodeInfo == null){
            return;
        }
        List<AccessibilityNodeInfo> damn = nodeInfo
                .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dam");

        if(damn != null && damn.size() > 0 && damn.get(0) != null && damn.get(0).getText() != null){
            String content = damn.get(0).getText().toString();
            if(content.contains("手慢了")){
                if(rollback){
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
                if(voice){
                    tts.speak(TextUtils.isEmpty(voiceConttent3) ? "手慢了" : voiceConttent1, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        }

        List<AccessibilityNodeInfo> open = nodeInfo
                .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dan");
        for (AccessibilityNodeInfo n : open) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

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


    @Override
    public void onInterrupt() {
        Toast.makeText(this, "结束", Toast.LENGTH_SHORT).show();
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

    private void sendToClient(String key, @Nullable Object value) {
        BroadcastUtils.send(this, GreenConfig.ACTION_CLIENT, key , value);
    }

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


}
