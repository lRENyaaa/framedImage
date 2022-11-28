/*
 *  Copyright (C) 2022  cakemc-ru
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

package ru.cakemc.framedimage.protocol.packets;

import com.jnngl.mapcolor.palette.Palette;
import io.netty.buffer.ByteBuf;
import ru.cakemc.framedimage.protocol.MinecraftVersion;
import ru.cakemc.framedimage.protocol.Packet;
import ru.cakemc.framedimage.protocol.ProtocolUtils;

import java.util.Map;

public class MapData implements Packet {

  private final int mapID;
  private final byte scale;
  private final int columns;
  private final int rows;
  private final int posX;
  private final int posY;
  private final Map<Palette, byte[]> data;

  public MapData(int mapID, byte scale, int columns, int rows, int posX, int posY, Map<Palette, byte[]> data) {
    this.mapID = mapID;
    this.scale = scale;
    this.columns = columns;
    this.rows = rows;
    this.posX = posX;
    this.posY = posY;
    this.data = data;
  }

  public MapData(int mapID, byte scale, int posX, Map<Palette, byte[]> data) {
    this(mapID, scale, 128, 128, posX, 0, data);
  }

  public MapData(int mapID, byte scale, Map<Palette, byte[]> data) {
    this(mapID, scale, 0, data);
  }

  @Override
  public void encode(ByteBuf buf, MinecraftVersion version) {
    byte[] data = this.data.get(Palette.getPaletteForProtocol(version.getProtocolVersion()));

    ProtocolUtils.writeVarInt(buf, this.mapID);
    if (version.compareTo(MinecraftVersion.MINECRAFT_1_8) < 0) {
      buf.writeShort(data.length + 3);
      buf.writeByte(0);
      buf.writeByte(posX);
      buf.writeByte(posY);

      buf.writeBytes(data);
    } else {
      buf.writeByte(this.scale);

      if (version.compareTo(MinecraftVersion.MINECRAFT_1_9) >= 0 && version.compareTo(MinecraftVersion.MINECRAFT_1_17) < 0) {
        buf.writeBoolean(false);
      }

      if (version.compareTo(MinecraftVersion.MINECRAFT_1_14) >= 0) {
        buf.writeBoolean(false);
      }

      if (version.compareTo(MinecraftVersion.MINECRAFT_1_17) >= 0) {
        buf.writeBoolean(false);
      } else {
        ProtocolUtils.writeVarInt(buf, 0);
      }

      buf.writeByte(columns);
      buf.writeByte(rows);
      buf.writeByte(posX);
      buf.writeByte(posY);

      ProtocolUtils.writeVarInt(buf, data.length);
      buf.writeBytes(data);
    }
  }

  @Override
  public int getID(MinecraftVersion version) {
    if (version.compareTo(MinecraftVersion.MINECRAFT_1_19_1) >= 0) {
      return 0x26;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_19) >= 0) {
      return 0x24;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_17) >= 0) {
      return 0x27;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_16_2) >= 0) {
      return 0x25;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_16) >= 0) {
      return 0x26;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_15) >= 0) {
      return 0x27;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_13) >= 0) {
      return 0x26;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_9) >= 0) {
      return 0x24;
    } else {
      return 0x34;
    }
  }
}
