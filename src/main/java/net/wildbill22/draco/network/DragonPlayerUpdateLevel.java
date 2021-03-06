package net.wildbill22.draco.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.wildbill22.draco.entities.player.DragonPlayer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Used to tell the client about changes
 * @author William
 *
 */
public class DragonPlayerUpdateLevel implements IMessage {
	private int value;

	/**
	 * Don't use
	 */
	public DragonPlayerUpdateLevel() {	}

	/**
	 * 
	 * @param value new value
	 */
	public DragonPlayerUpdateLevel(int value) {
		this.value = value;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.value = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.value);
	}
	
	public static class Handler implements IMessageHandler<DragonPlayerUpdateLevel, IMessage> {
		@Override
		public IMessage onMessage(DragonPlayerUpdateLevel message, MessageContext ctx) {
			EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
			if (player != null) {
				DragonPlayer dragonPlayer = DragonPlayer.get(player);
				if (dragonPlayer != null)
					dragonPlayer.setLevel(message.value, false);
			}
			return null;
		}
	}
}
