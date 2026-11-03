package com.fou.passport.test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class main {

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("ws://192.168.137.1:8080")
                .build();

        WebSocket ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // WebSocket连接已打开
                System.out.println("WebSocket连接已打开：");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // 收到消息
                System.out.println("收到消息：" + text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                // WebSocket连接已关闭
                System.out.println("onClosed：");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // 连接失败
                System.out.println("onFailure：" );
            }
        });
        ws.send("Hello, WebSocket!");




        //ws.close(NORMAL_CLOSURE_STATUS, "Goodbye!");
    }

    }

