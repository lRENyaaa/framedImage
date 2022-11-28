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

import io.netty.buffer.ByteBuf;
import ru.cakemc.framedimage.protocol.MinecraftVersion;
import ru.cakemc.framedimage.protocol.Packet;
import ru.cakemc.framedimage.protocol.ProtocolUtils;

public class DestroyEntity implements Packet {

  private final int entity;

  public DestroyEntity(int entity) {
    this.entity = entity;
  }

  @Override
  public void encode(ByteBuf buf, MinecraftVersion version) {
    if (version.compareTo(MinecraftVersion.MINECRAFT_1_17) != 0) {
      buf.writeByte(1);
    }

    if (version.compareTo(MinecraftVersion.MINECRAFT_1_8) >= 0) {
      ProtocolUtils.writeVarInt(buf, entity);
    } else {
      buf.writeInt(entity);
    }
  }

  @Override
  public int getID(MinecraftVersion version) {
    if (version.compareTo(MinecraftVersion.MINECRAFT_1_19_1) >= 0) {
      return 0x3B;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_19) >= 0) {
      return 0x38;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_17) >= 0) {
      return 0x3A;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_16) >= 0) {
      return 0x36;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_15) >= 0) {
      return 0x38;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_14) >= 0) {
      return 0x37;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_13) >= 0) {
      return 0x35;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_12_1) >= 0) {
      return 0x32;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_12) >= 0) {
      return 0x31;
    } else if (version.compareTo(MinecraftVersion.MINECRAFT_1_9) >= 0) {
      return 0x30;
    } else {
      return 0x13;
    }
  }
}
