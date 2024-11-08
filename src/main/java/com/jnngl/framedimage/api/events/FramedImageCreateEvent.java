package com.jnngl.framedimage.api.events;

import com.jnngl.framedimage.FrameDisplay;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FramedImageCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final FrameDisplay display;

    public FramedImageCreateEvent(FrameDisplay display){
        this.display = display;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean callEvent() {
        Bukkit.getPluginManager().callEvent(this);
        return this.isCancelled();
    }

    @SuppressWarnings("unused")
    public FrameDisplay getFrameDisplay() {
        return display;
    }
}
