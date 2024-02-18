package com.jnngl.framedimage.protocol;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChannels {
    private final Map<String, Channel> playerChannels = new ConcurrentHashMap<>();

    public Channel get(Player player){
        return get(player.getName());
    }

    public Channel get(String name){
        return playerChannels.get(name);
    }

    public void add(String name, Channel channel) {
        playerChannels.put(name, channel);
    }

    public void remove(String name){
        playerChannels.remove(name);
    }

    public void clear(){
        playerChannels.clear();
    }



}
