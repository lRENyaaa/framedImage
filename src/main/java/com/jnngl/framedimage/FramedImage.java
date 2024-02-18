/*
 *  Copyright (C) 2022  JNNGL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnngl.framedimage;

import com.jnngl.framedimage.api.FramedImageAPI;
import com.jnngl.framedimage.config.Config;
import com.jnngl.framedimage.config.Frames;
import com.jnngl.framedimage.injection.Injector;
import com.jnngl.framedimage.listener.HandshakeListener;
import com.jnngl.framedimage.listener.MoveListener;
import com.jnngl.framedimage.listener.PlayerListener;
import com.jnngl.framedimage.protocol.Packet;
import com.jnngl.framedimage.protocol.PlayerChannels;
import com.jnngl.framedimage.scheduler.BukkitTaskScheduler;
import com.jnngl.framedimage.scheduler.CancellableTask;
import com.jnngl.framedimage.scheduler.FoliaTaskScheduler;
import com.jnngl.framedimage.scheduler.TaskScheduler;
import com.jnngl.framedimage.sections.SectionUtil;
import com.jnngl.mapcolor.ColorMatcher;
import com.jnngl.mapcolor.matchers.BufferedImageMatcher;
import com.jnngl.mapcolor.matchers.CachedColorMatcher;
import com.jnngl.mapcolor.palette.Palette;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FramedImage extends JavaPlugin {

    private final Injector injector = new Injector();
    private final File configFile = new File(getDataFolder(), "config.yml");
    private final File framesFile = new File(getDataFolder(), "frames.yml");
    private final Map<String, Map<Long, List<FrameDisplay>>> sectionDisplays = new ConcurrentHashMap<>();

    // 当前向玩家展示的FrameDisplay
    private final Map<String, Set<FrameDisplay>> playerDisplays = new ConcurrentHashMap<>();

    // 玩家数据包Channel
    private final PlayerChannels playerChannels = new PlayerChannels();

    // 全部的FrameDisplay(World,DisplayList)
    private final Map<String, List<FrameDisplay>> displays = new ConcurrentHashMap<>();

    // 不知道啥玩意
    private final Map<Palette, ColorMatcher> colorMatchers = new ConcurrentHashMap<>();

    // 动态的FrameDisplay
    private final Map<FrameDisplay, CancellableTask> updatableDisplays = new ConcurrentHashMap<>();

    // 登录中的玩家
    private final Set<String> loggingPlayers = ConcurrentHashMap.newKeySet();
    private final TaskScheduler scheduler;
    private String encoderContext = null;
    private MoveListener moveListener = null;

    // api
    private static FramedImageAPI api;


    // folia
    {
        TaskScheduler scheduler;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            getLogger().info("Using folia scheduler");
            scheduler = new FoliaTaskScheduler();
        } catch (ClassNotFoundException e) {
            getLogger().info("Using bukkit scheduler");
            scheduler = new BukkitTaskScheduler();
        }

        this.scheduler = scheduler;
    }

    @Override
    public void onEnable() {
        // 应该是注入数据包通道
        injector.addInjector(channel -> channel.pipeline().addAfter("splitter", "framedimage:handshake", new HandshakeListener(this)));

        injector.inject();
        getLogger().info("Successfully injected!");

        // 加载主要内容
        scheduler.runDelayed(this, this::reload);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // API
        api = new FramedImageAPI(this);
    }

    public static FramedImageAPI getAPI(){
      return api;
    }

    @Override
    public void onDisable() {
        // 移除全部FrameDisplay
        removeAll();

        // 移除全部通道列表
        playerChannels.clear();
    }

    // 获取玩家通道
    public PlayerChannels getPlayerChannels() {
        return playerChannels;
    }

    public Map<String, List<FrameDisplay>> getDisplays() {
        return displays;
    }

    public Set<String> getLoggingPlayers() {
        return loggingPlayers;
    }

    public List<FrameDisplay> getSectionDisplays(String world, long section) {
        Map<Long, List<FrameDisplay>> sectionMap = sectionDisplays.getOrDefault(world, Collections.emptyMap());
        return sectionMap.getOrDefault(section, Collections.emptyList());
    }

    // 发包,在生成地图画(spawn),显示/填充地图画(display/displayNext),删除地图画(delete)处有用
    public void writePacket(Channel channel, Packet packet) {
        ChannelHandlerContext context = encoderContext != null ? channel.pipeline().context(encoderContext) : null;

        if (context == null) {
            Iterator<Map.Entry<String, ChannelHandler>> handlerIterator = channel.pipeline().iterator();
            do {
                if (!handlerIterator.hasNext()) {
                    throw new IllegalStateException("Couldn't find encoder handler.");
                }
            } while (!handlerIterator.next().getKey().equals("framedimage:encoder"));

            encoderContext = handlerIterator.next().getKey();

            writePacket(channel, packet);
            return;
        }

        context.writeAndFlush(packet);
    }

    // 刷新地图画(用于动态地图画)
    public void displayNextFrame(FrameDisplay display) {
        Location location = display.getLocation();
        Collection<Player> players = location.getNearbyPlayers(256);
        List<Packet> packets = display.getNextFramePackets();
        players.forEach(player -> {
            if (!loggingPlayers.contains(player.getName())) {
                Channel channel = playerChannels.get(player);
                if (channel != null) {
                    packets.forEach(packet -> writePacket(channel, packet));
                }
            }
        });
    }

    public void spawn(FrameDisplay display, Player player) {
        Channel channel = playerChannels.get(player);
        if (channel != null) {
            display.getSpawnPackets().forEach(packet -> writePacket(channel, packet));
            display.getNextFramePackets().forEach(packet -> writePacket(channel, packet));
        }
    }

    public void spawn(FrameDisplay display) {
        World world = display.getLocation().getWorld();
        if (world == null) {
            return;
        }

        List<Player> players = world.getPlayers();
        players.forEach(player -> spawn(display, player));
    }

    public void spawn(Player player) {
        World world = player.getLocation().getWorld();
        if (world == null) {
            return;
        }

        if (Config.IMP.DYNAMIC_FRAME_SPAWN.ENABLED) {
            updateSection(player);
            return;
        }

        List<FrameDisplay> displays = this.displays.get(world.getName());
        if (displays != null) {
            displays.forEach(display -> spawn(display, player));
        }
    }

    public void destroy(FrameDisplay display, Player player) {
        Channel channel = playerChannels.get(player);
        if (channel != null) {
            display.getDestroyPackets().forEach(packet -> writePacket(channel, packet));
        }
    }

    public void destroy(FrameDisplay display) {
        CancellableTask updater = updatableDisplays.remove(display);
        if (updater != null) {
            updater.cancel();
        }

        World world = display.getLocation().getWorld();
        if (world == null) {
            return;
        }

        List<Player> players = world.getPlayers();
        if (Config.IMP.DYNAMIC_FRAME_SPAWN.ENABLED) {
            removeSections(display);
            players.forEach(this::updateSection);
        } else {
            players.forEach(player -> destroy(display, player));
        }
    }

    public void destroyAll() {
        displays.values().stream().flatMap(List::stream).forEach(this::destroy);
    }

    private void addSections(FrameDisplay display) {
        String worldName = display.getLocation().getWorld().getName();
        Map<Long, List<FrameDisplay>> world = sectionDisplays.computeIfAbsent(worldName, key -> new ConcurrentHashMap<>());
        display.getSections().forEach(section -> world.computeIfAbsent(section, key -> Collections.synchronizedList(new ArrayList<>())).add(display));
    }

    private void removeSections(FrameDisplay display) {
        String worldName = display.getLocation().getWorld().getName();
        Map<Long, List<FrameDisplay>> world = sectionDisplays.get(worldName);
        display.getSections().forEach(section -> {
            List<FrameDisplay> displays = world.get(section);
            displays.remove(display);
            if (displays.isEmpty()) {
                world.remove(section);
                if (world.isEmpty()) {
                    sectionDisplays.remove(worldName);
                }
            }
        });
    }

    public void add(FrameDisplay display) {
        World world = display.getLocation().getWorld();
        if (world == null) {
            return;
        }

        if (Config.IMP.DYNAMIC_FRAME_SPAWN.ENABLED) {
            addSections(display);
            world.getPlayers().forEach(this::updateSection);
        } else {
            spawn(display);
        }

        if (display.getNumFrames() > 1) {
            updatableDisplays.put(display, scheduler.runAtFixedRate(this, display.getLocation(), () -> displayNextFrame(display), 1L, 1L));
        }

        displays.computeIfAbsent(world.getName(), k -> new ArrayList<>()).add(display);

        try {
            synchronized (Frames.class) {
                Frames.IMP.FRAMES.put(display.getUUID().toString(), new Frames.FrameNode(display, getDataFolder()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(FrameDisplay display) {
        World world = display.getLocation().getWorld();
        if (world == null) {
            return;
        }

        destroy(display);
        displays.get(world.getName()).remove(display);

        synchronized (Frames.class) {
            Frames.IMP.FRAMES.remove(display.getUUID().toString());
        }
    }

    public void removeAll() {
        destroyAll();
        displays.clear();
        sectionDisplays.clear();

        synchronized (Frames.class) {
            Frames.IMP.FRAMES.clear();
        }
    }

    public void saveFrames() {
        Frames.IMP.save(framesFile);
    }

    public void reload() {
        removeAll();

        Config.IMP.reload(configFile);
        Frames.IMP.reload(framesFile);

        for (Palette palette : Palette.ALL_PALETTES) {
            colorMatchers.put(palette, Config.IMP.DITHERING ? new BufferedImageMatcher(palette) : new CachedColorMatcher(palette));
        }

        synchronized (Frames.class) {
            getLogger().info("Loading " + Frames.IMP.FRAMES.size() + " images.");

            Frames.IMP.FRAMES.forEach((uuid, node) -> {
                try {
                    add(node.createFrameDisplay(this, UUID.fromString(uuid), UUID.fromString(node.PLAYER)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        saveFrames();

        if (moveListener != null) {
            HandlerList.unregisterAll(moveListener);
            moveListener = null;
        }

        if (Config.IMP.DYNAMIC_FRAME_SPAWN.ENABLED) {
            moveListener = new MoveListener(this);
            Bukkit.getPluginManager().registerEvents(moveListener, this);
        }
    }

    public void updateSection(Player player, String world, long section) {
        Set<FrameDisplay> currentlyLoaded = playerDisplays.computeIfAbsent(player.getName(), key -> ConcurrentHashMap.newKeySet());
        List<FrameDisplay> sectionDisplays = getSectionDisplays(world, section);

        for (FrameDisplay display : sectionDisplays) {
            if (!currentlyLoaded.remove(display)) {
                spawn(display, player);
            }
        }

        for (FrameDisplay display : currentlyLoaded) {
            destroy(display, player);
            currentlyLoaded.remove(display);
        }

        currentlyLoaded.addAll(sectionDisplays);
    }

    public void updateSection(Player player) {
        Location location = player.getLocation();
        updateSection(player, location.getWorld().getName(), SectionUtil.getSectionIndex(location));
    }

    public Map<Palette, ColorMatcher> getColorMatchers() {
        return colorMatchers;
    }

    public Map<String, Map<Long, List<FrameDisplay>>> getAllSectionDisplays() {
        return sectionDisplays;
    }

    public Map<String, Set<FrameDisplay>> getPlayerDisplays() {
        return playerDisplays;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }
}
