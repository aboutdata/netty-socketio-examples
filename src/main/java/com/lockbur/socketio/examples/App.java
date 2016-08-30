/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lockbur.socketio.examples;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * start app
 * Created by wangkun on 2016/8/29.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final Map<UUID, String> userMap = PlatformDependent.newConcurrentHashMap();


    public static void main(String[] args) {

        Configuration config = new Configuration();

        config.setHostname("0.0.0.0");
        //the server listen port
        config.setPort(9627);

        final SocketIOServer io = new SocketIOServer(config);

        //Connect event
        io.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                logger.info("onConnect {}", client.getSessionId());
            }
        });

        //add user event
        io.addEventListener("add user", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String username, AckRequest ackSender) throws Exception {

                client.set("username", username);

                userMap.put(client.getSessionId(), username);
                //++numUsers;
                Map<String, Integer> numUsersMap = new HashMap();
                numUsersMap.put("numUsers", userMap.size());

                client.sendEvent("login", numUsersMap);

                Map<String, Object> joined = new HashMap();
                joined.put("username", client.get("username"));
                joined.put("numUsers", userMap.size());

                io.getBroadcastOperations().sendEvent("user joined", joined);

            }
        });

        //message event
        io.addEventListener("new message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String message, AckRequest ackSender) throws Exception {
                Map<String, Object> data = new HashMap();
                data.put("username", client.get("username"));
                data.put("message", message);
                io.getBroadcastOperations().sendEvent("new message", data);
            }
        });

        //typing event
        io.addEventListener("typing", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {
                logger.info("typing {}", client.getSessionId());
                Map<String, Object> typing = new HashMap();
                typing.put("username", client.get("username"));
                io.getBroadcastOperations().sendEvent("typing", typing);
            }
        });

        //stop typing event
        io.addEventListener("stop typing", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) throws Exception {
                logger.info("stop typing{}", client.getSessionId());
                Map<String, Object> typing = new HashMap();
                typing.put("username", client.get("username"));
                io.getBroadcastOperations().sendEvent("stop typing", typing);
            }
        });

        //Disconnect event
        io.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                logger.info("onDisconnect {}", client.getSessionId());

                Map<String, Object> data = new HashMap();
                data.put("username", client.get("username"));
                data.put("numUsers", userMap.size());

                userMap.remove(client.getSessionId());

                client.del("username");

                io.getBroadcastOperations().sendEvent("user left", data);
            }
        });

        // start netty socketio server
        io.start();


        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
