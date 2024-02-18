package com.jnngl.framedimage.api;

import com.jnngl.framedimage.FrameDisplay;
import com.jnngl.framedimage.FramedImage;
import com.jnngl.framedimage.util.BlockUtil;
import com.jnngl.framedimage.util.ImageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FramedImageAPI {
    private final FramedImage plugin;
    public FramedImageAPI(FramedImage plugin){
        this.plugin = plugin;
    }

    public List<BufferedImage> getImageOf(String link){
        try {
            return ImageUtil.readFrames(link);
        } catch (IOException e) {
            return null;
        }
    }

    public FrameDisplay createMap(Player player, List<BufferedImage> image, int width, int height){
        BlockFace blockFace = BlockUtil.getBlockFace(player.getLocation().getYaw());

        Block block = player.getTargetBlock(null, 10);
        if (block.isEmpty()) {
            return null;
        }

        Location location = BlockUtil.getNextBlockLocation(block.getLocation(), blockFace);
        return createMap(player.getUniqueId(),location,blockFace,width, height,image);

    }

    public FrameDisplay createMap(UUID player, Location location, BlockFace face, int width, int height, List<BufferedImage> image){
        FrameDisplay display = new FrameDisplay(plugin, location, face, width, height, image, player);
        plugin.add(display);
        plugin.saveFrames();
        return display;
    }

    public FrameDisplay move(FrameDisplay display,Location location){
        plugin.remove(display);
        FrameDisplay frameDisplay = new FrameDisplay(plugin, location, display.getFace(), display.getWidth(), display.getHeight(), display.getFrames(), display.getUUID(), display.getPlayer());
        plugin.add(frameDisplay);
        plugin.saveFrames();
        return display;
    }

    public void removeMap(FrameDisplay display){
        plugin.remove(display);
    }

    public void removeMaps(List<FrameDisplay> displays){
        for (FrameDisplay display : displays) {
            removeMap(display);
        }

    }

    public FrameDisplay getMap(UUID uuid){
        for (Map.Entry<String, List<FrameDisplay>> entry : plugin.getDisplays().entrySet()) {
            for (FrameDisplay frameDisplay : entry.getValue()) {
                if (frameDisplay.getUUID().equals(uuid)) return frameDisplay;
            }
        }
        return null;
    }

    public List<FrameDisplay> getPlayerMaps(Player player){
        UUID playerUniqueId = player.getUniqueId();
        List<FrameDisplay> map = new ArrayList<>();
        for (Map.Entry<String, List<FrameDisplay>> entry : plugin.getDisplays().entrySet()) {
            for (FrameDisplay frameDisplay : entry.getValue()) {
                if (frameDisplay.getPlayer().equals(playerUniqueId)) map.add(frameDisplay);
            }
        }
        return map;
    }

    public void reload(){
        plugin.reload();
    }


}
