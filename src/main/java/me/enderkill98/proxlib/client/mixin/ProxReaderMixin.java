package me.enderkill98.proxlib.client.mixin;

import me.enderkill98.proxlib.ProxPacketReceiveHandler;
import me.enderkill98.proxlib.ProxPlayerReader;
import me.enderkill98.proxlib.client.ProxLib;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ProxReaderMixin {

	@Unique private static final Logger LOGGER = LoggerFactory.getLogger("ProxLib/Mixin/Reader");
	@Unique public final HashMap<PlayerEntity, ProxPlayerReader> proxlib$readers = new HashMap<>();
	@Unique private long readersLastCleanedUpAt = -1L;

	@Unique private static ProxPlayerReader proxChat$createReaderFor(PlayerEntity sender) {
		ProxPlayerReader reader = new ProxPlayerReader(sender);
		reader.addHandler((_sender, id, data) -> {
			// Call all global handlers
			for(ProxPacketReceiveHandler globalHandler : ProxLib.getGlobalHandlers()) {
				try {
					globalHandler.onReceived(sender, id, data);
				}catch (Exception ex) {
					LOGGER.error("A registered global handler threw an exception when handling a packet (id: " + id + ", data: " + data.length + " bytes)!", ex);
				}
			}

			// Call relevant specific handlers
			for(ProxPacketReceiveHandler specificHandler : ProxLib.getHandlersFor(id)) {
				try {
					specificHandler.onReceived(sender, id, data);
				}catch (Exception ex) {
					LOGGER.error("A registered specific handler threw an exception when handling a packet (id: " + id + ", data: " + data.length + " bytes)!", ex);
				}
			}
		});
		return reader;
	}

	@Inject(at = @At("RETURN"), method = { "onPlayerRespawn", "onGameJoin", "clearWorld" })
	public void stateCleared(CallbackInfo info) {
		proxlib$readers.clear();
	}

	/**
	 * Should be received every 20 server ticks. Might be more or less for numerous factors (lag, changed tick speed)
	 */
	@Inject(at = @At("RETURN"), method = "onWorldTimeUpdate")
	public void onWorldTimeUpdate(CallbackInfo info) {
		if(readersLastCleanedUpAt != -1L && Util.getMeasuringTimeMs() - readersLastCleanedUpAt < 750)
			return; // Already called too recently

		// Remove all readers where the player was removed
		proxlib$readers.keySet().stream().filter(Entity::isRemoved).toList().forEach(proxlib$readers::remove);
		readersLastCleanedUpAt = Util.getMeasuringTimeMs();
	}

	@Inject(at = @At("RETURN"), method = "onBlockBreakingProgress")
	public void onBlockBreakingProgress(BlockBreakingProgressS2CPacket packet, CallbackInfo info) {
		if(packet.getProgress() != 255) return; // Not the result of ABORT_DESTROY_BLOCK

		final MinecraftClient client = MinecraftClient.getInstance();
		if(client.player == null || client.world == null) return;
		Entity senderEntity = client.world.getEntityById(packet.getEntityId());
		if(!(senderEntity instanceof PlayerEntity sender)) return;

		ProxPlayerReader reader = proxlib$readers.computeIfAbsent(sender, ProxReaderMixin::proxChat$createReaderFor);
		reader.handle(packet);
	}

}
