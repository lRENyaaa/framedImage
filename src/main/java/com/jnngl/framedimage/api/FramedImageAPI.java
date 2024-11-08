package com.jnngl.framedimage.api;

import com.jnngl.framedimage.FrameDisplay;
import com.jnngl.framedimage.FramedImage;
import com.jnngl.framedimage.command.SubCommand;
import com.jnngl.framedimage.util.ImageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class FramedImageAPI {

    private final FramedImage plugin;

    public FramedImageAPI(FramedImage plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<FrameDisplay> create(String urlString, Location location, BlockFace blockFace, int width, int height){
        CompletableFuture<FrameDisplay> future = new CompletableFuture<>();
        plugin.getScheduler().runAsync(plugin, () -> {
            try {
                List<BufferedImage> frames = ImageUtil.readFrames(urlString);
                FrameDisplay frameDisplay = new FrameDisplay(plugin, location, blockFace, width, height, frames);
                plugin.add(frameDisplay);
                plugin.saveFrames();
                future.complete(frameDisplay);
            } catch(IOException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + e.getClass().getName() + ": " + e.getMessage());
                future.complete(null);
            }
        });
        return future;
    }

    public void create(FrameDisplay display){
        plugin.getScheduler().runAsync(plugin, () -> {
            plugin.add(display);
            plugin.saveFrames();
        });
    }

    public void remove(FrameDisplay display){
        plugin.getScheduler().runAsync(plugin, () -> {
            plugin.remove(display);
            plugin.saveFrames();
        });
    }

    public void registerSubcommand(String name, SubCommand command){
        plugin.getCommand().register(name, command);
    }

    public void unregisterSubcommand(String name){
        plugin.getCommand().unregister(name);
    }

    public List<String> getAllSubcommands(){
        return plugin.getCommand().getCommandList();
    }


}
